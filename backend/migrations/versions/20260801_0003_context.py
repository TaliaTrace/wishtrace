"""Create recipient, preference, hint, and occasion tables.

Revision ID: 20260801_0003
Revises: 20260801_0002
Create Date: 2026-08-01 16:10:00+05:00
"""
from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260801_0003"
down_revision: str | Sequence[str] | None = "20260801_0002"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "recipients",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("display_name", sa.String(length=100), nullable=False),
        sa.Column("relationship", sa.String(length=100), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_recipients_user_id", "recipients", ["user_id"], unique=False)
    op.create_table(
        "recipient_preferences",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("recipient_id", sa.Uuid(), nullable=False),
        sa.Column("kind", sa.String(length=16), nullable=False),
        sa.Column("value", sa.String(length=100), nullable=False),
        sa.Column("position", sa.Integer(), nullable=False),
        sa.CheckConstraint("kind IN ('INTEREST', 'DISLIKE')", name="ck_preference_kind"),
        sa.ForeignKeyConstraint(["recipient_id"], ["recipients.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("recipient_id", "kind", "value", name="uq_preference_value"),
    )
    op.create_index(
        "ix_recipient_preferences_recipient_id",
        "recipient_preferences",
        ["recipient_id"],
        unique=False,
    )
    op.create_table(
        "hints",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("recipient_id", sa.Uuid(), nullable=False),
        sa.Column("text", sa.Text(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["recipient_id"], ["recipients.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_hints_recipient_id", "hints", ["recipient_id"], unique=False)
    op.create_table(
        "occasions",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("recipient_id", sa.Uuid(), nullable=False),
        sa.Column("kind", sa.String(length=32), nullable=False),
        sa.Column("local_date", sa.Date(), nullable=False),
        sa.Column("time_zone", sa.String(length=64), nullable=False),
        sa.Column("budget_minor", sa.BigInteger(), nullable=False),
        sa.Column("currency", sa.String(length=3), nullable=False),
        sa.Column("required_arrival_date", sa.Date(), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.CheckConstraint("budget_minor > 0", name="ck_occasion_budget_positive"),
        sa.CheckConstraint("currency = 'USD'", name="ck_occasion_currency_usd"),
        sa.CheckConstraint("kind = 'BIRTHDAY'", name="ck_occasion_kind"),
        sa.ForeignKeyConstraint(["recipient_id"], ["recipients.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("recipient_id", "kind", name="uq_recipient_occasion_kind"),
    )
    op.create_index("ix_occasions_recipient_id", "occasions", ["recipient_id"], unique=False)
    op.create_index("ix_occasions_user_id", "occasions", ["user_id"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_occasions_user_id", table_name="occasions")
    op.drop_index("ix_occasions_recipient_id", table_name="occasions")
    op.drop_table("occasions")
    op.drop_index("ix_hints_recipient_id", table_name="hints")
    op.drop_table("hints")
    op.drop_index("ix_recipient_preferences_recipient_id", table_name="recipient_preferences")
    op.drop_table("recipient_preferences")
    op.drop_index("ix_recipients_user_id", table_name="recipients")
    op.drop_table("recipients")
