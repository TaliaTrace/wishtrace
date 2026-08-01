import hashlib
import hmac
import uuid
from datetime import datetime

from httpx import ASGITransport, AsyncClient
from pydantic import SecretStr

from app.auth import (
    AuthenticatedUser,
    AuthService,
    AuthStore,
    GoogleIdentity,
    GoogleTokenVerifier,
    GoogleVerificationFailed,
    SessionTokenHasher,
)
from app.config import Settings
from app.database import DatabaseProbe
from app.main import create_app


class MemoryAuthStore(AuthStore):
    def __init__(self) -> None:
        self.challenges: dict[uuid.UUID, tuple[bytes, datetime, bool]] = {}
        self.sessions: dict[bytes, tuple[AuthenticatedUser, datetime, bool]] = {}
        self.users: dict[str, AuthenticatedUser] = {}

    async def create_challenge(self, nonce_hash: bytes, expires_at: datetime) -> uuid.UUID:
        challenge_id = uuid.uuid4()
        self.challenges[challenge_id] = (nonce_hash, expires_at, False)
        return challenge_id

    async def consume_challenge_and_create_session(
        self,
        *,
        challenge_id: uuid.UUID,
        nonce_hash: bytes,
        identity: GoogleIdentity,
        token_hash: bytes,
        session_expires_at: datetime,
        now: datetime,
    ) -> AuthenticatedUser | None:
        challenge = self.challenges.get(challenge_id)
        if challenge is None:
            return None
        expected_hash, challenge_expiry, consumed = challenge
        if (
            consumed
            or challenge_expiry <= now
            or not hmac.compare_digest(expected_hash, nonce_hash)
        ):
            return None
        self.challenges[challenge_id] = (expected_hash, challenge_expiry, True)
        user = self.users.get(identity.subject)
        if user is None:
            user = AuthenticatedUser(
                id=uuid.uuid4(),
                email=identity.email,
                display_name=identity.display_name,
                picture_url=identity.picture_url,
            )
            self.users[identity.subject] = user
        self.sessions[token_hash] = (user, session_expires_at, False)
        return user

    async def authenticate(
        self,
        token_hash: bytes,
        now: datetime,
    ) -> AuthenticatedUser | None:
        session = self.sessions.get(token_hash)
        if session is None:
            return None
        user, expires_at, revoked = session
        return user if not revoked and expires_at > now else None

    async def revoke(self, token_hash: bytes, now: datetime) -> bool:
        del now
        session = self.sessions.get(token_hash)
        if session is None or session[2]:
            return False
        self.sessions[token_hash] = (session[0], session[1], True)
        return True


class MutableGoogleVerifier(GoogleTokenVerifier):
    def __init__(self) -> None:
        self.nonce = ""
        self.fail = False
        self.audiences: list[str] = []

    async def verify(self, token: str, audience: str) -> GoogleIdentity:
        self.audiences.append(audience)
        if self.fail or token != "valid-google-id-token":
            raise GoogleVerificationFailed
        return GoogleIdentity(
            subject="google-subject-123",
            nonce=self.nonce,
            email="talia@example.com",
            email_verified=True,
            display_name="Talia",
            picture_url="https://example.invalid/talia.jpg",
        )


async def _auth_client(
    store: MemoryAuthStore,
    verifier: MutableGoogleVerifier,
) -> AsyncClient:
    settings = Settings(
        app_env="test",
        database_url=SecretStr(
            "postgresql://wishtrace:password@database.invalid:5432/wishtrace?sslmode=require"
        ),
        google_web_client_id=SecretStr("web-client.apps.googleusercontent.com"),
        session_token_pepper=SecretStr("p" * 32),
    )
    service = AuthService(
        store=store,
        verifier=verifier,
        google_audience="web-client.apps.googleusercontent.com",
        token_hasher=SessionTokenHasher("p" * 32),
    )

    async def healthy_database() -> DatabaseProbe:
        return DatabaseProbe(connected=True, tls=True, server_version="17.0")

    app = create_app(
        settings=settings,
        database_probe=healthy_database,
        auth_operations=service,
    )
    return AsyncClient(transport=ASGITransport(app=app), base_url="http://testserver")


async def test_challenge_exchange_me_and_logout() -> None:
    store = MemoryAuthStore()
    verifier = MutableGoogleVerifier()
    async with await _auth_client(store, verifier) as client:
        challenge_response = await client.post("/v1/auth/google/challenge")
        assert challenge_response.status_code == 200
        challenge = challenge_response.json()
        verifier.nonce = challenge["nonce"]

        stored_nonce_hash = store.challenges[uuid.UUID(challenge["challenge_id"])][0]
        assert stored_nonce_hash == hashlib.sha256(challenge["nonce"].encode()).digest()
        assert challenge["nonce"].encode() not in stored_nonce_hash

        exchange = await client.post(
            "/v1/auth/google/exchange",
            json={
                "challenge_id": challenge["challenge_id"],
                "id_token": "valid-google-id-token",
            },
        )
        assert exchange.status_code == 200
        session = exchange.json()
        assert session["access_token"].startswith("wt_")
        assert session["token_type"] == "Bearer"
        assert session["user"]["display_name"] == "Talia"
        assert verifier.audiences == ["web-client.apps.googleusercontent.com"]
        assert all(isinstance(key, bytes) for key in store.sessions)

        authorization = {"Authorization": f"Bearer {session['access_token']}"}
        me = await client.get("/v1/me", headers=authorization)
        assert me.status_code == 200
        assert me.json()["email"] == "talia@example.com"

        logout = await client.post("/v1/auth/logout", headers=authorization)
        assert logout.status_code == 204
        signed_out = await client.get("/v1/me", headers=authorization)
        assert signed_out.status_code == 401
        assert signed_out.json()["code"] == "AUTHENTICATION_REQUIRED"
        assert signed_out.headers["WWW-Authenticate"] == "Bearer"


async def test_challenge_is_single_use() -> None:
    store = MemoryAuthStore()
    verifier = MutableGoogleVerifier()
    async with await _auth_client(store, verifier) as client:
        challenge = (await client.post("/v1/auth/google/challenge")).json()
        verifier.nonce = challenge["nonce"]
        body = {
            "challenge_id": challenge["challenge_id"],
            "id_token": "valid-google-id-token",
        }

        first = await client.post("/v1/auth/google/exchange", json=body)
        replay = await client.post("/v1/auth/google/exchange", json=body)

        assert first.status_code == 200
        assert replay.status_code == 401
        assert replay.json()["code"] == "AUTH_CHALLENGE_INVALID"


async def test_nonce_mismatch_does_not_consume_challenge() -> None:
    store = MemoryAuthStore()
    verifier = MutableGoogleVerifier()
    async with await _auth_client(store, verifier) as client:
        challenge = (await client.post("/v1/auth/google/challenge")).json()
        verifier.nonce = "different-nonce"

        response = await client.post(
            "/v1/auth/google/exchange",
            json={
                "challenge_id": challenge["challenge_id"],
                "id_token": "valid-google-id-token",
            },
        )

        assert response.status_code == 401
        assert response.json()["code"] == "AUTH_CHALLENGE_INVALID"
        assert store.challenges[uuid.UUID(challenge["challenge_id"])][2] is False


async def test_invalid_google_token_uses_safe_error() -> None:
    store = MemoryAuthStore()
    verifier = MutableGoogleVerifier()
    verifier.fail = True
    async with await _auth_client(store, verifier) as client:
        challenge = (await client.post("/v1/auth/google/challenge")).json()
        response = await client.post(
            "/v1/auth/google/exchange",
            json={
                "challenge_id": challenge["challenge_id"],
                "id_token": "invalid-google-token-value",
            },
        )

        assert response.status_code == 401
        assert response.json()["code"] == "GOOGLE_TOKEN_INVALID"
        assert "invalid-google-token-value" not in response.text


async def test_missing_bearer_uses_api_error_envelope() -> None:
    store = MemoryAuthStore()
    verifier = MutableGoogleVerifier()
    async with await _auth_client(store, verifier) as client:
        response = await client.get("/v1/me")

        assert response.status_code == 401
        assert response.json()["code"] == "AUTHENTICATION_REQUIRED"
        assert response.headers["WWW-Authenticate"] == "Bearer"
