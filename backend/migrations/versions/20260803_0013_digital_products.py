"""Represent verified digital products explicitly.

Revision ID: 20260803_0013
Revises: 20260802_0012
Create Date: 2026-08-03 01:45:00+05:00
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260803_0013"
down_revision: str | Sequence[str] | None = "20260802_0012"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.drop_constraint(
        "ck_candidate_product_kind",
        "candidate_snapshots",
        type_="check",
    )
    op.create_check_constraint(
        "ck_candidate_product_kind",
        "candidate_snapshots",
        "product_kind IN ('PHYSICAL', 'DIGITAL', 'STORED_VALUE')",
    )


def downgrade() -> None:
    op.execute(
        sa.text(
            "UPDATE candidate_snapshots SET product_kind = 'PHYSICAL' "
            "WHERE product_kind = 'DIGITAL'"
        )
    )
    op.drop_constraint(
        "ck_candidate_product_kind",
        "candidate_snapshots",
        type_="check",
    )
    op.create_check_constraint(
        "ck_candidate_product_kind",
        "candidate_snapshots",
        "product_kind IN ('PHYSICAL', 'STORED_VALUE')",
    )
