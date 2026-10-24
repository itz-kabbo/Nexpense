package com.nexusbench.nexpense.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

// VERIFY: Motion parameters proposed in section 3.3 and docs/UI.md redesign specification
object AppMotion {
    const val SPRING_DAMPING: Float = 0.85f
    const val SPRING_STIFFNESS: Float = Spring.StiffnessMedium
    const val FADE_DURATION_MS: Int = 180 // 150-200ms
    const val LIST_INSERT_DURATION_MS: Int = 250
    const val RECEIPT_PRINT_DURATION_MS: Int = 350

    fun <T> sheetSpring(): SpringSpec<T> = spring(
        dampingRatio = SPRING_DAMPING,
        stiffness = SPRING_STIFFNESS,
    )

    fun <T> fadeSpec(durationMillis: Int = FADE_DURATION_MS): TweenSpec<T> = tween(
        durationMillis = durationMillis,
        easing = FastOutSlowInEasing,
    )

    fun <T> listInsertSpec(): TweenSpec<T> = tween(
        durationMillis = LIST_INSERT_DURATION_MS,
        easing = FastOutSlowInEasing,
    )

    fun <T> receiptPrintSpec(): TweenSpec<T> = tween(
        durationMillis = RECEIPT_PRINT_DURATION_MS,
        easing = FastOutSlowInEasing,
    )
}
