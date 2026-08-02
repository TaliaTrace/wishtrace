import json
from datetime import UTC, datetime

import httpx
import pytest
from pydantic import SecretStr, ValidationError

from app.prava import (
    PravaGatewayError,
    PravaHttpGateway,
    PravaMandateFrequency,
    PravaMandateScope,
    PravaMandateSessionRequest,
    PravaMandateStatus,
    PravaPaymentStatus,
    PravaReportOutcome,
    PravaSessionRequest,
)

BASE_URL = "https://sandbox.api.prava.space"
FAKE_SECRET = "sandbox-secret-fixture-not-a-real-key"


def _request() -> PravaSessionRequest:
    return PravaSessionRequest(
        user_id="user-123",
        user_email="talia@example.com",
        total_minor=6999,
        currency="USD",
        merchant_name="Observed Merchant",
        merchant_url="https://www.example.com",
        merchant_country="US",
        product_description="Observed headset — Black",
        product_unit_minor=6499,
        external_product_id="variant-123",
        quantity=1,
        callback_url="https://api.wishtrace.example.com/v1/prava/return",
        external_order_ref="purchase-intent-123",
    )


def _gateway(handler: httpx.MockTransport) -> PravaHttpGateway:
    return PravaHttpGateway(
        base_url=BASE_URL,
        secret_key=SecretStr(FAKE_SECRET),
        transport=handler,
    )


async def test_create_session_sends_exact_money_and_discards_session_token() -> None:
    observed: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url == httpx.URL(f"{BASE_URL}/v1/sessions")
        assert request.headers["Authorization"] == f"Bearer {FAKE_SECRET}"
        observed.update(json.loads(request.content))
        return httpx.Response(
            200,
            headers={
                "Content-Type": "application/json",
                "X-Response-ID": "response-create-1",
            },
            json={
                "session_id": "session-1",
                "session_token": "provider-session-token-must-not-escape",
                "iframe_url": "https://sandbox.collect.prava.space/checkout?session=1",
                "order_id": "order-1",
                "expires_at": "2026-08-01T16:00:00Z",
            },
        )

    session = await _gateway(httpx.MockTransport(handler)).create_session(_request())

    assert observed == {
        "user_id": "user-123",
        "user_email": "talia@example.com",
        "total_amount": "69.99",
        "currency": "USD",
        "integration_type": "full_checkout",
        "callback_url": "https://api.wishtrace.example.com/v1/prava/return",
        "external_order_ref": "purchase-intent-123",
        "purchase_context": [
            {
                "merchant_details": {
                    "name": "Observed Merchant",
                    "url": "https://www.example.com",
                    "country_code_iso2": "US",
                },
                "product_details": [
                    {
                        "description": "Observed headset — Black",
                        "unit_price": "64.99",
                        "product_id": "variant-123",
                        "quantity": 1,
                    }
                ],
            }
        ],
    }
    assert session.session_id == "session-1"
    assert session.response_id == "response-create-1"
    assert session.expires_at == datetime(2026, 8, 1, 16, 0, tzinfo=UTC)
    assert "provider-session-token-must-not-escape" not in repr(session)
    assert "provider-session-token-must-not-escape" not in session.model_dump_json()


async def test_create_session_rejects_untrusted_hosted_url() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json={
                "session_id": "session-1",
                "session_token": "provider-session-token",
                "iframe_url": "https://attacker.example/steal",
                "order_id": "order-1",
                "expires_at": "2026-08-01T16:00:00Z",
            },
        )

    with pytest.raises(PravaGatewayError) as captured:
        await _gateway(httpx.MockTransport(handler)).create_session(_request())

    assert captured.value.code == "PRAVA_OUTCOME_UNKNOWN"
    assert captured.value.recoverable is True
    assert captured.value.outcome_unknown is True


async def test_create_timeout_is_unknown_and_must_not_be_blindly_retried() -> None:
    def timeout(request: httpx.Request) -> httpx.Response:
        raise httpx.ReadTimeout("provider may have created the session", request=request)

    with pytest.raises(PravaGatewayError) as captured:
        await _gateway(httpx.MockTransport(timeout)).create_session(_request())

    assert captured.value.code == "PRAVA_OUTCOME_UNKNOWN"
    assert captured.value.outcome_unknown is True
    assert captured.value.recoverable is True


async def test_create_server_error_is_unknown_after_ambiguous_write() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            503,
            headers={
                "Content-Type": "application/json",
                "X-Response-ID": "response-server-1",
            },
            json={"error": {"code": "TEMPORARY_FAILURE"}},
        )

    with pytest.raises(PravaGatewayError) as captured:
        await _gateway(httpx.MockTransport(handler)).create_session(_request())

    assert captured.value.code == "PRAVA_OUTCOME_UNKNOWN"
    assert captured.value.response_id == "response-server-1"
    assert captured.value.outcome_unknown is True


async def test_payment_result_keeps_credentials_secret_and_memory_only() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/v1/sessions/session-1/payment-result"
        return httpx.Response(
            200,
            headers={
                "Content-Type": "application/json",
                "X-Response-ID": "response-poll-1",
            },
            json={
                "session_id": "session-1",
                "order_id": "order-1",
                "status": "awaiting_result",
                "transactions": [
                    {
                        "txn_id": "txn-1",
                        "status": "awaiting_result",
                        "line_items": [
                            {
                                "txn_ref_id": "line-1",
                                "merchant_name": "Observed Merchant",
                                "merchant_url": "https://www.example.com",
                                "total_amount": "69.99",
                                "status": "awaiting_result",
                                "token": "test-token-redacted",
                                "dynamic_cvv": "123",
                                "expiry_month": "12",
                                "expiry_year": "2030",
                                "products": [],
                            }
                        ],
                    }
                ],
            },
        )

    result = await _gateway(httpx.MockTransport(handler)).get_payment_result("session-1")

    assert result.status == PravaPaymentStatus.AWAITING_RESULT
    assert result.response_id == "response-poll-1"
    assert len(result.credentials) == 1
    txn_ref_id, credential = result.credentials[0]
    assert txn_ref_id == "line-1"
    assert credential.token.get_secret_value() == "test-token-redacted"
    serialized = result.model_dump_json()
    assert "test-token-redacted" not in serialized
    assert "123" not in serialized
    assert "test-token-redacted" not in repr(result)


async def test_payment_result_rejects_partial_or_misplaced_credentials() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json={
                "session_id": "session-1",
                "status": "completed",
                "transactions": [
                    {
                        "txn_id": "txn-1",
                        "status": "completed",
                        "line_items": [
                            {
                                "txn_ref_id": "line-1",
                                "total_amount": "69.99",
                                "status": "completed",
                                "token": "test-token-redacted",
                                "dynamic_cvv": "123",
                                "expiry_month": "12",
                                "expiry_year": "2030",
                            }
                        ],
                    }
                ],
            },
        )

    with pytest.raises(PravaGatewayError) as captured:
        await _gateway(httpx.MockTransport(handler)).get_payment_result("session-1")

    assert captured.value.code == "PRAVA_RESPONSE_INVALID"


async def test_report_status_sends_only_documented_outcome_fields() -> None:
    observed: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/v1/sessions/session-1/report-status"
        observed.update(json.loads(request.content))
        return httpx.Response(
            200,
            headers={
                "Content-Type": "application/json",
                "X-Response-ID": "response-report-1",
            },
            json={
                "status": "confirmed",
                "txn_ref_id": "line-1",
                "txn_status": "DECLINED",
                "visa_confirmation": "SUCCESS",
            },
        )

    result = await _gateway(httpx.MockTransport(handler)).report_status(
        session_id="session-1",
        txn_ref_id="line-1",
        outcome=PravaReportOutcome.DECLINED,
    )

    assert observed == {
        "txn_ref_id": "line-1",
        "txn_status": "DECLINED",
        "txn_type": "PURCHASE",
    }
    assert result.status == "confirmed"
    assert result.visa_confirmation == "SUCCESS"
    assert result.response_id == "response-report-1"


async def test_provider_errors_are_safe_and_keep_only_machine_code() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            401,
            headers={
                "Content-Type": "application/json",
                "X-Response-ID": "response-auth-1",
            },
            json={
                "error": {
                    "code": "AUTH_1001",
                    "message": f"invalid provider key {FAKE_SECRET}",
                }
            },
        )

    with pytest.raises(PravaGatewayError) as captured:
        await _gateway(httpx.MockTransport(handler)).get_payment_result("session-1")

    error = captured.value
    assert error.code == "PRAVA_AUTHENTICATION_FAILED"
    assert error.provider_code == "AUTH_1001"
    assert error.response_id == "response-auth-1"
    assert FAKE_SECRET not in str(error)


def test_session_request_rejects_non_https_and_invalid_email() -> None:
    values = _request().model_dump()
    values["callback_url"] = "http://api.wishtrace.example.com/return"
    values["user_email"] = "not-an-email"

    with pytest.raises(ValidationError) as captured:
        PravaSessionRequest.model_validate(values)

    assert "URL must use HTTPS" in str(captured.value)
    assert "routable domain" in str(captured.value)


@pytest.mark.parametrize(
    "email",
    [
        "owner@wishtrace.local",
        "owner@wishtrace.test",
        "owner@wishtrace.example",
        "owner@wishtrace.demo",
        "owner@wishtrace.invalid",
        "owner@wishtrace.internal",
        "owner@wishtrace.nep",
    ],
)
def test_session_request_rejects_reserved_email_domains(email: str) -> None:
    values = _request().model_dump()
    values["user_email"] = email

    with pytest.raises(ValidationError, match="routable domain"):
        PravaSessionRequest.model_validate(values)


@pytest.mark.parametrize(
    "merchant_url",
    [
        "htttps://www.example.com",
        "https://merchant.example",
        "https://www.example.com/products/gift-card",
        "https://www.example.com?variant=1",
        "www.example.com",
    ],
)
def test_session_request_requires_bare_delegated_merchant_origin(
    merchant_url: str,
) -> None:
    values = _request().model_dump()
    values["merchant_url"] = merchant_url

    with pytest.raises(ValidationError, match="bare HTTPS origin"):
        PravaSessionRequest.model_validate(values)


async def test_provider_path_identifier_rejects_path_injection() -> None:
    with pytest.raises(ValueError, match="session_id is invalid"):
        await _gateway(httpx.MockTransport(lambda request: httpx.Response(200))).get_payment_result(
            "../sessions/other"
        )


def _mandate_request() -> PravaMandateSessionRequest:
    return PravaMandateSessionRequest(
        user_id="user-123",
        user_email="talia@example.com",
        total_minor=500,
        currency="USD",
        merchant_name="Jackbox Games",
        merchant_url="https://checkout.jackboxgames.com",
        merchant_country="US",
        product_description="The Jackbox Party Starter",
        product_unit_minor=500,
        external_product_id="variant-abc",
        quantity=1,
        callback_url="https://api.wishtrace.example.com/v1/prava/return",
        external_order_ref="occasion-123",
        recurring_frequency=PravaMandateFrequency.YEARLY,
        merchant_scope=PravaMandateScope.LISTED,
        max_charges=5,
        valid_until="2031-08-01T00:00:00Z",
    )


def test_mandate_request_rejects_reserved_email_and_product_path_origin() -> None:
    values = _mandate_request().model_dump()
    values["user_email"] = "owner@wishtrace.local"
    values["merchant_url"] = "https://www.example.com/products/gift-card"

    with pytest.raises(ValidationError) as captured:
        PravaMandateSessionRequest.model_validate(values)

    assert "routable domain" in str(captured.value)
    assert "bare HTTPS origin" in str(captured.value)


async def test_create_mandate_session_sends_setup_block_and_discards_token() -> None:
    observed: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url == httpx.URL(f"{BASE_URL}/v1/sessions")
        assert request.headers["Authorization"] == f"Bearer {FAKE_SECRET}"
        observed.update(json.loads(request.content))
        return httpx.Response(
            200,
            headers={
                "Content-Type": "application/json",
                "X-Response-ID": "response-mandate-create-1",
            },
            json={
                "session_id": "session-m1",
                "session_token": "provider-session-token-must-not-escape",
                "iframe_url": "https://sandbox.collect.prava.space/checkout?session=m1",
                "order_id": "order-m1",
                "expires_at": "2026-08-01T16:00:00Z",
            },
        )

    session = await _gateway(httpx.MockTransport(handler)).create_mandate_session(
        _mandate_request()
    )

    assert observed["mandate_setup"] == {
        "intent": "mandate_setup",
        "recurring_frequency": "yearly",
        "merchant_scope": "listed",
        "max_charges": 5,
        "valid_until": "2031-08-01T00:00:00Z",
    }
    assert observed["integration_type"] == "full_checkout"
    assert "authorize_only" not in observed
    assert session.session_id == "session-m1"
    assert session.hosted_url == "https://sandbox.collect.prava.space/checkout?session=m1"
    assert "provider-session-token-must-not-escape" not in session.model_dump_json()


async def test_create_mandate_session_rejects_checkout_contradiction() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            201,
            headers={
                "Content-Type": "application/json",
                "X-Response-ID": "response-mandate-create-without-authorization",
            },
            json={
                "session_id": "session-m1",
                "session_token": "token",
                "iframe_url": "https://sandbox.collect.prava.space/checkout?session=m1",
                "order_id": "order-m1",
                "expires_at": "2026-08-01T16:00:00Z",
                "authorizeOnly": False,
            },
        )

    with pytest.raises(PravaGatewayError) as captured:
        await _gateway(httpx.MockTransport(handler)).create_mandate_session(
            _mandate_request()
        )

    assert captured.value.code == "PRAVA_OUTCOME_UNKNOWN"
    assert captured.value.outcome_unknown is True
    assert (
        captured.value.response_id
        == "response-mandate-create-without-authorization"
    )


async def test_create_mandate_session_rejects_untrusted_hosted_url() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json={
                "session_id": "session-m1",
                "session_token": "token",
                "iframe_url": "https://evil.example/checkout",
                "order_id": "order-m1",
                "expires_at": "2026-08-01T16:00:00Z",
                "authorizeOnly": True,
            },
        )

    with pytest.raises(PravaGatewayError) as captured:
        await _gateway(httpx.MockTransport(handler)).create_mandate_session(_mandate_request())

    assert captured.value.code == "PRAVA_OUTCOME_UNKNOWN"
    assert captured.value.outcome_unknown is True


async def test_get_mandate_parses_guardrails() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/v1/mandates/mandate-1"
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json={
                "id": "mandate-1",
                "status": "active",
                "recurringFrequency": "yearly",
                "merchantScope": "listed",
                "merchantName": "Jackbox Games",
                "approvedAmount": "5.00",
                "currency": "USD",
                "createdAt": "2026-08-01T12:00:00Z",
                "validUntil": "2031-08-01T00:00:00Z",
                "chargeCount": 5,
            },
        )

    mandate = await _gateway(httpx.MockTransport(handler)).get_mandate("mandate-1")

    assert mandate.status == PravaMandateStatus.ACTIVE
    assert mandate.recurring_frequency == PravaMandateFrequency.YEARLY
    assert mandate.merchant_scope == PravaMandateScope.LISTED
    assert mandate.total_charges == 5


async def test_list_mandates_uses_customer_scope_and_parses_current_shape() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/v1/mandates"
        assert dict(request.url.params) == {
            "customer_id": "user-123",
            "standing_only": "true",
        }
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json={
                "mandates": [
                    {
                        "id": "mandate-1",
                        "status": "active",
                        "recurringFrequency": "yearly",
                        "merchantScope": "listed",
                        "merchantName": "Jackbox Games",
                        "approvedAmount": "5.00",
                        "currency": "USD",
                        "validUntil": "2031-08-01T00:00:00Z",
                        "createdAt": "2026-08-01T12:00:00Z",
                        "externalUserId": "user-123",
                    }
                ]
            },
        )

    mandates = await _gateway(httpx.MockTransport(handler)).list_mandates("user-123")

    assert len(mandates) == 1
    assert mandates[0].mandate_id == "mandate-1"
    assert mandates[0].merchant_name == "Jackbox Games"
    assert mandates[0].external_user_id == "user-123"


async def test_charge_mandate_mints_memory_only_credentials() -> None:
    observed: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/v1/mandates/mandate-1/charge"
        observed.update(json.loads(request.content))
        return httpx.Response(
            200,
            headers={
                "Content-Type": "application/json",
                "X-Response-ID": "response-charge-1",
            },
            json={
                "mandateId": "mandate-1",
                "transactionId": "charge-1",
                "orderId": "order-1",
                "status": "awaiting_result",
                "fetchStatus": "SUCCESS",
                "credentials": {
                    "token": "mandate-token-redacted",
                    "dynamicCvv": "599",
                    "expiryMonth": "12",
                    "expiryYear": "2027",
                },
            },
        )

    result = await _gateway(httpx.MockTransport(handler)).charge_mandate(
        mandate_id="mandate-1",
        amount_minor=500,
        reference="occasion-123-charge-1",
    )

    assert observed == {"amount": "5.00", "reference": "occasion-123-charge-1"}
    assert result.status == "awaiting_result"
    assert result.order_id == "order-1"
    assert result.credential is not None
    assert result.credential.token.get_secret_value() == "mandate-token-redacted"
    serialized = result.model_dump_json()
    assert "mandate-token-redacted" not in serialized
    assert "599" not in serialized


async def test_charge_mandate_over_cap_declines_without_credentials() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json={
                "mandateId": "mandate-1",
                "transactionId": "charge-2",
                "status": "failed",
                "errorCode": "THRESHOLD_EXCEEDED",
            },
        )

    result = await _gateway(httpx.MockTransport(handler)).charge_mandate(
        mandate_id="mandate-1",
        amount_minor=999999,
        reference="occasion-123-charge-2",
    )

    assert result.status == "failed"
    assert result.credential is None
    assert result.error_code == "THRESHOLD_EXCEEDED"


async def test_charge_mandate_rejects_credentials_on_failed_status() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json={
                "mandateId": "mandate-1",
                "transactionId": "charge-3",
                "status": "failed",
                "credentials": {
                    "token": "leaked-token",
                    "dynamicCvv": "599",
                    "expiryMonth": "12",
                    "expiryYear": "2027",
                },
            },
        )

    with pytest.raises(PravaGatewayError) as captured:
        await _gateway(httpx.MockTransport(handler)).charge_mandate(
            mandate_id="mandate-1",
            amount_minor=500,
            reference="occasion-123-charge-3",
        )

    assert captured.value.code == "PRAVA_OUTCOME_UNKNOWN"


async def test_report_mandate_charge_sends_documented_fields() -> None:
    observed: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/v1/mandates/mandate-1/charges/charge-1/report"
        observed.update(json.loads(request.content))
        return httpx.Response(
            200,
            headers={
                "Content-Type": "application/json",
                "X-Response-ID": "response-mandate-report-1",
            },
            json={
                "mandateId": "mandate-1",
                "transactionId": "charge-1",
                "orderId": "order-1",
                "status": "failed",
                "mandateStatus": "active",
                "visaConfirmation": "FAILURE",
            },
        )

    result = await _gateway(httpx.MockTransport(handler)).report_mandate_charge(
        mandate_id="mandate-1",
        charge_id="charge-1",
        outcome=PravaReportOutcome.DECLINED,
    )

    assert observed == {"txn_status": "DECLINED", "txn_type": "PURCHASE"}
    assert result.status == "failed"
    assert result.mandate_id == "mandate-1"
    assert result.charge_id == "charge-1"
    assert result.visa_confirmation == "FAILURE"
    assert result.response_id == "response-mandate-report-1"


async def test_mandate_charge_path_identifier_rejects_injection() -> None:
    with pytest.raises(ValueError, match="mandate_id is invalid"):
        await _gateway(
            httpx.MockTransport(lambda request: httpx.Response(200))
        ).charge_mandate(
            mandate_id="../mandates/other",
            amount_minor=500,
            reference="occasion-123-charge-1",
        )
