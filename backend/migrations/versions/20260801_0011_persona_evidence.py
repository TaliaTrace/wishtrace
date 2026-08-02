"""Widen ranking evidence kinds for Gift-DNA persona/age signals.

Partial-profile ranking sources evidence from the green-tile personality taps
and rough age band when no interests are captured, so the ranking_evidence.kind
check constraint must admit the new PERSONALITY and AGE kinds.

Revision ID: 20260801_0011
Revises: 20260801_0010
Create Date: 2026-08-02 12:50:00+05:00
"""

from collections.abc import Sequence

from alembic import op

revision: str = "20260801_0011"
down_revision: str | Sequence[str] | None = "20260801_0010"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

_OLD_KINDS = "kind IN ('INTEREST', 'HINT', 'RELATIONSHIP', 'OCCASION')"
_NEW_KINDS = (
    "kind IN ('INTEREST', 'HINT', 'RELATIONSHIP', 'OCCASION', "
    "'PERSONALITY', 'AGE')"
)


def upgrade() -> None:
    op.drop_constraint(
        "ck_ranking_evidence_kind",
        "ranking_evidence",
        type_="check",
    )
    op.create_check_constraint(
        "ck_ranking_evidence_kind",
        "ranking_evidence",
        _NEW_KINDS,
    )


def downgrade() -> None:
    op.drop_constraint(
        "ck_ranking_evidence_kind",
        "ranking_evidence",
        type_="check",
    )
    op.create_check_constraint(
        "ck_ranking_evidence_kind",
        "ranking_evidence",
        _OLD_KINDS,
    )
