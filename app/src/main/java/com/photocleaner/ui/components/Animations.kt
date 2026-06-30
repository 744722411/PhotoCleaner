package com.photocleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
