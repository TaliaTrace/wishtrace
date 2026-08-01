from typing import Annotated

from fastapi import APIRouter, Depends, Request, Response
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.auth import (
    AuthenticatedUser,
    AuthOperations,
    ChallengeResponse,
    GoogleExchangeRequest,
    SessionResponse,
    UserResponse,
)
from app.errors import ApiError

bearer = HTTPBearer(auto_error=False)


def get_auth_operations(request: Request) -> AuthOperations:
    service: AuthOperations = request.app.state.auth_operations
    return service


async def require_bearer_token(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)],
) -> str:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise ApiError(
            status_code=401,
            code="AUTHENTICATION_REQUIRED",
            message="Sign in again to continue.",
            recoverable=True,
            headers={"WWW-Authenticate": "Bearer"},
        )
    return credentials.credentials


async def require_user(
    token: Annotated[str, Depends(require_bearer_token)],
    auth: Annotated[AuthOperations, Depends(get_auth_operations)],
) -> AuthenticatedUser:
    return await auth.authenticate(token)


def build_auth_router() -> APIRouter:
    router = APIRouter(prefix="/v1")

    @router.post("/auth/google/challenge", response_model=ChallengeResponse)
    async def create_challenge(
        auth: Annotated[AuthOperations, Depends(get_auth_operations)],
    ) -> ChallengeResponse:
        return await auth.create_challenge()

    @router.post("/auth/google/exchange", response_model=SessionResponse)
    async def exchange_google_token(
        body: GoogleExchangeRequest,
        auth: Annotated[AuthOperations, Depends(get_auth_operations)],
    ) -> SessionResponse:
        return await auth.exchange(body)

    @router.post("/auth/logout", status_code=204)
    async def logout(
        token: Annotated[str, Depends(require_bearer_token)],
        auth: Annotated[AuthOperations, Depends(get_auth_operations)],
    ) -> Response:
        await auth.logout(token)
        return Response(status_code=204)

    @router.get("/me", response_model=UserResponse)
    async def me(user: Annotated[AuthenticatedUser, Depends(require_user)]) -> UserResponse:
        return UserResponse(
            id=user.id,
            email=user.email,
            display_name=user.display_name,
            picture_url=user.picture_url,
        )

    return router
