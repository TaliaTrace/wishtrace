import asyncio
import os
import uuid
from datetime import UTC, datetime, timedelta

import pytest
from pydantic import ValidationError

from app.merchant_browser import (
    JACKBOX_VARIANT_GID,
    BillingContact,
    MerchantCheckoutOutcome,
    MerchantCheckoutResult,
    MerchantQuote,
    MerchantQuoteRequest,
    _extract_order_id,
    _is_region_code,
    _money_minor,
    _numeric_variant_id,
    _PlaywrightLoopThread,
)

QUIPLASH_2_URL = (
    "https://checkout.jackboxgames.com/products/quiplash-2-interlashional"
)
QUIPLASH_2_VARIANT = "gid://shopify/ProductVariant/40190131404934"


def _billing() -> BillingContact:
    return BillingContact(
        email="checkout-test@example.com",
        first_name="Test",
        last_name="Buyer",
        address_line1="123 Test Street",
        address_line2=None,
        city="Seattle",
        region="Washington",
        postal_code="98101",
        country_code="US",
        phone="+1 202 555 0100",
    )


def test_billing_details_are_absent_from_repr() -> None:
    billing = _billing()
    request = MerchantQuoteRequest(
        purchase_intent_id=uuid.uuid4(),
        product_url=(
            "https://checkout.jackboxgames.com/products/"
            "jackbox-games-gift-card-5"
        ),
        merchant_variant_id=JACKBOX_VARIANT_GID,
        expected_item_minor=500,
        currency="USD",
        billing=billing,
    )

    assert "checkout-test" not in repr(billing)
    assert "123 Test Street" not in repr(billing)
    assert "checkout-test" not in repr(request)
    assert "123 Test Street" not in repr(request)


def test_quote_accepts_allowlisted_digital_game_pair() -> None:
    request = MerchantQuoteRequest(
        purchase_intent_id=uuid.uuid4(),
        product_url=QUIPLASH_2_URL,
        merchant_variant_id=QUIPLASH_2_VARIANT,
        expected_item_minor=999,
        currency="USD",
        billing=_billing(),
    )

    assert request.product_url == QUIPLASH_2_URL
    assert request.merchant_variant_id == QUIPLASH_2_VARIANT


def test_quote_rejects_cross_product_variant_pair() -> None:
    with pytest.raises(ValidationError, match="identify one product"):
        MerchantQuoteRequest(
            purchase_intent_id=uuid.uuid4(),
            product_url=QUIPLASH_2_URL,
            merchant_variant_id=JACKBOX_VARIANT_GID,
            expected_item_minor=999,
            currency="USD",
            billing=_billing(),
        )


@pytest.mark.parametrize(
    "product_url",
    [
        "http://checkout.jackboxgames.com/products/jackbox-games-gift-card-5",
        "https://attacker.example/products/jackbox-games-gift-card-5",
        "https://checkout.jackboxgames.com.attacker.example/products/jackbox-games-gift-card-5",
        "https://user:password@checkout.jackboxgames.com/products/jackbox-games-gift-card-5",
        "https://checkout.jackboxgames.com/cart",
    ],
)
def test_quote_rejects_untrusted_product_url(product_url: str) -> None:
    with pytest.raises(ValidationError, match="allowlisted Jackbox"):
        MerchantQuoteRequest(
            purchase_intent_id=uuid.uuid4(),
            product_url=product_url,
            merchant_variant_id=JACKBOX_VARIANT_GID,
            expected_item_minor=500,
            currency="USD",
            billing=_billing(),
        )


def test_billing_rejects_invalid_country_code() -> None:
    with pytest.raises(ValidationError, match="two-letter ISO code"):
        _billing().model_copy(update={"country_code": "U1"}).model_validate(
            _billing().model_dump() | {"country_code": "U1"}
        )


def test_region_code_uses_shopify_option_value() -> None:
    assert _is_region_code("WA") is True
    assert _is_region_code("Washington") is False


def test_shopify_variant_id_is_reduced_to_numeric_value() -> None:
    assert (
        _numeric_variant_id(JACKBOX_VARIANT_GID)
        == "39783705149574"
    )


def test_money_parser_uses_exact_minor_units() -> None:
    assert _money_minor("Total\nUSD\n$1,234.56") == 123456


def test_quote_requires_components_to_equal_total() -> None:
    now = datetime.now(UTC)
    with pytest.raises(ValidationError, match="components"):
        MerchantQuote(
            item_minor=500,
            shipping_minor=0,
            tax_minor=100,
            total_minor=500,
            currency="USD",
            delivery_summary="Digital delivery by merchant email",
            quoted_at=now,
            expires_at=now + timedelta(minutes=12),
        )


def test_verified_order_requires_merchant_order_id() -> None:
    with pytest.raises(ValidationError, match="merchant order ID"):
        MerchantCheckoutResult(
            outcome=MerchantCheckoutOutcome.ORDER_VERIFIED,
            order_id=None,
            amount_minor=500,
            currency="USD",
            reason_code="MERCHANT_ORDER_VERIFIED",
        )


def test_decline_cannot_carry_order_id() -> None:
    with pytest.raises(ValidationError, match="Only ORDER_VERIFIED"):
        MerchantCheckoutResult(
            outcome=MerchantCheckoutOutcome.DECLINED,
            order_id="order-123",
            amount_minor=500,
            currency="USD",
            reason_code="MERCHANT_PAYMENT_DECLINED",
        )


def test_order_id_is_extracted_only_from_confirmation_copy() -> None:
    assert _extract_order_id("Thank you. Order #HX-12345") == "HX-12345"
    assert _extract_order_id("Your cart reference is abc") is None


@pytest.mark.skipif(os.name != "nt", reason="Windows event-loop boundary")
def test_playwright_uses_proactor_beside_psycopg_selector_loop() -> None:
    loop_thread = _PlaywrightLoopThread()

    async def running_loop_name() -> str:
        return type(asyncio.get_running_loop()).__name__

    try:
        loop_name = loop_thread.submit(running_loop_name()).result(timeout=5)
    finally:
        loop_thread.stop()

    assert loop_name == "ProactorEventLoop"
