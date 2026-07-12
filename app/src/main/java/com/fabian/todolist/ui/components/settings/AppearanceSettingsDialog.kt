package com.fabian.todolist.ui.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fabian.todolist.R
import com.fabian.todolist.ui.components.FabiPriorityBadge
import com.fabian.todolist.ui.components.FabiSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsDialog(
    currentAccent: String,
    currentDark: String,
    onSaveAccent: (String) -> Unit,
    onSaveDark: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            val selectedColorAccentValue = when (currentAccent) {
                "blue" -> Color(0xFF4285F4)
                "green" -> Color(0xFF34A853)
                "coral" -> Color(0xFFEA4335)
                "purple" -> Color(0xFF8B5CF6)
                "gold" -> Color(0xFFFBBC05)
                "pink" -> Color(0xFFE91E63)
                else -> MaterialTheme.colorScheme.primary
            }

            SettingsSubmenuScaffold(
                title = stringResource(R.string.settings_appearance_title),
                onBack = onDismiss
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text(
                            stringResource(R.string.settings_appearance_header),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            stringResource(R.string.settings_appearance_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }

                    // Live Dynamic Preview
                    item {
                        FabiSectionHeader(
                            title = stringResource(R.string.settings_appearance_preview_title),
                            subtitle = stringResource(R.string.settings_appearance_preview_desc),
                            accentColor = selectedColorAccentValue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(26.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.settings_appearance_preview_task),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.settings_appearance_preview_task_desc),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        FabiPriorityBadge(
                                            priorityLabel = stringResource(R.string.priority_high),
                                            accentColor = selectedColorAccentValue
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Colors Accent Section
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        FabiSectionHeader(
                            title = stringResource(R.string.select_accent_color),
                            subtitle = stringResource(R.string.settings_accent_color_desc),
                            accentColor = selectedColorAccentValue
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val isSystemAccentSelected = currentAccent == "system"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onSaveAccent("system") },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSystemAccentSelected) selectedColorAccentValue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(
                                width = if (isSystemAccentSelected) 2.dp else 1.dp,
                                color = if (isSystemAccentSelected) selectedColorAccentValue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF8AB4F8), Color(0xFF81C995), Color(0xFFFDD663), Color(0xFFF28B82))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.color_system),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSystemAccentSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSystemAccentSelected) selectedColorAccentValue else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.color_system_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSystemAccentSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = selectedColorAccentValue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val presetColors = listOf(
                            "blue" to (stringResource(R.string.color_blue) to Color(0xFF4285F4)),
                            "green" to (stringResource(R.string.color_green) to Color(0xFF34A853)),
                            "coral" to (stringResource(R.string.color_orange) to Color(0xFFEA4335)),
                            "purple" to (stringResource(R.string.color_purple) to Color(0xFF8B5CF6)),
                            "gold" to (stringResource(R.string.color_gold) to Color(0xFFFBBC05)),
                            "pink" to (stringResource(R.string.color_pink) to Color(0xFFE91E63))
                        )

                        val chunkedColors = presetColors.chunked(2)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            chunkedColors.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { (colorKey, details) ->
                                        val (label, colorHex) = details
                                        val isSelected = currentAccent == colorKey
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                                .clip(RoundedCornerShape(27.dp))
                                                .clickable { onSaveAccent(colorKey) },
                                            shape = RoundedCornerShape(27.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) selectedColorAccentValue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                            ),
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) selectedColorAccentValue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(colorHex)
                                                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) selectedColorAccentValue else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Theme Mode Section
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        FabiSectionHeader(
                            title = stringResource(R.string.theme_mode),
                            subtitle = stringResource(R.string.theme_mode_desc),
                            accentColor = selectedColorAccentValue
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val themeSelectionOptions = listOf(
                            "system" to Triple(R.string.theme_system, R.string.theme_system_desc, Icons.Rounded.Contrast),
                            "light" to Triple(R.string.theme_light, R.string.theme_light_desc, Icons.Rounded.WbSunny),
                            "dark" to Triple(R.string.theme_dark, R.string.theme_dark_desc, Icons.Rounded.NightsStay),
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            themeSelectionOptions.forEach { (mode, details) ->
                                val (titleRes, descRes, iconVector) = details
                                val isSelected = currentDark == mode

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable { onSaveDark(mode) },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) selectedColorAccentValue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) selectedColorAccentValue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) selectedColorAccentValue.copy(alpha = 0.2f)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = null,
                                                tint = if (isSelected) selectedColorAccentValue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(titleRes),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) selectedColorAccentValue else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(descRes),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = selectedColorAccentValue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Spacer
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}
