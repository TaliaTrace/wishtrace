from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.config import Settings, get_settings
from app.database import DatabaseProbe, create_database_engine, probe_database
from app.errors import register_error_handlers
from app.observability import configure_logging, register_request_context
from app.system import DatabaseProbeCallable, build_system_router


def create_app(
    settings: Settings | None = None,
    database_probe: DatabaseProbeCallable | None = None,
) -> FastAPI:
    resolved_settings = settings or get_settings()
    engine = None
    resolved_database_probe: DatabaseProbeCallable
    if database_probe is None:
        engine = create_database_engine(resolved_settings)

        async def live_database_probe() -> DatabaseProbe:
            return await probe_database(engine)

        resolved_database_probe = live_database_probe
    else:
        resolved_database_probe = database_probe

    @asynccontextmanager
    async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
        yield
        if engine is not None:
            await engine.dispose()

    configure_logging()
    app = FastAPI(
        title=resolved_settings.app_name,
        version=resolved_settings.app_version,
        lifespan=lifespan,
    )
    register_request_context(app)
    register_error_handlers(app)
    app.include_router(build_system_router(resolved_settings, resolved_database_probe))
    return app
