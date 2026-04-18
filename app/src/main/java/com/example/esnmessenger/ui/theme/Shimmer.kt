package com.example.esnmessenger.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape

/**
 * A box that renders a left-to-right shimmer sweep animation.
 * Use it to replace loading spinners with skeleton placeholders.
 * Works in both light and dark mode.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    val base = MaterialTheme.colorScheme.onSurface
    val shimmerColors = listOf(
        base.copy(alpha = 0.06f),
        base.copy(alpha = 0.16f),
        base.copy(alpha = 0.06f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(x - 400f, 0f),
                end = Offset(x, 0f)
            ),
            shape = shape
        )
    )
}
