package com.photocleaner.ui.review

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.photocleaner.R
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.ui.components.GlassCard
import com.photocleaner.ui.components.ModernEmptyState
import com.photocleaner.ui.components.ModernLoadingIndicator
import com.photocleaner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onTrashConfirmed()
        } else {
            viewModel.onTrashCanceled()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is ReviewEvent.LaunchTrashIntent -> {
                    val request = androidx.activity.result.IntentSenderRequest.Builder(
                        event.pendingIntent.intentSender
                    ).build()
                    trashLauncher.launch(request)
                }
            }
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.commitPendingDeletes()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, withDismissAction = true)
            viewModel.clearError()
        }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeletePhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var isPendingBatchDelete by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    val gradientColors = remember {
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.isBatchMode && uiState.selectedPhotos.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        val selected = uiState.photos.filter { it.id in uiState.selectedPhotos }
                        pendingDeletePhotos = selected
                        isPendingBatchDelete = true
                        showDeleteConfirmDialog = selected.isNotEmpty()
                    },
                    containerColor = RedAccent,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(colors = gradientColors))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ReviewHeader(
                    uiState = uiState,
                    isGridView = isGridView,
                    onToggleView = { isGridView = !isGridView },
                    onToggleBatchMode = { viewModel.toggleBatchMode() },
                    onSelectAll = { viewModel.selectAll() },
                    onDeselectAll = { viewModel.deselectAll() }
                )

                ReviewFilterRow(
                    selected = uiState.filter,
                    onSelect = { viewModel.setFilter(it) }
                )

                ReviewSummary(
                    uiState = uiState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ModernLoadingIndicator(color = BlueAccent)
                            }
                        }

                        uiState.filter == FilterType.SIMILAR -> {
                            if (uiState.similarGroups.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    ModernEmptyState(
                                        icon = Icons.Default.CheckCircle,
                                        title = stringResource(R.string.review_empty_similar_title),
                                        subtitle = stringResource(R.string.review_empty_similar_sub)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(uiState.similarGroups, key = { group -> group.firstOrNull()?.id ?: group.hashCode() }) { group ->
                                        SimilarGroupCard(
                                            group = group,
                                            onKeepBest = { viewModel.keepBestInGroup(group) },
                                            onPhotoClick = { viewModel.showDetail(it) }
                                        )
                                    }
                                }
                            }
                        }

                        uiState.photos.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ModernEmptyState(
                                    icon = Icons.Default.CheckCircle,
                                    title = stringResource(R.string.review_empty_title),
                                    subtitle = stringResource(R.string.review_empty_sub)
                                )
                            }
                        }

                        isGridView -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(uiState.photos, key = { it.id }) { photo ->
                                    PhotoCard(
                                        photo = photo,
                                        isBatchMode = uiState.isBatchMode,
                                        isSelected = photo.id in uiState.selectedPhotos,
                                        onToggleSelection = { viewModel.toggleSelection(photo.id) },
                                        onDelete = {
                                            pendingDeletePhotos = listOf(photo)
                                            isPendingBatchDelete = false
                                            showDeleteConfirmDialog = true
                                        },
                                        onKeep = { viewModel.keepPhoto(photo) },
                                        onClick = { viewModel.showDetail(photo) }
                                    )
                                }
                            }
                        }

                        else -> {
                            ReviewSwipeContent(
                                photos = uiState.photos,
                                onDelete = { photo ->
                                    pendingDeletePhotos = listOf(photo)
                                    isPendingBatchDelete = false
                                    showDeleteConfirmDialog = true
                                },
                                onKeep = { viewModel.keepPhoto(it) }
                            )
                        }
                    }
                }
            }

        ReviewPendingDeleteBanner(
                uiState = uiState,
                onUndo = { viewModel.undoDelete() },
                onCommit = { viewModel.commitPendingDeletes() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    uiState.detailPhoto?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            onDismiss = { viewModel.hideDetail() },
            onDelete = {
                pendingDeletePhotos = listOf(photo)
                isPendingBatchDelete = false
                showDeleteConfirmDialog = true
                viewModel.hideDetail()
            },
            onKeep = {
                viewModel.keepPhoto(photo)
                viewModel.hideDetail()
            }
        )
    }

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
private fun ReviewHeader(
    uiState: ReviewUiState,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    onToggleBatchMode: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
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
                    .background(BlueAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Preview, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(stringResource(R.string.review_title), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(if (uiState.isBatchMode) R.string.review_subtitle_batch else R.string.review_subtitle_normal),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (uiState.filter != FilterType.SIMILAR) {
                IconButton(onClick = onToggleView) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.Style else Icons.Default.GridView,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            if (uiState.isBatchMode && uiState.filter != FilterType.SIMILAR) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = null, tint = BlueAccent)
                }
                IconButton(onClick = onDeselectAll) {
                    Icon(Icons.Default.ClearAll, contentDescription = null, tint = Color.White)
                }
            }
            if (uiState.filter != FilterType.SIMILAR) {
                IconButton(onClick = onToggleBatchMode) {
                    Icon(
                        imageVector = if (uiState.isBatchMode) Icons.Default.Close else Icons.Default.Checklist,
                        contentDescription = null,
                        tint = if (uiState.isBatchMode) RedAccent else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewFilterRow(
    selected: FilterType,
    onSelect: (FilterType) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = FilterType.entries.toList(),
            key = { it.name }
        ) { filter ->
            val color = when (filter) {
                FilterType.ALL -> BlueAccent
                FilterType.SIMILAR -> YellowAccent
                FilterType.LARGE -> Purple80
                FilterType.USELESS -> RedAccent
                FilterType.UNCERTAIN -> Color(0xFFFF9800)
                FilterType.KEEP -> GreenAccent
            }
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(filter.displayRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.24f),
                    selectedLabelColor = color,
                    containerColor = DarkSurface.copy(alpha = 0.55f),
                    labelColor = Color.White.copy(alpha = 0.75f)
                )
            )
        }
    }
}

@Composable
private fun ReviewSummary(
    uiState: ReviewUiState,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(if (uiState.filter == FilterType.SIMILAR) R.string.review_summary_similar else R.string.review_summary_result),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Text(
                    text = if (uiState.filter == FilterType.SIMILAR) {
                        stringResource(R.string.review_group_count, uiState.similarGroups.size)
                    } else {
                        stringResource(R.string.review_photo_count, uiState.photos.size)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            if (uiState.isBatchMode) {
                    Text(
                        text = stringResource(R.string.review_selected, uiState.selectedPhotos.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BlueAccent,
                        fontWeight = FontWeight.Bold
                    )
                } else if (uiState.lastDeletedPhotos.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.review_pending, uiState.lastDeletedPhotos.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = YellowAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReviewSwipeContent(
    photos: List<Photo>,
    onDelete: (Photo) -> Unit,
    onKeep: (Photo) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.Center
    ) {
        val topPhoto = photos.first()
        if (photos.size > 1) {
            val nextPhoto = photos[1]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .scale(0.96f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.45f))
            ) {
                AsyncImage(
                    model = remember(nextPhoto.uri) {
                        ImageRequest.Builder(LocalContext.current)
                            .data(nextPhoto.uri)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.48f
                )
            }
        }

        SwipeablePhotoCard(
            photo = topPhoto,
            onSwipedLeft = { onDelete(topPhoto) },
            onSwipedRight = { onKeep(topPhoto) }
        )
    }
}

@Composable
private fun ReviewPendingDeleteBanner(
    uiState: ReviewUiState,
    onUndo: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = uiState.showUndo || uiState.lastDeletedPhotos.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.showUndo) Icons.Default.Delete else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (uiState.showUndo) RedAccent else YellowAccent
                    )
                    Text(
                        text = if (uiState.showUndo) {
                            stringResource(R.string.review_marked, uiState.lastDeletedPhotos.size)
                        } else {
                            stringResource(R.string.review_pending_submit, uiState.lastDeletedPhotos.size)
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onUndo) {
                        Text(stringResource(R.string.undo), color = BlueAccent, fontWeight = FontWeight.Bold)
                    }
                    if (!uiState.showUndo && uiState.lastDeletedPhotos.isNotEmpty()) {
                        Button(
                            onClick = onCommit,
                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                        ) {
                            Text(stringResource(R.string.commit_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimilarGroupCard(
    group: List<Photo>,
    onKeepBest: () -> Unit,
    onPhotoClick: (Photo) -> Unit
) {
    val context = LocalContext.current
    val bestPhoto = remember(group) {
        group.minWithOrNull { p1, p2 ->
            val p1Blur = p1.localReason.contains("模糊")
            val p2Blur = p2.localReason.contains("模糊")
            if (p1Blur != p2Blur) return@minWithOrNull if (p1Blur) 1 else -1
            val p1Area = p1.width * p1.height
            val p2Area = p2.width * p2.height
            if (p1Area != p2Area) return@minWithOrNull p2Area.compareTo(p1Area)
            if (p1.size != p2.size) return@minWithOrNull p2.size.compareTo(p1.size)
            p1.dateAdded.compareTo(p2.dateAdded)
        } ?: group.first()
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                Text("相似照片组", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.review_similar_candidates, group.size), color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
                }
                    OutlinedButton(onClick = onKeepBest) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.review_keep_best))
                }
            }

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(group, key = { it.id }) { photo ->
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .clickable { onPhotoClick(photo) }
                    ) {
                        AsyncImage(
                            model = remember(photo.uri) {
                                ImageRequest.Builder(context)
                                    .data(photo.uri)
                                    .crossfade(true)
                                    .build()
                            },
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (photo.id == bestPhoto.id) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                                color = GreenAccent,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.best),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoDetailDialog(
    photo: Photo,
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
                    model = remember(photo.uri) {
                        ImageRequest.Builder(context)
                            .data(photo.uri)
                            .crossfade(true)
                            .build()
                    },
                    contentDescription = photo.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                }
                Text(
                    text = photo.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                        Text("${(photo.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${photo.width}×${photo.height}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text(com.photocleaner.util.ImageUtils.formatFileSize(photo.size), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onKeep,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenAccent)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.keep))
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.delete))
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
                Icon(Icons.Default.Warning, contentDescription = null, tint = RedAccent, modifier = Modifier.size(32.dp))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.review_confirm_delete_title),
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
                    text = stringResource(R.string.review_confirm_delete_count, count),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.review_confirm_delete_body),
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
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
