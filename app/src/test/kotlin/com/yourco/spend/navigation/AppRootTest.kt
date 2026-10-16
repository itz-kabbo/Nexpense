package com.yourco.spend.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.yourco.spend.core.designsystem.AppTheme
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
}
