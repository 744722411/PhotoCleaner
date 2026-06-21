package com.photocleaner.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photocleaner.ui.components.*
import com.photocleaner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Android 14+ Partial Access Detection (READ_MEDIA_VISUAL_USER_SELECTED is granted, but READ_MEDIA_IMAGES is not)
    val isPartialAccess = remember(context) {
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_MEDIA_IMAGES
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Permission Launcher for requesting incremental media access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Auto refresh stats via database updates
    }

    // Material You dynamic colors gradient background
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer
    val backgroundColor = MaterialTheme.colorScheme.background
    val gradientColors = remember(primaryColor, secondaryColor, backgroundColor) {
        listOf(
            primaryColor.copy(alpha = 0.12f),
            secondaryColor.copy(alpha = 0.08f),
            backgroundColor
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = gradientColors)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(colors = listOf(BlueAccent, Purple80))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "🧹 照片清理助手",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "智能识别并清理无用照片",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Android 14+ Partial access warning banner
        if (isPartialAccess) {
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(animationSpec = tween(600, delayMillis = 100))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.READ_MEDIA_IMAGES,
                                        android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                                    )
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = YellowAccent.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YellowAccent.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(YellowAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = YellowAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "已授权部分照片访问",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "当前应用仅可读取授权范围内的照片。为找出所有相似或无用片，建议在此追加照片授权。",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Stats Overview
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(600, delayMillis = 200))
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        ModernSectionHeader(
                            title = "📊 总览",
                            icon = Icons.Default.Dashboard
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ModernStatCard(
                                title = "照片总数",
                                value = uiState.totalPhotos.toString(),
                                icon = Icons.Default.PhotoLibrary,
                                color = BlueAccent,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            ModernStatCard(
                                title = "已分类",
                                value = uiState.classifiedPhotos.toString(),
                                icon = Icons.Default.CheckCircle,
                                color = GreenAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ModernStatCard(
                                title = "无用照片",
                                value = uiState.uselessPhotos.toString(),
                                icon = Icons.Default.Delete,
                                color = RedAccent,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            ModernStatCard(
                                title = "可节省",
                                value = uiState.spaceSaved,
                                icon = Icons.Default.Storage,
                                color = YellowAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(600, delayMillis = 400))
            ) {
                Column {
                    ModernSectionHeader(
                        title = "⚡ 快捷操作",
                        icon = Icons.Default.Bolt
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernActionButton(
                            title = "开始扫描",
                            subtitle = "扫描设备照片",
                            icon = Icons.Default.CameraAlt,
                            gradient = Brush.linearGradient(
                                colors = listOf(BlueAccent, Color(0xFF1E88E5))
                            ),
                            onClick = onNavigateToScan,
                            modifier = Modifier.weight(1f)
                        )
                        ModernActionButton(
                            title = "审查照片",
                            subtitle = "查看分类结果",
                            icon = Icons.Default.Preview,
                            gradient = Brush.linearGradient(
                                colors = listOf(GreenAccent, Color(0xFF2E7D32))
                            ),
                            onClick = onNavigateToReview,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernActionButton(
                            title = "查看统计",
                            subtitle = "数据分析",
                            icon = Icons.Default.BarChart,
                            gradient = Brush.linearGradient(
                                colors = listOf(Purple80, Color(0xFF7B1FA2))
                            ),
                            onClick = onNavigateToStats,
                            modifier = Modifier.weight(1f)
                        )
                        ModernActionButton(
                            title = "高级设置",
                            subtitle = "AI与规则配置",
                            icon = Icons.Default.Settings,
                            gradient = Brush.linearGradient(
                                colors = listOf(YellowAccent, Color(0xFFF57F17))
                            ),
                            onClick = onNavigateToSettings,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Recent Photos
        if (uiState.recentPhotos.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(600, delayMillis = 600))
                ) {
                    Column {
                        ModernSectionHeader(
                            title = "📸 最近照片",
                            icon = Icons.Default.History
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        GlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.recentPhotos.take(5).forEachIndexed { index, photo ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
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
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkSurfaceVariant),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = photo.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "${photo.width}×${photo.height}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                        ModernClassificationBadge(
                                            classification = photo.classification.displayName,
                                            confidence = photo.confidence,
                                            color = when (photo.classification) {
                                                com.photocleaner.domain.model.Classification.USELESS -> RedAccent
                                                com.photocleaner.domain.model.Classification.KEEP -> GreenAccent
                                                com.photocleaner.domain.model.Classification.UNCERTAIN -> YellowAccent
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                    if (index < uiState.recentPhotos.take(5).size - 1) {
                                        HorizontalDivider(
                                            color = Color.White.copy(alpha = 0.1f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
