package com.photocleaner.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photocleaner.R
import com.photocleaner.ui.components.GlassCard
import com.photocleaner.ui.components.ModernSectionHeader
import com.photocleaner.ui.theme.BlueAccent
import com.photocleaner.ui.theme.GreenAccent
import com.photocleaner.ui.theme.RedAccent
import com.photocleaner.ui.theme.YellowAccent
import com.photocleaner.util.MediaAccessLevel
import com.photocleaner.util.PermissionHelper

@Composable
fun SettingsScreen(
    onRequestPermission: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mediaAccessLevel = PermissionHelper.getMediaAccessLevel(context)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
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
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernSectionHeader(title = stringResource(R.string.settings_scan_behavior), icon = Icons.Default.Settings)
                    BatchSizeSelector(
                        batchSize = uiState.batchSize,
                        onBatchSizeChange = viewModel::setBatchSize
                    )
                    SettingSwitchRow(
                        icon = Icons.Default.RestartAlt,
                        title = stringResource(R.string.settings_rescan_existing_title),
                        description = stringResource(R.string.settings_rescan_existing_desc),
                        checked = uiState.rescanExistingPhotos,
                        onCheckedChange = viewModel::setRescanExistingPhotos
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernSectionHeader(title = stringResource(R.string.settings_permission), icon = Icons.Default.PhotoLibrary)
                        PermissionPill(mediaAccessLevel = mediaAccessLevel)
                    }
                    Text(
                        text = when (mediaAccessLevel) {
                            MediaAccessLevel.FULL -> stringResource(R.string.permission_full_desc)
                            MediaAccessLevel.PARTIAL -> stringResource(R.string.permission_partial_desc)
                            MediaAccessLevel.NONE -> stringResource(R.string.permission_none_desc)
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(stringResource(R.string.permission_reauthorize))
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernSectionHeader(title = stringResource(R.string.settings_privacy), icon = Icons.Default.Security)
                        StatusPill(
                            text = stringResource(R.string.settings_offline),
                            color = GreenAccent
                        )
                    }

                    SettingInfoRow(
                        icon = Icons.Default.CheckCircle,
                        iconColor = GreenAccent,
                        title = stringResource(R.string.settings_net_removed_title),
                        description = stringResource(R.string.settings_net_removed_desc)
                    )
                    SettingInfoRow(
                        icon = Icons.Default.FolderOpen,
                        iconColor = BlueAccent,
                        title = stringResource(R.string.settings_local_title),
                        description = stringResource(R.string.settings_local_desc)
                    )
                    SettingInfoRow(
                        icon = Icons.Default.TipsAndUpdates,
                        iconColor = YellowAccent,
                        title = stringResource(R.string.settings_confirm_title),
                        description = stringResource(R.string.settings_confirm_desc)
                    )
                }
            }
        }

        item {
            Surface(
                color = GreenAccent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.settings_footer),
                    modifier = Modifier.padding(14.dp),
                    color = GreenAccent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BatchSizeSelector(
    batchSize: Int,
    onBatchSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.scan_batch_size),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(100, 500, 2000, 0).forEach { option ->
                val selected = batchSize == option
                val label = if (option == 0) stringResource(R.string.scan_batch_all) else option.toString()
                Button(
                    onClick = { onBatchSizeChange(option) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = if (selected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) BlueAccent else Color.White.copy(alpha = 0.06f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
        Text(
            text = stringResource(R.string.settings_batch_desc),
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = YellowAccent, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingInfoRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PermissionPill(mediaAccessLevel: MediaAccessLevel) {
    val text = when (mediaAccessLevel) {
        MediaAccessLevel.FULL -> stringResource(R.string.permission_full)
        MediaAccessLevel.PARTIAL -> stringResource(R.string.permission_partial)
        MediaAccessLevel.NONE -> stringResource(R.string.permission_none)
    }
    val color = when (mediaAccessLevel) {
        MediaAccessLevel.FULL -> GreenAccent
        MediaAccessLevel.PARTIAL -> YellowAccent
        MediaAccessLevel.NONE -> RedAccent
    }
    StatusPill(text = text, color = color)
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
