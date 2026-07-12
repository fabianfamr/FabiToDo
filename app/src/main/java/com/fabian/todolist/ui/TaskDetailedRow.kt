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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val localView = androidx.compose.ui.platform.LocalView.current
    
    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else if (task.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 300),
        label = "CardBgColor"
    )

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.4f },
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                    currentOnDelete()
                    false // Spring back, let dialog/state handle it
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                    currentOnToggleComplete()
                    false // Spring back, let app state handle visibility
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
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.StartToEnd -> if (task.isCompleted) Color(0xFFFBBC05) else Color(0xFF34A853)
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                    },
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
                        contentDescription = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> stringResource(if(task.isCompleted) R.string.desc_restore else R.string.mark_as_completed)
                            SwipeToDismissBoxValue.EndToStart -> stringResource(R.string.desc_delete)
                            else -> null
                        }, 
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
                colors = CardDefaults.cardColors(
                    containerColor = cardBgColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isExpanded) 6.dp else 2.dp
                ),
                border = if (isSelected) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                shape = SettingsDimens.CardRadiusRoot
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(durationMillis = 150))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SettingsDimens.CardRadiusRoot)
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionModeActive) {
                                        onToggleSelect()
                                    } else {
                                        onClick()
                                    }
                                },
                                onLongClick = { 
                                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                                    if (isSelectionModeActive) {
                                        onToggleSelect()
                                    } else {
                                        if (longPressAction == "select") {
                                            onToggleSelect()
                                        } else {
                                            onEdit()
                                        }
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

                        TaskCheckbox(
                            isCompleted = task.isCompleted,
                            taskId = task.id,
                            onToggleComplete = {
                                com.fabian.todolist.util.HapticUtil.performToggleHaptic(localView, !task.isCompleted)
                                if (isSelectionModeActive) {
                                    onToggleSelect()
                                } else {
                                    onToggleComplete()
                                }
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
                            TaskHeader(task = task, isExpanded = isExpanded)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            TaskBadges(task = task, categoryColors = categoryColors)

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
                        }
                        
                        if (isManualOrderEnabled) {
                            TaskManualOrderControls(onMoveUp = onMoveUp, onMoveDown = onMoveDown)
                        }
                    }

                    if (isExpanded) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            TaskSubtasks(task = task, onUpdateSubtasks = onUpdateSubtasks)
                            TaskActions(
                                task = task,
                                onEdit = onEdit,
                                onDelete = onDelete,
                                onRestore = onRestore,
                                onDuplicate = onDuplicate
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
    }
    })
}

@Composable
private fun TaskCheckbox(
    isCompleted: Boolean,
    taskId: Int,
    onToggleComplete: () -> Unit,
    isSelected: Boolean = false,
    isSelectionModeActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    val displayChecked = if (isSelectionModeActive) isSelected else isCompleted
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (displayChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(bounded = false, radius = 24.dp)
            ) { onToggleComplete() }
            .testTag("checkbox_$taskId")
    ) {
        val scale by animateFloatAsState(
            targetValue = if (displayChecked) 1.15f else 1.0f,
            animationSpec = spring(dampingRatio = 0.5f),
            label = "ScaleCircle"
        )

        Icon(
            imageVector = if (displayChecked)
                Icons.Filled.CheckCircle
            else
                Icons.Outlined.RadioButtonUnchecked,
            contentDescription = stringResource(R.string.mark_as_completed),
            tint = if (displayChecked)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline,
            modifier = Modifier.size((28.toFloat() * scale).dp)
        )
    }
}

@Composable
private fun TaskHeader(task: Task, isExpanded: Boolean) {
    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted)
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "TaskTitleColor"
    )

    val strikethroughFraction by animateFloatAsState(
        targetValue = if (task.isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "StrikethroughWidth"
    )

    Box(
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = task.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.drawBehind {
                if (strikethroughFraction > 0f) {
                    val y = size.height / 2f
                    drawLine(
                        color = textColor.copy(alpha = 0.7f),
                        start = androidx.compose.ui.geometry.Offset(x = 0f, y = y),
                        end = androidx.compose.ui.geometry.Offset(x = size.width * strikethroughFraction, y = y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskBadges(task: Task, categoryColors: Map<String, Int>) {
    val todayLabel = stringResource(R.string.date_today)
    val tomorrowLabel = stringResource(R.string.date_tomorrow)
    val dateText = remember(task.dueDate, todayLabel, tomorrowLabel) {
        task.dueDate?.let { com.fabian.todolist.util.DateTimeUtils.formatDueDateLocal(it, todayLabel, tomorrowLabel) } ?: ""
    }
    
    val defaultIndicator = Color(0xFF444444)
    val defaultText = Color(0xFFBDBDBD)
    val isDark = isSystemInDarkTheme()
    val priorityData = remember(task.priority, isDark) {
        when (task.priority) {
            com.fabian.todolist.data.TaskPriority.CRITICAL -> {
                if (isDark) Pair(Color(0xFF5C1919), Color(0xFFF48FB1))
                else Pair(Color(0xFFFFDAD9), Color(0xFFBA1A1A))
            }
            com.fabian.todolist.data.TaskPriority.HIGH -> {
                if (isDark) Pair(Color(0xFF5C2D00), Color(0xFFFFB582))
                else Pair(Color(0xFFFFEADB), Color(0xFFD16C00))
            }
            com.fabian.todolist.data.TaskPriority.MEDIUM -> {
                if (isDark) Pair(Color(0xFF4F4700), Color(0xFFFFE082))
                else Pair(Color(0xFFFFF9C4), Color(0xFF6F6000))
            }
            com.fabian.todolist.data.TaskPriority.LOW -> {
                if (isDark) Pair(Color(0xFF00391E), Color(0xFFA5D6A7))
                else Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
            }
            else -> Pair(defaultIndicator, defaultText)
        }
    }
    
    val (priorityChipColor, priorityTextColor) = priorityData

    val priorityLocalized = when (task.priority) {
        com.fabian.todolist.data.TaskPriority.CRITICAL -> stringResource(R.string.priority_critical_label)
        com.fabian.todolist.data.TaskPriority.HIGH -> stringResource(R.string.priority_high_label)
        com.fabian.todolist.data.TaskPriority.MEDIUM -> stringResource(R.string.priority_medium_label)
        com.fabian.todolist.data.TaskPriority.LOW -> stringResource(R.string.priority_low_label)
        else -> stringResource(R.string.priority_none_label)
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (task.category.isNotBlank()) {
            val resolvedCategoryColor = com.fabian.todolist.util.getCategoryColor(task.category, categoryColors[task.category])
            val isDarkThemeNow = isSystemInDarkTheme()
            val categoryBgColor = resolvedCategoryColor.copy(alpha = if (isDarkThemeNow) 0.15f else 0.12f)
            val categoryTextColor = resolvedCategoryColor
            
            Box(
                modifier = Modifier
                    .background(
                        color = categoryBgColor,
                        shape = SettingsDimens.CardRadiusSmall
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = getLocalizedCategoryName(task.category),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryTextColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Box(
            modifier = Modifier
                .background(
                    color = priorityChipColor,
                    shape = SettingsDimens.CardRadiusSmall
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = priorityLocalized,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = priorityTextColor,
                maxLines = 1,
                softWrap = false
            )
        }
        
        val subtasks = remember(task.subtasks) { task.getSubtasks() }
        if (subtasks.isNotEmpty()) {
            val completedCount = remember(subtasks) { subtasks.count { it.isCompleted } }
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = SettingsDimens.CardRadiusSmall
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = stringResource(R.string.desc_subtasks),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "$completedCount/${subtasks.size}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (dateText.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Icon(
                    imageVector = if (task.reminderTime != null)
                        Icons.Default.NotificationsActive
                    else
                        Icons.Default.CalendarToday,
                    contentDescription = stringResource(R.string.content_desc_alarm_trigger),
                    tint = if (task.reminderTime != null && !task.isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(12.dp)
                )
                
                val dailyStr = stringResource(R.string.repeat_daily)
                val weeklyStr = stringResource(R.string.repeat_weekly)
                val monthlyStr = stringResource(R.string.repeat_monthly)
                val repeatLabelDefault = stringResource(R.string.repeat_label_msg)
                val repeatLabel = remember(task.repeatType, dailyStr, weeklyStr, monthlyStr, repeatLabelDefault) {
                    when (task.repeatType) {
                        "Diario" -> dailyStr
                        "Semanal" -> weeklyStr
                        "Mensual" -> monthlyStr
                        else -> repeatLabelDefault
                    }
                }
                
                val dateAndRepeatText = remember(dateText, task.dueTime, task.isRepeat, repeatLabel) {
                    buildString {
                        append(dateText)
                        if (!task.dueTime.isNullOrBlank()) {
                            append(" - ")
                            append(task.dueTime)
                        }
                        if (task.isRepeat) {
                            append(" ($repeatLabel)")
                        }
                    }
                }
                
                Text(
                    text = dateAndRepeatText,
                    fontSize = 10.sp,
                    color = if (task.reminderTime != null && !task.isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun TaskSubtasks(task: Task, onUpdateSubtasks: (List<Subtask>) -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val localView = androidx.compose.ui.platform.LocalView.current
    val subtasks = remember(task.subtasks) { task.getSubtasks() }
    if (subtasks.isNotEmpty()) {
        Text(
            text = stringResource(R.string.subtasks_title),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f), SettingsDimens.CardRadiusSmall)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), SettingsDimens.CardRadiusSmall)
                .clip(SettingsDimens.CardRadiusSmall)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            subtasks.forEachIndexed { index, subtask ->
                key(subtask.id) {
                    var isDragging by remember { mutableStateOf(false) }
                    var offsetY by remember { mutableStateOf(0f) }
                    
                    val animatedSubtaskColor by animateColorAsState(
                        targetValue = if (subtask.isCompleted)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        animationSpec = tween(durationMillis = 300),
                        label = "SubtaskText"
                    )

                    val animatedSubtaskTint by animateColorAsState(
                        targetValue = if (subtask.isCompleted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                        animationSpec = tween(durationMillis = 250),
                        label = "SubtaskTint"
                    )

                    val subtaskScale by animateFloatAsState(
                        targetValue = if (subtask.isCompleted) 1.15f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.6f),
                        label = "SubtaskScale"
                    )

                    val subtaskStrikethrough by animateFloatAsState(
                        targetValue = if (subtask.isCompleted) 1f else 0f,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                        label = "SubtaskStrikethrough"
                    )
                    
                    val rowBackgroundColor = if (isDragging) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        Color.Transparent
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(rowBackgroundColor)
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isDragging) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
                                .pointerInput(subtask) {
                                    detectVerticalDragGestures(
                                        onDragStart = { isDragging = true },
                                        onDragEnd = { isDragging = false; offsetY = 0f },
                                        onDragCancel = { isDragging = false; offsetY = 0f },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetY += dragAmount
                                            val currentIndex = subtasks.indexOf(subtask)
                                            if (offsetY > 100f && currentIndex < subtasks.size - 1) {
                                                val updated = subtasks.toMutableList().apply {
                                                    val temp = get(currentIndex)
                                                    set(currentIndex, get(currentIndex + 1))
                                                    set(currentIndex + 1, temp)
                                                }
                                                onUpdateSubtasks(updated)
                                                offsetY = 0f
                                            } else if (offsetY < -100f && currentIndex > 0) {
                                                val updated = subtasks.toMutableList().apply {
                                                    val temp = get(currentIndex)
                                                    set(currentIndex, get(currentIndex - 1))
                                                    set(currentIndex - 1, temp)
                                                }
                                                onUpdateSubtasks(updated)
                                                offsetY = 0f
                                            }
                                        }
                                    )
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = stringResource(R.string.desc_drag_handle),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val updated = subtasks.toMutableList().apply {
                                        this[index] = subtask.copy(isCompleted = !subtask.isCompleted)
                                    }
                                    com.fabian.todolist.util.HapticUtil.performToggleHaptic(localView, !subtask.isCompleted)
                                    onUpdateSubtasks(updated)
                                }
                        ) {
                            Icon(
                                imageVector = if (subtask.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = stringResource(R.string.desc_toggle_subtask),
                                tint = animatedSubtaskTint,
                                modifier = Modifier.size((20.toFloat() * subtaskScale).dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = subtask.title,
                                fontSize = 13.sp,
                                color = animatedSubtaskColor,
                                modifier = Modifier.drawBehind {
                                    if (subtaskStrikethrough > 0f) {
                                        val y = size.height / 2f
                                        drawLine(
                                            color = animatedSubtaskColor.copy(alpha = 0.7f),
                                            start = androidx.compose.ui.geometry.Offset(x = 0f, y = y),
                                            end = androidx.compose.ui.geometry.Offset(x = size.width * subtaskStrikethrough, y = y),
                                            strokeWidth = 1.5.dp.toPx()
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskActions(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val shareTitle = stringResource(R.string.share_task_title)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task.isDeleted) {
            FilledTonalButton(
                onClick = onRestore ?: {},
                shape = SettingsDimens.CardRadiusSmall,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .height(38.dp)
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.RestoreFromTrash, contentDescription = stringResource(R.string.desc_restore), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.restore_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
                onClick = onDelete,
                shape = SettingsDimens.CardRadiusSmall,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .height(38.dp)
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.desc_delete_forever), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.delete_forever_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            IconButton(
                onClick = {
                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, "${task.title}\n${task.description}")
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, shareTitle))
                },
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), SettingsDimens.CardRadiusSmall)
            ) {
                Icon(
                    Icons.Default.Share, 
                    contentDescription = stringResource(R.string.share_label), 
                    tint = MaterialTheme.colorScheme.onSecondaryContainer, 
                    modifier = Modifier.size(18.dp)
                )
            }
            FilledTonalButton(
                onClick = onEdit,
                shape = SettingsDimens.CardRadiusSmall,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .height(38.dp)
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.desc_edit), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.nav_edit), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
                onClick = onDelete,
                shape = SettingsDimens.CardRadiusSmall,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .height(38.dp)
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.desc_delete), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.nav_delete), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TaskManualOrderControls(onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?) {
    IconButton(
        onClick = { onMoveUp?.invoke() },
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = stringResource(R.string.desc_move_up),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
    IconButton(
        onClick = { onMoveDown?.invoke() },
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = stringResource(R.string.desc_move_down),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

