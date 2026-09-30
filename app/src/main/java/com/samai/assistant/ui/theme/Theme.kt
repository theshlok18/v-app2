package com.samai.assistant.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SAMColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Black,
    primaryContainer = AccentGlow,
    onPrimaryContainer = TextPrimary,
    secondary = AccentPurple,
    onSecondary = Black,
    secondaryContainer = DarkElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentCyan,
    onTertiary = Black,
    background = Black,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary,
    outline = TextMuted
)

@Composable
fun SAMTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SAMColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SAMTypography,
        content = content
    )
}
