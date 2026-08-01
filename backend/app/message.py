import re
import uuid
from datetime import UTC, datetime
from enum import StrEnum
from typing import Protocol

from pydantic import BaseModel, ConfigDict, Field, field_validator
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.auth import AuthenticatedUser
from app.errors import ApiError
from app.models import PersonalMessageModel, PurchaseIntentModel

SAFE_MESSAGE_PATTERN = re.compile(r"^[^\x00-\x08\x0b\x0c\x0e-\x1f\x7f]{1,500}$")


class MessageOrigin(StrEnum):
    USER = "USER"
    AZURE_OPENAI = "AZURE_OPENAI"


class PersonalMessageWrite(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, str_strip_whitespace=True)

    text: str = Field(min_length=1, max_length=500)

    @field_validator("text")
    @classmethod
    def reject_control_characters(cls, value: str) -> str:
        if SAFE_MESSAGE_PATTERN.fullmatch(value) is None:
            raise ValueError("message contains unsupported control characters")
        return value


class PersonalMessageResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    id: uuid.UUID
    purchase_intent_id: uuid.UUID
    text: str
    origin: MessageOrigin
    edited: bool
    created_at: datetime
    updated_at: datetime


class MessageOperations(Protocol):
    async def get(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
    ) -> PersonalMessageResponse: ...

    async def save_user_message(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
        body: PersonalMessageWrite,
    ) -> PersonalMessageResponse: ...


class SqlMessageStore:
    def __init__(self, session_factory: async_sessionmaker[AsyncSession]) -> None:
        self._session_factory = session_factory

    async def get(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
    ) -> PersonalMessageResponse:
        async with self._session_factory() as session:
            message = await session.scalar(
                select(PersonalMessageModel).where(
                    PersonalMessageModel.purchase_intent_id == purchase_intent_id,
                    PersonalMessageModel.user_id == user.id,
                )
            )
            if message is None:
                raise _not_found()
            return _response(message)

    async def save_user_message(
        self,
        user: AuthenticatedUser,
        purchase_intent_id: uuid.UUID,
        body: PersonalMessageWrite,
    ) -> PersonalMessageResponse:
        async with self._session_factory() as session, session.begin():
            purchase = await session.scalar(
                select(PurchaseIntentModel)
                .where(
                    PurchaseIntentModel.id == purchase_intent_id,
                    PurchaseIntentModel.user_id == user.id,
                )
                .with_for_update()
            )
            if purchase is None:
                raise ApiError(
                    status_code=404,
                    code="PURCHASE_INTENT_NOT_FOUND",
                    message="That purchase review was not found.",
                    recoverable=True,
                )
            message = await session.scalar(
                select(PersonalMessageModel)
                .where(
                    PersonalMessageModel.purchase_intent_id == purchase_intent_id,
                    PersonalMessageModel.user_id == user.id,
                )
                .with_for_update()
            )
            now = datetime.now(UTC)
            if message is None:
                message = PersonalMessageModel(
                    user_id=user.id,
                    purchase_intent_id=purchase_intent_id,
                    text=body.text,
                    origin=MessageOrigin.USER.value,
                    edited=False,
                    updated_at=now,
                )
                session.add(message)
            elif message.text != body.text:
                message.text = body.text
                message.edited = True
                message.updated_at = now
            await session.flush()
            return _response(message)


def _response(message: PersonalMessageModel) -> PersonalMessageResponse:
    return PersonalMessageResponse(
        id=message.id,
        purchase_intent_id=message.purchase_intent_id,
        text=message.text,
        origin=MessageOrigin(message.origin),
        edited=message.edited,
        created_at=message.created_at,
        updated_at=message.updated_at,
    )


def _not_found() -> ApiError:
    return ApiError(
        status_code=404,
        code="PERSONAL_MESSAGE_NOT_FOUND",
        message="No personal message has been saved yet.",
        recoverable=True,
    )
