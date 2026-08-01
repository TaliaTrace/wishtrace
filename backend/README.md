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
loop on Windows.

## Quality gates

```powershell
uv run python -m pytest
uv run ruff check .
uv run mypy app scripts
```

`GET /health` proves a TLS database connection. `GET /.well-known/ucp` publishes only
the UCP catalog capabilities currently implemented by WishTrace; checkout and payment
capabilities are intentionally absent until they are verified.
