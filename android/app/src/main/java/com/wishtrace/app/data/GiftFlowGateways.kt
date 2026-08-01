package com.wishtrace.app.data

import com.wishtrace.app.domain.DiscoveryPreparation
import com.wishtrace.app.domain.DiscoveryStage
import com.wishtrace.app.domain.GiftDiscoveryRequest
import com.wishtrace.app.domain.GroundedRankingRequest
import com.wishtrace.app.domain.PravaApprovalHandoff
import com.wishtrace.app.domain.PravaApprovalRequest
import com.wishtrace.app.domain.RankedDecision

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
