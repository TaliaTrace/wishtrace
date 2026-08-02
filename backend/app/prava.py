import re
from datetime import datetime
from enum import StrEnum
from typing import Any, Literal, cast
from urllib.parse import urlsplit

import httpx
from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    SecretStr,
    ValidationError,
    field_validator,
)

MAX_RESPONSE_BYTES = 1_000_000
PROVIDER_ID_PATTERN = re.compile(r"^[A-Za-z0-9._:-]{1,255}$")
PROVIDER_ID_REGEX = r"^[A-Za-z0-9._:-]{1,255}$"


class PravaPaymentStatus(StrEnum):
    PENDING = "pending"
    AWAITING_RESULT = "awaiting_result"
    COMPLETED = "completed"
    FAILED = "failed"


class PravaReportOutcome(StrEnum):
    APPROVED = "APPROVED"
    DECLINED = "DECLINED"


class PravaMandateFrequency(StrEnum):
    ONE_TIME = "one_time"
    WEEKLY = "weekly"
    MONTHLY = "monthly"
    YEARLY = "yearly"


class PravaMandateScope(StrEnum):
    LISTED = "listed"
    ANY = "any"


class PravaMandateStatus(StrEnum):
    PENDING = "pending"
    ACTIVE = "active"
    PAUSED = "paused"
    CONSUMED = "consumed"
    CANCELLED = "cancelled"
    EXPIRED = "expired"


class PravaGatewayError(Exception):
    def __init__(
        self,
        code: str,
        message: str,
        *,
        recoverable: bool,
        outcome_unknown: bool = False,
        response_id: str | None = None,
        provider_code: str | None = None,
    ) -> None:
        super().__init__(message)
        self.code = code
        self.safe_message = message
        self.recoverable = recoverable
        self.outcome_unknown = outcome_unknown
        self.response_id = response_id
        self.provider_code = provider_code


class PravaSessionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, str_strip_whitespace=True)

    user_id: str = Field(min_length=1, max_length=255)
    user_email: str = Field(min_length=3, max_length=320)
    total_minor: int = Field(gt=0)
    currency: Literal["USD"]
    merchant_name: str = Field(min_length=1, max_length=200)
    merchant_url: str
    merchant_country: Literal["US"]
    product_description: str = Field(min_length=1, max_length=500)
    product_unit_minor: int = Field(gt=0)
    external_product_id: str = Field(min_length=1, max_length=50)
    quantity: int = Field(default=1, gt=0, le=100)
    callback_url: str
    external_order_ref: str = Field(min_length=1, max_length=255)

    @field_validator("user_email")
    @classmethod
    def validate_email_shape(cls, value: str) -> str:
        local, separator, domain = value.rpartition("@")
        if separator != "@" or not local or "." not in domain:
            raise ValueError("user_email must be a valid email address")
        return value

    @field_validator("merchant_url", "callback_url")
    @classmethod
    def require_https(cls, value: str) -> str:
        if not _is_https_url(value):
            raise ValueError("URL must use HTTPS without embedded credentials or fragments")
        return value

    def provider_payload(self) -> dict[str, object]:
        return {
            "user_id": self.user_id,
            "user_email": self.user_email,
            "total_amount": _minor_to_decimal(self.total_minor),
            "currency": self.currency,
            "integration_type": "full_checkout",
            "callback_url": self.callback_url,
            "external_order_ref": self.external_order_ref,
            "purchase_context": [
                {
                    "merchant_details": {
                        "name": self.merchant_name,
                        "url": self.merchant_url,
                        "country_code_iso2": self.merchant_country,
                    },
                    "product_details": [
                        {
                            "description": self.product_description,
                            "unit_price": _minor_to_decimal(self.product_unit_minor),
                            "product_id": self.external_product_id,
                            "quantity": self.quantity,
                        }
                    ],
                }
            ],
        }


class PravaMandateSessionRequest(BaseModel):
    """Session creation for mandate setup — authorize_only, no credentials issued."""

    model_config = ConfigDict(extra="forbid", frozen=True, str_strip_whitespace=True)

    user_id: str = Field(min_length=1, max_length=255)
    user_email: str = Field(min_length=3, max_length=320)
    total_minor: int = Field(gt=0)
    currency: Literal["USD"]
    merchant_name: str = Field(min_length=1, max_length=200)
    merchant_url: str
    merchant_country: Literal["US"]
    product_description: str = Field(min_length=1, max_length=500)
    product_unit_minor: int = Field(gt=0)
    external_product_id: str = Field(min_length=1, max_length=50)
    quantity: int = Field(default=1, gt=0, le=100)
    callback_url: str
    external_order_ref: str = Field(min_length=1, max_length=255)

    recurring_frequency: PravaMandateFrequency
    merchant_scope: PravaMandateScope = PravaMandateScope.LISTED
    max_charges: int = Field(default=1, ge=1, le=365)
    valid_until: str = Field(min_length=10, max_length=25)

    @field_validator("user_email")
    @classmethod
    def validate_email_shape(cls, value: str) -> str:
        local, separator, domain = value.rpartition("@")
        if separator != "@" or not local or "." not in domain:
            raise ValueError("user_email must be a valid email address")
        return value

    @field_validator("merchant_url", "callback_url")
    @classmethod
    def require_https(cls, value: str) -> str:
        if not _is_https_url(value):
            raise ValueError("URL must use HTTPS without embedded credentials or fragments")
        return value

    def provider_payload(self) -> dict[str, object]:
        return {
            "user_id": self.user_id,
            "user_email": self.user_email,
            "total_amount": _minor_to_decimal(self.total_minor),
            "currency": self.currency,
            "intent": "mandate_setup",
            "authorize_only": True,
            "callback_url": self.callback_url,
            "external_order_ref": self.external_order_ref,
            "purchase_context": [
                {
                    "merchant_details": {
                        "name": self.merchant_name,
                        "url": self.merchant_url,
                        "country_code_iso2": self.merchant_country,
                    },
                    "product_details": [
                        {
                            "description": self.product_description,
                            "unit_price": _minor_to_decimal(self.product_unit_minor),
                            "product_id": self.external_product_id,
                            "quantity": self.quantity,
                        }
                    ],
                }
            ],
            "mandate_setup": {
                "recurring_frequency": self.recurring_frequency.value,
                "merchant_scope": self.merchant_scope.value,
                "max_charges": self.max_charges,
                "valid_until": self.valid_until,
            },
        }


class HostedPravaSession(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    session_id: str
    hosted_url: str
    order_id: str
    expires_at: datetime
    response_id: str | None
    # Populated only for mandate setup: Prava mints the mandate record (in
    # ``pending``) at setup time and returns its id so it can be charged later.
    mandate_id: str | None = None


class SensitivePaymentCredential(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    token: SecretStr
    dynamic_cvv: SecretStr
    expiry_month: SecretStr
    expiry_year: SecretStr


class PravaLineItemResult(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    txn_ref_id: str
    merchant_name: str | None
    merchant_url: str | None
    total_minor: int
    status: PravaPaymentStatus
    credential: SensitivePaymentCredential | None


class PravaTransactionResult(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    txn_id: str
    status: PravaPaymentStatus
    line_items: list[PravaLineItemResult]
    error_code: str | None


class PravaPaymentResult(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    session_id: str
    order_id: str | None
    status: PravaPaymentStatus
    transactions: list[PravaTransactionResult]
    response_id: str | None

    @property
    def credentials(self) -> list[tuple[str, SensitivePaymentCredential]]:
        return [
            (line_item.txn_ref_id, line_item.credential)
            for transaction in self.transactions
            for line_item in transaction.line_items
            if line_item.credential is not None
        ]


class PravaReportResult(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    status: Literal["confirmed"]
    txn_ref_id: str
    txn_status: PravaReportOutcome
    visa_confirmation: Literal["SUCCESS", "FAILURE"]
    response_id: str | None


class PravaMandateInfo(BaseModel):
    """A live mandate from Prava's GET /v1/mandates response."""

    model_config = ConfigDict(extra="forbid", frozen=True)

    mandate_id: str
    status: PravaMandateStatus
    recurring_frequency: PravaMandateFrequency
    merchant_scope: PravaMandateScope
    approved_amount: str
    currency: str
    created_at: datetime
    valid_until: datetime
    total_charges: int = 0
    remaining_charges: int = 0


class PravaMandateChargeResult(BaseModel):
    """Result of POST /v1/mandates/{id}/charge — one-time credentials minted."""

    model_config = ConfigDict(extra="forbid", frozen=True)

    mandate_id: str
    charge_id: str
    status: Literal["completed", "failed"]
    credential: SensitivePaymentCredential | None
    txn_ref_id: str | None
    error_code: str | None


class PravaMandateReportResult(BaseModel):
    """Result of POST /v1/mandates/{id}/charges/{txnId}/report."""

    model_config = ConfigDict(extra="forbid", frozen=True)

    status: Literal["confirmed"]
    charge_id: str
    txn_ref_id: str
    txn_status: PravaReportOutcome
    visa_confirmation: Literal["SUCCESS", "FAILURE"]
    response_id: str | None


class _RawSession(BaseModel):
    model_config = ConfigDict(extra="ignore")

    session_id: str = Field(pattern=PROVIDER_ID_REGEX)
    session_token: SecretStr
    iframe_url: str
    order_id: str = Field(pattern=PROVIDER_ID_REGEX)
    expires_at: datetime
    mandate_id: str | None = Field(default=None, pattern=PROVIDER_ID_REGEX)

    @field_validator("expires_at")
    @classmethod
    def require_aware_expiry(cls, value: datetime) -> datetime:
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("expires_at must include a timezone")
        return value


class _RawProduct(BaseModel):
    model_config = ConfigDict(extra="ignore")

    product_ref_id: str
    external_product_id: str | None = None
    name: str
    unit_price: str
    quantity: int


class _RawLineItem(BaseModel):
    model_config = ConfigDict(extra="ignore")

    txn_ref_id: str = Field(pattern=PROVIDER_ID_REGEX)
    merchant_name: str | None = None
    merchant_url: str | None = None
    total_amount: str
    status: PravaPaymentStatus
    token: SecretStr | None = None
    dynamic_cvv: SecretStr | None = None
    expiry_month: SecretStr | None = None
    expiry_year: SecretStr | None = None
    products: list[_RawProduct] = Field(default_factory=list)


class _RawTransactionError(BaseModel):
    model_config = ConfigDict(extra="ignore")

    code: str
    message: str | None = None


class _RawTransaction(BaseModel):
    model_config = ConfigDict(extra="ignore")

    txn_id: str = Field(pattern=PROVIDER_ID_REGEX)
    status: PravaPaymentStatus
    line_items: list[_RawLineItem] = Field(default_factory=list)
    error: _RawTransactionError | None = None


class _RawPaymentResult(BaseModel):
    model_config = ConfigDict(extra="ignore")

    session_id: str = Field(pattern=PROVIDER_ID_REGEX)
    order_id: str | None = Field(default=None, pattern=PROVIDER_ID_REGEX)
    status: PravaPaymentStatus
    transactions: list[_RawTransaction] = Field(default_factory=list)


class _RawReportResult(BaseModel):
    model_config = ConfigDict(extra="ignore")

    status: Literal["confirmed"]
    txn_ref_id: str = Field(pattern=PROVIDER_ID_REGEX)
    txn_status: PravaReportOutcome
    visa_confirmation: Literal["SUCCESS", "FAILURE"]


class _RawMandate(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: str = Field(pattern=PROVIDER_ID_REGEX)
    status: PravaMandateStatus
    recurring_frequency: PravaMandateFrequency
    merchant_scope: PravaMandateScope
    approved_amount: str
    currency: str
    created_at: datetime
    valid_until: datetime
    total_charges: int = 0
    remaining_charges: int = 0


class _RawChargeResult(BaseModel):
    model_config = ConfigDict(extra="ignore")

    mandate_id: str = Field(pattern=PROVIDER_ID_REGEX)
    charge_id: str = Field(pattern=PROVIDER_ID_REGEX)
    txn_ref_id: str | None = Field(default=None, pattern=PROVIDER_ID_REGEX)
    status: Literal["completed", "failed"]
    token: SecretStr | None = None
    dynamic_cvv: SecretStr | None = None
    expiry_month: SecretStr | None = None
    expiry_year: SecretStr | None = None
    error: _RawTransactionError | None = None


class _RawMandateReport(BaseModel):
    model_config = ConfigDict(extra="ignore")

    status: Literal["confirmed"]
    charge_id: str = Field(pattern=PROVIDER_ID_REGEX)
    txn_ref_id: str = Field(pattern=PROVIDER_ID_REGEX)
    txn_status: PravaReportOutcome
    visa_confirmation: Literal["SUCCESS", "FAILURE"]


class PravaHttpGateway:
    def __init__(
        self,
        *,
        base_url: str,
        secret_key: SecretStr,
        transport: httpx.AsyncBaseTransport | None = None,
        allowed_hosted_hosts: set[str] | None = None,
    ) -> None:
        parsed_base = urlsplit(base_url)
        if not _is_https_url(base_url) or parsed_base.hostname is None:
            raise ValueError("Prava base URL must use HTTPS.")
        self._base_url = base_url.rstrip("/")
        self._secret_key = secret_key
        self._transport = transport
        self._allowed_hosted_hosts = allowed_hosted_hosts or _hosted_hosts(
            parsed_base.hostname.casefold()
        )

    async def create_session(self, request: PravaSessionRequest) -> HostedPravaSession:
        response = await self._request(
            "POST",
            "/v1/sessions",
            outcome_unknown_on_network_failure=True,
            ambiguous_write=True,
            json=request.provider_payload(),
        )
        try:
            raw = _RawSession.model_validate(response.json())
        except (ValueError, ValidationError) as error:
            raise _invalid_response(response, outcome_unknown=True) from error
        if not _allowed_https_url(raw.iframe_url, self._allowed_hosted_hosts):
            raise PravaGatewayError(
                "PRAVA_OUTCOME_UNKNOWN",
                "Prava created an approval response that WishTrace cannot open safely.",
                recoverable=True,
                outcome_unknown=True,
                response_id=_response_id(response),
            )
        return HostedPravaSession(
            session_id=raw.session_id,
            hosted_url=raw.iframe_url,
            order_id=raw.order_id,
            expires_at=raw.expires_at,
            response_id=_response_id(response),
        )

    async def get_payment_result(self, session_id: str) -> PravaPaymentResult:
        _require_provider_id(session_id, "session_id")
        response = await self._request(
            "GET",
            f"/v1/sessions/{session_id}/payment-result",
        )
        try:
            raw = _RawPaymentResult.model_validate(response.json())
            transactions = [
                PravaTransactionResult(
                    txn_id=transaction.txn_id,
                    status=transaction.status,
                    line_items=[_line_item(item) for item in transaction.line_items],
                    error_code=(transaction.error.code if transaction.error is not None else None),
                )
                for transaction in raw.transactions
            ]
        except (ValueError, ValidationError) as error:
            raise _invalid_response(response) from error
        return PravaPaymentResult(
            session_id=raw.session_id,
            order_id=raw.order_id,
            status=raw.status,
            transactions=transactions,
            response_id=_response_id(response),
        )

    async def report_status(
        self,
        *,
        session_id: str,
        txn_ref_id: str,
        outcome: PravaReportOutcome,
    ) -> PravaReportResult:
        _require_provider_id(session_id, "session_id")
        _require_provider_id(txn_ref_id, "txn_ref_id")
        response = await self._request(
            "POST",
            f"/v1/sessions/{session_id}/report-status",
            outcome_unknown_on_network_failure=True,
            ambiguous_write=True,
            json={
                "txn_ref_id": txn_ref_id,
                "txn_status": outcome.value,
                "txn_type": "PURCHASE",
            },
        )
        try:
            raw = _RawReportResult.model_validate(response.json())
        except (ValueError, ValidationError) as error:
            raise _invalid_response(response) from error
        return PravaReportResult(
            status=raw.status,
            txn_ref_id=raw.txn_ref_id,
            txn_status=raw.txn_status,
            visa_confirmation=raw.visa_confirmation,
            response_id=_response_id(response),
        )

    async def create_mandate_session(
        self, request: PravaMandateSessionRequest
    ) -> HostedPravaSession:
        response = await self._request(
            "POST",
            "/v1/sessions",
            outcome_unknown_on_network_failure=True,
            ambiguous_write=True,
            json=request.provider_payload(),
        )
        try:
            raw = _RawSession.model_validate(response.json())
        except (ValueError, ValidationError) as error:
            raise _invalid_response(response, outcome_unknown=True) from error
        if not _allowed_https_url(raw.iframe_url, self._allowed_hosted_hosts):
            raise PravaGatewayError(
                "PRAVA_OUTCOME_UNKNOWN",
                "Prava created an approval response that WishTrace cannot open safely.",
                recoverable=True,
                outcome_unknown=True,
                response_id=_response_id(response),
            )
        return HostedPravaSession(
            session_id=raw.session_id,
            hosted_url=raw.iframe_url,
            order_id=raw.order_id,
            expires_at=raw.expires_at,
            response_id=_response_id(response),
            mandate_id=raw.mandate_id,
        )

    async def get_mandate(self, mandate_id: str) -> PravaMandateInfo:
        _require_provider_id(mandate_id, "mandate_id")
        response = await self._request(
            "GET",
            f"/v1/mandates/{mandate_id}",
        )
        try:
            raw = _RawMandate.model_validate(response.json())
        except (ValueError, ValidationError) as error:
            raise _invalid_response(response) from error
        return _mandate_info(raw)

    async def charge_mandate(
        self,
        *,
        mandate_id: str,
        amount_minor: int,
        reference: str,
    ) -> PravaMandateChargeResult:
        _require_provider_id(mandate_id, "mandate_id")
        _require_provider_id(reference, "reference")
        if amount_minor <= 0:
            raise ValueError("amount_minor must be positive")
        response = await self._request(
            "POST",
            f"/v1/mandates/{mandate_id}/charge",
            outcome_unknown_on_network_failure=True,
            ambiguous_write=True,
            json={
                "amount": _minor_to_decimal(amount_minor),
                "reference": reference,
            },
        )
        try:
            raw = _RawChargeResult.model_validate(response.json())
            return _charge_result(raw)
        except (ValueError, ValidationError) as error:
            raise _invalid_response(response, outcome_unknown=True) from error

    async def report_mandate_charge(
        self,
        *,
        mandate_id: str,
        charge_id: str,
        outcome: PravaReportOutcome,
    ) -> PravaMandateReportResult:
        _require_provider_id(mandate_id, "mandate_id")
        _require_provider_id(charge_id, "charge_id")
        response = await self._request(
            "POST",
            f"/v1/mandates/{mandate_id}/charges/{charge_id}/report",
            outcome_unknown_on_network_failure=True,
            ambiguous_write=True,
            json={
                "txn_status": outcome.value,
                "txn_type": "PURCHASE",
            },
        )
        try:
            raw = _RawMandateReport.model_validate(response.json())
        except (ValueError, ValidationError) as error:
            raise _invalid_response(response) from error
        return PravaMandateReportResult(
            status=raw.status,
            charge_id=raw.charge_id,
            txn_ref_id=raw.txn_ref_id,
            txn_status=raw.txn_status,
            visa_confirmation=raw.visa_confirmation,
            response_id=_response_id(response),
        )

    async def _request(
        self,
        method: str,
        path: str,
        *,
        outcome_unknown_on_network_failure: bool = False,
        ambiguous_write: bool = False,
        **kwargs: Any,
    ) -> httpx.Response:
        timeout = httpx.Timeout(20.0, connect=10.0)
        try:
            async with httpx.AsyncClient(
                timeout=timeout,
                follow_redirects=False,
                transport=self._transport,
                headers={
                    "Authorization": f"Bearer {self._secret_key.get_secret_value()}",
                    "Accept": "application/json",
                    "User-Agent": "WishTrace/0.1",
                },
            ) as client:
                response = await client.request(method, f"{self._base_url}{path}", **kwargs)
        except (httpx.TimeoutException, httpx.NetworkError) as error:
            raise PravaGatewayError(
                "PRAVA_OUTCOME_UNKNOWN"
                if outcome_unknown_on_network_failure
                else "PRAVA_UNAVAILABLE",
                "Prava did not confirm the operation. Refresh before trying anything again."
                if outcome_unknown_on_network_failure
                else "Prava is temporarily unavailable. Try again.",
                recoverable=True,
                outcome_unknown=outcome_unknown_on_network_failure,
            ) from error
        if 300 <= response.status_code < 400:
            raise PravaGatewayError(
                "PRAVA_OUTCOME_UNKNOWN" if ambiguous_write else "PRAVA_REDIRECT_REJECTED",
                "Prava did not confirm the operation. Refresh before trying anything again."
                if ambiguous_write
                else "Prava returned an unexpected redirect.",
                recoverable=ambiguous_write,
                outcome_unknown=ambiguous_write,
                response_id=_response_id(response),
            )
        if len(response.content) > MAX_RESPONSE_BYTES:
            raise PravaGatewayError(
                "PRAVA_OUTCOME_UNKNOWN" if ambiguous_write else "PRAVA_RESPONSE_TOO_LARGE",
                "Prava did not confirm the operation. Refresh before trying anything again."
                if ambiguous_write
                else "Prava returned an invalid response.",
                recoverable=ambiguous_write,
                outcome_unknown=ambiguous_write,
                response_id=_response_id(response),
            )
        if response.status_code >= 400:
            provider_code = _provider_error_code(response)
            outcome_unknown = ambiguous_write and (
                response.status_code >= 500 or response.status_code == 408
            )
            raise PravaGatewayError(
                "PRAVA_OUTCOME_UNKNOWN"
                if outcome_unknown
                else (
                    "PRAVA_AUTHENTICATION_FAILED"
                    if response.status_code == 401
                    else "PRAVA_REQUEST_FAILED"
                ),
                "Prava did not confirm the operation. Refresh before trying anything again."
                if outcome_unknown
                else (
                    "WishTrace could not authenticate with Prava."
                    if response.status_code == 401
                    else "Prava could not complete that request."
                ),
                recoverable=(
                    outcome_unknown
                    or response.status_code >= 500
                    or response.status_code == 429
                ),
                outcome_unknown=outcome_unknown,
                response_id=_response_id(response),
                provider_code=provider_code,
            )
        content_type = response.headers.get("content-type", "").casefold()
        if "application/json" not in content_type:
            raise _invalid_response(response, outcome_unknown=ambiguous_write)
        return response


def _line_item(raw: _RawLineItem) -> PravaLineItemResult:
    credentials = (raw.token, raw.dynamic_cvv, raw.expiry_month, raw.expiry_year)
    supplied = [value is not None for value in credentials]
    if any(supplied) and not all(supplied):
        raise ValueError("Prava returned incomplete payment credentials")
    credential: SensitivePaymentCredential | None = None
    if all(supplied):
        assert raw.token is not None
        assert raw.dynamic_cvv is not None
        assert raw.expiry_month is not None
        assert raw.expiry_year is not None
        credential = SensitivePaymentCredential(
            token=raw.token,
            dynamic_cvv=raw.dynamic_cvv,
            expiry_month=raw.expiry_month,
            expiry_year=raw.expiry_year,
        )
    if credential is not None and raw.status is not PravaPaymentStatus.AWAITING_RESULT:
        raise ValueError("Prava returned credentials outside awaiting_result")
    merchant_url = raw.merchant_url
    if merchant_url is not None and not _is_https_url(merchant_url):
        raise ValueError("Prava returned an invalid merchant URL")
    return PravaLineItemResult(
        txn_ref_id=raw.txn_ref_id,
        merchant_name=raw.merchant_name,
        merchant_url=merchant_url,
        total_minor=_decimal_to_minor(raw.total_amount),
        status=raw.status,
        credential=credential,
    )


def _mandate_info(raw: _RawMandate) -> PravaMandateInfo:
    return PravaMandateInfo(
        mandate_id=raw.id,
        status=raw.status,
        recurring_frequency=raw.recurring_frequency,
        merchant_scope=raw.merchant_scope,
        approved_amount=raw.approved_amount,
        currency=raw.currency,
        created_at=raw.created_at,
        valid_until=raw.valid_until,
        total_charges=raw.total_charges,
        remaining_charges=raw.remaining_charges,
    )


def _charge_result(raw: _RawChargeResult) -> PravaMandateChargeResult:
    credentials = (raw.token, raw.dynamic_cvv, raw.expiry_month, raw.expiry_year)
    supplied = [value is not None for value in credentials]
    if any(supplied) and not all(supplied):
        raise ValueError("Prava returned incomplete mandate charge credentials")
    credential: SensitivePaymentCredential | None = None
    if all(supplied):
        assert raw.token is not None
        assert raw.dynamic_cvv is not None
        assert raw.expiry_month is not None
        assert raw.expiry_year is not None
        credential = SensitivePaymentCredential(
            token=raw.token,
            dynamic_cvv=raw.dynamic_cvv,
            expiry_month=raw.expiry_month,
            expiry_year=raw.expiry_year,
        )
    if credential is not None and raw.status != "completed":
        raise ValueError("Prava returned mandate credentials on a non-completed charge")
    if raw.status == "completed" and credential is None:
        raise ValueError("Prava completed a mandate charge without credentials")
    return PravaMandateChargeResult(
        mandate_id=raw.mandate_id,
        charge_id=raw.charge_id,
        status=raw.status,
        credential=credential,
        txn_ref_id=raw.txn_ref_id,
        error_code=(raw.error.code if raw.error is not None else None),
    )


def _provider_error_code(response: httpx.Response) -> str:
    try:
        body = response.json()
        if not isinstance(body, dict):
            return "UNKNOWN"
        error = body.get("error")
        if not isinstance(error, dict):
            return "UNKNOWN"
        code = error.get("code")
        return (
            code
            if isinstance(code, str)
            and len(code) <= 100
            and PROVIDER_ID_PATTERN.fullmatch(code)
            else "UNKNOWN"
        )
    except ValueError:
        return "UNKNOWN"


def _response_id(response: httpx.Response) -> str | None:
    value = cast(str | None, response.headers.get("x-response-id"))
    return value if value is not None and PROVIDER_ID_PATTERN.fullmatch(value) else None


def _invalid_response(
    response: httpx.Response,
    *,
    outcome_unknown: bool = False,
) -> PravaGatewayError:
    return PravaGatewayError(
        "PRAVA_OUTCOME_UNKNOWN" if outcome_unknown else "PRAVA_RESPONSE_INVALID",
        "Prava did not confirm the operation. Refresh before trying anything again."
        if outcome_unknown
        else "Prava returned an invalid response.",
        recoverable=outcome_unknown,
        outcome_unknown=outcome_unknown,
        response_id=_response_id(response),
    )


def _minor_to_decimal(amount: int) -> str:
    if amount < 0:
        raise ValueError("minor-unit amount cannot be negative")
    return f"{amount // 100}.{amount % 100:02d}"


def _decimal_to_minor(value: str) -> int:
    whole, separator, fractional = value.partition(".")
    if (
        not separator
        or not whole.isdigit()
        or not fractional.isdigit()
        or len(fractional) != 2
    ):
        raise ValueError("amount must contain exactly two decimal places")
    return int(whole) * 100 + int(fractional)


def _is_https_url(value: str) -> bool:
    parsed = urlsplit(value)
    return (
        parsed.scheme == "https"
        and parsed.hostname is not None
        and parsed.username is None
        and parsed.password is None
        and not parsed.fragment
    )


def _allowed_https_url(value: str, allowed_hosts: set[str]) -> bool:
    parsed = urlsplit(value)
    return (
        _is_https_url(value)
        and parsed.hostname is not None
        and parsed.hostname.casefold() in allowed_hosts
        and parsed.port in (None, 443)
    )


def _hosted_hosts(api_host: str) -> set[str]:
    if api_host == "sandbox.api.prava.space":
        return {"sandbox.collect.prava.space"}
    if api_host == "api.prava.space":
        return {"collect.prava.space"}
    raise ValueError("Prava API host is not allowlisted.")


def _require_provider_id(value: str, field: str) -> None:
    if PROVIDER_ID_PATTERN.fullmatch(value) is None:
        raise ValueError(f"{field} is invalid")
