package com.photocleaner.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var baseUrlInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("") }

    // Animated entrance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd)
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Section with animated entrance
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = 600)
                ) + fadeIn(animationSpec = tween(durationMillis = 600))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // App icon with glow effect
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(BlueAccent, Purple80)
                                )
                            ),
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

        // Stats Overview with animated cards
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(durationMillis = 600, delayMillis = 200)
                ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 200))
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

        // Quick Actions with gradient buttons
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(durationMillis = 600, delayMillis = 400)
                ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 400))
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
                            title = "API设置",
                            subtitle = "配置AI密钥",
                            icon = Icons.Default.Key,
                            gradient = Brush.linearGradient(
                                colors = listOf(YellowAccent, Color(0xFFF57F17))
                            ),
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Recent Photos with animation
        if (uiState.recentPhotos.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 600, delayMillis = 600)
                    ) + fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 600))
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

    // API Key Dialog with modern design
    if (showApiKeyDialog) {
        LaunchedEffect(showApiKeyDialog) {
            apiKeyInput = uiState.apiKey
            baseUrlInput = uiState.baseUrl
            modelInput = uiState.model
        }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    "API 设置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "配置AI分类服务的连接信息",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.openai.com/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("AI 模型") },
                        placeholder = { Text("mimo-v2.5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (uiState.testResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.testResult!!,
                            color = if (uiState.testSuccess) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.testConnection(baseUrlInput, apiKeyInput, modelInput)
                        },
                        enabled = !uiState.isTestingConnection,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("测试")
                    }
                    Button(
                        onClick = {
                            viewModel.setBaseUrl(baseUrlInput.trim())
                            viewModel.setApiKey(apiKeyInput.trim())
                            viewModel.setModel(modelInput.trim())
                            showApiKeyDialog = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("保存") }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApiKeyDialog = false }
                ) { Text("取消") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
