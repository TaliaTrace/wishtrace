import re
import uuid
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from enum import StrEnum
from typing import Protocol

from pydantic import BaseModel, ConfigDict, Field, model_validator
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.errors import ApiError
from app.models import (
    CandidateSnapshotModel,
    DiscoveryRunModel,
    HintModel,
    OccasionModel,
    RankingEvidenceModel,
    RankingItemModel,
    RankingRunModel,
    RecipientModel,
    RecipientPreferenceModel,
)

PROMPT_VERSION = "gift-rank-v1"
SCHEMA_VERSION = "ranked-decision-v1"
RANKING_CLAIM_TTL = timedelta(seconds=90)
COMMERCE_CLAIM_PATTERN = re.compile(
    r"(?:\$|\b(?:arriv(?:e|es|ing)|deliver(?:y|ed|s)|discount|price|sale|shipping|stock)\b)",
    re.IGNORECASE,
)
FALLBACK_STOP_WORDS = {
    "about",
    "after",
    "also",
    "been",
    "from",
    "gift",
    "have",
    "likes",
    "really",
    "that",
    "their",
    "they",
    "this",
    "want",
    "wants",
    "with",
}


class RankingUncertainty(StrEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class RankingMode(StrEnum):
    MODEL = "MODEL"
    DETERMINISTIC = "DETERMINISTIC"


class EvidenceKind(StrEnum):
    INTEREST = "INTEREST"
    HINT = "HINT"
    RELATIONSHIP = "RELATIONSHIP"
    OCCASION = "OCCASION"


class ModelDecisionStatus(StrEnum):
    SELECTED = "SELECTED"
    NO_SELECTION = "NO_SELECTION"


class RankingEvidence(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    id: str = Field(min_length=1, max_length=48)
    kind: EvidenceKind
    value: str = Field(min_length=1, max_length=500)


class RankingCandidate(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    id: uuid.UUID
    position: int = Field(ge=0)
    title: str = Field(min_length=1, max_length=500)
    variant_title: str | None = Field(default=None, max_length=500)
    description: str | None = Field(default=None, max_length=800)
    categories: list[str]
    tags: list[str]


class RankingPackage(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    run_id: uuid.UUID
    discovery_id: uuid.UUID
    candidates: list[RankingCandidate]
    evidence: list[RankingEvidence]


class ModelRationale(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, str_strip_whitespace=True)

    candidate_id: str = Field(min_length=1, max_length=64)
    evidence_ids: list[str]
    reason: str = Field(min_length=1, max_length=180)


class ModelRankingDecision(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, str_strip_whitespace=True)

    status: ModelDecisionStatus
    selected_candidate_id: str | None
    alternative_candidate_ids: list[str]
    rationales: list[ModelRationale]
    uncertainty: RankingUncertainty
    no_selection_reason: str | None = Field(max_length=240)

    @model_validator(mode="after")
    def validate_shape(self) -> "ModelRankingDecision":
        if self.status is ModelDecisionStatus.NO_SELECTION:
            if (
                self.selected_candidate_id is not None
                or self.alternative_candidate_ids
                or self.rationales
                or not self.no_selection_reason
            ):
                raise ValueError("NO_SELECTION must contain only a reason")
            return self
        if self.selected_candidate_id is None or self.no_selection_reason is not None:
            raise ValueError("SELECTED must contain a selected candidate and no refusal reason")
        if len(self.alternative_candidate_ids) > 2:
            raise ValueError("at most two alternatives are allowed")
        ranked_ids = [self.selected_candidate_id, *self.alternative_candidate_ids]
        if len(set(ranked_ids)) != len(ranked_ids):
            raise ValueError("ranked candidate IDs must be unique")
        rationale_ids = [item.candidate_id for item in self.rationales]
        if len(set(rationale_ids)) != len(rationale_ids) or set(rationale_ids) != set(
            ranked_ids
        ):
            raise ValueError("every ranked candidate needs exactly one rationale")
        return self


class RankingRationaleResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    candidate_id: uuid.UUID
    evidence_ids: list[str]
    reason: str


class RankingResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    id: uuid.UUID
    discovery_id: uuid.UUID
    selected_candidate_id: uuid.UUID
    alternative_candidate_ids: list[uuid.UUID]
    rationales: list[RankingRationaleResponse]
    evidence: list[RankingEvidence]
    uncertainty: RankingUncertainty
    mode: RankingMode
    model_request_id: str | None
    prompt_version: str
    schema_version: str
    attempt_count: int
    created_at: datetime


@dataclass(frozen=True, slots=True)
class ProviderRankingAttempt:
    decision: ModelRankingDecision
    request_id: str | None
    duration_ms: int


class RankingGatewayError(Exception):
    def __init__(
        self,
        category: str,
        *,
        repairable: bool,
        repair_reason: str,
        request_id: str | None = None,
        duration_ms: int = 0,
    ) -> None:
        super().__init__(category)
        self.category = category
        self.repairable = repairable
        self.repair_reason = repair_reason
        self.request_id = request_id
        self.duration_ms = duration_ms


class RankingGateway(Protocol):
    @property
    def deployment(self) -> str | None: ...

    async def rank(
        self,
        package: RankingPackage,
        *,
        repair_reason: str | None,
    ) -> ProviderRankingAttempt: ...


class RankingClaimAction(StrEnum):
    CREATE = "CREATE"
    REPLAY = "REPLAY"


@dataclass(frozen=True, slots=True)
class RankingClaim:
    action: RankingClaimAction
    package: RankingPackage | None = None
    existing: RankingResponse | None = None


@dataclass(frozen=True, slots=True)
class ValidatedRanking:
    selected_candidate_id: uuid.UUID
    alternative_candidate_ids: list[uuid.UUID]
    rationales: list[RankingRationaleResponse]
    uncertainty: RankingUncertainty


class RankingStore(Protocol):
    async def claim(
        self,
        user_id: uuid.UUID,
        discovery_id: uuid.UUID,
        now: datetime,
    ) -> RankingClaim: ...

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
    ) -> RankingResponse: ...

    async def require_user_choice(
        self,
        *,
        user_id: uuid.UUID,
        run_id: uuid.UUID,
        provider_request_id: str | None,
        provider_deployment: str | None,
        duration_ms: int,
        failure_category: str,
    ) -> None: ...

    async def get(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> RankingResponse: ...


class RankingOperations(Protocol):
    async def rank(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> RankingResponse: ...

    async def get(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> RankingResponse: ...


class RankingService:
    def __init__(self, *, store: RankingStore, gateway: RankingGateway) -> None:
        self._store = store
        self._gateway = gateway

    async def rank(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> RankingResponse:
        claim = await self._store.claim(user_id, discovery_id, datetime.now(UTC))
        if claim.action is RankingClaimAction.REPLAY:
            assert claim.existing is not None
            return claim.existing
        package = claim.package
        assert package is not None

        total_duration_ms = 0
        last_request_id: str | None = None
        failure_category: str | None = None
        repair_reason: str | None = None
        for attempt_number in range(2):
            try:
                attempt = await self._gateway.rank(
                    package,
                    repair_reason=repair_reason,
                )
                total_duration_ms += attempt.duration_ms
                last_request_id = attempt.request_id or last_request_id
                if attempt.decision.status is ModelDecisionStatus.NO_SELECTION:
                    await self._store.require_user_choice(
                        user_id=user_id,
                        run_id=package.run_id,
                        provider_request_id=last_request_id,
                        provider_deployment=self._gateway.deployment,
                        duration_ms=total_duration_ms,
                        failure_category="MODEL_NO_SELECTION",
                    )
                    raise _user_choice_required()
                try:
                    decision = validate_model_decision(package, attempt.decision)
                except DecisionValidationFailure as error:
                    failure_category = error.category
                    if attempt_number == 0:
                        repair_reason = error.repair_reason
                        continue
                    break
                return await self._store.complete(
                    user_id=user_id,
                    run_id=package.run_id,
                    decision=decision,
                    mode=RankingMode.MODEL,
                    provider_request_id=last_request_id,
                    provider_deployment=self._gateway.deployment,
                    duration_ms=total_duration_ms,
                    failure_category=(
                        "MODEL_OUTPUT_REPAIRED" if attempt_number == 1 else None
                    ),
                )
            except RankingGatewayError as error:
                total_duration_ms += error.duration_ms
                last_request_id = error.request_id or last_request_id
                failure_category = error.category
                if error.repairable and attempt_number == 0:
                    repair_reason = error.repair_reason
                    continue
                break

        fallback = deterministic_ranking(package)
        if fallback is not None:
            return await self._store.complete(
                user_id=user_id,
                run_id=package.run_id,
                decision=fallback,
                mode=RankingMode.DETERMINISTIC,
                provider_request_id=last_request_id,
                provider_deployment=self._gateway.deployment,
                duration_ms=total_duration_ms,
                failure_category=failure_category or "MODEL_UNAVAILABLE",
            )
        await self._store.require_user_choice(
            user_id=user_id,
            run_id=package.run_id,
            provider_request_id=last_request_id,
            provider_deployment=self._gateway.deployment,
            duration_ms=total_duration_ms,
            failure_category=failure_category or "NO_DIRECT_EVIDENCE_MATCH",
        )
        raise _user_choice_required()

    async def get(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> RankingResponse:
        return await self._store.get(user_id, discovery_id)


class UnavailableRankingGateway:
    @property
    def deployment(self) -> str | None:
        return None

    async def rank(
        self,
        package: RankingPackage,
        *,
        repair_reason: str | None,
    ) -> ProviderRankingAttempt:
        del package, repair_reason
        raise RankingGatewayError(
            "MODEL_UNAVAILABLE",
            repairable=False,
            repair_reason="The configured model is unavailable.",
        )


class DecisionValidationFailure(Exception):
    def __init__(self, category: str, repair_reason: str) -> None:
        super().__init__(repair_reason)
        self.category = category
        self.repair_reason = repair_reason


def validate_model_decision(
    package: RankingPackage,
    output: ModelRankingDecision,
) -> ValidatedRanking:
    if output.status is not ModelDecisionStatus.SELECTED:
        raise DecisionValidationFailure(
            "MODEL_NO_SELECTION",
            "Select only when evidence supports a candidate.",
        )
    assert output.selected_candidate_id is not None
    allowed_ids = {str(candidate.id): candidate.id for candidate in package.candidates}
    evidence_ids = {item.id for item in package.evidence}
    ranked_raw = [output.selected_candidate_id, *output.alternative_candidate_ids]
    if any(candidate_id not in allowed_ids for candidate_id in ranked_raw):
        raise DecisionValidationFailure(
            "MODEL_UNKNOWN_CANDIDATE",
            "Use only candidate IDs from the supplied eligible_candidate_ids list.",
        )
    rationales_by_candidate: dict[str, RankingRationaleResponse] = {}
    for rationale in output.rationales:
        if rationale.candidate_id not in allowed_ids:
            raise DecisionValidationFailure(
                "MODEL_UNKNOWN_CANDIDATE",
                "Every rationale candidate_id must be an allowed candidate ID.",
            )
        if not rationale.evidence_ids or any(
            evidence_id not in evidence_ids for evidence_id in rationale.evidence_ids
        ):
            raise DecisionValidationFailure(
                "MODEL_UNKNOWN_EVIDENCE",
                "Every rationale must cite one or more supplied evidence IDs only.",
            )
        if len(set(rationale.evidence_ids)) != len(rationale.evidence_ids):
            raise DecisionValidationFailure(
                "MODEL_DUPLICATE_EVIDENCE",
                "Do not repeat an evidence ID inside a rationale.",
            )
        if COMMERCE_CLAIM_PATTERN.search(rationale.reason):
            raise DecisionValidationFailure(
                "MODEL_UNSUPPORTED_COMMERCE_CLAIM",
                (
                    "Reasons must discuss fit only; do not mention price, stock, "
                    "shipping, or delivery."
                ),
            )
        rationales_by_candidate[rationale.candidate_id] = RankingRationaleResponse(
            candidate_id=allowed_ids[rationale.candidate_id],
            evidence_ids=rationale.evidence_ids,
            reason=rationale.reason,
        )
    return ValidatedRanking(
        selected_candidate_id=allowed_ids[output.selected_candidate_id],
        alternative_candidate_ids=[
            allowed_ids[candidate_id] for candidate_id in output.alternative_candidate_ids
        ],
        rationales=[rationales_by_candidate[candidate_id] for candidate_id in ranked_raw],
        uncertainty=output.uncertainty,
    )


def deterministic_ranking(package: RankingPackage) -> ValidatedRanking | None:
    scored: list[tuple[int, int, RankingCandidate, list[RankingEvidence]]] = []
    usable_evidence = [
        evidence
        for evidence in package.evidence
        if evidence.kind in {EvidenceKind.INTEREST, EvidenceKind.HINT}
    ]
    for candidate in package.candidates:
        haystack = " ".join(
            part
            for part in [
                candidate.title,
                candidate.variant_title or "",
                candidate.description or "",
                *candidate.categories,
                *candidate.tags,
            ]
            if part
        ).casefold()
        score = 0
        matches: list[RankingEvidence] = []
        for evidence in usable_evidence:
            evidence_score = _evidence_match_score(evidence, haystack)
            if evidence_score > 0:
                score += evidence_score
                matches.append(evidence)
        scored.append((score, -candidate.position, candidate, matches))
    scored.sort(key=lambda item: (item[0], item[1]), reverse=True)
    ranked = [item for item in scored if item[0] > 0][:3]
    if not ranked:
        return None
    rationales = [
        RankingRationaleResponse(
            candidate_id=candidate.id,
            evidence_ids=[item.id for item in matches],
            reason=_fallback_reason(matches[0]),
        )
        for _, _, candidate, matches in ranked
    ]
    return ValidatedRanking(
        selected_candidate_id=ranked[0][2].id,
        alternative_candidate_ids=[item[2].id for item in ranked[1:]],
        rationales=rationales,
        uncertainty=RankingUncertainty.HIGH,
    )


def _evidence_match_score(evidence: RankingEvidence, haystack: str) -> int:
    normalized = evidence.value.casefold().strip()
    if evidence.kind is EvidenceKind.INTEREST and normalized in haystack:
        return 5
    words = {
        word
        for word in re.findall(r"[a-z0-9]+", normalized)
        if len(word) >= 4 and word not in FALLBACK_STOP_WORDS
    }
    matches = sum(1 for word in words if word in haystack)
    return min(matches, 3) if evidence.kind is EvidenceKind.HINT else matches * 2


def _fallback_reason(evidence: RankingEvidence) -> str:
    if evidence.kind is EvidenceKind.INTEREST:
        return f"Matches the saved {evidence.value} interest."
    return "Connects directly to a saved gift clue."


class SqlRankingStore:
    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def claim(
        self,
        user_id: uuid.UUID,
        discovery_id: uuid.UUID,
        now: datetime,
    ) -> RankingClaim:
        async with self._session_factory() as session, session.begin():
            discovery = await session.scalar(
                select(DiscoveryRunModel)
                .where(
                    DiscoveryRunModel.id == discovery_id,
                    DiscoveryRunModel.user_id == user_id,
                )
                .with_for_update()
            )
            if discovery is None:
                raise _not_found()
            existing = await session.scalar(
                select(RankingRunModel)
                .where(RankingRunModel.discovery_run_id == discovery_id)
                .with_for_update()
            )
            if existing is not None:
                if existing.status == "COMPLETED":
                    return RankingClaim(
                        action=RankingClaimAction.REPLAY,
                        existing=await _ranking_response(session, existing),
                    )
                if existing.status == "USER_CHOICE_REQUIRED":
                    raise _user_choice_required()
                if existing.updated_at > now - RANKING_CLAIM_TTL:
                    raise ApiError(
                        status_code=409,
                        code="RANKING_IN_PROGRESS",
                        message="WishTrace is already choosing from these gifts.",
                        recoverable=True,
                    )
                existing.attempt_count += 1
                existing.failure_category = "STALE_ATTEMPT_RECOVERED"
                existing.started_at = now
                existing.updated_at = now
                package = await _ranking_package(session, discovery, existing)
                return RankingClaim(action=RankingClaimAction.CREATE, package=package)

            candidates = await _eligible_candidates(session, discovery)
            evidence_sources = await _load_evidence_sources(session, discovery, user_id)
            run = RankingRunModel(
                user_id=user_id,
                discovery_run_id=discovery.id,
                status="IN_PROGRESS",
                prompt_version=PROMPT_VERSION,
                schema_version=SCHEMA_VERSION,
                attempt_count=1,
                started_at=now,
                updated_at=now,
            )
            session.add(run)
            await session.flush()
            evidence: list[RankingEvidence] = []
            for position, (kind, source_ref, value) in enumerate(evidence_sources):
                evidence_uuid = uuid.uuid4()
                evidence_id = f"ev_{evidence_uuid.hex}"
                session.add(
                    RankingEvidenceModel(
                        id=evidence_uuid,
                        ranking_run_id=run.id,
                        evidence_id=evidence_id,
                        source_ref=source_ref,
                        position=position,
                        kind=kind.value,
                        value=value,
                    )
                )
                evidence.append(RankingEvidence(id=evidence_id, kind=kind, value=value))
            await session.flush()
            return RankingClaim(
                action=RankingClaimAction.CREATE,
                package=RankingPackage(
                    run_id=run.id,
                    discovery_id=discovery.id,
                    candidates=candidates,
                    evidence=evidence,
                ),
            )

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
        async with self._session_factory() as session, session.begin():
            run = await _owned_run(session, user_id, run_id, lock=True)
            if run.status == "COMPLETED":
                return await _ranking_response(session, run)
            if run.status != "IN_PROGRESS":
                raise _state_conflict()
            ranked_ids = [
                decision.selected_candidate_id,
                *decision.alternative_candidate_ids,
            ]
            candidates = (
                await session.scalars(
                    select(CandidateSnapshotModel).where(
                        CandidateSnapshotModel.id.in_(ranked_ids),
                        CandidateSnapshotModel.discovery_run_id == run.discovery_run_id,
                        CandidateSnapshotModel.eligible.is_(True),
                    )
                )
            ).all()
            if {item.id for item in candidates} != set(ranked_ids):
                raise _state_conflict()
            evidence_ids = {
                item.evidence_id
                for item in (
                    await session.scalars(
                        select(RankingEvidenceModel).where(
                            RankingEvidenceModel.ranking_run_id == run.id
                        )
                    )
                ).all()
            }
            for position, rationale in enumerate(decision.rationales):
                if any(item not in evidence_ids for item in rationale.evidence_ids):
                    raise _state_conflict()
                session.add(
                    RankingItemModel(
                        ranking_run_id=run.id,
                        candidate_snapshot_id=rationale.candidate_id,
                        position=position,
                        role="SELECTED" if position == 0 else "ALTERNATIVE",
                        reason=rationale.reason,
                        evidence_ids=rationale.evidence_ids,
                    )
                )
            completed_at = datetime.now(UTC)
            run.status = "COMPLETED"
            run.mode = mode.value
            run.uncertainty = decision.uncertainty.value
            run.provider_request_id = provider_request_id
            run.provider_deployment = provider_deployment
            run.duration_ms = duration_ms
            run.failure_category = failure_category
            run.completed_at = completed_at
            run.updated_at = completed_at
            await session.flush()
            return await _ranking_response(session, run)

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
        async with self._session_factory() as session, session.begin():
            run = await _owned_run(session, user_id, run_id, lock=True)
            if run.status == "COMPLETED":
                return
            if run.status != "IN_PROGRESS":
                raise _user_choice_required()
            completed_at = datetime.now(UTC)
            run.status = "USER_CHOICE_REQUIRED"
            run.provider_request_id = provider_request_id
            run.provider_deployment = provider_deployment
            run.duration_ms = duration_ms
            run.failure_category = failure_category
            run.completed_at = completed_at
            run.updated_at = completed_at
            await session.flush()

    async def get(self, user_id: uuid.UUID, discovery_id: uuid.UUID) -> RankingResponse:
        async with self._session_factory() as session:
            run = await session.scalar(
                select(RankingRunModel).where(
                    RankingRunModel.discovery_run_id == discovery_id,
                    RankingRunModel.user_id == user_id,
                )
            )
            if run is None:
                raise ApiError(
                    status_code=404,
                    code="RANKING_NOT_FOUND",
                    message="No gift decision exists for that search yet.",
                    recoverable=True,
                )
            if run.status == "IN_PROGRESS":
                raise ApiError(
                    status_code=409,
                    code="RANKING_IN_PROGRESS",
                    message="WishTrace is still choosing from these gifts.",
                    recoverable=True,
                )
            if run.status == "USER_CHOICE_REQUIRED":
                raise _user_choice_required()
            return await _ranking_response(session, run)


async def _ranking_package(
    session: AsyncSession,
    discovery: DiscoveryRunModel,
    run: RankingRunModel,
) -> RankingPackage:
    candidates = await _eligible_candidates(session, discovery)
    evidence_rows = (
        await session.scalars(
            select(RankingEvidenceModel)
            .where(RankingEvidenceModel.ranking_run_id == run.id)
            .order_by(RankingEvidenceModel.position)
        )
    ).all()
    if not evidence_rows:
        raise _context_changed()
    return RankingPackage(
        run_id=run.id,
        discovery_id=discovery.id,
        candidates=candidates,
        evidence=[
            RankingEvidence(
                id=item.evidence_id,
                kind=EvidenceKind(item.kind),
                value=item.value,
            )
            for item in evidence_rows
        ],
    )


async def _eligible_candidates(
    session: AsyncSession,
    discovery: DiscoveryRunModel,
) -> list[RankingCandidate]:
    snapshots = (
        await session.scalars(
            select(CandidateSnapshotModel)
            .where(CandidateSnapshotModel.discovery_run_id == discovery.id)
            .order_by(CandidateSnapshotModel.position)
        )
    ).all()
    eligible: list[RankingCandidate] = []
    for item in snapshots:
        if not item.eligible:
            continue
        if (
            item.source_mode != "LIVE"
            or not item.checkout_supported
            or item.availability != "AVAILABLE"
            or item.merchant_variant_id is None
            or item.currency != "USD"
            or item.price_minor > discovery.budget_minor
        ):
            raise ApiError(
                status_code=409,
                code="RANKING_DATA_INVALID",
                message="A gift option no longer passes WishTrace's hard checks.",
                recoverable=True,
            )
        eligible.append(
            RankingCandidate(
                id=item.id,
                position=item.position,
                title=item.title,
                variant_title=item.variant_title,
                description=item.description[:800] if item.description else None,
                categories=[value[:100] for value in item.categories[:20]],
                tags=[value[:100] for value in item.tags[:20]],
            )
        )
    if not eligible:
        raise ApiError(
            status_code=409,
            code="NO_ELIGIBLE_CANDIDATES",
            message="No live gift currently has a verified checkout path.",
            recoverable=True,
        )
    return eligible


async def _load_evidence_sources(
    session: AsyncSession,
    discovery: DiscoveryRunModel,
    user_id: uuid.UUID,
) -> list[tuple[EvidenceKind, str, str]]:
    recipient = await session.scalar(
        select(RecipientModel).where(
            RecipientModel.id == discovery.recipient_id,
            RecipientModel.user_id == user_id,
        )
    )
    occasion = await session.scalar(
        select(OccasionModel).where(
            OccasionModel.id == discovery.occasion_id,
            OccasionModel.user_id == user_id,
            OccasionModel.recipient_id == discovery.recipient_id,
        )
    )
    if recipient is None or occasion is None:
        raise _context_changed()
    preferences = (
        await session.scalars(
            select(RecipientPreferenceModel)
            .where(
                RecipientPreferenceModel.recipient_id == recipient.id,
                RecipientPreferenceModel.kind == "INTEREST",
            )
            .order_by(RecipientPreferenceModel.position)
        )
    ).all()
    if not preferences:
        raise _context_changed()
    hints = (
        await session.scalars(
            select(HintModel)
            .where(HintModel.recipient_id == recipient.id)
            .order_by(HintModel.created_at, HintModel.id)
        )
    ).all()
    sources: list[tuple[EvidenceKind, str, str]] = [
        (
            EvidenceKind.RELATIONSHIP,
            f"recipient:{recipient.id}:relationship",
            recipient.relationship.strip()[:500],
        ),
        (
            EvidenceKind.OCCASION,
            f"occasion:{occasion.id}:kind",
            occasion.kind.strip().title()[:500],
        ),
    ]
    sources.extend(
        (
            EvidenceKind.INTEREST,
            f"preference:{item.id}",
            item.value.strip()[:500],
        )
        for item in preferences
    )
    sources.extend(
        (
            EvidenceKind.HINT,
            f"hint:{item.id}",
            item.text.strip()[:500],
        )
        for item in hints
        if item.text.strip()
    )
    return [item for item in sources if item[2]]


async def _owned_run(
    session: AsyncSession,
    user_id: uuid.UUID,
    run_id: uuid.UUID,
    *,
    lock: bool,
) -> RankingRunModel:
    statement = select(RankingRunModel).where(
        RankingRunModel.id == run_id,
        RankingRunModel.user_id == user_id,
    )
    if lock:
        statement = statement.with_for_update()
    run = await session.scalar(statement)
    if run is None:
        raise _not_found()
    return run


async def _ranking_response(
    session: AsyncSession,
    run: RankingRunModel,
) -> RankingResponse:
    items = (
        await session.scalars(
            select(RankingItemModel)
            .where(RankingItemModel.ranking_run_id == run.id)
            .order_by(RankingItemModel.position)
        )
    ).all()
    evidence_rows = (
        await session.scalars(
            select(RankingEvidenceModel)
            .where(RankingEvidenceModel.ranking_run_id == run.id)
            .order_by(RankingEvidenceModel.position)
        )
    ).all()
    if (
        run.status != "COMPLETED"
        or run.mode is None
        or run.uncertainty is None
        or not items
        or items[0].role != "SELECTED"
    ):
        raise _state_conflict()
    return RankingResponse(
        id=run.id,
        discovery_id=run.discovery_run_id,
        selected_candidate_id=items[0].candidate_snapshot_id,
        alternative_candidate_ids=[
            item.candidate_snapshot_id for item in items[1:]
        ],
        rationales=[
            RankingRationaleResponse(
                candidate_id=item.candidate_snapshot_id,
                evidence_ids=item.evidence_ids,
                reason=item.reason,
            )
            for item in items
        ],
        evidence=[
            RankingEvidence(
                id=item.evidence_id,
                kind=EvidenceKind(item.kind),
                value=item.value,
            )
            for item in evidence_rows
        ],
        uncertainty=RankingUncertainty(run.uncertainty),
        mode=RankingMode(run.mode),
        model_request_id=run.provider_request_id,
        prompt_version=run.prompt_version,
        schema_version=run.schema_version,
        attempt_count=run.attempt_count,
        created_at=run.completed_at or run.started_at,
    )


def _not_found() -> ApiError:
    return ApiError(
        status_code=404,
        code="DISCOVERY_NOT_FOUND",
        message="That gift search was not found.",
        recoverable=True,
    )


def _context_changed() -> ApiError:
    return ApiError(
        status_code=409,
        code="RANKING_CONTEXT_CHANGED",
        message="Recipient context changed. Run gift discovery again.",
        recoverable=True,
    )


def _state_conflict() -> ApiError:
    return ApiError(
        status_code=409,
        code="RANKING_STATE_CONFLICT",
        message="Gift ranking state changed. Refresh before continuing.",
        recoverable=True,
    )


def _user_choice_required() -> ApiError:
    return ApiError(
        status_code=409,
        code="RANKING_REQUIRES_USER_CHOICE",
        message=(
            "The saved clues do not support one defensible choice. "
            "Choose from the eligible gifts."
        ),
        recoverable=True,
    )


def build_ranking_service(
    *,
    session_factory: async_sessionmaker[AsyncSession],
    gateway: RankingGateway,
) -> RankingOperations:
    return RankingService(store=SqlRankingStore(session_factory), gateway=gateway)
