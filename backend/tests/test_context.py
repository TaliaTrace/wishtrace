import uuid
from datetime import date

import pytest
from httpx import ASGITransport, AsyncClient
from pydantic import SecretStr, ValidationError

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
from app.recipient_context import (
    ContextOperations,
    HintResponse,
    HomeResponse,
    OccasionResponse,
    OccasionWrite,
    RecipientResponse,
    RecipientWrite,
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


class MemoryContext(ContextOperations):
    def __init__(self) -> None:
        self.recipient: RecipientResponse | None = None
        self.occasion: OccasionResponse | None = None
        self.user_ids: list[uuid.UUID] = []

    async def list_recipients(self, user_id: uuid.UUID) -> list[RecipientResponse]:
        self.user_ids.append(user_id)
        return [self.recipient] if self.recipient is not None else []

    async def get_recipient(
        self,
        user_id: uuid.UUID,
        recipient_id: uuid.UUID,
    ) -> RecipientResponse:
        self.user_ids.append(user_id)
        if self.recipient is None or self.recipient.id != recipient_id:
            raise ApiError(
                status_code=404,
                code="RECIPIENT_NOT_FOUND",
                message="That person was not found.",
                recoverable=True,
            )
        return self.recipient

    async def save_recipient(
        self,
        user_id: uuid.UUID,
        body: RecipientWrite,
        recipient_id: uuid.UUID | None = None,
    ) -> RecipientResponse:
        self.user_ids.append(user_id)
        resolved_id = recipient_id or uuid.uuid4()
        self.recipient = RecipientResponse(
            id=resolved_id,
            display_name=body.display_name,
            relationship=body.relationship,
            initials="".join(part[0].upper() for part in body.display_name.split()[:2]),
            interests=body.interests,
            dislikes=body.dislikes,
            hints=(
                [
                    HintResponse(
                        id=uuid.uuid4(),
                        text=body.hint,
                        saved_on=date(2026, 8, 1),
                    )
                ]
                if body.hint is not None
                else []
            ),
        )
        return self.recipient

    async def save_occasion(
        self,
        user_id: uuid.UUID,
        body: OccasionWrite,
        occasion_id: uuid.UUID | None = None,
    ) -> OccasionResponse:
        self.user_ids.append(user_id)
        if self.recipient is None or self.recipient.id != body.recipient_id:
            raise ApiError(
                status_code=404,
                code="RECIPIENT_NOT_FOUND",
                message="That person was not found.",
                recoverable=True,
            )
        self.occasion = OccasionResponse(
            id=occasion_id or uuid.uuid4(),
            recipient_id=body.recipient_id,
            kind=body.kind,
            local_date=body.local_date,
            time_zone=body.time_zone,
            budget_minor=body.budget_minor,
            currency=body.currency,
            required_arrival_date=body.required_arrival_date,
        )
        return self.occasion

    async def get_home(self, user_id: uuid.UUID) -> HomeResponse:
        self.user_ids.append(user_id)
        return HomeResponse(
            recipient=self.recipient if self.occasion is not None else None,
            occasion=self.occasion,
            today=date(2026, 8, 1),
        )


async def _context_client(
    context: MemoryContext,
) -> tuple[AsyncClient, AuthenticatedUser]:
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
        context_operations=context,
    )
    return (
        AsyncClient(transport=ASGITransport(app=app), base_url="http://testserver"),
        user,
    )


def test_recipient_tags_are_trimmed_and_deduplicated() -> None:
    body = RecipientWrite(
        display_name=" Sophie ",
        relationship=" Close friend ",
        interests=["Gaming", " gaming ", "Books"],
        dislikes=["Clutter", "clutter"],
        hint="  Wants a new headset  ",
    )

    assert body.display_name == "Sophie"
    assert body.relationship == "Close friend"
    assert body.interests == ["Gaming", "Books"]
    assert body.dislikes == ["Clutter"]
    assert body.hint == "Wants a new headset"


@pytest.mark.parametrize(
    ("field", "value", "message"),
    [
        ("budget_minor", 0, "greater than 0"),
        ("time_zone", "Not/AZone", "valid IANA timezone"),
        ("local_date", date(2020, 1, 1), "cannot be in the past"),
    ],
)
def test_occasion_validation_rejects_invalid_facts(
    field: str,
    value: object,
    message: str,
) -> None:
    values: dict[str, object] = {
        "recipient_id": uuid.uuid4(),
        "kind": "BIRTHDAY",
        "local_date": date(2099, 8, 12),
        "time_zone": "Asia/Karachi",
        "budget_minor": 6_000,
        "currency": "USD",
        "required_arrival_date": date(2099, 8, 11),
    }
    values[field] = value

    with pytest.raises(ValidationError, match=message):
        OccasionWrite.model_validate(values)


async def test_authenticated_context_round_trip() -> None:
    context = MemoryContext()
    client, user = await _context_client(context)
    headers = {"Authorization": "Bearer valid-session"}
    async with client:
        created_recipient = await client.post(
            "/v1/recipients",
            headers=headers,
            json={
                "display_name": "Sophie",
                "relationship": "Close friend",
                "interests": ["Gaming", "Books"],
                "dislikes": ["Clutter"],
                "hint": "Wants a new headset",
            },
        )
        assert created_recipient.status_code == 201
        recipient_id = created_recipient.json()["id"]

        created_occasion = await client.post(
            "/v1/occasions",
            headers=headers,
            json={
                "recipient_id": recipient_id,
                "kind": "BIRTHDAY",
                "local_date": "2099-08-12",
                "time_zone": "Asia/Karachi",
                "budget_minor": 6000,
                "currency": "USD",
                "required_arrival_date": "2099-08-11",
            },
        )
        assert created_occasion.status_code == 201

        home = await client.get("/v1/home", headers=headers)
        assert home.status_code == 200
        assert home.json()["recipient"]["display_name"] == "Sophie"
        assert home.json()["occasion"]["budget_minor"] == 6000

    assert context.user_ids
    assert set(context.user_ids) == {user.id}


async def test_context_rejects_missing_auth_and_invalid_budget() -> None:
    context = MemoryContext()
    client, _user = await _context_client(context)
    async with client:
        missing_auth = await client.get("/v1/home")
        assert missing_auth.status_code == 401
        assert missing_auth.json()["code"] == "AUTHENTICATION_REQUIRED"

        invalid = await client.post(
            "/v1/occasions",
            headers={"Authorization": "Bearer valid-session"},
            json={
                "recipient_id": str(uuid.uuid4()),
                "kind": "BIRTHDAY",
                "local_date": "2099-08-12",
                "time_zone": "Asia/Karachi",
                "budget_minor": 0,
                "currency": "USD",
            },
        )
        assert invalid.status_code == 422
        assert invalid.json()["code"] == "VALIDATION_ERROR"
        assert "body.budget_minor" in invalid.json()["field_errors"]
