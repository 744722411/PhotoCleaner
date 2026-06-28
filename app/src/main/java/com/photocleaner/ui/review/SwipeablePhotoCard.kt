package com.photocleaner.ui.review

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.ui.theme.DarkSurfaceVariant
import com.photocleaner.ui.theme.GreenAccent
import com.photocleaner.ui.theme.RedAccent
import com.photocleaner.ui.theme.YellowAccent
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeablePhotoCard(
    photo: Photo,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var dragOffset by remember(photo.id) { mutableStateOf(0f) }
    val animOffset = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val screenWidth = with(density) { maxWidth.toPx() }
        val threshold = screenWidth * 0.2f

    var hasHapticTriggeredRight by remember { mutableStateOf(false) }
    var hasHapticTriggeredLeft by remember { mutableStateOf(false) }

    // Resolve the active offset to use in graphicsLayer and overlays
    val activeOffset = if (animOffset.isRunning) animOffset.value else dragOffset
    val activeRotation = activeOffset / 30f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                translationX = activeOffset,
                rotationZ = activeRotation
            )
            .pointerInput(photo.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (dragOffset > threshold) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                animOffset.snapTo(dragOffset)
                                animOffset.animateTo(screenWidth, tween(300))
                                onSwipedRight()
                            } else if (dragOffset < -threshold) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                animOffset.snapTo(dragOffset)
                                animOffset.animateTo(-screenWidth, tween(300))
                                onSwipedLeft()
                            } else {
                                // Reset
                                hasHapticTriggeredRight = false
                                hasHapticTriggeredLeft = false
                                animOffset.snapTo(dragOffset)
                                dragOffset = 0f
                                animOffset.animateTo(0f, tween(300))
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount

                        // Threshold crossing haptic cues
                        if (dragOffset > threshold) {
                            if (!hasHapticTriggeredRight) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                hasHapticTriggeredRight = true
                                hasHapticTriggeredLeft = false
                            }
                        } else if (dragOffset < -threshold) {
                            if (!hasHapticTriggeredLeft) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                hasHapticTriggeredLeft = true
                                hasHapticTriggeredRight = false
                            }
                        } else {
                            hasHapticTriggeredRight = false
                            hasHapticTriggeredLeft = false
                        }
                    }
                )
            }
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurfaceVariant)
            .border(
                width = 4.dp,
                color = when {
                    activeOffset > 50f -> GreenAccent.copy(alpha = (activeOffset / threshold).coerceIn(0f, 1f))
                    activeOffset < -50f -> RedAccent.copy(alpha = (abs(activeOffset) / threshold).coerceIn(0f, 1f))
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        AsyncImage(
            model = remember(photo.uri) {
                ImageRequest.Builder(context)
                    .data(photo.uri)
                    .crossfade(true)
                    .build()
            },
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay status based on drag
        if (activeOffset > 50f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GreenAccent.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(120.dp)
                )
            }
        } else if (activeOffset < -50f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RedAccent.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(120.dp)
                )
            }
        }

        // Info Overlay at Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = photo.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${photo.width}×${photo.height} • ${com.photocleaner.util.ImageUtils.formatFileSize(photo.size)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                // Classification Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (photo.classification) {
                                Classification.USELESS -> RedAccent.copy(alpha = 0.2f)
                                Classification.KEEP -> GreenAccent.copy(alpha = 0.2f)
                                Classification.UNCERTAIN -> YellowAccent.copy(alpha = 0.2f)
                                else -> Color.Gray.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = photo.classification.displayName,
                        color = when (photo.classification) {
                            Classification.USELESS -> RedAccent
                            Classification.KEEP -> GreenAccent
                            Classification.UNCERTAIN -> YellowAccent
                            else -> Color.Gray
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    }
}
