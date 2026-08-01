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
    purchase_intents: Mapped[list["PurchaseIntentModel"]] = orm_relationship(
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
            "product_kind IN ('PHYSICAL', 'STORED_VALUE')",
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


class PravaSessionModel(Base):
    __tablename__ = "prava_sessions"
    __table_args__ = (
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
        CheckConstraint("operation = 'PRAVA_SESSION'", name="ck_idempotency_operation"),
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
