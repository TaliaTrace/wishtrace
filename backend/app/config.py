from functools import lru_cache
from pathlib import Path
from typing import Literal
from urllib.parse import parse_qs, urlsplit

from pydantic import AnyHttpUrl, SecretStr, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

REPOSITORY_ENV_FILE = Path(__file__).resolve().parents[2] / ".env"


class Settings(BaseSettings):
    """Validated server configuration.

    Secret values remain wrapped in ``SecretStr`` so configuration errors and object
    representations cannot accidentally reveal them.
    """

    model_config = SettingsConfigDict(
        env_file=REPOSITORY_ENV_FILE,
        env_file_encoding="utf-8",
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

    @property
    def sqlalchemy_database_url(self) -> str:
        raw = self.database_url.get_secret_value()
        if raw.startswith("postgresql+psycopg://"):
            return raw
        if raw.startswith("postgresql://"):
            return raw.replace("postgresql://", "postgresql+psycopg://", 1)
        return raw.replace("postgres://", "postgresql+psycopg://", 1)


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
