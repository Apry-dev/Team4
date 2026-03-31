package com.example.esnmessenger.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ESNColorScheme = lightColorScheme(
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

private val ESNShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun ESNMessengerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ESNColorScheme,
        typography = Typography,
        shapes = ESNShapes,
        content = content
    )
}
