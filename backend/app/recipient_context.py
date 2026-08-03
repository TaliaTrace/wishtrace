import uuid
from datetime import UTC, date, datetime
from typing import Literal, Protocol
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.errors import ApiError
from app.models import HintModel, OccasionModel, RecipientModel, RecipientPreferenceModel

AgeBand = Literal["child", "teen", "young_adult", "adult", "senior"]


class PersonalityTraits(BaseModel):
    """Green-tile "Gift DNA": three binary axes, all optional.

    Captured from three either/or taps rather than free-text so onboarding needs
    zero typing. Any axis may be omitted; an empty object means "no signal yet".
    """

    model_config = ConfigDict(extra="forbid")

    # ⚔️ Competitive ↔ 🌿 Chill
    energy: Literal["competitive", "chill"] | None = None
    # 📱 Screens ↔ 🏔️ Outdoors
    environment: Literal["screens", "outdoors"] | None = None
    # ✨ Trendy ↔ 📻 Nostalgic
    style: Literal["trendy", "nostalgic"] | None = None

    def as_storage(self) -> dict[str, str] | None:
        """Only persist axes the owner actually set; None when nothing is set."""
        stored = {
            key: value
            for key, value in self.model_dump().items()
            if value is not None
        }
        return stored or None


class RecipientWrite(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    display_name: str = Field(min_length=1, max_length=100)
    relationship: str = Field(min_length=1, max_length=100)
    # Interests are now optional: the green-tile taps (personality_traits) plus
    # relationship and age band are enough for a strong first-pass ranking.
    interests: list[str] = Field(default_factory=list, max_length=10)
    dislikes: list[str] = Field(default_factory=list, max_length=10)
    personality_traits: PersonalityTraits = Field(default_factory=PersonalityTraits)
    age_band: AgeBand | None = None
    hint: str | None = Field(default=None, max_length=1_000)

    @field_validator("interests", "dislikes")
    @classmethod
    def normalize_tags(cls, values: list[str]) -> list[str]:
        normalized: list[str] = []
        seen: set[str] = set()
        for value in values:
            item = value.strip()
            if not item:
                raise ValueError("tags cannot be blank")
            if len(item) > 100:
                raise ValueError("tags cannot exceed 100 characters")
            key = item.casefold()
            if key not in seen:
                normalized.append(item)
                seen.add(key)
        return normalized

    @field_validator("hint")
    @classmethod
    def normalize_hint(cls, value: str | None) -> str | None:
        if value is None:
            return None
        return value.strip() or None


class OccasionWrite(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    recipient_id: uuid.UUID
    kind: Literal["BIRTHDAY"]
    local_date: date
    time_zone: str = Field(min_length=1, max_length=64)
    budget_minor: int = Field(gt=0, le=10_000_000)
    currency: Literal["USD"]
    # Yellow-tile toggle: "Just this once" vs "Every year, automatically" — this
    # is what arms the mandate as one-time vs recurring.
    recurring_frequency: Literal["one_time", "yearly"] = "one_time"
    required_arrival_date: date | None = None

    @model_validator(mode="after")
    def validate_dates(self) -> "OccasionWrite":
        try:
            today = datetime.now(ZoneInfo(self.time_zone)).date()
        except ZoneInfoNotFoundError as error:
            raise ValueError("time_zone must be a valid IANA timezone") from error
        if self.local_date < today:
            raise ValueError("local_date cannot be in the past")
        if self.required_arrival_date is not None:
            if self.required_arrival_date < today:
                raise ValueError("required_arrival_date cannot be in the past")
            if self.required_arrival_date > self.local_date:
                raise ValueError("required_arrival_date cannot follow the occasion")
        return self


class HintResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: uuid.UUID
    text: str
    source_label: str = "Saved note"
    saved_on: date


class RecipientResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: uuid.UUID
    display_name: str
    relationship: str
    initials: str
    interests: list[str]
    dislikes: list[str]
    personality_traits: PersonalityTraits
    age_band: AgeBand | None
    hints: list[HintResponse]


class OccasionResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: uuid.UUID
    recipient_id: uuid.UUID
    kind: Literal["BIRTHDAY"]
    local_date: date
    time_zone: str
    budget_minor: int
    currency: Literal["USD"]
    recurring_frequency: Literal["one_time", "yearly"]
    required_arrival_date: date | None


class HomeResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    recipient: RecipientResponse | None
    occasion: OccasionResponse | None
    today: date


class ContextOperations(Protocol):
    async def list_recipients(self, user_id: uuid.UUID) -> list[RecipientResponse]: ...

    async def get_recipient(
        self,
        user_id: uuid.UUID,
        recipient_id: uuid.UUID,
    ) -> RecipientResponse: ...

    async def save_recipient(
        self,
        user_id: uuid.UUID,
        body: RecipientWrite,
        recipient_id: uuid.UUID | None = None,
    ) -> RecipientResponse: ...

    async def save_occasion(
        self,
        user_id: uuid.UUID,
        body: OccasionWrite,
        occasion_id: uuid.UUID | None = None,
    ) -> OccasionResponse: ...

    async def list_occasions(
        self,
        user_id: uuid.UUID,
        recipient_id: uuid.UUID | None = None,
    ) -> list[OccasionResponse]: ...

    async def get_home(self, user_id: uuid.UUID) -> HomeResponse: ...


class SqlContextStore:
    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def list_recipients(self, user_id: uuid.UUID) -> list[RecipientResponse]:
        async with self._session_factory() as session:
            recipients = (
                await session.scalars(
                    select(RecipientModel)
                    .where(RecipientModel.user_id == user_id)
                    .order_by(RecipientModel.created_at, RecipientModel.id)
                )
            ).all()
            return [await _recipient_response(session, recipient) for recipient in recipients]

    async def get_recipient(
        self,
        user_id: uuid.UUID,
        recipient_id: uuid.UUID,
    ) -> RecipientResponse:
        async with self._session_factory() as session:
            recipient = await _owned_recipient(session, user_id, recipient_id)
            return await _recipient_response(session, recipient)

    async def save_recipient(
        self,
        user_id: uuid.UUID,
        body: RecipientWrite,
        recipient_id: uuid.UUID | None = None,
    ) -> RecipientResponse:
        async with self._session_factory() as session, session.begin():
            if recipient_id is None:
                recipient = RecipientModel(
                    user_id=user_id,
                    display_name=body.display_name,
                    relationship=body.relationship,
                    personality_traits=body.personality_traits.as_storage(),
                    age_band=body.age_band,
                )
                session.add(recipient)
                await session.flush()
            else:
                recipient = await _owned_recipient(session, user_id, recipient_id, lock=True)
                recipient.display_name = body.display_name
                recipient.relationship = body.relationship
                recipient.personality_traits = body.personality_traits.as_storage()
                recipient.age_band = body.age_band
                recipient.updated_at = datetime.now(UTC)

            await session.execute(
                delete(RecipientPreferenceModel).where(
                    RecipientPreferenceModel.recipient_id == recipient.id
                )
            )
            await session.execute(
                delete(HintModel).where(HintModel.recipient_id == recipient.id)
            )

            for position, value in enumerate(body.interests):
                session.add(
                    RecipientPreferenceModel(
                        recipient_id=recipient.id,
                        kind="INTEREST",
                        value=value,
                        position=position,
                    )
                )
            for position, value in enumerate(body.dislikes):
                session.add(
                    RecipientPreferenceModel(
                        recipient_id=recipient.id,
                        kind="DISLIKE",
                        value=value,
                        position=position,
                    )
                )
            if body.hint is not None:
                session.add(
                    HintModel(
                        recipient_id=recipient.id,
                        text=body.hint,
                        created_at=datetime.now(UTC),
                    )
                )
            await session.flush()
            response = await _recipient_response(session, recipient)
            return response

    async def list_occasions(
        self,
        user_id: uuid.UUID,
        recipient_id: uuid.UUID | None = None,
    ) -> list[OccasionResponse]:
        async with self._session_factory() as session:
            if recipient_id is not None:
                await _owned_recipient(session, user_id, recipient_id)
            statement = select(OccasionModel).where(OccasionModel.user_id == user_id)
            if recipient_id is not None:
                statement = statement.where(OccasionModel.recipient_id == recipient_id)
            occasions = (
                await session.scalars(
                    statement.order_by(OccasionModel.local_date, OccasionModel.created_at)
                )
            ).all()
            return [_occasion_response(occasion) for occasion in occasions]

    async def save_occasion(
        self,
        user_id: uuid.UUID,
        body: OccasionWrite,
        occasion_id: uuid.UUID | None = None,
    ) -> OccasionResponse:
        async with self._session_factory() as session, session.begin():
            await _owned_recipient(session, user_id, body.recipient_id)
            occasion: OccasionModel | None
            if occasion_id is None:
                occasion = await session.scalar(
                    select(OccasionModel)
                    .where(
                        OccasionModel.user_id == user_id,
                        OccasionModel.recipient_id == body.recipient_id,
                        OccasionModel.kind == body.kind,
                    )
                    .with_for_update()
                )
                if occasion is None:
                    occasion = OccasionModel(
                        user_id=user_id,
                        recipient_id=body.recipient_id,
                        kind=body.kind,
                        local_date=body.local_date,
                        time_zone=body.time_zone,
                        budget_minor=body.budget_minor,
                        currency=body.currency,
                        recurring_frequency=body.recurring_frequency,
                        required_arrival_date=body.required_arrival_date,
                    )
                    session.add(occasion)
                    await session.flush()
            else:
                occasion = await session.scalar(
                    select(OccasionModel)
                    .where(
                        OccasionModel.id == occasion_id,
                        OccasionModel.user_id == user_id,
                    )
                    .with_for_update()
                )
                if occasion is None:
                    raise _not_found("OCCASION_NOT_FOUND", "That occasion was not found.")
                if occasion.recipient_id != body.recipient_id:
                    raise ApiError(
                        status_code=409,
                        code="OCCASION_RECIPIENT_MISMATCH",
                        message="That occasion belongs to another person.",
                        recoverable=True,
                    )

            occasion.local_date = body.local_date
            occasion.time_zone = body.time_zone
            occasion.budget_minor = body.budget_minor
            occasion.currency = body.currency
            occasion.recurring_frequency = body.recurring_frequency
            occasion.required_arrival_date = body.required_arrival_date
            occasion.updated_at = datetime.now(UTC)
            await session.flush()
            return _occasion_response(occasion)

    async def get_home(self, user_id: uuid.UUID) -> HomeResponse:
        async with self._session_factory() as session:
            occasions = (
                await session.scalars(
                    select(OccasionModel)
                    .where(OccasionModel.user_id == user_id)
                    .order_by(OccasionModel.local_date, OccasionModel.created_at)
                )
            ).all()
            occasion = next(
                (
                    item
                    for item in occasions
                    if item.local_date >= datetime.now(ZoneInfo(item.time_zone)).date()
                ),
                None,
            )
            if occasion is None:
                return HomeResponse(
                    recipient=None,
                    occasion=None,
                    today=datetime.now(UTC).date(),
                )
            recipient = await _owned_recipient(session, user_id, occasion.recipient_id)
            today = datetime.now(ZoneInfo(occasion.time_zone)).date()
            return HomeResponse(
                recipient=await _recipient_response(session, recipient),
                occasion=_occasion_response(occasion),
                today=today,
            )


async def _owned_recipient(
    session: AsyncSession,
    user_id: uuid.UUID,
    recipient_id: uuid.UUID,
    *,
    lock: bool = False,
) -> RecipientModel:
    statement = select(RecipientModel).where(
        RecipientModel.id == recipient_id,
        RecipientModel.user_id == user_id,
    )
    if lock:
        statement = statement.with_for_update()
    recipient = await session.scalar(statement)
    if recipient is None:
        raise _not_found("RECIPIENT_NOT_FOUND", "That person was not found.")
    return recipient


async def _recipient_response(
    session: AsyncSession,
    recipient: RecipientModel,
) -> RecipientResponse:
    preferences = (
        await session.scalars(
            select(RecipientPreferenceModel)
            .where(RecipientPreferenceModel.recipient_id == recipient.id)
            .order_by(RecipientPreferenceModel.kind, RecipientPreferenceModel.position)
        )
    ).all()
    hints = (
        await session.scalars(
            select(HintModel)
            .where(HintModel.recipient_id == recipient.id)
            .order_by(HintModel.created_at, HintModel.id)
        )
    ).all()
    return RecipientResponse(
        id=recipient.id,
        display_name=recipient.display_name,
        relationship=recipient.relationship,
        initials=_initials(recipient.display_name),
        interests=[item.value for item in preferences if item.kind == "INTEREST"],
        dislikes=[item.value for item in preferences if item.kind == "DISLIKE"],
        personality_traits=PersonalityTraits.model_validate(
            recipient.personality_traits or {}
        ),
        age_band=recipient.age_band,
        hints=[
            HintResponse(
                id=hint.id,
                text=hint.text,
                saved_on=hint.created_at.date(),
            )
            for hint in hints
        ],
    )


def _occasion_response(occasion: OccasionModel) -> OccasionResponse:
    return OccasionResponse(
        id=occasion.id,
        recipient_id=occasion.recipient_id,
        kind="BIRTHDAY",
        local_date=occasion.local_date,
        time_zone=occasion.time_zone,
        budget_minor=occasion.budget_minor,
        currency="USD",
        recurring_frequency=occasion.recurring_frequency,
        required_arrival_date=occasion.required_arrival_date,
    )


def _initials(name: str) -> str:
    return "".join(part[0].upper() for part in name.split()[:2] if part)


def _not_found(code: str, message: str) -> ApiError:
    return ApiError(
        status_code=404,
        code=code,
        message=message,
        recoverable=True,
    )
