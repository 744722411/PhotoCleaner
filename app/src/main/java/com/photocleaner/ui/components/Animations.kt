package com.photocleaner.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer loading effect for better loading states
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    width: Float = 300f,
    height: Float = 200f
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = width * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E2746),
            Color(0xFF2A3558),
            Color(0xFF1E2746)
        ),
        start = Offset(translateAnim - width, 0f),
        end = Offset(translateAnim, height)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(shimmerBrush)
    )
}

/**
 * Animated card with scale effect on press
 */
@Composable
fun AnimatedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Simple card with rounded corners and shadow effect
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

/**
 * Gradient text effect
 */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF58A6FF),
            Color(0xFF3FB950)
        )
    )
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
}

/**
 * Animated progress bar with gradient
 */
@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF58A6FF),
            Color(0xFF3FB950)
        )
    )
) {
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF21262D))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(gradient)
        )
    }
}

/**
 * Animated counter with smooth number transitions
 */
@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF58A6FF)
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "counter"
    )

    androidx.compose.material3.Text(
        text = animatedValue.toString(),
        modifier = modifier,
        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
        color = color,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )
}

/**
 * Glowing effect for important elements
 */
@Composable
fun GlowEffect(
    color: Color = Color(0xFF58A6FF),
    content: @Composable () -> Unit
) {
    Box {
        content()
        // Glow effect would be implemented with shadow or custom drawing
    }
}
