package com.example.speaksmart.ui.main

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.speaksmart.theme.SpeakSmartTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testHoldToSpeakButton_isDisplayed() {
        composeTestRule.setContent {
            SpeakSmartTheme {
                MainScreen()
            }
        }

        // Verify Hold to Speak button and text exist in hierarchy
        composeTestRule.onNodeWithText("Hold to Speak", substring = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Hold to speak", substring = true).assertExists()
    }

    @Test
    fun testSpeechAndCorrectionsSections_areDisplayed() {
        composeTestRule.setContent {
            SpeakSmartTheme {
                MainScreen()
            }
        }

        // Verify 'Your Speech' and 'AI Corrections' section titles exist in layout
        composeTestRule.onNodeWithText("Your Speech", substring = true).assertExists()
        composeTestRule.onNodeWithText("AI Corrections", substring = true).assertExists()
    }
}
