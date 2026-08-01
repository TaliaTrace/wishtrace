"""Enforce the one-recipient Gold scope.

Revision ID: 20260801_0004
Revises: 20260801_0003
Create Date: 2026-08-01 19:50:00+05:00
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260801_0004"
down_revision: str | Sequence[str] | None = "20260801_0003"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_unique_constraint("uq_recipients_user_id", "recipients", ["user_id"])


def downgrade() -> None:
    op.drop_constraint("uq_recipients_user_id", "recipients", type_="unique")
