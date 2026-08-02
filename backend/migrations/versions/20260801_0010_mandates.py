"""Add mandate tables, Gift-DNA columns, and mandate idempotency operations.

Revision ID: 20260801_0010
Revises: 20260801_0009
Create Date: 2026-08-02 15:30:00+05:00
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "20260801_0010"
down_revision: str | Sequence[str] | None = "20260801_0009"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    # --- Gift-DNA columns on existing tables ---
    op.add_column(
        "recipients",
        sa.Column("personality_traits", sa.JSON(), nullable=True),
    )
    op.add_column(
        "recipients",
        sa.Column("age_band", sa.String(length=16), nullable=True),
    )
    op.add_column(
        "occasions",
        sa.Column(
            "recurring_frequency",
            sa.String(length=16),
            server_default="one_time",
            nullable=False,
        ),
    )
    op.create_check_constraint(
        "ck_occasion_recurring_frequency",
        "occasions",
        "recurring_frequency IN ('one_time', 'yearly')",
    )

    # --- Mandates table ---
    op.create_table(
        "mandates",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("recipient_id", sa.Uuid(), nullable=False),
        sa.Column("occasion_id", sa.Uuid(), nullable=False),
        sa.Column("state", sa.String(length=32), nullable=False),
        sa.Column("approved_amount_minor", sa.BigInteger(), nullable=False),
        sa.Column("currency", sa.String(length=3), nullable=False),
        sa.Column("recurring_frequency", sa.String(length=16), nullable=False),
        sa.Column("merchant_scope", sa.String(length=16), nullable=False),
        sa.Column("max_charges", sa.Integer(), nullable=False),
        sa.Column("valid_until", sa.DateTime(timezone=True), nullable=True),
        sa.Column("merchant_id", sa.String(length=100), nullable=False),
        sa.Column("merchant_name", sa.String(length=200), nullable=False),
        sa.Column("merchant_url", sa.Text(), nullable=False),
        sa.Column("merchant_product_id", sa.String(length=255), nullable=False),
        sa.Column("merchant_variant_id", sa.String(length=255), nullable=False),
        sa.Column("product_title", sa.String(length=500), nullable=False),
        sa.Column("item_price_minor", sa.BigInteger(), nullable=False),
        sa.Column("setup_session_id", sa.String(length=255), nullable=True),
        sa.Column("setup_hosted_url", sa.Text(), nullable=True),
        sa.Column("setup_expires_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("setup_response_id", sa.String(length=255), nullable=True),
        sa.Column("provider_mandate_id", sa.String(length=255), nullable=True),
        sa.Column("provider_status", sa.String(length=32), nullable=True),
        sa.Column("charges_used", sa.Integer(), server_default="0", nullable=False),
        sa.Column("merchant_order_id", sa.String(length=255), nullable=True),
        sa.Column("merchant_outcome", sa.String(length=32), nullable=True),
        sa.Column("visa_confirmation", sa.String(length=16), nullable=True),
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
        sa.CheckConstraint("currency = 'USD'", name="ck_mandate_currency_usd"),
        sa.CheckConstraint(
            "approved_amount_minor > 0", name="ck_mandate_amount_positive"
        ),
        sa.CheckConstraint("max_charges >= 1", name="ck_mandate_max_charges"),
        sa.CheckConstraint(
            "recurring_frequency IN ('one_time', 'weekly', 'monthly', 'yearly')",
            name="ck_mandate_recurring_frequency",
        ),
        sa.CheckConstraint(
            "merchant_scope IN ('listed', 'any')", name="ck_mandate_merchant_scope"
        ),
        sa.CheckConstraint(
            "visa_confirmation IS NULL OR visa_confirmation IN ('SUCCESS', 'FAILURE')",
            name="ck_mandate_visa_confirmation",
        ),
        sa.CheckConstraint(
            "merchant_outcome IS NULL OR merchant_outcome IN "
            "('ORDER_VERIFIED', 'DECLINED', 'UNKNOWN')",
            name="ck_mandate_merchant_outcome",
        ),
        sa.CheckConstraint(
            "state IN ('SETUP_CREATING', 'AWAITING_APPROVAL', 'ACTIVE', "
            "'CHARGING', 'CHECKOUT_IN_PROGRESS', 'REPORTING', 'SUCCEEDED', "
            "'DECLINED', 'CONSUMED', 'PAUSED', 'CANCELLED', 'EXPIRED', "
            "'FAILED', 'UNKNOWN')",
            name="ck_mandate_state",
        ),
        sa.ForeignKeyConstraint(
            ["occasion_id"], ["occasions.id"], ondelete="CASCADE"
        ),
        sa.ForeignKeyConstraint(
            ["recipient_id"], ["recipients.id"], ondelete="CASCADE"
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("occasion_id", name="uq_mandate_occasion"),
        sa.UniqueConstraint("provider_mandate_id", name="uq_mandate_provider_id"),
    )
    op.create_index("ix_mandates_occasion_id", "mandates", ["occasion_id"], unique=False)
    op.create_index("ix_mandates_recipient_id", "mandates", ["recipient_id"], unique=False)
    op.create_index("ix_mandates_user_id", "mandates", ["user_id"], unique=False)

    # --- Mandate charges table ---
    op.create_table(
        "mandate_charges",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("mandate_id", sa.Uuid(), nullable=False),
        sa.Column("reference", sa.String(length=255), nullable=False),
        sa.Column("amount_minor", sa.BigInteger(), nullable=False),
        sa.Column("state", sa.String(length=32), nullable=False),
        sa.Column("provider_charge_id", sa.String(length=255), nullable=True),
        sa.Column("provider_txn_ref_id", sa.String(length=255), nullable=True),
        sa.Column("provider_error_code", sa.String(length=100), nullable=True),
        sa.Column("merchant_order_id", sa.String(length=255), nullable=True),
        sa.Column("merchant_outcome", sa.String(length=32), nullable=True),
        sa.Column("visa_confirmation", sa.String(length=16), nullable=True),
        sa.Column("charge_response_id", sa.String(length=255), nullable=True),
        sa.Column("report_response_id", sa.String(length=255), nullable=True),
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
            "amount_minor > 0", name="ck_mandate_charge_amount_positive"
        ),
        sa.CheckConstraint(
            "state IN ('CHARGING', 'CHECKOUT_IN_PROGRESS', 'REPORTING', "
            "'SUCCEEDED', 'DECLINED', 'FAILED', 'UNKNOWN')",
            name="ck_mandate_charge_state",
        ),
        sa.CheckConstraint(
            "visa_confirmation IS NULL OR visa_confirmation IN ('SUCCESS', 'FAILURE')",
            name="ck_mandate_charge_visa_confirmation",
        ),
        sa.CheckConstraint(
            "merchant_outcome IS NULL OR merchant_outcome IN "
            "('ORDER_VERIFIED', 'DECLINED', 'UNKNOWN')",
            name="ck_mandate_charge_merchant_outcome",
        ),
        sa.ForeignKeyConstraint(
            ["mandate_id"], ["mandates.id"], ondelete="CASCADE"
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint(
            "mandate_id", "reference", name="uq_mandate_charge_reference"
        ),
    )
    op.create_index(
        "ix_mandate_charges_mandate_id",
        "mandate_charges",
        ["mandate_id"],
        unique=False,
    )

    # --- Idempotency operations now include mandate actions ---
    op.drop_constraint(
        "ck_idempotency_operation",
        "idempotency_operations",
        type_="check",
    )
    op.create_check_constraint(
        "ck_idempotency_operation",
        "idempotency_operations",
        "operation IN ('MERCHANT_QUOTE', 'PRAVA_SESSION', 'MANDATE_SETUP', 'MANDATE_CHARGE')",
    )


def downgrade() -> None:
    op.drop_constraint(
        "ck_idempotency_operation",
        "idempotency_operations",
        type_="check",
    )
    op.create_check_constraint(
        "ck_idempotency_operation",
        "idempotency_operations",
        "operation IN ('MERCHANT_QUOTE', 'PRAVA_SESSION')",
    )
    op.drop_index("ix_mandate_charges_mandate_id", table_name="mandate_charges")
    op.drop_table("mandate_charges")
    op.drop_index("ix_mandates_user_id", table_name="mandates")
    op.drop_index("ix_mandates_recipient_id", table_name="mandates")
    op.drop_index("ix_mandates_occasion_id", table_name="mandates")
    op.drop_table("mandates")
    op.drop_constraint(
        "ck_occasion_recurring_frequency",
        "occasions",
        type_="check",
    )
    op.drop_column("occasions", "recurring_frequency")
    op.drop_column("recipients", "age_band")
    op.drop_column("recipients", "personality_traits")
