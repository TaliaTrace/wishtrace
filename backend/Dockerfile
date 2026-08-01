FROM python:3.12-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    UV_PROJECT_ENVIRONMENT=/opt/wishtrace/.venv \
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

WORKDIR /opt/wishtrace

RUN python -m pip install --no-cache-dir uv==0.11.12

COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev \
    && uv run playwright install --with-deps chromium \
    && rm -rf /var/lib/apt/lists/*

COPY alembic.ini ./
COPY app ./app
COPY migrations ./migrations
COPY scripts ./scripts

ENV PATH="/opt/wishtrace/.venv/bin:${PATH}"

EXPOSE 8000

CMD ["uvicorn", "app.main:create_app", "--factory", "--host", "0.0.0.0", "--port", "8000"]
