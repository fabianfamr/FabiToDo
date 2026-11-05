package com.fabian.todolist.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.todolist.R
import com.fabian.todolist.data.Subtask
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.getSubtasks
import com.fabian.todolist.ui.SettingsDimens
import com.fabian.todolist.util.*
import java.util.*

@Composable
fun TaskCheckbox(
    isCompleted: Boolean,
    taskId: Int,
    onToggleComplete: () -> Unit,
    isSelected: Boolean = false,
    isSelectionModeActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val displayChecked = if (isSelectionModeActive) isSelected else isCompleted
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (displayChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 24.dp)
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
fun TaskTitle(
    title: String,
    isCompleted: Boolean,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isCompleted)
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "TaskTitleColor"
    )

    val strikethroughFraction by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "StrikethroughWidth"
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
    ) {
        Text(
            text = title,
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
fun TaskBadges(
    task: Task,
    categoryColors: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val todayLabel = stringResource(R.string.date_today)
    val tomorrowLabel = stringResource(R.string.date_tomorrow)
    val dateText = remember(task.dueDate, todayLabel, tomorrowLabel) {
        task.dueDate?.let { DateTimeUtils.formatDueDateLocal(it, todayLabel, tomorrowLabel) } ?: ""
    }
    
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
            else -> Pair(Color(0xFF444444), Color(0xFFBDBDBD))
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
        modifier = modifier.fillMaxWidth()
    ) {
        if (task.category.isNotBlank()) {
            val resolvedCategoryColor = getCategoryColor(task.category, categoryColors[task.category])
            val categoryBgColor = resolvedCategoryColor.copy(alpha = if (isDark) 0.15f else 0.12f)
            
            Box(
                modifier = Modifier
                    .background(color = categoryBgColor, shape = SettingsDimens.CardRadiusSmall)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = getLocalizedCategoryName(task.category),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = resolvedCategoryColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Box(
            modifier = Modifier
                .background(color = priorityChipColor, shape = SettingsDimens.CardRadiusSmall)
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
                    imageVector = if (task.reminderTime != null) Icons.Default.NotificationsActive else Icons.Default.CalendarToday,
                    contentDescription = stringResource(R.string.content_desc_alarm_trigger),
                    tint = if (task.reminderTime != null && !task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(12.dp)
                )
                
                val dailyLabel = stringResource(R.string.repeat_daily)
                val weeklyLabel = stringResource(R.string.repeat_weekly)
                val monthlyLabel = stringResource(R.string.repeat_monthly)
                val repeatLabel = stringResource(R.string.repeat_label)
                
                val dateAndRepeatText = remember(dateText, task.dueTime, task.isRepeat, task.repeatType, dailyLabel, weeklyLabel, monthlyLabel, repeatLabel) {
                    buildString {
                        append(dateText)
                        if (!task.dueTime.isNullOrBlank()) {
                            append(" - ")
                            append(task.dueTime)
                        }
                        if (task.isRepeat) {
                            append(" (")
                            append(when (task.repeatType) {
                                com.fabian.todolist.data.RepeatType.DAILY -> dailyLabel
                                com.fabian.todolist.data.RepeatType.WEEKLY -> weeklyLabel
                                com.fabian.todolist.data.RepeatType.MONTHLY -> monthlyLabel
                                else -> repeatLabel
                            })
                            append(")")
                        }
                    }
                }
                
                Text(
                    text = dateAndRepeatText,
                    fontSize = 10.sp,
                    color = if (task.reminderTime != null && !task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun TaskSubtasksList(
    task: Task,
    onUpdateSubtasks: (List<Subtask>) -> Unit,
    modifier: Modifier = Modifier
) {
    val localView = androidx.compose.ui.platform.LocalView.current
    val subtasks = remember(task.subtasks) { task.getSubtasks() }
    if (subtasks.isNotEmpty()) {
        Column(modifier = modifier) {
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
                            targetValue = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            label = "SubtaskText"
                        )

                        val animatedSubtaskTint by animateColorAsState(
                            targetValue = if (subtask.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
                                        HapticUtil.performToggleHaptic(localView, !subtask.isCompleted)
                                        onUpdateSubtasks(updated)
                                    }
                            ) {
                                Icon(
                                    imageVector = if (subtask.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
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
}

@Composable
fun TaskActionsRow(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.share_task_title)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task.isDeleted) {
            ActionBtn(
                icon = Icons.Default.RestoreFromTrash,
                label = stringResource(R.string.restore_label),
                onClick = onRestore ?: {},
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            ActionBtn(
                icon = Icons.Default.DeleteForever,
                label = stringResource(R.string.delete_forever_label),
                onClick = onDelete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        } else {
            IconButton(
                onClick = {
                    val shareIntent = android.content.Intent().apply {
                        val shareText = TaskShareUtils.formatTaskForSharing(context, task)
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, shareTitle))
                },
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), SettingsDimens.CardRadiusSmall)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
            }
            ActionBtn(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.nav_edit),
                onClick = onEdit,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            ActionBtn(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.nav_delete),
                onClick = onDelete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        shape = SettingsDimens.CardRadiusSmall,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = containerColor, contentColor = contentColor),
        modifier = modifier.height(38.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TaskManualOrderControls(onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?) {
    Row {
        IconButton(onClick = { onMoveUp?.invoke() }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = { onMoveDown?.invoke() }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}
