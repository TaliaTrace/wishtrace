import hashlib
import json
import re
import uuid
from datetime import UTC, datetime
from enum import StrEnum
from typing import Literal, Protocol
from urllib.parse import urlsplit

from pydantic import BaseModel, ConfigDict
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.auth import AuthenticatedUser
from app.errors import ApiError
from app.models import (
    CandidateSnapshotModel,
    DiscoveryRunModel,
    IdempotencyOperationModel,
    OccasionModel,
    PravaSessionModel,
    PurchaseIntentModel,
    TransactionTransitionModel,
)
from app.prava import (
    HostedPravaSession,
    PravaGatewayError,
    PravaHttpGateway,
    PravaPaymentResult,
    PravaPaymentStatus,
    PravaSessionRequest,
)

IDEMPOTENCY_PATTERN = re.compile(r"^[A-Za-z0-9._:-]{8,255}$")


class TransactionState(StrEnum):
    DRAFT = "DRAFT"
    VALIDATING = "VALIDATING"
    QUOTED = "QUOTED"
    READY_FOR_APPROVAL = "READY_FOR_APPROVAL"
    SESSION_CREATING = "SESSION_CREATING"
    AWAITING_USER = "AWAITING_USER"
    CREDENTIALS_READY = "CREDENTIALS_READY"
    CHECKOUT_IN_PROGRESS = "CHECKOUT_IN_PROGRESS"
    ORDER_VERIFIED = "ORDER_VERIFIED"
    SUCCEEDED = "SUCCEEDED"
    DECLINED = "DECLINED"
    CANCELLED = "CANCELLED"
    EXPIRED = "EXPIRED"
    FAILED = "FAILED"
    UNKNOWN = "UNKNOWN"
    RECONCILING = "RECONCILING"


class PurchaseIntentCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    candidate_id: uuid.UUID


class ApprovalSessionResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    session_id: str
    hosted_url: str
    expires_at: datetime


class PurchaseIntentResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    id: uuid.UUID
    recipient_id: uuid.UUID
    occasion_id: uuid.UUID
    candidate_id: uuid.UUID
    state: TransactionState
    merchant_id: str
    merchant_name: str
    merchant_url: str
    merchant_product_id: str
    merchant_variant_id: str
    sku: str | None
    title: str
    variant_title: str | None
    item_price_minor: int
    currency: Literal["USD"]
    approved_total_minor: int | None
    quote_source: str | None
    quote_timestamp: datetime | None
    quote_expires_at: datetime | None
    delivery_summary: str | None
    approval_session: ApprovalSessionResponse | None
    provider_status: str | None
    created_at: datetime
    updated_at: datetime


class PublicTransactionStatus(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    purchase_intent_id: uuid.UUID
    state: TransactionState
    provider_status: str | None
    recoverable: bool
    message: str


class SessionClaimAction(StrEnum):
    CREATE = "CREATE"
    REPLAY = "REPLAY"


class SessionClaim(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    action: SessionClaimAction
    existing_session: ApprovalSessionResponse | None = None


class PurchaseStore(Protocol):
    async def create_intent(
        self,
        user_id: uuid.UUID,
        candidate_id: uuid.UUID,
    ) -> PurchaseIntentResponse: ...

    async def get_intent(
        self,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse: ...

    async def claim_session_creation(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        request_hash: bytes,
        now: datetime,
    ) -> SessionClaim: ...

    async def complete_session_creation(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        session: HostedPravaSession,
    ) -> PurchaseIntentResponse: ...

    async def fail_session_creation(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        unknown: bool,
        response_id: str | None,
    ) -> PurchaseIntentResponse: ...

    async def record_payment_result(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        result: PravaPaymentResult,
        state: TransactionState,
        reason_code: str,
    ) -> PurchaseIntentResponse: ...

    async def begin_reconciliation(
        self,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse: ...


class PravaOperations(Protocol):
    async def create_session(self, request: PravaSessionRequest) -> HostedPravaSession: ...

    async def get_payment_result(self, session_id: str) -> PravaPaymentResult: ...


class PurchaseOperations(Protocol):
    async def create(
        self,
        user: AuthenticatedUser,
        body: PurchaseIntentCreate,
    ) -> PurchaseIntentResponse: ...

    async def get(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse: ...

    async def create_prava_session(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
        idempotency_key: str,
    ) -> PurchaseIntentResponse: ...

    async def reconcile(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse: ...


class PurchaseService:
    def __init__(
        self,
        *,
        store: PurchaseStore,
        prava: PravaOperations,
        public_base_url: str,
    ) -> None:
        self._store = store
        self._prava = prava
        self._public_base_url = public_base_url.rstrip("/")

    async def create(
        self,
        user: AuthenticatedUser,
        body: PurchaseIntentCreate,
    ) -> PurchaseIntentResponse:
        return await self._store.create_intent(user.id, body.candidate_id)

    async def get(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        return await self._store.get_intent(user.id, purchase_intent_id)

    async def create_prava_session(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
        idempotency_key: str,
    ) -> PurchaseIntentResponse:
        if not IDEMPOTENCY_PATTERN.fullmatch(idempotency_key):
            raise ApiError(
                status_code=400,
                code="IDEMPOTENCY_KEY_INVALID",
                message="A stable idempotency key is required for approval.",
                recoverable=True,
            )
        intent = await self._store.get_intent(user.id, purchase_intent_id)
        provider_request = self._session_request(user, intent)
        request_hash = hashlib.sha256(
            json.dumps(
                provider_request.provider_payload(),
                sort_keys=True,
                separators=(",", ":"),
            ).encode()
        ).digest()
        key_hash = hashlib.sha256(idempotency_key.encode()).digest()
        claim = await self._store.claim_session_creation(
            user_id=user.id,
            purchase_intent_id=purchase_intent_id,
            key_hash=key_hash,
            request_hash=request_hash,
            now=datetime.now(UTC),
        )
        if claim.action is SessionClaimAction.REPLAY:
            return await self._store.get_intent(user.id, purchase_intent_id)
        try:
            session = await self._prava.create_session(provider_request)
        except PravaGatewayError as error:
            await self._store.fail_session_creation(
                user_id=user.id,
                purchase_intent_id=purchase_intent_id,
                key_hash=key_hash,
                unknown=error.outcome_unknown,
                response_id=error.response_id,
            )
            raise ApiError(
                status_code=503 if error.recoverable else 502,
                code=error.code,
                message=error.safe_message,
                recoverable=error.recoverable,
            ) from error
        return await self._store.complete_session_creation(
            user_id=user.id,
            purchase_intent_id=purchase_intent_id,
            key_hash=key_hash,
            session=session,
        )

    async def reconcile(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        intent = await self._store.get_intent(user.id, purchase_intent_id)
        approval_session = intent.approval_session
        if approval_session is None:
            raise ApiError(
                status_code=409,
                code="PRAVA_SESSION_NOT_CREATED",
                message="Start Prava approval before refreshing its status.",
                recoverable=True,
            )
        if intent.state is TransactionState.UNKNOWN:
            intent = await self._store.begin_reconciliation(
                user.id,
                purchase_intent_id,
            )
        try:
            result = await self._prava.get_payment_result(approval_session.session_id)
        except PravaGatewayError as error:
            raise ApiError(
                status_code=503 if error.recoverable else 502,
                code=error.code,
                message=error.safe_message,
                recoverable=error.recoverable,
            ) from error
        if result.session_id != approval_session.session_id:
            raise ApiError(
                status_code=502,
                code="PRAVA_RESPONSE_INVALID",
                message="Prava returned a mismatched session.",
                recoverable=False,
            )
        _validate_payment_result(intent, result)
        next_state, reason = _state_from_payment_result(result)
        if (
            next_state is TransactionState.AWAITING_USER
            and approval_session.expires_at <= datetime.now(UTC)
        ):
            next_state = TransactionState.EXPIRED
            reason = "PRAVA_SESSION_EXPIRED"
        return await self._store.record_payment_result(
            user_id=user.id,
            purchase_intent_id=purchase_intent_id,
            result=result,
            state=next_state,
            reason_code=reason,
        )

    def _session_request(
        self,
        user: AuthenticatedUser,
        intent: PurchaseIntentResponse,
    ) -> PravaSessionRequest:
        if urlsplit(self._public_base_url).scheme != "https":
            raise ApiError(
                status_code=503,
                code="PUBLIC_CALLBACK_UNAVAILABLE",
                message="Secure Prava return handling is not configured yet.",
                recoverable=True,
            )
        if user.email is None:
            raise ApiError(
                status_code=409,
                code="VERIFIED_EMAIL_REQUIRED",
                message="A verified Google email is required for Prava approval.",
                recoverable=True,
            )
        # Build the same canonical request after the state has advanced so an
        # idempotency replay can be matched without issuing a second provider
        # call. The store atomically enforces READY_FOR_APPROVAL and quote
        # freshness before it claims a new operation.
        if intent.approved_total_minor is None or intent.quote_expires_at is None:
            raise ApiError(
                status_code=409,
                code="FRESH_QUOTE_REQUIRED",
                message="Refresh the final merchant total before opening Prava.",
                recoverable=True,
            )
        if len(intent.merchant_variant_id) > 50:
            raise ApiError(
                status_code=409,
                code="PRODUCT_IDENTIFIER_UNSUPPORTED",
                message="This merchant variant cannot be sent to Prava safely.",
                recoverable=False,
            )
        description = intent.title
        if intent.variant_title:
            description = f"{description} — {intent.variant_title}"
        callback_url = (
            f"{self._public_base_url}/v1/prava/return"
            f"?purchase_intent_id={intent.id}"
        )
        return PravaSessionRequest(
            user_id=str(user.id),
            user_email=user.email,
            total_minor=intent.approved_total_minor,
            currency="USD",
            merchant_name=intent.merchant_name,
            merchant_url=intent.merchant_url,
            merchant_country="US",
            product_description=description,
            product_unit_minor=intent.item_price_minor,
            external_product_id=intent.merchant_variant_id,
            quantity=1,
            callback_url=callback_url,
            external_order_ref=str(intent.id),
        )


class UnavailablePurchaseService:
    async def create(
        self,
        user: AuthenticatedUser,
        body: PurchaseIntentCreate,
    ) -> PurchaseIntentResponse:
        del user, body
        raise _unavailable()

    async def get(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        del user, purchase_intent_id
        raise _unavailable()

    async def create_prava_session(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
        idempotency_key: str,
    ) -> PurchaseIntentResponse:
        del user, purchase_intent_id, idempotency_key
        raise _unavailable()

    async def reconcile(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        del user, purchase_intent_id
        raise _unavailable()


class SqlPurchaseStore:
    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def create_intent(
        self,
        user_id: uuid.UUID,
        candidate_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        async with self._session_factory() as session, session.begin():
            row = (
                await session.execute(
                    select(CandidateSnapshotModel, DiscoveryRunModel)
                    .join(
                        DiscoveryRunModel,
                        DiscoveryRunModel.id == CandidateSnapshotModel.discovery_run_id,
                    )
                    .where(
                        CandidateSnapshotModel.id == candidate_id,
                        DiscoveryRunModel.user_id == user_id,
                    )
                    .with_for_update()
                )
            ).one_or_none()
            if row is None:
                raise _not_found("CANDIDATE_NOT_FOUND", "That gift option was not found.")
            candidate, discovery = row
            if (
                not candidate.eligible
                or not candidate.checkout_supported
                or candidate.merchant_variant_id is None
                or candidate.source_mode != "LIVE"
            ):
                raise ApiError(
                    status_code=409,
                    code="CANDIDATE_NOT_PURCHASABLE",
                    message="This gift does not have a verified checkout path yet.",
                    recoverable=True,
                )
            occasion = await session.scalar(
                select(OccasionModel).where(
                    OccasionModel.id == discovery.occasion_id,
                    OccasionModel.user_id == user_id,
                )
            )
            if occasion is None:
                raise _not_found("OCCASION_NOT_FOUND", "That occasion was not found.")
            if candidate.price_minor > occasion.budget_minor:
                raise ApiError(
                    status_code=409,
                    code="CANDIDATE_OVER_BUDGET",
                    message="This gift now exceeds the saved budget.",
                    recoverable=True,
                )
            existing = await session.scalar(
                select(PurchaseIntentModel).where(
                    PurchaseIntentModel.user_id == user_id,
                    PurchaseIntentModel.candidate_snapshot_id == candidate_id,
                )
            )
            if existing is not None:
                return await _intent_response(session, existing)
            intent = PurchaseIntentModel(
                user_id=user_id,
                recipient_id=discovery.recipient_id,
                occasion_id=discovery.occasion_id,
                discovery_run_id=discovery.id,
                candidate_snapshot_id=candidate.id,
                state=TransactionState.DRAFT.value,
                merchant_id=discovery.merchant_id,
                merchant_name=discovery.merchant_name,
                merchant_url=_origin(candidate.product_url),
                merchant_product_id=candidate.merchant_product_id,
                merchant_variant_id=candidate.merchant_variant_id,
                sku=candidate.sku,
                title=candidate.title,
                variant_title=candidate.variant_title,
                item_price_minor=candidate.price_minor,
                currency="USD",
            )
            session.add(intent)
            await session.flush()
            session.add(
                TransactionTransitionModel(
                    purchase_intent_id=intent.id,
                    from_state=None,
                    to_state=TransactionState.DRAFT.value,
                    reason_code="PURCHASE_INTENT_CREATED",
                )
            )
            await session.flush()
            return await _intent_response(session, intent)

    async def get_intent(
        self,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        async with self._session_factory() as session:
            intent = await _owned_intent(session, user_id, purchase_intent_id)
            return await _intent_response(session, intent)

    async def claim_session_creation(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        request_hash: bytes,
        now: datetime,
    ) -> SessionClaim:
        async with self._session_factory() as session, session.begin():
            intent = await _owned_intent(
                session,
                user_id,
                purchase_intent_id,
                lock=True,
            )
            operation = await session.scalar(
                select(IdempotencyOperationModel)
                .where(
                    IdempotencyOperationModel.purchase_intent_id == purchase_intent_id,
                    IdempotencyOperationModel.operation == "PRAVA_SESSION",
                )
                .with_for_update()
            )
            if operation is not None:
                if operation.key_hash != key_hash or operation.request_hash != request_hash:
                    raise ApiError(
                        status_code=409,
                        code="IDEMPOTENCY_CONFLICT",
                        message="Approval was already requested with different facts.",
                        recoverable=False,
                    )
                if operation.status == "COMPLETED":
                    response = await _intent_response(session, intent)
                    if response.approval_session is None:
                        raise ApiError(
                            status_code=409,
                            code="TRANSACTION_UNKNOWN",
                            message="Approval state is incomplete. Refresh before continuing.",
                            recoverable=True,
                        )
                    return SessionClaim(
                        action=SessionClaimAction.REPLAY,
                        existing_session=response.approval_session,
                    )
                if operation.status == "UNKNOWN":
                    raise ApiError(
                        status_code=409,
                        code="TRANSACTION_UNKNOWN",
                        message=(
                            "Prava may have created a session. Do not retry; "
                            "refresh or contact support."
                        ),
                        recoverable=True,
                    )
                if operation.status == "IN_PROGRESS":
                    raise ApiError(
                        status_code=409,
                        code="PRAVA_SESSION_IN_PROGRESS",
                        message="Prava approval is already being created.",
                        recoverable=True,
                    )
                raise ApiError(
                    status_code=409,
                    code="PRAVA_SESSION_FAILED",
                    message="The prior approval request failed. Refresh before trying again.",
                    recoverable=True,
                )
            if (
                intent.state != TransactionState.READY_FOR_APPROVAL.value
                or intent.approved_total_minor is None
                or intent.quote_expires_at is None
                or intent.quote_expires_at <= now
            ):
                raise ApiError(
                    status_code=409,
                    code="FRESH_QUOTE_REQUIRED",
                    message="Refresh the final merchant total before opening Prava.",
                    recoverable=True,
                )
            session.add(
                IdempotencyOperationModel(
                    user_id=user_id,
                    purchase_intent_id=purchase_intent_id,
                    operation="PRAVA_SESSION",
                    key_hash=key_hash,
                    request_hash=request_hash,
                    status="IN_PROGRESS",
                )
            )
            _transition(
                session,
                intent,
                TransactionState.SESSION_CREATING,
                "PRAVA_SESSION_REQUESTED",
            )
            await session.flush()
            return SessionClaim(action=SessionClaimAction.CREATE)

    async def complete_session_creation(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        session: HostedPravaSession,
    ) -> PurchaseIntentResponse:
        async with self._session_factory() as database, database.begin():
            intent = await _owned_intent(
                database,
                user_id,
                purchase_intent_id,
                lock=True,
            )
            operation = await _owned_operation(database, purchase_intent_id, key_hash)
            if operation.status == "COMPLETED":
                return await _intent_response(database, intent)
            if operation.status != "IN_PROGRESS":
                raise ApiError(
                    status_code=409,
                    code="TRANSACTION_UNKNOWN",
                    message="Approval state changed unexpectedly. Refresh before continuing.",
                    recoverable=True,
                )
            database.add(
                PravaSessionModel(
                    purchase_intent_id=intent.id,
                    provider_session_id=session.session_id,
                    provider_order_id=session.order_id,
                    hosted_url=session.hosted_url,
                    expires_at=session.expires_at,
                    create_response_id=session.response_id,
                    provider_status=PravaPaymentStatus.PENDING.value,
                )
            )
            operation.status = "COMPLETED"
            operation.provider_response_id = session.response_id
            operation.updated_at = datetime.now(UTC)
            _transition(
                database,
                intent,
                TransactionState.AWAITING_USER,
                "PRAVA_SESSION_CREATED",
                session.response_id,
            )
            await database.flush()
            return await _intent_response(database, intent)

    async def fail_session_creation(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        unknown: bool,
        response_id: str | None,
    ) -> PurchaseIntentResponse:
        async with self._session_factory() as session, session.begin():
            intent = await _owned_intent(
                session,
                user_id,
                purchase_intent_id,
                lock=True,
            )
            operation = await _owned_operation(session, purchase_intent_id, key_hash)
            operation.status = "UNKNOWN" if unknown else "FAILED"
            operation.provider_response_id = response_id
            operation.updated_at = datetime.now(UTC)
            _transition(
                session,
                intent,
                TransactionState.UNKNOWN if unknown else TransactionState.FAILED,
                "PRAVA_SESSION_OUTCOME_UNKNOWN" if unknown else "PRAVA_SESSION_REJECTED",
                response_id,
            )
            await session.flush()
            return await _intent_response(session, intent)

    async def record_payment_result(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        result: PravaPaymentResult,
        state: TransactionState,
        reason_code: str,
    ) -> PurchaseIntentResponse:
        async with self._session_factory() as session, session.begin():
            intent = await _owned_intent(
                session,
                user_id,
                purchase_intent_id,
                lock=True,
            )
            provider_session = await session.scalar(
                select(PravaSessionModel)
                .where(PravaSessionModel.purchase_intent_id == purchase_intent_id)
                .with_for_update()
            )
            if (
                provider_session is None
                or provider_session.provider_session_id != result.session_id
            ):
                raise ApiError(
                    status_code=409,
                    code="PRAVA_SESSION_NOT_FOUND",
                    message="The Prava session could not be reconciled.",
                    recoverable=True,
                )
            if (
                result.order_id is not None
                and result.order_id != provider_session.provider_order_id
            ):
                raise ApiError(
                    status_code=502,
                    code="PRAVA_RESPONSE_MISMATCH",
                    message="Prava returned a result for a different purchase.",
                    recoverable=False,
                )
            provider_session.provider_status = result.status.value
            provider_session.last_response_id = result.response_id
            provider_session.updated_at = datetime.now(UTC)
            if result.transactions:
                provider_session.provider_transaction_id = result.transactions[0].txn_id
                if result.transactions[0].line_items:
                    provider_session.provider_txn_ref_id = (
                        result.transactions[0].line_items[0].txn_ref_id
                    )
            if intent.state != state.value:
                _transition(session, intent, state, reason_code, result.response_id)
            await session.flush()
            return await _intent_response(session, intent)

    async def begin_reconciliation(
        self,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        async with self._session_factory() as session, session.begin():
            intent = await _owned_intent(
                session,
                user_id,
                purchase_intent_id,
                lock=True,
            )
            if intent.state == TransactionState.UNKNOWN.value:
                _transition(
                    session,
                    intent,
                    TransactionState.RECONCILING,
                    "USER_REQUESTED_RECONCILIATION",
                )
                await session.flush()
            elif intent.state != TransactionState.RECONCILING.value:
                raise ApiError(
                    status_code=409,
                    code="TRANSACTION_STATE_CONFLICT",
                    message="This transaction is not waiting for reconciliation.",
                    recoverable=True,
                )
            return await _intent_response(session, intent)


async def _owned_intent(
    session: AsyncSession,
    user_id: uuid.UUID,
    purchase_intent_id: uuid.UUID,
    *,
    lock: bool = False,
) -> PurchaseIntentModel:
    statement = select(PurchaseIntentModel).where(
        PurchaseIntentModel.id == purchase_intent_id,
        PurchaseIntentModel.user_id == user_id,
    )
    if lock:
        statement = statement.with_for_update()
    intent = await session.scalar(statement)
    if intent is None:
        raise _not_found("PURCHASE_INTENT_NOT_FOUND", "That purchase review was not found.")
    return intent


async def _owned_operation(
    session: AsyncSession,
    purchase_intent_id: uuid.UUID,
    key_hash: bytes,
) -> IdempotencyOperationModel:
    operation = await session.scalar(
        select(IdempotencyOperationModel)
        .where(
            IdempotencyOperationModel.purchase_intent_id == purchase_intent_id,
            IdempotencyOperationModel.operation == "PRAVA_SESSION",
            IdempotencyOperationModel.key_hash == key_hash,
        )
        .with_for_update()
    )
    if operation is None:
        raise ApiError(
            status_code=409,
            code="TRANSACTION_UNKNOWN",
            message="Approval state could not be recovered.",
            recoverable=True,
        )
    return operation


async def _intent_response(
    session: AsyncSession,
    intent: PurchaseIntentModel,
) -> PurchaseIntentResponse:
    provider_session = await session.scalar(
        select(PravaSessionModel).where(
            PravaSessionModel.purchase_intent_id == intent.id
        )
    )
    return PurchaseIntentResponse(
        id=intent.id,
        recipient_id=intent.recipient_id,
        occasion_id=intent.occasion_id,
        candidate_id=intent.candidate_snapshot_id,
        state=TransactionState(intent.state),
        merchant_id=intent.merchant_id,
        merchant_name=intent.merchant_name,
        merchant_url=intent.merchant_url,
        merchant_product_id=intent.merchant_product_id,
        merchant_variant_id=intent.merchant_variant_id,
        sku=intent.sku,
        title=intent.title,
        variant_title=intent.variant_title,
        item_price_minor=intent.item_price_minor,
        currency="USD",
        approved_total_minor=intent.approved_total_minor,
        quote_source=intent.quote_source,
        quote_timestamp=intent.quote_timestamp,
        quote_expires_at=intent.quote_expires_at,
        delivery_summary=intent.delivery_summary,
        approval_session=(
            ApprovalSessionResponse(
                session_id=provider_session.provider_session_id,
                hosted_url=provider_session.hosted_url,
                expires_at=provider_session.expires_at,
            )
            if provider_session is not None
            else None
        ),
        provider_status=(provider_session.provider_status if provider_session else None),
        created_at=intent.created_at,
        updated_at=intent.updated_at,
    )


def _transition(
    session: AsyncSession,
    intent: PurchaseIntentModel,
    target: TransactionState,
    reason_code: str,
    provider_response_id: str | None = None,
) -> None:
    source = intent.state
    _require_transition(TransactionState(source), target)
    intent.state = target.value
    intent.updated_at = datetime.now(UTC)
    session.add(
        TransactionTransitionModel(
            purchase_intent_id=intent.id,
            from_state=source,
            to_state=target.value,
            reason_code=reason_code,
            provider_response_id=provider_response_id,
        )
    )


def _require_transition(source: TransactionState, target: TransactionState) -> None:
    allowed: dict[TransactionState, set[TransactionState]] = {
        TransactionState.DRAFT: {TransactionState.VALIDATING},
        TransactionState.VALIDATING: {
            TransactionState.QUOTED,
            TransactionState.FAILED,
        },
        TransactionState.QUOTED: {
            TransactionState.READY_FOR_APPROVAL,
            TransactionState.FAILED,
        },
        TransactionState.READY_FOR_APPROVAL: {
            TransactionState.SESSION_CREATING,
            TransactionState.EXPIRED,
            TransactionState.CANCELLED,
        },
        TransactionState.SESSION_CREATING: {
            TransactionState.AWAITING_USER,
            TransactionState.FAILED,
            TransactionState.UNKNOWN,
        },
        TransactionState.AWAITING_USER: {
            TransactionState.CREDENTIALS_READY,
            TransactionState.CANCELLED,
            TransactionState.EXPIRED,
            TransactionState.FAILED,
            TransactionState.UNKNOWN,
        },
        TransactionState.CREDENTIALS_READY: {
            TransactionState.CHECKOUT_IN_PROGRESS,
            TransactionState.EXPIRED,
            TransactionState.FAILED,
            TransactionState.UNKNOWN,
        },
        TransactionState.CHECKOUT_IN_PROGRESS: {
            TransactionState.ORDER_VERIFIED,
            TransactionState.DECLINED,
            TransactionState.FAILED,
            TransactionState.UNKNOWN,
        },
        TransactionState.ORDER_VERIFIED: {
            TransactionState.SUCCEEDED,
            TransactionState.UNKNOWN,
        },
        TransactionState.UNKNOWN: {TransactionState.RECONCILING},
        TransactionState.RECONCILING: {
            TransactionState.AWAITING_USER,
            TransactionState.CREDENTIALS_READY,
            TransactionState.ORDER_VERIFIED,
            TransactionState.SUCCEEDED,
            TransactionState.DECLINED,
            TransactionState.CANCELLED,
            TransactionState.EXPIRED,
            TransactionState.FAILED,
            TransactionState.UNKNOWN,
        },
    }
    if target not in allowed.get(source, set()):
        raise ApiError(
            status_code=409,
            code="TRANSACTION_STATE_CONFLICT",
            message="Transaction state changed. Refresh before continuing.",
            recoverable=True,
        )


def _state_from_payment_result(
    result: PravaPaymentResult,
) -> tuple[TransactionState, str]:
    if result.status is PravaPaymentStatus.PENDING:
        if result.credentials:
            raise ApiError(
                status_code=502,
                code="PRAVA_RESPONSE_INVALID",
                message="Prava returned credentials before approval completed.",
                recoverable=False,
            )
        return TransactionState.AWAITING_USER, "PRAVA_PENDING"
    if result.status is PravaPaymentStatus.AWAITING_RESULT:
        if len(result.credentials) != 1:
            raise ApiError(
                status_code=502,
                code="PRAVA_RESPONSE_INVALID",
                message="Prava did not return one usable checkout credential.",
                recoverable=False,
            )
        return TransactionState.CREDENTIALS_READY, "PRAVA_CREDENTIALS_READY"
    if result.status is PravaPaymentStatus.FAILED:
        return TransactionState.FAILED, "PRAVA_FAILED"
    raise ApiError(
        status_code=409,
        code="ORDER_VERIFICATION_REQUIRED",
        message="Prava completed, but WishTrace has not verified a merchant order.",
        recoverable=True,
    )


def _validate_payment_result(
    intent: PurchaseIntentResponse,
    result: PravaPaymentResult,
) -> None:
    if result.status is not PravaPaymentStatus.AWAITING_RESULT:
        return
    if len(result.transactions) != 1 or len(result.transactions[0].line_items) != 1:
        raise _payment_fact_mismatch()
    transaction = result.transactions[0]
    line_item = transaction.line_items[0]
    if (
        transaction.status is not PravaPaymentStatus.AWAITING_RESULT
        or line_item.status is not PravaPaymentStatus.AWAITING_RESULT
        or intent.approved_total_minor is None
        or line_item.total_minor != intent.approved_total_minor
        or line_item.merchant_url is None
        or _origin(line_item.merchant_url) != intent.merchant_url
    ):
        raise _payment_fact_mismatch()


def _payment_fact_mismatch() -> ApiError:
    return ApiError(
        status_code=502,
        code="PRAVA_RESPONSE_MISMATCH",
        message="Prava returned purchase facts that do not match your review.",
        recoverable=False,
    )


def transaction_status(intent: PurchaseIntentResponse) -> PublicTransactionStatus:
    messages: dict[TransactionState, tuple[str, bool]] = {
        TransactionState.DRAFT: ("A fresh merchant quote is still required.", True),
        TransactionState.VALIDATING: ("WishTrace is validating the selected gift.", True),
        TransactionState.QUOTED: ("The merchant total is ready for review.", True),
        TransactionState.READY_FOR_APPROVAL: ("Review and approve this amount in Prava.", True),
        TransactionState.SESSION_CREATING: ("Prava approval is being created.", True),
        TransactionState.AWAITING_USER: ("Finish approval in the secure Prava page.", True),
        TransactionState.CREDENTIALS_READY: (
            "Approval finished; merchant checkout still needs verification.",
            True,
        ),
        TransactionState.CHECKOUT_IN_PROGRESS: (
            "The real merchant checkout is in progress.",
            True,
        ),
        TransactionState.ORDER_VERIFIED: ("The merchant order is being reconciled.", True),
        TransactionState.SUCCEEDED: ("The merchant order and Prava result are verified.", False),
        TransactionState.DECLINED: ("The merchant checkout was declined.", False),
        TransactionState.CANCELLED: ("Approval was cancelled.", False),
        TransactionState.EXPIRED: ("The approval session expired.", True),
        TransactionState.FAILED: ("The transaction failed safely.", True),
        TransactionState.UNKNOWN: (
            "The result is uncertain. Do not retry the purchase; refresh first.",
            True,
        ),
        TransactionState.RECONCILING: ("WishTrace is checking the authoritative result.", True),
    }
    message, recoverable = messages[intent.state]
    return PublicTransactionStatus(
        purchase_intent_id=intent.id,
        state=intent.state,
        provider_status=intent.provider_status,
        recoverable=recoverable,
        message=message,
    )


def _origin(value: str) -> str:
    parsed = urlsplit(value)
    if parsed.scheme != "https" or parsed.hostname is None:
        raise ApiError(
            status_code=409,
            code="MERCHANT_URL_INVALID",
            message="This merchant link is not safe for checkout.",
            recoverable=False,
        )
    port = f":{parsed.port}" if parsed.port is not None else ""
    return f"https://{parsed.hostname}{port}"


def _not_found(code: str, message: str) -> ApiError:
    return ApiError(
        status_code=404,
        code=code,
        message=message,
        recoverable=True,
    )


def _unavailable() -> ApiError:
    return ApiError(
        status_code=503,
        code="PRAVA_UNAVAILABLE",
        message="Prava approval is not configured yet.",
        recoverable=True,
    )


def build_purchase_service(
    *,
    session_factory: async_sessionmaker[AsyncSession],
    prava: PravaHttpGateway,
    public_base_url: str,
) -> PurchaseOperations:
    return PurchaseService(
        store=SqlPurchaseStore(session_factory),
        prava=prava,
        public_base_url=public_base_url,
    )
