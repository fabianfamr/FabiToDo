package com.fabian.todolist.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.todolist.data.Task
import com.fabian.todolist.R
import com.fabian.todolist.ui.TaskDetailedRow
import java.util.Calendar
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EisenhowerMatrixView(
    tasks: List<Task>,
    categoryColors: Map<String, Int>,
    onToggleComplete: (Task) -> Unit,
    onRestore: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onUpdateSubtasks: (Task, List<com.fabian.todolist.data.Subtask>) -> Unit,
    selectedTaskIds: List<Int>,
    onToggleSelect: (Task) -> Unit,
    longPressAction: String,
    hapticFeedbackOnComplete: Boolean,
    confirmOnDelete: Boolean
) {
    val localView = androidx.compose.ui.platform.LocalView.current
    val scope = rememberCoroutineScope()

    val quadrantsList = remember(tasks) {
        val now = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
             add(Calendar.DAY_OF_YEAR, 1)
             set(Calendar.HOUR_OF_DAY, 23)
             set(Calendar.MINUTE, 59)
             set(Calendar.SECOND, 59)
        }
        val tomorrowMillis = tomorrow.timeInMillis

        val hHacerAhora = mutableListOf<Task>()
        val hProgramar = mutableListOf<Task>()
        val hDelegar = mutableListOf<Task>()
        val hEliminar = mutableListOf<Task>()

        for (task in tasks) {
            if (task.isCompleted || task.isDeleted) continue
            val isImportant = task.priority == "Critica" || task.priority == "Alta"
            val isUrgent = task.dueDate != null && task.dueDate <= tomorrowMillis

            if (isImportant && isUrgent) hHacerAhora.add(task)
            else if (isImportant && !isUrgent) hProgramar.add(task)
            else if (!isImportant && isUrgent) hDelegar.add(task)
            else hEliminar.add(task)
        }
        listOf(hHacerAhora, hProgramar, hDelegar, hEliminar)
    }

    val q1 = quadrantsList[0]
    val q2 = quadrantsList[1]
    val q3 = quadrantsList[2]
    val q4 = quadrantsList[3]

    val quadrants = listOf(
        Triple(stringResource(R.string.matrix_q1), Color(0xFFEA4335), q1),
        Triple(stringResource(R.string.matrix_q2), Color(0xFF4285F4), q2),
        Triple(stringResource(R.string.matrix_q3), Color(0xFFFBBC05), q3),
        Triple(stringResource(R.string.matrix_q4), Color(0xFF9E9E9E), q4)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        quadrants.forEach { (title, color, quadrantTasks) ->
            if (quadrantTasks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                itemsIndexed(quadrantTasks, key = { _, it -> it.id }) { index, task ->
                    var isVisible by rememberSaveable { mutableStateOf(true) }
                    var isExpanded by rememberSaveable { mutableStateOf(false) }

                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier.animateItem(
                            placementSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
                        )
                    ) {
                        TaskDetailedRow(
                            task = task,
                            isExpanded = isExpanded,
                            onToggleComplete = {
                                if (hapticFeedbackOnComplete && !task.isCompleted) {
                                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                                }
                                isVisible = false
                                scope.launch {
                                    delay(300)
                                    onToggleComplete(task)
                                }
                            },
                            onRestore = {
                                isVisible = false
                                onRestore(task)
                            },
                            onEdit = { onEdit(task) },
                            onDelete = {
                                if (hapticFeedbackOnComplete) {
                                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                                }
                                if (!confirmOnDelete) {
                                    isVisible = false
                                    scope.launch {
                                        delay(300)
                                        onDelete(task)
                                    }
                                } else {
                                    onDelete(task)
                                }
                            },
                            onClick = { isExpanded = !isExpanded },
                            categoryColors = categoryColors,
                            onUpdateSubtasks = { updatedSubtasks ->
                                onUpdateSubtasks(task, updatedSubtasks)
                            },
                            isSelected = selectedTaskIds.contains(task.id),
                            isSelectionModeActive = selectedTaskIds.isNotEmpty(),
                            onToggleSelect = { onToggleSelect(task) },
                            longPressAction = longPressAction
                        )
                    }
                }
            }
        }
    }
}
