package com.yourco.spend.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppColors = staticCompositionLocalOf { lightAppColors() }
val LocalAppTypography = staticCompositionLocalOf { AppTypography() }
val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val shapes: AppShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalAppShapes.current

    val spacing: AppSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAppSpacing.current

    val motion: AppMotion = AppMotion
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkAppColors() else lightAppColors()
    val typography = AppTypography()
    val shapes = AppShapes()
    val spacing = AppSpacing()

    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.textPrimary,
            onPrimary = colors.background,
            secondary = colors.textSecondary,
            onSecondary = colors.background,
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.paper,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.hairline,
            outlineVariant = colors.hairline,
        )
    } else {
        lightColorScheme(
            primary = colors.textPrimary,
            onPrimary = colors.background,
            secondary = colors.textSecondary,
            onSecondary = colors.background,
            background = colors.background,
            surface = colors.surface,
            surfaceVariant = colors.paper,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.hairline,
            outlineVariant = colors.hairline,
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppShapes provides shapes,
        LocalAppSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content,
        )
    }
}
