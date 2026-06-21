package com.photocleaner.ui.scan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photocleaner.data.repository.DirectoryInfo
import com.photocleaner.ui.components.*
import com.photocleaner.ui.theme.*

@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GradientStart, GradientMid, GradientEnd)))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 48.dp, bottom = 32.dp)
        ) {
            // Header
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = scaleIn(initialScale = 0.8f, animationSpec = tween(600)) + fadeIn(tween(600))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val infiniteTransition = rememberInfiniteTransition(label = "scan")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
                            label = "rotation"
                        )
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BlueAccent, Purple80))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp).rotate(if (uiState.isScanning || uiState.isClassifying) rotation else 0f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "\uD83D\uDD0D 照片扫描", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Main content based on state
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(600, 200)) + fadeIn(tween(600, 200))
                ) {
                    when {
                        !uiState.isScanning && !uiState.scanComplete && !uiState.isClassifying && !uiState.classifyComplete && !uiState.isPaused -> ScanReadyContent(
                            selectedDirectories = uiState.selectedDirectories,
                            discoveredDirectories = uiState.discoveredDirectories,
                            isDiscovering = uiState.isDiscoveringDirs,
                            batchSize = uiState.batchSize,
                            onToggleDir = { viewModel.toggleDirectory(it) },
                            onSelectAll = { viewModel.selectAllDirectories() },
                            onDeselectAll = { viewModel.deselectAllDirectories() },
                            onRefreshDirs = { viewModel.discoverDirectories() },
                            onSaveDirs = { viewModel.saveDirectories() },
                            onBatchSizeChange = { viewModel.setBatchSize(it) },
                            onStartScan = { viewModel.startScan() }
                        )
                        (uiState.isScanning || uiState.isPaused) && !uiState.isClassifying -> ScanProgressContent(
                            uiState,
                            onPauseScan = { viewModel.pauseScan() },
                            onResumeScan = { viewModel.resumeScan() },
                            onStopScan = { viewModel.stopScan() }
                        )
                        uiState.isClassifying || uiState.isClassifyPaused -> ClassifyProgressContent(
                            uiState,
                            onPauseClassify = { viewModel.pauseClassify() },
                            onResumeClassify = { viewModel.resumeClassify() },
                            onStopClassify = { viewModel.stopScan() }
                        )
                        uiState.classifyComplete -> ClassifyCompleteContent(uiState) { viewModel.reset() }
                    }
                }
            }

            // Error display
            uiState.error?.let { error ->
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = RedAccent.copy(alpha = 0.2f)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = RedAccent)
                            Text(text = error, color = RedAccent, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Scan log display
            if (uiState.scanLogs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    ScanLogPanel(uiState.scanLogs)
                }
            }
        }
    }
}

@Composable
fun ScanLogPanel(logs: List<ScanLogEntry>) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(18.dp))
            Text(
                text = "实时日志 (${logs.size})",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117).copy(alpha = 0.9f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { entry ->
                    ScanLogItem(entry)
                }
            }
        }
    }
}

@Composable
fun ScanLogItem(entry: ScanLogEntry) {
    val (icon, color, prefix) = when (entry.status) {
        LogStatus.PROCESSING -> Triple("⏳", YellowAccent, "")
        LogStatus.SUCCESS -> Triple("✅", GreenAccent, "")
        LogStatus.LOCAL_HIT -> Triple("⚠️", YellowAccent, "")
        LogStatus.SKIP -> Triple("⏭️", Color.Gray, "")
        LogStatus.ERROR -> Triple("❌", RedAccent, "")
        LogStatus.INFO -> Triple("ℹ️", BlueAccent, "")
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = icon,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 1.dp)
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
            color = Color.White.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = if (entry.photoName.isNotBlank()) Modifier.weight(0.65f) else Modifier.weight(1f)
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
    onSaveDirs: () -> Unit,
    onBatchSizeChange: (Int) -> Unit,
    onStartScan: () -> Unit
) {
    val allPaths = discoveredDirectories.map { it.relativePath }.toSet()
    val isAllSelected = allPaths.isNotEmpty() && selectedDirectories.containsAll(allPaths)
    val totalCount = discoveredDirectories.sumOf { it.imageCount }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(64.dp), tint = BlueAccent)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "选择扫描目录", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "自动发现设备上的所有图片目录，选择要扫描的目录",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (isDiscovering) {
                // Loading state
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp), color = BlueAccent, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "正在扫描设备目录...", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                }
            } else if (discoveredDirectories.isEmpty()) {
                // No directories found / first time
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "点击下方按钮扫描设备", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onRefreshDirs,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueAccent)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("扫描设备目录")
                    }
                }
            } else {
                // Summary
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BlueAccent.copy(alpha = 0.12f)).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "共发现 ${discoveredDirectories.size} 个目录",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${formatCount(totalCount)} 张图片",
                        color = BlueAccent,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Select All / Deselect All / Refresh buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = if (isAllSelected) onDeselectAll else onSelectAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(if (isAllSelected) Icons.Default.CheckBoxOutlineBlank else Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAllSelected) "全不选" else "全选", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onRefreshDirs,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Directory list
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
                                .background(if (isSelected) BlueAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { onToggleDir(dir.relativePath) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dir.displayName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = dir.relativePath,
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                text = "${formatCount(dir.imageCount)}张",
                                color = if (isSelected) BlueAccent else Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (discoveredDirectories.isNotEmpty()) {
                Text(
                    text = "已选 ${selectedDirectories.size} 个目录 (${formatCount(discoveredDirectories.filter { selectedDirectories.contains(it.relativePath) }.sumOf { it.imageCount })} 张)",
                    style = MaterialTheme.typography.bodySmall,
                    color = BlueAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            // Batch size selector
            if (discoveredDirectories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "每次处理数量",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(50, 100, 200, 500, 0) // 0 = all
                        val labels = listOf("50", "100", "200", "500", "全部")
                        options.forEachIndexed { index, value ->
                            val isSelected = batchSize == value
                            val selectedTotal = discoveredDirectories.filter { selectedDirectories.contains(it.relativePath) }.sumOf { it.imageCount }
                            val displayCount = if (value == 0) selectedTotal else minOf(value, selectedTotal)
                            OutlinedButton(
                                onClick = { onBatchSizeChange(value) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSelected) BlueAccent else Color.White.copy(alpha = 0.5f),
                                    containerColor = if (isSelected) BlueAccent.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) BlueAccent else Color.White.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(labels[index], fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onSaveDirs()
                    onStartScan()
                },
                enabled = selectedDirectories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (selectedDirectories.isNotEmpty())
                                Brush.horizontalGradient(listOf(BlueAccent, Color(0xFF1E88E5)))
                            else
                                Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("开始扫描", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(60.dp), color = BlueAccent, strokeWidth = 5.dp, trackColor = BlueAccent.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if (uiState.isPaused) "已暂停" else "正在扫描...", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.totalToScan > 0) {
                GradientProgressBar(progress = uiState.scannedCount.toFloat() / uiState.totalToScan, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "已扫描", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                    Text(text = "${uiState.scannedCount} / ${uiState.totalToScan}", style = MaterialTheme.typography.titleMedium, color = BlueAccent, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = if (uiState.isPaused) onResumeScan else onPauseScan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = YellowAccent)
                ) {
                    Icon(if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uiState.isPaused) "继续" else "暂停")
                }
                OutlinedButton(
                    onClick = onStopScan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("停止")
                }
            }
        }
    }
}

@Composable
fun ScanCompleteContent(uiState: ScanUiState, onStartClassify: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(GreenAccent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(36.dp), tint = GreenAccent)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "扫描完成！", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "共扫描 ${uiState.totalToScan} 张照片\n本地检测发现 ${uiState.uselessFound} 张疑似无用照片", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onStartClassify,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(GreenAccent, Color(0xFF2E7D32)))), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Psychology, contentDescription = null)
                        Text("AI 智能分类", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
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
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(60.dp), color = GreenAccent, strokeWidth = 5.dp, trackColor = GreenAccent.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if (uiState.isClassifyPaused) "AI 分类已暂停" else "AI 正在分类...", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.totalToClassify > 0) {
                GradientProgressBar(progress = uiState.classifiedCount.toFloat() / uiState.totalToClassify, modifier = Modifier.fillMaxWidth(), gradient = Brush.horizontalGradient(listOf(GreenAccent, Color(0xFF2E7D32))))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "已分类", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                    Text(text = "${uiState.classifiedCount} / ${uiState.totalToClassify}", style = MaterialTheme.typography.titleMedium, color = GreenAccent, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = if (uiState.isClassifyPaused) onResumeClassify else onPauseClassify,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = YellowAccent)
                ) {
                    Icon(if (uiState.isClassifyPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uiState.isClassifyPaused) "继续" else "暂停")
                }
                OutlinedButton(
                    onClick = onStopClassify,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("停止")
                }
            }
        }
    }
}

@Composable
fun ClassifyCompleteContent(uiState: ScanUiState, onReset: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(YellowAccent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(36.dp), tint = YellowAccent)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "分类完成！", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "发现 ${uiState.uselessFound} 张无用照片\n前往\"审查\"页面查看并清理", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("重新扫描", fontSize = 16.sp)
                }
            }
        }
    }
}
