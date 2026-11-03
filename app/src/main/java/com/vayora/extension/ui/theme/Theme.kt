package com.vayora.extension.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE8414E),
    onPrimary = Color.White,
    secondary = Color(0xFFFF8A91),
    secondaryContainer = Color(0xFF3A1D22),
    onSecondaryContainer = Color(0xFFFFD9DC),
    background = Color(0xFF0B0C0E),
    onBackground = Color(0xFFF2F2F4),
    surface = Color(0xFF15171A),
    onSurface = Color(0xFFF2F2F4),
    surfaceVariant = Color(0xFF1D1F23),
    onSurfaceVariant = Color(0xFFA2A5AE),
    outline = Color(0xFF444750),
    outlineVariant = Color(0xFF2A2C31),
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF1B0507)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFD22B3B),
    onPrimary = Color.White,
    secondary = Color(0xFFB3172E),
    secondaryContainer = Color(0xFFFBE3E6),
    onSecondaryContainer = Color(0xFF7A0F1B),
    background = Color(0xFFF4F4F6),
    onBackground = Color(0xFF16171A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16171A),
    surfaceVariant = Color(0xFFEDEDF0),
    onSurfaceVariant = Color(0xFF5B5E67),
    outline = Color(0xFF8D909A),
    outlineVariant = Color(0xFFE0E1E6),
    error = Color(0xFFC62828),
    onError = Color.White
)

private val VayoraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun VayoraTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(
        LocalSentinelPalette provides (if (dark) DarkPalette else LightPalette)
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = VayoraTypography,
            shapes = VayoraShapes,
            content = content
        )
    }
}
