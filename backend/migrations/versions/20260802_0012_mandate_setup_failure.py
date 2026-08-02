"""Persist safe Prava mandate setup failure categories.

Revision ID: 20260802_0012
Revises: 20260801_0011
Create Date: 2026-08-02 23:15:00+05:00
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260802_0012"
down_revision: str | Sequence[str] | None = "20260801_0011"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "mandates",
        sa.Column("setup_failure_code", sa.String(length=100), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("mandates", "setup_failure_code")
