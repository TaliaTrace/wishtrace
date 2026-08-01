package com.wishtrace.app

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.wishtrace.app.ui.WishTraceTestTags
import org.junit.Rule
import org.junit.Test

class WishTraceFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun seededGoldEntryReachesRecipientAndDiscovery() {
        composeRule
            .onNodeWithTag(WishTraceTestTags.OnboardingScreen)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(WishTraceTestTags.ContinueFromWelcome)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        repeat(4) {
            composeRule
                .onNodeWithTag(WishTraceTestTags.ContinueFromWelcome)
                .performClick()
        }
        composeRule
            .onNodeWithText("Not now")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(WishTraceTestTags.HomeScreen)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNodeWithText("Find a gift for Sophie")
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(WishTraceTestTags.ReviewRecipient)
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithTag(WishTraceTestTags.RecipientScreen)
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Back")
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithTag(WishTraceTestTags.ProfileFindGift)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule
            .onNodeWithTag(WishTraceTestTags.DiscoveryScreen)
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Back")
            .assertHeightIsAtLeast(48.dp)

        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule
                .onAllNodesWithTag(WishTraceTestTags.ContinueDiscovery)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNodeWithTag(WishTraceTestTags.ContinueDiscovery)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule
            .onNodeWithTag(WishTraceTestTags.RecommendationScreen)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(WishTraceTestTags.WriteMessage)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule
            .onNodeWithTag(WishTraceTestTags.MessageScreen)
            .assertIsDisplayed()
    }

    @Test
    fun recipientContextCanBeReviewedAndSaved() {
        repeat(5) {
            composeRule
                .onNodeWithTag(WishTraceTestTags.ContinueFromWelcome)
                .performClick()
        }
        composeRule
            .onNodeWithText("Not now")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithTag(WishTraceTestTags.HomeScreen)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule
            .onNodeWithContentDescription("People", useUnmergedTree = true)
            .performClick()
        composeRule
            .onNodeWithText("Edit")
            .performClick()
        composeRule
            .onNodeWithTag(WishTraceTestTags.SetupPersonStep)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(WishTraceTestTags.SetupPrimaryAction)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule
            .onNodeWithTag(WishTraceTestTags.SetupOccasionStep)
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(WishTraceTestTags.SetupPrimaryAction)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("Sophie", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule
            .onNodeWithText("Sophie", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
