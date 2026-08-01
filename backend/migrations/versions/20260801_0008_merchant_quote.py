"""Allow idempotent live merchant quotes.

Revision ID: 20260801_0008
Revises: 20260801_0007
Create Date: 2026-08-01 22:06:45+05:00
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260801_0008"
down_revision: str | Sequence[str] | None = "20260801_0007"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "purchase_intents",
        sa.Column("merchant_order_id", sa.String(length=255), nullable=True),
    )
    op.add_column(
        "purchase_intents",
        sa.Column("merchant_outcome", sa.String(length=32), nullable=True),
    )
    op.add_column(
        "purchase_intents",
        sa.Column("merchant_attempted_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_check_constraint(
        "ck_purchase_merchant_outcome",
        "purchase_intents",
        "merchant_outcome IS NULL OR merchant_outcome IN "
        "('ORDER_VERIFIED', 'DECLINED', 'UNKNOWN')",
    )
    op.add_column(
        "prava_sessions",
        sa.Column("report_response_id", sa.String(length=255), nullable=True),
    )
    op.add_column(
        "prava_sessions",
        sa.Column("visa_confirmation", sa.String(length=16), nullable=True),
    )
    op.create_check_constraint(
        "ck_prava_visa_confirmation",
        "prava_sessions",
        "visa_confirmation IS NULL OR visa_confirmation IN ('SUCCESS', 'FAILURE')",
    )
    op.drop_constraint(
        "ck_idempotency_operation",
        "idempotency_operations",
        type_="check",
    )
    op.create_check_constraint(
        "ck_idempotency_operation",
        "idempotency_operations",
        "operation IN ('MERCHANT_QUOTE', 'PRAVA_SESSION')",
    )


def downgrade() -> None:
    op.drop_constraint(
        "ck_idempotency_operation",
        "idempotency_operations",
        type_="check",
    )
    op.create_check_constraint(
        "ck_idempotency_operation",
        "idempotency_operations",
        "operation = 'PRAVA_SESSION'",
    )
    op.drop_constraint(
        "ck_prava_visa_confirmation",
        "prava_sessions",
        type_="check",
    )
    op.drop_column("prava_sessions", "visa_confirmation")
    op.drop_column("prava_sessions", "report_response_id")
    op.drop_constraint(
        "ck_purchase_merchant_outcome",
        "purchase_intents",
        type_="check",
    )
    op.drop_column("purchase_intents", "merchant_attempted_at")
    op.drop_column("purchase_intents", "merchant_outcome")
    op.drop_column("purchase_intents", "merchant_order_id")
