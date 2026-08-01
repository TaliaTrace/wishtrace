# WishTrace backend

The backend owns authentication, persistence, commerce facts, OpenAI orchestration,
Prava sessions, idempotency, and authoritative transaction state. Secrets never belong
in the Android application.

## Local setup

Use Python 3.12 through `uv`:

```powershell
cd backend
uv sync --all-groups
uv run python -m scripts.probe_database
uv run alembic upgrade head
uv run python -m scripts.run_server
```

The repository-root `.env` is loaded automatically. `DATABASE_URL` must use PostgreSQL
and contain `sslmode=require`; startup and the database probe reject an insecure DSN.
`GOOGLE_WEB_CLIENT_ID` and a stable, random `SESSION_TOKEN_PEPPER` of at least 32 bytes
are required for authentication. The local launcher selects a psycopg-compatible event
loop on Windows. Azure ranking requires all three `AZURE_OPENAI_BASE_URL`,
`AZURE_OPENAI_API_KEY`, and `AZURE_OPENAI_DEPLOYMENT` values. The base URL must be the
exact HTTPS Azure target ending in `/openai/v1/`; copy it from the deployment rather than
deriving the hostname.

The narrow commerce path is the observed Jackbox Games $5 digital gift card. To exercise
the real Shopify quote/checkout actor, set `MERCHANT_CHECKOUT_ENABLED=true`, point
`MERCHANT_BROWSER_EXECUTABLE_PATH` at a local Chrome/Chromium executable, and set
`ALLOW_STORED_VALUE_PRODUCTS=true` only after Prava confirms stored-value eligibility.
Billing exists only in request/browser memory; Prava one-time credentials stay in the
short-lived browser process. Neither is persisted. The live SKU has no shipping
requirement, but its regional and stored-value restrictions still apply. Jackbox sends
the card to the purchaser's verified checkout email; the purchaser must forward it to
the recipient. WishTrace does not claim direct recipient delivery or guaranteed timing.

## Quality gates

```powershell
uv run python -m pytest
uv run ruff check .
uv run mypy app scripts
```

`GET /health` proves a TLS database connection. `GET /.well-known/ucp` publishes only
the UCP catalog capabilities currently implemented by WishTrace; checkout and payment
capabilities are intentionally absent until they are verified. Authenticated discovery
ranking uses `POST /v1/discoveries/{id}/rank` and
`GET /v1/discoveries/{id}/ranking`. Both routes refuse to rank when no live candidate has
a verified checkout path. Purchase routes add an idempotent live quote, hosted Prava
approval, authoritative reconciliation, and an editable personal message. A Prava result
cannot become an order receipt without a verified merchant order ID.
