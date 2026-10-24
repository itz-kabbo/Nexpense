package com.nexusbench.nexpense.feature.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for splash theme tokens.
 *
 * Light: background #FFFFFF, mark + wordmark #111111
 * Dark:  background #0B0B0C, mark + wordmark #F5F5F5
 * (off-white on purpose: pure #FFFFFF on near-black causes halation on OLED displays)
 */
@Immutable
data class SplashPalette(
    val background: Color,
    val content: Color,
    val isDark: Boolean,
) {
    companion object {
        val Light = SplashPalette(
            background = Color(0xFFFFFFFF),
            content = Color(0xFF111111),
            isDark = false,
        )

        val Dark = SplashPalette(
            background = Color(0xFF0B0B0C),
            content = Color(0xFFF5F5F5),
            isDark = true,
        )

        fun forTheme(isDark: Boolean): SplashPalette = if (isDark) Dark else Light
    }
}

/**
 * Single configuration object holding all splash timing parameters.
 * All constants are exposed to enable fine-tuning on real devices.
 */
object SplashTimings {
    /** Blank pause at start for continuity with system splash */
    const val holdBlankEnd: Long = 100L

    /** Draw-on of the "n" stroke (750ms duration) */
    const val drawOnStart: Long = 100L
    const val drawOnEnd: Long = 850L
    val drawOnDuration: Long get() = drawOnEnd - drawOnStart

    /** Dot drop animation into the arch counter (400ms duration) */
    const val dotDropStart: Long = 750L
    const val dotDropEnd: Long = 1150L
    val dotDropDuration: Long get() = dotDropEnd - dotDropStart

    /** Wordmark fade and rise reveal (450ms duration) */
    const val wordmarkStart: Long = 1050L
    const val wordmarkEnd: Long = 1500L
    val wordmarkDuration: Long get() = wordmarkEnd - wordmarkStart

    /** Stagger delay between each letter of "nexpense" */
    const val wordmarkLetterStagger: Long = 25L

    /** Complete logo hold duration (500ms duration) */
    const val holdEnd: Long = 2000L

    /** Minimum display time before exit may begin (2000ms from start) */
    const val minDisplayMs: Long = 2000L

    /** Exit fade and scale out duration (250ms duration) */
    const val exitDuration: Long = 250L

    /** Crossfade duration when device theme and chosen theme disagree (200ms) */
    const val themeCrossfadeMs: Long = 200L

    /** Hold duration for reduced-motion accessibility mode (600ms) */
    const val reducedMotionHoldMs: Long = 600L

    /** Transition fade duration for reduced-motion mode (200ms) */
    const val reducedMotionFadeMs: Long = 200L

    // Easing curves specified in design
    val drawOnEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val wordmarkEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
}

/**
 * Geometric brand vector definitions for the Nexpense mark and wordmark.
 */
object NexpenseBrandAssets {
    /** Target mark width on screen (~96dp) */
    val MarkWidth = 96.dp
    /** Gap between logo mark and wordmark (~20dp) */
    val MarkToWordmarkGap = 20.dp

    /** Reference coordinate space width */
    const val VIEWBOX_WIDTH = 46f
    /** Reference coordinate space height */
    const val VIEWBOX_HEIGHT = 54f
    /** Reference stroke thickness (10 in 46x54 space) */
    const val STROKE_WIDTH = 10f
    /** Reference dot radius (6.5 in 46x54 space) */
    const val DOT_RADIUS = 6.5f
    /** Reference dot center X */
    const val DOT_CENTER_X = 23f
    /** Reference dot center Y */
    const val DOT_CENTER_Y = 34f

    /**
     * SVG path data string for the complete continuous single-stroke "n" mark.
     * Draws from bottom of left stem up to the top, retraces to the branch point,
     * curves over the arch, and descends down the right stem.
     */
    const val N_STROKE_PATH_DATA =
        "M 5,49 L 5,5 L 5,23 C 5,13.06 13.06,5 23,5 C 32.94,5 41,13.06 41,23 L 41,49"

    /**
     * SVG path data string for the center dot.
     */
    const val DOT_PATH_DATA =
        "M 23,34 m -6.5,0 a 6.5,6.5 0 1,1 13,0 a 6.5,6.5 0 1,1 -13,0"

    /**
     * Creates a reusable Compose [Path] for the complete "n" stroke in reference coordinates.
     */
    fun createNStrokePath(): Path = Path().apply {
        moveTo(5f, 49f)
        lineTo(5f, 5f)
        lineTo(5f, 23f)
        cubicTo(5f, 13.058875f, 13.058875f, 5f, 23f, 5f)
        cubicTo(32.941125f, 5f, 41f, 13.058875f, 41f, 23f)
        lineTo(41f, 49f)
    }
}
