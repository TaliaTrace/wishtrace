package com.wishtrace.app.data

import com.wishtrace.app.domain.DiscoveryStage
import com.wishtrace.app.domain.GiftDiscoveryRequest
import com.wishtrace.app.domain.Money
import com.wishtrace.app.domain.SourceMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SeededGiftDiscoveryGatewayTest {
    @Test
    fun emitsHardConstraintStagesInDeterministicOrder() = runBlocking {
        val observedStages = mutableListOf<DiscoveryStage>()
        val result = SeededGiftDiscoveryGateway(stageDelayMillis = 0)
            .prepareCandidates(
                request = GiftDiscoveryRequest(
                    recipientId = "recipient_sophie",
                    occasionId = "occasion_sophie_birthday",
                    budget = Money(minorUnits = 6_000, currencyCode = "USD"),
                ),
                onStage = observedStages::add,
            )

        assertEquals(DiscoveryStage.entries, observedStages)
        assertEquals(SourceMode.CONTROLLED, result.sourceMode)
        assertEquals(2, result.eligibleCandidateIds.size)
    }
}
