package com.vayora.extension.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Brand colours that change with light and dark mode. */
data class SentinelPalette(
    val accent: Color,
    val accentSoft: Color,
    val raised: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val isDark: Boolean
)

val DarkPalette = SentinelPalette(
    accent = Color(0xFFE8414E),
    accentSoft = Color(0xFFFF8A91),
    raised = Color(0xFF26282E),
    success = Color(0xFF3DD68C),
    warning = Color(0xFFFFB84D),
    danger = Color(0xFFFF7A7A),
    isDark = true
)

val LightPalette = SentinelPalette(
    accent = Color(0xFFD22B3B),
    accentSoft = Color(0xFFB3172E),
    raised = Color(0xFFE9EAEE),
    success = Color(0xFF168A57),
    warning = Color(0xFFA86400),
    danger = Color(0xFFC62828),
    isDark = false
)

val LocalSentinelPalette = staticCompositionLocalOf { DarkPalette }

val Accent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSentinelPalette.current.accent

val AccentSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSentinelPalette.current.accentSoft

val Raised: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSentinelPalette.current.raised

val Success: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSentinelPalette.current.success

val Warning: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSentinelPalette.current.warning

val Danger: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalSentinelPalette.current.danger
