import json
from datetime import UTC, datetime

import httpx
import pytest
from pydantic import SecretStr, ValidationError

from app.prava import (
    PravaGatewayError,
    PravaHttpGateway,
    PravaPaymentStatus,
    PravaReportOutcome,
    PravaSessionRequest,
)

BASE_URL = "https://sandbox.api.prava.space"
FAKE_SECRET = "sk_test_fixture_value_not_a_real_key"


def _request() -> PravaSessionRequest:
    return PravaSessionRequest(
        user_id="user-123",
        user_email="talia@example.com",
        total_minor=6999,
        currency="USD",
        merchant_name="Observed Merchant",
        merchant_url="https://merchant.example",
        merchant_country="US",
        product_description="Observed headset — Black",
        product_unit_minor=6499,
        external_product_id="variant-123",
        quantity=1,
        callback_url="https://api.wishtrace.example/v1/prava/return",
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
        "callback_url": "https://api.wishtrace.example/v1/prava/return",
        "external_order_ref": "purchase-intent-123",
        "purchase_context": [
            {
                "merchant_details": {
                    "name": "Observed Merchant",
                    "url": "https://merchant.example",
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
                                "merchant_url": "https://merchant.example",
                                "total_amount": "69.99",
                                "status": "awaiting_result",
                                "token": "4111111111111111",
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
    assert credential.token.get_secret_value() == "4111111111111111"
    serialized = result.model_dump_json()
    assert "4111111111111111" not in serialized
    assert "123" not in serialized
    assert "4111111111111111" not in repr(result)


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
                                "token": "4111111111111111",
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
    values["callback_url"] = "http://api.wishtrace.example/return"
    values["user_email"] = "not-an-email"

    with pytest.raises(ValidationError) as captured:
        PravaSessionRequest.model_validate(values)

    assert "URL must use HTTPS" in str(captured.value)
    assert "valid email" in str(captured.value)


async def test_provider_path_identifier_rejects_path_injection() -> None:
    with pytest.raises(ValueError, match="session_id is invalid"):
        await _gateway(httpx.MockTransport(lambda request: httpx.Response(200))).get_payment_result(
            "../sessions/other"
        )
