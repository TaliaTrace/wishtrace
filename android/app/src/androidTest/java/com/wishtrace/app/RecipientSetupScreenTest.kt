package com.wishtrace.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.wishtrace.app.ui.screens.setup.RecipientSetupScreen
import com.wishtrace.app.ui.screens.setup.RecipientSetupUiState
import com.wishtrace.app.ui.theme.WishTraceTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class RecipientSetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun personFieldsRemainVisibleWhenTheKeyboardOpens() {
        composeRule.setContent {
            WishTraceTheme {
                RecipientSetupScreen(
                    state = RecipientSetupUiState(),
                    title = "Add a person",
                    occasionOnly = false,
                    onBack = {},
                    onNameChange = {},
                    onRelationshipChange = {},
                    onDateChange = { _: LocalDate -> },
                    onInterestToggle = {},
                    onDislikesChange = {},
                    onBudgetChange = {},
                    onHintChange = {},
                    onContinue = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("Who are they?").assertIsDisplayed()
        composeRule.onNodeWithText("Name").assertIsDisplayed().performTextInput("Avery")
        composeRule.onNodeWithText("Relationship").assertIsDisplayed()
    }
}
