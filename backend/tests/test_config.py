import pytest
from pydantic import SecretStr, ValidationError

from app.config import Settings


def test_database_url_requires_tls() -> None:
    with pytest.raises(ValidationError, match="sslmode=require"):
        Settings(
            app_env="test",
            database_url=SecretStr(
                "postgresql://wishtrace:password@database.invalid:5432/wishtrace"
            ),
        )


def test_database_url_is_normalized_for_psycopg() -> None:
    settings = Settings(
        app_env="test",
        database_url=SecretStr(
            "postgresql://wishtrace:password@database.invalid:5432/wishtrace?sslmode=require"
        ),
    )

    assert settings.sqlalchemy_database_url.startswith("postgresql+psycopg://")
    assert "sslmode=require" in settings.sqlalchemy_database_url


def test_deployed_public_url_requires_https() -> None:
    with pytest.raises(ValidationError, match="PUBLIC_BASE_URL must use HTTPS"):
        Settings(
            app_env="production",
            database_url=SecretStr(
                "postgresql://wishtrace:password@database.invalid:5432/wishtrace?sslmode=require"
            ),
            public_base_url="http://wishtrace.invalid",
        )
