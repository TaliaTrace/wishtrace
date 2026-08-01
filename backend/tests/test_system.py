from httpx import AsyncClient
from pydantic import SecretStr

from app.config import Settings
from app.main import create_app


async def test_health_proves_tls_database_connection(client: AsyncClient) -> None:
    response = await client.get("/health", headers={"X-Correlation-ID": "test-health"})

    assert response.status_code == 200
    assert response.headers["X-Correlation-ID"] == "test-health"
    assert response.json() == {
        "status": "ok",
        "service": "WishTrace API",
        "version": "0.1.0",
        "database": {"connected": True, "tls": True, "server_version": "17.0"},
    }


async def test_health_uses_safe_error_envelope_when_database_fails() -> None:
    settings = Settings(
        app_env="test",
        database_url=SecretStr(
            "postgresql://wishtrace:password@database.invalid:5432/wishtrace?sslmode=require"
        ),
    )

    async def failed_database() -> None:
        raise RuntimeError("password=must-never-escape")

    app = create_app(settings=settings, database_probe=failed_database)  # type: ignore[arg-type]
    from httpx import ASGITransport

    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://testserver",
    ) as client:
        response = await client.get("/health")

    assert response.status_code == 503
    body = response.json()
    assert body["code"] == "DATABASE_UNAVAILABLE"
    assert body["recoverable"] is True
    assert body["field_errors"] == {}
    assert "password" not in response.text
    assert response.headers["X-Correlation-ID"] == body["correlation_id"]


async def test_ucp_profile_advertises_only_catalog_capabilities(client: AsyncClient) -> None:
    response = await client.get("/.well-known/ucp")

    assert response.status_code == 200
    assert response.headers["Cache-Control"] == "public, max-age=300"
    ucp = response.json()["ucp"]
    assert ucp["version"] == "2026-04-08"
    assert set(ucp["capabilities"]) == {
        "dev.ucp.shopping.catalog.search",
        "dev.ucp.shopping.catalog.lookup",
    }
    assert ucp["payment_handlers"] == {}
    assert "endpoint" not in ucp["services"]["dev.ucp.shopping"][0]


async def test_invalid_correlation_id_is_replaced(client: AsyncClient) -> None:
    response = await client.get("/.well-known/ucp", headers={"X-Correlation-ID": "bad id"})

    assert response.status_code == 200
    assert response.headers["X-Correlation-ID"] != "bad id"
