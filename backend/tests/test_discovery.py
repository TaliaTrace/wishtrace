import uuid
from datetime import UTC, datetime

from httpx import ASGITransport, AsyncClient
from pydantic import SecretStr

from app.auth import (
    AuthenticatedUser,
    ChallengeResponse,
    GoogleExchangeRequest,
    SessionResponse,
)
from app.commerce import (
    AvailabilityState,
    CandidateEvaluation,
    LiveCandidate,
    MerchantGatewayError,
    MerchantSearchResult,
    Money,
    ProductKind,
)
from app.config import Settings
from app.database import DatabaseProbe
from app.discovery import (
    DiscoveryCandidateResponse,
    DiscoveryContext,
    DiscoveryCreate,
    DiscoveryOperations,
    DiscoveryResponse,
    DiscoveryService,
    DiscoveryStore,
)
from app.errors import ApiError
from app.main import create_app


class RecordingStore(DiscoveryStore):
    def __init__(self, context: DiscoveryContext) -> None:
        self.context = context
        self.user_ids: list[uuid.UUID] = []
        self.search_query: str | None = None
        self.evaluations: list[CandidateEvaluation] = []
        self.persisted: DiscoveryResponse | None = None

    async def load_context(
        self,
        user_id: uuid.UUID,
        recipient_id: uuid.UUID,
        occasion_id: uuid.UUID,
    ) -> DiscoveryContext:
        self.user_ids.append(user_id)
        assert recipient_id == self.context.recipient_id
        assert occasion_id == self.context.occasion_id
        return self.context

    async def persist_completed(
        self,
        *,
        user_id: uuid.UUID,
        context: DiscoveryContext,
        search_query: str,
        merchant_result: MerchantSearchResult,
        evaluations: list[CandidateEvaluation],
    ) -> DiscoveryResponse:
        self.user_ids.append(user_id)
        self.search_query = search_query
        self.evaluations = evaluations
        self.persisted = _response_from_evaluations(context, merchant_result, evaluations)
        return self.persisted

    async def get(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> DiscoveryResponse:
        self.user_ids.append(user_id)
        if self.persisted is None or self.persisted.id != discovery_id:
            raise AssertionError("unexpected discovery")
        return self.persisted


class RecordingMerchant:
    def __init__(self, candidates: list[LiveCandidate]) -> None:
        self.candidates = candidates
        self.queries: list[tuple[str, int]] = []
        self.error: MerchantGatewayError | None = None

    async def search(self, *, query: str, budget_minor: int) -> MerchantSearchResult:
        self.queries.append((query, budget_minor))
        if self.error is not None:
            raise self.error
        now = datetime(2026, 8, 1, 17, 0, tzinfo=UTC)
        return MerchantSearchResult(
            merchant_id="hyperx-us",
            merchant_name="HyperX US",
            request_id="merchant-request-1",
            profile_cache_compliant=False,
            candidates=self.candidates,
            source_timestamp=now,
        )


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


class StaticDiscovery(DiscoveryOperations):
    def __init__(self, response: DiscoveryResponse) -> None:
        self.response = response
        self.user_ids: list[uuid.UUID] = []

    async def create(
        self,
        user_id: uuid.UUID,
        body: DiscoveryCreate,
    ) -> DiscoveryResponse:
        self.user_ids.append(user_id)
        assert body.recipient_id == self.response.recipient_id
        assert body.occasion_id == self.response.occasion_id
        return self.response

    async def get(
        self,
        user_id: uuid.UUID,
        discovery_id: uuid.UUID,
    ) -> DiscoveryResponse:
        self.user_ids.append(user_id)
        assert discovery_id == self.response.id
        return self.response


def _candidate(
    *,
    product_id: str = "product-1",
    amount: int = 7000,
    product_kind: ProductKind = ProductKind.PHYSICAL,
) -> LiveCandidate:
    return LiveCandidate(
        merchant_id="hyperx-us",
        merchant_name="HyperX US",
        merchant_product_id=product_id,
        merchant_variant_id=f"{product_id}-black",
        sku=f"{product_id}-sku",
        title="Observed gaming headset",
        variant_title="Black",
        description="Observed description",
        product_url=f"https://merchant.example/products/{product_id}",
        image_url=None,
        price=Money(amount=amount, currency="USD"),
        availability=AvailabilityState.AVAILABLE,
        selected_options={"Color": "Black"},
        categories=["gaming-headsets"],
        tags=["Gaming"],
        product_kind=product_kind,
        checkout_supported=True,
        source_timestamp=datetime(2026, 8, 1, 17, 0, tzinfo=UTC),
    )


def _context() -> DiscoveryContext:
    return DiscoveryContext(
        recipient_id=uuid.uuid4(),
        occasion_id=uuid.uuid4(),
        interests=["Fitness", "Gaming"],
        dislikes=["pink"],
        budget_minor=8000,
        currency="USD",
        previous_product_ids=frozenset(),
    )


def _response_from_evaluations(
    context: DiscoveryContext,
    merchant_result: MerchantSearchResult,
    evaluations: list[CandidateEvaluation],
) -> DiscoveryResponse:
    return DiscoveryResponse(
        id=uuid.uuid4(),
        recipient_id=context.recipient_id,
        occasion_id=context.occasion_id,
        status="COMPLETED",
        merchant_id=merchant_result.merchant_id,
        merchant_name=merchant_result.merchant_name,
        search_query="gaming headset",
        budget_minor=context.budget_minor,
        currency="USD",
        source_request_id=merchant_result.request_id,
        profile_cache_compliant=merchant_result.profile_cache_compliant,
        source_timestamp=merchant_result.source_timestamp,
        candidates=[
            DiscoveryCandidateResponse(
                id=uuid.uuid4(),
                merchant_product_id=item.candidate.merchant_product_id,
                merchant_variant_id=item.candidate.merchant_variant_id,
                sku=item.candidate.sku,
                title=item.candidate.title,
                variant_title=item.candidate.variant_title,
                description=item.candidate.description,
                product_url=item.candidate.product_url,
                image_url=item.candidate.image_url,
                price_minor=item.candidate.price.amount,
                currency="USD",
                availability=item.candidate.availability,
                selected_options=item.candidate.selected_options,
                categories=item.candidate.categories,
                tags=item.candidate.tags,
                product_kind=item.candidate.product_kind,
                checkout_supported=item.candidate.checkout_supported,
                delivery=item.candidate.delivery,
                source_timestamp=item.candidate.source_timestamp,
                source_mode="LIVE",
                eligible=item.eligible,
                rejection=(
                    {
                        "code": item.rejection.code,
                        "reason": item.rejection.reason,
                    }
                    if item.rejection is not None
                    else None
                ),
            )
            for item in evaluations
        ],
    )


async def test_discovery_uses_saved_context_and_persists_only_evaluated_live_ids() -> None:
    context = _context()
    store = RecordingStore(context)
    merchant = RecordingMerchant(
        [
            _candidate(),
            _candidate(
                product_id="gift-card",
                product_kind=ProductKind.STORED_VALUE,
            ),
            _candidate(product_id="over-budget", amount=9000),
        ]
    )
    service = DiscoveryService(
        store=store,
        merchant=merchant,
        allow_stored_value_products=False,
    )
    user_id = uuid.uuid4()

    response = await service.create(
        user_id,
        DiscoveryCreate(
            recipient_id=context.recipient_id,
            occasion_id=context.occasion_id,
        ),
    )

    assert merchant.queries == [("games", 8000)]
    assert store.search_query == "games"
    assert store.user_ids == [user_id, user_id]
    assert len(response.candidates) == 3
    assert response.candidates[0].eligible is True
    assert response.candidates[1].rejection is not None
    assert response.candidates[1].rejection.code == "UNSUPPORTED_CHECKOUT"
    assert response.candidates[2].rejection is not None
    assert response.candidates[2].rejection.code == "OVER_BUDGET"
    assert response.eligible_candidate_ids == [response.candidates[0].id]


async def test_discovery_maps_merchant_failure_to_safe_api_error() -> None:
    context = _context()
    store = RecordingStore(context)
    merchant = RecordingMerchant([])
    merchant.error = MerchantGatewayError(
        "MERCHANT_UNAVAILABLE",
        "The gift source is temporarily unavailable. Try again.",
    )
    service = DiscoveryService(
        store=store,
        merchant=merchant,
        allow_stored_value_products=False,
    )

    try:
        await service.create(
            uuid.uuid4(),
            DiscoveryCreate(
                recipient_id=context.recipient_id,
                occasion_id=context.occasion_id,
            ),
        )
    except ApiError as error:
        assert error.status_code == 503
        assert error.code == "MERCHANT_UNAVAILABLE"
        assert error.recoverable is True
    else:
        raise AssertionError("merchant failure should be mapped")


async def test_discovery_prefers_fresh_live_product_after_prior_attempt() -> None:
    context = _context()
    context = DiscoveryContext(
        recipient_id=context.recipient_id,
        occasion_id=context.occasion_id,
        interests=context.interests,
        dislikes=context.dislikes,
        budget_minor=context.budget_minor,
        currency=context.currency,
        previous_product_ids=frozenset({"product-1"}),
    )
    store = RecordingStore(context)
    merchant = RecordingMerchant(
        [_candidate(product_id="product-1"), _candidate(product_id="product-2")]
    )
    service = DiscoveryService(
        store=store,
        merchant=merchant,
        allow_stored_value_products=False,
    )

    response = await service.create(
        uuid.uuid4(),
        DiscoveryCreate(
            recipient_id=context.recipient_id,
            occasion_id=context.occasion_id,
        ),
    )

    assert response.candidates[0].eligible is False
    assert response.candidates[0].rejection is not None
    assert response.candidates[0].rejection.code == "RECENTLY_ATTEMPTED"
    assert response.candidates[1].eligible is True


async def test_discovery_keeps_prior_product_when_no_fresh_live_option_exists() -> None:
    context = _context()
    context = DiscoveryContext(
        recipient_id=context.recipient_id,
        occasion_id=context.occasion_id,
        interests=context.interests,
        dislikes=context.dislikes,
        budget_minor=context.budget_minor,
        currency=context.currency,
        previous_product_ids=frozenset({"product-1"}),
    )
    store = RecordingStore(context)
    merchant = RecordingMerchant([_candidate(product_id="product-1")])
    service = DiscoveryService(
        store=store,
        merchant=merchant,
        allow_stored_value_products=False,
    )

    response = await service.create(
        uuid.uuid4(),
        DiscoveryCreate(
            recipient_id=context.recipient_id,
            occasion_id=context.occasion_id,
        ),
    )

    assert response.candidates[0].eligible is True


async def test_authenticated_discovery_routes_keep_user_ownership() -> None:
    context = _context()
    merchant = RecordingMerchant([_candidate()])
    result = await merchant.search(query="gaming headset", budget_minor=8000)
    response = _response_from_evaluations(
        context,
        result,
        [CandidateEvaluation(candidate=result.candidates[0], rejection=None)],
    )
    discovery = StaticDiscovery(response)
    user = AuthenticatedUser(
        id=uuid.uuid4(),
        email="talia@example.com",
        display_name="Talia",
        picture_url=None,
    )
    settings = Settings(
        app_env="test",
        database_url=SecretStr(
            "postgresql://wishtrace:password@database.invalid:5432/wishtrace?sslmode=require"
        ),
    )

    async def healthy_database() -> DatabaseProbe:
        return DatabaseProbe(connected=True, tls=True, server_version="17.0")

    app = create_app(
        settings=settings,
        database_probe=healthy_database,
        auth_operations=StaticAuth(user),
        discovery_operations=discovery,
    )
    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://testserver",
    ) as client:
        missing_auth = await client.post(
            "/v1/discoveries",
            json={
                "recipient_id": str(context.recipient_id),
                "occasion_id": str(context.occasion_id),
            },
        )
        assert missing_auth.status_code == 401

        headers = {"Authorization": "Bearer valid-session"}
        created = await client.post(
            "/v1/discoveries",
            headers=headers,
            json={
                "recipient_id": str(context.recipient_id),
                "occasion_id": str(context.occasion_id),
            },
        )
        assert created.status_code == 201
        assert created.json()["candidates"][0]["source_mode"] == "LIVE"

        fetched = await client.get(f"/v1/discoveries/{response.id}", headers=headers)
        assert fetched.status_code == 200
        assert fetched.json() == created.json()

    assert discovery.user_ids == [user.id, user.id]
