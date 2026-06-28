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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photocleaner.R
import com.photocleaner.domain.model.DirectoryInfo
import com.photocleaner.service.LogStatus
import com.photocleaner.service.ScanLogEntry
import com.photocleaner.service.ScanUiState
import com.photocleaner.ui.components.GlassCard
import com.photocleaner.ui.components.GradientProgressBar
import com.photocleaner.ui.theme.BlueAccent
import com.photocleaner.ui.theme.GreenAccent
import com.photocleaner.ui.theme.Purple80
import com.photocleaner.ui.theme.RedAccent
import com.photocleaner.ui.theme.YellowAccent
import com.photocleaner.util.PermissionHelper
import kotlinx.coroutines.launch

@Composable
fun ScanScreen(
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionMissingMsg = stringResource(R.string.permission_missing_scan)
    val grantLabel = stringResource(R.string.permission_grant)

    fun startScanOrRequestPermission() {
        if (PermissionHelper.hasStoragePermission(context)) {
            viewModel.startScan()
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = permissionMissingMsg,
                    actionLabel = grantLabel,
                    withDismissAction = true
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    onRequestPermission()
                }
            }
        }
    }

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
        item { ScanHeader(uiState = uiState) }
        item { ScanSummary(uiState = uiState) }
        item {
            when {
                !uiState.isScanning && !uiState.scanComplete && !uiState.isProcessing && !uiState.processingComplete && !uiState.isPaused -> {
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
                        onStartScan = { startScanOrRequestPermission() }
                    )
                }
                (uiState.isScanning || uiState.isPaused) && !uiState.isProcessing -> {
                    ScanProgressContent(uiState = uiState, onPauseScan = viewModel::pauseScan, onResumeScan = viewModel::resumeScan, onStopScan = viewModel::stopScan)
                }
                uiState.isProcessing || uiState.isProcessingPaused -> {
                    ProcessingProgressContent(uiState = uiState, onPauseProcessing = viewModel::pauseScan, onResumeProcessing = viewModel::resumeScan, onStopProcessing = viewModel::stopScan)
                }
                uiState.processingComplete -> {
                    ProcessingCompleteContent(uiState = uiState, onReset = viewModel::reset)
                }
            }
        }
        uiState.error?.let { error -> item { Surface(color = RedAccent.copy(alpha = 0.14f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Default.Error, contentDescription = null, tint = RedAccent); Text(error, color = RedAccent, style = MaterialTheme.typography.bodyMedium) } } } }
        if (uiState.scanLogs.isNotEmpty()) { item { ScanLogPanel(logs = uiState.scanLogs) } }
    }
}

@Composable
private fun ScanHeader(uiState: ScanUiState) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(BlueAccent, Purple80))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) }
            Column {
                Text(stringResource(R.string.scan_title), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = when {
                    uiState.isProcessing || uiState.isProcessingPaused -> stringResource(R.string.scan_subtitle_classifying)
                    uiState.isScanning || uiState.isPaused -> stringResource(R.string.scan_subtitle_scanning)
                    uiState.processingComplete -> stringResource(R.string.scan_subtitle_complete)
                    else -> stringResource(R.string.scan_subtitle_ready)
                }, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun ScanSummary(uiState: ScanUiState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("已选目录", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelMedium); Text("${uiState.selectedDirectories.size} 个", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("扫描进度", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelMedium); Text("${uiState.scannedCount}/${uiState.totalToScan}", color = BlueAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Column(horizontalAlignment = Alignment.End) { Text("处理进度", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelMedium); Text("${uiState.processedCount}/${uiState.totalToProcess}", color = GreenAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ScanLogPanel(logs: List<ScanLogEntry>) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1) }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(18.dp))
                Text("处理日志", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${logs.size} 条", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            }
            Surface(color = Color(0xFF0D1117).copy(alpha = 0.92f), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                LazyColumn(state = listState, modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(logs, key = { entry -> entry.timestamp }) { entry -> ScanLogItem(entry = entry) }
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
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.padding(top = 1.dp).size(14.dp))
        if (entry.photoName.isNotBlank()) {
            Text(text = entry.photoName, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(0.35f, fill = false))
        }
        Text(text = entry.message, fontSize = 11.sp, color = Color.White.copy(alpha = 0.72f), fontFamily = FontFamily.Monospace, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = if (entry.photoName.isNotBlank()) Modifier.weight(0.65f) else Modifier.weight(1f))
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
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.scan_scope), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.scan_scope_hint), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
            if (discoveredDirectories.isEmpty() && isDiscovering) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BlueAccent)
                    Text(stringResource(R.string.scan_discovering), color = Color.White.copy(alpha = 0.8f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefreshDirs) { Icon(Icons.Default.Refresh, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.scan_discover_dirs)) }
                OutlinedButton(onClick = if (isAllSelected) onDeselectAll else onSelectAll, enabled = discoveredDirectories.isNotEmpty()) { Icon(if (isAllSelected) Icons.Default.CheckBoxOutlineBlank else Icons.Default.CheckBox, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (isAllSelected) stringResource(R.string.scan_deselect_all) else stringResource(R.string.scan_select_all)) }
            }
            Button(onClick = onStartScan, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Search, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.scan_start)) }
        }
    }
}
