package com.nexusbench.nexpense.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.nexusbench.nexpense.core.designsystem.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppRootTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun threeDotIndicator_rendersAccessibleDotsAndHandlesClicks() {
        var clickedPage = -1

        composeTestRule.setContent {
            val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
            AppTheme {
                ThreeDotIndicator(
                    pagerState = pagerState,
                    onDotClick = { page -> clickedPage = page },
                )
            }
        }

        // Verify TalkBack semantics for all 3 dots
        composeTestRule.onNodeWithContentDescription("Page 1").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Page 2").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Page 3").assertIsDisplayed()

        // Verify clicking dot triggers callback with 0-based page index
        composeTestRule.onNodeWithContentDescription("Page 2").performClick()
        assertEquals(1, clickedPage)

        composeTestRule.onNodeWithContentDescription("Page 3").performClick()
        assertEquals(2, clickedPage)
    }

    @Test
    fun twoPaneRoot_rendersPanesAndSwitchesTabs() {
        composeTestRule.setContent {
            AppTheme {
                TwoPaneRoot(
                    onNavigateToSettings = {},
                    recordPane = { androidx.compose.material3.Text("Record Pane Content") },
                    receiptPane = { androidx.compose.material3.Text("Receipt Pane Content") },
                    insightPane = { androidx.compose.material3.Text("Insight Pane Content") },
                )
            }
        }

        // Record pane is always visible on the left
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Record Pane Content")).assertIsDisplayed()

        // Tab switcher shows Receipt and Insight
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Receipt")).assertIsDisplayed()
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Insight")).assertIsDisplayed()

        // Initially Receipt tab is selected
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Receipt Pane Content")).assertIsDisplayed()

        // Switch to Insight tab
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Insight")).performClick()
        composeTestRule.waitForIdle()

        // Insight pane is now visible
        composeTestRule.onNode(androidx.compose.ui.test.hasText("Insight Pane Content")).assertIsDisplayed()
    }
}
