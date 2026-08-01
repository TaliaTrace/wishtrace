import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, Request

from app.auth import AuthenticatedUser
from app.auth_api import require_user
from app.recipient_context import (
    ContextOperations,
    HomeResponse,
    OccasionResponse,
    OccasionWrite,
    RecipientResponse,
    RecipientWrite,
)


def get_context_operations(request: Request) -> ContextOperations:
    service: ContextOperations = request.app.state.context_operations
    return service


def build_context_router() -> APIRouter:
    router = APIRouter(prefix="/v1")

    @router.get("/recipients", response_model=list[RecipientResponse])
    async def list_recipients(
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        context: Annotated[ContextOperations, Depends(get_context_operations)],
    ) -> list[RecipientResponse]:
        return await context.list_recipients(user.id)

    @router.post("/recipients", response_model=RecipientResponse, status_code=201)
    async def create_recipient(
        body: RecipientWrite,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        context: Annotated[ContextOperations, Depends(get_context_operations)],
    ) -> RecipientResponse:
        return await context.save_recipient(user.id, body)

    @router.get("/recipients/{recipient_id}", response_model=RecipientResponse)
    async def get_recipient(
        recipient_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        context: Annotated[ContextOperations, Depends(get_context_operations)],
    ) -> RecipientResponse:
        return await context.get_recipient(user.id, recipient_id)

    @router.put("/recipients/{recipient_id}", response_model=RecipientResponse)
    async def update_recipient(
        recipient_id: uuid.UUID,
        body: RecipientWrite,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        context: Annotated[ContextOperations, Depends(get_context_operations)],
    ) -> RecipientResponse:
        return await context.save_recipient(user.id, body, recipient_id)

    @router.post("/occasions", response_model=OccasionResponse, status_code=201)
    async def create_occasion(
        body: OccasionWrite,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        context: Annotated[ContextOperations, Depends(get_context_operations)],
    ) -> OccasionResponse:
        return await context.save_occasion(user.id, body)

    @router.put("/occasions/{occasion_id}", response_model=OccasionResponse)
    async def update_occasion(
        occasion_id: uuid.UUID,
        body: OccasionWrite,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        context: Annotated[ContextOperations, Depends(get_context_operations)],
    ) -> OccasionResponse:
        return await context.save_occasion(user.id, body, occasion_id)

    @router.get("/home", response_model=HomeResponse)
    async def home(
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        context: Annotated[ContextOperations, Depends(get_context_operations)],
    ) -> HomeResponse:
        return await context.get_home(user.id)

    return router
