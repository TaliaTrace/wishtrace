import uuid
from datetime import date, datetime

from sqlalchemy import (
    JSON,
    BigInteger,
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    LargeBinary,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.orm import relationship as orm_relationship


class Base(DeclarativeBase):
    pass


class UserModel(Base):
    __tablename__ = "users"
    __table_args__ = (
        UniqueConstraint("google_subject", name="users_google_subject_key"),
        Index("ix_users_google_subject", "google_subject", unique=True),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    google_subject: Mapped[str] = mapped_column(String(255))
    email: Mapped[str | None] = mapped_column(String(320))
    email_verified: Mapped[bool] = mapped_column(Boolean, default=False)
    display_name: Mapped[str] = mapped_column(String(200))
    picture_url: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    sessions: Mapped[list["AppSessionModel"]] = orm_relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    recipients: Mapped[list["RecipientModel"]] = orm_relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    occasions: Mapped[list["OccasionModel"]] = orm_relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    discovery_runs: Mapped[list["DiscoveryRunModel"]] = orm_relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    ranking_runs: Mapped[list["RankingRunModel"]] = orm_relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    purchase_intents: Mapped[list["PurchaseIntentModel"]] = orm_relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )
    mandates: Mapped[list["MandateModel"]] = orm_relationship(
        back_populates="user",
        cascade="all, delete-orphan",
    )


class AuthChallengeModel(Base):
    __tablename__ = "auth_challenges"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    nonce_hash: Mapped[bytes] = mapped_column(LargeBinary(32))
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
    consumed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )


class AppSessionModel(Base):
    __tablename__ = "app_sessions"
    __table_args__ = (
        UniqueConstraint("token_hash", name="app_sessions_token_hash_key"),
        Index("ix_app_sessions_token_hash", "token_hash", unique=True),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    token_hash: Mapped[bytes] = mapped_column(LargeBinary(32))
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    user: Mapped[UserModel] = orm_relationship(back_populates="sessions")


class RecipientModel(Base):
    __tablename__ = "recipients"
    __table_args__ = (UniqueConstraint("user_id", name="uq_recipients_user_id"),)

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    display_name: Mapped[str] = mapped_column(String(100))
    relationship: Mapped[str] = mapped_column(String(100))
    personality_traits: Mapped[dict[str, str] | None] = mapped_column(JSON)
    age_band: Mapped[str | None] = mapped_column(String(16))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    user: Mapped[UserModel] = orm_relationship(back_populates="recipients")
    preferences: Mapped[list["RecipientPreferenceModel"]] = orm_relationship(
        back_populates="recipient",
        cascade="all, delete-orphan",
    )
    hints: Mapped[list["HintModel"]] = orm_relationship(
        back_populates="recipient",
        cascade="all, delete-orphan",
    )
    occasions: Mapped[list["OccasionModel"]] = orm_relationship(
        back_populates="recipient",
        cascade="all, delete-orphan",
    )
    discovery_runs: Mapped[list["DiscoveryRunModel"]] = orm_relationship(
        back_populates="recipient",
        cascade="all, delete-orphan",
    )
    purchase_intents: Mapped[list["PurchaseIntentModel"]] = orm_relationship(
        back_populates="recipient",
        cascade="all, delete-orphan",
    )


class RecipientPreferenceModel(Base):
    __tablename__ = "recipient_preferences"
    __table_args__ = (
        CheckConstraint("kind IN ('INTEREST', 'DISLIKE')", name="ck_preference_kind"),
        UniqueConstraint("recipient_id", "kind", "value", name="uq_preference_value"),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    recipient_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("recipients.id", ondelete="CASCADE"), index=True
    )
    kind: Mapped[str] = mapped_column(String(16))
    value: Mapped[str] = mapped_column(String(100))
    position: Mapped[int] = mapped_column(Integer)

    recipient: Mapped[RecipientModel] = orm_relationship(back_populates="preferences")


class HintModel(Base):
    __tablename__ = "hints"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    recipient_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("recipients.id", ondelete="CASCADE"), index=True
    )
    text: Mapped[str] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    recipient: Mapped[RecipientModel] = orm_relationship(back_populates="hints")


class OccasionModel(Base):
    __tablename__ = "occasions"
    __table_args__ = (
        CheckConstraint("kind = 'BIRTHDAY'", name="ck_occasion_kind"),
        CheckConstraint("budget_minor > 0", name="ck_occasion_budget_positive"),
        CheckConstraint("currency = 'USD'", name="ck_occasion_currency_usd"),
        CheckConstraint(
            "recurring_frequency IN ('one_time', 'yearly')",
            name="ck_occasion_recurring_frequency",
        ),
        UniqueConstraint("recipient_id", "kind", name="uq_recipient_occasion_kind"),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    recipient_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("recipients.id", ondelete="CASCADE"), index=True
    )
    kind: Mapped[str] = mapped_column(String(32))
    local_date: Mapped[date] = mapped_column(Date)
    time_zone: Mapped[str] = mapped_column(String(64))
    budget_minor: Mapped[int] = mapped_column(BigInteger)
    currency: Mapped[str] = mapped_column(String(3))
    recurring_frequency: Mapped[str] = mapped_column(
        String(16), server_default="one_time"
    )
    required_arrival_date: Mapped[date | None] = mapped_column(Date)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    user: Mapped[UserModel] = orm_relationship(back_populates="occasions")
    recipient: Mapped[RecipientModel] = orm_relationship(back_populates="occasions")
    discovery_runs: Mapped[list["DiscoveryRunModel"]] = orm_relationship(
        back_populates="occasion",
        cascade="all, delete-orphan",
    )
    purchase_intents: Mapped[list["PurchaseIntentModel"]] = orm_relationship(
        back_populates="occasion",
        cascade="all, delete-orphan",
    )
    mandates: Mapped[list["MandateModel"]] = orm_relationship(
        back_populates="occasion",
        cascade="all, delete-orphan",
    )


class DiscoveryRunModel(Base):
    __tablename__ = "discovery_runs"
    __table_args__ = (
        CheckConstraint("status = 'COMPLETED'", name="ck_discovery_status"),
        CheckConstraint("currency = 'USD'", name="ck_discovery_currency_usd"),
        CheckConstraint("budget_minor > 0", name="ck_discovery_budget_positive"),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    recipient_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("recipients.id", ondelete="CASCADE"), index=True
    )
    occasion_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("occasions.id", ondelete="CASCADE"), index=True
    )
    status: Mapped[str] = mapped_column(String(16))
    merchant_id: Mapped[str] = mapped_column(String(100))
    merchant_name: Mapped[str] = mapped_column(String(200))
    search_query: Mapped[str] = mapped_column(String(200))
    budget_minor: Mapped[int] = mapped_column(BigInteger)
    currency: Mapped[str] = mapped_column(String(3))
    source_request_id: Mapped[str | None] = mapped_column(String(255))
    profile_cache_compliant: Mapped[bool] = mapped_column(Boolean)
    source_timestamp: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    user: Mapped[UserModel] = orm_relationship(back_populates="discovery_runs")
    recipient: Mapped[RecipientModel] = orm_relationship(back_populates="discovery_runs")
    occasion: Mapped[OccasionModel] = orm_relationship(back_populates="discovery_runs")
    candidates: Mapped[list["CandidateSnapshotModel"]] = orm_relationship(
        back_populates="discovery_run",
        cascade="all, delete-orphan",
    )
    ranking_run: Mapped["RankingRunModel | None"] = orm_relationship(
        back_populates="discovery_run",
        cascade="all, delete-orphan",
        uselist=False,
    )


class CandidateSnapshotModel(Base):
    __tablename__ = "candidate_snapshots"
    __table_args__ = (
        CheckConstraint("price_minor >= 0", name="ck_candidate_price_non_negative"),
        CheckConstraint("currency = 'USD'", name="ck_candidate_currency_usd"),
        CheckConstraint(
            "availability IN ('AVAILABLE', 'UNAVAILABLE', 'UNKNOWN')",
            name="ck_candidate_availability",
        ),
        CheckConstraint(
            "product_kind IN ('PHYSICAL', 'DIGITAL', 'STORED_VALUE')",
            name="ck_candidate_product_kind",
        ),
        CheckConstraint("delivery_state = 'UNKNOWN'", name="ck_candidate_delivery"),
        CheckConstraint("source_mode = 'LIVE'", name="ck_candidate_source_mode"),
        UniqueConstraint(
            "discovery_run_id",
            "source_key",
            name="uq_candidate_run_source_key",
        ),
        UniqueConstraint(
            "discovery_run_id",
            "position",
            name="uq_candidate_run_position",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    discovery_run_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("discovery_runs.id", ondelete="CASCADE"), index=True
    )
    position: Mapped[int] = mapped_column(Integer)
    source_key: Mapped[str] = mapped_column(String(64))
    merchant_product_id: Mapped[str] = mapped_column(String(255))
    merchant_variant_id: Mapped[str | None] = mapped_column(String(255))
    sku: Mapped[str | None] = mapped_column(String(255))
    title: Mapped[str] = mapped_column(String(500))
    variant_title: Mapped[str | None] = mapped_column(String(500))
    description: Mapped[str | None] = mapped_column(Text)
    product_url: Mapped[str] = mapped_column(Text)
    image_url: Mapped[str | None] = mapped_column(Text)
    price_minor: Mapped[int] = mapped_column(BigInteger)
    currency: Mapped[str] = mapped_column(String(3))
    availability: Mapped[str] = mapped_column(String(16))
    selected_options: Mapped[dict[str, str]] = mapped_column(JSON)
    categories: Mapped[list[str]] = mapped_column(JSON)
    tags: Mapped[list[str]] = mapped_column(JSON)
    product_kind: Mapped[str] = mapped_column(String(16))
    checkout_supported: Mapped[bool] = mapped_column(Boolean)
    delivery_state: Mapped[str] = mapped_column(String(16))
    source_timestamp: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    source_mode: Mapped[str] = mapped_column(String(8))
    eligible: Mapped[bool] = mapped_column(Boolean)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    discovery_run: Mapped[DiscoveryRunModel] = orm_relationship(back_populates="candidates")
    rejection: Mapped["CandidateRejectionModel | None"] = orm_relationship(
        back_populates="candidate",
        cascade="all, delete-orphan",
        uselist=False,
    )
    purchase_intents: Mapped[list["PurchaseIntentModel"]] = orm_relationship(
        back_populates="candidate_snapshot",
        cascade="all, delete-orphan",
    )
    ranking_items: Mapped[list["RankingItemModel"]] = orm_relationship(
        back_populates="candidate_snapshot",
        cascade="all, delete-orphan",
    )


class CandidateRejectionModel(Base):
    __tablename__ = "candidate_rejections"
    __table_args__ = (
        UniqueConstraint(
            "candidate_snapshot_id",
            name="candidate_rejections_candidate_snapshot_id_key",
        ),
        Index(
            "ix_candidate_rejections_candidate_snapshot_id",
            "candidate_snapshot_id",
            unique=True,
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    candidate_snapshot_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("candidate_snapshots.id", ondelete="CASCADE"),
    )
    code: Mapped[str] = mapped_column(String(32))
    reason: Mapped[str] = mapped_column(String(500))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    candidate: Mapped[CandidateSnapshotModel] = orm_relationship(back_populates="rejection")


class RankingRunModel(Base):
    __tablename__ = "ranking_runs"
    __table_args__ = (
        CheckConstraint(
            "status IN ('IN_PROGRESS', 'COMPLETED', 'USER_CHOICE_REQUIRED')",
            name="ck_ranking_status",
        ),
        CheckConstraint(
            "mode IS NULL OR mode IN ('MODEL', 'DETERMINISTIC')",
            name="ck_ranking_mode",
        ),
        CheckConstraint(
            "uncertainty IS NULL OR uncertainty IN ('LOW', 'MEDIUM', 'HIGH')",
            name="ck_ranking_uncertainty",
        ),
        CheckConstraint(
            "(status = 'IN_PROGRESS' AND mode IS NULL AND uncertainty IS NULL "
            "AND completed_at IS NULL) OR "
            "(status = 'COMPLETED' AND mode IS NOT NULL AND uncertainty IS NOT NULL "
            "AND completed_at IS NOT NULL) OR "
            "(status = 'USER_CHOICE_REQUIRED' AND mode IS NULL AND uncertainty IS NULL "
            "AND completed_at IS NOT NULL)",
            name="ck_ranking_state_shape",
        ),
        CheckConstraint(
            "duration_ms IS NULL OR duration_ms >= 0",
            name="ck_ranking_duration_non_negative",
        ),
        CheckConstraint("attempt_count >= 1", name="ck_ranking_attempt_positive"),
        UniqueConstraint("discovery_run_id", name="uq_ranking_discovery_run"),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    discovery_run_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("discovery_runs.id", ondelete="CASCADE")
    )
    status: Mapped[str] = mapped_column(String(32))
    mode: Mapped[str | None] = mapped_column(String(16))
    uncertainty: Mapped[str | None] = mapped_column(String(16))
    provider_request_id: Mapped[str | None] = mapped_column(String(255))
    provider_deployment: Mapped[str | None] = mapped_column(String(200))
    prompt_version: Mapped[str] = mapped_column(String(32))
    schema_version: Mapped[str] = mapped_column(String(32))
    duration_ms: Mapped[int | None] = mapped_column(Integer)
    attempt_count: Mapped[int] = mapped_column(Integer, default=1)
    failure_category: Mapped[str | None] = mapped_column(String(100))
    started_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    user: Mapped[UserModel] = orm_relationship(back_populates="ranking_runs")
    discovery_run: Mapped[DiscoveryRunModel] = orm_relationship(
        back_populates="ranking_run"
    )
    items: Mapped[list["RankingItemModel"]] = orm_relationship(
        back_populates="ranking_run",
        cascade="all, delete-orphan",
    )
    evidence: Mapped[list["RankingEvidenceModel"]] = orm_relationship(
        back_populates="ranking_run",
        cascade="all, delete-orphan",
    )


class RankingEvidenceModel(Base):
    __tablename__ = "ranking_evidence"
    __table_args__ = (
        CheckConstraint(
            "kind IN ('INTEREST', 'HINT', 'RELATIONSHIP', 'OCCASION', "
            "'PERSONALITY', 'AGE')",
            name="ck_ranking_evidence_kind",
        ),
        CheckConstraint("position >= 0", name="ck_ranking_evidence_position"),
        UniqueConstraint(
            "ranking_run_id",
            "evidence_id",
            name="uq_ranking_evidence_id",
        ),
        UniqueConstraint(
            "ranking_run_id",
            "source_ref",
            name="uq_ranking_evidence_source",
        ),
        UniqueConstraint(
            "ranking_run_id",
            "position",
            name="uq_ranking_evidence_position",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    ranking_run_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("ranking_runs.id", ondelete="CASCADE"), index=True
    )
    evidence_id: Mapped[str] = mapped_column(String(48))
    source_ref: Mapped[str] = mapped_column(String(100))
    position: Mapped[int] = mapped_column(Integer)
    kind: Mapped[str] = mapped_column(String(16))
    value: Mapped[str] = mapped_column(String(500))

    ranking_run: Mapped[RankingRunModel] = orm_relationship(back_populates="evidence")


class RankingItemModel(Base):
    __tablename__ = "ranking_items"
    __table_args__ = (
        CheckConstraint("role IN ('SELECTED', 'ALTERNATIVE')", name="ck_ranking_item_role"),
        CheckConstraint("position >= 0 AND position <= 2", name="ck_ranking_item_position"),
        CheckConstraint(
            "(position = 0 AND role = 'SELECTED') OR "
            "(position > 0 AND role = 'ALTERNATIVE')",
            name="ck_ranking_item_role_position",
        ),
        UniqueConstraint(
            "ranking_run_id",
            "candidate_snapshot_id",
            name="uq_ranking_item_candidate",
        ),
        UniqueConstraint(
            "ranking_run_id",
            "position",
            name="uq_ranking_item_position",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    ranking_run_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("ranking_runs.id", ondelete="CASCADE"), index=True
    )
    candidate_snapshot_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("candidate_snapshots.id", ondelete="RESTRICT"), index=True
    )
    position: Mapped[int] = mapped_column(Integer)
    role: Mapped[str] = mapped_column(String(16))
    reason: Mapped[str] = mapped_column(String(240))
    evidence_ids: Mapped[list[str]] = mapped_column(JSON)

    ranking_run: Mapped[RankingRunModel] = orm_relationship(back_populates="items")
    candidate_snapshot: Mapped[CandidateSnapshotModel] = orm_relationship(
        back_populates="ranking_items"
    )


class PurchaseIntentModel(Base):
    __tablename__ = "purchase_intents"
    __table_args__ = (
        CheckConstraint(
            "state IN ('DRAFT', 'VALIDATING', 'QUOTED', 'READY_FOR_APPROVAL', "
            "'SESSION_CREATING', 'AWAITING_USER', 'CREDENTIALS_READY', "
            "'CHECKOUT_IN_PROGRESS', 'ORDER_VERIFIED', 'SUCCEEDED', 'DECLINED', "
            "'CANCELLED', 'EXPIRED', 'FAILED', 'UNKNOWN', 'RECONCILING')",
            name="ck_purchase_intent_state",
        ),
        CheckConstraint("currency = 'USD'", name="ck_purchase_intent_currency_usd"),
        CheckConstraint("item_price_minor >= 0", name="ck_purchase_item_price_non_negative"),
        CheckConstraint(
            "approved_total_minor IS NULL OR approved_total_minor > 0",
            name="ck_purchase_approved_total_positive",
        ),
        CheckConstraint(
            "merchant_outcome IS NULL OR merchant_outcome IN "
            "('ORDER_VERIFIED', 'DECLINED', 'UNKNOWN')",
            name="ck_purchase_merchant_outcome",
        ),
        UniqueConstraint(
            "user_id",
            "candidate_snapshot_id",
            name="uq_purchase_user_candidate",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    recipient_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("recipients.id", ondelete="CASCADE"), index=True
    )
    occasion_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("occasions.id", ondelete="CASCADE"), index=True
    )
    discovery_run_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("discovery_runs.id", ondelete="RESTRICT"), index=True
    )
    candidate_snapshot_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("candidate_snapshots.id", ondelete="RESTRICT"), index=True
    )
    state: Mapped[str] = mapped_column(String(32))
    merchant_id: Mapped[str] = mapped_column(String(100))
    merchant_name: Mapped[str] = mapped_column(String(200))
    merchant_url: Mapped[str] = mapped_column(Text)
    merchant_product_id: Mapped[str] = mapped_column(String(255))
    merchant_variant_id: Mapped[str] = mapped_column(String(255))
    sku: Mapped[str | None] = mapped_column(String(255))
    title: Mapped[str] = mapped_column(String(500))
    variant_title: Mapped[str | None] = mapped_column(String(500))
    item_price_minor: Mapped[int] = mapped_column(BigInteger)
    currency: Mapped[str] = mapped_column(String(3))
    approved_total_minor: Mapped[int | None] = mapped_column(BigInteger)
    quote_source: Mapped[str | None] = mapped_column(String(100))
    quote_timestamp: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    quote_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    delivery_summary: Mapped[str | None] = mapped_column(String(500))
    merchant_order_id: Mapped[str | None] = mapped_column(String(255))
    merchant_outcome: Mapped[str | None] = mapped_column(String(32))
    merchant_attempted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True)
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    user: Mapped[UserModel] = orm_relationship(back_populates="purchase_intents")
    recipient: Mapped[RecipientModel] = orm_relationship(back_populates="purchase_intents")
    occasion: Mapped[OccasionModel] = orm_relationship(back_populates="purchase_intents")
    candidate_snapshot: Mapped[CandidateSnapshotModel] = orm_relationship(
        back_populates="purchase_intents"
    )
    prava_session: Mapped["PravaSessionModel | None"] = orm_relationship(
        back_populates="purchase_intent",
        cascade="all, delete-orphan",
        uselist=False,
    )
    idempotency_operations: Mapped[list["IdempotencyOperationModel"]] = orm_relationship(
        back_populates="purchase_intent",
        cascade="all, delete-orphan",
    )
    transitions: Mapped[list["TransactionTransitionModel"]] = orm_relationship(
        back_populates="purchase_intent",
        cascade="all, delete-orphan",
    )


class PersonalMessageModel(Base):
    __tablename__ = "personal_messages"
    __table_args__ = (
        CheckConstraint(
            "origin IN ('USER', 'AZURE_OPENAI')",
            name="ck_personal_message_origin",
        ),
        UniqueConstraint(
            "purchase_intent_id",
            name="uq_personal_message_purchase_intent",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    purchase_intent_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("purchase_intents.id", ondelete="CASCADE"),
        index=True,
    )
    text: Mapped[str] = mapped_column(String(500))
    origin: Mapped[str] = mapped_column(String(16))
    edited: Mapped[bool] = mapped_column(Boolean)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class PravaSessionModel(Base):
    __tablename__ = "prava_sessions"
    __table_args__ = (
        CheckConstraint(
            "visa_confirmation IS NULL OR visa_confirmation IN ('SUCCESS', 'FAILURE')",
            name="ck_prava_visa_confirmation",
        ),
        UniqueConstraint(
            "purchase_intent_id",
            name="prava_sessions_purchase_intent_id_key",
        ),
        Index(
            "ix_prava_sessions_purchase_intent_id",
            "purchase_intent_id",
            unique=True,
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    purchase_intent_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("purchase_intents.id", ondelete="CASCADE"),
    )
    provider_session_id: Mapped[str] = mapped_column(String(255), unique=True)
    provider_order_id: Mapped[str] = mapped_column(String(255))
    hosted_url: Mapped[str] = mapped_column(Text)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    create_response_id: Mapped[str | None] = mapped_column(String(255))
    provider_transaction_id: Mapped[str | None] = mapped_column(String(255))
    provider_txn_ref_id: Mapped[str | None] = mapped_column(String(255))
    provider_status: Mapped[str | None] = mapped_column(String(32))
    last_response_id: Mapped[str | None] = mapped_column(String(255))
    report_response_id: Mapped[str | None] = mapped_column(String(255))
    visa_confirmation: Mapped[str | None] = mapped_column(String(16))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    purchase_intent: Mapped[PurchaseIntentModel] = orm_relationship(
        back_populates="prava_session"
    )


class IdempotencyOperationModel(Base):
    __tablename__ = "idempotency_operations"
    __table_args__ = (
        CheckConstraint(
            "operation IN ('MERCHANT_QUOTE', 'PRAVA_SESSION')",
            name="ck_idempotency_operation",
        ),
        CheckConstraint(
            "status IN ('IN_PROGRESS', 'COMPLETED', 'UNKNOWN', 'FAILED')",
            name="ck_idempotency_status",
        ),
        UniqueConstraint(
            "user_id",
            "operation",
            "key_hash",
            name="uq_idempotency_user_operation_key",
        ),
        UniqueConstraint(
            "purchase_intent_id",
            "operation",
            name="uq_idempotency_purchase_operation",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    purchase_intent_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("purchase_intents.id", ondelete="CASCADE"), index=True
    )
    operation: Mapped[str] = mapped_column(String(32))
    key_hash: Mapped[bytes] = mapped_column(LargeBinary(32))
    request_hash: Mapped[bytes] = mapped_column(LargeBinary(32))
    status: Mapped[str] = mapped_column(String(16))
    provider_response_id: Mapped[str | None] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    purchase_intent: Mapped[PurchaseIntentModel] = orm_relationship(
        back_populates="idempotency_operations"
    )


class TransactionTransitionModel(Base):
    __tablename__ = "transaction_transitions"

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    purchase_intent_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("purchase_intents.id", ondelete="CASCADE"), index=True
    )
    from_state: Mapped[str | None] = mapped_column(String(32))
    to_state: Mapped[str] = mapped_column(String(32))
    reason_code: Mapped[str] = mapped_column(String(100))
    provider_response_id: Mapped[str | None] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    purchase_intent: Mapped[PurchaseIntentModel] = orm_relationship(
        back_populates="transitions"
    )


class MandateModel(Base):
    """A standing spend authorization armed once per occasion.

    The owner approves this a single time with a passkey; WishTrace can then
    charge within the cap when a moment nears, without re-approval. Card
    credentials minted per charge are never persisted here — they stay in
    backend memory for the length of a single checkout only.
    """

    __tablename__ = "mandates"
    __table_args__ = (
        CheckConstraint(
            "state IN ('SETUP_CREATING', 'AWAITING_APPROVAL', 'ACTIVE', "
            "'CHARGING', 'CHECKOUT_IN_PROGRESS', 'REPORTING', 'SUCCEEDED', "
            "'DECLINED', 'CONSUMED', 'PAUSED', 'CANCELLED', 'EXPIRED', "
            "'FAILED', 'UNKNOWN')",
            name="ck_mandate_state",
        ),
        CheckConstraint("currency = 'USD'", name="ck_mandate_currency_usd"),
        CheckConstraint(
            "approved_amount_minor > 0", name="ck_mandate_amount_positive"
        ),
        CheckConstraint("max_charges >= 1", name="ck_mandate_max_charges"),
        CheckConstraint(
            "recurring_frequency IN ('one_time', 'weekly', 'monthly', 'yearly')",
            name="ck_mandate_recurring_frequency",
        ),
        CheckConstraint(
            "merchant_scope IN ('listed', 'any')", name="ck_mandate_merchant_scope"
        ),
        CheckConstraint(
            "visa_confirmation IS NULL OR visa_confirmation IN ('SUCCESS', 'FAILURE')",
            name="ck_mandate_visa_confirmation",
        ),
        CheckConstraint(
            "merchant_outcome IS NULL OR merchant_outcome IN "
            "('ORDER_VERIFIED', 'DECLINED', 'UNKNOWN')",
            name="ck_mandate_merchant_outcome",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    recipient_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("recipients.id", ondelete="CASCADE"), index=True
    )
    occasion_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("occasions.id", ondelete="CASCADE"), index=True
    )
    state: Mapped[str] = mapped_column(String(32))
    approved_amount_minor: Mapped[int] = mapped_column(BigInteger)
    currency: Mapped[str] = mapped_column(String(3))
    recurring_frequency: Mapped[str] = mapped_column(String(16))
    merchant_scope: Mapped[str] = mapped_column(String(16))
    max_charges: Mapped[int] = mapped_column(Integer)
    valid_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    merchant_id: Mapped[str] = mapped_column(String(100))
    merchant_name: Mapped[str] = mapped_column(String(200))
    merchant_url: Mapped[str] = mapped_column(Text)
    merchant_product_id: Mapped[str] = mapped_column(String(255))
    merchant_variant_id: Mapped[str] = mapped_column(String(255))
    product_title: Mapped[str] = mapped_column(String(500))
    item_price_minor: Mapped[int] = mapped_column(BigInteger)
    setup_session_id: Mapped[str | None] = mapped_column(String(255))
    setup_hosted_url: Mapped[str | None] = mapped_column(Text)
    setup_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    setup_response_id: Mapped[str | None] = mapped_column(String(255))
    provider_mandate_id: Mapped[str | None] = mapped_column(String(255), unique=True)
    provider_status: Mapped[str | None] = mapped_column(String(32))
    setup_failure_code: Mapped[str | None] = mapped_column(String(100))
    charges_used: Mapped[int] = mapped_column(Integer, server_default="0")
    merchant_order_id: Mapped[str | None] = mapped_column(String(255))
    merchant_outcome: Mapped[str | None] = mapped_column(String(32))
    visa_confirmation: Mapped[str | None] = mapped_column(String(16))
    last_response_id: Mapped[str | None] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    user: Mapped[UserModel] = orm_relationship(back_populates="mandates")
    recipient: Mapped[RecipientModel] = orm_relationship()
    occasion: Mapped[OccasionModel] = orm_relationship(back_populates="mandates")
    charges: Mapped[list["MandateChargeModel"]] = orm_relationship(
        back_populates="mandate",
        cascade="all, delete-orphan",
    )


class MandateChargeModel(Base):
    """An individual charge attempt against a mandate, for audit and idempotency.

    Deduplicated by ``reference`` so a retried execute never double-charges.
    """

    __tablename__ = "mandate_charges"
    __table_args__ = (
        CheckConstraint(
            "state IN ('CHARGING', 'CHECKOUT_IN_PROGRESS', 'REPORTING', "
            "'SUCCEEDED', 'DECLINED', 'FAILED', 'UNKNOWN')",
            name="ck_mandate_charge_state",
        ),
        CheckConstraint(
            "amount_minor > 0", name="ck_mandate_charge_amount_positive"
        ),
        CheckConstraint(
            "visa_confirmation IS NULL OR visa_confirmation IN ('SUCCESS', 'FAILURE')",
            name="ck_mandate_charge_visa_confirmation",
        ),
        CheckConstraint(
            "merchant_outcome IS NULL OR merchant_outcome IN "
            "('ORDER_VERIFIED', 'DECLINED', 'UNKNOWN')",
            name="ck_mandate_charge_merchant_outcome",
        ),
        UniqueConstraint("mandate_id", "reference", name="uq_mandate_charge_reference"),
    )

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    mandate_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("mandates.id", ondelete="CASCADE"), index=True
    )
    reference: Mapped[str] = mapped_column(String(255))
    amount_minor: Mapped[int] = mapped_column(BigInteger)
    state: Mapped[str] = mapped_column(String(32))
    provider_charge_id: Mapped[str | None] = mapped_column(String(255))
    provider_txn_ref_id: Mapped[str | None] = mapped_column(String(255))
    provider_error_code: Mapped[str | None] = mapped_column(String(100))
    merchant_order_id: Mapped[str | None] = mapped_column(String(255))
    merchant_outcome: Mapped[str | None] = mapped_column(String(32))
    visa_confirmation: Mapped[str | None] = mapped_column(String(16))
    charge_response_id: Mapped[str | None] = mapped_column(String(255))
    report_response_id: Mapped[str | None] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    mandate: Mapped[MandateModel] = orm_relationship(back_populates="charges")
