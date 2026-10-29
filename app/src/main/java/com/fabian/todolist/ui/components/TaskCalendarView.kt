package com.fabian.todolist.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.todolist.R
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.TaskPriority
import com.fabian.todolist.data.getSubtasks
import com.fabian.todolist.util.DateTimeUtils
import com.fabian.todolist.util.getCategoryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * High-fidelity, adaptive Task Calendar & Agenda View.
 * Works seamlessly in dual-pane layouts on Tablets & Foldables (Samsung Fold/Flip),
 * as well as a standalone full-screen view on compact phones.
 */
@Composable
fun TaskCalendarPane(
    tasks: List<Task>,
    categoryColors: Map<String, Int>,
    onToggleComplete: (Task) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTaskForDate: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isDualPane: Boolean = false,
    onBackToTasks: () -> Unit = {}
) {
    // Current visible month calendar
    val displayedCalendar = remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    // Selected day timestamp (defaults to today midnight)
    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var selectedDateMillis by rememberSaveable { mutableStateOf(todayMidnight) }

    // Map tasks by day formatted as yyyyMMdd for fast lookup
    val dayFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayHeaderFormat = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()) }

    // Pre-index tasks with due dates
    val tasksByDay = remember(tasks) {
        tasks.filter { it.dueDate != null }.groupBy { task ->
            dayFormat.format(Date(task.dueDate!!))
        }
    }

    val selectedDayKey = remember(selectedDateMillis) {
        dayFormat.format(Date(selectedDateMillis))
    }
    val tasksForSelectedDay = tasksByDay[selectedDayKey] ?: emptyList()

    // Tasks due in the next 7 days (upcoming horizon)
    val upcomingTasks = remember(tasks) {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val inSevenDays = now + (7L * 24 * 60 * 60 * 1000)
        tasks.filter { task ->
            task.dueDate != null && !task.isCompleted && task.dueDate!! in now..inSevenDays
        }.sortedBy { it.dueDate }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = if (isDualPane) 16.dp else 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mobile-only navigation header box to return to task list
            if (!isDualPane) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.calendar_nav_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.calendar_agenda_title),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = onBackToTasks,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.back_to_tasks),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 1. CALENDAR HEADER CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Month navigation row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val currentMonthName = remember(displayedCalendar.value.timeInMillis) {
                                    monthFormat.format(displayedCalendar.value.time).replaceFirstChar { it.uppercase() }
                                }
                                Text(
                                    text = currentMonthName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.calendar_nav_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Jump to today button
                                FilledTonalButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        selectedDateMillis = cal.timeInMillis
                                        displayedCalendar.value = (cal.clone() as Calendar).apply {
                                            set(Calendar.DAY_OF_MONTH, 1)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.calendar_today), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = {
                                        val cal = (displayedCalendar.value.clone() as Calendar).apply {
                                            add(Calendar.MONTH, -1)
                                        }
                                        displayedCalendar.value = cal
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Mes anterior",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val cal = (displayedCalendar.value.clone() as Calendar).apply {
                                            add(Calendar.MONTH, 1)
                                        }
                                        displayedCalendar.value = cal
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Mes siguiente",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Weekday labels (L, M, X, J, V, S, D)
                        val weekdays = listOf("L", "M", "X", "J", "V", "S", "D")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            weekdays.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Month Grid Days computation
                        val cal = displayedCalendar.value.clone() as Calendar
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        // In Java Calendar, SUNDAY=1, MONDAY=2. We convert to Mon=0..Sun=6
                        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

                        val totalCells = firstDayOfWeek + daysInMonth
                        val numRows = (totalCells + 6) / 7

                        val todayCal = Calendar.getInstance()
                        val isCurrentMonthDisplayed = todayCal.get(Calendar.YEAR) == displayedCalendar.value.get(Calendar.YEAR) &&
                                todayCal.get(Calendar.MONTH) == displayedCalendar.value.get(Calendar.MONTH)
                        val todayDayNum = if (isCurrentMonthDisplayed) todayCal.get(Calendar.DAY_OF_MONTH) else -1

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in 0 until numRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (col in 0..6) {
                                        val cellIndex = row * 7 + col
                                        val dayNumber = cellIndex - firstDayOfWeek + 1

                                        if (dayNumber in 1..daysInMonth) {
                                            val cellCal = (displayedCalendar.value.clone() as Calendar).apply {
                                                set(Calendar.DAY_OF_MONTH, dayNumber)
                                                set(Calendar.HOUR_OF_DAY, 0)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            val cellTimeMillis = cellCal.timeInMillis
                                            val dayKey = dayFormat.format(cellCal.time)
                                            val dayTasks = tasksByDay[dayKey] ?: emptyList()
                                            val isSelected = selectedDayKey == dayKey
                                            val isToday = dayNumber == todayDayNum

                                            CalendarDayCell(
                                                dayNumber = dayNumber,
                                                isSelected = isSelected,
                                                isToday = isToday,
                                                tasks = dayTasks,
                                                onClick = {
                                                    selectedDateMillis = cellTimeMillis
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. SELECTED DAY SCHEDULE / AGENDA HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val headerText = remember(selectedDateMillis) {
                            dayHeaderFormat.format(Date(selectedDateMillis)).replaceFirstChar { it.uppercase() }
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (tasksForSelectedDay.isEmpty()) {
                                stringResource(R.string.calendar_no_tasks_day)
                            } else {
                                "${tasksForSelectedDay.size} ${if (tasksForSelectedDay.size == 1) "tarea" else "tareas"} programadas"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Add Task Button for this specific day
                    FilledTonalButton(
                        onClick = { onAddTaskForDate(selectedDateMillis) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add_task), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. TASKS FOR SELECTED DAY LIST
            if (tasksForSelectedDay.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddTaskForDate(selectedDateMillis) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.calendar_no_tasks_day),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.calendar_add_task_day),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                items(tasksForSelectedDay, key = { it.id }) { task ->
                    CalendarTaskItemCard(
                        task = task,
                        categoryColor = getCategoryColor(task.category, categoryColors[task.category]),
                        onToggleComplete = { onToggleComplete(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }

            // 4. UPCOMING DEADLINES IN THE NEXT 7 DAYS
            if (upcomingTasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.calendar_upcoming_deadlines),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(upcomingTasks.take(5), key = { "upcoming_${it.id}" }) { task ->
                    CalendarTaskItemCard(
                        task = task,
                        categoryColor = getCategoryColor(task.category, categoryColors[task.category]),
                        onToggleComplete = { onToggleComplete(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Individual Day Cell inside the Month Grid with task status indicator dots.
 */
@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    tasks: List<Task>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingTasks = tasks.filter { !it.isCompleted }
    val hasTasks = tasks.isNotEmpty()

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else if (isToday) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else if (hasTasks) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                fontSize = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Normal,
                color = textColor
            )

            // Task dots row
            if (hasTasks) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val dotColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        pendingTasks.any { it.priority == TaskPriority.CRITICAL || it.priority == TaskPriority.HIGH } -> Color(0xFFE53935)
                        pendingTasks.any { it.priority == TaskPriority.MEDIUM } -> Color(0xFFFB8C00)
                        pendingTasks.isNotEmpty() -> MaterialTheme.colorScheme.primary
                        else -> Color(0xFF4CAF50) // All completed on this day
                    }

                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )

                    if (tasks.size > 1) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(dotColor.copy(alpha = 0.6f))
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(7.dp))
            }
        }
    }
}

/**
 * Task card designed for calendar agenda view.
 */
@Composable
private fun CalendarTaskItemCard(
    task: Task,
    categoryColor: Color,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = BorderStroke(
            1.2.dp,
            if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority vertical stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when (task.priority) {
                            TaskPriority.CRITICAL -> Color(0xFFE53935)
                            TaskPriority.HIGH -> Color(0xFFFF5722)
                            TaskPriority.MEDIUM -> Color(0xFFFF9800)
                            TaskPriority.LOW -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Title & Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Category chip
                    Surface(
                        color = categoryColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.category,
                            color = categoryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Due Time (if set)
                    if (!task.dueTime.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = task.dueTime!!,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Subtasks counter
                    val subtasks = task.getSubtasks()
                    if (subtasks.isNotEmpty()) {
                        val completedCount = subtasks.count { it.isCompleted }
                        Text(
                            text = "$completedCount/${subtasks.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick actions
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
