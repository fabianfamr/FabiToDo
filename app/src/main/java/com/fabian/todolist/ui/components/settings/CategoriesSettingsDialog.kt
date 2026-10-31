package com.fabian.todolist.ui.components.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fabian.todolist.R
import com.fabian.todolist.data.SystemCategory
import com.fabian.todolist.ui.TaskViewModel
import com.fabian.todolist.ui.SettingsViewModel
import com.fabian.todolist.ui.components.TaskAddCategoryDialog
import com.fabian.todolist.util.getCategoryColor
import com.fabian.todolist.util.getCategoryIconVector
import com.fabian.todolist.util.getLocalizedCategoryName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesSettingsDialog(
    categories: List<String>,
    categoryColors: Map<String, Int>,
    categoryIcons: Map<String, String>,
    viewModel: TaskViewModel,
    settingsViewModel: SettingsViewModel,
    onEditCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    val userCategories = remember(categories) {
        categories.filter {
            it != SystemCategory.ALL_TASKS && it != SystemCategory.COMPLETED &&
            it != "Completed" && it != "Todas" && it != SystemCategory.TRASH &&
            it != SystemCategory.CALENDAR && it != SystemCategory.EISENHOWER
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            SettingsSubmenuScaffold(
                title = stringResource(R.string.dialog_task_list_header),
                onBack = onDismiss
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_categories_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.settings_categories_desc_detailed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            // Quick add category button inside the management screen
                            Card(
                                onClick = { showAddCategoryDialog = true },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.add_category_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.new_category),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(categories, key = { it }) { category ->
                        val isImmutable = category == SystemCategory.ALL_TASKS || category == SystemCategory.COMPLETED ||
                                category == "Completed" || category == "Todas" || category == SystemCategory.TRASH

                        val userIndex = userCategories.indexOf(category)
                        val canMoveUp = !isImmutable && userIndex > 0
                        val canMoveDown = !isImmutable && userIndex >= 0 && userIndex < userCategories.size - 1

                        var isDragging by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(if (isDragging) 1.03f else 1.0f, label = "card_scale")
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "card_elevation")

                        val customColor = getCategoryColor(category, categoryColors[category])
                        val customIcon = getCategoryIconVector(category, categoryIcons[category])

                        val borderColor = if (isDragging) {
                            customColor.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .shadow(elevation = elevation, shape = RoundedCornerShape(22.dp))
                                .animateItem(),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, borderColor),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDragging) {
                                    customColor.copy(alpha = 0.08f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isImmutable) {
                                    // Reordering controls: Up & Down arrows for 100% reliable one-touch moving
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (canMoveUp) {
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                    settingsViewModel.moveUserCategory(category, -1)
                                                }
                                            },
                                            enabled = canMoveUp,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = stringResource(R.string.desc_move_up),
                                                tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (canMoveDown) {
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                    settingsViewModel.moveUserCategory(category, 1)
                                                }
                                            },
                                            enabled = canMoveDown,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = stringResource(R.string.desc_move_down),
                                                tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Drag handle with fast, smooth 40f responsiveness and haptics
                                    var offsetY by remember { mutableStateOf(0f) }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                            .pointerInput(category, canMoveUp, canMoveDown) {
                                                detectVerticalDragGestures(
                                                    onDragStart = { isDragging = true },
                                                    onDragEnd = { isDragging = false; offsetY = 0f },
                                                    onDragCancel = { isDragging = false; offsetY = 0f },
                                                    onVerticalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        offsetY += dragAmount
                                                        if (offsetY > 40f && canMoveDown) {
                                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                            settingsViewModel.moveUserCategory(category, 1)
                                                            offsetY = 0f
                                                        } else if (offsetY < -40f && canMoveUp) {
                                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                            settingsViewModel.moveUserCategory(category, -1)
                                                            offsetY = 0f
                                                        }
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = stringResource(R.string.content_desc_drag_reorder),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    // Immutable/system category badge indicator
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = stringResource(R.string.category_default),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                // Color badge & circular Icon representation
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(customColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = customIcon,
                                        contentDescription = null,
                                        tint = customColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = getLocalizedCategoryName(category),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isImmutable) stringResource(R.string.category_status_system) else stringResource(R.string.category_status_custom),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isImmutable) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontWeight = if (isImmutable) FontWeight.Medium else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }

                                // Actions: Edit and Delete
                                if (!isImmutable) {
                                    IconButton(
                                        onClick = { onEditCategory(category) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.nav_edit),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            categoryToDelete = category
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.nav_delete),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
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

    // Modal to add a new category
    TaskAddCategoryDialog(
        showDialog = showAddCategoryDialog,
        onDismiss = { showAddCategoryDialog = false },
        onConfirm = { settingsViewModel.addCategory(it) }
    )

    // Modal to safely confirm category deletion
    if (categoryToDelete != null) {
        val catName = categoryToDelete ?: ""
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.delete_task),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_delete_category, catName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { settingsViewModel.deleteCategory(it) }
                        categoryToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.accept),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}
