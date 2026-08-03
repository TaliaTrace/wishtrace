"""Allow a user to remember more than one recipient.

Revision ID: 20260803_0015
Revises: 20260803_0014
Create Date: 2026-08-03 05:00:00+05:00
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260803_0015"
down_revision: str | Sequence[str] | None = "20260803_0014"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.drop_constraint("uq_recipients_user_id", "recipients", type_="unique")


def downgrade() -> None:
    op.create_unique_constraint("uq_recipients_user_id", "recipients", ["user_id"])
