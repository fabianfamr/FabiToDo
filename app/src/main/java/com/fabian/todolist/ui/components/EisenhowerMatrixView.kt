package com.fabian.todolist.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.GridView
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

data class QuadrantInfo(
    val title: String,
    val description: String,
    val color: Color,
    val tasks: List<Task>
)

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
    var isInfoExpanded by rememberSaveable { mutableStateOf(true) }

    val quadrantsList = remember(tasks) {
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
        QuadrantInfo(
            title = stringResource(R.string.matrix_q1),
            description = stringResource(R.string.matrix_q1_desc),
            color = Color(0xFFEA4335),
            tasks = q1
        ),
        QuadrantInfo(
            title = stringResource(R.string.matrix_q2),
            description = stringResource(R.string.matrix_q2_desc),
            color = Color(0xFF4285F4),
            tasks = q2
        ),
        QuadrantInfo(
            title = stringResource(R.string.matrix_q3),
            description = stringResource(R.string.matrix_q3_desc),
            color = Color(0xFFFBBC05),
            tasks = q3
        ),
        QuadrantInfo(
            title = stringResource(R.string.matrix_q4),
            description = stringResource(R.string.matrix_q4_desc),
            color = Color(0xFF9E9E9E),
            tasks = q4
        )
    )

    val totalActiveTasks = q1.size + q2.size + q3.size + q4.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // 1. Explanation Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isInfoExpanded = !isInfoExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.matrix_info_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Icon(
                            imageVector = if (isInfoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    AnimatedVisibility(
                        visible = isInfoExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = stringResource(R.string.matrix_info_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Summary 2x2 Grid Pills
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuadrantSummaryChip(
                        title = "1. Hacer",
                        count = q1.size,
                        color = Color(0xFFEA4335),
                        modifier = Modifier.weight(1f)
                    )
                    QuadrantSummaryChip(
                        title = "2. Programar",
                        count = q2.size,
                        color = Color(0xFF4285F4),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuadrantSummaryChip(
                        title = "3. Delegar",
                        count = q3.size,
                        color = Color(0xFFFBBC05),
                        modifier = Modifier.weight(1f)
                    )
                    QuadrantSummaryChip(
                        title = "4. Descartar",
                        count = q4.size,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Quadrant Sections
        quadrants.forEach { quad ->
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(quad.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = quad.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = quad.color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${quad.tasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = quad.color,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = quad.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }

            if (quad.tasks.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = stringResource(R.string.matrix_empty_quadrant),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(quad.tasks, key = { _, it -> it.id }) { _, task ->
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

@Composable
private fun QuadrantSummaryChip(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}
