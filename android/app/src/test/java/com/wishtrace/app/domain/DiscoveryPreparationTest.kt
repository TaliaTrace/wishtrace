package com.wishtrace.app.domain

import org.junit.Assert.assertThrows
import org.junit.Test

class DiscoveryPreparationTest {
    @Test
    fun rejectsDuplicateCandidateIdsBeforeRanking() {
        assertThrows(IllegalArgumentException::class.java) {
            DiscoveryPreparation(
                eligibleCandidateIds = listOf("candidate_a", "candidate_a"),
                sourceMode = SourceMode.LIVE,
            )
        }
    }
}
