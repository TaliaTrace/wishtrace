"""Persist immutable live discovery evidence.

Revision ID: 20260801_0005
Revises: 20260801_0004
Create Date: 2026-08-01 20:30:00+05:00
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260801_0005"
down_revision: str | Sequence[str] | None = "20260801_0004"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "discovery_runs",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("recipient_id", sa.Uuid(), nullable=False),
        sa.Column("occasion_id", sa.Uuid(), nullable=False),
        sa.Column("status", sa.String(length=16), nullable=False),
        sa.Column("merchant_id", sa.String(length=100), nullable=False),
        sa.Column("merchant_name", sa.String(length=200), nullable=False),
        sa.Column("search_query", sa.String(length=200), nullable=False),
        sa.Column("budget_minor", sa.BigInteger(), nullable=False),
        sa.Column("currency", sa.String(length=3), nullable=False),
        sa.Column("source_request_id", sa.String(length=255), nullable=True),
        sa.Column("profile_cache_compliant", sa.Boolean(), nullable=False),
        sa.Column("source_timestamp", sa.DateTime(timezone=True), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.CheckConstraint("budget_minor > 0", name="ck_discovery_budget_positive"),
        sa.CheckConstraint("currency = 'USD'", name="ck_discovery_currency_usd"),
        sa.CheckConstraint("status = 'COMPLETED'", name="ck_discovery_status"),
        sa.ForeignKeyConstraint(["occasion_id"], ["occasions.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["recipient_id"], ["recipients.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "ix_discovery_runs_occasion_id",
        "discovery_runs",
        ["occasion_id"],
        unique=False,
    )
    op.create_index(
        "ix_discovery_runs_recipient_id",
        "discovery_runs",
        ["recipient_id"],
        unique=False,
    )
    op.create_index(
        "ix_discovery_runs_user_id",
        "discovery_runs",
        ["user_id"],
        unique=False,
    )
    op.create_table(
        "candidate_snapshots",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("discovery_run_id", sa.Uuid(), nullable=False),
        sa.Column("position", sa.Integer(), nullable=False),
        sa.Column("source_key", sa.String(length=64), nullable=False),
        sa.Column("merchant_product_id", sa.String(length=255), nullable=False),
        sa.Column("merchant_variant_id", sa.String(length=255), nullable=True),
        sa.Column("sku", sa.String(length=255), nullable=True),
        sa.Column("title", sa.String(length=500), nullable=False),
        sa.Column("variant_title", sa.String(length=500), nullable=True),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("product_url", sa.Text(), nullable=False),
        sa.Column("image_url", sa.Text(), nullable=True),
        sa.Column("price_minor", sa.BigInteger(), nullable=False),
        sa.Column("currency", sa.String(length=3), nullable=False),
        sa.Column("availability", sa.String(length=16), nullable=False),
        sa.Column("selected_options", sa.JSON(), nullable=False),
        sa.Column("categories", sa.JSON(), nullable=False),
        sa.Column("tags", sa.JSON(), nullable=False),
        sa.Column("product_kind", sa.String(length=16), nullable=False),
        sa.Column("checkout_supported", sa.Boolean(), nullable=False),
        sa.Column("delivery_state", sa.String(length=16), nullable=False),
        sa.Column("source_timestamp", sa.DateTime(timezone=True), nullable=False),
        sa.Column("source_mode", sa.String(length=8), nullable=False),
        sa.Column("eligible", sa.Boolean(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.CheckConstraint(
            "availability IN ('AVAILABLE', 'UNAVAILABLE', 'UNKNOWN')",
            name="ck_candidate_availability",
        ),
        sa.CheckConstraint("currency = 'USD'", name="ck_candidate_currency_usd"),
        sa.CheckConstraint("delivery_state = 'UNKNOWN'", name="ck_candidate_delivery"),
        sa.CheckConstraint("price_minor >= 0", name="ck_candidate_price_non_negative"),
        sa.CheckConstraint(
            "product_kind IN ('PHYSICAL', 'STORED_VALUE')",
            name="ck_candidate_product_kind",
        ),
        sa.CheckConstraint("source_mode = 'LIVE'", name="ck_candidate_source_mode"),
        sa.ForeignKeyConstraint(
            ["discovery_run_id"],
            ["discovery_runs.id"],
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "discovery_run_id",
            "position",
            name="uq_candidate_run_position",
        ),
        sa.UniqueConstraint(
            "discovery_run_id",
            "source_key",
            name="uq_candidate_run_source_key",
        ),
    )
    op.create_index(
        "ix_candidate_snapshots_discovery_run_id",
        "candidate_snapshots",
        ["discovery_run_id"],
        unique=False,
    )
    op.create_table(
        "candidate_rejections",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("candidate_snapshot_id", sa.Uuid(), nullable=False),
        sa.Column("code", sa.String(length=32), nullable=False),
        sa.Column("reason", sa.String(length=500), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["candidate_snapshot_id"],
            ["candidate_snapshots.id"],
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("candidate_snapshot_id"),
    )
    op.create_index(
        "ix_candidate_rejections_candidate_snapshot_id",
        "candidate_rejections",
        ["candidate_snapshot_id"],
        unique=True,
    )


def downgrade() -> None:
    op.drop_index(
        "ix_candidate_rejections_candidate_snapshot_id",
        table_name="candidate_rejections",
    )
    op.drop_table("candidate_rejections")
    op.drop_index(
        "ix_candidate_snapshots_discovery_run_id",
        table_name="candidate_snapshots",
    )
    op.drop_table("candidate_snapshots")
    op.drop_index("ix_discovery_runs_user_id", table_name="discovery_runs")
    op.drop_index("ix_discovery_runs_recipient_id", table_name="discovery_runs")
    op.drop_index("ix_discovery_runs_occasion_id", table_name="discovery_runs")
    op.drop_table("discovery_runs")
