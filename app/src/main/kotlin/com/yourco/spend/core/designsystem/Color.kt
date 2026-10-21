package com.yourco.spend.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// VERIFY: Proposed design tokens from section 3.3 and docs/UI.md redesign specification
// Light Palette (Tactile Porcelain)
val BackgroundLight = Color(0xFFFBFBF9)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF2F2EC)
val PaperLight = Color(0xFFFDFCF7)
val TextPrimaryLight = Color(0xFF18181B)
val TextSecondaryLight = Color(0xFF71717A)
val TextTertiaryLight = Color(0xFFA1A1AA)
val HairlineLight = Color(0xFFE8E8E2)
val RimHighlightLight = Color.Transparent

// Dark Palette (Obsidian & Bevel)
val BackgroundDark = Color(0xFF09090B)
val SurfaceDark = Color(0xFF141417)
val SurfaceVariantDark = Color(0xFF1D1D22)
val PaperDark = Color(0xFF19191E)
val TextPrimaryDark = Color(0xFFF4F4F5)
val TextSecondaryDark = Color(0xFFA1A1AA)
val TextTertiaryDark = Color(0xFF52525B)
val HairlineDark = Color(0xFF27272A)
val RimHighlightDark = Color(0x18FFFFFF) // 0.5dp top-edge specular highlight

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color = SurfaceVariantLight,
    val paper: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color = TextTertiaryLight,
    val hairline: Color,
    val rimHighlight: Color = RimHighlightLight,
    val isDark: Boolean,
)

fun lightAppColors(): AppColors = AppColors(
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    paper = PaperLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    textTertiary = TextTertiaryLight,
    hairline = HairlineLight,
    rimHighlight = RimHighlightLight,
    isDark = false,
)

fun darkAppColors(): AppColors = AppColors(
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    paper = PaperDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    textTertiary = TextTertiaryDark,
    hairline = HairlineDark,
    rimHighlight = RimHighlightDark,
    isDark = true,
)
