from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.auth import AuthOperations, build_auth_service
from app.auth_api import build_auth_router
from app.config import Settings, get_settings
from app.context_api import build_context_router
from app.database import (
    DatabaseProbe,
    create_database_engine,
    create_database_session_factory,
    probe_database,
)
from app.errors import register_error_handlers
from app.observability import configure_logging, register_request_context
from app.recipient_context import ContextOperations, SqlContextStore
from app.system import DatabaseProbeCallable, build_system_router


def create_app(
    settings: Settings | None = None,
    database_probe: DatabaseProbeCallable | None = None,
    auth_operations: AuthOperations | None = None,
    context_operations: ContextOperations | None = None,
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
    session_factory = create_database_session_factory(engine) if engine is not None else None
    resolved_auth_operations = auth_operations
    if resolved_auth_operations is None and session_factory is not None:
        resolved_auth_operations = build_auth_service(
            session_factory=session_factory,
            google_audience=(
                resolved_settings.google_web_client_id.get_secret_value()
                if resolved_settings.google_web_client_id is not None
                else None
            ),
            session_token_pepper=(
                resolved_settings.session_token_pepper.get_secret_value()
                if resolved_settings.session_token_pepper is not None
                else None
            ),
        )
    resolved_context_operations = context_operations
    if resolved_context_operations is None and session_factory is not None:
        resolved_context_operations = SqlContextStore(session_factory)

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
    if resolved_auth_operations is not None:
        app.state.auth_operations = resolved_auth_operations
        app.include_router(build_auth_router())
    if resolved_context_operations is not None:
        app.state.context_operations = resolved_context_operations
        app.include_router(build_context_router())
    app.include_router(build_system_router(resolved_settings, resolved_database_probe))
    return app
