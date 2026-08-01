# WishTrace backend

The backend owns authentication, persistence, commerce facts, OpenAI orchestration,
Prava sessions, idempotency, and authoritative transaction state. Secrets never belong
in the Android application.

## Local setup

Use Python 3.12 through `uv`:

```powershell
cd backend
uv sync --dev
uv run python -m scripts.probe_database
uv run alembic upgrade head
uv run uvicorn app.main:create_app --factory --reload
```

The repository-root `.env` is loaded automatically. `DATABASE_URL` must use PostgreSQL
and contain `sslmode=require`; startup and the database probe reject an insecure DSN.

## Quality gates

```powershell
uv run pytest
uv run ruff check .
uv run mypy app scripts
```

`GET /health` proves a TLS database connection. `GET /.well-known/ucp` publishes only
the UCP catalog capabilities currently implemented by WishTrace; checkout and payment
capabilities are intentionally absent until they are verified.
