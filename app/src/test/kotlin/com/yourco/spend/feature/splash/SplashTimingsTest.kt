package com.yourco.spend.feature.splash

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SplashTimingsTest {

    @Test
    fun `verify splash timings match specification exactly`() {
        assertEquals(100L, SplashTimings.holdBlankEnd)

        // Draw-on: 100ms - 850ms (750ms)
        assertEquals(100L, SplashTimings.drawOnStart)
        assertEquals(850L, SplashTimings.drawOnEnd)
        assertEquals(750L, SplashTimings.drawOnDuration)

        // Dot drop: 750ms - 1150ms (400ms)
        assertEquals(750L, SplashTimings.dotDropStart)
        assertEquals(1150L, SplashTimings.dotDropEnd)
        assertEquals(400L, SplashTimings.dotDropDuration)

        // Wordmark reveal: 1050ms - 1500ms (450ms)
        assertEquals(1050L, SplashTimings.wordmarkStart)
        assertEquals(1500L, SplashTimings.wordmarkEnd)
        assertEquals(450L, SplashTimings.wordmarkDuration)
        assertEquals(25L, SplashTimings.wordmarkLetterStagger)

        // Hold: 2000ms
        assertEquals(2000L, SplashTimings.holdEnd)
        assertEquals(2000L, SplashTimings.minDisplayMs)

        // Exit: 250ms
        assertEquals(250L, SplashTimings.exitDuration)

        // Theme crossfade on mismatch: 200ms
        assertEquals(200L, SplashTimings.themeCrossfadeMs)

        // Reduced motion
        assertEquals(600L, SplashTimings.reducedMotionHoldMs)
        assertEquals(200L, SplashTimings.reducedMotionFadeMs)
    }

    @Test
    fun `verify theme palette tokens adhere to specification`() {
        // LIGHT: background #FFFFFF, mark + wordmark #111111
        val light = SplashPalette.Light
        assertEquals(Color(0xFFFFFFFF), light.background)
        assertEquals(Color(0xFF111111), light.content)
        assertFalse(light.isDark)

        // DARK: background #0B0B0C, mark + wordmark #F5F5F5
        val dark = SplashPalette.Dark
        assertEquals(Color(0xFF0B0B0C), dark.background)
        assertEquals(Color(0xFFF5F5F5), dark.content)
        assertTrue(dark.isDark)

        // Helper resolution
        assertEquals(light, SplashPalette.forTheme(isDark = false))
        assertEquals(dark, SplashPalette.forTheme(isDark = true))
    }

    @Test
    fun `verify brand vector path assets are well-formed`() {
        assertEquals(46f, NexpenseBrandAssets.VIEWBOX_WIDTH)
        assertEquals(54f, NexpenseBrandAssets.VIEWBOX_HEIGHT)
        assertEquals(10f, NexpenseBrandAssets.STROKE_WIDTH)
        assertEquals(6.5f, NexpenseBrandAssets.DOT_RADIUS)
        assertEquals(23f, NexpenseBrandAssets.DOT_CENTER_X)
        assertEquals(34f, NexpenseBrandAssets.DOT_CENTER_Y)

        assertTrue(NexpenseBrandAssets.N_STROKE_PATH_DATA.startsWith("M 5,49"))
        assertTrue(NexpenseBrandAssets.N_STROKE_PATH_DATA.contains("L 5,5"))
        assertTrue(NexpenseBrandAssets.DOT_PATH_DATA.contains("6.5"))

        val path = NexpenseBrandAssets.createNStrokePath()
        assertNotNull(path)
    }
}
