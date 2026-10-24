package com.nexusbench.nexpense.core.designsystem

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignSystemUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun galleryRendersInNormalFontScale() {
        composeTestRule.setContent {
            AppTheme {
                ComponentGalleryScreen()
            }
        }

        // Verify key text nodes are displayed
        composeTestRule.onNodeWithText("Component Gallery").assertIsDisplayed()
        composeTestRule.onNodeWithText("74.50").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Drink").assertIsDisplayed()
    }

    @Test
    fun galleryRendersInDarkTheme() {
        composeTestRule.setContent {
            AppTheme(darkTheme = true) {
                ComponentGalleryScreen()
            }
        }

        // Verify key text nodes are displayed in dark theme
        composeTestRule.onNodeWithText("Component Gallery").assertIsDisplayed()
        composeTestRule.onNodeWithText("74.50").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Drink").assertIsDisplayed()
    }


    @Test
    fun galleryRendersInLargeFontScale() {
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 2.0f,
                ),
            ) {
                AppTheme {
                    ComponentGalleryScreen()
                }
            }
        }

        // Verify key text nodes are displayed without crashing at 2.0 font scale
        composeTestRule.onNodeWithText("Component Gallery").assertIsDisplayed()
        composeTestRule.onNodeWithText("74.50").assertIsDisplayed()
    }

    @Test
    fun segmentedControlHandlesClicks() {
        composeTestRule.setContent {
            AppTheme {
                ComponentGalleryScreen()
            }
        }

        // Click "Day" segment
        composeTestRule.onNodeWithText("Day").performClick()
        composeTestRule.onNodeWithText("Day").assertIsDisplayed()

        // Click "Year" segment
        composeTestRule.onNodeWithText("Year").performClick()
        composeTestRule.onNodeWithText("Year").assertIsDisplayed()
    }
}
