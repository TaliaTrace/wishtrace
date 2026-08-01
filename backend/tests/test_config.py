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


def test_configured_session_pepper_must_be_strong() -> None:
    with pytest.raises(ValidationError, match="at least 32 bytes"):
        Settings(
            app_env="test",
            database_url=SecretStr(
                "postgresql://wishtrace:password@database.invalid:5432/wishtrace?sslmode=require"
            ),
            session_token_pepper=SecretStr("too-short"),
        )


def test_android_return_uri_cannot_be_redirected() -> None:
    with pytest.raises(ValidationError, match="wishtrace://prava/return"):
        Settings(
            app_env="test",
            database_url=SecretStr(
                "postgresql://wishtrace:password@database.invalid:5432/"
                "wishtrace?sslmode=require"
            ),
            android_return_uri="https://attacker.example/return",
        )


def test_azure_openai_configuration_must_be_complete() -> None:
    with pytest.raises(
        ValidationError,
        match="Azure OpenAI configuration must be complete",
    ):
        Settings(
            app_env="test",
            database_url=SecretStr(
                "postgresql://wishtrace:password@database.invalid:5432/"
                "wishtrace?sslmode=require"
            ),
            azure_openai_base_url=None,
            azure_openai_api_key=None,
            azure_openai_deployment="wishtrace-ranking",
        )


def test_azure_openai_endpoint_rejects_non_azure_host() -> None:
    with pytest.raises(ValidationError, match="approved Azure /openai/v1 endpoint"):
        Settings(
            app_env="test",
            database_url=SecretStr(
                "postgresql://wishtrace:password@database.invalid:5432/"
                "wishtrace?sslmode=require"
            ),
            azure_openai_base_url="https://attacker.example/openai/v1/",
            azure_openai_api_key=SecretStr("not-a-real-key-but-long-enough"),
            azure_openai_deployment="wishtrace-ranking",
        )


def test_azure_openai_endpoint_rejects_nested_project_path() -> None:
    with pytest.raises(ValidationError, match="approved Azure /openai/v1 endpoint"):
        Settings(
            app_env="test",
            database_url=SecretStr(
                "postgresql://wishtrace:password@database.invalid:5432/"
                "wishtrace?sslmode=require"
            ),
            azure_openai_base_url=(
                "https://wishtrace.services.ai.azure.com/api/projects/example/openai/v1/"
            ),
            azure_openai_api_key=SecretStr("not-a-real-key-but-long-enough"),
            azure_openai_deployment="wishtrace-ranking",
        )


def test_azure_openai_foundry_endpoint_is_accepted() -> None:
    settings = Settings(
        app_env="test",
        database_url=SecretStr(
            "postgresql://wishtrace:password@database.invalid:5432/"
            "wishtrace?sslmode=require"
        ),
        azure_openai_base_url=(
            "https://wishtrace.services.ai.azure.com/openai/v1/"
        ),
        azure_openai_api_key=SecretStr("not-a-real-key-but-long-enough"),
        azure_openai_deployment="wishtrace-ranking",
    )

    assert settings.azure_openai_deployment == "wishtrace-ranking"


def test_checkout_can_use_playwright_managed_browser() -> None:
    settings = Settings(
        app_env="test",
        database_url=SecretStr(
            "postgresql://wishtrace:password@database.invalid:5432/"
            "wishtrace?sslmode=require"
        ),
        merchant_checkout_enabled=True,
        merchant_browser_executable_path=None,
    )

    assert settings.merchant_checkout_enabled is True
    assert settings.merchant_browser_executable_path is None
