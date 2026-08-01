from collections.abc import Mapping
from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse


class ApiError(Exception):
    def __init__(
        self,
        *,
        status_code: int,
        code: str,
        message: str,
        recoverable: bool,
        field_errors: Mapping[str, str] | None = None,
    ) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.code = code
        self.message = message
        self.recoverable = recoverable
        self.field_errors = dict(field_errors or {})


def _correlation_id(request: Request) -> str:
    return str(getattr(request.state, "correlation_id", "unavailable"))


def _body(
    request: Request,
    *,
    code: str,
    message: str,
    recoverable: bool,
    field_errors: Mapping[str, str] | None = None,
) -> dict[str, Any]:
    return {
        "code": code,
        "message": message,
        "correlation_id": _correlation_id(request),
        "recoverable": recoverable,
        "field_errors": dict(field_errors or {}),
    }


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(ApiError)
    async def handle_api_error(request: Request, error: ApiError) -> JSONResponse:
        return JSONResponse(
            status_code=error.status_code,
            content=_body(
                request,
                code=error.code,
                message=error.message,
                recoverable=error.recoverable,
                field_errors=error.field_errors,
            ),
        )

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(
        request: Request,
        error: RequestValidationError,
    ) -> JSONResponse:
        field_errors = {
            ".".join(str(part) for part in item["loc"]): str(item["msg"])
            for item in error.errors()
        }
        return JSONResponse(
            status_code=422,
            content=_body(
                request,
                code="VALIDATION_ERROR",
                message="Some request fields are invalid.",
                recoverable=True,
                field_errors=field_errors,
            ),
        )

    @app.exception_handler(Exception)
    async def handle_unexpected_error(request: Request, _error: Exception) -> JSONResponse:
        return JSONResponse(
            status_code=500,
            content=_body(
                request,
                code="INTERNAL_ERROR",
                message="WishTrace could not complete that request.",
                recoverable=True,
            ),
        )
