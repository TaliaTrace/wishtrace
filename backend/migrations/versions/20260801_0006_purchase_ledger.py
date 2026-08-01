"""Create purchase, Prava, idempotency, and transition ledgers.

Revision ID: 20260801_0006
Revises: 20260801_0005
Create Date: 2026-08-01 20:55:00+05:00
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260801_0006"
down_revision: str | Sequence[str] | None = "20260801_0005"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.create_table(
        "purchase_intents",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("recipient_id", sa.Uuid(), nullable=False),
        sa.Column("occasion_id", sa.Uuid(), nullable=False),
        sa.Column("discovery_run_id", sa.Uuid(), nullable=False),
        sa.Column("candidate_snapshot_id", sa.Uuid(), nullable=False),
        sa.Column("state", sa.String(length=32), nullable=False),
        sa.Column("merchant_id", sa.String(length=100), nullable=False),
        sa.Column("merchant_name", sa.String(length=200), nullable=False),
        sa.Column("merchant_url", sa.Text(), nullable=False),
        sa.Column("merchant_product_id", sa.String(length=255), nullable=False),
        sa.Column("merchant_variant_id", sa.String(length=255), nullable=False),
        sa.Column("sku", sa.String(length=255), nullable=True),
        sa.Column("title", sa.String(length=500), nullable=False),
        sa.Column("variant_title", sa.String(length=500), nullable=True),
        sa.Column("item_price_minor", sa.BigInteger(), nullable=False),
        sa.Column("currency", sa.String(length=3), nullable=False),
        sa.Column("approved_total_minor", sa.BigInteger(), nullable=True),
        sa.Column("quote_source", sa.String(length=100), nullable=True),
        sa.Column("quote_timestamp", sa.DateTime(timezone=True), nullable=True),
        sa.Column("quote_expires_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("delivery_summary", sa.String(length=500), nullable=True),
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
            "approved_total_minor IS NULL OR approved_total_minor > 0",
            name="ck_purchase_approved_total_positive",
        ),
        sa.CheckConstraint("currency = 'USD'", name="ck_purchase_intent_currency_usd"),
        sa.CheckConstraint(
            "item_price_minor >= 0",
            name="ck_purchase_item_price_non_negative",
        ),
        sa.CheckConstraint(
            "state IN ('DRAFT', 'VALIDATING', 'QUOTED', 'READY_FOR_APPROVAL', "
            "'SESSION_CREATING', 'AWAITING_USER', 'CREDENTIALS_READY', "
            "'CHECKOUT_IN_PROGRESS', 'ORDER_VERIFIED', 'SUCCEEDED', 'DECLINED', "
            "'CANCELLED', 'EXPIRED', 'FAILED', 'UNKNOWN', 'RECONCILING')",
            name="ck_purchase_intent_state",
        ),
        sa.ForeignKeyConstraint(
            ["candidate_snapshot_id"],
            ["candidate_snapshots.id"],
            ondelete="RESTRICT",
        ),
        sa.ForeignKeyConstraint(
            ["discovery_run_id"],
            ["discovery_runs.id"],
            ondelete="RESTRICT",
        ),
        sa.ForeignKeyConstraint(["occasion_id"], ["occasions.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["recipient_id"], ["recipients.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "user_id",
            "candidate_snapshot_id",
            name="uq_purchase_user_candidate",
        ),
    )
    for column in (
        "candidate_snapshot_id",
        "discovery_run_id",
        "occasion_id",
        "recipient_id",
        "user_id",
    ):
        op.create_index(
            f"ix_purchase_intents_{column}",
            "purchase_intents",
            [column],
            unique=False,
        )

    op.create_table(
        "prava_sessions",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("purchase_intent_id", sa.Uuid(), nullable=False),
        sa.Column("provider_session_id", sa.String(length=255), nullable=False),
        sa.Column("provider_order_id", sa.String(length=255), nullable=False),
        sa.Column("hosted_url", sa.Text(), nullable=False),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("create_response_id", sa.String(length=255), nullable=True),
        sa.Column("provider_transaction_id", sa.String(length=255), nullable=True),
        sa.Column("provider_txn_ref_id", sa.String(length=255), nullable=True),
        sa.Column("provider_status", sa.String(length=32), nullable=True),
        sa.Column("last_response_id", sa.String(length=255), nullable=True),
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
        sa.ForeignKeyConstraint(
            ["purchase_intent_id"],
            ["purchase_intents.id"],
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("provider_session_id"),
        sa.UniqueConstraint("purchase_intent_id"),
    )
    op.create_index(
        "ix_prava_sessions_purchase_intent_id",
        "prava_sessions",
        ["purchase_intent_id"],
        unique=True,
    )

    op.create_table(
        "idempotency_operations",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("purchase_intent_id", sa.Uuid(), nullable=False),
        sa.Column("operation", sa.String(length=32), nullable=False),
        sa.Column("key_hash", sa.LargeBinary(length=32), nullable=False),
        sa.Column("request_hash", sa.LargeBinary(length=32), nullable=False),
        sa.Column("status", sa.String(length=16), nullable=False),
        sa.Column("provider_response_id", sa.String(length=255), nullable=True),
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
        sa.CheckConstraint("operation = 'PRAVA_SESSION'", name="ck_idempotency_operation"),
        sa.CheckConstraint(
            "status IN ('IN_PROGRESS', 'COMPLETED', 'UNKNOWN', 'FAILED')",
            name="ck_idempotency_status",
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
            "operation",
            name="uq_idempotency_purchase_operation",
        ),
        sa.UniqueConstraint(
            "user_id",
            "operation",
            "key_hash",
            name="uq_idempotency_user_operation_key",
        ),
    )
    op.create_index(
        "ix_idempotency_operations_purchase_intent_id",
        "idempotency_operations",
        ["purchase_intent_id"],
        unique=False,
    )
    op.create_index(
        "ix_idempotency_operations_user_id",
        "idempotency_operations",
        ["user_id"],
        unique=False,
    )

    op.create_table(
        "transaction_transitions",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("purchase_intent_id", sa.Uuid(), nullable=False),
        sa.Column("from_state", sa.String(length=32), nullable=True),
        sa.Column("to_state", sa.String(length=32), nullable=False),
        sa.Column("reason_code", sa.String(length=100), nullable=False),
        sa.Column("provider_response_id", sa.String(length=255), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["purchase_intent_id"],
            ["purchase_intents.id"],
            ondelete="CASCADE",
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "ix_transaction_transitions_purchase_intent_id",
        "transaction_transitions",
        ["purchase_intent_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index(
        "ix_transaction_transitions_purchase_intent_id",
        table_name="transaction_transitions",
    )
    op.drop_table("transaction_transitions")
    op.drop_index(
        "ix_idempotency_operations_user_id",
        table_name="idempotency_operations",
    )
    op.drop_index(
        "ix_idempotency_operations_purchase_intent_id",
        table_name="idempotency_operations",
    )
    op.drop_table("idempotency_operations")
    op.drop_index("ix_prava_sessions_purchase_intent_id", table_name="prava_sessions")
    op.drop_table("prava_sessions")
    for column in (
        "user_id",
        "recipient_id",
        "occasion_id",
        "discovery_run_id",
        "candidate_snapshot_id",
    ):
        op.drop_index(f"ix_purchase_intents_{column}", table_name="purchase_intents")
    op.drop_table("purchase_intents")
