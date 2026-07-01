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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photocleaner.R
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
    val appName = stringResource(R.string.home_app_name)
    val subtitle = stringResource(R.string.home_subtitle)
    val quickActions = stringResource(R.string.home_quick_actions)
    val recentPhotosTitle = stringResource(R.string.home_recent_photos)

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background
    val gradientColors = remember(primaryColor, secondaryColor, backgroundColor) {
        listOf(
            primaryColor.copy(alpha = 0.10f),
            secondaryColor.copy(alpha = 0.08f),
            backgroundColor
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = gradientColors)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BlueAccent, Purple80))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(appName, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.60f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    if (uiState.totalPhotos > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricPill(label = stringResource(R.string.home_stat_total), value = uiState.totalPhotos.toString(), color = BlueAccent, modifier = Modifier.weight(1f))
                            MetricPill(label = stringResource(R.string.home_stat_processed), value = uiState.processedPhotos.toString(), color = GreenAccent, modifier = Modifier.weight(1f))
                            MetricPill(label = stringResource(R.string.home_stat_useless), value = uiState.uselessPhotos.toString(), color = RedAccent, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.home_coverage, uiState.processingCoverage), style = MaterialTheme.typography.labelMedium, color = BlueAccent, fontWeight = FontWeight.Bold)
                            Text(uiState.spaceSaved, style = MaterialTheme.typography.labelLarge, color = YellowAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModernSectionHeader(quickActions, Icons.Default.Bolt)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernActionButton(title = stringResource(R.string.home_action_scan_title), subtitle = stringResource(R.string.home_action_scan_sub), icon = Icons.Default.Search, gradient = Brush.linearGradient(listOf(BlueAccent, Color(0xFF1E88E5))), onClick = onNavigateToScan, modifier = Modifier.weight(1f))
                    ModernActionButton(title = stringResource(R.string.home_action_review_title), subtitle = stringResource(R.string.home_action_review_sub), icon = Icons.Default.Preview, gradient = Brush.linearGradient(listOf(GreenAccent, Color(0xFF2E7D32))), onClick = onNavigateToReview, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernActionButton(title = stringResource(R.string.home_action_stats_title), subtitle = stringResource(R.string.home_action_stats_sub), icon = Icons.Default.BarChart, gradient = Brush.linearGradient(listOf(Purple80, Color(0xFF7B1FA2))), onClick = onNavigateToStats, modifier = Modifier.weight(1f))
                    ModernActionButton(title = stringResource(R.string.home_action_settings_title), subtitle = stringResource(R.string.home_action_settings_sub), icon = Icons.Default.Settings, gradient = Brush.linearGradient(listOf(YellowAccent, Color(0xFFF57F17))), onClick = onNavigateToSettings, modifier = Modifier.weight(1f))
                }
            }
        }

        if (uiState.recentPhotos.isNotEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernSectionHeader(recentPhotosTitle, Icons.Default.History)
                        uiState.recentPhotos.take(4).forEach { photo ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(text = photo.displayName, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = "${photo.width}x${photo.height}", color = Color.White.copy(alpha = 0.45f), style = MaterialTheme.typography.labelSmall)
                                }
                                ModernClassificationBadge(classification = photo.classification.displayName, confidence = photo.confidence, color = when (photo.classification) {
                                    Classification.USELESS -> RedAccent
                                    Classification.KEEP -> GreenAccent
                                    Classification.UNCERTAIN -> YellowAccent
                                    else -> Color.Gray
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
