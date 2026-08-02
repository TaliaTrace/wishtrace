import uuid
from dataclasses import dataclass
from datetime import datetime
from typing import Literal, Protocol

from pydantic import BaseModel, ConfigDict
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.commerce import (
    AvailabilityState,
    CandidateEvaluation,
    DeliveryState,
    MerchantGatewayError,
    MerchantSearchResult,
    ProductKind,
    RejectionCode,
    UcpMerchantGateway,
    evaluate_candidates,
)
from app.errors import ApiError
from app.models import (
    CandidateRejectionModel,
    CandidateSnapshotModel,
    DiscoveryRunModel,
    OccasionModel,
    RecipientModel,
    RecipientPreferenceModel,
)


class DiscoveryCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    recipient_id: uuid.UUID
    occasion_id: uuid.UUID


class DiscoveryRejectionResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    code: RejectionCode
    reason: str


class DiscoveryCandidateResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    id: uuid.UUID
    merchant_product_id: str
    merchant_variant_id: str | None
    sku: str | None
    title: str
    variant_title: str | None
    description: str | None
    product_url: str
    image_url: str | None
    price_minor: int
    currency: Literal["USD"]
    availability: AvailabilityState
    selected_options: dict[str, str]
    categories: list[str]
    tags: list[str]
    product_kind: ProductKind
    checkout_supported: bool
    delivery: DeliveryState
    source_timestamp: datetime
    source_mode: Literal["LIVE"]
    eligible: bool
    rejection: DiscoveryRejectionResponse | None


class DiscoveryResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    id: uuid.UUID
    recipient_id: uuid.UUID
    occasion_id: uuid.UUID
    status: Literal["COMPLETED"]
    merchant_id: str
    merchant_name: str
    search_query: str
    budget_minor: int
    currency: Literal["USD"]
    source_request_id: str | None
    profile_cache_compliant: bool
    source_timestamp: datetime
    candidates: list[DiscoveryCandidateResponse]

    @property
    def eligible_candidate_ids(self) -> list[uuid.UUID]:
        return [candidate.id for candidate in self.candidates if candidate.eligible]


@dataclass(frozen=True, slots=True)
class DiscoveryContext:
    recipient_id: uuid.UUID
    occasion_id: uuid.UUID
    interests: list[str]
    dislikes: list[str]
    budget_minor: int
    currency: Literal["USD"]


class DiscoveryStore(Protocol):
    async def load_context(
        self,
        user_id: uuid.UUID,
        recipient_id: uuid.UUID,
        occasion_id: uuid.UUID,
    ) -> DiscoveryContext: ...

    async def persist_completed(
        self,
        *,
        user_id: uuid.UUID,
        context: DiscoveryContext,
        search_query: str,
        merchant_result: MerchantSearchResult,
        evaluations: list[CandidateEvaluation],
    ) -> DiscoveryResponse: ...

    async def get(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> DiscoveryResponse: ...


class MerchantSearchGateway(Protocol):
    async def search(self, *, query: str, budget_minor: int) -> MerchantSearchResult: ...


class DiscoveryOperations(Protocol):
    async def create(
        self,
        user_id: uuid.UUID,
        body: DiscoveryCreate,
    ) -> DiscoveryResponse: ...

    async def get(
        self,
        user_id: uuid.UUID,
        discovery_id: uuid.UUID,
    ) -> DiscoveryResponse: ...


class DiscoveryService:
    def __init__(
        self,
        *,
        store: DiscoveryStore,
        merchant: MerchantSearchGateway,
        allow_stored_value_products: bool,
    ) -> None:
        self._store = store
        self._merchant = merchant
        self._allow_stored_value_products = allow_stored_value_products

    async def create(
        self,
        user_id: uuid.UUID,
        body: DiscoveryCreate,
    ) -> DiscoveryResponse:
        context = await self._store.load_context(
            user_id,
            body.recipient_id,
            body.occasion_id,
        )
        search_query = _catalog_query(context.interests)
        try:
            merchant_result = await self._merchant.search(
                query=search_query,
                budget_minor=context.budget_minor,
            )
        except MerchantGatewayError as error:
            raise ApiError(
                status_code=503 if error.recoverable else 502,
                code=error.code,
                message=error.safe_message,
                recoverable=error.recoverable,
            ) from error
        evaluations = evaluate_candidates(
            merchant_result.candidates,
            budget_minor=context.budget_minor,
            dislikes=context.dislikes,
            allow_stored_value_products=self._allow_stored_value_products,
        )
        return await self._store.persist_completed(
            user_id=user_id,
            context=context,
            search_query=search_query,
            merchant_result=merchant_result,
            evaluations=evaluations,
        )

    async def get(
        self,
        user_id: uuid.UUID,
        discovery_id: uuid.UUID,
    ) -> DiscoveryResponse:
        return await self._store.get(user_id, discovery_id)


class UnavailableDiscoveryService:
    async def create(
        self,
        user_id: uuid.UUID,
        body: DiscoveryCreate,
    ) -> DiscoveryResponse:
        del user_id, body
        raise _unavailable()

    async def get(
        self,
        user_id: uuid.UUID,
        discovery_id: uuid.UUID,
    ) -> DiscoveryResponse:
        del user_id, discovery_id
        raise _unavailable()


class SqlDiscoveryStore:
    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def load_context(
        self,
        user_id: uuid.UUID,
        recipient_id: uuid.UUID,
        occasion_id: uuid.UUID,
    ) -> DiscoveryContext:
        async with self._session_factory() as session:
            recipient = await session.scalar(
                select(RecipientModel).where(
                    RecipientModel.id == recipient_id,
                    RecipientModel.user_id == user_id,
                )
            )
            if recipient is None:
                raise _not_found("RECIPIENT_NOT_FOUND", "That person was not found.")
            occasion = await session.scalar(
                select(OccasionModel).where(
                    OccasionModel.id == occasion_id,
                    OccasionModel.user_id == user_id,
                    OccasionModel.recipient_id == recipient_id,
                )
            )
            if occasion is None:
                raise _not_found("OCCASION_NOT_FOUND", "That occasion was not found.")
            preferences = (
                await session.scalars(
                    select(RecipientPreferenceModel)
                    .where(RecipientPreferenceModel.recipient_id == recipient_id)
                    .order_by(
                        RecipientPreferenceModel.kind,
                        RecipientPreferenceModel.position,
                    )
                )
            ).all()
            # Interests are optional now: the green-tile personality taps plus
            # relationship and occasion carry the first-pass ranking, so an empty
            # interest list is a valid partial profile rather than an error.
            interests = [item.value for item in preferences if item.kind == "INTEREST"]
            return DiscoveryContext(
                recipient_id=recipient.id,
                occasion_id=occasion.id,
                interests=interests,
                dislikes=[item.value for item in preferences if item.kind == "DISLIKE"],
                budget_minor=occasion.budget_minor,
                currency="USD",
            )

    async def persist_completed(
        self,
        *,
        user_id: uuid.UUID,
        context: DiscoveryContext,
        search_query: str,
        merchant_result: MerchantSearchResult,
        evaluations: list[CandidateEvaluation],
    ) -> DiscoveryResponse:
        async with self._session_factory() as session, session.begin():
            run = DiscoveryRunModel(
                user_id=user_id,
                recipient_id=context.recipient_id,
                occasion_id=context.occasion_id,
                status="COMPLETED",
                merchant_id=merchant_result.merchant_id,
                merchant_name=merchant_result.merchant_name,
                search_query=search_query,
                budget_minor=context.budget_minor,
                currency=context.currency,
                source_request_id=merchant_result.request_id,
                profile_cache_compliant=merchant_result.profile_cache_compliant,
                source_timestamp=merchant_result.source_timestamp,
            )
            session.add(run)
            await session.flush()
            for position, evaluation in enumerate(evaluations):
                candidate = evaluation.candidate
                snapshot = CandidateSnapshotModel(
                    discovery_run_id=run.id,
                    position=position,
                    source_key=candidate.source_key,
                    merchant_product_id=candidate.merchant_product_id,
                    merchant_variant_id=candidate.merchant_variant_id,
                    sku=candidate.sku,
                    title=candidate.title,
                    variant_title=candidate.variant_title,
                    description=candidate.description,
                    product_url=candidate.product_url,
                    image_url=candidate.image_url,
                    price_minor=candidate.price.amount,
                    currency=candidate.price.currency,
                    availability=candidate.availability.value,
                    selected_options=candidate.selected_options,
                    categories=candidate.categories,
                    tags=candidate.tags,
                    product_kind=candidate.product_kind.value,
                    checkout_supported=candidate.checkout_supported,
                    delivery_state=candidate.delivery.value,
                    source_timestamp=candidate.source_timestamp,
                    source_mode=candidate.source_mode,
                    eligible=evaluation.eligible,
                )
                session.add(snapshot)
                await session.flush()
                if evaluation.rejection is not None:
                    session.add(
                        CandidateRejectionModel(
                            candidate_snapshot_id=snapshot.id,
                            code=evaluation.rejection.code.value,
                            reason=evaluation.rejection.reason,
                        )
                    )
            await session.flush()
            return await _response(session, run)

    async def get(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> DiscoveryResponse:
        async with self._session_factory() as session:
            run = await session.scalar(
                select(DiscoveryRunModel).where(
                    DiscoveryRunModel.id == discovery_id,
                    DiscoveryRunModel.user_id == user_id,
                )
            )
            if run is None:
                raise _not_found("DISCOVERY_NOT_FOUND", "That gift search was not found.")
            return await _response(session, run)


async def _response(session: AsyncSession, run: DiscoveryRunModel) -> DiscoveryResponse:
    snapshots = (
        await session.scalars(
            select(CandidateSnapshotModel)
            .where(CandidateSnapshotModel.discovery_run_id == run.id)
            .order_by(CandidateSnapshotModel.position)
        )
    ).all()
    snapshot_ids = [snapshot.id for snapshot in snapshots]
    rejections: dict[uuid.UUID, CandidateRejectionModel] = {}
    if snapshot_ids:
        rejection_rows = (
            await session.scalars(
                select(CandidateRejectionModel).where(
                    CandidateRejectionModel.candidate_snapshot_id.in_(snapshot_ids)
                )
            )
        ).all()
        rejections = {item.candidate_snapshot_id: item for item in rejection_rows}
    return DiscoveryResponse(
        id=run.id,
        recipient_id=run.recipient_id,
        occasion_id=run.occasion_id,
        status="COMPLETED",
        merchant_id=run.merchant_id,
        merchant_name=run.merchant_name,
        search_query=run.search_query,
        budget_minor=run.budget_minor,
        currency="USD",
        source_request_id=run.source_request_id,
        profile_cache_compliant=run.profile_cache_compliant,
        source_timestamp=run.source_timestamp,
        candidates=[
            _candidate_response(snapshot, rejections.get(snapshot.id))
            for snapshot in snapshots
        ],
    )


def _candidate_response(
    snapshot: CandidateSnapshotModel,
    rejection: CandidateRejectionModel | None,
) -> DiscoveryCandidateResponse:
    return DiscoveryCandidateResponse(
        id=snapshot.id,
        merchant_product_id=snapshot.merchant_product_id,
        merchant_variant_id=snapshot.merchant_variant_id,
        sku=snapshot.sku,
        title=snapshot.title,
        variant_title=snapshot.variant_title,
        description=snapshot.description,
        product_url=snapshot.product_url,
        image_url=snapshot.image_url,
        price_minor=snapshot.price_minor,
        currency="USD",
        availability=AvailabilityState(snapshot.availability),
        selected_options=snapshot.selected_options,
        categories=snapshot.categories,
        tags=snapshot.tags,
        product_kind=ProductKind(snapshot.product_kind),
        checkout_supported=snapshot.checkout_supported,
        delivery=DeliveryState(snapshot.delivery_state),
        source_timestamp=snapshot.source_timestamp,
        source_mode="LIVE",
        eligible=snapshot.eligible,
        rejection=(
            DiscoveryRejectionResponse(
                code=RejectionCode(rejection.code),
                reason=rejection.reason,
            )
            if rejection is not None
            else None
        ),
    )


def _catalog_query(interests: list[str]) -> str:
    gaming_terms = {"game", "games", "gaming", "video games", "cozy gaming"}
    if any(interest.strip().casefold() in gaming_terms for interest in interests):
        return "games"
    # No interests captured: fall back to the merchant's broad catalog anchor so
    # ranking receives distinct products instead of a single denomination.
    first = next((interest.strip() for interest in interests if interest.strip()), "")
    return first or "games"


def _not_found(code: str, message: str) -> ApiError:
    return ApiError(
        status_code=404,
        code=code,
        message=message,
        recoverable=True,
    )


def _unavailable() -> ApiError:
    return ApiError(
        status_code=503,
        code="COMMERCE_UNAVAILABLE",
        message="Gift search is not connected securely yet. Try again later.",
        recoverable=True,
    )


def build_discovery_service(
    *,
    session_factory: async_sessionmaker[AsyncSession],
    merchant: UcpMerchantGateway,
    allow_stored_value_products: bool,
) -> DiscoveryOperations:
    return DiscoveryService(
        store=SqlDiscoveryStore(session_factory),
        merchant=merchant,
        allow_stored_value_products=allow_stored_value_products,
    )
