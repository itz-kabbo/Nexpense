package com.nexusbench.nexpense.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexusbench.nexpense.core.designsystem.components.calculatePerforatedTeeth
import com.nexusbench.nexpense.core.designsystem.components.clampCategoryFraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemTest {

    @Test
    fun lightColorsMatchSpecTokens() {
        val colors = lightAppColors()
        assertFalse(colors.isDark)
        assertEquals(Color(0xFFFBFBF9), colors.background)
        assertEquals(Color(0xFFFFFFFF), colors.surface)
        assertEquals(Color(0xFFF2F2EC), colors.surfaceVariant)
        assertEquals(Color(0xFFFDFCF7), colors.paper)
        assertEquals(Color(0xFF18181B), colors.textPrimary)
        assertEquals(Color(0xFF71717A), colors.textSecondary)
        assertEquals(Color(0xFFA1A1AA), colors.textTertiary)
        assertEquals(Color(0xFFE8E8E2), colors.hairline)
        assertEquals(Color.Transparent, colors.rimHighlight)
    }

    @Test
    fun darkColorsMatchSpecTokens() {
        val colors = darkAppColors()
        assertTrue(colors.isDark)
        assertEquals(Color(0xFF09090B), colors.background)
        assertEquals(Color(0xFF141417), colors.surface)
        assertEquals(Color(0xFF1D1D22), colors.surfaceVariant)
        assertEquals(Color(0xFF19191E), colors.paper)
        assertEquals(Color(0xFFF4F4F5), colors.textPrimary)
        assertEquals(Color(0xFFA1A1AA), colors.textSecondary)
        assertEquals(Color(0xFF52525B), colors.textTertiary)
        assertEquals(Color(0xFF27272A), colors.hairline)
        assertEquals(Color(0x18FFFFFF), colors.rimHighlight)
    }

    @Test
    fun typographyTokensUseTabularFiguresAndMonospace() {
        val typo = AppTypography()

        // Tabular figures ("tnum") for amount styles
        assertEquals("tnum", typo.bigAmount.fontFeatureSettings)
        assertEquals("tnum", typo.bigAmountInteger.fontFeatureSettings)
        assertEquals("tnum", typo.bigAmountDecimals.fontFeatureSettings)
        assertEquals("tnum", typo.amount.fontFeatureSettings)
        assertEquals("tnum", typo.receipt.fontFeatureSettings)
        assertEquals("tnum", typo.receiptTitle.fontFeatureSettings)
        assertEquals("tnum", typo.receiptTotal.fontFeatureSettings)

        // Monospace for receipt styles
        assertEquals(FontFamily.Monospace, typo.receipt.fontFamily)
        assertEquals(FontFamily.Monospace, typo.receiptTitle.fontFamily)
        assertEquals(FontFamily.Monospace, typo.receiptTotal.fontFamily)

        // Optical hero amount sizes
        assertEquals(52.sp, typo.bigAmount.fontSize)
        assertEquals(52.sp, typo.bigAmountInteger.fontSize)
        assertEquals(34.sp, typo.bigAmountDecimals.fontSize)
        assertEquals(20.sp, typo.bigAmountCurrency.fontSize)
    }

    @Test
    fun shapesMatchSpecRadii() {
        val shapes = AppShapes()
        val density = Density(1f)

        // Sheets 28dp top radius
        assertTrue(shapes.sheet is RoundedCornerShape)
        val sheetShape = shapes.sheet as RoundedCornerShape
        assertEquals(28f, sheetShape.topStart.toPx(Size(100f, 100f), density))
        assertEquals(28f, sheetShape.topEnd.toPx(Size(100f, 100f), density))
        assertEquals(0f, sheetShape.bottomStart.toPx(Size(100f, 100f), density))
        assertEquals(0f, sheetShape.bottomEnd.toPx(Size(100f, 100f), density))

        // Cards 16dp radius
        assertTrue(shapes.card is RoundedCornerShape)
        val cardShape = shapes.card as RoundedCornerShape
        assertEquals(16f, cardShape.topStart.toPx(Size(100f, 100f), density))
        assertEquals(16f, cardShape.bottomEnd.toPx(Size(100f, 100f), density))

        // Dock 20dp radius
        assertTrue(shapes.dock is RoundedCornerShape)
        val dockShape = shapes.dock as RoundedCornerShape
        assertEquals(20f, dockShape.topStart.toPx(Size(100f, 100f), density))
        assertEquals(20f, dockShape.bottomEnd.toPx(Size(100f, 100f), density))

        // Chips fully rounded (CircleShape)
        assertEquals(CircleShape, shapes.chip)
    }

    @Test
    fun motionTokensMatchSpec() {
        // Damping ~0.85, medium stiffness
        assertEquals(0.85f, AppMotion.SPRING_DAMPING)
        // Fade 150-200ms
        assertTrue(AppMotion.FADE_DURATION_MS in 150..200)
        // List insert 250ms
        assertEquals(250, AppMotion.LIST_INSERT_DURATION_MS)

        val spring = AppMotion.sheetSpring<Float>()
        assertEquals(0.85f, spring.dampingRatio)
        assertEquals(AppMotion.SPRING_STIFFNESS, spring.stiffness)

        val fade = AppMotion.fadeSpec<Float>()
        assertEquals(AppMotion.FADE_DURATION_MS, fade.durationMillis)

        val listInsert = AppMotion.listInsertSpec<Float>()
        assertEquals(AppMotion.LIST_INSERT_DURATION_MS, listInsert.durationMillis)
    }

    @Test
    fun spacingTokensMatch4dpGridAndScreenHorizontal() {
        val spacing = AppSpacing()
        assertEquals(24.dp, spacing.screenHorizontal)
        assertEquals(4.dp, spacing.xs)
        assertEquals(8.dp, spacing.s)
        assertEquals(12.dp, spacing.m)
        assertEquals(16.dp, spacing.l)
    }

    @Test
    fun perforatedShapeCalculationMatchesGeometry() {
        // Even division
        val teeth1 = calculatePerforatedTeeth(width = 100f, toothPx = 10f)
        assertEquals(10, teeth1.count)
        assertEquals(10f, teeth1.toothWidth, 0.001f)
        assertEquals(100f, teeth1.count * teeth1.toothWidth, 0.001f)

        // Fractional division: width = 105f, toothPx = 10f -> 10 teeth of 10.5f each
        val teeth2 = calculatePerforatedTeeth(width = 105f, toothPx = 10f)
        assertEquals(10, teeth2.count)
        assertEquals(10.5f, teeth2.toothWidth, 0.001f)
        assertEquals(105f, teeth2.count * teeth2.toothWidth, 0.001f)

        // Width smaller than toothPx: coerce to at least 1 tooth
        val teeth3 = calculatePerforatedTeeth(width = 5f, toothPx = 10f)
        assertEquals(1, teeth3.count)
        assertEquals(5f, teeth3.toothWidth, 0.001f)

        // Zero or negative toothPx fallback
        val teeth4 = calculatePerforatedTeeth(width = 100f, toothPx = 0f)
        assertEquals(1, teeth4.count)
        assertEquals(100f, teeth4.toothWidth, 0.001f)
    }
    @Test
    fun categoryBarFractionClampingLogic() {
        assertEquals(0f, clampCategoryFraction(Float.NaN))
        assertEquals(0f, clampCategoryFraction(-0.5f))
        assertEquals(1f, clampCategoryFraction(1.5f))
        assertEquals(0.42f, clampCategoryFraction(0.42f))
        assertEquals(0f, clampCategoryFraction(0f))
        assertEquals(1f, clampCategoryFraction(1f))
    }

    @Test
    fun segmentedControlSelectionStateLogic() {
        val items = listOf("Day", "Month", "Year")
        var selectedIndex = 1

        val onSelect: (Int) -> Unit = { newIndex ->
            if (newIndex in items.indices) {
                selectedIndex = newIndex
            }
        }

        onSelect(0)
        assertEquals(0, selectedIndex)

        onSelect(2)
        assertEquals(2, selectedIndex)

        // Invalid index should not update
        onSelect(5)
        assertEquals(2, selectedIndex)
    }
}
