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
from app.ranking import (
    EvidenceKind,
    ModelDecisionStatus,
    ModelRankingDecision,
    ModelRationale,
    ProviderRankingAttempt,
    RankingCandidate,
    RankingClaim,
    RankingClaimAction,
    RankingEvidence,
    RankingGatewayError,
    RankingMode,
    RankingPackage,
    RankingRationaleResponse,
    RankingResponse,
    RankingService,
    RankingStore,
    RankingUncertainty,
    ValidatedRanking,
)


class MemoryRankingStore(RankingStore):
    def __init__(self, package: RankingPackage) -> None:
        self.package = package
        self.response: RankingResponse | None = None
        self.user_choice_required = False
        self.failure_category: str | None = None

    async def claim(
        self,
        user_id: uuid.UUID,
        discovery_id: uuid.UUID,
        now: datetime,
    ) -> RankingClaim:
        del user_id, now
        assert discovery_id == self.package.discovery_id
        if self.response is not None:
            return RankingClaim(
                action=RankingClaimAction.REPLAY,
                existing=self.response,
            )
        return RankingClaim(action=RankingClaimAction.CREATE, package=self.package)

    async def complete(
        self,
        *,
        user_id: uuid.UUID,
        run_id: uuid.UUID,
        decision: ValidatedRanking,
        mode: RankingMode,
        provider_request_id: str | None,
        provider_deployment: str | None,
        duration_ms: int,
        failure_category: str | None,
    ) -> RankingResponse:
        del user_id, provider_deployment, duration_ms
        assert run_id == self.package.run_id
        self.failure_category = failure_category
        self.response = RankingResponse(
            id=run_id,
            discovery_id=self.package.discovery_id,
            selected_candidate_id=decision.selected_candidate_id,
            alternative_candidate_ids=decision.alternative_candidate_ids,
            rationales=decision.rationales,
            evidence=self.package.evidence,
            uncertainty=decision.uncertainty,
            mode=mode,
            model_request_id=provider_request_id,
            prompt_version="gift-rank-v1",
            schema_version="ranked-decision-v1",
            attempt_count=1,
            created_at=datetime(2026, 8, 1, 16, 0, tzinfo=UTC),
        )
        return self.response

    async def require_user_choice(
        self,
        *,
        user_id: uuid.UUID,
        run_id: uuid.UUID,
        provider_request_id: str | None,
        provider_deployment: str | None,
        duration_ms: int,
        failure_category: str,
    ) -> None:
        del user_id, provider_request_id, provider_deployment, duration_ms
        assert run_id == self.package.run_id
        self.user_choice_required = True
        self.failure_category = failure_category

    async def get(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> RankingResponse:
        del user_id
        assert discovery_id == self.package.discovery_id
        assert self.response is not None
        return self.response


class QueueRankingGateway:
    def __init__(
        self,
        results: list[ProviderRankingAttempt | RankingGatewayError],
    ) -> None:
        self.results = results
        self.repairs: list[str | None] = []

    @property
    def deployment(self) -> str:
        return "configured-deployment"

    async def rank(
        self,
        package: RankingPackage,
        *,
        repair_reason: str | None,
    ) -> ProviderRankingAttempt:
        del package
        self.repairs.append(repair_reason)
        result = self.results.pop(0)
        if isinstance(result, RankingGatewayError):
            raise result
        return result


class StaticRankingOperations:
    def __init__(self, response: RankingResponse) -> None:
        self.response = response

    async def rank(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> RankingResponse:
        del user_id
        assert discovery_id == self.response.discovery_id
        return self.response

    async def get(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> RankingResponse:
        return await self.rank(user_id, discovery_id)


class StaticAuth:
    def __init__(self, user: AuthenticatedUser) -> None:
        self.user = user

    async def create_challenge(self) -> ChallengeResponse:
        raise NotImplementedError

    async def exchange(self, body: GoogleExchangeRequest) -> SessionResponse:
        del body
        raise NotImplementedError

    async def authenticate(self, raw_token: str) -> AuthenticatedUser:
        if raw_token != "valid-session":
            raise ApiError(
                status_code=401,
                code="UNAUTHORIZED",
                message="Sign in again.",
                recoverable=True,
            )
        return self.user

    async def logout(self, raw_token: str) -> None:
        del raw_token


def _package() -> RankingPackage:
    return RankingPackage(
        run_id=uuid.uuid4(),
        discovery_id=uuid.uuid4(),
        candidates=[
            RankingCandidate(
                id=uuid.uuid4(),
                position=0,
                title="Observed gaming headset",
                variant_title="Black",
                description="A wired headset for games.",
                categories=["Gaming audio"],
                tags=["headset"],
            ),
            RankingCandidate(
                id=uuid.uuid4(),
                position=1,
                title="Observed training mat",
                variant_title=None,
                description="A mat for workouts.",
                categories=["Fitness"],
                tags=["gym"],
            ),
        ],
        evidence=[
            RankingEvidence(
                id="ev_interest_gaming",
                kind=EvidenceKind.INTEREST,
                value="gaming",
            ),
            RankingEvidence(
                id="ev_interest_gym",
                kind=EvidenceKind.INTEREST,
                value="gym",
            ),
            RankingEvidence(
                id="ev_relationship",
                kind=EvidenceKind.RELATIONSHIP,
                value="Sibling",
            ),
        ],
    )


def _attempt(
    package: RankingPackage,
    *,
    selected_id: str | None = None,
    reason: str = "Matches the saved gaming interest.",
    request_id: str = "resp_model_1",
) -> ProviderRankingAttempt:
    candidate_id = selected_id or str(package.candidates[0].id)
    return ProviderRankingAttempt(
        decision=ModelRankingDecision(
            status=ModelDecisionStatus.SELECTED,
            selected_candidate_id=candidate_id,
            alternative_candidate_ids=[],
            rationales=[
                ModelRationale(
                    candidate_id=candidate_id,
                    evidence_ids=["ev_interest_gaming"],
                    reason=reason,
                )
            ],
            uncertainty=RankingUncertainty.LOW,
            no_selection_reason=None,
        ),
        request_id=request_id,
        duration_ms=42,
    )


async def test_model_ranking_uses_only_allowed_ids_and_replays() -> None:
    package = _package()
    store = MemoryRankingStore(package)
    gateway = QueueRankingGateway([_attempt(package)])
    service = RankingService(store=store, gateway=gateway)
    user_id = uuid.uuid4()

    first = await service.rank(user_id, package.discovery_id)
    replay = await service.rank(user_id, package.discovery_id)

    assert first.mode == RankingMode.MODEL
    assert first.selected_candidate_id == package.candidates[0].id
    assert first.model_request_id == "resp_model_1"
    assert replay == first
    assert gateway.repairs == [None]


async def test_unknown_candidate_is_repaired_once() -> None:
    package = _package()
    store = MemoryRankingStore(package)
    gateway = QueueRankingGateway(
        [
            _attempt(package, selected_id=str(uuid.uuid4()), request_id="resp_bad"),
            _attempt(package, request_id="resp_repaired"),
        ]
    )
    service = RankingService(store=store, gateway=gateway)

    response = await service.rank(uuid.uuid4(), package.discovery_id)

    assert response.mode == RankingMode.MODEL
    assert response.model_request_id == "resp_repaired"
    assert gateway.repairs[0] is None
    assert "supplied eligible_candidate_ids" in (gateway.repairs[1] or "")
    assert store.failure_category == "MODEL_OUTPUT_REPAIRED"


async def test_unsupported_commerce_claim_is_repaired_once() -> None:
    package = _package()
    store = MemoryRankingStore(package)
    gateway = QueueRankingGateway(
        [
            _attempt(
                package,
                reason="It is in stock and matches the saved gaming interest.",
                request_id="resp_claim",
            ),
            _attempt(package, request_id="resp_repaired"),
        ]
    )
    service = RankingService(store=store, gateway=gateway)

    response = await service.rank(uuid.uuid4(), package.discovery_id)

    assert response.mode == RankingMode.MODEL
    assert response.model_request_id == "resp_repaired"
    assert "fit only" in (gateway.repairs[1] or "")
    assert store.failure_category == "MODEL_OUTPUT_REPAIRED"


async def test_model_no_selection_requires_explicit_user_choice() -> None:
    package = _package()
    store = MemoryRankingStore(package)
    gateway = QueueRankingGateway(
        [
            ProviderRankingAttempt(
                decision=ModelRankingDecision(
                    status=ModelDecisionStatus.NO_SELECTION,
                    selected_candidate_id=None,
                    alternative_candidate_ids=[],
                    rationales=[],
                    uncertainty=RankingUncertainty.HIGH,
                    no_selection_reason="The supplied clues do not distinguish the options.",
                ),
                request_id="resp_no_selection",
                duration_ms=35,
            )
        ]
    )
    service = RankingService(store=store, gateway=gateway)

    with pytest.raises(ApiError) as captured:
        await service.rank(uuid.uuid4(), package.discovery_id)

    assert captured.value.code == "RANKING_REQUIRES_USER_CHOICE"
    assert store.user_choice_required is True
    assert store.failure_category == "MODEL_NO_SELECTION"
    assert gateway.repairs == [None]


async def test_invalid_model_twice_falls_back_to_direct_evidence() -> None:
    package = _package()
    store = MemoryRankingStore(package)
    gateway = QueueRankingGateway(
        [
            _attempt(package, selected_id=str(uuid.uuid4()), request_id="resp_bad_1"),
            _attempt(package, selected_id=str(uuid.uuid4()), request_id="resp_bad_2"),
        ]
    )
    service = RankingService(store=store, gateway=gateway)

    response = await service.rank(uuid.uuid4(), package.discovery_id)

    assert response.mode == RankingMode.DETERMINISTIC
    assert response.uncertainty == RankingUncertainty.HIGH
    assert response.selected_candidate_id == package.candidates[0].id
    assert set(response.alternative_candidate_ids) == {package.candidates[1].id}
    assert store.failure_category == "MODEL_UNKNOWN_CANDIDATE"


async def test_model_failure_without_direct_evidence_requires_user_choice() -> None:
    package = _package().model_copy(
        update={
            "evidence": [
                RankingEvidence(
                    id="ev_relationship",
                    kind=EvidenceKind.RELATIONSHIP,
                    value="Sibling",
                )
            ]
        }
    )
    store = MemoryRankingStore(package)
    gateway = QueueRankingGateway(
        [
            RankingGatewayError(
                "MODEL_TIMEOUT",
                repairable=False,
                repair_reason="Provider timeout.",
                duration_ms=30_000,
            )
        ]
    )
    service = RankingService(store=store, gateway=gateway)

    with pytest.raises(ApiError) as captured:
        await service.rank(uuid.uuid4(), package.discovery_id)

    assert captured.value.code == "RANKING_REQUIRES_USER_CHOICE"
    assert store.user_choice_required is True
    assert store.failure_category == "MODEL_TIMEOUT"


async def test_no_eligible_candidates_never_calls_model() -> None:
    package = _package()

    class EmptyStore(MemoryRankingStore):
        async def claim(
            self,
            user_id: uuid.UUID,
            discovery_id: uuid.UUID,
            now: datetime,
        ) -> RankingClaim:
            del user_id, discovery_id, now
            raise ApiError(
                status_code=409,
                code="NO_ELIGIBLE_CANDIDATES",
                message="No verified checkout path.",
                recoverable=True,
            )

    gateway = QueueRankingGateway([])
    service = RankingService(store=EmptyStore(package), gateway=gateway)

    with pytest.raises(ApiError) as captured:
        await service.rank(uuid.uuid4(), package.discovery_id)

    assert captured.value.code == "NO_ELIGIBLE_CANDIDATES"
    assert gateway.repairs == []


async def test_ranking_routes_require_authentication() -> None:
    package = _package()
    response = RankingResponse(
        id=package.run_id,
        discovery_id=package.discovery_id,
        selected_candidate_id=package.candidates[0].id,
        alternative_candidate_ids=[],
        rationales=[
            RankingRationaleResponse(
                candidate_id=package.candidates[0].id,
                evidence_ids=[package.evidence[0].id],
                reason="Matches the saved gaming interest.",
            )
        ],
        evidence=package.evidence,
        uncertainty=RankingUncertainty.LOW,
        mode=RankingMode.MODEL,
        model_request_id="resp_model_1",
        prompt_version="gift-rank-v1",
        schema_version="ranked-decision-v1",
        attempt_count=1,
        created_at=datetime(2026, 8, 1, 16, 0, tzinfo=UTC),
    )
    user = AuthenticatedUser(
        id=uuid.uuid4(),
        email="talia@example.com",
        display_name="Talia",
        picture_url=None,
    )
    settings = Settings(
        app_env="test",
        database_url=SecretStr(
            "postgresql://wishtrace:password@database.invalid:5432/"
            "wishtrace?sslmode=require"
        ),
    )

    async def healthy_database() -> DatabaseProbe:
        return DatabaseProbe(connected=True, tls=True, server_version="17.0")

    app = create_app(
        settings=settings,
        database_probe=healthy_database,
        auth_operations=StaticAuth(user),
        ranking_operations=StaticRankingOperations(response),
    )
    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://testserver",
    ) as client:
        path = f"/v1/discoveries/{package.discovery_id}/rank"
        unauthenticated = await client.post(path)
        assert unauthenticated.status_code == 401

        ranked = await client.post(
            path,
            headers={"Authorization": "Bearer valid-session"},
        )
        assert ranked.status_code == 200
        assert ranked.json()["selected_candidate_id"] == str(package.candidates[0].id)

        fetched = await client.get(
            f"/v1/discoveries/{package.discovery_id}/ranking",
            headers={"Authorization": "Bearer valid-session"},
        )
        assert fetched.status_code == 200
        assert fetched.json()["model_request_id"] == "resp_model_1"
