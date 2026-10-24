package com.nexusbench.nexpense.feature.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders the Nexpense logo mark (the continuous "n" stroke and centered circular dot)
 * alongside the staggered wordmark "nexpense".
 *
 * Implements 60fps allocation-free rendering by caching Paths, PathMeasures, and using
 * Compose draw transforms and graphicsLayers.
 */
@Composable
fun NexpenseLogo(
    drawProgress: Float,
    dotTranslateY: Float,
    dotScaleX: Float,
    dotScaleY: Float,
    dotAlpha: Float,
    shadowAlpha: Float,
    rippleProgress: Float,
    penTipAlpha: Float,
    letterAlphas: List<Float>,
    letterTranslationsY: List<Float>,
    contentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val markWidth = NexpenseBrandAssets.MarkWidth
    val markHeight = markWidth * (NexpenseBrandAssets.VIEWBOX_HEIGHT / NexpenseBrandAssets.VIEWBOX_WIDTH)

    // Pre-allocated paths & path measure for stroke draw-on
    val referencePath = remember { NexpenseBrandAssets.createNStrokePath() }
    val pathMeasure = remember { PathMeasure().apply { setPath(referencePath, forceClosed = false) } }
    val segmentPath = remember { Path() }

    val density = LocalDensity.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // --- LOGO MARK CANVAS ---
        Canvas(
            modifier = Modifier.size(width = markWidth, height = markHeight),
        ) {
            val scaleFactor = size.width / NexpenseBrandAssets.VIEWBOX_WIDTH
            val dotTranslateYRef = with(density) { dotTranslateY.dp.toPx() } / scaleFactor

            scale(scale = scaleFactor, pivot = Offset.Zero) {
                // 1. Light theme contact shadow under the dot on impact
                if (!isDark && shadowAlpha > 0f) {
                    val shadowWidth = NexpenseBrandAssets.DOT_RADIUS * 1.8f
                    val shadowHeight = 2.0f
                    drawOval(
                        color = contentColor.copy(alpha = shadowAlpha),
                        topLeft = Offset(
                            x = NexpenseBrandAssets.DOT_CENTER_X - shadowWidth / 2f,
                            y = NexpenseBrandAssets.DOT_CENTER_Y + NexpenseBrandAssets.DOT_RADIUS + 0.5f,
                        ),
                        size = Size(shadowWidth, shadowHeight),
                    )
                }

                // 2. Dark theme luminous ring ripple emitted on impact
                if (isDark && rippleProgress > 0f && rippleProgress <= 1f) {
                    val currentRippleScale = 1.0f + 1.2f * rippleProgress
                    val currentRippleAlpha = (0.40f * (1f - rippleProgress)).coerceIn(0f, 1f)
                    val strokePx = (1.5.dp.toPx() / scaleFactor).coerceAtLeast(0.5f)
                    drawCircle(
                        color = contentColor.copy(alpha = currentRippleAlpha),
                        radius = NexpenseBrandAssets.DOT_RADIUS * currentRippleScale,
                        center = Offset(NexpenseBrandAssets.DOT_CENTER_X, NexpenseBrandAssets.DOT_CENTER_Y),
                        style = Stroke(width = strokePx),
                    )
                }

                // 3. Dot drop into arch counter
                if (dotAlpha > 0f) {
                    val dotCenter = Offset(
                        x = NexpenseBrandAssets.DOT_CENTER_X,
                        y = NexpenseBrandAssets.DOT_CENTER_Y + dotTranslateYRef,
                    )
                    scale(
                        scaleX = dotScaleX,
                        scaleY = dotScaleY,
                        pivot = dotCenter,
                    ) {
                        drawCircle(
                            color = contentColor.copy(alpha = dotAlpha),
                            radius = NexpenseBrandAssets.DOT_RADIUS,
                            center = dotCenter,
                        )
                    }
                }

                // 4. Continuous "n" stroke draw-on
                if (drawProgress > 0f) {
                    segmentPath.reset()
                    val totalLength = pathMeasure.length
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = totalLength * drawProgress.coerceIn(0f, 1f),
                        destination = segmentPath,
                        startWithMoveTo = true,
                    )
                    drawPath(
                        path = segmentPath,
                        color = contentColor,
                        style = Stroke(
                            width = NexpenseBrandAssets.STROKE_WIDTH,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )

                    // 5. Dark theme leading pen tip glow (fades out by stroke completion)
                    if (isDark && penTipAlpha > 0f && drawProgress < 1f) {
                        val tipPos = pathMeasure.getPosition(totalLength * drawProgress)
                        val penRadius = (6.dp.toPx() / scaleFactor)
                        drawCircle(
                            color = contentColor.copy(alpha = 0.35f * penTipAlpha),
                            radius = penRadius,
                            center = tipPos,
                        )
                        drawCircle(
                            color = contentColor.copy(alpha = 0.15f * penTipAlpha),
                            radius = penRadius * 1.6f,
                            center = tipPos,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(NexpenseBrandAssets.MarkToWordmarkGap))

        // --- WORDMARK "nexpense" WITH STAGGERED CHARACTER CASCADE ---
        val letters = remember { "nexpense".map { it.toString() } }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            letters.forEachIndexed { index, letter ->
                val alpha = letterAlphas.getOrElse(index) { 0f }
                val translationY = letterTranslationsY.getOrElse(index) { 12f }
                Text(
                    text = letter,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp,
                    color = contentColor,
                    modifier = Modifier.graphicsLayer {
                        this.alpha = alpha
                        this.translationY = with(density) { translationY.dp.toPx() }
                    },
                )
            }
        }
    }
}
