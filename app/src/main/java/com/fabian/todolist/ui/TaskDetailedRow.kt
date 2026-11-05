package com.fabian.todolist.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.fabian.todolist.R
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.Subtask
import com.fabian.todolist.data.getSubtasks
import com.fabian.todolist.data.withSubtasks
import com.fabian.todolist.util.getLocalizedCategoryName
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailedRow(
    task: Task,
    isExpanded: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    categoryColors: Map<String, Int> = emptyMap(),
    isManualOrderEnabled: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onUpdateSubtasks: (List<Subtask>) -> Unit = {},
    isSelected: Boolean = false,
    isSelectionModeActive: Boolean = false,
    onToggleSelect: () -> Unit = {},
    longPressAction: String = "select"
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnToggleComplete by rememberUpdatedState(onToggleComplete)
    val localView = androidx.compose.ui.platform.LocalView.current
    
    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else if (task.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "CardBgColor"
    )

    val cardElevation by animateDpAsState(
        targetValue = if (isExpanded) 6.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.75f),
        label = "CardElevation"
    )

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                    currentOnDelete()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                    currentOnToggleComplete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = !isManualOrderEnabled,
        enableDismissFromStartToEnd = !isManualOrderEnabled,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val color by animateColorAsState(
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> if (task.isCompleted) Color(0xFFFBBC05) else Color(0xFF34A853)
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                    },
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "SwipeColor"
                )
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                val icon = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> if(task.isCompleted) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                    else -> Icons.Default.Delete
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(SettingsDimens.CardRadiusRoot)
                        .background(color)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        icon, 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        content = {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("task_item_${task.id}"),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                border = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else if (isExpanded) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                },
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SettingsDimens.CardRadiusRoot)
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionModeActive) onToggleSelect() else onClick()
                                },
                                onLongClick = { 
                                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                                    if (isSelectionModeActive) onToggleSelect() else {
                                        if (longPressAction == "select") onToggleSelect() else onEdit()
                                    }
                                }
                            )
                            .padding(top = 16.dp, bottom = if(isExpanded) 4.dp else 16.dp, start = 20.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val priorityColor = when (task.priority) {
                            com.fabian.todolist.data.TaskPriority.CRITICAL -> Color(0xFFB3261E)
                            com.fabian.todolist.data.TaskPriority.HIGH -> Color(0xFFEA4335)
                            com.fabian.todolist.data.TaskPriority.MEDIUM -> Color(0xFFFBBC05)
                            com.fabian.todolist.data.TaskPriority.LOW -> Color(0xFF34A853)
                            else -> Color.Transparent
                        }

                        if (priorityColor != Color.Transparent && !task.isCompleted) {
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(priorityColor)
                                    .align(Alignment.CenterVertically)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        com.fabian.todolist.ui.components.TaskCheckbox(
                            isCompleted = task.isCompleted,
                            taskId = task.id,
                            onToggleComplete = {
                                com.fabian.todolist.util.HapticUtil.performToggleHaptic(localView, !task.isCompleted)
                                if (isSelectionModeActive) onToggleSelect() else onToggleComplete()
                            },
                            isSelected = isSelected,
                            isSelectionModeActive = isSelectionModeActive
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 4.dp)
                        ) {
                            com.fabian.todolist.ui.components.TaskTitle(
                                title = task.title,
                                isCompleted = task.isCompleted,
                                isExpanded = isExpanded
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            com.fabian.todolist.ui.components.TaskBadges(task = task, categoryColors = categoryColors)

                            if (isExpanded && task.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = task.description,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (isExpanded && !task.attachedImageUri.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                coil.compose.AsyncImage(
                                    model = task.attachedImageUri,
                                    contentDescription = stringResource(R.string.desc_task_image_attached),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        
                        if (isManualOrderEnabled) {
                            com.fabian.todolist.ui.components.TaskManualOrderControls(onMoveUp = onMoveUp, onMoveDown = onMoveDown)
                        }
                    }

                    if (isExpanded) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            com.fabian.todolist.ui.components.TaskSubtasksList(task = task, onUpdateSubtasks = onUpdateSubtasks)
                            com.fabian.todolist.ui.components.TaskActionsRow(
                                task = task,
                                onEdit = onEdit,
                                onDelete = onDelete,
                                onRestore = onRestore
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    )
}

