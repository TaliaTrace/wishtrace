import uuid
from datetime import UTC, datetime

import pytest
from pydantic import SecretStr

from app.auth import AuthenticatedUser
from app.errors import ApiError
from app.mandate import (
    ChargeClaim,
    MandateApprovalSession,
    MandateExecuteRequest,
    MandateResponse,
    MandateService,
    MandateSetupFacts,
    MandateSetupRequest,
    MandateState,
)
from app.merchant_browser import (
    JACKBOX_VARIANT_GID,
    BillingContact,
    MerchantBrowserError,
    MerchantCheckoutOutcome,
    MerchantCheckoutResult,
    MerchantQuote,
    MerchantQuoteRequest,
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
    PravaReportOutcome,
    SensitivePaymentCredential,
)

_NOW = datetime(2026, 8, 1, 15, 0, tzinfo=UTC)
_MANDATE_ID = "mandate-abc"


def _user(email: str | None = "talia@example.com") -> AuthenticatedUser:
    return AuthenticatedUser(
        id=uuid.uuid4(),
        email=email,
        display_name="Talia",
        picture_url=None,
    )


def _billing(email: str = "talia@example.com") -> BillingContact:
    return BillingContact(
        email=email,
        first_name="Test",
        last_name="Buyer",
        address_line1="123 Test Street",
        address_line2=None,
        city="Seattle",
        region="Washington",
        postal_code="98101",
        country_code="US",
        phone=None,
    )


def _credential() -> SensitivePaymentCredential:
    return SensitivePaymentCredential(
        token=SecretStr("test-token-redacted"),
        dynamic_cvv=SecretStr("599"),
        expiry_month=SecretStr("12"),
        expiry_year=SecretStr("27"),
    )


def _mandate(
    *,
    state: MandateState = MandateState.ACTIVE,
    provider_mandate_id: str | None = _MANDATE_ID,
    recurring_frequency: PravaMandateFrequency = PravaMandateFrequency.ONE_TIME,
    max_charges: int = 1,
    charges_used: int = 0,
) -> MandateResponse:
    return MandateResponse(
        id=uuid.uuid4(),
        recipient_id=uuid.uuid4(),
        occasion_id=uuid.uuid4(),
        state=state,
        approved_amount_minor=500,
        currency="USD",
        recurring_frequency=recurring_frequency,
        merchant_scope=PravaMandateScope.LISTED,
        max_charges=max_charges,
        charges_used=charges_used,
        valid_until=datetime(2099, 8, 1, tzinfo=UTC),
        merchant_id="jackbox-games-us",
        merchant_name="Jackbox Games",
        merchant_url="https://checkout.jackboxgames.com/products/jackbox-games-gift-card-5",
        merchant_product_id="gid://shopify/Product/6734381809798",
        merchant_variant_id=JACKBOX_VARIANT_GID,
        product_title="Jackbox Games Gift Card - $5 USD",
        item_price_minor=500,
        provider_mandate_id=provider_mandate_id,
        provider_status="active" if state is MandateState.ACTIVE else None,
        merchant_order_id=None,
        merchant_outcome=None,
        visa_confirmation=None,
        approval_session=None,
        charges=[],
        created_at=_NOW,
        updated_at=_NOW,
    )
class MemoryMandateStore:
    """In-memory store modelling the state machine the service relies on.

    Records the calls the service makes so tests can assert the sequence, and
    keeps enough state (charge references, charges_used) to prove idempotency
    and one-time consumption.
    """

    def __init__(self, mandate: MandateResponse) -> None:
        self.mandate = mandate
        self.setup_facts: MandateSetupFacts | None = None
        self.charges_by_reference: dict[str, uuid.UUID] = {}
        self.settled_charges: list[uuid.UUID] = []
        self.declined_charges: list[uuid.UUID] = []
        self.unknown_charges: list[tuple[uuid.UUID, str]] = []
        self.failed_charges: list[tuple[uuid.UUID, str, bool]] = []
        self.checkout_started: list[uuid.UUID] = []
        self.recorded_checkouts: list[uuid.UUID] = []

    # -- setup surface ------------------------------------------------------
    async def create_setup(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        candidate_id: uuid.UUID,
    ) -> MandateSetupFacts:
        del user_id, occasion_id, candidate_id
        facts = MandateSetupFacts(
            setup_id=self.mandate.id,
            recurring_frequency=self.mandate.recurring_frequency,
            merchant_scope=self.mandate.merchant_scope,
            max_charges=self.mandate.max_charges,
            approved_amount_minor=self.mandate.approved_amount_minor,
            product_title=self.mandate.product_title,
        )
        self.setup_facts = facts
        self.mandate = self.mandate.model_copy(
            update={"state": MandateState.SETUP_CREATING}
        )
        return facts

    async def complete_setup(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        session: HostedPravaSession,
    ) -> MandateResponse:
        del user_id, occasion_id
        self.mandate = self.mandate.model_copy(
            update={
                "state": MandateState.AWAITING_APPROVAL,
                "provider_mandate_id": session.mandate_id,
                "provider_status": "pending",
                "approval_session": MandateApprovalSession(
                    session_id=session.session_id,
                    hosted_url=session.hosted_url,
                    expires_at=session.expires_at,
                ),
            }
        )
        return self.mandate

    async def fail_setup(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        unknown: bool,
        response_id: str | None,
    ) -> MandateResponse:
        del user_id, occasion_id, response_id
        self.mandate = self.mandate.model_copy(
            update={
                "state": MandateState.UNKNOWN if unknown else MandateState.FAILED
            }
        )
        return self.mandate

    async def get(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
    ) -> MandateResponse:
        del user_id, occasion_id
        return self.mandate

    async def record_activation(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        info: PravaMandateInfo,
    ) -> MandateResponse:
        del user_id, occasion_id
        mapping = {
            PravaMandateStatus.ACTIVE: MandateState.ACTIVE,
            PravaMandateStatus.CONSUMED: MandateState.CONSUMED,
            PravaMandateStatus.CANCELLED: MandateState.CANCELLED,
            PravaMandateStatus.EXPIRED: MandateState.EXPIRED,
            PravaMandateStatus.PAUSED: MandateState.PAUSED,
            PravaMandateStatus.PENDING: MandateState.AWAITING_APPROVAL,
        }
        self.mandate = self.mandate.model_copy(
            update={
                "state": mapping[info.status],
                "provider_mandate_id": info.mandate_id,
                "provider_status": info.status.value,
            }
        )
        return self.mandate
    # -- charge surface -----------------------------------------------------
    async def begin_charge(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        reference: str,
        amount_minor: int,
    ) -> ChargeClaim:
        del user_id, occasion_id, amount_minor
        if reference in self.charges_by_reference:
            return ChargeClaim(id=self.charges_by_reference[reference], replayed=True)
        charge_id = uuid.uuid4()
        self.charges_by_reference[reference] = charge_id
        self.mandate = self.mandate.model_copy(
            update={"state": MandateState.CHARGING}
        )
        return ChargeClaim(id=charge_id, replayed=False)

    async def fail_charge(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        reason_code: str,
        outcome_unknown: bool,
    ) -> MandateResponse:
        del user_id, occasion_id
        self.failed_charges.append((charge_id, reason_code, outcome_unknown))
        self.mandate = self.mandate.model_copy(
            update={
                "state": MandateState.UNKNOWN
                if outcome_unknown
                else MandateState.ACTIVE
            }
        )
        return self.mandate

    async def record_charge_declined(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        provider_charge_id: str,
        error_code: str,
    ) -> MandateResponse:
        del user_id, occasion_id, provider_charge_id, error_code
        self.declined_charges.append(charge_id)
        self.mandate = self.mandate.model_copy(
            update={"state": MandateState.DECLINED}
        )
        return self.mandate

    async def begin_charge_checkout(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        provider_charge_id: str,
    ) -> MandateResponse:
        del user_id, occasion_id, provider_charge_id
        self.checkout_started.append(charge_id)
        self.mandate = self.mandate.model_copy(
            update={"state": MandateState.CHECKOUT_IN_PROGRESS}
        )
        return self.mandate

    async def record_charge_checkout(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        result: MerchantCheckoutResult,
    ) -> MandateResponse:
        del user_id, occasion_id
        assert result.amount_minor == self.mandate.approved_amount_minor
        self.recorded_checkouts.append(charge_id)
        self.mandate = self.mandate.model_copy(
            update={
                "state": MandateState.REPORTING,
                "merchant_order_id": result.order_id,
                "merchant_outcome": result.outcome,
            }
        )
        return self.mandate

    async def mark_charge_unknown(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        reason_code: str,
        response_id: str | None,
    ) -> MandateResponse:
        del user_id, occasion_id, response_id
        self.unknown_charges.append((charge_id, reason_code))
        self.mandate = self.mandate.model_copy(
            update={"state": MandateState.UNKNOWN}
        )
        return self.mandate

    async def settle_charge(
        self,
        *,
        user_id: uuid.UUID,
        occasion_id: uuid.UUID,
        charge_id: uuid.UUID,
        merchant_outcome: MerchantCheckoutOutcome,
        visa_confirmation: str,
        response_id: str | None,
        recurring_frequency: PravaMandateFrequency,
    ) -> MandateResponse:
        del user_id, occasion_id, response_id
        self.settled_charges.append(charge_id)
        charges_used = self.mandate.charges_used + 1
        if (
            recurring_frequency is PravaMandateFrequency.ONE_TIME
            or charges_used >= self.mandate.max_charges
        ):
            state = MandateState.CONSUMED
        else:
            state = MandateState.ACTIVE
        self.mandate = self.mandate.model_copy(
            update={
                "state": state,
                "charges_used": charges_used,
                "merchant_outcome": merchant_outcome,
                "visa_confirmation": visa_confirmation,
            }
        )
        return self.mandate
class FakeMandatePrava:
    def __init__(self) -> None:
        self.session_requests: list[PravaMandateSessionRequest] = []
        self.create_error: PravaGatewayError | None = None
        self.charge_result: PravaMandateChargeResult | None = None
        self.charge_error: PravaGatewayError | None = None
        self.report_result: PravaMandateReportResult | None = None
        self.report_error: PravaGatewayError | None = None
        self.mandate_info: PravaMandateInfo | None = None
        self.listed_mandates: list[PravaMandateInfo] = []
        self.list_calls: list[str] = []
        self.charge_calls: list[tuple[str, int, str]] = []
        self.report_calls: list[tuple[str, str, PravaReportOutcome]] = []

    async def create_mandate_session(
        self, request: PravaMandateSessionRequest
    ) -> HostedPravaSession:
        self.session_requests.append(request)
        if self.create_error is not None:
            raise self.create_error
        return HostedPravaSession(
            session_id="session-1",
            hosted_url="https://sandbox.collect.prava.space/checkout?session=1",
            order_id="order-1",
            expires_at=datetime(2099, 8, 1, 16, 0, tzinfo=UTC),
            response_id="response-create-1",
            mandate_id=None,
        )

    async def get_mandate(self, mandate_id: str) -> PravaMandateInfo:
        assert mandate_id == _MANDATE_ID
        if self.mandate_info is None:
            raise AssertionError("mandate_info not configured")
        return self.mandate_info

    async def list_mandates(self, customer_id: str) -> list[PravaMandateInfo]:
        self.list_calls.append(customer_id)
        return self.listed_mandates

    async def charge_mandate(
        self,
        *,
        mandate_id: str,
        amount_minor: int,
        reference: str,
    ) -> PravaMandateChargeResult:
        self.charge_calls.append((mandate_id, amount_minor, reference))
        if self.charge_error is not None:
            raise self.charge_error
        if self.charge_result is None:
            raise AssertionError("charge_result not configured")
        return self.charge_result

    async def report_mandate_charge(
        self,
        *,
        mandate_id: str,
        charge_id: str,
        outcome: PravaReportOutcome,
    ) -> PravaMandateReportResult:
        self.report_calls.append((mandate_id, charge_id, outcome))
        if self.report_error is not None:
            raise self.report_error
        if self.report_result is None:
            return PravaMandateReportResult(
                status=(
                    "completed" if outcome is PravaReportOutcome.APPROVED else "failed"
                ),
                mandate_id=mandate_id,
                charge_id=charge_id,
                order_id="order-1",
                visa_confirmation=(
                    "SUCCESS" if outcome is PravaReportOutcome.APPROVED else "FAILURE"
                ),
                response_id="response-report-1",
            )
        return self.report_result


class FakeMerchantCheckout:
    def __init__(self, checkout_result: MerchantCheckoutResult) -> None:
        self.checkout_result = checkout_result
        self.checkout_error: MerchantBrowserError | None = None
        self.quote_error: MerchantBrowserError | None = None
        self.quote_requests: list[MerchantQuoteRequest] = []
        self.checkout_calls = 0

    async def quote(self, request: MerchantQuoteRequest) -> MerchantQuote:
        self.quote_requests.append(request)
        if self.quote_error is not None:
            raise self.quote_error
        return MerchantQuote(
            item_minor=500,
            shipping_minor=0,
            tax_minor=0,
            total_minor=500,
            currency="USD",
            delivery_summary="Jackbox shop only",
            source="JACKBOX_SHOPIFY_BROWSER",
            quoted_at=_NOW,
            expires_at=datetime(2099, 8, 1, 16, 0, tzinfo=UTC),
        )

    async def checkout(
        self,
        *,
        purchase_intent_id: uuid.UUID,
        credential: SensitivePaymentCredential,
    ) -> MerchantCheckoutResult:
        del purchase_intent_id
        assert credential.token.get_secret_value() == "test-token-redacted"
        self.checkout_calls += 1
        if self.checkout_error is not None:
            raise self.checkout_error
        return self.checkout_result

    async def is_quote_active(self, purchase_intent_id: uuid.UUID) -> bool:
        del purchase_intent_id
        return False

    async def close(self) -> None:
        return None


def _service(
    store: MemoryMandateStore,
    prava: FakeMandatePrava,
    checkout: FakeMerchantCheckout | None,
) -> MandateService:
    return MandateService(
        store=store,  # type: ignore[arg-type]
        prava=prava,  # type: ignore[arg-type]
        public_base_url="https://api.wishtrace.example",
        merchant_checkout=checkout,  # type: ignore[arg-type]
        idempotency_pepper="pepper-value-for-tests",
    )


def _ok_checkout() -> MerchantCheckoutResult:
    return MerchantCheckoutResult(
        outcome=MerchantCheckoutOutcome.ORDER_VERIFIED,
        order_id="jackbox-order-1",
        amount_minor=500,
        currency="USD",
        reason_code="MERCHANT_ORDER_VERIFIED",
    )


def _minted_credential() -> PravaMandateChargeResult:
    return PravaMandateChargeResult(
        mandate_id=_MANDATE_ID,
        charge_id="charge-1",
        status="awaiting_result",
        credential=_credential(),
        order_id="order-1",
        error_code=None,
    )
# ── Happy-path: one-time mandate executes → merchant verified → reported → consumed ──
async def test_execute_one_time_mandate_reports_and_consumes() -> None:
    store = MemoryMandateStore(_mandate())
    prava = FakeMandatePrava()
    prava.charge_result = _minted_credential()
    checkout = FakeMerchantCheckout(_ok_checkout())
    service = _service(store, prava, checkout)

    result = await service.execute(
        _user(),
        store.mandate.occasion_id,
        MandateExecuteRequest(billing=_billing()),
        "exec-key-0001",
    )

    assert result.state is MandateState.CONSUMED
    assert result.charges_used == 1
    assert result.visa_confirmation == "SUCCESS"
    assert result.merchant_outcome is MerchantCheckoutOutcome.ORDER_VERIFIED
    assert len(store.settled_charges) == 1
    assert checkout.checkout_calls == 1
    # Exactly one Prava charge call and one report call (APPROVED).
    assert len(prava.charge_calls) == 1
    assert prava.report_calls == [
        (_MANDATE_ID, "charge-1", PravaReportOutcome.APPROVED)
    ]
    # The credentials never surfaced in the response.
    assert result.provider_mandate_id == _MANDATE_ID
    assert getattr(result, "credential", None) is None


async def test_execute_idempotent_replay_does_not_double_charge() -> None:
    # A recurring mandate stays ACTIVE after a charge, so a same-key retry
    # exercises the begin_charge replay path rather than the not-active guard.
    store = MemoryMandateStore(
        _mandate(
            recurring_frequency=PravaMandateFrequency.MONTHLY,
            max_charges=5,
        )
    )
    prava = FakeMandatePrava()
    prava.charge_result = _minted_credential()
    checkout = FakeMerchantCheckout(_ok_checkout())
    service = _service(store, prava, checkout)

    first = await service.execute(
        _user(),
        store.mandate.occasion_id,
        MandateExecuteRequest(billing=_billing()),
        "exec-key-0001",
    )
    assert first.state is MandateState.ACTIVE
    assert first.charges_used == 1
    assert checkout.checkout_calls == 1

    # Same idempotency key replays: begin_charge returns replayed=True and the
    # service returns the current view without minting/checking out again.
    replayed = await service.execute(
        _user(),
        store.mandate.occasion_id,
        MandateExecuteRequest(billing=_billing()),
        "exec-key-0001",
    )
    assert replayed.charges_used == 1
    assert checkout.checkout_calls == 1
    assert len(prava.charge_calls) == 1


async def test_execute_recurring_mandate_stays_active_while_charges_remain() -> None:
    store = MemoryMandateStore(
        _mandate(
            recurring_frequency=PravaMandateFrequency.MONTHLY,
            max_charges=5,
        )
    )
    prava = FakeMandatePrava()
    prava.charge_result = _minted_credential()
    checkout = FakeMerchantCheckout(_ok_checkout())
    service = _service(store, prava, checkout)

    result = await service.execute(
        _user(),
        store.mandate.occasion_id,
        MandateExecuteRequest(billing=_billing()),
        "exec-key-0002",
    )

    assert result.state is MandateState.ACTIVE
    assert result.charges_used == 1
    assert len(store.settled_charges) == 1


async def test_execute_prava_decline_records_and_never_checks_out() -> None:
    store = MemoryMandateStore(_mandate())
    prava = FakeMandatePrava()
    prava.charge_result = PravaMandateChargeResult(
        mandate_id=_MANDATE_ID,
        charge_id="charge-declined",
        status="failed",
        credential=None,
        order_id=None,
        error_code="THRESHOLD_EXCEEDED",
    )
    checkout = FakeMerchantCheckout(_ok_checkout())
    service = _service(store, prava, checkout)

    result = await service.execute(
        _user(),
        store.mandate.occasion_id,
        MandateExecuteRequest(billing=_billing()),
        "exec-key-0003",
    )

    assert result.state is MandateState.DECLINED
    assert len(store.declined_charges) == 1
    assert checkout.checkout_calls == 0
    assert prava.report_calls == []


async def test_execute_rejects_unverified_or_mismatched_email() -> None:
    store = MemoryMandateStore(_mandate())
    prava = FakeMandatePrava()
    prava.charge_result = _minted_credential()
    checkout = FakeMerchantCheckout(_ok_checkout())
    service = _service(store, prava, checkout)

    # No verified email on the user.
    with pytest.raises(ApiError) as no_email:
        await service.execute(
            _user(email=None),
            store.mandate.occasion_id,
            MandateExecuteRequest(billing=_billing()),
            "exec-key-0004",
        )
    assert no_email.value.code == "VERIFIED_EMAIL_REQUIRED"

    # Billing email differs from the verified sign-in email.
    with pytest.raises(ApiError) as mismatch:
        await service.execute(
            _user(email="talia@example.com"),
            store.mandate.occasion_id,
            MandateExecuteRequest(billing=_billing(email="other@example.com")),
            "exec-key-0005",
        )
    assert mismatch.value.code == "CHECKOUT_EMAIL_MISMATCH"
    assert checkout.checkout_calls == 0


async def test_execute_requires_active_mandate() -> None:
    store = MemoryMandateStore(
        _mandate(state=MandateState.AWAITING_APPROVAL, provider_mandate_id=None)
    )
    prava = FakeMandatePrava()
    checkout = FakeMerchantCheckout(_ok_checkout())
    service = _service(store, prava, checkout)

    with pytest.raises(ApiError) as inactive:
        await service.execute(
            _user(),
            store.mandate.occasion_id,
            MandateExecuteRequest(billing=_billing()),
            "exec-key-0006",
        )
    assert inactive.value.code == "MANDATE_NOT_ACTIVE"
    assert checkout.checkout_calls == 0


async def test_execute_requires_valid_idempotency_key() -> None:
    store = MemoryMandateStore(_mandate())
    prava = FakeMandatePrava()
    prava.charge_result = _minted_credential()
    checkout = FakeMerchantCheckout(_ok_checkout())
    service = _service(store, prava, checkout)

    with pytest.raises(ApiError) as bad_key:
        await service.execute(
            _user(),
            store.mandate.occasion_id,
            MandateExecuteRequest(billing=_billing()),
            "short",
        )
    assert bad_key.value.code == "IDEMPOTENCY_KEY_INVALID"
    assert checkout.checkout_calls == 0


async def test_execute_merchant_checkout_unknown_returns_current_view() -> None:
    store = MemoryMandateStore(_mandate())
    prava = FakeMandatePrava()
    prava.charge_result = _minted_credential()
    checkout = FakeMerchantCheckout(
        MerchantCheckoutResult(
            outcome=MerchantCheckoutOutcome.UNKNOWN,
            order_id=None,
            amount_minor=500,
            currency="USD",
            reason_code="MERCHANT_OUTCOME_UNKNOWN",
        )
    )
    service = _service(store, prava, checkout)

    result = await service.execute(
        _user(),
        store.mandate.occasion_id,
        MandateExecuteRequest(billing=_billing()),
        "exec-key-0007",
    )

    # Unknown merchant outcome → no report call; the view is left for reconcile.
    assert prava.report_calls == []
    assert result.state is not MandateState.CONSUMED
    assert checkout.checkout_calls == 1


async def test_execute_report_mismatch_marks_charge_unknown() -> None:
    store = MemoryMandateStore(_mandate())
    prava = FakeMandatePrava()
    prava.charge_result = _minted_credential()
    prava.report_result = PravaMandateReportResult(
        status="completed",
        mandate_id=_MANDATE_ID,
        charge_id="charge-DIFFERENT",
        order_id="order-1",
        visa_confirmation="SUCCESS",
        response_id="response-report-1",
    )
    checkout = FakeMerchantCheckout(_ok_checkout())
    service = _service(store, prava, checkout)

    with pytest.raises(ApiError) as mismatch:
        await service.execute(
            _user(),
            store.mandate.occasion_id,
            MandateExecuteRequest(billing=_billing()),
            "exec-key-0008",
        )
    assert mismatch.value.code == "PRAVA_RESPONSE_MISMATCH"
    assert store.unknown_charges
    assert store.unknown_charges[0][1] == "PRAVA_REPORT_MISMATCH"
    assert checkout.checkout_calls == 1


async def test_setup_creates_session_with_frequency_and_records_awaiting() -> None:
    store = MemoryMandateStore(_mandate())
    prava = FakeMandatePrava()
    service = _service(store, prava, checkout=None)

    result = await service.setup(
        _user(),
        store.mandate.occasion_id,
        MandateSetupRequest(candidate_id=uuid.uuid4()),
    )

    assert result.state is MandateState.AWAITING_APPROVAL
    assert result.approval_session is not None
    assert result.provider_mandate_id is None
    assert len(prava.session_requests) == 1
    request = prava.session_requests[0]
    assert request.recurring_frequency is PravaMandateFrequency.ONE_TIME
    assert request.merchant_scope is PravaMandateScope.LISTED
    assert request.max_charges == 1
    assert request.total_minor == 500
    assert request.external_order_ref == f"mandate-{store.mandate.id.hex}"
    # The callback routes Prava's approval redirect back through our return path.
    assert "/v1/prava/mandate-return" in request.callback_url
    assert f"occasion_id={store.mandate.occasion_id}" in request.callback_url


async def test_setup_prava_error_fails_mandate_and_raises() -> None:
    store = MemoryMandateStore(_mandate())
    prava = FakeMandatePrava()
    prava.create_error = PravaGatewayError(
        code="PRAVA_UNAVAILABLE",
        message="Prava approval is not configured yet.",
        recoverable=True,
        outcome_unknown=True,
    )
    service = _service(store, prava, checkout=None)

    with pytest.raises(ApiError) as error:
        await service.setup(
            _user(),
            store.mandate.occasion_id,
            MandateSetupRequest(candidate_id=uuid.uuid4()),
        )
    assert error.value.code == "PRAVA_UNAVAILABLE"
    assert store.mandate.state is MandateState.UNKNOWN


async def test_refresh_activates_pending_mandate_from_provider() -> None:
    store = MemoryMandateStore(
        _mandate(state=MandateState.AWAITING_APPROVAL, provider_mandate_id=_MANDATE_ID)
    )
    prava = FakeMandatePrava()
    prava.mandate_info = PravaMandateInfo(
        mandate_id=_MANDATE_ID,
        status=PravaMandateStatus.ACTIVE,
        recurring_frequency=PravaMandateFrequency.ONE_TIME,
        merchant_scope=PravaMandateScope.LISTED,
        approved_amount="5.00",
        currency="USD",
        created_at=_NOW,
        valid_until=datetime(2099, 8, 1, tzinfo=UTC),
        total_charges=0,
        remaining_charges=1,
    )
    service = _service(store, prava, checkout=None)

    result = await service.refresh(_user(), store.mandate.occasion_id)

    assert result.state is MandateState.ACTIVE
    assert result.provider_status == "active"


async def test_refresh_associates_current_customer_mandate_after_hosted_approval() -> None:
    store = MemoryMandateStore(
        _mandate(
            state=MandateState.AWAITING_APPROVAL,
            provider_mandate_id=None,
        )
    )
    prava = FakeMandatePrava()
    prava.listed_mandates = [
        PravaMandateInfo(
            mandate_id=_MANDATE_ID,
            status=PravaMandateStatus.ACTIVE,
            recurring_frequency=PravaMandateFrequency.ONE_TIME,
            merchant_scope=PravaMandateScope.LISTED,
            approved_amount="5.00",
            currency="USD",
            created_at=_NOW,
            valid_until=datetime(2099, 8, 1, tzinfo=UTC),
            merchant_name="Jackbox Games",
        )
    ]
    service = _service(store, prava, checkout=None)
    user = _user()

    result = await service.refresh(user, store.mandate.occasion_id)

    assert prava.list_calls == [str(user.id)]
    assert result.provider_mandate_id == _MANDATE_ID
    assert result.state is MandateState.ACTIVE


async def test_refresh_rejects_ambiguous_current_customer_mandates() -> None:
    store = MemoryMandateStore(
        _mandate(
            state=MandateState.AWAITING_APPROVAL,
            provider_mandate_id=None,
        )
    )
    match = PravaMandateInfo(
        mandate_id=_MANDATE_ID,
        status=PravaMandateStatus.ACTIVE,
        recurring_frequency=PravaMandateFrequency.ONE_TIME,
        merchant_scope=PravaMandateScope.LISTED,
        approved_amount="5.00",
        currency="USD",
        created_at=_NOW,
        valid_until=datetime(2099, 8, 1, tzinfo=UTC),
        merchant_name="Jackbox Games",
    )
    prava = FakeMandatePrava()
    prava.listed_mandates = [
        match,
        match.model_copy(update={"mandate_id": "mandate-other"}),
    ]
    service = _service(store, prava, checkout=None)

    with pytest.raises(ApiError) as ambiguous:
        await service.refresh(_user(), store.mandate.occasion_id)

    assert ambiguous.value.code == "PRAVA_RESPONSE_AMBIGUOUS"


async def test_refresh_rejects_mismatched_provider_mandate() -> None:
    store = MemoryMandateStore(
        _mandate(state=MandateState.AWAITING_APPROVAL, provider_mandate_id=_MANDATE_ID)
    )
    prava = FakeMandatePrava()
    prava.mandate_info = PravaMandateInfo(
        mandate_id="mandate-OTHER",
        status=PravaMandateStatus.ACTIVE,
        recurring_frequency=PravaMandateFrequency.ONE_TIME,
        merchant_scope=PravaMandateScope.LISTED,
        approved_amount="5.00",
        currency="USD",
        created_at=_NOW,
        valid_until=datetime(2099, 8, 1, tzinfo=UTC),
        total_charges=0,
        remaining_charges=1,
    )
    service = _service(store, prava, checkout=None)

    with pytest.raises(ApiError) as mismatch:
        await service.refresh(_user(), store.mandate.occasion_id)
    assert mismatch.value.code == "PRAVA_RESPONSE_INVALID"


async def test_execute_without_checkout_configured_fails_closed() -> None:
    store = MemoryMandateStore(_mandate())
    prava = FakeMandatePrava()
    prava.charge_result = _minted_credential()
    service = _service(store, prava, checkout=None)

    with pytest.raises(ApiError) as unavailable:
        await service.execute(
            _user(),
            store.mandate.occasion_id,
            MandateExecuteRequest(billing=_billing()),
            "exec-key-0009",
        )
    assert unavailable.value.code == "MERCHANT_CHECKOUT_UNAVAILABLE"
