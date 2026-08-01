package com.wishtrace.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.wishtrace.app.ui.WishTraceTestTags
import org.junit.Rule
import org.junit.Test

class WishTraceFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboardingRequiresGoogleAuthentication() {
        composeRule
            .onNodeWithTag(WishTraceTestTags.OnboardingScreen)
            .assertIsDisplayed()
        repeat(5) {
            composeRule
                .onNodeWithTag(WishTraceTestTags.ContinueFromWelcome)
                .assertHeightIsAtLeast(48.dp)
                .performClick()
        }

        composeRule
            .onNodeWithContentDescription("Sign in with Google")
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
        composeRule
            .onAllNodesWithText("Not now")
            .assertCountEquals(0)
    }

    @Test
    fun signInBackReturnsToOnboarding() {
        repeat(5) {
            composeRule
                .onNodeWithTag(WishTraceTestTags.ContinueFromWelcome)
                .performClick()
        }
        composeRule
            .onNodeWithContentDescription("Back")
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule
            .onNodeWithTag(WishTraceTestTags.OnboardingScreen)
            .assertIsDisplayed()
    }
}
