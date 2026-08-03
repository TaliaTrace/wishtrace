package com.wishtrace.app.ui.screens.giftdna

import com.wishtrace.app.data.OccasionInput
import com.wishtrace.app.data.OccasionRepository
import com.wishtrace.app.data.PeopleRepository
import com.wishtrace.app.data.RecipientInput
import com.wishtrace.app.domain.Occasion
import com.wishtrace.app.domain.Recipient
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftDnaViewModelTest {
    private val viewModel = GiftDnaViewModel(
        peopleRepository = UnusedPeopleRepository,
        occasionRepository = UnusedOccasionRepository,
        today = LocalDate.of(2026, 8, 3),
    )

    @Test
    fun interestBentoAllowsThreeIndependentSignalsAndCanReplaceOne() {
        viewModel.toggleInterest(InterestChoice.GAMING)
        viewModel.toggleInterest(InterestChoice.FITNESS)
        viewModel.toggleInterest(InterestChoice.MUSIC)
        viewModel.toggleInterest(InterestChoice.BOOKS)

        assertEquals(
            setOf(InterestChoice.GAMING, InterestChoice.FITNESS, InterestChoice.MUSIC),
            viewModel.state.value.interests,
        )

        viewModel.toggleInterest(InterestChoice.MUSIC)
        viewModel.toggleInterest(InterestChoice.BOOKS)

        assertEquals(
            setOf(InterestChoice.GAMING, InterestChoice.FITNESS, InterestChoice.BOOKS),
            viewModel.state.value.interests,
        )
    }

    @Test
    fun redAndBlueChaptersStillGateMissingRequiredContext() {
        viewModel.advance()

        assertEquals(GiftDnaTile.RED, viewModel.state.value.tile)
        assertTrue(viewModel.state.value.nameError != null)
        assertTrue(viewModel.state.value.relationshipError != null)

        viewModel.updateName("Zaid")
        viewModel.selectRelationship(RelationshipChoice.FAMILY)
        viewModel.advance()
        assertEquals(GiftDnaTile.BLUE, viewModel.state.value.tile)

        viewModel.advance()
        assertEquals(GiftDnaTile.BLUE, viewModel.state.value.tile)
        assertFalse(viewModel.state.value.dateError.isNullOrBlank())
    }

    private object UnusedPeopleRepository : PeopleRepository {
        override suspend fun listRecipients(): List<Recipient> = emptyList()

        override suspend fun saveRecipient(input: RecipientInput): Recipient =
            error("Save is not exercised by this test")
    }

    private object UnusedOccasionRepository : OccasionRepository {
        override suspend fun listOccasions(recipientId: String?): List<Occasion> = emptyList()

        override suspend fun saveOccasion(input: OccasionInput): Occasion =
            error("Save is not exercised by this test")
    }
}
