import uuid
from datetime import UTC, datetime

import pytest
from httpx import ASGITransport, AsyncClient
from pydantic import SecretStr

from app.auth import (
    AuthenticatedUser,
    ChallengeResponse,
    GoogleExchangeRequest,
    SessionResponse,
)
from app.config import Settings
from app.database import DatabaseProbe
from app.errors import ApiError
from app.main import create_app
from app.prava import (
    HostedPravaSession,
    PravaGatewayError,
    PravaLineItemResult,
    PravaPaymentResult,
    PravaPaymentStatus,
    PravaSessionRequest,
    PravaTransactionResult,
    SensitivePaymentCredential,
)
from app.purchase import (
    ApprovalSessionResponse,
    PurchaseIntentResponse,
    PurchaseService,
    PurchaseStore,
    SessionClaim,
    SessionClaimAction,
    TransactionState,
    _require_transition,
)


class MemoryPurchaseStore(PurchaseStore):
    def __init__(self, intent: PurchaseIntentResponse) -> None:
        self.intent = intent
        self.key_hash: bytes | None = None
        self.request_hash: bytes | None = None
        self.operation_status: str | None = None
        self.persisted_sensitive_credential = False

    async def create_intent(
        self,
        user_id: uuid.UUID,
        candidate_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        del user_id
        assert candidate_id == self.intent.candidate_id
        return self.intent

    async def get_intent(
        self,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        del user_id
        assert purchase_intent_id == self.intent.id
        return self.intent

    async def claim_session_creation(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        request_hash: bytes,
        now: datetime,
    ) -> SessionClaim:
        del user_id, now
        assert purchase_intent_id == self.intent.id
        if self.key_hash is not None:
            if self.key_hash != key_hash or self.request_hash != request_hash:
                raise ApiError(
                    status_code=409,
                    code="IDEMPOTENCY_CONFLICT",
                    message="Approval was already requested with different facts.",
                    recoverable=False,
                )
            if self.operation_status == "COMPLETED":
                return SessionClaim(
                    action=SessionClaimAction.REPLAY,
                    existing_session=self.intent.approval_session,
                )
            if self.operation_status == "UNKNOWN":
                raise ApiError(
                    status_code=409,
                    code="TRANSACTION_UNKNOWN",
                    message="Do not retry.",
                    recoverable=True,
                )
            raise ApiError(
                status_code=409,
                code="PRAVA_SESSION_IN_PROGRESS",
                message="Already creating.",
                recoverable=True,
            )
        if self.intent.state != TransactionState.READY_FOR_APPROVAL:
            raise ApiError(
                status_code=409,
                code="FRESH_QUOTE_REQUIRED",
                message="Refresh quote.",
                recoverable=True,
            )
        self.key_hash = key_hash
        self.request_hash = request_hash
        self.operation_status = "IN_PROGRESS"
        self.intent = self.intent.model_copy(update={"state": TransactionState.SESSION_CREATING})
        return SessionClaim(action=SessionClaimAction.CREATE)

    async def complete_session_creation(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        session: HostedPravaSession,
    ) -> PurchaseIntentResponse:
        del user_id
        assert purchase_intent_id == self.intent.id
        assert key_hash == self.key_hash
        self.operation_status = "COMPLETED"
        self.intent = self.intent.model_copy(
            update={
                "state": TransactionState.AWAITING_USER,
                "approval_session": ApprovalSessionResponse(
                    session_id=session.session_id,
                    hosted_url=session.hosted_url,
                    expires_at=session.expires_at,
                ),
                "provider_status": "pending",
            }
        )
        return self.intent

    async def fail_session_creation(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        unknown: bool,
        response_id: str | None,
    ) -> PurchaseIntentResponse:
        del user_id, response_id
        assert purchase_intent_id == self.intent.id
        assert key_hash == self.key_hash
        self.operation_status = "UNKNOWN" if unknown else "FAILED"
        self.intent = self.intent.model_copy(
            update={
                "state": TransactionState.UNKNOWN if unknown else TransactionState.FAILED
            }
        )
        return self.intent

    async def record_payment_result(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        result: PravaPaymentResult,
        state: TransactionState,
        reason_code: str,
    ) -> PurchaseIntentResponse:
        del user_id, reason_code
        assert purchase_intent_id == self.intent.id
        # Deliberately persist only the public provider status. The production
        # store follows the same boundary and never receives a credential field.
        self.persisted_sensitive_credential = False
        self.intent = self.intent.model_copy(
            update={"state": state, "provider_status": result.status.value}
        )
        return self.intent

    async def begin_reconciliation(
        self,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        del user_id
        assert purchase_intent_id == self.intent.id
        assert self.intent.state == TransactionState.UNKNOWN
        self.intent = self.intent.model_copy(update={"state": TransactionState.RECONCILING})
        return self.intent


class FakePrava:
    def __init__(self) -> None:
        self.requests: list[PravaSessionRequest] = []
        self.create_error: PravaGatewayError | None = None
        self.payment_result: PravaPaymentResult | None = None

    async def create_session(self, request: PravaSessionRequest) -> HostedPravaSession:
        self.requests.append(request)
        if self.create_error is not None:
            raise self.create_error
        return HostedPravaSession(
            session_id="session-1",
            hosted_url="https://sandbox.collect.prava.space/checkout?session=1",
            order_id="order-1",
            expires_at=datetime(2099, 8, 1, 16, 0, tzinfo=UTC),
            response_id="response-create-1",
        )

    async def get_payment_result(self, session_id: str) -> PravaPaymentResult:
        assert session_id == "session-1"
        if self.payment_result is None:
            raise AssertionError("payment result was not configured")
        return self.payment_result


class StaticAuth:
    def __init__(self, user: AuthenticatedUser) -> None:
        self.user = user

    async def create_challenge(self) -> ChallengeResponse:
        raise NotImplementedError

    async def exchange(self, request: GoogleExchangeRequest) -> SessionResponse:
        del request
        raise NotImplementedError

    async def authenticate(self, token: str) -> AuthenticatedUser:
        if token != "valid-session":
            raise ApiError(
                status_code=401,
                code="AUTHENTICATION_REQUIRED",
                message="Sign in again to continue.",
                recoverable=True,
            )
        return self.user

    async def logout(self, token: str) -> None:
        del token


def _user() -> AuthenticatedUser:
    return AuthenticatedUser(
        id=uuid.uuid4(),
        email="talia@example.com",
        display_name="Talia",
        picture_url=None,
    )


def _intent(
    *,
    state: TransactionState = TransactionState.READY_FOR_APPROVAL,
    approval_session: ApprovalSessionResponse | None = None,
) -> PurchaseIntentResponse:
    now = datetime(2026, 8, 1, 15, 0, tzinfo=UTC)
    return PurchaseIntentResponse(
        id=uuid.uuid4(),
        recipient_id=uuid.uuid4(),
        occasion_id=uuid.uuid4(),
        candidate_id=uuid.uuid4(),
        state=state,
        merchant_id="hyperx-us",
        merchant_name="HyperX US",
        merchant_url="https://hyperx.com",
        merchant_product_id="product-1",
        merchant_variant_id="variant-1",
        sku="SKU-1",
        title="Observed headset",
        variant_title="Black",
        item_price_minor=6499,
        currency="USD",
        approved_total_minor=6999,
        quote_source="BROWSER_HARNESS",
        quote_timestamp=now,
        quote_expires_at=datetime(2099, 8, 1, 15, 15, tzinfo=UTC),
        delivery_summary="Merchant checkout delivery option",
        approval_session=approval_session,
        provider_status="pending" if approval_session else None,
        created_at=now,
        updated_at=now,
    )


def _payment_result(status: PravaPaymentStatus) -> PravaPaymentResult:
    credential = (
        SensitivePaymentCredential(
            token=SecretStr("4111111111111111"),
            dynamic_cvv=SecretStr("123"),
            expiry_month=SecretStr("12"),
            expiry_year=SecretStr("2030"),
        )
        if status is PravaPaymentStatus.AWAITING_RESULT
        else None
    )
    return PravaPaymentResult(
        session_id="session-1",
        order_id="order-1",
        status=status,
        transactions=[
            PravaTransactionResult(
                txn_id="transaction-1",
                status=status,
                line_items=[
                    PravaLineItemResult(
                        txn_ref_id="line-1",
                        merchant_name="HyperX US",
                        merchant_url="https://hyperx.com",
                        total_minor=6999,
                        status=status,
                        credential=credential,
                    )
                ],
                error_code=None,
            )
        ],
        response_id="response-poll-1",
    )


async def test_duplicate_session_tap_replays_without_second_provider_call() -> None:
    user = _user()
    store = MemoryPurchaseStore(_intent())
    prava = FakePrava()
    service = PurchaseService(
        store=store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
    )

    first = await service.create_prava_session(user, store.intent.id, "stable-key-123")
    replay = await service.create_prava_session(user, store.intent.id, "stable-key-123")

    assert len(prava.requests) == 1
    request = prava.requests[0]
    assert request.total_minor == 6999
    assert request.product_unit_minor == 6499
    assert request.callback_url.endswith(f"purchase_intent_id={store.intent.id}")
    assert first.state == TransactionState.AWAITING_USER
    assert replay.approval_session == first.approval_session


async def test_session_timeout_becomes_unknown_and_cannot_retry() -> None:
    user = _user()
    store = MemoryPurchaseStore(_intent())
    prava = FakePrava()
    prava.create_error = PravaGatewayError(
        "PRAVA_OUTCOME_UNKNOWN",
        "Prava did not confirm the operation.",
        recoverable=True,
        outcome_unknown=True,
    )
    service = PurchaseService(
        store=store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
    )

    with pytest.raises(ApiError) as first:
        await service.create_prava_session(user, store.intent.id, "stable-key-123")
    assert first.value.code == "PRAVA_OUTCOME_UNKNOWN"
    assert store.intent.state == TransactionState.UNKNOWN

    with pytest.raises(ApiError) as retry:
        await service.create_prava_session(user, store.intent.id, "stable-key-123")
    assert retry.value.code == "TRANSACTION_UNKNOWN"
    assert len(prava.requests) == 1


async def test_session_requires_https_callback_and_fresh_quote() -> None:
    user = _user()
    prava = FakePrava()
    insecure_store = MemoryPurchaseStore(_intent())
    insecure = PurchaseService(
        store=insecure_store,
        prava=prava,
        public_base_url="http://127.0.0.1:8000",
    )
    with pytest.raises(ApiError) as callback_error:
        await insecure.create_prava_session(
            user,
            insecure_store.intent.id,
            "stable-key-123",
        )
    assert callback_error.value.code == "PUBLIC_CALLBACK_UNAVAILABLE"

    draft_store = MemoryPurchaseStore(_intent(state=TransactionState.DRAFT))
    service = PurchaseService(
        store=draft_store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
    )
    with pytest.raises(ApiError) as quote_error:
        await service.create_prava_session(user, draft_store.intent.id, "stable-key-123")
    assert quote_error.value.code == "FRESH_QUOTE_REQUIRED"
    assert not prava.requests


async def test_reconcile_exposes_state_but_never_persists_credentials() -> None:
    user = _user()
    approval = ApprovalSessionResponse(
        session_id="session-1",
        hosted_url="https://sandbox.collect.prava.space/checkout?session=1",
        expires_at=datetime(2099, 8, 1, 16, 0, tzinfo=UTC),
    )
    store = MemoryPurchaseStore(
        _intent(state=TransactionState.AWAITING_USER, approval_session=approval)
    )
    prava = FakePrava()
    prava.payment_result = _payment_result(PravaPaymentStatus.AWAITING_RESULT)
    service = PurchaseService(
        store=store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
    )

    response = await service.reconcile(user, store.intent.id)

    assert response.state == TransactionState.CREDENTIALS_READY
    assert response.provider_status == "awaiting_result"
    assert "4111111111111111" not in response.model_dump_json()
    assert store.persisted_sensitive_credential is False


async def test_reconcile_rejects_credentials_for_different_approved_total() -> None:
    user = _user()
    approval = ApprovalSessionResponse(
        session_id="session-1",
        hosted_url="https://sandbox.collect.prava.space/checkout?session=1",
        expires_at=datetime(2099, 8, 1, 16, 0, tzinfo=UTC),
    )
    store = MemoryPurchaseStore(
        _intent(state=TransactionState.AWAITING_USER, approval_session=approval)
    )
    prava = FakePrava()
    result = _payment_result(PravaPaymentStatus.AWAITING_RESULT)
    transaction = result.transactions[0]
    line_item = transaction.line_items[0].model_copy(update={"total_minor": 7000})
    prava.payment_result = result.model_copy(
        update={
            "transactions": [
                transaction.model_copy(update={"line_items": [line_item]})
            ]
        }
    )
    service = PurchaseService(
        store=store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
    )

    with pytest.raises(ApiError) as captured:
        await service.reconcile(user, store.intent.id)

    assert captured.value.code == "PRAVA_RESPONSE_MISMATCH"
    assert store.intent.state == TransactionState.AWAITING_USER


async def test_completed_prava_result_cannot_claim_unverified_order() -> None:
    user = _user()
    approval = ApprovalSessionResponse(
        session_id="session-1",
        hosted_url="https://sandbox.collect.prava.space/checkout?session=1",
        expires_at=datetime(2099, 8, 1, 16, 0, tzinfo=UTC),
    )
    store = MemoryPurchaseStore(
        _intent(state=TransactionState.AWAITING_USER, approval_session=approval)
    )
    prava = FakePrava()
    prava.payment_result = _payment_result(PravaPaymentStatus.COMPLETED)
    service = PurchaseService(
        store=store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
    )

    with pytest.raises(ApiError) as captured:
        await service.reconcile(user, store.intent.id)

    assert captured.value.code == "ORDER_VERIFICATION_REQUIRED"
    assert store.intent.state == TransactionState.AWAITING_USER


def test_transaction_state_machine_rejects_unsafe_shortcuts() -> None:
    _require_transition(TransactionState.READY_FOR_APPROVAL, TransactionState.SESSION_CREATING)
    _require_transition(TransactionState.SESSION_CREATING, TransactionState.UNKNOWN)
    _require_transition(TransactionState.UNKNOWN, TransactionState.RECONCILING)

    with pytest.raises(ApiError):
        _require_transition(TransactionState.READY_FOR_APPROVAL, TransactionState.SUCCEEDED)
    with pytest.raises(ApiError):
        _require_transition(TransactionState.CREDENTIALS_READY, TransactionState.SUCCEEDED)


async def test_purchase_routes_require_auth_idempotency_and_safe_return() -> None:
    user = _user()
    store = MemoryPurchaseStore(_intent())
    prava = FakePrava()
    service = PurchaseService(
        store=store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
    )
    settings = Settings(
        app_env="test",
        database_url=SecretStr(
            "postgresql://wishtrace:password@database.invalid:5432/wishtrace?sslmode=require"
        ),
        public_base_url="https://api.wishtrace.example",
    )

    async def healthy_database() -> DatabaseProbe:
        return DatabaseProbe(connected=True, tls=True, server_version="17.0")

    app = create_app(
        settings=settings,
        database_probe=healthy_database,
        auth_operations=StaticAuth(user),
        purchase_operations=service,
    )
    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://testserver",
        follow_redirects=False,
    ) as client:
        path = f"/v1/purchase-intents/{store.intent.id}/prava-session"
        unauthenticated = await client.post(path)
        assert unauthenticated.status_code == 401

        missing_key = await client.post(
            path,
            headers={"Authorization": "Bearer valid-session"},
        )
        assert missing_key.status_code == 400
        assert missing_key.json()["code"] == "IDEMPOTENCY_KEY_INVALID"

        created = await client.post(
            path,
            headers={
                "Authorization": "Bearer valid-session",
                "Idempotency-Key": "stable-key-123",
            },
        )
        assert created.status_code == 200
        assert created.json()["state"] == "AWAITING_USER"

        status = await client.get(
            f"/v1/purchase-intents/{store.intent.id}/status",
            headers={"Authorization": "Bearer valid-session"},
        )
        assert status.status_code == 200
        assert status.json()["state"] == "AWAITING_USER"

        callback = await client.get(
            f"/v1/prava/return?purchase_intent_id={store.intent.id}"
        )
        assert callback.status_code == 302
        assert callback.headers["location"] == (
            f"wishtrace://prava/return?purchase_intent_id={store.intent.id}"
        )
