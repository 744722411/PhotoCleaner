package com.photocleaner.ui.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photocleaner.domain.model.DirectoryInfo
import com.photocleaner.ui.components.GlassCard
import com.photocleaner.ui.components.GradientProgressBar
import com.photocleaner.ui.theme.BlueAccent
import com.photocleaner.ui.theme.GreenAccent
import com.photocleaner.ui.theme.Purple80
import com.photocleaner.ui.theme.RedAccent
import com.photocleaner.ui.theme.YellowAccent

@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel()
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
            ScanHeader(uiState = uiState)
        }

        item {
            ScanSummary(uiState = uiState)
        }

        item {
            when {
                !uiState.isScanning &&
                    !uiState.scanComplete &&
                    !uiState.isClassifying &&
                    !uiState.classifyComplete &&
                    !uiState.isPaused -> {
                    ScanReadyContent(
                        selectedDirectories = uiState.selectedDirectories,
                        discoveredDirectories = uiState.discoveredDirectories,
                        isDiscovering = uiState.isDiscoveringDirs,
                        batchSize = uiState.batchSize,
                        onToggleDir = viewModel::toggleDirectory,
                        onSelectAll = viewModel::selectAllDirectories,
                        onDeselectAll = viewModel::deselectAllDirectories,
                        onRefreshDirs = viewModel::discoverDirectories,
                        onBatchSizeChange = viewModel::setBatchSize,
                        onStartScan = viewModel::startScan
                    )
                }

                (uiState.isScanning || uiState.isPaused) && !uiState.isClassifying -> {
                    ScanProgressContent(
                        uiState = uiState,
                        onPauseScan = viewModel::pauseScan,
                        onResumeScan = viewModel::resumeScan,
                        onStopScan = viewModel::stopScan
                    )
                }

                uiState.isClassifying || uiState.isClassifyPaused -> {
                    ClassifyProgressContent(
                        uiState = uiState,
                        onPauseClassify = viewModel::pauseScan,
                        onResumeClassify = viewModel::resumeScan,
                        onStopClassify = viewModel::stopScan
                    )
                }

                uiState.classifyComplete -> {
                    ClassifyCompleteContent(
                        uiState = uiState,
                        onReset = viewModel::reset
                    )
                }
            }
        }

        uiState.error?.let { error ->
            item {
                Surface(
                    color = RedAccent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = RedAccent)
                        Text(error, color = RedAccent, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (uiState.scanLogs.isNotEmpty()) {
            item {
                ScanLogPanel(logs = uiState.scanLogs)
            }
        }
    }
}

@Composable
private fun ScanHeader(uiState: ScanUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BlueAccent, Purple80))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
            }
            Column {
                Text(
                    "照片扫描",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        uiState.isClassifying || uiState.isClassifyPaused -> "正在完成本地检测"
                        uiState.isScanning || uiState.isPaused -> "正在读取和检测本地照片"
                        uiState.classifyComplete -> "本轮处理已完成"
                        else -> "选择目录并开始新一轮扫描"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ScanSummary(uiState: ScanUiState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "已选目录",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "${uiState.selectedDirectories.size} 个",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "扫描进度",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "${uiState.scannedCount}/${uiState.totalToScan}",
                    color = BlueAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "处理进度",
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "${uiState.classifiedCount}/${uiState.totalToClassify}",
                    color = GreenAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ScanLogPanel(logs: List<ScanLogEntry>) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = null,
                    tint = BlueAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "处理日志",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${logs.size} 条",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Surface(
                color = Color(0xFF0D1117).copy(alpha = 0.92f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs) { entry ->
                        ScanLogItem(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun ScanLogItem(entry: ScanLogEntry) {
    val (icon, color): Pair<ImageVector, Color> = when (entry.status) {
        LogStatus.PROCESSING -> Icons.Default.HourglassTop to YellowAccent
        LogStatus.SUCCESS -> Icons.Default.CheckCircle to GreenAccent
        LogStatus.LOCAL_HIT -> Icons.Default.TipsAndUpdates to YellowAccent
        LogStatus.SKIP -> Icons.Default.SkipNext to Color.Gray
        LogStatus.ERROR -> Icons.Default.Error to RedAccent
        LogStatus.INFO -> Icons.Default.Info to BlueAccent
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(14.dp)
        )
        if (entry.photoName.isNotBlank()) {
            Text(
                text = entry.photoName,
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.35f, fill = false)
            )
        }
        Text(
            text = entry.message,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.72f),
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = if (entry.photoName.isNotBlank()) {
                Modifier.weight(0.65f)
            } else {
                Modifier.weight(1f)
            }
        )
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 10000 -> "${"%.1f".format(count / 10000.0)}万"
        count >= 1000 -> "${"%.1f".format(count / 1000.0)}k"
        else -> "$count"
    }
}

@Composable
fun ScanReadyContent(
    selectedDirectories: Set<String>,
    discoveredDirectories: List<DirectoryInfo>,
    isDiscovering: Boolean,
    batchSize: Int,
    onToggleDir: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRefreshDirs: () -> Unit,
    onBatchSizeChange: (Int) -> Unit,
    onStartScan: () -> Unit
) {
    val allPaths = discoveredDirectories.map { it.relativePath }.toSet()
    val isAllSelected = allPaths.isNotEmpty() && selectedDirectories.containsAll(allPaths)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "扫描范围",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "先确认本轮要覆盖的目录，再决定处理数量。",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )

            when {
                isDiscovering -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = BlueAccent, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("正在扫描设备目录...", color = Color.White.copy(alpha = 0.7f))
                    }
                }

                discoveredDirectories.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("先扫描设备目录", color = Color.White, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onRefreshDirs) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("发现目录")
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = if (isAllSelected) onDeselectAll else onSelectAll,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (isAllSelected) {
                                    Icons.Default.CheckBoxOutlineBlank
                                } else {
                                    Icons.Default.CheckBox
                                },
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAllSelected) "全不选" else "全选")
                        }
                        OutlinedButton(onClick = onRefreshDirs) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        discoveredDirectories.forEach { dir ->
                            val isSelected = selectedDirectories.contains(dir.relativePath)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) {
                                            BlueAccent.copy(alpha = 0.14f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .clickable { onToggleDir(dir.relativePath) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleDir(dir.relativePath) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = BlueAccent,
                                        uncheckedColor = Color.White.copy(alpha = 0.4f),
                                        checkmarkColor = Color.White
                                    )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        dir.displayName,
                                        color = Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        dir.relativePath,
                                        color = Color.White.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    "${formatCount(dir.imageCount)} 张",
                                    color = if (isSelected) BlueAccent else Color.White.copy(alpha = 0.45f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    Text(
                        text = "已选 ${selectedDirectories.size} 个目录，共 ${
                            formatCount(
                                discoveredDirectories
                                    .filter { selectedDirectories.contains(it.relativePath) }
                                    .sumOf { it.imageCount }
                            )
                        } 张",
                        color = BlueAccent,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "每次处理数量",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(50, 100, 200, 500, 0)
                            options.forEach { value ->
                                val isSelected = batchSize == value
                                OutlinedButton(
                                    onClick = { onBatchSizeChange(value) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isSelected) {
                                            BlueAccent
                                        } else {
                                            Color.White.copy(alpha = 0.6f)
                                        },
                                        containerColor = if (isSelected) {
                                            BlueAccent.copy(alpha = 0.14f)
                                        } else {
                                            Color.Transparent
                                        }
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) BlueAccent else Color.White.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Text(if (value == 0) "全部" else value.toString())
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onStartScan,
                enabled = selectedDirectories.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("开始扫描")
            }
        }
    }
}

@Composable
fun ScanProgressContent(
    uiState: ScanUiState,
    onPauseScan: () -> Unit,
    onResumeScan: () -> Unit,
    onStopScan: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (uiState.isPaused) "扫描已暂停" else "正在扫描",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (uiState.totalToScan > 0) {
                GradientProgressBar(
                    progress = uiState.scannedCount.toFloat() / uiState.totalToScan,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("已扫描", color = Color.White.copy(alpha = 0.6f))
                    Text(
                        "${uiState.scannedCount} / ${uiState.totalToScan}",
                        color = BlueAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = if (uiState.isPaused) onResumeScan else onPauseScan,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isPaused) "继续" else "暂停")
                }
                OutlinedButton(
                    onClick = onStopScan,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("停止")
                }
            }
        }
    }
}

@Composable
fun ClassifyProgressContent(
    uiState: ScanUiState,
    onPauseClassify: () -> Unit,
    onResumeClassify: () -> Unit,
    onStopClassify: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (uiState.isClassifyPaused) "处理已暂停" else "正在处理",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (uiState.totalToClassify > 0) {
                GradientProgressBar(
                    progress = uiState.classifiedCount.toFloat() / uiState.totalToClassify,
                    modifier = Modifier.fillMaxWidth(),
                    gradient = Brush.horizontalGradient(listOf(GreenAccent, Color(0xFF2E7D32)))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("已处理", color = Color.White.copy(alpha = 0.6f))
                    Text(
                        "${uiState.classifiedCount} / ${uiState.totalToClassify}",
                        color = GreenAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = if (uiState.isClassifyPaused) onResumeClassify else onPauseClassify,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (uiState.isClassifyPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isClassifyPaused) "继续" else "暂停")
                }
                OutlinedButton(
                    onClick = onStopClassify,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("停止")
                }
            }
        }
    }
}

@Composable
fun ClassifyCompleteContent(
    uiState: ScanUiState,
    onReset: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(YellowAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Celebration,
                    contentDescription = null,
                    tint = YellowAccent,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                "本轮处理完成",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "共发现 ${uiState.uselessFound} 张建议清理的照片，接下来可以去审查页逐项确认。",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
            if (uiState.totalToClassify > 0) {
                Text(
                    text = "已完成 ${uiState.classifiedCount} / ${uiState.totalToClassify} 张本地检测",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GreenAccent,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("重新扫描")
            }
        }
    }
}
