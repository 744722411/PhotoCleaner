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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BlueAccent, Purple80))), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.BarChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(text = "\uD83D\uDCCA 照片统计", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(text = "查看照片分类数据与存储分析", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        item {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600, 200)) + fadeIn(tween(600, 200))) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernStatCard(title = "照片总数", value = uiState.totalPhotos.toString(), icon = Icons.Default.PhotoLibrary, color = BlueAccent, modifier = Modifier.weight(1f))
                    ModernStatCard(title = "已分类", value = uiState.classifiedPhotos.toString(), icon = Icons.Default.CheckCircle, color = GreenAccent, modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600, 300)) + fadeIn(tween(600, 300))) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernStatCard(title = "无用照片", value = uiState.uselessPhotos.toString(), icon = Icons.Default.Delete, color = RedAccent, modifier = Modifier.weight(1f))
                    ModernStatCard(title = "可节省", value = uiState.spaceSaved, icon = Icons.Default.Storage, color = YellowAccent, modifier = Modifier.weight(1f))
                }
            }
        }

        if (uiState.categoryStats.isNotEmpty()) {
            item {
                AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600, 400)) + fadeIn(tween(600, 400))) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            ModernSectionHeader(title = "分类明细", icon = Icons.Default.PieChart)
                            Spacer(modifier = Modifier.height(16.dp))
                            val total = uiState.categoryStats.sumOf { it.count }.toFloat()
                            if (total > 0) {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    AnimatedPieChart(data = uiState.categoryStats.associate { it.classification.displayName to it.count }, total = total)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            uiState.categoryStats.forEach { stat ->
                                val color = when (stat.classification) {
                                    Classification.USELESS -> RedAccent
                                    Classification.KEEP -> GreenAccent
                                    Classification.UNCERTAIN -> YellowAccent
                                    else -> Color.Gray
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                AnimatedVisibility(visible = isVisible, enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600, 500)) + fadeIn(tween(600, 500))) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            ModernSectionHeader(title = "类别分布", icon = Icons.Default.Category)
                            Spacer(modifier = Modifier.height(16.dp))
                            uiState.categoryStats.forEach { stat ->
                                val percentage = if (uiState.totalPhotos > 0) (stat.count.toFloat() / uiState.totalPhotos * 100).toInt() else 0
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = stat.classification.displayName, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        Text(text = "${stat.count} 张 ($percentage%)", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
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
