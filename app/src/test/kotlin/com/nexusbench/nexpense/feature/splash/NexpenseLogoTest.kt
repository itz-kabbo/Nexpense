package com.nexusbench.nexpense.feature.splash

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.nexusbench.nexpense.data.repo.ThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NexpenseLogoTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `nexpense logo renders properly in light mode`() {
        composeTestRule.setContent {
            NexpenseLogo(
                drawProgress = 1.0f,
                dotTranslateY = 0.0f,
                dotScaleX = 1.0f,
                dotScaleY = 1.0f,
                dotAlpha = 1.0f,
                shadowAlpha = 0.08f,
                rippleProgress = 0.0f,
                penTipAlpha = 0.0f,
                letterAlphas = List(8) { 1.0f },
                letterTranslationsY = List(8) { 0.0f },
                contentColor = SplashPalette.Light.content,
                isDark = false,
            )
        }

        // Verify letter "x" in nexpense is present and displayed
        composeTestRule.onNodeWithText("x").assertIsDisplayed()
        composeTestRule.onNodeWithText("p").assertIsDisplayed()
    }

    @Test
    fun `nexpense logo renders properly in dark mode`() {
        composeTestRule.setContent {
            NexpenseLogo(
                drawProgress = 0.5f,
                dotTranslateY = -10.0f,
                dotScaleX = 1.0f,
                dotScaleY = 1.0f,
                dotAlpha = 1.0f,
                shadowAlpha = 0.0f,
                rippleProgress = 0.5f,
                penTipAlpha = 0.5f,
                letterAlphas = List(8) { 0.5f },
                letterTranslationsY = List(8) { 6.0f },
                contentColor = SplashPalette.Dark.content,
                isDark = true,
            )
        }

        composeTestRule.onNodeWithText("x").assertIsDisplayed()
    }

    @Test
    fun `nexpense splash reports first frame drawn`() {
        composeTestRule.mainClock.autoAdvance = false
        var firstFrameDrawn = false

        composeTestRule.setContent {
            NexpenseSplash(
                themeMode = ThemeMode.SYSTEM,
                isAppReady = true,
                onFirstFrameDrawn = { firstFrameDrawn = true },
                onSplashComplete = {},
            )
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        assertTrue("First frame must be reported to dismiss system splash", firstFrameDrawn)
    }

    @Test
    fun `nexpense splash completes and calls onSplashComplete when time advances`() {
        composeTestRule.mainClock.autoAdvance = false
        var splashComplete = false

        composeTestRule.setContent {
            NexpenseSplash(
                themeMode = ThemeMode.SYSTEM,
                isAppReady = true,
                onFirstFrameDrawn = {},
                onSplashComplete = { splashComplete = true },
            )
        }

        // Advance beyond minDisplayMs (2000ms) + exitDuration (250ms) + buffer
        composeTestRule.mainClock.advanceTimeBy(2500L)
        assertTrue("onSplashComplete must be invoked after animation timeline finishes", splashComplete)
    }
}
