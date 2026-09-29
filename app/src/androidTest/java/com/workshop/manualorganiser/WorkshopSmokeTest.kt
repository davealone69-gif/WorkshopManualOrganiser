package com.workshop.manualorganiser

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntil
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkshopSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun criticalNavigationAndCreateFlowWorksOnDevice() {
        compose.onNodeWithText("Workshop Manual Organiser").assertIsDisplayed()

        // Home loads its persistent library asynchronously. Wait for the real FAB
        // rather than racing the first composition.
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText("Add Manual").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Add Manual").performClick()

        compose.onNodeWithText("Title").performTextInput("CI smoke manual")
        compose.onNodeWithText("Save").performClick()

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("CI smoke manual").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("CI smoke manual").assertIsDisplayed()

        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("VIN Decoder").performClick()
        compose.onNodeWithText("VIN Decoder").assertIsDisplayed()
    }
}
