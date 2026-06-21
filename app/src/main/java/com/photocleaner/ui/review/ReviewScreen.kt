package com.photocleaner.ui.review

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.photocleaner.domain.model.Classification
import com.photocleaner.ui.components.*
import com.photocleaner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Animated entrance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Delete confirmation dialog state
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeletePhotos by remember { mutableStateOf<List<com.photocleaner.domain.model.Photo>>(emptyList()) }
    var isPendingBatchDelete by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.isBatchMode && uiState.selectedPhotos.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        val selected = uiState.photos.filter { it.id in uiState.selectedPhotos }
                        if (selected.isNotEmpty()) {
                            pendingDeletePhotos = selected
                            isPendingBatchDelete = true
                            showDeleteConfirmDialog = true
                        }
                    },
                    containerColor = RedAccent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除选中")
                        Text(
                            "${uiState.selectedPhotos.size}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GradientStart, GradientMid, GradientEnd)
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar with animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 400)
                    ) + fadeIn(animationSpec = tween(durationMillis = 400))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BlueAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "照片审查",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row {
                            if (uiState.isBatchMode) {
                                IconButton(onClick = { viewModel.selectAll() }) {
                                    Icon(Icons.Default.SelectAll, "全选", tint = BlueAccent)
                                }
                                IconButton(onClick = { viewModel.deselectAll() }) {
                                    Icon(Icons.Default.Deselect, "取消全选", tint = Color.White)
                                }
                            }
                            IconButton(onClick = { viewModel.toggleBatchMode() }) {
                                Icon(
                                    if (uiState.isBatchMode) Icons.Default.Close else Icons.Default.Checklist,
                                    contentDescription = if (uiState.isBatchMode) "退出批量" else "批量模式",
                                    tint = if (uiState.isBatchMode) RedAccent else Color.White
                                )
                            }
                        }
                    }
                }

                // Filter chips with animation
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 400, delayMillis = 100)
                    ) + fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 100))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterType.entries.forEach { filter ->
                            val isSelected = uiState.filter == filter
                            val color = when (filter) {
                                FilterType.ALL -> BlueAccent
                                FilterType.USELESS -> RedAccent
                                FilterType.UNCERTAIN -> YellowAccent
                                FilterType.KEEP -> GreenAccent
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilter(filter) },
                                label = { 
                                    Text(
                                        filter.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.3f),
                                    selectedLabelColor = color,
                                    containerColor = DarkSurface.copy(alpha = 0.6f),
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Photo count
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 200))
                ) {
                    Text(
                        text = "共 ${uiState.photos.size} 张照片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 300))
                ) {
                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ModernLoadingIndicator(color = BlueAccent)
                        }
                    } else if (uiState.photos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ModernEmptyState(
                                icon = Icons.Default.PhotoLibrary,
                                title = "暂无照片",
                                subtitle = "请先进行扫描"
                            )
                        }
                    } else {
                        // Photo grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uiState.photos, key = { it.id }) { photo ->
                                val onToggleSelection = remember(photo.id) { { viewModel.toggleSelection(photo.id) } }
                                val onDelete = remember(photo.id) { {
                                    pendingDeletePhotos = listOf(photo)
                                    showDeleteConfirmDialog = true
                                } }
                                val onKeep = remember(photo.id) { { viewModel.keepPhoto(photo) } }
                                val onClick = remember(photo.id) { { viewModel.showDetail(photo) } }

                                PhotoCard(
                                    photo = photo,
                                    isBatchMode = uiState.isBatchMode,
                                    isSelected = photo.id in uiState.selectedPhotos,
                                    onToggleSelection = onToggleSelection,
                                    onDelete = onDelete,
                                    onKeep = onKeep,
                                    onClick = onClick
                                )
                            }
                        }
                    }
                }
            }

            // Undo snackbar with animation
            AnimatedVisibility(
                visible = uiState.showUndo,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = RedAccent
                            )
                            Text(
                                text = "已删除 ${uiState.lastDeletedPhotos.size} 张照片",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        TextButton(onClick = { viewModel.undoDelete() }) {
                            Text("撤销", color = BlueAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Full-screen detail dialog
    uiState.detailPhoto?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            onDismiss = { viewModel.hideDetail() },
            onDelete = {
                pendingDeletePhotos = listOf(photo)
                showDeleteConfirmDialog = true
                viewModel.hideDetail()
            },
            onKeep = {
                viewModel.keepPhoto(photo)
                viewModel.hideDetail()
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && pendingDeletePhotos.isNotEmpty()) {
        DeleteConfirmDialog(
            count = pendingDeletePhotos.size,
            onConfirm = {
                if (isPendingBatchDelete) {
                    viewModel.deleteSelected()
                } else {
                    pendingDeletePhotos.forEach { viewModel.deletePhoto(it) }
                }
                showDeleteConfirmDialog = false
                pendingDeletePhotos = emptyList()
                isPendingBatchDelete = false
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                pendingDeletePhotos = emptyList()
                isPendingBatchDelete = false
            }
        )
    }
}

@Composable
fun PhotoDetailDialog(
    photo: com.photocleaner.domain.model.Photo,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onKeep: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "关闭", tint = Color.White)
                }
                Text(
                    text = photo.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Bottom info and actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                // Photo info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = photo.classification.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = when (photo.classification) {
                                Classification.USELESS -> RedAccent
                                Classification.KEEP -> GreenAccent
                                Classification.UNCERTAIN -> YellowAccent
                                else -> Color.Gray
                            }
                        )
                        Text(
                            text = "${(photo.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${photo.width}×${photo.height}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Text(
                            text = com.photocleaner.util.ImageUtils.formatFileSize(photo.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onKeep,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GreenAccent
                        )
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保留")
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(RedAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RedAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "⚠️ 确认删除",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "即将删除 $count 张照片",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "删除后仍可在回收站中恢复。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = YellowAccent,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("确认删除", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("取消")
            }
        }
    )
}
