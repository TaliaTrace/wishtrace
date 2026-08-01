import hashlib
import hmac
import secrets
import uuid
from collections.abc import Mapping
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from typing import Any, Protocol, cast

import anyio
from google.auth import exceptions as google_exceptions
from google.auth.transport.requests import Request as GoogleRequest
from google.oauth2 import id_token as google_id_token
from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import select, update
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.errors import ApiError
from app.models import AppSessionModel, AuthChallengeModel, UserModel

CHALLENGE_LIFETIME = timedelta(minutes=5)
SESSION_LIFETIME = timedelta(hours=24)
GOOGLE_ISSUERS = {"accounts.google.com", "https://accounts.google.com"}


def utc_now() -> datetime:
    return datetime.now(UTC)


def _token(byte_length: int = 32) -> str:
    return secrets.token_urlsafe(byte_length)


def _nonce_hash(nonce: str) -> bytes:
    return hashlib.sha256(nonce.encode()).digest()


class SessionTokenHasher:
    def __init__(self, pepper: str) -> None:
        encoded = pepper.encode()
        if len(encoded) < 32:
            raise ValueError("session token pepper must contain at least 32 bytes")
        self._pepper = encoded

    def hash(self, token: str) -> bytes:
        return hmac.digest(self._pepper, token.encode(), "sha256")


@dataclass(frozen=True, slots=True)
class GoogleIdentity:
    subject: str
    nonce: str
    email: str | None
    email_verified: bool
    display_name: str
    picture_url: str | None


@dataclass(frozen=True, slots=True)
class AuthenticatedUser:
    id: uuid.UUID
    email: str | None
    display_name: str
    picture_url: str | None


class GoogleVerificationFailed(Exception):
    pass


class GoogleTokenVerifier(Protocol):
    async def verify(self, token: str, audience: str) -> GoogleIdentity: ...


class GoogleOAuthTokenVerifier:
    async def verify(self, token: str, audience: str) -> GoogleIdentity:
        def verify_sync() -> Mapping[str, Any]:
            return cast(
                Mapping[str, Any],
                google_id_token.verify_oauth2_token(  # type: ignore[no-untyped-call]
                    token,
                    GoogleRequest(),
                    audience,
                ),
            )

        try:
            claims = await anyio.to_thread.run_sync(verify_sync)
        except (ValueError, google_exceptions.GoogleAuthError) as error:
            raise GoogleVerificationFailed from error

        issuer = claims.get("iss")
        subject = claims.get("sub")
        nonce = claims.get("nonce")
        if issuer not in GOOGLE_ISSUERS or not isinstance(subject, str) or not subject:
            raise GoogleVerificationFailed
        if not isinstance(nonce, str) or not nonce:
            raise GoogleVerificationFailed

        email_value = claims.get("email")
        email = email_value if isinstance(email_value, str) else None
        name_value = claims.get("name")
        display_name = (
            name_value if isinstance(name_value, str) and name_value else email or subject
        )
        picture_value = claims.get("picture")
        picture_url = picture_value if isinstance(picture_value, str) else None
        return GoogleIdentity(
            subject=subject,
            nonce=nonce,
            email=email,
            email_verified=claims.get("email_verified") is True,
            display_name=display_name,
            picture_url=picture_url,
        )


class AuthStore(Protocol):
    async def create_challenge(self, nonce_hash: bytes, expires_at: datetime) -> uuid.UUID: ...

    async def consume_challenge_and_create_session(
        self,
        *,
        challenge_id: uuid.UUID,
        nonce_hash: bytes,
        identity: GoogleIdentity,
        token_hash: bytes,
        session_expires_at: datetime,
        now: datetime,
    ) -> AuthenticatedUser | None: ...

    async def authenticate(
        self,
        token_hash: bytes,
        now: datetime,
    ) -> AuthenticatedUser | None: ...

    async def revoke(self, token_hash: bytes, now: datetime) -> bool: ...


class SqlAuthStore:
    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def create_challenge(self, nonce_hash: bytes, expires_at: datetime) -> uuid.UUID:
        challenge = AuthChallengeModel(nonce_hash=nonce_hash, expires_at=expires_at)
        async with self._session_factory() as session, session.begin():
            session.add(challenge)
        return challenge.id

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
        async with self._session_factory() as session, session.begin():
            consumed = await session.execute(
                update(AuthChallengeModel)
                .where(
                    AuthChallengeModel.id == challenge_id,
                    AuthChallengeModel.nonce_hash == nonce_hash,
                    AuthChallengeModel.consumed_at.is_(None),
                    AuthChallengeModel.expires_at > now,
                )
                .values(consumed_at=now)
                .returning(AuthChallengeModel.id)
            )
            if consumed.scalar_one_or_none() is None:
                return None

            user_result = await session.execute(
                insert(UserModel)
                .values(
                    google_subject=identity.subject,
                    email=identity.email,
                    email_verified=identity.email_verified,
                    display_name=identity.display_name,
                    picture_url=identity.picture_url,
                )
                .on_conflict_do_update(
                    index_elements=[UserModel.google_subject],
                    set_={
                        "email": identity.email,
                        "email_verified": identity.email_verified,
                        "display_name": identity.display_name,
                        "picture_url": identity.picture_url,
                        "updated_at": now,
                    },
                )
                .returning(UserModel)
            )
            user = user_result.scalar_one()
            session.add(
                AppSessionModel(
                    user_id=user.id,
                    token_hash=token_hash,
                    expires_at=session_expires_at,
                )
            )
            return _authenticated_user(user)

    async def authenticate(
        self,
        token_hash: bytes,
        now: datetime,
    ) -> AuthenticatedUser | None:
        async with self._session_factory() as session:
            user = await session.scalar(
                select(UserModel)
                .join(AppSessionModel, AppSessionModel.user_id == UserModel.id)
                .where(
                    AppSessionModel.token_hash == token_hash,
                    AppSessionModel.revoked_at.is_(None),
                    AppSessionModel.expires_at > now,
                )
            )
            return _authenticated_user(user) if user is not None else None

    async def revoke(self, token_hash: bytes, now: datetime) -> bool:
        async with self._session_factory() as session, session.begin():
            result = await session.execute(
                update(AppSessionModel)
                .where(
                    AppSessionModel.token_hash == token_hash,
                    AppSessionModel.revoked_at.is_(None),
                )
                .values(revoked_at=now)
                .returning(AppSessionModel.id)
            )
            return result.scalar_one_or_none() is not None


def _authenticated_user(user: UserModel) -> AuthenticatedUser:
    return AuthenticatedUser(
        id=user.id,
        email=user.email,
        display_name=user.display_name,
        picture_url=user.picture_url,
    )


class ChallengeResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    challenge_id: uuid.UUID
    nonce: str
    expires_at: datetime


class GoogleExchangeRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    challenge_id: uuid.UUID
    id_token: str = Field(min_length=20, max_length=16_384)


class UserResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: uuid.UUID
    email: str | None
    display_name: str
    picture_url: str | None


class SessionResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    access_token: str
    token_type: str = "Bearer"
    expires_at: datetime
    user: UserResponse


class AuthOperations(Protocol):
    async def create_challenge(self) -> ChallengeResponse: ...

    async def exchange(self, request: GoogleExchangeRequest) -> SessionResponse: ...

    async def authenticate(self, token: str) -> AuthenticatedUser: ...

    async def logout(self, token: str) -> None: ...


class AuthService:
    def __init__(
        self,
        *,
        store: AuthStore,
        verifier: GoogleTokenVerifier,
        google_audience: str,
        token_hasher: SessionTokenHasher,
    ) -> None:
        self._store = store
        self._verifier = verifier
        self._google_audience = google_audience
        self._token_hasher = token_hasher

    async def create_challenge(self) -> ChallengeResponse:
        nonce = _token()
        expires_at = utc_now() + CHALLENGE_LIFETIME
        challenge_id = await self._store.create_challenge(_nonce_hash(nonce), expires_at)
        return ChallengeResponse(
            challenge_id=challenge_id,
            nonce=nonce,
            expires_at=expires_at,
        )

    async def exchange(self, request: GoogleExchangeRequest) -> SessionResponse:
        try:
            identity = await self._verifier.verify(request.id_token, self._google_audience)
        except GoogleVerificationFailed as error:
            raise ApiError(
                status_code=401,
                code="GOOGLE_TOKEN_INVALID",
                message="Google could not verify this sign-in.",
                recoverable=True,
            ) from error

        access_token = f"wt_{_token(48)}"
        now = utc_now()
        expires_at = now + SESSION_LIFETIME
        user = await self._store.consume_challenge_and_create_session(
            challenge_id=request.challenge_id,
            nonce_hash=_nonce_hash(identity.nonce),
            identity=identity,
            token_hash=self._token_hasher.hash(access_token),
            session_expires_at=expires_at,
            now=now,
        )
        if user is None:
            raise ApiError(
                status_code=401,
                code="AUTH_CHALLENGE_INVALID",
                message="This sign-in request expired or was already used.",
                recoverable=True,
            )
        return SessionResponse(
            access_token=access_token,
            expires_at=expires_at,
            user=_user_response(user),
        )

    async def authenticate(self, token: str) -> AuthenticatedUser:
        user = await self._store.authenticate(self._token_hasher.hash(token), utc_now())
        if user is None:
            raise _authentication_required()
        return user

    async def logout(self, token: str) -> None:
        revoked = await self._store.revoke(self._token_hasher.hash(token), utc_now())
        if not revoked:
            raise _authentication_required()


class UnavailableAuthService:
    async def create_challenge(self) -> ChallengeResponse:
        raise self._error()

    async def exchange(self, request: GoogleExchangeRequest) -> SessionResponse:
        del request
        raise self._error()

    async def authenticate(self, token: str) -> AuthenticatedUser:
        del token
        raise self._error()

    async def logout(self, token: str) -> None:
        del token
        raise self._error()

    @staticmethod
    def _error() -> ApiError:
        return ApiError(
            status_code=503,
            code="AUTH_UNAVAILABLE",
            message="WishTrace sign-in is temporarily unavailable.",
            recoverable=True,
        )


def _user_response(user: AuthenticatedUser) -> UserResponse:
    return UserResponse(
        id=user.id,
        email=user.email,
        display_name=user.display_name,
        picture_url=user.picture_url,
    )


def _authentication_required() -> ApiError:
    return ApiError(
        status_code=401,
        code="AUTHENTICATION_REQUIRED",
        message="Sign in again to continue.",
        recoverable=True,
        headers={"WWW-Authenticate": "Bearer"},
    )


def build_auth_service(
    *,
    session_factory: async_sessionmaker[AsyncSession],
    google_audience: str | None,
    session_token_pepper: str | None,
) -> AuthOperations:
    if not google_audience or not session_token_pepper:
        return UnavailableAuthService()
    return AuthService(
        store=SqlAuthStore(session_factory),
        verifier=GoogleOAuthTokenVerifier(),
        google_audience=google_audience,
        token_hasher=SessionTokenHasher(session_token_pepper),
    )
