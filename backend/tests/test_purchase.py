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
from app.merchant_browser import (
    JACKBOX_VARIANT_GID,
    BillingContact,
    MerchantCheckoutOutcome,
    MerchantCheckoutResult,
    MerchantQuote,
    MerchantQuoteRequest,
)
from app.prava import (
    HostedPravaSession,
    PravaGatewayError,
    PravaLineItemResult,
    PravaPaymentResult,
    PravaPaymentStatus,
    PravaReportOutcome,
    PravaReportResult,
    PravaSessionRequest,
    PravaTransactionResult,
    SensitivePaymentCredential,
)
from app.purchase import (
    ApprovalSessionResponse,
    PurchaseIntentResponse,
    PurchaseQuoteFacts,
    PurchaseQuoteRequest,
    PurchaseService,
    PurchaseStore,
    QuoteClaim,
    QuoteClaimAction,
    SessionClaim,
    SessionClaimAction,
    TransactionState,
    _require_transition,
    receipt,
)


class MemoryPurchaseStore(PurchaseStore):
    def __init__(self, intent: PurchaseIntentResponse) -> None:
        self.intent = intent
        self.quote_key_hash: bytes | None = None
        self.quote_request_hash: bytes | None = None
        self.quote_status: str | None = None
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

    async def claim_quote(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        request_hash: bytes,
        now: datetime,
    ) -> QuoteClaim:
        del user_id, now
        assert purchase_intent_id == self.intent.id
        if self.quote_key_hash is not None:
            if (
                self.quote_key_hash == key_hash
                and self.quote_request_hash == request_hash
                and self.quote_status == "COMPLETED"
            ):
                return QuoteClaim(
                    action=QuoteClaimAction.REPLAY,
                    existing=self.intent,
                )
            raise AssertionError("unexpected quote retry")
        self.quote_key_hash = key_hash
        self.quote_request_hash = request_hash
        self.quote_status = "IN_PROGRESS"
        self.intent = self.intent.model_copy(
            update={"state": TransactionState.VALIDATING}
        )
        return QuoteClaim(
            action=QuoteClaimAction.CREATE,
            facts=PurchaseQuoteFacts(
                intent=self.intent,
                product_url=(
                    "https://checkout.jackboxgames.com/products/"
                    "jackbox-games-gift-card-5"
                ),
                budget_minor=500,
            ),
        )

    async def complete_quote(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        quote: MerchantQuote,
    ) -> PurchaseIntentResponse:
        del user_id
        assert purchase_intent_id == self.intent.id
        assert key_hash == self.quote_key_hash
        self.quote_status = "COMPLETED"
        self.intent = self.intent.model_copy(
            update={
                "state": TransactionState.READY_FOR_APPROVAL,
                "item_price_minor": quote.item_minor,
                "approved_total_minor": quote.total_minor,
                "quote_source": quote.source,
                "quote_timestamp": quote.quoted_at,
                "quote_expires_at": quote.expires_at,
                "delivery_summary": quote.delivery_summary,
            }
        )
        return self.intent

    async def fail_quote(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        key_hash: bytes,
        reason_code: str,
    ) -> PurchaseIntentResponse:
        del user_id, reason_code
        assert purchase_intent_id == self.intent.id
        assert key_hash == self.quote_key_hash
        self.quote_status = "FAILED"
        self.intent = self.intent.model_copy(update={"state": TransactionState.FAILED})
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

    async def begin_merchant_checkout(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
    ) -> PurchaseIntentResponse:
        del user_id
        assert purchase_intent_id == self.intent.id
        assert self.intent.state == TransactionState.CREDENTIALS_READY
        self.intent = self.intent.model_copy(
            update={"state": TransactionState.CHECKOUT_IN_PROGRESS}
        )
        return self.intent

    async def record_merchant_checkout(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        result: MerchantCheckoutResult,
    ) -> PurchaseIntentResponse:
        del user_id
        assert purchase_intent_id == self.intent.id
        assert result.amount_minor == self.intent.approved_total_minor
        state = {
            MerchantCheckoutOutcome.ORDER_VERIFIED: TransactionState.ORDER_VERIFIED,
            MerchantCheckoutOutcome.DECLINED: TransactionState.UNKNOWN,
            MerchantCheckoutOutcome.UNKNOWN: TransactionState.UNKNOWN,
        }[result.outcome]
        self.intent = self.intent.model_copy(
            update={
                "state": state,
                "merchant_order_id": result.order_id,
                "merchant_outcome": result.outcome,
                "merchant_attempted_at": datetime.now(UTC),
            }
        )
        return self.intent

    async def fail_merchant_checkout(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        reason_code: str,
        outcome_unknown: bool,
    ) -> PurchaseIntentResponse:
        del user_id, reason_code
        assert purchase_intent_id == self.intent.id
        self.intent = self.intent.model_copy(
            update={
                "state": (
                    TransactionState.UNKNOWN
                    if outcome_unknown
                    else TransactionState.FAILED
                ),
                "merchant_outcome": (
                    MerchantCheckoutOutcome.UNKNOWN if outcome_unknown else None
                ),
            }
        )
        return self.intent

    async def record_prava_report(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        report: PravaReportResult,
    ) -> PurchaseIntentResponse:
        del user_id
        assert purchase_intent_id == self.intent.id
        self.intent = self.intent.model_copy(
            update={"visa_confirmation": report.visa_confirmation}
        )
        return self.intent

    async def mark_transaction_unknown(
        self,
        *,
        user_id: uuid.UUID,
        purchase_intent_id: uuid.UUID,
        reason_code: str,
        response_id: str | None,
    ) -> PurchaseIntentResponse:
        del user_id, reason_code, response_id
        assert purchase_intent_id == self.intent.id
        self.intent = self.intent.model_copy(update={"state": TransactionState.UNKNOWN})
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
        self.payment_results: list[PravaPaymentResult] = []
        self.reports: list[tuple[str, str, PravaReportOutcome]] = []

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
        if self.payment_results:
            return self.payment_results.pop(0)
        if self.payment_result is None:
            raise AssertionError("payment result was not configured")
        return self.payment_result

    async def report_status(
        self,
        *,
        session_id: str,
        txn_ref_id: str,
        outcome: PravaReportOutcome,
    ) -> PravaReportResult:
        self.reports.append((session_id, txn_ref_id, outcome))
        return PravaReportResult(
            status="confirmed",
            txn_ref_id=txn_ref_id,
            txn_status=outcome,
            visa_confirmation=(
                "SUCCESS" if outcome is PravaReportOutcome.APPROVED else "FAILURE"
            ),
            response_id="response-report-1",
        )


class FakeMerchantCheckout:
    def __init__(self, checkout_result: MerchantCheckoutResult) -> None:
        self.checkout_result = checkout_result
        self.quote_requests: list[MerchantQuoteRequest] = []
        self.checkout_calls = 0
        self.quote_active = False

    async def quote(self, request: MerchantQuoteRequest) -> MerchantQuote:
        self.quote_requests.append(request)
        self.quote_active = True
        now = datetime.now(UTC)
        return MerchantQuote(
            item_minor=500,
            shipping_minor=0,
            tax_minor=0,
            total_minor=500,
            currency="USD",
            delivery_summary=(
                "Sent to the checkout contact email for manual forwarding; "
                "Jackbox shop only, supported regions only, timing not guaranteed"
            ),
            quoted_at=now,
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
        self.quote_active = False
        return self.checkout_result

    async def is_quote_active(self, purchase_intent_id: uuid.UUID) -> bool:
        del purchase_intent_id
        return self.quote_active

    async def close(self) -> None:
        return None


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


def _billing() -> BillingContact:
    return BillingContact(
        email="talia@example.com",
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
        merchant_id="jackbox-games-us",
        merchant_name="Jackbox Games",
        merchant_url="https://checkout.jackboxgames.com",
        merchant_product_id="gid://shopify/Product/6734381809798",
        merchant_variant_id=JACKBOX_VARIANT_GID,
        sku="GC20221246",
        title="Jackbox Games Gift Card - $5 USD",
        variant_title="$5.00",
        item_price_minor=500,
        currency="USD",
        approved_total_minor=500,
        quote_source="JACKBOX_SHOPIFY_BROWSER",
        quote_timestamp=now,
        quote_expires_at=datetime(2099, 8, 1, 15, 15, tzinfo=UTC),
        delivery_summary=(
            "Sent to the checkout contact email for manual forwarding; "
            "Jackbox shop only, supported regions only, timing not guaranteed"
        ),
        approval_session=approval_session,
        provider_status="pending" if approval_session else None,
        created_at=now,
        updated_at=now,
    )


def _payment_result(status: PravaPaymentStatus) -> PravaPaymentResult:
    credential = (
        SensitivePaymentCredential(
            token=SecretStr("test-token-redacted"),
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
                        merchant_name="Jackbox Games",
                        merchant_url="https://checkout.jackboxgames.com",
                        total_minor=500,
                        status=status,
                        credential=credential,
                    )
                ],
                error_code=None,
            )
        ],
        response_id="response-poll-1",
    )


async def test_live_quote_is_idempotent_and_keeps_billing_out_of_response() -> None:
    user = _user()
    store = MemoryPurchaseStore(_intent(state=TransactionState.DRAFT))
    merchant = FakeMerchantCheckout(
        MerchantCheckoutResult(
            outcome=MerchantCheckoutOutcome.DECLINED,
            amount_minor=500,
            currency="USD",
            reason_code="MERCHANT_PAYMENT_DECLINED",
        )
    )
    service = PurchaseService(
        store=store,
        prava=FakePrava(),
        public_base_url="https://api.wishtrace.example",
        merchant_checkout=merchant,
        idempotency_pepper="quote-test-pepper-with-at-least-32-bytes",
    )
    body = PurchaseQuoteRequest(billing=_billing())

    first = await service.quote(user, store.intent.id, body, "quote-key-123")
    replay = await service.quote(user, store.intent.id, body, "quote-key-123")

    assert len(merchant.quote_requests) == 1
    assert first.state is TransactionState.READY_FOR_APPROVAL
    assert first.approved_total_minor == 500
    assert replay == first
    assert "123 Test Street" not in first.model_dump_json()
    assert "talia@example.com" not in first.model_dump_json()


async def test_live_quote_requires_verified_checkout_email() -> None:
    user = _user()
    store = MemoryPurchaseStore(_intent(state=TransactionState.DRAFT))
    merchant = FakeMerchantCheckout(
        MerchantCheckoutResult(
            outcome=MerchantCheckoutOutcome.DECLINED,
            amount_minor=500,
            currency="USD",
            reason_code="MERCHANT_PAYMENT_DECLINED",
        )
    )
    service = PurchaseService(
        store=store,
        prava=FakePrava(),
        public_base_url="https://api.wishtrace.example",
        merchant_checkout=merchant,
        idempotency_pepper="quote-test-pepper-with-at-least-32-bytes",
    )
    mismatched = _billing().model_copy(update={"email": "friend@example.com"})

    with pytest.raises(ApiError) as error:
        await service.quote(
            user,
            store.intent.id,
            PurchaseQuoteRequest(billing=mismatched),
            "quote-key-123",
        )

    assert error.value.code == "CHECKOUT_EMAIL_MISMATCH"
    assert merchant.quote_requests == []


@pytest.mark.parametrize(
    ("merchant_outcome", "prava_final", "expected_state", "report_outcome"),
    [
        (
            MerchantCheckoutOutcome.ORDER_VERIFIED,
            PravaPaymentStatus.COMPLETED,
            TransactionState.SUCCEEDED,
            PravaReportOutcome.APPROVED,
        ),
        (
            MerchantCheckoutOutcome.DECLINED,
            PravaPaymentStatus.FAILED,
            TransactionState.DECLINED,
            PravaReportOutcome.DECLINED,
        ),
    ],
)
async def test_reconcile_runs_one_real_merchant_attempt_and_reports_prava(
    merchant_outcome: MerchantCheckoutOutcome,
    prava_final: PravaPaymentStatus,
    expected_state: TransactionState,
    report_outcome: PravaReportOutcome,
) -> None:
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
    prava.payment_results = [
        _payment_result(PravaPaymentStatus.AWAITING_RESULT),
        _payment_result(prava_final),
    ]
    merchant = FakeMerchantCheckout(
        MerchantCheckoutResult(
            outcome=merchant_outcome,
            order_id=(
                "JB-12345"
                if merchant_outcome is MerchantCheckoutOutcome.ORDER_VERIFIED
                else None
            ),
            amount_minor=500,
            currency="USD",
            reason_code=(
                "MERCHANT_ORDER_VERIFIED"
                if merchant_outcome is MerchantCheckoutOutcome.ORDER_VERIFIED
                else "MERCHANT_PAYMENT_DECLINED"
            ),
        )
    )
    service = PurchaseService(
        store=store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
        merchant_checkout=merchant,
    )

    response = await service.reconcile(user, store.intent.id)

    assert merchant.checkout_calls == 1
    assert prava.reports == [("session-1", "line-1", report_outcome)]
    assert response.state is expected_state
    assert response.merchant_outcome is merchant_outcome
    assert response.merchant_order_id == (
        "JB-12345"
        if merchant_outcome is MerchantCheckoutOutcome.ORDER_VERIFIED
        else None
    )
    assert response.visa_confirmation == (
        "SUCCESS" if report_outcome is PravaReportOutcome.APPROVED else "FAILURE"
    )
    assert store.persisted_sensitive_credential is False


@pytest.mark.parametrize(
    ("merchant_outcome", "prava_final", "expected_state", "report_outcome"),
    [
        (
            MerchantCheckoutOutcome.ORDER_VERIFIED,
            PravaPaymentStatus.COMPLETED,
            TransactionState.SUCCEEDED,
            PravaReportOutcome.APPROVED,
        ),
        (
            MerchantCheckoutOutcome.DECLINED,
            PravaPaymentStatus.FAILED,
            TransactionState.DECLINED,
            PravaReportOutcome.DECLINED,
        ),
    ],
)
async def test_reconcile_reports_persisted_merchant_result_without_second_checkout(
    merchant_outcome: MerchantCheckoutOutcome,
    prava_final: PravaPaymentStatus,
    expected_state: TransactionState,
    report_outcome: PravaReportOutcome,
) -> None:
    user = _user()
    approval = ApprovalSessionResponse(
        session_id="session-1",
        hosted_url="https://sandbox.collect.prava.space/checkout?session=1",
        expires_at=datetime(2099, 8, 1, 16, 0, tzinfo=UTC),
    )
    persisted = _intent(
        state=TransactionState.UNKNOWN,
        approval_session=approval,
    ).model_copy(
        update={
            "merchant_outcome": merchant_outcome,
            "merchant_order_id": (
                "JB-RECOVERED"
                if merchant_outcome is MerchantCheckoutOutcome.ORDER_VERIFIED
                else None
            ),
            "provider_status": PravaPaymentStatus.AWAITING_RESULT.value,
        }
    )
    store = MemoryPurchaseStore(persisted)
    prava = FakePrava()
    prava.payment_results = [
        _payment_result(PravaPaymentStatus.AWAITING_RESULT),
        _payment_result(prava_final),
    ]
    merchant = FakeMerchantCheckout(
        MerchantCheckoutResult(
            outcome=MerchantCheckoutOutcome.UNKNOWN,
            amount_minor=500,
            currency="USD",
            reason_code="SHOULD_NOT_RUN",
        )
    )
    service = PurchaseService(
        store=store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
        merchant_checkout=merchant,
    )

    response = await service.reconcile(user, store.intent.id)

    assert merchant.checkout_calls == 0
    assert prava.reports == [("session-1", "line-1", report_outcome)]
    assert response.state is expected_state
    assert response.merchant_outcome is merchant_outcome


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
    assert request.total_minor == 500
    assert request.product_unit_minor == 500
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

    active_store = MemoryPurchaseStore(_intent())
    inactive_merchant = FakeMerchantCheckout(
        MerchantCheckoutResult(
            outcome=MerchantCheckoutOutcome.DECLINED,
            amount_minor=500,
            currency="USD",
            reason_code="MERCHANT_PAYMENT_DECLINED",
        )
    )
    guarded = PurchaseService(
        store=active_store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
        merchant_checkout=inactive_merchant,
    )
    with pytest.raises(ApiError) as inactive_quote:
        await guarded.create_prava_session(
            user,
            active_store.intent.id,
            "stable-key-123",
        )
    assert inactive_quote.value.code == "FRESH_QUOTE_REQUIRED"
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
    assert "test-token-redacted" not in response.model_dump_json()
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
    line_item = transaction.line_items[0].model_copy(update={"total_minor": 501})
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


def test_receipt_requires_both_prava_and_merchant_evidence() -> None:
    with pytest.raises(ApiError, match="final authoritative"):
        receipt(_intent())

    order = receipt(
        _intent(state=TransactionState.SUCCEEDED).model_copy(
            update={
                "provider_status": "completed",
                "visa_confirmation": "SUCCESS",
                "merchant_outcome": MerchantCheckoutOutcome.ORDER_VERIFIED,
                "merchant_order_id": "JB-12345",
            }
        )
    )
    assert order.kind == "ORDER_RECEIPT"
    assert order.merchant_order_id == "JB-12345"

    decline = receipt(
        _intent(state=TransactionState.DECLINED).model_copy(
            update={
                "provider_status": "failed",
                "visa_confirmation": "FAILURE",
                "merchant_outcome": MerchantCheckoutOutcome.DECLINED,
            }
        )
    )
    assert decline.kind == "AUTHORIZATION_RESULT"
    assert "decline" in decline.message.casefold()


async def test_purchase_routes_require_auth_idempotency_and_safe_return() -> None:
    user = _user()
    store = MemoryPurchaseStore(_intent())
    prava = FakePrava()
    merchant = FakeMerchantCheckout(
        MerchantCheckoutResult(
            outcome=MerchantCheckoutOutcome.DECLINED,
            amount_minor=500,
            currency="USD",
            reason_code="MERCHANT_PAYMENT_DECLINED",
        )
    )
    service = PurchaseService(
        store=store,
        prava=prava,
        public_base_url="https://api.wishtrace.example",
        merchant_checkout=merchant,
        idempotency_pepper="route-test-pepper-with-at-least-32-bytes",
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
        quote_path = f"/v1/purchase-intents/{store.intent.id}/quote"
        quote = await client.post(
            quote_path,
            headers={
                "Authorization": "Bearer valid-session",
                "Idempotency-Key": "quote-key-123",
            },
            json={"billing": _billing().model_dump(mode="json")},
        )
        assert quote.status_code == 200
        assert quote.json()["approved_total_minor"] == 500
        assert "billing" not in quote.json()

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
