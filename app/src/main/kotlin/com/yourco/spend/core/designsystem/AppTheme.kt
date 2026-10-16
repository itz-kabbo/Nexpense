package com.yourco.spend.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// VERIFY: Proposed design tokens from section 3.3
val BackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFF6F6F4)
val PaperLight = Color(0xFFFBFAF6)
val TextPrimaryLight = Color(0xFF111111)
val TextSecondaryLight = Color(0xFF8A8A8E)
val HairlineLight = Color(0xFFE5E5EA)

val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF121212)
val PaperDark = Color(0xFF1A1A1A)
val TextPrimaryDark = Color(0xFFF2F2F2)
val TextSecondaryDark = Color(0xFF8E8E93)
val HairlineDark = Color(0xFF2C2C2E)

private val LightColorScheme = lightColorScheme(
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
)

private val DarkColorScheme = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
