package com.wishtrace.app.domain

import java.time.Instant

/**
 * Yellow-tile autopilot state, mirroring the backend `MandateState` wire values.
 * `UNKNOWN` is the parse fallback so a new server state never crashes the client.
 */
enum class MandateStatus(val wire: String) {
    SETUP_CREATING("SETUP_CREATING"),
    AWAITING_APPROVAL("AWAITING_APPROVAL"),
    ACTIVE("ACTIVE"),
    CHARGING("CHARGING"),
    CHECKOUT_IN_PROGRESS("CHECKOUT_IN_PROGRESS"),
    REPORTING("REPORTING"),
    SUCCEEDED("SUCCEEDED"),
    DECLINED("DECLINED"),
    CONSUMED("CONSUMED"),
    PAUSED("PAUSED"),
    CANCELLED("CANCELLED"),
    EXPIRED("EXPIRED"),
    FAILED("FAILED"),
    UNKNOWN("UNKNOWN"),
    ;

    companion object {
        fun fromWire(value: String): MandateStatus =
            entries.firstOrNull { it.wire == value } ?: UNKNOWN
    }

    /** States where the owner still has an action to take in the approval UI. */
    val requiresApproval: Boolean
        get() = this == SETUP_CREATING || this == AWAITING_APPROVAL

    /** Terminal autopilot states — no further UI action will change them. */
    val isTerminal: Boolean
        get() = this == SUCCEEDED || this == DECLINED || this == CONSUMED ||
            this == CANCELLED || this == EXPIRED || this == FAILED
}

enum class MandateMerchantOutcome(val wire: String) {
    ORDER_VERIFIED("ORDER_VERIFIED"),
    DECLINED("DECLINED"),
    UNKNOWN("UNKNOWN"),
    ;

    companion object {
        fun fromWire(value: String): MandateMerchantOutcome =
            entries.firstOrNull { it.wire == value } ?: UNKNOWN
    }
}

enum class MandateVisaConfirmation(val wire: String) {
    SUCCESS("SUCCESS"),
    FAILURE("FAILURE"),
    ;

    companion object {
        fun fromWire(value: String): MandateVisaConfirmation? =
            entries.firstOrNull { it.wire == value }
    }
}

/** Owner-facing mandate view. Never carries card credentials. */
data class MandateDetails(
    val id: String,
    val recipientId: String,
    val occasionId: String,
    val status: MandateStatus,
    val approvedAmountMinor: Int,
    val currency: String,
    val recurringFrequency: String,
    val merchantScope: String,
    val maxCharges: Int,
    val chargesUsed: Int,
    val merchantName: String,
    val productTitle: String,
    val itemPriceMinor: Int,
    val approvalUrl: String?,
    val lastProviderStatus: String?,
    val setupFailureCode: String?,
    val merchantOrderId: String?,
    val merchantOutcome: MandateMerchantOutcome?,
    val visaConfirmation: MandateVisaConfirmation?,
    val lastChargeState: String?,
    val lastChargeAmountMinor: Int?,
    val lastChargeFailureCode: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isArmed: Boolean
        get() = status == MandateStatus.ACTIVE

    val hasApprovalPending: Boolean
        get() = status.requiresApproval
}
