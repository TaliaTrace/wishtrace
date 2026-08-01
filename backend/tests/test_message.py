import uuid
from datetime import UTC, datetime

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
from app.message import (
    MessageOrigin,
    PersonalMessageResponse,
    PersonalMessageWrite,
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


class MemoryMessages:
    def __init__(self) -> None:
        self.saved: PersonalMessageResponse | None = None

    async def get(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
    ) -> PersonalMessageResponse:
        del user
        if self.saved is None or self.saved.purchase_intent_id != purchase_intent_id:
            raise ApiError(
                status_code=404,
                code="PERSONAL_MESSAGE_NOT_FOUND",
                message="No personal message has been saved yet.",
                recoverable=True,
            )
        return self.saved

    async def save_user_message(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
        body: PersonalMessageWrite,
    ) -> PersonalMessageResponse:
        del user
        now = datetime.now(UTC)
        self.saved = PersonalMessageResponse(
            id=self.saved.id if self.saved is not None else uuid.uuid4(),
            purchase_intent_id=purchase_intent_id,
            text=body.text,
            origin=MessageOrigin.USER,
            edited=(self.saved is not None and self.saved.text != body.text),
            created_at=self.saved.created_at if self.saved is not None else now,
            updated_at=now,
        )
        return self.saved


def test_personal_message_rejects_blank_and_control_characters() -> None:
    for value in ("   ", "Happy birthday\x00"):
        try:
            PersonalMessageWrite(text=value)
        except ValidationError:
            pass
        else:
            raise AssertionError("invalid personal message was accepted")


async def test_message_routes_save_edit_and_require_authentication() -> None:
    user = AuthenticatedUser(
        id=uuid.uuid4(),
        email="giver@example.com",
        display_name="Giver",
        picture_url=None,
    )
    messages = MemoryMessages()
    settings = Settings(
        app_env="test",
        database_url=SecretStr(
            "postgresql://wishtrace:password@database.invalid:5432/"
            "wishtrace?sslmode=require"
        ),
        public_base_url="https://api.wishtrace.example",
    )

    async def healthy_database() -> DatabaseProbe:
        return DatabaseProbe(connected=True, tls=True, server_version="17.0")

    app = create_app(
        settings=settings,
        database_probe=healthy_database,
        auth_operations=StaticAuth(user),
        message_operations=messages,
    )
    purchase_intent_id = uuid.uuid4()
    path = f"/v1/purchase-intents/{purchase_intent_id}/message"
    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://testserver",
    ) as client:
        unauthenticated = await client.post(path, json={"text": "Happy birthday!"})
        assert unauthenticated.status_code == 401

        created = await client.post(
            path,
            headers={"Authorization": "Bearer valid-session"},
            json={"text": "Happy birthday!"},
        )
        assert created.status_code == 200
        assert created.json()["origin"] == "USER"
        assert created.json()["edited"] is False

        edited = await client.post(
            path,
            headers={"Authorization": "Bearer valid-session"},
            json={"text": "Happy birthday, Zaid!"},
        )
        assert edited.status_code == 200
        assert edited.json()["edited"] is True

        fetched = await client.get(
            path,
            headers={"Authorization": "Bearer valid-session"},
        )
        assert fetched.status_code == 200
        assert fetched.json()["text"] == "Happy birthday, Zaid!"
