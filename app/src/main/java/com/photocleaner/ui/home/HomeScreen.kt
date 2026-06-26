package com.photocleaner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photocleaner.domain.model.Classification
import com.photocleaner.ui.components.GlassCard
import com.photocleaner.ui.components.ModernActionButton
import com.photocleaner.ui.components.ModernClassificationBadge
import com.photocleaner.ui.components.ModernSectionHeader
import com.photocleaner.ui.components.ModernStatCard
import com.photocleaner.ui.theme.BlueAccent
import com.photocleaner.ui.theme.GreenAccent
import com.photocleaner.ui.theme.Purple80
import com.photocleaner.ui.theme.RedAccent
import com.photocleaner.ui.theme.YellowAccent

@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val gradientColors = remember {
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(BlueAccent, Purple80))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "照片清理助手",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "扫描目录、本地检测、逐项确认删除",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                            if (uiState.totalPhotos > 0) {
                                Text(
                                    "当前覆盖率 ${uiState.classificationCoverage}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BlueAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    if (uiState.totalPhotos > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "最近入库",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                uiState.recentPhotos.firstOrNull()?.displayName ?: "${uiState.totalPhotos} 张",
                                style = MaterialTheme.typography.bodySmall,
                                color = BlueAccent,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernStatCard(
                    title = "总数",
                    value = uiState.totalPhotos.toString(),
                    icon = Icons.Default.PhotoLibrary,
                    color = BlueAccent,
                    modifier = Modifier.weight(1f)
                )
                ModernStatCard(
                    title = "已分类",
                    value = uiState.classifiedPhotos.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = GreenAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernStatCard(
                    title = "建议清理",
                    value = uiState.uselessPhotos.toString(),
                    icon = Icons.Default.Delete,
                    color = RedAccent,
                    modifier = Modifier.weight(1f)
                )
                ModernStatCard(
                    title = "可节省",
                    value = uiState.spaceSaved,
                    icon = Icons.Default.Storage,
                    color = YellowAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernSectionHeader("快捷入口", Icons.Default.Bolt)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernActionButton(
                        title = "开始扫描",
                        subtitle = "选择目录并读取本地照片",
                        icon = Icons.Default.Search,
                        gradient = Brush.linearGradient(listOf(BlueAccent, Color(0xFF1E88E5))),
                        onClick = onNavigateToScan,
                        modifier = Modifier.weight(1f)
                    )
                    ModernActionButton(
                        title = "审查照片",
                        subtitle = "确认保留项和待删除项",
                        icon = Icons.Default.Preview,
                        gradient = Brush.linearGradient(listOf(GreenAccent, Color(0xFF2E7D32))),
                        onClick = onNavigateToReview,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernActionButton(
                        title = "查看统计",
                        subtitle = "检查分类分布和空间收益",
                        icon = Icons.Default.BarChart,
                        gradient = Brush.linearGradient(listOf(Purple80, Color(0xFF7B1FA2))),
                        onClick = onNavigateToStats,
                        modifier = Modifier.weight(1f)
                    )
                    ModernActionButton(
                        title = "隐私设置",
                        subtitle = "查看本地处理和离线状态",
                        icon = Icons.Default.Settings,
                        gradient = Brush.linearGradient(listOf(YellowAccent, Color(0xFFF57F17))),
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (uiState.recentPhotos.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernSectionHeader("最近照片", Icons.Default.History)
                        uiState.recentPhotos.forEach { photo ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = photo.displayName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${photo.width}×${photo.height}",
                                        color = Color.White.copy(alpha = 0.45f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                ModernClassificationBadge(
                                    classification = photo.classification.displayName,
                                    confidence = photo.confidence,
                                    color = when (photo.classification) {
                                        Classification.USELESS -> RedAccent
                                        Classification.KEEP -> GreenAccent
                                        Classification.UNCERTAIN -> YellowAccent
                                        else -> Color.Gray
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
