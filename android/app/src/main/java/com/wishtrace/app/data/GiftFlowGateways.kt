package com.wishtrace.app.data

import com.wishtrace.app.domain.DiscoveryPreparation
import com.wishtrace.app.domain.DiscoveryStage
import com.wishtrace.app.domain.BillingContact
import com.wishtrace.app.domain.GiftDiscoveryRequest
import com.wishtrace.app.domain.GroundedRankingRequest
import com.wishtrace.app.domain.PravaApprovalHandoff
import com.wishtrace.app.domain.PravaApprovalRequest
import com.wishtrace.app.domain.PurchaseIntentDetails
import com.wishtrace.app.domain.RankedDecision
import com.wishtrace.app.domain.VerifiedResult

/**
 * Android-facing boundaries for the WishTrace backend.
 *
 * Implementations must call the backend over HTTPS. OpenAI and Prava credentials never belong
 * in the Android client.
 */
interface GiftDiscoveryGateway {
    suspend fun prepareCandidates(
        request: GiftDiscoveryRequest,
        onStage: suspend (DiscoveryStage) -> Unit,
    ): DiscoveryPreparation
}

interface GroundedRankingGateway {
    suspend fun rankAllowedCandidates(
        request: GroundedRankingRequest,
    ): RankedDecision
}

interface PravaApprovalGateway {
    suspend fun createSandboxApproval(
        request: PravaApprovalRequest,
    ): PravaApprovalHandoff
}

interface PurchaseFlowGateway {
    suspend fun createIntent(candidateId: String): PurchaseIntentDetails

    suspend fun getIntent(purchaseIntentId: String): PurchaseIntentDetails

    suspend fun quote(
        purchaseIntentId: String,
        billing: BillingContact,
        idempotencyKey: String,
    ): PurchaseIntentDetails

    suspend fun createApprovalSession(
        purchaseIntentId: String,
        idempotencyKey: String,
    ): PurchaseIntentDetails

    suspend fun reconcile(purchaseIntentId: String): PurchaseIntentDetails

    suspend fun getVerifiedResult(purchaseIntentId: String): VerifiedResult

    suspend fun saveMessage(purchaseIntentId: String, text: String)
}
