import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, Request

from app.auth import AuthenticatedUser
from app.auth_api import require_user
from app.message import (
    MessageOperations,
    PersonalMessageResponse,
    PersonalMessageWrite,
)


def get_message_operations(request: Request) -> MessageOperations:
    service: MessageOperations = request.app.state.message_operations
    return service


def build_message_router() -> APIRouter:
    router = APIRouter(prefix="/v1")

    @router.get(
        "/purchase-intents/{purchase_intent_id}/message",
        response_model=PersonalMessageResponse,
    )
    async def get_personal_message(
        purchase_intent_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        messages: Annotated[MessageOperations, Depends(get_message_operations)],
    ) -> PersonalMessageResponse:
        return await messages.get(user, purchase_intent_id)

    @router.post(
        "/purchase-intents/{purchase_intent_id}/message",
        response_model=PersonalMessageResponse,
    )
    async def save_personal_message(
        purchase_intent_id: uuid.UUID,
        body: PersonalMessageWrite,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        messages: Annotated[MessageOperations, Depends(get_message_operations)],
    ) -> PersonalMessageResponse:
        return await messages.save_user_message(user, purchase_intent_id, body)

    return router
