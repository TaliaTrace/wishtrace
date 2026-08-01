"""Establish the WishTrace migration baseline.

Revision ID: 20260801_0001
Revises:
Create Date: 2026-08-01 14:45:00+05:00
"""
from collections.abc import Sequence

revision: str = "20260801_0001"
down_revision: str | Sequence[str] | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Alembic's version table is the only foundation object."""


def downgrade() -> None:
    """No application tables exist in the foundation revision."""
