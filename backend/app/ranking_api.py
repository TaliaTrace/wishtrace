import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, Request

from app.auth import AuthenticatedUser
from app.auth_api import require_user
from app.ranking import RankingOperations, RankingResponse


def get_ranking_operations(request: Request) -> RankingOperations:
    service: RankingOperations = request.app.state.ranking_operations
    return service


def build_ranking_router() -> APIRouter:
    router = APIRouter(prefix="/v1")

    @router.post(
        "/discoveries/{discovery_id}/rank",
        response_model=RankingResponse,
    )
    async def rank_discovery(
        discovery_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        ranking: Annotated[RankingOperations, Depends(get_ranking_operations)],
    ) -> RankingResponse:
        return await ranking.rank(user.id, discovery_id)

    @router.get(
        "/discoveries/{discovery_id}/ranking",
        response_model=RankingResponse,
    )
    async def get_ranking(
        discovery_id: uuid.UUID,
        user: Annotated[AuthenticatedUser, Depends(require_user)],
        ranking: Annotated[RankingOperations, Depends(get_ranking_operations)],
    ) -> RankingResponse:
        return await ranking.get(user.id, discovery_id)

    return router
