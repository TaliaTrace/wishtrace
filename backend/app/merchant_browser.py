import asyncio
import os
import re
import uuid
from collections.abc import Coroutine
from concurrent.futures import Future
from contextlib import suppress
from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from decimal import Decimal, InvalidOperation
from enum import StrEnum
from pathlib import Path
from threading import Event, Thread
from typing import Any, Literal, Protocol, TypeVar, cast
from urllib.parse import urlsplit

from playwright.async_api import (
    APIResponse,
    Browser,
    BrowserContext,
    Page,
    Playwright,
    async_playwright,
)
from playwright.async_api import TimeoutError as PlaywrightTimeoutError
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.prava import SensitivePaymentCredential

JACKBOX_CHECKOUT_HOST = "checkout.jackboxgames.com"
JACKBOX_PRODUCT_PATH = "/products/jackbox-games-gift-card-5"
JACKBOX_VARIANT_GID = "gid://shopify/ProductVariant/39783705149574"
JACKBOX_CART_URL = f"https://{JACKBOX_CHECKOUT_HOST}/cart/add.js"
JACKBOX_CART_STATE_URL = f"https://{JACKBOX_CHECKOUT_HOST}/cart.js"
JACKBOX_CHECKOUT_URL = f"https://{JACKBOX_CHECKOUT_HOST}/checkout"
SHOPIFY_CARD_HOST = "checkout.pci.shopifyinc.com"
VARIANT_ID_PATTERN = re.compile(r"^gid://shopify/ProductVariant/([0-9]{8,24})$")
ORDER_ID_PATTERNS = (
    re.compile(r"\bOrder\s+#([A-Za-z0-9-]{3,64})\b", re.IGNORECASE),
    re.compile(
        r"\border\s+(?:number|confirmation)\s+"
        r"(?:is\s+)?#?([A-Za-z0-9-]{3,64})\b",
        re.IGNORECASE,
    ),
)
EXPLICIT_DECLINE_PATTERNS = (
    "card was declined",
    "payment could not be processed",
    "there was an issue processing your payment",
    "your payment was declined",
    "transaction was declined",
)
SAFE_TEXT_PATTERN = re.compile(r"^[^\x00-\x1f\x7f]{1,200}$")
PHONE_PATTERN = re.compile(r"^[0-9+(). -]{7,32}$")
POSTAL_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9 -]{1,19}$")
COUNTRY_PATTERN = re.compile(r"^[A-Z]{2}$")
EMAIL_PATTERN = re.compile(r"^[^@\s]{1,64}@[^@\s]{1,190}\.[^@\s]{2,63}$")
MONEY_PATTERN = re.compile(r"(?:USD\s*)?\$([0-9][0-9,]*\.[0-9]{2})")
QUOTE_TTL = timedelta(minutes=20)
MAX_CART_RESPONSE_BYTES = 1_000_000
T = TypeVar("T")


class MerchantCheckoutOutcome(StrEnum):
    ORDER_VERIFIED = "ORDER_VERIFIED"
    DECLINED = "DECLINED"
    UNKNOWN = "UNKNOWN"


class MerchantBrowserError(Exception):
    def __init__(
        self,
        code: str,
        message: str,
        *,
        recoverable: bool,
        outcome_unknown: bool = False,
    ) -> None:
        super().__init__(message)
        self.code = code
        self.safe_message = message
        self.recoverable = recoverable
        self.outcome_unknown = outcome_unknown


class BillingContact(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, str_strip_whitespace=True)

    email: str = Field(min_length=3, max_length=320, repr=False)
    first_name: str = Field(min_length=1, max_length=100, repr=False)
    last_name: str = Field(min_length=1, max_length=100, repr=False)
    address_line1: str = Field(min_length=1, max_length=200, repr=False)
    address_line2: str | None = Field(default=None, max_length=200, repr=False)
    city: str = Field(min_length=1, max_length=100, repr=False)
    region: str | None = Field(default=None, max_length=100, repr=False)
    postal_code: str = Field(min_length=2, max_length=20, repr=False)
    country_code: str = Field(min_length=2, max_length=2, repr=False)
    phone: str | None = Field(default=None, max_length=32, repr=False)

    @field_validator(
        "first_name",
        "last_name",
        "address_line1",
        "address_line2",
        "city",
        "region",
    )
    @classmethod
    def reject_control_characters(cls, value: str | None) -> str | None:
        if value is not None and SAFE_TEXT_PATTERN.fullmatch(value) is None:
            raise ValueError("billing text contains unsupported characters")
        return value

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        if EMAIL_PATTERN.fullmatch(value) is None:
            raise ValueError("email is invalid")
        return value

    @field_validator("country_code")
    @classmethod
    def validate_country(cls, value: str) -> str:
        normalized = value.upper()
        if COUNTRY_PATTERN.fullmatch(normalized) is None:
            raise ValueError("country_code must be a two-letter ISO code")
        return normalized

    @field_validator("postal_code")
    @classmethod
    def validate_postal_code(cls, value: str) -> str:
        if POSTAL_PATTERN.fullmatch(value) is None:
            raise ValueError("postal_code contains unsupported characters")
        return value

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, value: str | None) -> str | None:
        if value is not None and PHONE_PATTERN.fullmatch(value) is None:
            raise ValueError("phone contains unsupported characters")
        return value

    @property
    def cardholder_name(self) -> str:
        return f"{self.first_name} {self.last_name}"


class MerchantQuoteRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, str_strip_whitespace=True)

    purchase_intent_id: uuid.UUID
    product_url: str
    merchant_variant_id: str
    expected_item_minor: int = Field(gt=0)
    currency: Literal["USD"]
    billing: BillingContact = Field(repr=False)

    @field_validator("product_url")
    @classmethod
    def require_jackbox_product_url(cls, value: str) -> str:
        parsed = urlsplit(value)
        if (
            parsed.scheme != "https"
            or (parsed.hostname or "").casefold() != JACKBOX_CHECKOUT_HOST
            or parsed.path.rstrip("/") != JACKBOX_PRODUCT_PATH
            or parsed.username is not None
            or parsed.password is not None
            or parsed.port not in (None, 443)
            or parsed.fragment
        ):
            raise ValueError("product_url must be the allowlisted Jackbox gift card")
        return value

    @field_validator("merchant_variant_id")
    @classmethod
    def require_shopify_variant(cls, value: str) -> str:
        if value != JACKBOX_VARIANT_GID:
            raise ValueError("merchant_variant_id must be the allowlisted $5 variant")
        return value


class MerchantQuote(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    item_minor: int = Field(gt=0)
    shipping_minor: int = Field(ge=0)
    tax_minor: int = Field(ge=0)
    total_minor: int = Field(gt=0)
    currency: Literal["USD"]
    delivery_summary: str = Field(min_length=1, max_length=200)
    source: Literal["JACKBOX_SHOPIFY_BROWSER"] = "JACKBOX_SHOPIFY_BROWSER"
    quoted_at: datetime
    expires_at: datetime

    @model_validator(mode="after")
    def validate_totals_and_time(self) -> "MerchantQuote":
        if self.total_minor != self.item_minor + self.shipping_minor + self.tax_minor:
            raise ValueError("quote components must equal total_minor")
        if (
            self.quoted_at.tzinfo is None
            or self.expires_at.tzinfo is None
            or self.expires_at <= self.quoted_at
        ):
            raise ValueError("quote timestamps must be aware and increasing")
        return self


class MerchantCheckoutResult(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    outcome: MerchantCheckoutOutcome
    order_id: str | None = Field(default=None, max_length=64)
    amount_minor: int = Field(gt=0)
    currency: Literal["USD"]
    reason_code: str = Field(min_length=1, max_length=100)

    @model_validator(mode="after")
    def validate_order_evidence(self) -> "MerchantCheckoutResult":
        if self.outcome is MerchantCheckoutOutcome.ORDER_VERIFIED:
            if self.order_id is None:
                raise ValueError("ORDER_VERIFIED requires a merchant order ID")
        elif self.order_id is not None:
            raise ValueError("Only ORDER_VERIFIED may carry a merchant order ID")
        return self


class MerchantCheckoutGateway(Protocol):
    async def quote(self, request: MerchantQuoteRequest) -> MerchantQuote: ...

    async def checkout(
        self,
        *,
        purchase_intent_id: uuid.UUID,
        credential: SensitivePaymentCredential,
    ) -> MerchantCheckoutResult: ...

    async def is_quote_active(self, purchase_intent_id: uuid.UUID) -> bool: ...

    async def close(self) -> None: ...


@dataclass(slots=True)
class _ActiveCheckout:
    playwright: Playwright
    browser: Browser
    context: BrowserContext
    page: Page
    quote: MerchantQuote
    cardholder_name: str
    attempted: bool = False


class _PlaywrightLoopThread:
    """Run Playwright on a Proactor loop beside psycopg's Windows Selector loop."""

    def __init__(self) -> None:
        self._ready = Event()
        self._loop: asyncio.AbstractEventLoop | None = None
        self._startup_error: BaseException | None = None
        self._thread = Thread(
            target=self._run,
            name="wishtrace-playwright",
            daemon=True,
        )
        self._thread.start()
        if not self._ready.wait(timeout=5):
            raise RuntimeError("Playwright event loop did not start")
        if self._startup_error is not None:
            raise RuntimeError("Playwright event loop failed to start") from self._startup_error

    def submit(self, coroutine: Coroutine[Any, Any, T]) -> Future[T]:
        loop = self._loop
        if loop is None or not loop.is_running():
            coroutine.close()
            raise RuntimeError("Playwright event loop is not running")
        return asyncio.run_coroutine_threadsafe(coroutine, loop)

    def stop(self) -> None:
        loop = self._loop
        if loop is None:
            return
        loop.call_soon_threadsafe(loop.stop)
        self._thread.join(timeout=10)
        if self._thread.is_alive():
            raise RuntimeError("Playwright event loop did not stop")

    def _run(self) -> None:
        try:
            loop_type = asyncio.ProactorEventLoop
            loop = cast(asyncio.AbstractEventLoop, loop_type())
            asyncio.set_event_loop(loop)
            self._loop = loop
        except BaseException as error:
            self._startup_error = error
            self._ready.set()
            return
        self._ready.set()
        try:
            loop.run_forever()
        finally:
            pending = asyncio.all_tasks(loop)
            for task in pending:
                task.cancel()
            if pending:
                loop.run_until_complete(
                    asyncio.gather(*pending, return_exceptions=True)
                )
            loop.close()
            self._loop = None


class JackboxPlaywrightCheckoutGateway:
    """One-cart Jackbox digital-gift checkout actor.

    Billing values and card credentials exist only inside the short-lived browser
    context. They are never returned, logged, screenshotted, or persisted by this class.
    """

    def __init__(self, *, browser_executable_path: str | None = None) -> None:
        if browser_executable_path is not None:
            path = Path(browser_executable_path)
            if not path.is_absolute() or not path.is_file():
                raise ValueError("browser_executable_path must be an existing absolute file")
        self._browser_executable_path = browser_executable_path
        self._active: dict[uuid.UUID, _ActiveCheckout] = {}
        self._lock = asyncio.Lock()
        self._loop_thread = _PlaywrightLoopThread() if os.name == "nt" else None

    async def quote(self, request: MerchantQuoteRequest) -> MerchantQuote:
        return await self._dispatch(self._quote(request))

    async def _quote(self, request: MerchantQuoteRequest) -> MerchantQuote:
        await self._discard(request.purchase_intent_id)
        playwright: Playwright | None = None
        browser: Browser | None = None
        context: BrowserContext | None = None
        try:
            playwright = await async_playwright().start()
            browser = await playwright.chromium.launch(
                executable_path=self._browser_executable_path,
                headless=True,
                args=["--disable-background-networking"],
            )
            context = await browser.new_context(locale="en-US")
            page = await context.new_page()
            await page.goto(
                request.product_url,
                wait_until="domcontentloaded",
                timeout=45_000,
            )
            variant_id = _numeric_variant_id(request.merchant_variant_id)
            add_response = await context.request.post(
                JACKBOX_CART_URL,
                form={"id": variant_id, "quantity": "1"},
                headers={"Accept": "application/json"},
                timeout=30_000,
            )
            if add_response.status != 200:
                raise _quote_unavailable("MERCHANT_CART_REJECTED")
            cart_response = await context.request.get(
                JACKBOX_CART_STATE_URL,
                headers={"Accept": "application/json"},
                timeout=30_000,
            )
            cart = await _validated_cart(
                cart_response,
                variant_id,
                request.expected_item_minor,
            )
            item_minor = cart["total_price"]
            await page.goto(
                JACKBOX_CHECKOUT_URL,
                wait_until="domcontentloaded",
                timeout=60_000,
            )
            _require_checkout_url(page.url)
            await _fill_billing(page, request.billing)
            subtotal_minor, total_minor = await _wait_for_quote(page, item_minor)
            if subtotal_minor != item_minor:
                raise _quote_unavailable("MERCHANT_SUBTOTAL_MISMATCH")
            shipping_minor = 0
            tax_minor = total_minor - subtotal_minor
            if tax_minor < 0:
                raise _quote_unavailable("MERCHANT_TOTAL_INVALID")
            quoted_at = datetime.now(UTC)
            quote = MerchantQuote(
                item_minor=item_minor,
                shipping_minor=shipping_minor,
                tax_minor=tax_minor,
                total_minor=total_minor,
                currency="USD",
                delivery_summary=(
                    "Sent to the checkout contact email for manual forwarding; "
                    "Jackbox shop only, supported regions only, timing not guaranteed"
                ),
                quoted_at=quoted_at,
                expires_at=quoted_at + QUOTE_TTL,
            )
            active = _ActiveCheckout(
                playwright=playwright,
                browser=browser,
                context=context,
                page=page,
                quote=quote,
                cardholder_name=request.billing.cardholder_name,
            )
            async with self._lock:
                self._active[request.purchase_intent_id] = active
            return quote
        except MerchantBrowserError:
            await _close_parts(playwright, browser, context)
            raise
        except PlaywrightTimeoutError as error:
            await _close_parts(playwright, browser, context)
            raise MerchantBrowserError(
                "MERCHANT_QUOTE_TIMEOUT",
                "The merchant did not finish the live quote. Try again.",
                recoverable=True,
            ) from error
        except Exception as error:
            await _close_parts(playwright, browser, context)
            raise MerchantBrowserError(
                "MERCHANT_QUOTE_FAILED",
                "The merchant could not produce a verified total. Try again.",
                recoverable=True,
            ) from error

    async def checkout(
        self,
        *,
        purchase_intent_id: uuid.UUID,
        credential: SensitivePaymentCredential,
    ) -> MerchantCheckoutResult:
        return await self._dispatch(
            self._checkout(
                purchase_intent_id=purchase_intent_id,
                credential=credential,
            )
        )

    async def _checkout(
        self,
        *,
        purchase_intent_id: uuid.UUID,
        credential: SensitivePaymentCredential,
    ) -> MerchantCheckoutResult:
        async with self._lock:
            active = self._active.get(purchase_intent_id)
            if active is None:
                raise MerchantBrowserError(
                    "MERCHANT_QUOTE_EXPIRED",
                    "The live merchant quote expired. Refresh it before paying.",
                    recoverable=True,
                )
            if active.attempted:
                raise MerchantBrowserError(
                    "MERCHANT_CHECKOUT_ALREADY_ATTEMPTED",
                    "The merchant checkout was already attempted. Refresh its status.",
                    recoverable=False,
                    outcome_unknown=True,
                )
            if active.quote.expires_at <= datetime.now(UTC):
                self._active.pop(purchase_intent_id, None)
                expired = active
            else:
                active.attempted = True
                expired = None
        if expired is not None:
            await _close_active(expired)
            raise MerchantBrowserError(
                "MERCHANT_QUOTE_EXPIRED",
                "The live merchant quote expired. Refresh it before paying.",
                recoverable=True,
            )

        outcome = MerchantCheckoutOutcome.UNKNOWN
        order_id: str | None = None
        reason_code = "MERCHANT_CHECKOUT_OUTCOME_UNKNOWN"
        try:
            observed_total = await _extract_total(active.page)
            if observed_total != active.quote.total_minor:
                raise MerchantBrowserError(
                    "MERCHANT_TOTAL_CHANGED",
                    "The merchant total changed. Do not charge; refresh the quote.",
                    recoverable=True,
                )
            await _fill_payment_fields(
                active.page,
                credential,
                active.cardholder_name,
            )
            pay_button = active.page.locator("#checkout-pay-button")
            if await pay_button.count() != 1 or not await pay_button.is_enabled():
                raise MerchantBrowserError(
                    "MERCHANT_PAYMENT_FORM_INVALID",
                    "The merchant payment form is not ready.",
                    recoverable=True,
                )
            # This is the single money-moving action. Never retry it automatically.
            await pay_button.click(timeout=15_000)
            outcome, order_id, reason_code = await _observe_checkout_outcome(active.page)
        except MerchantBrowserError:
            raise
        except PlaywrightTimeoutError as error:
            raise MerchantBrowserError(
                "MERCHANT_CHECKOUT_UNKNOWN",
                "The merchant did not confirm the checkout outcome. Do not retry.",
                recoverable=True,
                outcome_unknown=True,
            ) from error
        except Exception as error:
            raise MerchantBrowserError(
                "MERCHANT_CHECKOUT_UNKNOWN",
                "The merchant checkout outcome is unknown. Do not retry.",
                recoverable=True,
                outcome_unknown=True,
            ) from error
        finally:
            async with self._lock:
                self._active.pop(purchase_intent_id, None)
            await _close_active(active)
        return MerchantCheckoutResult(
            outcome=outcome,
            order_id=order_id,
            amount_minor=active.quote.total_minor,
            currency="USD",
            reason_code=reason_code,
        )

    async def close(self) -> None:
        loop_thread = self._loop_thread
        if loop_thread is None:
            await self._close()
            return
        await self._dispatch(self._close())
        self._loop_thread = None
        loop_thread.stop()

    async def _close(self) -> None:
        async with self._lock:
            active = list(self._active.values())
            self._active.clear()
        for item in active:
            await _close_active(item)

    async def is_quote_active(self, purchase_intent_id: uuid.UUID) -> bool:
        return await self._dispatch(self._is_quote_active(purchase_intent_id))

    async def _is_quote_active(self, purchase_intent_id: uuid.UUID) -> bool:
        async with self._lock:
            active = self._active.get(purchase_intent_id)
            if active is None:
                return False
            if active.quote.expires_at > datetime.now(UTC):
                return True
            self._active.pop(purchase_intent_id, None)
        await _close_active(active)
        return False

    async def _discard(self, purchase_intent_id: uuid.UUID) -> None:
        async with self._lock:
            active = self._active.pop(purchase_intent_id, None)
        if active is not None:
            await _close_active(active)

    async def _dispatch(self, coroutine: Coroutine[Any, Any, T]) -> T:
        loop_thread = self._loop_thread
        if loop_thread is None:
            return await coroutine
        return await asyncio.wrap_future(loop_thread.submit(coroutine))


def _numeric_variant_id(value: str) -> str:
    match = VARIANT_ID_PATTERN.fullmatch(value)
    if match is None:
        raise _quote_unavailable("MERCHANT_VARIANT_INVALID")
    return match.group(1)


async def _validated_cart(
    response: APIResponse,
    variant_id: str,
    expected_item_minor: int,
) -> dict[str, int]:
    if response.status != 200:
        raise _quote_unavailable("MERCHANT_CART_UNAVAILABLE")
    try:
        if len(await response.body()) > MAX_CART_RESPONSE_BYTES:
            raise _quote_unavailable("MERCHANT_CART_INVALID")
        payload = await response.json()
    except Exception as error:
        raise _quote_unavailable("MERCHANT_CART_INVALID") from error
    if not isinstance(payload, dict):
        raise _quote_unavailable("MERCHANT_CART_INVALID")
    items = payload.get("items")
    item_count = payload.get("item_count")
    total_price = payload.get("total_price")
    if (
        item_count != 1
        or not isinstance(items, list)
        or len(items) != 1
        or not isinstance(items[0], dict)
        or str(items[0].get("variant_id")) != variant_id
        or items[0].get("gift_card") is not True
        or items[0].get("requires_shipping") is not False
        or not isinstance(total_price, int)
        or total_price != expected_item_minor
    ):
        raise _quote_unavailable("MERCHANT_CART_MISMATCH")
    return {"total_price": total_price}


def _require_checkout_url(value: str) -> None:
    parsed = urlsplit(value)
    if (
        parsed.scheme != "https"
        or (parsed.hostname or "").casefold() != JACKBOX_CHECKOUT_HOST
        or not parsed.path.startswith("/checkouts/")
        or parsed.username is not None
        or parsed.password is not None
        or parsed.port not in (None, 443)
    ):
        raise _quote_unavailable("MERCHANT_CHECKOUT_REDIRECTED")


async def _fill_billing(page: Page, billing: BillingContact) -> None:
    editable = ':not([aria-hidden="true"]):visible'
    await page.locator(f'input[name="email"]{editable}').fill(billing.email)
    await page.locator('select[name="countryCode"]:visible').select_option(
        billing.country_code
    )
    await page.locator(
        f'input[autocomplete="billing given-name"]{editable}'
    ).fill(billing.first_name)
    await page.locator(
        f'input[autocomplete="billing family-name"]{editable}'
    ).fill(billing.last_name)
    await page.locator(
        f'input[autocomplete="billing address-line1"]{editable}'
    ).fill(billing.address_line1)
    if billing.address_line2:
        await page.locator(
            f'input[autocomplete="billing address-line2"]{editable}'
        ).fill(billing.address_line2)
    await page.locator(
        f'input[autocomplete="billing address-level2"]{editable}'
    ).fill(billing.city)
    zone_select = page.locator('select[name="zone"]:visible')
    zone_input = page.locator(
        f'input[autocomplete="billing address-level1"]{editable}'
    )
    if billing.region:
        if await zone_select.count() == 1:
            if _is_region_code(billing.region):
                await zone_select.select_option(value=billing.region)
            else:
                await zone_select.select_option(label=billing.region)
        elif await zone_input.count() == 1:
            await zone_input.fill(billing.region)
    postal = page.locator(
        f'input[autocomplete="billing postal-code"]{editable}'
    )
    await postal.fill(billing.postal_code)
    phone = page.locator(
        f'input[autocomplete="billing tel-national"]{editable}'
    )
    if billing.phone and await phone.count() == 1:
        await phone.fill(billing.phone)
    await postal.press("Tab")


def _is_region_code(value: str) -> bool:
    return re.fullmatch(r"[A-Z]{2}", value) is not None


async def _wait_for_quote(page: Page, item_minor: int) -> tuple[int, int]:
    previous: int | None = None
    stable_count = 0
    for _ in range(30):
        await page.wait_for_timeout(500)
        try:
            total = await _extract_total(page)
        except MerchantBrowserError:
            continue
        if total == previous:
            stable_count += 1
        else:
            previous = total
            stable_count = 0
        if stable_count >= 2 and total >= item_minor:
            return item_minor, total
    raise _quote_unavailable("MERCHANT_QUOTE_INCOMPLETE")


async def _summary_row(page: Page, label: str) -> str:
    matches = page.get_by_text(label, exact=True)
    count = await matches.count()
    if count == 0:
        raise _quote_unavailable("MERCHANT_QUOTE_INVALID")
    return cast(
        str,
        await matches.nth(count - 1).evaluate(
            "el => el.parentElement?.parentElement?.innerText || ''"
        ),
    )


async def _extract_summary_money(page: Page, label: str) -> int:
    return _money_minor(await _summary_row(page, label))


async def _extract_total(page: Page) -> int:
    return await _extract_summary_money(page, "Total")


def _money_minor(value: str) -> int:
    match = MONEY_PATTERN.search(value)
    if match is None:
        raise _quote_unavailable("MERCHANT_QUOTE_INCOMPLETE")
    try:
        amount = Decimal(match.group(1).replace(",", ""))
    except InvalidOperation as error:
        raise _quote_unavailable("MERCHANT_QUOTE_INVALID") from error
    minor = int(amount * 100)
    if minor <= 0:
        raise _quote_unavailable("MERCHANT_QUOTE_INVALID")
    return minor


async def _fill_payment_fields(
    page: Page,
    credential: SensitivePaymentCredential,
    cardholder_name: str,
) -> None:
    expiry_month = credential.expiry_month.get_secret_value().zfill(2)
    expiry_year = credential.expiry_year.get_secret_value()
    if not expiry_month.isdigit() or not expiry_year.isdigit():
        raise MerchantBrowserError(
            "PRAVA_CREDENTIAL_INVALID",
            "Prava returned an invalid credential expiry.",
            recoverable=False,
        )
    expiry = f"{expiry_month}/{expiry_year[-2:]}"
    await _fill_frame_field(
        page,
        "card-fields-number-",
        'input[name="number"]:visible',
        credential.token.get_secret_value(),
    )
    await _fill_optional_frame_field(
        page,
        "card-fields-number-",
        'input[name="name"]:visible',
        cardholder_name,
    )
    await _fill_frame_field(
        page,
        "card-fields-expiry-",
        'input[name="expiry"]:visible',
        expiry,
    )
    await _fill_frame_field(
        page,
        "card-fields-verification_value-",
        'input[name="verification_value"]:visible',
        credential.dynamic_cvv.get_secret_value(),
    )


async def _fill_frame_field(
    page: Page,
    frame_prefix: str,
    selector: str,
    value: str,
) -> None:
    for frame in page.frames:
        if frame.name.startswith(frame_prefix):
            field = frame.locator(selector)
            if await field.count() == 1:
                await field.fill(value)
                return
    raise MerchantBrowserError(
        "MERCHANT_PAYMENT_FORM_INVALID",
        "The merchant payment field could not be verified.",
        recoverable=True,
    )


async def _fill_optional_frame_field(
    page: Page,
    frame_prefix: str,
    selector: str,
    value: str,
) -> None:
    for frame in page.frames:
        if frame.name.startswith(frame_prefix):
            field = frame.locator(selector)
            if await field.count() == 1:
                await field.fill(value)
                return


async def _observe_checkout_outcome(
    page: Page,
) -> tuple[MerchantCheckoutOutcome, str | None, str]:
    for _ in range(45):
        await page.wait_for_timeout(1000)
        body = (await page.locator("body").inner_text()).strip()
        folded = body.casefold()
        parsed = urlsplit(page.url)
        if "thank_you" in parsed.path or any(
            marker in folded
            for marker in ("your order is confirmed", "thank you for your purchase")
        ):
            order_id = _extract_order_id(body)
            if order_id is not None:
                return (
                    MerchantCheckoutOutcome.ORDER_VERIFIED,
                    order_id,
                    "MERCHANT_ORDER_VERIFIED",
                )
        if any(marker in folded for marker in EXPLICIT_DECLINE_PATTERNS):
            return (
                MerchantCheckoutOutcome.DECLINED,
                None,
                "MERCHANT_PAYMENT_DECLINED",
            )
    return (
        MerchantCheckoutOutcome.UNKNOWN,
        None,
        "MERCHANT_CHECKOUT_OUTCOME_UNKNOWN",
    )


def _extract_order_id(value: str) -> str | None:
    for pattern in ORDER_ID_PATTERNS:
        match = pattern.search(value)
        if match is not None:
            return match.group(1)
    return None


def _quote_unavailable(code: str) -> MerchantBrowserError:
    return MerchantBrowserError(
        code,
        "The merchant could not produce a verified total. Try again.",
        recoverable=True,
    )


async def _close_active(active: _ActiveCheckout) -> None:
    await _close_parts(active.playwright, active.browser, active.context)


async def _close_parts(
    playwright: Playwright | None,
    browser: Browser | None,
    context: BrowserContext | None,
) -> None:
    if context is not None:
        with suppress(Exception):
            await context.close()
    if browser is not None:
        with suppress(Exception):
            await browser.close()
    if playwright is not None:
        with suppress(Exception):
            await playwright.stop()
