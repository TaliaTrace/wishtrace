import json
from datetime import UTC, datetime
from typing import Any

import httpx
import pytest

from app.commerce import (
    AvailabilityState,
    DeliveryState,
    LiveCandidate,
    MerchantGatewayError,
    Money,
    ProductKind,
    RejectionCode,
    UcpMerchantGateway,
    evaluate_candidates,
)

UCP_VERSION = "2026-04-08"
PROFILE_URL = "https://merchant.example/.well-known/ucp"
MCP_URL = "https://merchant-mcp.example/api/ucp/mcp"
AGENT_PROFILE_URL = "https://wishtrace.example/.well-known/ucp"


def _profile(*, endpoint: str = MCP_URL) -> dict[str, Any]:
    capability = {
        "version": UCP_VERSION,
        "spec": f"https://ucp.dev/{UCP_VERSION}/specification/catalog/search",
    }
    return {
        "ucp": {
            "version": UCP_VERSION,
            "services": {
                "dev.ucp.shopping": [
                    {
                        "version": UCP_VERSION,
                        "spec": f"https://ucp.dev/{UCP_VERSION}/specification/overview",
                        "transport": "mcp",
                        "endpoint": endpoint,
                    }
                ]
            },
            "capabilities": {
                "dev.ucp.shopping.catalog.search": [capability],
                "dev.ucp.shopping.catalog.lookup": [capability],
                "dev.ucp.shopping.checkout": [capability],
            },
        }
    }


def _product(*, stored_value: bool = False) -> dict[str, Any]:
    return {
        "id": "gid://shopify/Product/100",
        "title": "Observed gaming headset",
        "description": {"html": "<p>Merchant supplied description.</p>"},
        "url": "https://merchant.example/products/headset",
        "price_range": {
            "min": {"amount": 5000, "currency": "USD"},
            "max": {"amount": 9000, "currency": "USD"},
        },
        "variants": [
            {
                "id": "gid://shopify/ProductVariant/unavailable-cheap",
                "sku": "HEADSET-OLD",
                "title": "Old color",
                "price": {"amount": 5000, "currency": "USD"},
                "availability": {"available": False},
            },
            {
                "id": "gid://shopify/ProductVariant/black",
                "sku": "HEADSET-BLACK",
                "title": "Black",
                "price": {"amount": 7000, "currency": "USD"},
                "availability": {"available": True},
                "options": [{"name": "Color", "label": "Black"}],
                "media": [{"type": "image", "url": "https://cdn.example/black.png"}],
            },
            {
                "id": "gid://shopify/ProductVariant/white",
                "sku": "HEADSET-WHITE",
                "title": "White",
                "price": {"amount": 9000, "currency": "USD"},
                "availability": {"available": True},
            },
        ],
        "categories": (
            [{"value": "gid://shopify/TaxonomyCategory/gc"}]
            if stored_value
            else [{"value": "gaming-headsets"}]
        ),
        "tags": ["Gaming", {"value": "Audio"}],
    }


def _gateway(
    transport: httpx.AsyncBaseTransport,
    *,
    checkout_verified: bool = False,
) -> UcpMerchantGateway:
    return UcpMerchantGateway(
        merchant_id="merchant-us",
        merchant_name="Merchant US",
        business_profile_url=PROFILE_URL,
        allowed_endpoint_host="merchant-mcp.example",
        agent_profile_url=AGENT_PROFILE_URL,
        checkout_verified=checkout_verified,
        transport=transport,
    )


async def test_search_negotiates_profile_and_normalizes_exact_live_variant() -> None:
    observed_payload: dict[str, Any] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url == httpx.URL(PROFILE_URL):
            return httpx.Response(
                200,
                headers={"Content-Type": "application/json"},
                json=_profile(),
            )
        assert request.url == httpx.URL(MCP_URL)
        observed_payload.update(json.loads(request.content))
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json", "X-Request-ID": "req-live-1"},
            json={
                "jsonrpc": "2.0",
                "id": 1,
                "result": {
                    "structuredContent": {
                        "ucp": {"version": UCP_VERSION},
                        "products": [_product()],
                    }
                },
            },
        )

    result = await _gateway(httpx.MockTransport(handler)).search(
        query="gaming headset",
        budget_minor=8000,
    )

    arguments = observed_payload["params"]["arguments"]
    assert arguments["meta"]["ucp-agent"]["profile"] == AGENT_PROFILE_URL
    assert arguments["catalog"] == {
        "query": "gaming headset",
        "context": {"address_country": "US"},
        "filters": {"price": {"max": 8000}},
        "pagination": {"limit": 10},
    }
    assert result.request_id == "req-live-1"
    assert result.profile_cache_compliant is False
    assert len(result.candidates) == 1
    candidate = result.candidates[0]
    assert candidate.merchant_variant_id == "gid://shopify/ProductVariant/black"
    assert candidate.sku == "HEADSET-BLACK"
    assert candidate.price == Money(amount=7000, currency="USD")
    assert candidate.availability == AvailabilityState.AVAILABLE
    assert candidate.selected_options == {"Color": "Black"}
    assert candidate.image_url == "https://cdn.example/black.png"
    assert candidate.description == "Merchant supplied description."
    assert candidate.tags == ["Gaming", "Audio"]
    assert candidate.delivery == DeliveryState.UNKNOWN
    assert candidate.source_mode == "LIVE"
    assert candidate.checkout_supported is False

    verified_result = await _gateway(
        httpx.MockTransport(handler),
        checkout_verified=True,
    ).search(query="gaming headset", budget_minor=8000)
    assert verified_result.candidates[0].checkout_supported is True


async def test_search_records_compliant_business_profile_cache_policy() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url == httpx.URL(PROFILE_URL):
            return httpx.Response(
                200,
                headers={
                    "Content-Type": "application/json",
                    "Cache-Control": "public, max-age=300",
                },
                json=_profile(),
            )
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json={
                "result": {
                    "structuredContent": {
                        "ucp": {"version": UCP_VERSION},
                        "products": [],
                    }
                }
            },
        )

    result = await _gateway(httpx.MockTransport(handler)).search(
        query="gaming",
        budget_minor=5000,
    )

    assert result.profile_cache_compliant is True


async def test_search_rejects_untrusted_endpoint_and_explicit_no_store() -> None:
    def untrusted_endpoint(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            200,
            headers={"Content-Type": "application/json"},
            json=_profile(endpoint="https://attacker.example/mcp"),
        )

    with pytest.raises(MerchantGatewayError) as endpoint_error:
        await _gateway(httpx.MockTransport(untrusted_endpoint)).search(
            query="gaming",
            budget_minor=5000,
        )
    assert endpoint_error.value.code == "MERCHANT_ENDPOINT_REJECTED"

    def no_store(request: httpx.Request) -> httpx.Response:
        del request
        return httpx.Response(
            200,
            headers={
                "Content-Type": "application/json",
                "Cache-Control": "no-store",
            },
            json=_profile(),
        )

    with pytest.raises(MerchantGatewayError) as cache_error:
        await _gateway(httpx.MockTransport(no_store)).search(
            query="gaming",
            budget_minor=5000,
        )
    assert cache_error.value.code == "MERCHANT_PROFILE_INVALID"


def _candidate(**changes: Any) -> LiveCandidate:
    values: dict[str, Any] = {
        "merchant_id": "merchant-us",
        "merchant_name": "Merchant US",
        "merchant_product_id": "product-1",
        "merchant_variant_id": "variant-1",
        "sku": "SKU-1",
        "title": "Gaming headset",
        "variant_title": "Black",
        "description": None,
        "product_url": "https://merchant.example/products/headset",
        "image_url": None,
        "price": Money(amount=7000, currency="USD"),
        "availability": AvailabilityState.AVAILABLE,
        "selected_options": {"Color": "Black"},
        "categories": ["gaming-headsets"],
        "tags": ["Gaming"],
        "product_kind": ProductKind.PHYSICAL,
        "checkout_supported": True,
        "source_timestamp": datetime(2026, 8, 1, tzinfo=UTC),
    }
    values.update(changes)
    return LiveCandidate.model_validate(values)


@pytest.mark.parametrize(
    ("candidate", "budget_minor", "dislikes", "allow_stored_value", "expected"),
    [
        (
            _candidate(
                checkout_supported=False,
                availability=AvailabilityState.UNAVAILABLE,
            ),
            5000,
            [],
            False,
            RejectionCode.UNSUPPORTED_CHECKOUT,
        ),
        (
            _candidate(product_kind=ProductKind.STORED_VALUE),
            9000,
            [],
            False,
            RejectionCode.UNSUPPORTED_CHECKOUT,
        ),
        (
            _candidate(availability=AvailabilityState.UNAVAILABLE),
            9000,
            [],
            False,
            RejectionCode.UNAVAILABLE,
        ),
        (
            _candidate(merchant_variant_id=None),
            9000,
            [],
            False,
            RejectionCode.MISSING_VARIANT,
        ),
        (
            _candidate(),
            6000,
            [],
            False,
            RejectionCode.OVER_BUDGET,
        ),
        (
            _candidate(),
            9000,
            ["headset"],
            False,
            RejectionCode.EXPLICIT_DISLIKE,
        ),
        (_candidate(), 9000, ["clutter"], False, None),
        (_candidate(product_kind=ProductKind.STORED_VALUE), 9000, [], True, None),
    ],
)
def test_candidate_rejections_use_locked_deterministic_order(
    candidate: LiveCandidate,
    budget_minor: int,
    dislikes: list[str],
    allow_stored_value: bool,
    expected: RejectionCode | None,
) -> None:
    [evaluation] = evaluate_candidates(
        [candidate],
        budget_minor=budget_minor,
        dislikes=dislikes,
        allow_stored_value_products=allow_stored_value,
    )

    assert (evaluation.rejection.code if evaluation.rejection else None) == expected
