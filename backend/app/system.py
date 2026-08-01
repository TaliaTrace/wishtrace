from collections.abc import Awaitable, Callable

from fastapi import APIRouter, Response

from app.config import Settings
from app.database import DatabaseProbe
from app.errors import ApiError

DatabaseProbeCallable = Callable[[], Awaitable[DatabaseProbe]]
UCP_VERSION = "2026-04-08"


def build_system_router(
    settings: Settings,
    database_probe: DatabaseProbeCallable,
) -> APIRouter:
    router = APIRouter()

    @router.get("/health")
    async def health() -> dict[str, object]:
        try:
            database = await database_probe()
        except Exception as error:
            raise ApiError(
                status_code=503,
                code="DATABASE_UNAVAILABLE",
                message="WishTrace storage is temporarily unavailable.",
                recoverable=True,
            ) from error
        if not database.tls:
            raise ApiError(
                status_code=503,
                code="DATABASE_TLS_REQUIRED",
                message="WishTrace storage did not establish a secure connection.",
                recoverable=False,
            )
        return {
            "status": "ok",
            "service": settings.app_name,
            "version": settings.app_version,
            "database": {
                "connected": database.connected,
                "tls": database.tls,
                "server_version": database.server_version,
            },
        }

    @router.get("/.well-known/ucp")
    async def ucp_profile(response: Response) -> dict[str, object]:
        response.headers["Cache-Control"] = "public, max-age=300"
        return {
            "ucp": {
                "version": UCP_VERSION,
                "services": {
                    "dev.ucp.shopping": [
                        {
                            "version": UCP_VERSION,
                            "spec": f"https://ucp.dev/{UCP_VERSION}/specification/overview",
                            "transport": "mcp",
                            "schema": (
                                f"https://ucp.dev/{UCP_VERSION}/services/"
                                "shopping/mcp.openrpc.json"
                            ),
                        }
                    ]
                },
                "capabilities": {
                    "dev.ucp.shopping.catalog.search": [
                        {
                            "version": UCP_VERSION,
                            "spec": (
                                f"https://ucp.dev/{UCP_VERSION}/specification/catalog/search"
                            ),
                            "schema": (
                                f"https://ucp.dev/{UCP_VERSION}/schemas/"
                                "shopping/catalog_search.json"
                            ),
                        }
                    ],
                    "dev.ucp.shopping.catalog.lookup": [
                        {
                            "version": UCP_VERSION,
                            "spec": (
                                f"https://ucp.dev/{UCP_VERSION}/specification/catalog/lookup"
                            ),
                            "schema": (
                                f"https://ucp.dev/{UCP_VERSION}/schemas/"
                                "shopping/catalog_lookup.json"
                            ),
                        }
                    ],
                },
                "payment_handlers": {},
            }
        }

    return router
