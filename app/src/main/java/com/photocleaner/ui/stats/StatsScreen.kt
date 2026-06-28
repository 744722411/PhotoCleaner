package com.photocleaner.ui.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photocleaner.R
import com.photocleaner.domain.model.Classification
import com.photocleaner.ui.components.*
import com.photocleaner.ui.theme.*

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GradientStart, GradientMid, GradientEnd))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(600)) + fadeIn(tween(600))) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BlueAccent, Purple80))), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(text = stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(text = stringResource(R.string.stats_subtitle), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.62f), maxLines = 2)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernStatCard(title = stringResource(R.string.stats_total), value = uiState.totalPhotos.toString(), icon = Icons.Default.PhotoLibrary, color = BlueAccent, modifier = Modifier.weight(1f))
                        ModernStatCard(title = stringResource(R.string.stats_processed), value = uiState.processedPhotos.toString(), icon = Icons.Default.CheckCircle, color = GreenAccent, modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernStatCard(title = stringResource(R.string.stats_useless), value = uiState.uselessPhotos.toString(), icon = Icons.Default.Delete, color = RedAccent, modifier = Modifier.weight(1f))
                        ModernStatCard(title = stringResource(R.string.stats_savings), value = uiState.spaceSaved, icon = Icons.Default.Storage, color = YellowAccent, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (uiState.categoryStats.isNotEmpty()) {
            item {
                AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600, 300)) + fadeIn(tween(600, 300))) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernSectionHeader(title = stringResource(R.string.stats_breakdown), icon = Icons.Default.PieChart)
                            val total = uiState.categoryStats.sumOf { it.count }.toFloat()
                            if (total > 0) {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    AnimatedPieChart(data = uiState.categoryStats.associate { it.classification.displayName to it.count }, total = total)
                                }
                            }
                            uiState.categoryStats.take(3).forEach { stat ->
                                val color = when (stat.classification) {
                                    Classification.USELESS -> RedAccent
                                    Classification.KEEP -> GreenAccent
                                    Classification.UNCERTAIN -> YellowAccent
                                    else -> Color.Gray
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                                        Text(text = stat.classification.displayName, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    }
                                    Text(text = "${stat.count} 张", style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.categoryStats.isNotEmpty()) {
            item {
                AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600, 400)) + fadeIn(tween(600, 400))) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernSectionHeader(title = stringResource(R.string.stats_distribution), icon = Icons.Default.Category)
                            uiState.categoryStats.forEach { stat ->
                                val percentage = if (uiState.totalPhotos > 0) (stat.count.toFloat() / uiState.totalPhotos * 100).toInt() else 0
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = stat.classification.displayName, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        Text(text = "${stat.count} 张 ($percentage%)", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    }
                                    GradientProgressBar(progress = stat.count.toFloat() / uiState.totalPhotos.coerceAtLeast(1), modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedPieChart(data: Map<String, Int>, total: Float) {
    val animatedProgress by animateFloatAsState(targetValue = 1f, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "pie")
    Canvas(modifier = Modifier.size(180.dp)) {
        val strokeWidth = 30.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f
        data.forEach { (classification, count) ->
            val sweepAngle = (count / total) * 360f * animatedProgress
            val color = when (classification) {
                "无用" -> RedAccent
                "保留" -> GreenAccent
                "待定" -> YellowAccent
                else -> Color.Gray
            }
            drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2), style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            startAngle += sweepAngle
        }
    }
}
