package com.yourco.spend.feature.splash

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.yourco.spend.data.repo.ThemeMode
import com.yourco.spend.data.repo.resolveIsDark
import kotlin.math.PI
import kotlin.math.sin

/**
 * Stage 2: Animated In-App Splash Composable.
 *
 * Pacing: Total ≈ 2.25s (cold start only).
 * Hand-off: Starts with the device theme background (seamless with Stage 1 system splash).
 * Mismatch crossfade: If user chose a theme differing from device theme, crossfades
 * background and content over 200ms and shifts animation timeline by 200ms.
 * Minimum display: Logo animation and hold guaranteed to play for at least 2000ms.
 * Reduced motion: Honors system animator duration scale = 0 by showing static logo with brief fade.
 */
@Composable
fun NexpenseSplash(
    themeMode: ThemeMode,
    isAppReady: Boolean,
    onFirstFrameDrawn: () -> Unit,
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val systemIsDark = isSystemInDarkTheme()
    val chosenIsDark = remember(themeMode, systemIsDark) {
        resolveIsDark(themeMode, systemIsDark)
    }

    val hasThemeMismatch = remember(systemIsDark, chosenIsDark) {
        systemIsDark != chosenIsDark
    }

    val timelineShiftMs = if (hasThemeMismatch) SplashTimings.themeCrossfadeMs else 0L

    // Palette tokens
    val devicePalette = remember(systemIsDark) { SplashPalette.forTheme(systemIsDark) }
    val chosenPalette = remember(chosenIsDark) { SplashPalette.forTheme(chosenIsDark) }

    // Check accessibility reduced-motion preference
    val isReducedMotion = remember {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f,
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    // Animation state variables
    var elapsedTotalMs by remember { mutableLongStateOf(0L) }
    var exitStartTime by remember { mutableLongStateOf(-1L) }
    var isExitActive by remember { mutableStateOf(false) }

    val currentOnFirstFrameDrawn by rememberUpdatedState(onFirstFrameDrawn)
    val currentOnSplashComplete by rememberUpdatedState(onSplashComplete)
    val currentIsAppReady by rememberUpdatedState(isAppReady)

    LaunchedEffect(isReducedMotion) {
        var startFrameTime = -1L
        var firstFrameReported = false

        while (true) {
            var shouldExitLoop = false
            withFrameMillis { frameTimeMs ->
                if (startFrameTime == -1L) {
                    startFrameTime = frameTimeMs
                }
                if (!firstFrameReported) {
                    firstFrameReported = true
                    currentOnFirstFrameDrawn()
                }

                val elapsed = frameTimeMs - startFrameTime
                elapsedTotalMs = elapsed

                if (isReducedMotion) {
                    if (elapsed >= SplashTimings.reducedMotionHoldMs + SplashTimings.reducedMotionFadeMs) {
                        currentOnSplashComplete()
                        shouldExitLoop = true
                    }
                } else {
                    // Check if ready to begin exit (at t >= 2000ms + shift, once app is ready or grace period)
                    val minDisplayThreshold = SplashTimings.minDisplayMs + timelineShiftMs
                    val canExit = elapsed >= minDisplayThreshold && (currentIsAppReady || elapsed >= minDisplayThreshold + 800L)
                    if (canExit && !isExitActive) {
                        isExitActive = true
                        exitStartTime = elapsed
                    }

                    if (isExitActive) {
                        val exitElapsed = elapsed - exitStartTime
                        if (exitElapsed >= SplashTimings.exitDuration) {
                            currentOnSplashComplete()
                            shouldExitLoop = true
                        }
                    }
                }
            }
            if (shouldExitLoop) {
                break
            }
        }
    }

    // --- COLOR INTERPOLATION ---
    val backgroundColor: Color
    val contentColor: Color

    if (hasThemeMismatch) {
        val crossfadeP = (elapsedTotalMs.toFloat() / SplashTimings.themeCrossfadeMs).coerceIn(0f, 1f)
        val easedCrossfade = FastOutSlowInEasing.transform(crossfadeP)
        backgroundColor = lerp(devicePalette.background, chosenPalette.background, easedCrossfade)
        contentColor = lerp(devicePalette.content, chosenPalette.content, easedCrossfade)
    } else {
        backgroundColor = chosenPalette.background
        contentColor = chosenPalette.content
    }

    // --- ANIMATION VALUES COMPUTATION ---
    val drawProgress: Float
    val dotTranslateY: Float
    val dotScaleX: Float
    val dotScaleY: Float
    val dotAlpha: Float
    val shadowAlpha: Float
    val rippleProgress: Float
    val penTipAlpha: Float
    val letterAlphas: List<Float>
    val letterTranslationsY: List<Float>
    val containerAlpha: Float
    val containerScale: Float

    if (isReducedMotion) {
        // Reduced motion: immediate static logo, hold 600ms, fade 200ms
        drawProgress = 1.0f
        dotTranslateY = 0.0f
        dotScaleX = 1.0f
        dotScaleY = 1.0f
        dotAlpha = 1.0f
        shadowAlpha = 0.0f
        rippleProgress = 0.0f
        penTipAlpha = 0.0f
        letterAlphas = List(8) { 1.0f }
        letterTranslationsY = List(8) { 0.0f }

        if (elapsedTotalMs < SplashTimings.reducedMotionHoldMs) {
            containerAlpha = 1.0f
            containerScale = 1.0f
        } else {
            val fadeElapsed = elapsedTotalMs - SplashTimings.reducedMotionHoldMs
            val fadeP = (fadeElapsed.toFloat() / SplashTimings.reducedMotionFadeMs).coerceIn(0f, 1f)
            containerAlpha = 1.0f - fadeP
            containerScale = 1.0f
        }
    } else {
        // Standard motion timeline shifted by timelineShiftMs
        val t = (elapsedTotalMs - timelineShiftMs).coerceAtLeast(0L)

        // 1. Draw-on of the "n" stroke (100ms - 850ms)
        when {
            t < SplashTimings.drawOnStart -> {
                drawProgress = 0.0f
                penTipAlpha = 0.0f
            }
            t in SplashTimings.drawOnStart..SplashTimings.drawOnEnd -> {
                val rawP = (t - SplashTimings.drawOnStart).toFloat() / SplashTimings.drawOnDuration
                val easedP = SplashTimings.drawOnEasing.transform(rawP.coerceIn(0f, 1f))
                drawProgress = easedP
                penTipAlpha = (1.0f - rawP).coerceIn(0f, 1f)
            }
            else -> {
                drawProgress = 1.0f
                penTipAlpha = 0.0f
            }
        }

        // 2. Dot drop, squash, rebound, and landing accents (750ms - 1150ms)
        when {
            t < SplashTimings.dotDropStart -> {
                dotAlpha = 0.0f
                dotTranslateY = -28.0f
                dotScaleX = 1.0f
                dotScaleY = 1.0f
                shadowAlpha = 0.0f
                rippleProgress = 0.0f
            }
            t in SplashTimings.dotDropStart..980L -> {
                // Drop phase: accelerating downward
                dotAlpha = 1.0f
                val dropP = (t - SplashTimings.dotDropStart).toFloat() / (980L - SplashTimings.dotDropStart)
                dotTranslateY = -28.0f * (1.0f - dropP * dropP)
                dotScaleX = 1.0f
                dotScaleY = 1.0f
                shadowAlpha = 0.0f
                rippleProgress = 0.0f
            }
            t in 980L..1050L -> {
                // Impact squash: scaleY compresses to 0.85, scaleX expands to 1.08
                dotAlpha = 1.0f
                dotTranslateY = 0.0f
                val squashP = (t - 980L).toFloat() / 70f
                val squashIntensity = (1.0f - (squashP - 0.5f) * (squashP - 0.5f) * 4f).coerceAtLeast(0f)
                dotScaleY = 1.0f - 0.15f * squashIntensity
                dotScaleX = 1.0f + 0.08f * squashIntensity

                // Accents on impact
                shadowAlpha = 0.12f * (1.0f - (t - 980L).toFloat() / 200f).coerceIn(0f, 0.12f)
                rippleProgress = ((t - 980L).toFloat() / 350f).coerceIn(0f, 1f)
            }
            t in 1050L..1200L -> {
                // Small rebound settling to 0
                dotAlpha = 1.0f
                val reboundP = (t - 1050L).toFloat() / 150f
                val bounce = sin(reboundP * PI.toFloat()).coerceAtLeast(0f)
                dotTranslateY = -4.0f * bounce
                dotScaleY = 1.0f + 0.04f * bounce
                dotScaleX = 1.0f - 0.02f * bounce

                val shadowElapsed = t - 980L
                shadowAlpha = if (shadowElapsed in 0..200) 0.12f * (1.0f - shadowElapsed / 200f) else 0.0f
                val rippleElapsed = t - 980L
                rippleProgress = if (rippleElapsed in 0..350) rippleElapsed / 350f else 0.0f
            }
            else -> {
                dotAlpha = 1.0f
                dotTranslateY = 0.0f
                dotScaleX = 1.0f
                dotScaleY = 1.0f
                shadowAlpha = 0.0f
                val rippleElapsed = t - 980L
                rippleProgress = if (rippleElapsed in 0..350) rippleElapsed / 350f else 0.0f
            }
        }

        // 3. Wordmark reveal with staggered letter cascade (1050ms - 1500ms)
        letterAlphas = List(8) { index ->
            val letterStart = SplashTimings.wordmarkStart + index * SplashTimings.wordmarkLetterStagger
            val letterDuration = 250L
            val p = ((t - letterStart).toFloat() / letterDuration).coerceIn(0f, 1f)
            SplashTimings.wordmarkEasing.transform(p)
        }

        letterTranslationsY = List(8) { index ->
            val letterStart = SplashTimings.wordmarkStart + index * SplashTimings.wordmarkLetterStagger
            val letterDuration = 250L
            val p = ((t - letterStart).toFloat() / letterDuration).coerceIn(0f, 1f)
            12.0f * (1.0f - SplashTimings.wordmarkEasing.transform(p))
        }

        // 4. Exit transition (fade out and scale 1.0 -> 0.96)
        if (isExitActive && exitStartTime != -1L) {
            val exitElapsed = elapsedTotalMs - exitStartTime
            val exitP = (exitElapsed.toFloat() / SplashTimings.exitDuration).coerceIn(0f, 1f)
            containerAlpha = 1.0f - exitP
            containerScale = 1.0f - 0.04f * exitP
        } else {
            containerAlpha = 1.0f
            containerScale = 1.0f
        }
    }

    // --- COMPOSABLE LAYOUT ---
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = containerAlpha
            }
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        NexpenseLogo(
            drawProgress = drawProgress,
            dotTranslateY = dotTranslateY,
            dotScaleX = dotScaleX,
            dotScaleY = dotScaleY,
            dotAlpha = dotAlpha,
            shadowAlpha = shadowAlpha,
            rippleProgress = rippleProgress,
            penTipAlpha = penTipAlpha,
            letterAlphas = letterAlphas,
            letterTranslationsY = letterTranslationsY,
            contentColor = contentColor,
            isDark = chosenIsDark,
            modifier = Modifier
                .safeDrawingPadding()
                .graphicsLayer {
                    this.scaleX = containerScale
                    this.scaleY = containerScale
                },
        )
    }
}
