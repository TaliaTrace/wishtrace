import hashlib
import re
from datetime import UTC, datetime
from enum import StrEnum
from html.parser import HTMLParser
from typing import Any, Literal
from urllib.parse import urlsplit

import httpx
from pydantic import BaseModel, ConfigDict, Field, ValidationError

UCP_VERSION = "2026-04-08"
CATALOG_SEARCH = "dev.ucp.shopping.catalog.search"
CATALOG_LOOKUP = "dev.ucp.shopping.catalog.lookup"
CHECKOUT = "dev.ucp.shopping.checkout"
MAX_RESPONSE_BYTES = 2_000_000
SHOPIFY_GIFT_CARD_CATEGORY = "gid://shopify/TaxonomyCategory/gc"


class ProductKind(StrEnum):
    PHYSICAL = "PHYSICAL"
    DIGITAL = "DIGITAL"
    STORED_VALUE = "STORED_VALUE"


class AvailabilityState(StrEnum):
    AVAILABLE = "AVAILABLE"
    UNAVAILABLE = "UNAVAILABLE"
    UNKNOWN = "UNKNOWN"


class DeliveryState(StrEnum):
    UNKNOWN = "UNKNOWN"


class RejectionCode(StrEnum):
    UNSUPPORTED_CHECKOUT = "UNSUPPORTED_CHECKOUT"
    UNAVAILABLE = "UNAVAILABLE"
    MISSING_VARIANT = "MISSING_VARIANT"
    OVER_BUDGET = "OVER_BUDGET"
    EXPLICIT_DISLIKE = "EXPLICIT_DISLIKE"
    RECENTLY_ATTEMPTED = "RECENTLY_ATTEMPTED"


class Money(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    amount: int = Field(ge=0)
    currency: Literal["USD"]


class LiveCandidate(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    merchant_id: str
    merchant_name: str
    merchant_product_id: str
    merchant_variant_id: str | None
    sku: str | None
    title: str
    variant_title: str | None
    description: str | None
    product_url: str
    image_url: str | None
    price: Money
    availability: AvailabilityState
    selected_options: dict[str, str]
    categories: list[str]
    tags: list[str]
    product_kind: ProductKind
    checkout_supported: bool
    delivery: DeliveryState = DeliveryState.UNKNOWN
    source_timestamp: datetime
    source_mode: Literal["LIVE"] = "LIVE"

    @property
    def source_key(self) -> str:
        raw = "|".join(
            (
                self.merchant_id,
                self.merchant_product_id,
                self.merchant_variant_id or "missing-variant",
            )
        )
        return f"live_{hashlib.sha256(raw.encode()).hexdigest()[:24]}"


class CandidateRejection(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    candidate_source_key: str
    code: RejectionCode
    reason: str


class CandidateEvaluation(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    candidate: LiveCandidate
    rejection: CandidateRejection | None

    @property
    def eligible(self) -> bool:
        return self.rejection is None


class MerchantSearchResult(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    merchant_id: str
    merchant_name: str
    request_id: str | None
    profile_cache_compliant: bool
    candidates: list[LiveCandidate]
    source_timestamp: datetime


class MerchantGatewayError(Exception):
    def __init__(self, code: str, message: str, *, recoverable: bool = True) -> None:
        super().__init__(message)
        self.code = code
        self.safe_message = message
        self.recoverable = recoverable


class _UcpEntity(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    version: str
    spec: str
    schema_url: str | None = Field(default=None, alias="schema")
    transport: str | None = None
    endpoint: str | None = None


class _UcpProfileBody(BaseModel):
    model_config = ConfigDict(extra="allow")

    version: str
    services: dict[str, list[_UcpEntity]]
    capabilities: dict[str, list[_UcpEntity]]


class _UcpProfile(BaseModel):
    model_config = ConfigDict(extra="allow")

    ucp: _UcpProfileBody


class _RawDescription(BaseModel):
    model_config = ConfigDict(extra="ignore")

    plain: str | None = None
    html: str | None = None


class _RawMoney(BaseModel):
    model_config = ConfigDict(extra="ignore")

    amount: int = Field(ge=0)
    currency: Literal["USD"]


class _RawAvailability(BaseModel):
    model_config = ConfigDict(extra="ignore")

    available: bool


class _RawSelectedOption(BaseModel):
    model_config = ConfigDict(extra="ignore")

    name: str
    label: str


class _RawMedia(BaseModel):
    model_config = ConfigDict(extra="ignore")

    type: str
    url: str


class _RawCategory(BaseModel):
    model_config = ConfigDict(extra="ignore")

    value: str


class _RawVariant(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: str
    sku: str | None = None
    title: str
    description: _RawDescription | None = None
    price: _RawMoney
    availability: _RawAvailability
    options: list[_RawSelectedOption] = Field(default_factory=list)
    media: list[_RawMedia] = Field(default_factory=list)


class _RawProduct(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: str
    title: str
    description: _RawDescription | None = None
    url: str
    price_range: dict[str, _RawMoney]
    variants: list[_RawVariant] = Field(default_factory=list)
    media: list[_RawMedia] = Field(default_factory=list)
    categories: list[_RawCategory] = Field(default_factory=list)
    tags: list[Any] = Field(default_factory=list)


class _RawUcpResponse(BaseModel):
    model_config = ConfigDict(extra="allow")

    version: str


class _RawSearchContent(BaseModel):
    model_config = ConfigDict(extra="ignore")

    ucp: _RawUcpResponse
    products: list[_RawProduct]


class _TextExtractor(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.parts: list[str] = []

    def handle_data(self, data: str) -> None:
        value = data.strip()
        if value:
            self.parts.append(value)


class UcpMerchantGateway:
    def __init__(
        self,
        *,
        merchant_id: str,
        merchant_name: str,
        business_profile_url: str,
        allowed_endpoint_host: str,
        agent_profile_url: str,
        checkout_verified: bool,
        checkout_product_ids: frozenset[str] | None = None,
        digital_product_ids: frozenset[str] = frozenset(),
        transport: httpx.AsyncBaseTransport | None = None,
    ) -> None:
        if not _is_public_https_url(agent_profile_url):
            raise ValueError("The UCP agent profile must use public HTTPS.")
        if not _is_public_https_url(business_profile_url):
            raise ValueError("The UCP business profile must use public HTTPS.")
        self._merchant_id = merchant_id
        self._merchant_name = merchant_name
        self._business_profile_url = business_profile_url
        self._allowed_endpoint_host = allowed_endpoint_host.casefold()
        self._agent_profile_url = agent_profile_url
        self._checkout_verified = checkout_verified
        self._checkout_product_ids = checkout_product_ids
        self._digital_product_ids = digital_product_ids
        self._transport = transport

    async def search(self, *, query: str, budget_minor: int) -> MerchantSearchResult:
        if not query.strip():
            raise ValueError("query cannot be blank")
        if budget_minor <= 0:
            raise ValueError("budget_minor must be positive")

        timeout = httpx.Timeout(20.0, connect=10.0)
        async with httpx.AsyncClient(
            timeout=timeout,
            follow_redirects=False,
            transport=self._transport,
            headers={"User-Agent": "WishTrace/0.1 (+https://wishtrace.app)"},
        ) as client:
            profile_response = await _request(
                client,
                "GET",
                self._business_profile_url,
                headers={"Accept": "application/json"},
            )
            profile, profile_cache_compliant = _parse_profile(profile_response)
            endpoint, checkout_advertised = _resolve_profile(
                profile,
                allowed_endpoint_host=self._allowed_endpoint_host,
            )
            payload = {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "tools/call",
                "params": {
                    "name": "search_catalog",
                    "arguments": {
                        "meta": {"ucp-agent": {"profile": self._agent_profile_url}},
                        "catalog": {
                            "query": query.strip(),
                            "context": {"address_country": "US"},
                            "filters": {"price": {"max": budget_minor}},
                            "pagination": {"limit": 10},
                        },
                    },
                },
            }
            search_response = await _request(
                client,
                "POST",
                endpoint,
                headers={"Accept": "application/json, text/event-stream"},
                json=payload,
            )
        content = _parse_search_response(search_response)
        timestamp = datetime.now(UTC)
        candidates: list[LiveCandidate] = []
        seen_product_ids: set[str] = set()
        for product in content.products:
            if product.id in seen_product_ids:
                continue
            seen_product_ids.add(product.id)
            candidates.append(
                _normalize_product(
                    product,
                    merchant_id=self._merchant_id,
                    merchant_name=self._merchant_name,
                    checkout_supported=(
                        checkout_advertised
                        and self._checkout_verified
                        and (
                            self._checkout_product_ids is None
                            or product.id in self._checkout_product_ids
                        )
                    ),
                    digital_product_ids=self._digital_product_ids,
                    source_timestamp=timestamp,
                )
            )
        return MerchantSearchResult(
            merchant_id=self._merchant_id,
            merchant_name=self._merchant_name,
            request_id=search_response.headers.get("x-request-id"),
            profile_cache_compliant=profile_cache_compliant,
            candidates=candidates,
            source_timestamp=timestamp,
        )


async def _request(
    client: httpx.AsyncClient,
    method: str,
    url: str,
    **kwargs: Any,
) -> httpx.Response:
    try:
        response = await client.request(method, url, **kwargs)
    except (httpx.TimeoutException, httpx.NetworkError) as error:
        raise MerchantGatewayError(
            "MERCHANT_UNAVAILABLE",
            "The gift source is temporarily unavailable. Try again.",
        ) from error
    if 300 <= response.status_code < 400:
        raise MerchantGatewayError(
            "MERCHANT_REDIRECT_REJECTED",
            "The gift source changed its connection details. Try again later.",
        )
    if response.status_code >= 400:
        raise MerchantGatewayError(
            "MERCHANT_REQUEST_FAILED",
            "The gift source could not complete that request. Try again.",
        )
    if len(response.content) > MAX_RESPONSE_BYTES:
        raise MerchantGatewayError(
            "MERCHANT_RESPONSE_TOO_LARGE",
            "The gift source returned too much data. Try again later.",
        )
    return response


def _parse_profile(response: httpx.Response) -> tuple[_UcpProfile, bool]:
    content_type = response.headers.get("content-type", "").casefold()
    if "application/json" not in content_type:
        raise MerchantGatewayError(
            "MERCHANT_PROFILE_INVALID",
            "The gift source profile is not JSON.",
            recoverable=False,
        )
    cache_control = response.headers.get("cache-control", "").casefold()
    if any(directive in cache_control for directive in ("private", "no-store", "no-cache")):
        raise MerchantGatewayError(
            "MERCHANT_PROFILE_INVALID",
            "The gift source profile forbids safe profile reuse.",
            recoverable=False,
        )
    match = re.search(r"(?:^|,)\s*max-age=(\d+)", cache_control)
    cache_compliant = (
        "public" in cache_control and match is not None and int(match.group(1)) >= 60
    )
    try:
        return _UcpProfile.model_validate(response.json()), cache_compliant
    except (ValueError, ValidationError) as error:
        raise MerchantGatewayError(
            "MERCHANT_PROFILE_INVALID",
            "The gift source profile is invalid.",
            recoverable=False,
        ) from error


def _resolve_profile(
    profile: _UcpProfile,
    *,
    allowed_endpoint_host: str,
) -> tuple[str, bool]:
    if profile.ucp.version != UCP_VERSION:
        raise MerchantGatewayError(
            "MERCHANT_VERSION_UNSUPPORTED",
            "The gift source uses an unsupported commerce version.",
            recoverable=False,
        )
    for capability_name in (CATALOG_SEARCH, CATALOG_LOOKUP):
        entries = profile.ucp.capabilities.get(capability_name, [])
        matching = [entry for entry in entries if entry.version == UCP_VERSION]
        if not matching:
            raise MerchantGatewayError(
                "MERCHANT_CAPABILITY_MISSING",
                "The gift source does not support the required catalog operation.",
                recoverable=False,
            )
        for entry in matching:
            if urlsplit(entry.spec).hostname != "ucp.dev":
                raise MerchantGatewayError(
                    "MERCHANT_PROFILE_INVALID",
                    "The gift source profile has an invalid capability authority.",
                    recoverable=False,
                )

    services = profile.ucp.services.get("dev.ucp.shopping", [])
    mcp = next(
        (
            service
            for service in services
            if service.version == UCP_VERSION and service.transport == "mcp"
        ),
        None,
    )
    if mcp is None or mcp.endpoint is None:
        raise MerchantGatewayError(
            "MERCHANT_TRANSPORT_MISSING",
            "The gift source does not expose the required catalog connection.",
            recoverable=False,
        )
    parsed_endpoint = urlsplit(mcp.endpoint)
    if (
        parsed_endpoint.scheme != "https"
        or parsed_endpoint.hostname is None
        or parsed_endpoint.hostname.casefold() != allowed_endpoint_host
    ):
        raise MerchantGatewayError(
            "MERCHANT_ENDPOINT_REJECTED",
            "The gift source advertised an untrusted endpoint.",
            recoverable=False,
        )
    checkout_supported = any(
        item.version == UCP_VERSION
        for item in profile.ucp.capabilities.get(CHECKOUT, [])
    )
    return mcp.endpoint, checkout_supported


def _parse_search_response(response: httpx.Response) -> _RawSearchContent:
    content_type = response.headers.get("content-type", "").casefold()
    if "application/json" not in content_type:
        raise MerchantGatewayError(
            "MERCHANT_RESPONSE_INVALID",
            "The gift source returned an invalid catalog response.",
        )
    try:
        payload = response.json()
        if not isinstance(payload, dict):
            raise ValueError
        error = payload.get("error")
        if error is not None:
            raise MerchantGatewayError(
                "MERCHANT_PROTOCOL_ERROR",
                "The gift source rejected the catalog request. Try again.",
            )
        result = payload["result"]
        if not isinstance(result, dict):
            raise ValueError
        content = _RawSearchContent.model_validate(result["structuredContent"])
    except MerchantGatewayError:
        raise
    except (KeyError, TypeError, ValueError, ValidationError) as error:
        raise MerchantGatewayError(
            "MERCHANT_RESPONSE_INVALID",
            "The gift source returned an invalid catalog response.",
        ) from error
    if content.ucp.version != UCP_VERSION:
        raise MerchantGatewayError(
            "MERCHANT_VERSION_UNSUPPORTED",
            "The gift source returned an unsupported commerce version.",
            recoverable=False,
        )
    return content


def _normalize_product(
    product: _RawProduct,
    *,
    merchant_id: str,
    merchant_name: str,
    checkout_supported: bool,
    digital_product_ids: frozenset[str] = frozenset(),
    source_timestamp: datetime,
) -> LiveCandidate:
    available = [variant for variant in product.variants if variant.availability.available]
    selected = min(available, key=lambda item: (item.price.amount, item.id)) if available else None
    fallback = min(
        product.variants,
        key=lambda item: (item.price.amount, item.id),
        default=None,
    )
    variant = selected or fallback
    minimum = product.price_range.get("min")
    if minimum is None:
        raise MerchantGatewayError(
            "MERCHANT_RESPONSE_INVALID",
            "The gift source omitted a required price.",
        )
    price = variant.price if variant is not None else minimum
    categories = [category.value for category in product.categories]
    if SHOPIFY_GIFT_CARD_CATEGORY in categories:
        kind = ProductKind.STORED_VALUE
    elif product.id in digital_product_ids:
        kind = ProductKind.DIGITAL
    else:
        kind = ProductKind.PHYSICAL
    image_url = _first_https_image(
        (variant.media if variant is not None else []) + product.media
    )
    raw_description = (
        variant.description
        if variant is not None and variant.description is not None
        else product.description
    )
    description = _plain_description(raw_description)
    return LiveCandidate(
        merchant_id=merchant_id,
        merchant_name=merchant_name,
        merchant_product_id=product.id,
        merchant_variant_id=variant.id if variant is not None else None,
        sku=variant.sku if variant is not None else None,
        title=product.title,
        variant_title=variant.title if variant is not None else None,
        description=description,
        product_url=_require_https(product.url),
        image_url=image_url,
        price=Money(amount=price.amount, currency=price.currency),
        availability=(
            AvailabilityState.AVAILABLE
            if selected is not None
            else AvailabilityState.UNAVAILABLE
            if fallback is not None
            else AvailabilityState.UNKNOWN
        ),
        selected_options=(
            {option.name: option.label for option in variant.options}
            if variant is not None
            else {}
        ),
        categories=categories,
        tags=_normalize_tags(product.tags),
        product_kind=kind,
        checkout_supported=checkout_supported,
        source_timestamp=source_timestamp,
    )


def evaluate_candidates(
    candidates: list[LiveCandidate],
    *,
    budget_minor: int,
    dislikes: list[str],
    allow_stored_value_products: bool,
) -> list[CandidateEvaluation]:
    if budget_minor <= 0:
        raise ValueError("budget_minor must be positive")
    normalized_dislikes = [item.strip().casefold() for item in dislikes if item.strip()]
    evaluations: list[CandidateEvaluation] = []
    for candidate in candidates:
        rejection: CandidateRejection | None = None
        if not candidate.checkout_supported or (
            candidate.product_kind is ProductKind.STORED_VALUE
            and not allow_stored_value_products
        ):
            reason = (
                "Stored-value checkout has not been verified for this merchant."
                if candidate.product_kind is ProductKind.STORED_VALUE
                else "This merchant does not advertise a supported checkout path."
            )
            rejection = CandidateRejection(
                candidate_source_key=candidate.source_key,
                code=RejectionCode.UNSUPPORTED_CHECKOUT,
                reason=reason,
            )
        elif candidate.availability is not AvailabilityState.AVAILABLE:
            rejection = CandidateRejection(
                candidate_source_key=candidate.source_key,
                code=RejectionCode.UNAVAILABLE,
                reason="The selected variant is not currently available.",
            )
        elif candidate.merchant_variant_id is None:
            rejection = CandidateRejection(
                candidate_source_key=candidate.source_key,
                code=RejectionCode.MISSING_VARIANT,
                reason="An exact purchasable variant is required.",
            )
        elif candidate.price.amount > budget_minor:
            rejection = CandidateRejection(
                candidate_source_key=candidate.source_key,
                code=RejectionCode.OVER_BUDGET,
                reason="The item price exceeds the gift budget before tax and shipping.",
            )
        else:
            evidence = " ".join(
                (
                    candidate.title,
                    candidate.variant_title or "",
                    *candidate.categories,
                    *candidate.tags,
                )
            ).casefold()
            matched = next((item for item in normalized_dislikes if item in evidence), None)
            if matched is not None:
                rejection = CandidateRejection(
                    candidate_source_key=candidate.source_key,
                    code=RejectionCode.EXPLICIT_DISLIKE,
                    reason=f"This item conflicts with the saved exclusion: {matched}.",
                )
        evaluations.append(CandidateEvaluation(candidate=candidate, rejection=rejection))
    return evaluations


def _plain_description(value: _RawDescription | None) -> str | None:
    if value is None:
        return None
    if value.plain:
        text = value.plain
    elif value.html:
        parser = _TextExtractor()
        parser.feed(value.html)
        text = " ".join(parser.parts)
    else:
        return None
    normalized = re.sub(r"\s+", " ", text).strip()
    return normalized[:800] or None


def _first_https_image(media: list[_RawMedia]) -> str | None:
    for item in media:
        if item.type == "image" and urlsplit(item.url).scheme == "https":
            return item.url
    return None


def _require_https(url: str) -> str:
    if not _is_public_https_url(url):
        raise MerchantGatewayError(
            "MERCHANT_RESPONSE_INVALID",
            "The gift source returned an insecure product link.",
        )
    return url


def _is_public_https_url(url: str) -> bool:
    parsed = urlsplit(url)
    return (
        parsed.scheme == "https"
        and parsed.hostname is not None
        and parsed.username is None
        and parsed.password is None
        and not parsed.fragment
    )


def _normalize_tags(values: list[Any]) -> list[str]:
    normalized: list[str] = []
    for value in values:
        if isinstance(value, str) and value.strip():
            normalized.append(value.strip())
        elif isinstance(value, dict):
            candidate = value.get("value")
            if isinstance(candidate, str) and candidate.strip():
                normalized.append(candidate.strip())
    return normalized
