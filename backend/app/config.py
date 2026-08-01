import re
from functools import lru_cache
from pathlib import Path
from typing import Literal
from urllib.parse import parse_qs, urlsplit

from pydantic import AnyHttpUrl, SecretStr, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

REPOSITORY_ENV_FILE = Path(__file__).resolve().parents[2] / ".env"
AZURE_DEPLOYMENT_PATTERN = re.compile(r"^[A-Za-z0-9._-]{1,200}$")
GOOGLE_CLIENT_ID_PATTERN = re.compile(
    r"^[A-Za-z0-9._-]+\.apps\.googleusercontent\.com$"
)
PRAVA_API_HOSTS = {"sandbox.api.prava.space", "api.prava.space"}


class Settings(BaseSettings):
    """Validated server configuration.

    Secret values remain wrapped in ``SecretStr`` so configuration errors and object
    representations cannot accidentally reveal them.
    """

    model_config = SettingsConfigDict(
        env_file=REPOSITORY_ENV_FILE,
        env_file_encoding="utf-8",
        env_ignore_empty=True,
        extra="ignore",
        case_sensitive=False,
    )

    app_env: Literal["local", "test", "staging", "production"] = "local"
    app_name: str = "WishTrace API"
    app_version: str = "0.1.0"
    database_url: SecretStr
    public_base_url: AnyHttpUrl = AnyHttpUrl("http://127.0.0.1:8000")

    google_web_client_id: SecretStr | None = None
    session_token_pepper: SecretStr | None = None
    azure_openai_base_url: AnyHttpUrl | None = None
    azure_openai_api_key: SecretStr | None = None
    azure_openai_deployment: str | None = None
    prava_base_url: AnyHttpUrl | None = None
    prava_secret_key: SecretStr | None = None
    android_return_uri: str = "wishtrace://prava/return"
    primary_merchant_profile_url: AnyHttpUrl = AnyHttpUrl(
        "https://checkout.jackboxgames.com/.well-known/ucp"
    )
    primary_merchant_endpoint_host: str = "jackbox-games.myshopify.com"
    merchant_checkout_enabled: bool = False
    merchant_browser_executable_path: Path | None = None
    allow_stored_value_products: bool = False

    @field_validator("database_url")
    @classmethod
    def require_secure_postgres(cls, value: SecretStr) -> SecretStr:
        raw = value.get_secret_value()
        parsed = urlsplit(raw)
        if parsed.scheme not in {"postgres", "postgresql", "postgresql+psycopg"}:
            raise ValueError("DATABASE_URL must use PostgreSQL with psycopg")
        ssl_modes = parse_qs(parsed.query).get("sslmode", [])
        if ssl_modes != ["require"]:
            raise ValueError("DATABASE_URL must contain sslmode=require")
        return value

    @model_validator(mode="after")
    def require_https_when_deployed(self) -> "Settings":
        if self.app_env in {"staging", "production"} and self.public_base_url.scheme != "https":
            raise ValueError("PUBLIC_BASE_URL must use HTTPS outside local/test environments")
        return self

    @field_validator("public_base_url")
    @classmethod
    def require_origin_public_base_url(cls, value: AnyHttpUrl) -> AnyHttpUrl:
        parsed = urlsplit(str(value))
        if (
            parsed.path.rstrip("/")
            or parsed.query
            or parsed.fragment
            or parsed.username is not None
            or parsed.password is not None
        ):
            raise ValueError("PUBLIC_BASE_URL must be an origin without path or credentials")
        return value

    @field_validator("google_web_client_id")
    @classmethod
    def validate_google_web_client_id(
        cls,
        value: SecretStr | None,
    ) -> SecretStr | None:
        if value is None:
            return None
        normalized = value.get_secret_value().strip()
        if GOOGLE_CLIENT_ID_PATTERN.fullmatch(normalized) is None:
            raise ValueError("GOOGLE_WEB_CLIENT_ID is invalid")
        return SecretStr(normalized)

    @field_validator("session_token_pepper")
    @classmethod
    def require_strong_session_pepper(cls, value: SecretStr | None) -> SecretStr | None:
        if value is not None and len(value.get_secret_value().encode()) < 32:
            raise ValueError("SESSION_TOKEN_PEPPER must contain at least 32 bytes")
        return value

    @field_validator("azure_openai_deployment")
    @classmethod
    def validate_azure_deployment(cls, value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        if AZURE_DEPLOYMENT_PATTERN.fullmatch(normalized) is None:
            raise ValueError("AZURE_OPENAI_DEPLOYMENT is invalid")
        return normalized

    @model_validator(mode="after")
    def validate_azure_openai_configuration(self) -> "Settings":
        values = (
            self.azure_openai_base_url,
            self.azure_openai_api_key,
            self.azure_openai_deployment,
        )
        if any(value is not None for value in values) and not all(
            value is not None for value in values
        ):
            raise ValueError("Azure OpenAI configuration must be complete")
        if self.azure_openai_base_url is None:
            return self
        parsed = urlsplit(str(self.azure_openai_base_url))
        hostname = (parsed.hostname or "").casefold()
        if (
            parsed.scheme != "https"
            or not hostname.endswith((".openai.azure.com", ".services.ai.azure.com"))
            or parsed.path.rstrip("/") != "/openai/v1"
            or parsed.query
            or parsed.fragment
            or parsed.username is not None
            or parsed.password is not None
            or parsed.port not in (None, 443)
        ):
            raise ValueError("AZURE_OPENAI_BASE_URL must use an approved Azure /openai/v1 endpoint")
        assert self.azure_openai_api_key is not None
        if len(self.azure_openai_api_key.get_secret_value()) < 16:
            raise ValueError("AZURE_OPENAI_API_KEY is invalid")
        return self

    @field_validator("prava_base_url")
    @classmethod
    def validate_prava_base_url(cls, value: AnyHttpUrl | None) -> AnyHttpUrl | None:
        if value is None:
            return None
        parsed = urlsplit(str(value))
        if (
            parsed.scheme != "https"
            or (parsed.hostname or "").casefold() not in PRAVA_API_HOSTS
            or parsed.path.rstrip("/")
            or parsed.query
            or parsed.fragment
            or parsed.username is not None
            or parsed.password is not None
            or parsed.port not in (None, 443)
        ):
            raise ValueError("PRAVA_BASE_URL must use an allowlisted HTTPS API origin")
        return value

    @field_validator("prava_secret_key")
    @classmethod
    def validate_prava_secret_key(
        cls,
        value: SecretStr | None,
    ) -> SecretStr | None:
        if value is not None and len(value.get_secret_value()) < 16:
            raise ValueError("PRAVA_SECRET_KEY is invalid")
        return value

    @model_validator(mode="after")
    def validate_deployment_readiness(self) -> "Settings":
        prava_values = (self.prava_base_url, self.prava_secret_key)
        if any(value is not None for value in prava_values) and not all(
            value is not None for value in prava_values
        ):
            raise ValueError("Prava configuration must be complete")
        if self.app_env in {"staging", "production"} and (
            self.google_web_client_id is None or self.session_token_pepper is None
        ):
            raise ValueError("Deployed authentication configuration must be complete")
        if self.allow_stored_value_products and not self.merchant_checkout_enabled:
            raise ValueError(
                "Stored-value products require the merchant checkout boundary"
            )
        if (
            self.app_env in {"staging", "production"}
            and self.merchant_checkout_enabled
            and (self.prava_base_url is None or self.prava_secret_key is None)
        ):
            raise ValueError("Deployed merchant checkout requires Prava configuration")
        return self

    @field_validator("android_return_uri")
    @classmethod
    def require_wishtrace_return_uri(cls, value: str) -> str:
        parsed = urlsplit(value)
        if (
            parsed.scheme != "wishtrace"
            or parsed.hostname != "prava"
            or parsed.path != "/return"
            or parsed.query
            or parsed.fragment
            or parsed.username is not None
            or parsed.password is not None
        ):
            raise ValueError("ANDROID_RETURN_URI must equal wishtrace://prava/return")
        return value

    @field_validator("merchant_browser_executable_path")
    @classmethod
    def require_browser_executable(cls, value: Path | None) -> Path | None:
        if value is None:
            return None
        resolved = value.expanduser().resolve()
        if not resolved.is_absolute() or not resolved.is_file():
            raise ValueError("MERCHANT_BROWSER_EXECUTABLE_PATH must be an existing file")
        return resolved

    @property
    def sqlalchemy_database_url(self) -> str:
        raw = self.database_url.get_secret_value()
        if raw.startswith("postgresql+psycopg://"):
            return raw
        if raw.startswith("postgresql://"):
            return raw.replace("postgresql://", "postgresql+psycopg://", 1)
        return raw.replace("postgres://", "postgresql+psycopg://", 1)

    @property
    def ucp_agent_profile_url(self) -> str:
        return f"{str(self.public_base_url).rstrip('/')}/.well-known/ucp"


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
