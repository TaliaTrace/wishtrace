"""Create grounded ranking runs and evidence-linked items.

Revision ID: 20260801_0007
Revises: 20260801_0006
Create Date: 2026-08-01 21:14:00+05:00
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260801_0007"
down_revision: str | Sequence[str] | None = "20260801_0006"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "ranking_runs",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("discovery_run_id", sa.Uuid(), nullable=False),
        sa.Column("status", sa.String(length=32), nullable=False),
        sa.Column("mode", sa.String(length=16), nullable=True),
        sa.Column("uncertainty", sa.String(length=16), nullable=True),
        sa.Column("provider_request_id", sa.String(length=255), nullable=True),
        sa.Column("provider_deployment", sa.String(length=200), nullable=True),
        sa.Column("prompt_version", sa.String(length=32), nullable=False),
        sa.Column("schema_version", sa.String(length=32), nullable=False),
        sa.Column("duration_ms", sa.Integer(), nullable=True),
        sa.Column("attempt_count", sa.Integer(), nullable=False),
        sa.Column("failure_category", sa.String(length=100), nullable=True),
        sa.Column(
            "started_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.CheckConstraint("attempt_count >= 1", name="ck_ranking_attempt_positive"),
        sa.CheckConstraint(
            "duration_ms IS NULL OR duration_ms >= 0",
            name="ck_ranking_duration_non_negative",
        ),
        sa.CheckConstraint(
            "mode IS NULL OR mode IN ('MODEL', 'DETERMINISTIC')",
            name="ck_ranking_mode",
        ),
        sa.CheckConstraint(
            "status IN ('IN_PROGRESS', 'COMPLETED', 'USER_CHOICE_REQUIRED')",
            name="ck_ranking_status",
        ),
        sa.CheckConstraint(
            "uncertainty IS NULL OR uncertainty IN ('LOW', 'MEDIUM', 'HIGH')",
            name="ck_ranking_uncertainty",
        ),
        sa.CheckConstraint(
            "(status = 'IN_PROGRESS' AND mode IS NULL AND uncertainty IS NULL "
            "AND completed_at IS NULL) OR "
            "(status = 'COMPLETED' AND mode IS NOT NULL AND uncertainty IS NOT NULL "
            "AND completed_at IS NOT NULL) OR "
            "(status = 'USER_CHOICE_REQUIRED' AND mode IS NULL AND uncertainty IS NULL "
            "AND completed_at IS NOT NULL)",
            name="ck_ranking_state_shape",
        ),
        sa.ForeignKeyConstraint(
            ["discovery_run_id"],
            ["discovery_runs.id"],
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("discovery_run_id", name="uq_ranking_discovery_run"),
    )
    op.create_index(
        "ix_ranking_runs_user_id",
        "ranking_runs",
        ["user_id"],
        unique=False,
    )

    op.create_table(
        "ranking_evidence",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("ranking_run_id", sa.Uuid(), nullable=False),
        sa.Column("evidence_id", sa.String(length=48), nullable=False),
        sa.Column("source_ref", sa.String(length=100), nullable=False),
        sa.Column("position", sa.Integer(), nullable=False),
        sa.Column("kind", sa.String(length=16), nullable=False),
        sa.Column("value", sa.String(length=500), nullable=False),
        sa.CheckConstraint(
            "kind IN ('INTEREST', 'HINT', 'RELATIONSHIP', 'OCCASION')",
            name="ck_ranking_evidence_kind",
        ),
        sa.CheckConstraint("position >= 0", name="ck_ranking_evidence_position"),
        sa.ForeignKeyConstraint(
            ["ranking_run_id"],
            ["ranking_runs.id"],
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "ranking_run_id",
            "evidence_id",
            name="uq_ranking_evidence_id",
        ),
        sa.UniqueConstraint(
            "ranking_run_id",
            "source_ref",
            name="uq_ranking_evidence_source",
        ),
        sa.UniqueConstraint(
            "ranking_run_id",
            "position",
            name="uq_ranking_evidence_position",
        ),
    )
    op.create_index(
        "ix_ranking_evidence_ranking_run_id",
        "ranking_evidence",
        ["ranking_run_id"],
        unique=False,
    )

    op.create_table(
        "ranking_items",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("ranking_run_id", sa.Uuid(), nullable=False),
        sa.Column("candidate_snapshot_id", sa.Uuid(), nullable=False),
        sa.Column("position", sa.Integer(), nullable=False),
        sa.Column("role", sa.String(length=16), nullable=False),
        sa.Column("reason", sa.String(length=240), nullable=False),
        sa.Column("evidence_ids", sa.JSON(), nullable=False),
        sa.CheckConstraint(
            "position >= 0 AND position <= 2",
            name="ck_ranking_item_position",
        ),
        sa.CheckConstraint(
            "role IN ('SELECTED', 'ALTERNATIVE')",
            name="ck_ranking_item_role",
        ),
        sa.CheckConstraint(
            "(position = 0 AND role = 'SELECTED') OR "
            "(position > 0 AND role = 'ALTERNATIVE')",
            name="ck_ranking_item_role_position",
        ),
        sa.ForeignKeyConstraint(
            ["candidate_snapshot_id"],
            ["candidate_snapshots.id"],
            ondelete="RESTRICT",
        ),
        sa.ForeignKeyConstraint(
            ["ranking_run_id"],
            ["ranking_runs.id"],
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "ranking_run_id",
            "candidate_snapshot_id",
            name="uq_ranking_item_candidate",
        ),
        sa.UniqueConstraint(
            "ranking_run_id",
            "position",
            name="uq_ranking_item_position",
        ),
    )
    op.create_index(
        "ix_ranking_items_candidate_snapshot_id",
        "ranking_items",
        ["candidate_snapshot_id"],
        unique=False,
    )
    op.create_index(
        "ix_ranking_items_ranking_run_id",
        "ranking_items",
        ["ranking_run_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(
        "ix_ranking_items_ranking_run_id",
        table_name="ranking_items",
    )
    op.drop_index(
        "ix_ranking_items_candidate_snapshot_id",
        table_name="ranking_items",
    )
    op.drop_table("ranking_items")
    op.drop_index(
        "ix_ranking_evidence_ranking_run_id",
        table_name="ranking_evidence",
    )
    op.drop_table("ranking_evidence")
    op.drop_index("ix_ranking_runs_user_id", table_name="ranking_runs")
    op.drop_table("ranking_runs")
