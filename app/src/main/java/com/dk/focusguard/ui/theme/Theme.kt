package com.dk.focusguard.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = CyberBackground,
    primaryContainer = CyberGreenDark,
    onPrimaryContainer = TextWhite,
    secondary = CyberCyan,
    onSecondary = CyberBackground,
    secondaryContainer = CyberCyanDark,
    onSecondaryContainer = TextWhite,
    tertiary = CyberCyan,
    background = CyberBackground,
    onBackground = TextWhite,
    surface = CyberCard,
    onSurface = TextWhite,
    surfaceVariant = CyberCardVariant,
    onSurfaceVariant = TextGray,
    error = CyberRed,
    onError = TextWhite,
    errorContainer = CyberRedGlow,
    outline = CyberCardBorder
)

@Composable
fun DKFocusGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = CyberDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.background.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
