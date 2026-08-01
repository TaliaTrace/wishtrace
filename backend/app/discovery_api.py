import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, Request

from app.auth import AuthenticatedUser
from app.auth_api import require_user
from app.discovery import DiscoveryCreate, DiscoveryOperations, DiscoveryResponse


def get_discovery_operations(request: Request) -> DiscoveryOperations:
    service: DiscoveryOperations = request.app.state.discovery_operations
    return service


def build_discovery_router() -> APIRouter:
    router = APIRouter(prefix="/v1")

    @router.post("/discoveries", response_model=DiscoveryResponse, status_code=201)
    async def create_discovery(
        body: DiscoveryCreate,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        discovery: Annotated[DiscoveryOperations, Depends(get_discovery_operations)],
    ) -> DiscoveryResponse:
        return await discovery.create(user.id, body)

    @router.get("/discoveries/{discovery_id}", response_model=DiscoveryResponse)
    async def get_discovery(
        discovery_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        discovery: Annotated[DiscoveryOperations, Depends(get_discovery_operations)],
    ) -> DiscoveryResponse:
        return await discovery.get(user.id, discovery_id)

    return router
