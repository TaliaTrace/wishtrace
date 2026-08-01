import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, Header, Request
from fastapi.responses import RedirectResponse

from app.auth import AuthenticatedUser
from app.auth_api import require_user
from app.purchase import (
    PublicTransactionStatus,
    PurchaseIntentCreate,
    PurchaseIntentResponse,
    PurchaseOperations,
    PurchaseQuoteRequest,
    ReceiptResponse,
    receipt,
    transaction_status,
)


def get_purchase_operations(request: Request) -> PurchaseOperations:
    service: PurchaseOperations = request.app.state.purchase_operations
    return service


def build_purchase_router(android_return_uri: str) -> APIRouter:
    router = APIRouter(prefix="/v1")

    @router.post("/purchase-intents", response_model=PurchaseIntentResponse, status_code=201)
    async def create_purchase_intent(
        body: PurchaseIntentCreate,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        purchase: Annotated[PurchaseOperations, Depends(get_purchase_operations)],
    ) -> PurchaseIntentResponse:
        return await purchase.create(user, body)

    @router.get("/purchase-intents/{purchase_intent_id}", response_model=PurchaseIntentResponse)
    async def get_purchase_intent(
        purchase_intent_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        purchase: Annotated[PurchaseOperations, Depends(get_purchase_operations)],
    ) -> PurchaseIntentResponse:
        return await purchase.get(user, purchase_intent_id)

    @router.post(
        "/purchase-intents/{purchase_intent_id}/quote",
        response_model=PurchaseIntentResponse,
    )
    async def quote_purchase_intent(
        purchase_intent_id: uuid.UUID,
        body: PurchaseQuoteRequest,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        purchase: Annotated[PurchaseOperations, Depends(get_purchase_operations)],
        idempotency_key: Annotated[
            str | None,
            Header(alias="Idempotency-Key"),
        ] = None,
    ) -> PurchaseIntentResponse:
        return await purchase.quote(
            user,
            purchase_intent_id,
            body,
            idempotency_key or "",
        )

    @router.post(
        "/purchase-intents/{purchase_intent_id}/prava-session",
        response_model=PurchaseIntentResponse,
    )
    async def create_prava_session(
        purchase_intent_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        purchase: Annotated[PurchaseOperations, Depends(get_purchase_operations)],
        idempotency_key: Annotated[
            str | None,
            Header(alias="Idempotency-Key"),
        ] = None,
    ) -> PurchaseIntentResponse:
        return await purchase.create_prava_session(
            user,
            purchase_intent_id,
            idempotency_key or "",
        )

    @router.post(
        "/purchase-intents/{purchase_intent_id}/reconcile",
        response_model=PurchaseIntentResponse,
    )
    async def reconcile_purchase_intent(
        purchase_intent_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        purchase: Annotated[PurchaseOperations, Depends(get_purchase_operations)],
    ) -> PurchaseIntentResponse:
        return await purchase.reconcile(user, purchase_intent_id)

    @router.get(
        "/purchase-intents/{purchase_intent_id}/status",
        response_model=PublicTransactionStatus,
    )
    async def get_purchase_status(
        purchase_intent_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        purchase: Annotated[PurchaseOperations, Depends(get_purchase_operations)],
    ) -> PublicTransactionStatus:
        return transaction_status(await purchase.get(user, purchase_intent_id))

    @router.get(
        "/purchase-intents/{purchase_intent_id}/receipt",
        response_model=ReceiptResponse,
    )
    async def get_purchase_receipt(
        purchase_intent_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        purchase: Annotated[PurchaseOperations, Depends(get_purchase_operations)],
    ) -> ReceiptResponse:
        return receipt(await purchase.get(user, purchase_intent_id))

    @router.get("/prava/return", include_in_schema=False)
    async def prava_return(purchase_intent_id: uuid.UUID) -> RedirectResponse:
        return RedirectResponse(
            f"{android_return_uri}?purchase_intent_id={purchase_intent_id}",
            status_code=302,
        )

    return router
