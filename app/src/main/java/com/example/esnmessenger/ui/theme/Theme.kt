package com.example.esnmessenger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dark mode surface colours (defined here, not exported — use MaterialTheme instead)
private val DarkBackground    = Color(0xFF0F1923)
private val DarkSurface       = Color(0xFF1A2535)
private val DarkSurfaceVariant = Color(0xFF243044)
private val DarkTextPrimary   = Color(0xFFE8EDF3)
private val DarkTextSecondary = Color(0xFF8B96A5)
private val DarkOutline       = Color(0xFF2A3A50)

private val ESNLightColorScheme = lightColorScheme(
    primary = ESNCyan,
    onPrimary = OnPrimaryWhite,
    primaryContainer = ESNCyanLight,
    onPrimaryContainer = ESNCyanDark,
    secondary = ESNMagenta,
    onSecondary = OnPrimaryWhite,
    secondaryContainer = ESNMagentaLight,
    onSecondaryContainer = ESNMagentaDark,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = OutlineColor,
)

private val ESNDarkColorScheme = darkColorScheme(
    primary = ESNCyan,
    onPrimary = OnPrimaryWhite,
    primaryContainer = ESNCyanDark,
    onPrimaryContainer = ESNCyanLight,
    secondary = ESNMagenta,
    onSecondary = OnPrimaryWhite,
    secondaryContainer = ESNMagentaDark,
    onSecondaryContainer = ESNMagentaLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
)

private val ESNShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ESNMessengerTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) ESNDarkColorScheme else ESNLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ESNShapes,
        content = content
    )
}
