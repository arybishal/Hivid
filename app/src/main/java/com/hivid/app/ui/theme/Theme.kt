package com.hivid.app.ui.theme

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkHdrColors.accent,
    onPrimary = DarkHdrColors.onAccent,
    primaryContainer = DarkHdrColors.surfaceHigh,
    onPrimaryContainer = DarkHdrColors.text,
    secondary = DarkHdrColors.accentDim,
    onSecondary = DarkHdrColors.text,
    background = DarkHdrColors.bg,
    onBackground = DarkHdrColors.text,
    surface = DarkHdrColors.surface,
    onSurface = DarkHdrColors.text,
    surfaceVariant = DarkHdrColors.surfaceHigh,
    onSurfaceVariant = DarkHdrColors.textMuted,
    outline = DarkHdrColors.border,
    error = DarkHdrColors.error,
    onError = DarkHdrColors.onAccent
)

private val LightColorScheme = lightColorScheme(
    primary = LightHdrColors.accent,
    onPrimary = LightHdrColors.onAccent,
    primaryContainer = LightHdrColors.surfaceHigh,
    onPrimaryContainer = LightHdrColors.text,
    secondary = LightHdrColors.accentDim,
    onSecondary = LightHdrColors.text,
    background = LightHdrColors.bg,
    onBackground = LightHdrColors.text,
    surface = LightHdrColors.surface,
    onSurface = LightHdrColors.text,
    surfaceVariant = LightHdrColors.surfaceHigh,
    onSurfaceVariant = LightHdrColors.textMuted,
    outline = LightHdrColors.border,
    error = LightHdrColors.error,
    onError = LightHdrColors.onAccent
)

@Composable
fun HividTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val hdrColors = if (darkTheme) DarkHdrColors else LightHdrColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            val activity = view.context as? ComponentActivity
            if (activity != null) {
                activity.enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
            }
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalHdrColors provides hdrColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HividTypography,
            content = content
        )
    }
}
