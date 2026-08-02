from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.auth import AuthOperations, build_auth_service
from app.auth_api import build_auth_router
from app.commerce import UcpMerchantGateway
from app.config import Settings, get_settings
from app.context_api import build_context_router
from app.database import (
    DatabaseProbe,
    create_database_engine,
    create_database_session_factory,
    probe_database,
)
from app.discovery import (
    DiscoveryOperations,
    UnavailableDiscoveryService,
    build_discovery_service,
)
from app.discovery_api import build_discovery_router
from app.errors import register_error_handlers
from app.mandate import (
    MandateOperations,
    UnavailableMandateService,
    build_mandate_service,
)
from app.mandate_api import build_mandate_router
from app.merchant_browser import JackboxPlaywrightCheckoutGateway
from app.message import MessageOperations, SqlMessageStore
from app.message_api import build_message_router
from app.observability import configure_logging, register_request_context
from app.openai_ranking import build_azure_ranking_gateway
from app.prava import PravaHttpGateway
from app.purchase import (
    PurchaseOperations,
    UnavailablePurchaseService,
    build_purchase_service,
)
from app.purchase_api import build_purchase_router
from app.ranking import (
    RankingGateway,
    RankingOperations,
    UnavailableRankingGateway,
    build_ranking_service,
)
from app.ranking_api import build_ranking_router
from app.recipient_context import ContextOperations, SqlContextStore
from app.system import DatabaseProbeCallable, build_system_router


def create_app(
    settings: Settings | None = None,
    database_probe: DatabaseProbeCallable | None = None,
    auth_operations: AuthOperations | None = None,
    context_operations: ContextOperations | None = None,
    discovery_operations: DiscoveryOperations | None = None,
    purchase_operations: PurchaseOperations | None = None,
    mandate_operations: MandateOperations | None = None,
    ranking_operations: RankingOperations | None = None,
    message_operations: MessageOperations | None = None,
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
    prava_checkout_ready = (
        resolved_settings.prava_base_url is not None
        and resolved_settings.prava_secret_key is not None
        and resolved_settings.session_token_pepper is not None
        and resolved_settings.public_base_url.scheme == "https"
    )
    merchant_checkout: JackboxPlaywrightCheckoutGateway | None = None
    if resolved_settings.merchant_checkout_enabled:
        try:
            browser_path = resolved_settings.merchant_browser_executable_path
            merchant_checkout = JackboxPlaywrightCheckoutGateway(
                browser_executable_path=(
                    str(browser_path) if browser_path is not None else None
                )
            )
        except ValueError:
            merchant_checkout = None
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
    resolved_message_operations = message_operations
    if resolved_message_operations is None and session_factory is not None:
        resolved_message_operations = SqlMessageStore(session_factory)
    resolved_discovery_operations = discovery_operations
    if resolved_discovery_operations is None and session_factory is not None:
        try:
            merchant = UcpMerchantGateway(
                merchant_id="jackbox-games-us",
                merchant_name="Jackbox Games",
                business_profile_url=str(resolved_settings.primary_merchant_profile_url),
                allowed_endpoint_host=resolved_settings.primary_merchant_endpoint_host,
                agent_profile_url=resolved_settings.ucp_agent_profile_url,
                checkout_verified=(
                    merchant_checkout is not None and prava_checkout_ready
                ),
            )
            resolved_discovery_operations = build_discovery_service(
                session_factory=session_factory,
                merchant=merchant,
                allow_stored_value_products=resolved_settings.allow_stored_value_products,
            )
        except ValueError:
            resolved_discovery_operations = UnavailableDiscoveryService()
    resolved_purchase_operations = purchase_operations
    if resolved_purchase_operations is None and session_factory is not None:
        if prava_checkout_ready:
            assert resolved_settings.prava_base_url is not None
            assert resolved_settings.prava_secret_key is not None
            assert resolved_settings.session_token_pepper is not None
            try:
                prava = PravaHttpGateway(
                    base_url=str(resolved_settings.prava_base_url),
                    secret_key=resolved_settings.prava_secret_key,
                )
                resolved_purchase_operations = build_purchase_service(
                    session_factory=session_factory,
                    prava=prava,
                    public_base_url=str(resolved_settings.public_base_url),
                    merchant_checkout=merchant_checkout,
                    idempotency_pepper=(
                        resolved_settings.session_token_pepper.get_secret_value()
                    ),
                )
            except ValueError:
                resolved_purchase_operations = UnavailablePurchaseService()
        else:
            resolved_purchase_operations = UnavailablePurchaseService()
    resolved_mandate_operations = mandate_operations
    if resolved_mandate_operations is None and session_factory is not None:
        # The mandate autopilot needs the same live Prava + checkout footing as
        # one-time purchases: charge credentials get spent in a real merchant
        # checkout, so both must be configured before it can arm.
        if prava_checkout_ready and merchant_checkout is not None:
            assert resolved_settings.prava_base_url is not None
            assert resolved_settings.prava_secret_key is not None
            assert resolved_settings.session_token_pepper is not None
            try:
                mandate_prava = PravaHttpGateway(
                    base_url=str(resolved_settings.prava_base_url),
                    secret_key=resolved_settings.prava_secret_key,
                )
                resolved_mandate_operations = build_mandate_service(
                    session_factory=session_factory,
                    prava=mandate_prava,
                    public_base_url=str(resolved_settings.public_base_url),
                    merchant_checkout=merchant_checkout,
                    idempotency_pepper=(
                        resolved_settings.session_token_pepper.get_secret_value()
                    ),
                )
            except ValueError:
                resolved_mandate_operations = UnavailableMandateService()
        else:
            resolved_mandate_operations = UnavailableMandateService()
    resolved_ranking_operations = ranking_operations
    if resolved_ranking_operations is None and session_factory is not None:
        ranking_gateway: RankingGateway = UnavailableRankingGateway()
        if (
            resolved_settings.azure_openai_base_url is not None
            and resolved_settings.azure_openai_api_key is not None
            and resolved_settings.azure_openai_deployment is not None
        ):
            try:
                ranking_gateway = build_azure_ranking_gateway(
                    base_url=str(resolved_settings.azure_openai_base_url),
                    api_key=resolved_settings.azure_openai_api_key,
                    deployment=resolved_settings.azure_openai_deployment,
                )
            except ValueError:
                ranking_gateway = UnavailableRankingGateway()
        resolved_ranking_operations = build_ranking_service(
            session_factory=session_factory,
            gateway=ranking_gateway,
        )

    @asynccontextmanager
    async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
        yield
        if merchant_checkout is not None:
            await merchant_checkout.close()
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
    if resolved_discovery_operations is not None:
        app.state.discovery_operations = resolved_discovery_operations
        app.include_router(build_discovery_router())
    if resolved_purchase_operations is not None:
        app.state.purchase_operations = resolved_purchase_operations
        app.include_router(
            build_purchase_router(resolved_settings.android_return_uri)
        )
    if resolved_mandate_operations is not None:
        app.state.mandate_operations = resolved_mandate_operations
        app.include_router(
            build_mandate_router(resolved_settings.android_return_uri)
        )
    if resolved_ranking_operations is not None:
        app.state.ranking_operations = resolved_ranking_operations
        app.include_router(build_ranking_router())
    if resolved_message_operations is not None:
        app.state.message_operations = resolved_message_operations
        app.include_router(build_message_router())
    app.include_router(build_system_router(resolved_settings, resolved_database_probe))
    return app
