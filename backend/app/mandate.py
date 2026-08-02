"""Occasion-scoped standing spend authorizations (Prava mandates).

WishTrace's autopilot: the owner approves a spend cap **once** with a passkey,
and WishTrace can then charge within that cap when a moment nears — no second
approval. This module owns the mandate lifecycle end to end:

    setup  → Prava mandate_setup session (owner approves once, ``pending``)
    refresh→ poll Prava until the mandate is ``active``
    execute→ charge within cap → mint one-time card (memory only) →
             merchant checkout (Playwright) → report outcome → settle

Card credentials minted per charge are **never** persisted or serialized; they
live in backend memory for the length of a single checkout only, exactly like
the one-time purchase path in :mod:`app.purchase`.
"""

import hashlib
import hmac
import json
import logging
import re
import uuid
from datetime import UTC, datetime, timedelta
from decimal import Decimal, InvalidOperation
from enum import StrEnum
from typing import Literal, Protocol

from pydantic import BaseModel, ConfigDict
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.auth import AuthenticatedUser
from app.errors import ApiError
from app.merchant_browser import (
    BillingContact,
    MerchantBrowserError,
    MerchantCheckoutGateway,
    MerchantCheckoutOutcome,
    MerchantCheckoutResult,
    MerchantQuoteRequest,
)
from app.models import (
    CandidateSnapshotModel,
    DiscoveryRunModel,
    MandateChargeModel,
    MandateModel,
    OccasionModel,
)
from app.prava import (
    HostedPravaSession,
    PravaGatewayError,
    PravaMandateChargeResult,
    PravaMandateFrequency,
    PravaMandateInfo,
    PravaMandateReportResult,
    PravaMandateScope,
    PravaMandateSessionRequest,
    PravaMandateStatus,
    PravaPaymentResult,
    PravaPaymentStatus,
    PravaReportOutcome,
    SensitivePaymentCredential,
)

IDEMPOTENCY_PATTERN = re.compile(r"^[A-Za-z0-9._:-]{8,255}$")
# One-time approvals must expire quickly; recurring ones ride a longer horizon.
_ONE_TIME_VALID_DAYS = 7
_RECURRING_VALID_DAYS = 400
_RECURRING_MAX_CHARGES = 5
logger = logging.getLogger("wishtrace")


class MandateState(StrEnum):
    SETUP_CREATING = "SETUP_CREATING"
    AWAITING_APPROVAL = "AWAITING_APPROVAL"
    ACTIVE = "ACTIVE"
    CHARGING = "CHARGING"
    CHECKOUT_IN_PROGRESS = "CHECKOUT_IN_PROGRESS"
    REPORTING = "REPORTING"
    SUCCEEDED = "SUCCEEDED"
    DECLINED = "DECLINED"
    CONSUMED = "CONSUMED"
    PAUSED = "PAUSED"
    CANCELLED = "CANCELLED"
    EXPIRED = "EXPIRED"
    FAILED = "FAILED"
    UNKNOWN = "UNKNOWN"


class MandateChargeState(StrEnum):
    CHARGING = "CHARGING"
    CHECKOUT_IN_PROGRESS = "CHECKOUT_IN_PROGRESS"
    REPORTING = "REPORTING"
    SUCCEEDED = "SUCCEEDED"
    DECLINED = "DECLINED"
    FAILED = "FAILED"
    UNKNOWN = "UNKNOWN"


_TERMINAL_MANDATE_STATES = {
    MandateState.CONSUMED,
    MandateState.CANCELLED,
    MandateState.EXPIRED,
    MandateState.FAILED,
}

class MandateSetupRequest(BaseModel):
    """Yellow-tile input: which gift the autopilot should stand ready to buy.

    ``candidate_id`` names the discovered, checkout-verified product the mandate
    is armed against; budget and frequency come from the occasion itself.
    """

    model_config = ConfigDict(extra="forbid", frozen=True)

    candidate_id: uuid.UUID


class MandateExecuteRequest(BaseModel):
    """Execute input: verified billing for the merchant checkout attempt."""

    model_config = ConfigDict(extra="forbid", frozen=True)

    billing: BillingContact


class MandateChargeView(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    id: uuid.UUID
    reference: str
    amount_minor: int
    state: MandateChargeState
    merchant_order_id: str | None
    merchant_outcome: MerchantCheckoutOutcome | None
    visa_confirmation: Literal["SUCCESS", "FAILURE"] | None
    created_at: datetime
    updated_at: datetime


class MandateApprovalSession(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    session_id: str
    hosted_url: str
    expires_at: datetime | None


class MandateResponse(BaseModel):
    """Owner-facing mandate view. Never carries card credentials."""

    model_config = ConfigDict(extra="forbid", frozen=True)

    id: uuid.UUID
    recipient_id: uuid.UUID
    occasion_id: uuid.UUID
    state: MandateState
    approved_amount_minor: int
    currency: Literal["USD"]
    recurring_frequency: PravaMandateFrequency
    merchant_scope: PravaMandateScope
    max_charges: int
    charges_used: int
    valid_until: datetime | None
    merchant_id: str
    merchant_name: str
    merchant_url: str
    merchant_product_id: str
    merchant_variant_id: str
    product_title: str
    item_price_minor: int
    provider_mandate_id: str | None
    provider_status: str | None
    setup_failure_code: str | None
    merchant_order_id: str | None
    merchant_outcome: MerchantCheckoutOutcome | None
    visa_confirmation: Literal["SUCCESS", "FAILURE"] | None
    approval_session: MandateApprovalSession | None
    charges: list[MandateChargeView]
    created_at: datetime
    updated_at: datetime


class MandateStore(Protocol):
    async def create_setup(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        candidate_id: uuid.UUID,
    ) -> "MandateSetupFacts": ...

    async def complete_setup(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        session: HostedPravaSession,
    ) -> MandateResponse: ...

    async def fail_setup(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        unknown: bool,
        response_id: str | None,
        provider_status: str | None = None,
        failure_code: str | None = None,
    ) -> MandateResponse: ...

    async def get(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
    ) -> MandateResponse: ...

    async def record_activation(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        info: PravaMandateInfo,
    ) -> MandateResponse: ...


class MandatePravaOperations(Protocol):
    async def create_mandate_session(
        self, request: PravaMandateSessionRequest
    ) -> HostedPravaSession: ...

    async def get_mandate(self, mandate_id: str) -> PravaMandateInfo: ...

    async def list_mandates(self, customer_id: str) -> list[PravaMandateInfo]: ...

    async def get_payment_result(self, session_id: str) -> PravaPaymentResult: ...

    async def charge_mandate(
        self,
        *,
        mandate_id: str,
        amount_minor: int,
        reference: str,
    ) -> PravaMandateChargeResult: ...

    async def report_mandate_charge(
        self,
        *,
        mandate_id: str,
        charge_id: str,
        outcome: PravaReportOutcome,
    ) -> PravaMandateReportResult: ...


class MandateSetupFacts(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    setup_id: uuid.UUID
    recurring_frequency: PravaMandateFrequency
    merchant_scope: PravaMandateScope
    max_charges: int
    approved_amount_minor: int
    product_title: str
    item_price_minor: int


class ChargeClaim(BaseModel):
    """Result of claiming a charge slot: a fresh charge id, or a replay."""

    model_config = ConfigDict(extra="forbid", frozen=True)

    id: uuid.UUID
    replayed: bool


class ChargeStore(Protocol):
    async def begin_charge(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        reference: str,
        amount_minor: int,
    ) -> ChargeClaim: ...

    async def fail_charge(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        reason_code: str,
        outcome_unknown: bool,
    ) -> MandateResponse: ...

    async def record_charge_declined(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        provider_charge_id: str,
        error_code: str,
    ) -> MandateResponse: ...

    async def begin_charge_checkout(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        provider_charge_id: str,
    ) -> MandateResponse: ...

    async def record_charge_checkout(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        result: MerchantCheckoutResult,
    ) -> MandateResponse: ...

    async def mark_charge_unknown(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        reason_code: str,
        response_id: str | None,
    ) -> MandateResponse: ...

    async def settle_charge(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        merchant_outcome: MerchantCheckoutOutcome,
        visa_confirmation: Literal["SUCCESS", "FAILURE"],
        response_id: str | None,
        recurring_frequency: PravaMandateFrequency,
    ) -> MandateResponse: ...


class MandateChargeStore(MandateStore, ChargeStore, Protocol):
    """The full persistence surface the service needs: setup + charge lifecycle.

    ``SqlMandateStore`` implements both halves; the service holds one object and
    drives it through the whole mandate/charge lifecycle.
    """


class MandateOperations(Protocol):
    async def setup(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
        body: MandateSetupRequest,
    ) -> MandateResponse: ...

    async def get(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
    ) -> MandateResponse: ...

    async def refresh(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
    ) -> MandateResponse: ...

    async def execute(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
        body: MandateExecuteRequest,
        idempotency_key: str,
    ) -> MandateResponse: ...


class MandateService:
    def __init__(
        self,
        *,
        store: MandateChargeStore,
        prava: MandatePravaOperations,
        public_base_url: str,
        merchant_checkout: MerchantCheckoutGateway | None = None,
        idempotency_pepper: str | None = None,
    ) -> None:
        self._store = store
        self._prava = prava
        self._public_base_url = public_base_url.rstrip("/")
        self._merchant_checkout = merchant_checkout
        self._idempotency_pepper = (
            idempotency_pepper.encode() if idempotency_pepper is not None else None
        )

    async def get(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
    ) -> MandateResponse:
        return await self._store.get(user_id=user.id, occasion_id=occasion_id)

    async def setup(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
        body: MandateSetupRequest,
    ) -> MandateResponse:
        facts = await self._store.create_setup(
            user_id=user.id,
            occasion_id=occasion_id,
            candidate_id=body.candidate_id,
        )
        request = self._setup_request(user, occasion_id, facts)
        try:
            session = await self._prava.create_mandate_session(request)
        except PravaGatewayError as error:
            logger.warning(
                "prava_mandate_setup_failed",
                extra={
                    "error_category": (
                        f"{error.code}:{error.provider_code or 'NO_PROVIDER_CODE'}"
                    )
                },
            )
            await self._store.fail_setup(
                user_id=user.id,
                occasion_id=occasion_id,
                unknown=error.outcome_unknown,
                response_id=error.response_id,
                failure_code=_safe_provider_code(error.provider_code or error.code),
            )
            raise ApiError(
                status_code=503 if error.recoverable else 502,
                code=error.code,
                message=error.safe_message,
                recoverable=error.recoverable,
            ) from error
        return await self._store.complete_setup(
            user_id=user.id,
            occasion_id=occasion_id,
            session=session,
        )

    async def refresh(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
    ) -> MandateResponse:
        mandate = await self._store.get(user_id=user.id, occasion_id=occasion_id)
        if mandate.state in _TERMINAL_MANDATE_STATES:
            return mandate
        try:
            if mandate.provider_mandate_id is None:
                candidates = await self._prava.list_mandates(str(user.id))
                matches = [
                    candidate
                    for candidate in candidates
                    if _matches_local_mandate(candidate, mandate, user.id)
                ]
                if not matches:
                    approval = mandate.approval_session
                    if approval is None:
                        return mandate
                    result = await self._prava.get_payment_result(approval.session_id)
                    if result.session_id != approval.session_id:
                        raise ApiError(
                            status_code=502,
                            code="PRAVA_RESPONSE_INVALID",
                            message="Prava returned a mismatched approval result.",
                            recoverable=False,
                        )
                    if result.status is PravaPaymentStatus.FAILED:
                        provider_code = next(
                            (
                                transaction.error_code
                                for transaction in result.transactions
                                if transaction.error_code
                            ),
                            "NO_PROVIDER_CODE",
                        )
                        logger.warning(
                            "prava_mandate_hosted_setup_failed",
                            extra={"error_category": _safe_provider_code(provider_code)},
                        )
                        return await self._store.fail_setup(
                            user_id=user.id,
                            occasion_id=occasion_id,
                            unknown=False,
                            response_id=result.response_id,
                            provider_status=result.status.value,
                            failure_code=_safe_provider_code(provider_code),
                        )
                    if result.status in {
                        PravaPaymentStatus.AWAITING_RESULT,
                        PravaPaymentStatus.COMPLETED,
                    }:
                        logger.warning(
                            "prava_mandate_setup_missing_after_provider_result",
                            extra={"error_category": result.status.value},
                        )
                        return await self._store.fail_setup(
                            user_id=user.id,
                            occasion_id=occasion_id,
                            unknown=True,
                            response_id=result.response_id,
                            provider_status=result.status.value,
                            failure_code="MANDATE_NOT_FOUND_AFTER_APPROVAL",
                        )
                    return mandate
                if len(matches) > 1:
                    raise ApiError(
                        status_code=502,
                        code="PRAVA_RESPONSE_AMBIGUOUS",
                        message=(
                            "Prava returned more than one matching approval. "
                            "WishTrace will not choose one automatically."
                        ),
                        recoverable=False,
                    )
                info = matches[0]
            else:
                info = await self._prava.get_mandate(mandate.provider_mandate_id)
        except PravaGatewayError as error:
            raise ApiError(
                status_code=503 if error.recoverable else 502,
                code=error.code,
                message=error.safe_message,
                recoverable=error.recoverable,
            ) from error
        if (
            mandate.provider_mandate_id is not None
            and info.mandate_id != mandate.provider_mandate_id
        ):
            raise ApiError(
                status_code=502,
                code="PRAVA_RESPONSE_INVALID",
                message="Prava returned a mismatched mandate.",
                recoverable=False,
            )
        return await self._store.record_activation(
            user_id=user.id,
            occasion_id=occasion_id,
            info=info,
        )

    async def execute(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
        body: MandateExecuteRequest,
        idempotency_key: str,
    ) -> MandateResponse:
        if user.email is None:
            raise ApiError(
                status_code=409,
                code="VERIFIED_EMAIL_REQUIRED",
                message="A verified Google email is required for digital delivery.",
                recoverable=True,
            )
        if body.billing.email.casefold() != user.email.casefold():
            raise ApiError(
                status_code=409,
                code="CHECKOUT_EMAIL_MISMATCH",
                message=(
                    "Use your verified sign-in email for checkout; Jackbox sends "
                    "the gift card there for manual forwarding."
                ),
                recoverable=True,
            )
        if self._merchant_checkout is None:
            raise ApiError(
                status_code=503,
                code="MERCHANT_CHECKOUT_UNAVAILABLE",
                message="Live merchant checkout is not configured yet.",
                recoverable=True,
            )
        if not IDEMPOTENCY_PATTERN.fullmatch(idempotency_key):
            raise ApiError(
                status_code=400,
                code="IDEMPOTENCY_KEY_INVALID",
                message="A stable idempotency key is required to charge the mandate.",
                recoverable=True,
            )
        mandate = await self._store.get(user_id=user.id, occasion_id=occasion_id)
        if mandate.provider_mandate_id is None or mandate.state is not MandateState.ACTIVE:
            raise ApiError(
                status_code=409,
                code="MANDATE_NOT_ACTIVE",
                message="This mandate is not active. Refresh its status before charging.",
                recoverable=True,
            )
        reference = self._charge_reference(occasion_id, idempotency_key)
        charge = await self._store.begin_charge(
            user_id=user.id,
            occasion_id=occasion_id,
            reference=reference,
            amount_minor=mandate.approved_amount_minor,
        )
        if charge.replayed:
            return await self._store.get(user_id=user.id, occasion_id=occasion_id)
        assert self._merchant_checkout is not None
        # Open (and validate) the merchant quote first so the browser session is
        # ready; only then mint the single-use card so it lives the shortest time.
        try:
            await self._merchant_checkout.quote(
                MerchantQuoteRequest(
                    purchase_intent_id=charge.id,
                    product_url=mandate.merchant_url,
                    merchant_variant_id=mandate.merchant_variant_id,
                    expected_item_minor=mandate.item_price_minor,
                    currency="USD",
                    billing=body.billing,
                )
            )
        except MerchantBrowserError as error:
            await self._store.fail_charge(
                user_id=user.id,
                occasion_id=occasion_id,
                charge_id=charge.id,
                reason_code=error.code,
                outcome_unknown=error.outcome_unknown,
            )
            raise ApiError(
                status_code=503 if error.recoverable else 502,
                code=error.code,
                message=error.safe_message,
                recoverable=error.recoverable,
            ) from error
        try:
            charge_result = await self._prava.charge_mandate(
                mandate_id=mandate.provider_mandate_id,
                amount_minor=mandate.approved_amount_minor,
                reference=reference,
            )
        except PravaGatewayError as error:
            await self._store.fail_charge(
                user_id=user.id,
                occasion_id=occasion_id,
                charge_id=charge.id,
                reason_code="PRAVA_CHARGE_OUTCOME_UNKNOWN"
                if error.outcome_unknown
                else error.code,
                outcome_unknown=error.outcome_unknown,
            )
            raise ApiError(
                status_code=503 if error.recoverable else 502,
                code=error.code,
                message=error.safe_message,
                recoverable=error.recoverable,
            ) from error
        if charge_result.mandate_id != mandate.provider_mandate_id:
            await self._store.mark_charge_unknown(
                user_id=user.id,
                occasion_id=occasion_id,
                charge_id=charge.id,
                reason_code="PRAVA_CHARGE_MANDATE_MISMATCH",
                response_id=None,
            )
            raise ApiError(
                status_code=502,
                code="PRAVA_RESPONSE_MISMATCH",
                message="Prava returned a charge for a different mandate.",
                recoverable=False,
            )
        if charge_result.status != "awaiting_result" or charge_result.credential is None:
            return await self._store.record_charge_declined(
                user_id=user.id,
                occasion_id=occasion_id,
                charge_id=charge.id,
                provider_charge_id=charge_result.charge_id,
                error_code=charge_result.error_code or "PRAVA_CHARGE_DECLINED",
            )
        credential: SensitivePaymentCredential = charge_result.credential
        merchant_result = await self._run_merchant_checkout(
            user=user,
            occasion_id=occasion_id,
            charge=charge,
            provider_charge_id=charge_result.charge_id,
            credential=credential,
        )
        if merchant_result.outcome is MerchantCheckoutOutcome.UNKNOWN:
            return await self._store.get(user_id=user.id, occasion_id=occasion_id)
        return await self._report_charge(
            user=user,
            occasion_id=occasion_id,
            charge_id=charge.id,
            provider_mandate_id=mandate.provider_mandate_id,
            provider_charge_id=charge_result.charge_id,
            recurring_frequency=mandate.recurring_frequency,
            merchant_outcome=merchant_result.outcome,
        )

    async def _run_merchant_checkout(
        self,
        *,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
        charge: "ChargeClaim",
        provider_charge_id: str,
        credential: SensitivePaymentCredential,
    ) -> MerchantCheckoutResult:
        assert self._merchant_checkout is not None
        await self._store.begin_charge_checkout(
            user_id=user.id,
            occasion_id=occasion_id,
            charge_id=charge.id,
            provider_charge_id=provider_charge_id,
        )
        try:
            result = await self._merchant_checkout.checkout(
                purchase_intent_id=charge.id,
                credential=credential,
            )
        except MerchantBrowserError as error:
            await self._store.fail_charge(
                user_id=user.id,
                occasion_id=occasion_id,
                charge_id=charge.id,
                reason_code=error.code,
                outcome_unknown=error.outcome_unknown,
            )
            raise ApiError(
                status_code=503 if error.recoverable else 502,
                code=error.code,
                message=error.safe_message,
                recoverable=error.recoverable,
            ) from error
        await self._store.record_charge_checkout(
            user_id=user.id,
            occasion_id=occasion_id,
            charge_id=charge.id,
            result=result,
        )
        return result

    async def _report_charge(
        self,
        *,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        provider_mandate_id: str,
        provider_charge_id: str,
        recurring_frequency: PravaMandateFrequency,
        merchant_outcome: MerchantCheckoutOutcome,
    ) -> MandateResponse:
        report_outcome = (
            PravaReportOutcome.APPROVED
            if merchant_outcome is MerchantCheckoutOutcome.ORDER_VERIFIED
            else PravaReportOutcome.DECLINED
        )
        try:
            report = await self._prava.report_mandate_charge(
                mandate_id=provider_mandate_id,
                charge_id=provider_charge_id,
                outcome=report_outcome,
            )
        except PravaGatewayError as error:
            await self._store.mark_charge_unknown(
                user_id=user.id,
                occasion_id=occasion_id,
                charge_id=charge_id,
                reason_code="PRAVA_REPORT_OUTCOME_UNKNOWN",
                response_id=error.response_id,
            )
            raise ApiError(
                status_code=503 if error.recoverable else 502,
                code=error.code,
                message=error.safe_message,
                recoverable=error.recoverable,
            ) from error
        expected_confirmation = (
            "SUCCESS" if report_outcome is PravaReportOutcome.APPROVED else "FAILURE"
        )
        expected_status = (
            "completed" if report_outcome is PravaReportOutcome.APPROVED else "failed"
        )
        if (
            report.mandate_id != provider_mandate_id
            or report.charge_id != provider_charge_id
            or report.status != expected_status
            or report.visa_confirmation != expected_confirmation
        ):
            await self._store.mark_charge_unknown(
                user_id=user.id,
                occasion_id=occasion_id,
                charge_id=charge_id,
                reason_code="PRAVA_REPORT_MISMATCH",
                response_id=report.response_id,
            )
            raise ApiError(
                status_code=502,
                code="PRAVA_RESPONSE_MISMATCH",
                message="Prava confirmed a different mandate charge outcome.",
                recoverable=False,
            )
        return await self._store.settle_charge(
            user_id=user.id,
            occasion_id=occasion_id,
            charge_id=charge_id,
            merchant_outcome=merchant_outcome,
            visa_confirmation=report.visa_confirmation,
            response_id=report.response_id,
            recurring_frequency=recurring_frequency,
        )

    def _setup_request(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
        facts: MandateSetupFacts,
    ) -> PravaMandateSessionRequest:
        if user.email is None:
            raise ApiError(
                status_code=409,
                code="VERIFIED_EMAIL_REQUIRED",
                message="A verified Google email is required for Prava approval.",
                recoverable=True,
            )
        now = datetime.now(UTC)
        if facts.recurring_frequency is PravaMandateFrequency.ONE_TIME:
            valid_until = now + timedelta(days=_ONE_TIME_VALID_DAYS)
        else:
            valid_until = now + timedelta(days=_RECURRING_VALID_DAYS)
        callback_url = (
            f"{self._public_base_url}/v1/prava/mandate-return"
            f"?occasion_id={occasion_id}"
        )
        return PravaMandateSessionRequest(
            user_id=str(user.id),
            user_email=user.email,
            total_minor=facts.approved_amount_minor,
            currency="USD",
            merchant_name="Jackbox Games",
            merchant_url="https://checkout.jackboxgames.com",
            merchant_country="US",
            product_description=facts.product_title,
            product_unit_minor=facts.item_price_minor,
            external_product_id=str(occasion_id),
            quantity=1,
            callback_url=callback_url,
            external_order_ref=f"mandate-{facts.setup_id.hex}",
            recurring_frequency=facts.recurring_frequency,
            merchant_scope=facts.merchant_scope,
            max_charges=facts.max_charges,
            valid_until=valid_until.strftime("%Y-%m-%dT%H:%M:%SZ"),
        )

    def _charge_reference(self, occasion_id: uuid.UUID, idempotency_key: str) -> str:
        if self._idempotency_pepper is None:
            digest = hashlib.sha256(idempotency_key.encode()).hexdigest()
        else:
            digest = _keyed_hash(
                self._idempotency_pepper,
                "mandate-charge-reference",
                idempotency_key.encode(),
            ).hex()
        return f"occ-{occasion_id.hex}-{digest[:24]}"


class SqlMandateStore:
    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def create_setup(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        candidate_id: uuid.UUID,
    ) -> MandateSetupFacts:
        async with self._session_factory() as session, session.begin():
            occasion = await session.scalar(
                select(OccasionModel)
                .where(
                    OccasionModel.id == occasion_id,
                    OccasionModel.user_id == user_id,
                )
                .with_for_update()
            )
            if occasion is None:
                raise _not_found("OCCASION_NOT_FOUND", "That occasion was not found.")
            existing = await session.scalar(
                select(MandateModel)
                .where(MandateModel.occasion_id == occasion_id)
                .with_for_update()
            )
            if existing is not None and MandateState(existing.state) not in {
                MandateState.FAILED,
                MandateState.CANCELLED,
                MandateState.EXPIRED,
            }:
                raise ApiError(
                    status_code=409,
                    code="MANDATE_ALREADY_EXISTS",
                    message="This occasion already has a mandate. Refresh its status.",
                    recoverable=True,
                )
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
                        DiscoveryRunModel.occasion_id == occasion_id,
                    )
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
            if candidate.price_minor > occasion.budget_minor:
                raise ApiError(
                    status_code=409,
                    code="CANDIDATE_OVER_BUDGET",
                    message="This gift now exceeds the saved budget.",
                    recoverable=True,
                )
            frequency = (
                PravaMandateFrequency.YEARLY
                if occasion.recurring_frequency == "yearly"
                else PravaMandateFrequency.ONE_TIME
            )
            # One-time approvals bind to the listed merchant with a single charge;
            # recurring approvals allow a small, bounded number of yearly charges.
            scope = PravaMandateScope.LISTED
            max_charges = (
                1
                if frequency is PravaMandateFrequency.ONE_TIME
                else _RECURRING_MAX_CHARGES
            )
            if existing is not None:
                await session.delete(existing)
                await session.flush()
            mandate = MandateModel(
                user_id=user_id,
                recipient_id=discovery.recipient_id,
                occasion_id=occasion_id,
                state=MandateState.SETUP_CREATING.value,
                approved_amount_minor=occasion.budget_minor,
                currency="USD",
                recurring_frequency=frequency.value,
                merchant_scope=scope.value,
                max_charges=max_charges,
                merchant_id=discovery.merchant_id,
                merchant_name=discovery.merchant_name,
                merchant_url=_origin(candidate.product_url),
                merchant_product_id=candidate.merchant_product_id,
                merchant_variant_id=candidate.merchant_variant_id,
                product_title=candidate.title,
                item_price_minor=candidate.price_minor,
            )
            session.add(mandate)
            await session.flush()
            return MandateSetupFacts(
                setup_id=mandate.id,
                recurring_frequency=frequency,
                merchant_scope=scope,
                max_charges=max_charges,
                approved_amount_minor=occasion.budget_minor,
                product_title=candidate.title,
                item_price_minor=candidate.price_minor,
            )

    async def complete_setup(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        session: HostedPravaSession,
    ) -> MandateResponse:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            if MandateState(mandate.state) is not MandateState.SETUP_CREATING:
                return await _mandate_response(db, mandate)
            mandate.setup_session_id = session.session_id
            mandate.setup_hosted_url = session.hosted_url
            mandate.setup_expires_at = session.expires_at
            mandate.setup_response_id = session.response_id
            mandate.provider_mandate_id = session.mandate_id
            mandate.provider_status = PravaMandateStatus.PENDING.value
            mandate.setup_failure_code = None
            mandate.last_response_id = session.response_id
            _transition(mandate, MandateState.AWAITING_APPROVAL)
            return await _flush_mandate_response(db, mandate)

    async def fail_setup(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        unknown: bool,
        response_id: str | None,
        provider_status: str | None = None,
        failure_code: str | None = None,
    ) -> MandateResponse:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            mandate.last_response_id = response_id
            if provider_status is not None:
                mandate.provider_status = provider_status
            mandate.setup_failure_code = failure_code
            _transition(
                mandate,
                MandateState.UNKNOWN if unknown else MandateState.FAILED,
            )
            return await _flush_mandate_response(db, mandate)

    async def get(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
    ) -> MandateResponse:
        async with self._session_factory() as db:
            mandate = await _owned_mandate(db, user_id, occasion_id)
            return await _mandate_response(db, mandate)

    async def record_activation(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        info: PravaMandateInfo,
    ) -> MandateResponse:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            if (
                mandate.provider_mandate_id is not None
                and mandate.provider_mandate_id != info.mandate_id
            ):
                raise ApiError(
                    status_code=502,
                    code="PRAVA_RESPONSE_MISMATCH",
                    message="Prava returned a mismatched mandate.",
                    recoverable=False,
                )
            mandate.provider_mandate_id = info.mandate_id
            mandate.provider_status = info.status.value
            mandate.setup_failure_code = None
            if mandate.valid_until is None:
                mandate.valid_until = info.valid_until
            target = _state_from_mandate_status(info.status, MandateState(mandate.state))
            if target is not MandateState(mandate.state):
                _transition(mandate, target)
            return await _flush_mandate_response(db, mandate)

    async def begin_charge(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        reference: str,
        amount_minor: int,
    ) -> ChargeClaim:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            existing = await db.scalar(
                select(MandateChargeModel)
                .where(
                    MandateChargeModel.mandate_id == mandate.id,
                    MandateChargeModel.reference == reference,
                )
                .with_for_update()
            )
            if existing is not None:
                # Idempotent replay: the same execute request already ran.
                return ChargeClaim(id=existing.id, replayed=True)
            if MandateState(mandate.state) is not MandateState.ACTIVE:
                raise ApiError(
                    status_code=409,
                    code="MANDATE_NOT_ACTIVE",
                    message="This mandate is not active. Refresh before charging.",
                    recoverable=True,
                )
            if mandate.charges_used >= mandate.max_charges:
                raise ApiError(
                    status_code=409,
                    code="MANDATE_CHARGES_EXHAUSTED",
                    message="This mandate has no remaining charges.",
                    recoverable=False,
                )
            charge = MandateChargeModel(
                mandate_id=mandate.id,
                reference=reference,
                amount_minor=amount_minor,
                state=MandateChargeState.CHARGING.value,
            )
            db.add(charge)
            _transition(mandate, MandateState.CHARGING)
            await db.flush()
            return ChargeClaim(id=charge.id, replayed=False)

    async def fail_charge(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        reason_code: str,
        outcome_unknown: bool,
    ) -> MandateResponse:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            charge = await _owned_charge(db, mandate.id, charge_id, lock=True)
            charge.provider_error_code = reason_code
            _set_charge_state(
                charge,
                MandateChargeState.UNKNOWN if outcome_unknown else MandateChargeState.FAILED,
            )
            _transition(
                mandate,
                MandateState.UNKNOWN if outcome_unknown else MandateState.FAILED,
            )
            return await _flush_mandate_response(db, mandate)

    async def record_charge_declined(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        provider_charge_id: str,
        error_code: str,
    ) -> MandateResponse:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            charge = await _owned_charge(db, mandate.id, charge_id, lock=True)
            charge.provider_charge_id = provider_charge_id
            charge.provider_error_code = error_code
            _set_charge_state(charge, MandateChargeState.DECLINED)
            _transition(mandate, MandateState.DECLINED)
            return await _flush_mandate_response(db, mandate)

    async def begin_charge_checkout(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        provider_charge_id: str,
    ) -> MandateResponse:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            charge = await _owned_charge(db, mandate.id, charge_id, lock=True)
            charge.provider_charge_id = provider_charge_id
            _set_charge_state(charge, MandateChargeState.CHECKOUT_IN_PROGRESS)
            _transition(mandate, MandateState.CHECKOUT_IN_PROGRESS)
            return await _flush_mandate_response(db, mandate)

    async def record_charge_checkout(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        result: MerchantCheckoutResult,
    ) -> MandateResponse:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            charge = await _owned_charge(db, mandate.id, charge_id, lock=True)
            if result.amount_minor != mandate.approved_amount_minor:
                raise ApiError(
                    status_code=502,
                    code="MERCHANT_RESULT_MISMATCH",
                    message="The merchant returned a result for a different total.",
                    recoverable=False,
                )
            charge.merchant_outcome = result.outcome.value
            charge.merchant_order_id = result.order_id
            if result.outcome is MerchantCheckoutOutcome.ORDER_VERIFIED:
                _set_charge_state(charge, MandateChargeState.REPORTING)
                _transition(mandate, MandateState.REPORTING)
            elif result.outcome is MerchantCheckoutOutcome.DECLINED:
                # Merchant decline is known but not final until Prava confirms it.
                _set_charge_state(charge, MandateChargeState.REPORTING)
                _transition(mandate, MandateState.REPORTING)
            else:
                _set_charge_state(charge, MandateChargeState.UNKNOWN)
                _transition(mandate, MandateState.UNKNOWN)
            mandate.merchant_order_id = result.order_id
            mandate.merchant_outcome = result.outcome.value
            return await _flush_mandate_response(db, mandate)

    async def mark_charge_unknown(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        reason_code: str,
        response_id: str | None,
    ) -> MandateResponse:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            charge = await _owned_charge(db, mandate.id, charge_id, lock=True)
            charge.provider_error_code = reason_code
            charge.report_response_id = response_id
            _set_charge_state(charge, MandateChargeState.UNKNOWN)
            if MandateState(mandate.state) is not MandateState.UNKNOWN:
                _transition(mandate, MandateState.UNKNOWN)
            return await _flush_mandate_response(db, mandate)

    async def settle_charge(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        merchant_outcome: MerchantCheckoutOutcome,
        visa_confirmation: Literal["SUCCESS", "FAILURE"],
        response_id: str | None,
        recurring_frequency: PravaMandateFrequency,
    ) -> MandateResponse:
        async with self._session_factory() as db, db.begin():
            mandate = await _owned_mandate(db, user_id, occasion_id, lock=True)
            charge = await _owned_charge(db, mandate.id, charge_id, lock=True)
            charge.visa_confirmation = visa_confirmation
            charge.report_response_id = response_id
            mandate.visa_confirmation = visa_confirmation
            mandate.last_response_id = response_id
            mandate.charges_used = mandate.charges_used + 1
            if merchant_outcome is MerchantCheckoutOutcome.ORDER_VERIFIED:
                _set_charge_state(charge, MandateChargeState.SUCCEEDED)
                terminal = MandateState.SUCCEEDED
            else:
                _set_charge_state(charge, MandateChargeState.DECLINED)
                terminal = MandateState.DECLINED
            _transition(mandate, terminal)
            # One-time mandates are consumed after a single settled charge;
            # recurring ones return to ACTIVE until their charges are used up.
            if recurring_frequency is PravaMandateFrequency.ONE_TIME:
                _transition(mandate, MandateState.CONSUMED)
                mandate.provider_status = PravaMandateStatus.CONSUMED.value
            elif mandate.charges_used < mandate.max_charges:
                _transition(mandate, MandateState.ACTIVE)
            else:
                _transition(mandate, MandateState.CONSUMED)
                mandate.provider_status = PravaMandateStatus.CONSUMED.value
            return await _flush_mandate_response(db, mandate)


def _keyed_hash(pepper: bytes, purpose: str, value: bytes) -> bytes:
    mac = hmac.new(pepper, purpose.encode(), hashlib.sha256)
    mac.update(b"\x00")
    mac.update(value)
    return mac.digest()


def _origin(url: str) -> str:
    scheme, separator, rest = url.partition("://")
    if not separator:
        return url
    host = rest.split("/", 1)[0]
    return f"{scheme}://{host}"


def _not_found(code: str, message: str) -> ApiError:
    return ApiError(status_code=404, code=code, message=message, recoverable=False)


def _matches_local_mandate(
    candidate: PravaMandateInfo,
    local: MandateResponse,
    user_id: uuid.UUID,
) -> bool:
    """Bind a provider mandate only when every observable setup fact agrees.

    Prava's documented create-session response does not include the new mandate
    id, so the only supported association path is the customer-scoped mandate
    list. A timestamp floor prevents an older, otherwise identical mandate from
    being attached after a retry; multiple matches still fail closed in refresh.
    """

    try:
        approved_amount = Decimal(candidate.approved_amount)
    except InvalidOperation:
        return False
    expected_amount = Decimal(local.approved_amount_minor) / Decimal(100)
    created_floor = local.created_at - timedelta(minutes=2)
    return (
        candidate.created_at >= created_floor
        and approved_amount == expected_amount
        and candidate.currency == local.currency
        and candidate.recurring_frequency is local.recurring_frequency
        and candidate.merchant_scope is local.merchant_scope
        and (
            not candidate.merchant_name
            or candidate.merchant_name.casefold() == local.merchant_name.casefold()
        )
        and (
            candidate.external_user_id is None
            or candidate.external_user_id == str(user_id)
        )
    )


def _safe_provider_code(value: str) -> str:
    """Keep provider diagnostics useful without allowing arbitrary log content."""

    normalized = value.strip().upper()
    return normalized if re.fullmatch(r"[A-Z0-9_:-]{1,100}", normalized) else "UNKNOWN"


def _state_from_mandate_status(
    status: PravaMandateStatus,
    current: MandateState,
) -> MandateState:
    mapping = {
        PravaMandateStatus.PENDING: MandateState.AWAITING_APPROVAL,
        PravaMandateStatus.ACTIVE: MandateState.ACTIVE,
        PravaMandateStatus.PAUSED: MandateState.PAUSED,
        PravaMandateStatus.CONSUMED: MandateState.CONSUMED,
        PravaMandateStatus.CANCELLED: MandateState.CANCELLED,
        PravaMandateStatus.EXPIRED: MandateState.EXPIRED,
    }
    target = mapping[status]
    # Never drag a mandate backwards out of an in-flight charge just because a
    # poll observed ``active`` again.
    if current in {
        MandateState.CHARGING,
        MandateState.CHECKOUT_IN_PROGRESS,
        MandateState.REPORTING,
        MandateState.SUCCEEDED,
        MandateState.DECLINED,
    } and target is MandateState.ACTIVE:
        return current
    return target


def _transition(mandate: MandateModel, target: MandateState) -> None:
    mandate.state = target.value


def _set_charge_state(charge: MandateChargeModel, target: MandateChargeState) -> None:
    charge.state = target.value


async def _owned_mandate(
    session: AsyncSession,
    user_id: uuid.UUID,
    occasion_id: uuid.UUID,
    *,
    lock: bool = False,
) -> MandateModel:
    query = select(MandateModel).where(
        MandateModel.occasion_id == occasion_id,
        MandateModel.user_id == user_id,
    )
    if lock:
        query = query.with_for_update()
    mandate = await session.scalar(query)
    if mandate is None:
        raise _not_found("MANDATE_NOT_FOUND", "No mandate exists for that occasion.")
    return mandate


async def _owned_charge(
    session: AsyncSession,
    mandate_id: uuid.UUID,
    charge_id: uuid.UUID,
    *,
    lock: bool = False,
) -> MandateChargeModel:
    query = select(MandateChargeModel).where(
        MandateChargeModel.id == charge_id,
        MandateChargeModel.mandate_id == mandate_id,
    )
    if lock:
        query = query.with_for_update()
    charge = await session.scalar(query)
    if charge is None:
        raise _not_found("MANDATE_CHARGE_NOT_FOUND", "That mandate charge was not found.")
    return charge


async def _mandate_response(
    session: AsyncSession,
    mandate: MandateModel,
) -> MandateResponse:
    charges = (
        await session.scalars(
            select(MandateChargeModel)
            .where(MandateChargeModel.mandate_id == mandate.id)
            .order_by(MandateChargeModel.created_at)
        )
    ).all()
    approval: MandateApprovalSession | None = None
    if (
        mandate.setup_session_id is not None
        and mandate.setup_hosted_url is not None
        and MandateState(mandate.state)
        in {MandateState.AWAITING_APPROVAL, MandateState.SETUP_CREATING}
    ):
        approval = MandateApprovalSession(
            session_id=mandate.setup_session_id,
            hosted_url=mandate.setup_hosted_url,
            expires_at=mandate.setup_expires_at,
        )
    return MandateResponse(
        id=mandate.id,
        recipient_id=mandate.recipient_id,
        occasion_id=mandate.occasion_id,
        state=MandateState(mandate.state),
        approved_amount_minor=mandate.approved_amount_minor,
        currency="USD",
        recurring_frequency=PravaMandateFrequency(mandate.recurring_frequency),
        merchant_scope=PravaMandateScope(mandate.merchant_scope),
        max_charges=mandate.max_charges,
        charges_used=mandate.charges_used,
        valid_until=mandate.valid_until,
        merchant_id=mandate.merchant_id,
        merchant_name=mandate.merchant_name,
        merchant_url=mandate.merchant_url,
        merchant_product_id=mandate.merchant_product_id,
        merchant_variant_id=mandate.merchant_variant_id,
        product_title=mandate.product_title,
        item_price_minor=mandate.item_price_minor,
        provider_mandate_id=mandate.provider_mandate_id,
        provider_status=mandate.provider_status,
        setup_failure_code=mandate.setup_failure_code,
        merchant_order_id=mandate.merchant_order_id,
        merchant_outcome=(
            MerchantCheckoutOutcome(mandate.merchant_outcome)
            if mandate.merchant_outcome is not None
            else None
        ),
        visa_confirmation=mandate.visa_confirmation,
        approval_session=approval,
        charges=[
            MandateChargeView(
                id=charge.id,
                reference=charge.reference,
                amount_minor=charge.amount_minor,
                state=MandateChargeState(charge.state),
                merchant_order_id=charge.merchant_order_id,
                merchant_outcome=(
                    MerchantCheckoutOutcome(charge.merchant_outcome)
                    if charge.merchant_outcome is not None
                    else None
                ),
                visa_confirmation=charge.visa_confirmation,
                created_at=charge.created_at,
                updated_at=charge.updated_at,
            )
            for charge in charges
        ],
        created_at=mandate.created_at,
        updated_at=mandate.updated_at,
    )


async def _flush_mandate_response(
    session: AsyncSession,
    mandate: MandateModel,
) -> MandateResponse:
    """Flush server-managed timestamps, then reload before synchronous serialization.

    PostgreSQL expires ``updated_at`` after the SQL ``onupdate`` expression. Accessing that
    expired attribute from Pydantic serialization would otherwise trigger implicit async IO and
    SQLAlchemy's ``MissingGreenlet`` error.
    """

    await session.flush()
    await session.refresh(mandate)
    return await _mandate_response(session, mandate)


# Kept for symmetry with app.purchase; JSON canonicalization for future keys.
def _canonical_json(payload: dict[str, object]) -> str:
    return json.dumps(payload, sort_keys=True, separators=(",", ":"))


def _unavailable() -> ApiError:
    return ApiError(
        status_code=503,
        code="MANDATE_UNAVAILABLE",
        message="Mandate autopilot is not configured yet.",
        recoverable=True,
    )


class UnavailableMandateService:
    """Stand-in used when Prava credentials or checkout are not configured.

    Every mandate operation fails closed with a recoverable 503 rather than
    silently pretending an autopilot is armed.
    """

    async def get(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
    ) -> MandateResponse:
        del user, occasion_id
        raise _unavailable()

    async def setup(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
        body: MandateSetupRequest,
    ) -> MandateResponse:
        del user, occasion_id, body
        raise _unavailable()

    async def refresh(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
    ) -> MandateResponse:
        del user, occasion_id
        raise _unavailable()

    async def execute(
        self,
        user: AuthenticatedUser,
        occasion_id: uuid.UUID,
        body: MandateExecuteRequest,
        idempotency_key: str,
    ) -> MandateResponse:
        del user, occasion_id, body, idempotency_key
        raise _unavailable()


def build_mandate_service(
    *,
    session_factory: async_sessionmaker[AsyncSession],
    prava: MandatePravaOperations,
    public_base_url: str,
    merchant_checkout: MerchantCheckoutGateway | None = None,
    idempotency_pepper: str | None = None,
) -> MandateOperations:
    return MandateService(
        store=SqlMandateStore(session_factory),
        prava=prava,
        public_base_url=public_base_url,
        merchant_checkout=merchant_checkout,
        idempotency_pepper=idempotency_pepper,
    )
