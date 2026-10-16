package com.yourco.spend.core.designsystem.components

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PerforatedTeeth(val count: Int, val toothWidth: Float)

/**
 * Calculates the number of teeth and exact width per tooth to fit evenly across the width.
 */
fun calculatePerforatedTeeth(width: Float, toothPx: Float): PerforatedTeeth {
    val n = if (toothPx <= 0f) 1 else (width / toothPx).toInt().coerceAtLeast(1)
    val w = if (n > 0) width / n else width
    return PerforatedTeeth(count = n, toothWidth = w)
}

/**
 * Creates a perforated Shape with triangular teeth along top and bottom edges.
 * From GUIDE.md section 8b M6 skeleton.
 */
fun perforated(toothPx: Float): Shape = GenericShape { size, _ ->
    if (toothPx <= 0f || size.width <= 0f || size.height <= 0f) return@GenericShape
    val (n, w) = calculatePerforatedTeeth(size.width, toothPx)
    moveTo(0f, 0f)
    for (i in 0 until n) {
        lineTo(i * w + w / 2, toothPx / 2)
        lineTo((i + 1) * w, 0f)
    }
    lineTo(size.width, size.height)
    for (i in n downTo 1) {
        lineTo(i * w - w / 2, size.height - toothPx / 2)
        lineTo((i - 1) * w, size.height)
    }
    close()
}

/**
 * Remembers a perforated Shape calculated from Dp tooth width.
 */
@Composable
fun rememberPerforatedShape(toothWidth: Dp = 6.dp): Shape {
    val density = LocalDensity.current
    return remember(density, toothWidth) {
        val toothPx = with(density) { toothWidth.toPx() }
        perforated(toothPx)
    }
}
