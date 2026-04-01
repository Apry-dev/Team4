package com.example.esnmessenger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ESNColorScheme = lightColorScheme(
    primary = ESNCyan,
    onPrimary = OnPrimaryWhite,
    primaryContainer = ESNCyanDark,
    secondary = ESNMagenta,
    onSecondary = OnPrimaryWhite,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun ESNMessengerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ESNColorScheme,
        typography = Typography,
        content = content
    )
}
