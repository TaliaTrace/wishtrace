"""Persist one editable personal message per purchase intent.

Revision ID: 20260801_0009
Revises: 20260801_0008
Create Date: 2026-08-01 22:17:20+05:00
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260801_0009"
down_revision: str | Sequence[str] | None = "20260801_0008"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "personal_messages",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("purchase_intent_id", sa.Uuid(), nullable=False),
        sa.Column("text", sa.String(length=500), nullable=False),
        sa.Column("origin", sa.String(length=16), nullable=False),
        sa.Column("edited", sa.Boolean(), nullable=False),
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
        sa.CheckConstraint(
            "origin IN ('USER', 'AZURE_OPENAI')",
            name="ck_personal_message_origin",
        ),
        sa.ForeignKeyConstraint(
            ["purchase_intent_id"],
            ["purchase_intents.id"],
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "purchase_intent_id",
            name="uq_personal_message_purchase_intent",
        ),
    )
    op.create_index(
        "ix_personal_messages_purchase_intent_id",
        "personal_messages",
        ["purchase_intent_id"],
        unique=False,
    )
    op.create_index(
        "ix_personal_messages_user_id",
        "personal_messages",
        ["user_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_personal_messages_user_id", table_name="personal_messages")
    op.drop_index(
        "ix_personal_messages_purchase_intent_id",
        table_name="personal_messages",
    )
    op.drop_table("personal_messages")
