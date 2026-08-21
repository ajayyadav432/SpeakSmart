package com.crispr.ai.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crispr.ai.theme.SpeakSmartTheme
import org.junit.Rule
import org.junit.Test

class MainScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_displaysHoldToSpeakButtonAndSections() {
        composeTestRule.setContent {
            SpeakSmartTheme {
                MainScreen()
            }
        }

        composeTestRule.onNodeWithText("Hold to Speak").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Speech").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Corrections").assertIsDisplayed()
    }
}
