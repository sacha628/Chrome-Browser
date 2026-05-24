package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ElegantDarkColorScheme = darkColorScheme(
    primary = ElegantDarkPrimary,
    secondary = ElegantDarkTextSecondary,
    background = ElegantDarkBackground,
    surface = ElegantDarkSurface,
    surfaceVariant = ElegantDarkSurfaceVariant,
    onPrimary = ElegantDarkOnPrimary,
    onSecondary = ElegantDarkBackground,
    onBackground = ElegantDarkTextPrimary,
    onSurface = ElegantDarkTextPrimary,
    outline = ElegantDarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Elegant Dark theme globally
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // We enforce the Elegant Dark color scheme globally
    val colorScheme = ElegantDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
