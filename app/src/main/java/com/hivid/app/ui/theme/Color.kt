package com.hivid.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class HdrColorsData(
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val border: Color,
    val accent: Color,
    val accentPressed: Color,
    val accentDim: Color,
    val onAccent: Color,
    val text: Color,
    val textMuted: Color,
    val textFaint: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val scrim: Color,
    val isDark: Boolean
)

val DarkHdrColors = HdrColorsData(
    bg = Color(0xFF0B0B0D),
    surface = Color(0xFF17171A),
    surfaceHigh = Color(0xFF202024),
    border = Color(0xFF2C2C32),
    accent = Color(0xFFF2DC3C),
    accentPressed = Color(0xFFD9C42E),
    accentDim = Color(0xFF8A7F22),
    onAccent = Color(0xFF1A1A1A),
    text = Color(0xFFF2F2F2),
    textMuted = Color(0xFFA3A3AB),
    textFaint = Color(0xFF6E6E76),
    success = Color(0xFF7BD88F),
    warning = Color(0xFFFFB74D),
    error = Color(0xFFFF6B6B),
    scrim = Color(0x99000000),
    isDark = true
)

val LightHdrColors = HdrColorsData(
    bg = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEAEAEE),
    border = Color(0xFFD6D6DE),
    accent = Color(0xFFE5CE20),
    accentPressed = Color(0xFFCBB614),
    accentDim = Color(0xFFFFF9C4),
    onAccent = Color(0xFF1A1A1A),
    text = Color(0xFF111114),
    textMuted = Color(0xFF5E5E68),
    textFaint = Color(0xFF8E8E98),
    success = Color(0xFF1E8E3E),
    warning = Color(0xFFE65100),
    error = Color(0xFFD32F2F),
    scrim = Color(0x66000000),
    isDark = false
)

val LocalHdrColors = staticCompositionLocalOf { DarkHdrColors }

object HdrColors {
    val Bg: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.bg

    val Surface: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.surface

    val SurfaceHigh: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.surfaceHigh

    val Border: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.border

    val Accent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.accent

    val AccentPressed: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.accentPressed

    val AccentDim: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.accentDim

    val OnAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.onAccent

    val Text: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.text

    val TextMuted: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.textMuted

    val TextFaint: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.textFaint

    val Success: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.success

    val Warning: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.warning

    val Error: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.error

    val Scrim: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.scrim

    val IsDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalHdrColors.current.isDark
}
