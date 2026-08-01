package com.wishtrace.app.data

import com.wishtrace.app.domain.DiscoveryPreparation
import com.wishtrace.app.domain.DiscoveryStage
import com.wishtrace.app.domain.GiftDiscoveryRequest
import com.wishtrace.app.domain.SourceMode
import kotlinx.coroutines.delay

class SeededGiftDiscoveryGateway(
    private val stageDelayMillis: Long = 520,
) : GiftDiscoveryGateway {
    override suspend fun prepareCandidates(
        request: GiftDiscoveryRequest,
        onStage: suspend (DiscoveryStage) -> Unit,
    ): DiscoveryPreparation {
        require(request.recipientId.isNotBlank())
        require(request.occasionId.isNotBlank())
        require(request.budget.minorUnits > 0)

        DiscoveryStage.entries.forEach { stage ->
            onStage(stage)
            delay(stageDelayMillis)
        }

        return DiscoveryPreparation(
            eligibleCandidateIds = listOf(
                "controlled_candidate_alpha",
                "controlled_candidate_bravo",
            ),
            sourceMode = SourceMode.CONTROLLED,
        )
    }
}
