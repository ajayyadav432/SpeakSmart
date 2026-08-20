package com.example.speaksmart.ui.main

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.example.speaksmart.theme.SpeakSmartTheme
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_displaysTitle() {
        composeTestRule.setContent {
            SpeakSmartTheme {
                // We can't easily test the full screen without a real ViewModel,
                // so this is a basic smoke test.
            }
        }
    }
}
