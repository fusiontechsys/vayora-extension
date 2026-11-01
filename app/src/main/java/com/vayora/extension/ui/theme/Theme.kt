package com.vayora.extension.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VayoraColors = darkColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    secondary = Blue300,
    background = Navy800,
    onBackground = TextPrimary,
    surface = Navy700,
    onSurface = TextPrimary,
    surfaceVariant = Navy600,
    onSurfaceVariant = TextSecondary,
    error = Danger
)

@Composable
fun VayoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VayoraColors,
        typography = VayoraTypography,
        content = content
    )
}
