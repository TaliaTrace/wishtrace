"""Create Google authentication and opaque session tables.

Revision ID: 20260801_0002
Revises: 20260801_0001
Create Date: 2026-08-01 15:40:00+05:00
"""
from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260801_0002"
down_revision: str | Sequence[str] | None = "20260801_0001"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "users",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("google_subject", sa.String(length=255), nullable=False),
        sa.Column("email", sa.String(length=320), nullable=True),
        sa.Column("email_verified", sa.Boolean(), nullable=False),
        sa.Column("display_name", sa.String(length=200), nullable=False),
        sa.Column("picture_url", sa.Text(), nullable=True),
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
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("google_subject"),
    )
    op.create_index("ix_users_google_subject", "users", ["google_subject"], unique=True)
    op.create_table(
        "auth_challenges",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("nonce_hash", sa.LargeBinary(length=32), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("consumed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "ix_auth_challenges_expires_at", "auth_challenges", ["expires_at"], unique=False
    )
    op.create_table(
        "app_sessions",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("token_hash", sa.LargeBinary(length=32), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("token_hash"),
    )
    op.create_index("ix_app_sessions_expires_at", "app_sessions", ["expires_at"], unique=False)
    op.create_index("ix_app_sessions_token_hash", "app_sessions", ["token_hash"], unique=True)
    op.create_index("ix_app_sessions_user_id", "app_sessions", ["user_id"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_app_sessions_user_id", table_name="app_sessions")
    op.drop_index("ix_app_sessions_token_hash", table_name="app_sessions")
    op.drop_index("ix_app_sessions_expires_at", table_name="app_sessions")
    op.drop_table("app_sessions")
    op.drop_index("ix_auth_challenges_expires_at", table_name="auth_challenges")
    op.drop_table("auth_challenges")
    op.drop_index("ix_users_google_subject", table_name="users")
    op.drop_table("users")
