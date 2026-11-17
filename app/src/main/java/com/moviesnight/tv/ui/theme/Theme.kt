package com.moviesnight.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val AppColors = darkColorScheme(
    primary = TextPrimary,
    onPrimary = Background,
    secondary = Star,
    onSecondary = Background,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceFocused,
    onSurfaceVariant = TextSecondary,
    border = FocusGlow,
)

@Composable
fun MoviesNightTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
