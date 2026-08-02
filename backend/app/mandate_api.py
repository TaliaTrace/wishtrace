import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, Header, Request
from fastapi.responses import RedirectResponse

from app.auth import AuthenticatedUser
from app.auth_api import require_user
from app.mandate import (
    MandateExecuteRequest,
    MandateOperations,
    MandateResponse,
    MandateSetupRequest,
)


def get_mandate_operations(request: Request) -> MandateOperations:
    service: MandateOperations = request.app.state.mandate_operations
    return service


def build_mandate_router(android_return_uri: str) -> APIRouter:
    router = APIRouter(prefix="/v1")

    @router.get(
        "/occasions/{occasion_id}/mandate",
        response_model=MandateResponse,
    )
    async def get_mandate(
        occasion_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        mandate: Annotated[MandateOperations, Depends(get_mandate_operations)],
    ) -> MandateResponse:
        return await mandate.get(user, occasion_id)

    @router.post(
        "/occasions/{occasion_id}/mandate/setup",
        response_model=MandateResponse,
        status_code=201,
    )
    async def setup_mandate(
        occasion_id: uuid.UUID,
        body: MandateSetupRequest,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        mandate: Annotated[MandateOperations, Depends(get_mandate_operations)],
    ) -> MandateResponse:
        return await mandate.setup(user, occasion_id, body)

    @router.post(
        "/occasions/{occasion_id}/mandate/refresh",
        response_model=MandateResponse,
    )
    async def refresh_mandate(
        occasion_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        mandate: Annotated[MandateOperations, Depends(get_mandate_operations)],
    ) -> MandateResponse:
        return await mandate.refresh(user, occasion_id)

    @router.post(
        "/occasions/{occasion_id}/mandate/cancel",
        response_model=MandateResponse,
    )
    async def cancel_mandate(
        occasion_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        mandate: Annotated[MandateOperations, Depends(get_mandate_operations)],
    ) -> MandateResponse:
        return await mandate.cancel(user, occasion_id)

    @router.post(
        "/occasions/{occasion_id}/mandate/execute",
        response_model=MandateResponse,
    )
    async def execute_mandate(
        occasion_id: uuid.UUID,
        body: MandateExecuteRequest,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        mandate: Annotated[MandateOperations, Depends(get_mandate_operations)],
        idempotency_key: Annotated[
            str | None,
            Header(alias="Idempotency-Key"),
        ] = None,
    ) -> MandateResponse:
        return await mandate.execute(
            user,
            occasion_id,
            body,
            idempotency_key or "",
        )

    @router.get("/prava/mandate-return", include_in_schema=False)
    async def prava_mandate_return(occasion_id: uuid.UUID) -> RedirectResponse:
        return RedirectResponse(
            f"{android_return_uri}?occasion_id={occasion_id}",
            status_code=302,
        )

    return router
