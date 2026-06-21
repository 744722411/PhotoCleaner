package com.photocleaner.ui.review

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCard(
    photo: Photo,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable {
            if (isBatchMode) onToggleSelection() else onClick()
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(photo.uri).crossfade(true).build(),
                contentDescription = photo.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
            )

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(text = photo.displayName, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                if (photo.category.isNotEmpty()) {
                    Text(text = photo.category, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                }
            }

            ClassificationChip(
                classification = photo.classification, confidence = photo.confidence,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            )

            if (isBatchMode) {
                Checkbox(
                    checked = isSelected, onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    colors = CheckboxDefaults.colors(checkedColor = BlueAccent, uncheckedColor = Color.White.copy(alpha = 0.6f), checkmarkColor = Color.White)
                )
            }

            if (!isBatchMode && photo.classification != Classification.KEEP) {
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onKeep,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(GreenAccent.copy(alpha = 0.9f))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保留", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(RedAccent.copy(alpha = 0.9f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (isBatchMode && isSelected) {
                Box(modifier = Modifier.fillMaxSize().background(BlueAccent.copy(alpha = 0.3f)))
            }
        }
    }
}

@Composable
fun ClassificationChip(
    classification: Classification,
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (classification) {
        Classification.USELESS -> RedAccent to "无用"
        Classification.KEEP -> GreenAccent to "保留"
        Classification.UNCERTAIN -> YellowAccent to "待定"
        Classification.UNKNOWN -> Color.Gray to "未分类"
    }

    Surface(modifier = modifier, color = color.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = text, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            if (confidence > 0f) {
                Text(text = "${(confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}
