package com.yourco.spend.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// VERIFY: Proposed design tokens from section 3.3
// Light: background #FFFFFF, surface #F6F6F4, paper #FBFAF6, text primary #111111, text secondary #8A8A8E, hairline #E5E5EA
val BackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFF6F6F4)
val PaperLight = Color(0xFFFBFAF6)
val TextPrimaryLight = Color(0xFF111111)
val TextSecondaryLight = Color(0xFF8A8A8E)
val HairlineLight = Color(0xFFE5E5EA)

// Dark: background #000000, surface #121212, paper #1A1A1A, text primary #F2F2F2, text secondary #8E8E93, hairline #2C2C2E
val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF121212)
val PaperDark = Color(0xFF1A1A1A)
val TextPrimaryDark = Color(0xFFF2F2F2)
val TextSecondaryDark = Color(0xFF8E8E93)
val HairlineDark = Color(0xFF2C2C2E)

@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val paper: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val hairline: Color,
    val isDark: Boolean,
)

fun lightAppColors(): AppColors = AppColors(
    background = BackgroundLight,
    surface = SurfaceLight,
    paper = PaperLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    hairline = HairlineLight,
    isDark = false,
)

fun darkAppColors(): AppColors = AppColors(
    background = BackgroundDark,
    surface = SurfaceDark,
    paper = PaperDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    hairline = HairlineDark,
    isDark = true,
)
