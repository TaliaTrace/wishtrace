from dataclasses import dataclass
from typing import Any, cast

from psycopg import AsyncConnection
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine
from sqlalchemy.pool import NullPool

from app.config import Settings


@dataclass(frozen=True, slots=True)
class DatabaseProbe:
    connected: bool
    tls: bool
    server_version: str


def create_database_engine(settings: Settings) -> AsyncEngine:
    return create_async_engine(
        settings.sqlalchemy_database_url,
        poolclass=NullPool,
        hide_parameters=True,
    )


async def probe_database(engine: AsyncEngine) -> DatabaseProbe:
    async with engine.connect() as connection:
        result = await connection.execute(text("SELECT current_setting('server_version')"))
        row = result.one()
        raw_connection = await connection.get_raw_connection()
        driver_connection = cast(AsyncConnection[Any], raw_connection.driver_connection)
        tls = bool(driver_connection.pgconn.ssl_in_use)
    return DatabaseProbe(
        connected=True,
        tls=tls,
        server_version=str(row[0]),
    )
