"""Preserve failed mandate attempts when the user chooses another gift.

Revision ID: 20260803_0014
Revises: 20260803_0013
Create Date: 2026-08-03 01:47:00+05:00
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260803_0014"
down_revision: str | Sequence[str] | None = "20260803_0013"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.drop_constraint("uq_mandate_occasion", "mandates", type_="unique")


def downgrade() -> None:
    # This intentionally fails if newer history rows exist; silently deleting
    # transaction evidence during a downgrade would violate the audit boundary.
    op.create_unique_constraint("uq_mandate_occasion", "mandates", ["occasion_id"])
