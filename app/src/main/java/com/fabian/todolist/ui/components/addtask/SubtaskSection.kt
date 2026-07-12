package com.fabian.todolist.ui.components.addtask

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.todolist.R
import com.fabian.todolist.data.Subtask

@Composable
fun SubtaskSection(
    localSubtasks: List<Subtask>,
    onSubtasksChanged: (List<Subtask>) -> Unit,
    isSubtasksExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    title: String,
    description: String,
    categories: List<String>,
    isGeneratingAI: Boolean,
    onGenerateSubtasksWithAI: ((String, String, String?, List<String>, (String, String, String, List<String>) -> Unit, (String) -> Unit) -> Unit)?,
    onDescriptionChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onPriorityChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var newSubtaskText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChanged(!isSubtasksExpanded) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FactCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.subtasks_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (localSubtasks.isNotEmpty()) {
                        val completedCount = localSubtasks.count { it.isCompleted }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$completedCount/${localSubtasks.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isSubtasksExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(visible = isSubtasksExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (localSubtasks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.subtasks_empty),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        localSubtasks.forEachIndexed { index, subtask ->
                            key(subtask.id) {
                                var isDragging by remember { mutableStateOf(false) }
                                var offsetY by remember { mutableStateOf(0f) }

                                val rowBackgroundColor = if (isDragging) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    Color.Transparent
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBackgroundColor, RoundedCornerShape(8.dp))
                                        .padding(vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
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
                                                        val currentIndex = localSubtasks.indexOf(subtask)
                                                        if (offsetY > 100f && currentIndex < localSubtasks.size - 1) {
                                                            val updatedList = localSubtasks.toMutableList().apply {
                                                                val temp = get(currentIndex)
                                                                set(currentIndex, get(currentIndex + 1))
                                                                set(currentIndex + 1, temp)
                                                            }
                                                            onSubtasksChanged(updatedList)
                                                            offsetY = 0f
                                                        } else if (offsetY < -100f && currentIndex > 0) {
                                                            val updatedList = localSubtasks.toMutableList().apply {
                                                                val temp = get(currentIndex)
                                                                set(currentIndex, get(currentIndex - 1))
                                                                set(currentIndex - 1, temp)
                                                            }
                                                            onSubtasksChanged(updatedList)
                                                            offsetY = 0f
                                                        }
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragIndicator,
                                            contentDescription = stringResource(R.string.desc_drag_handle),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Checkbox(
                                        checked = subtask.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            val updatedList = localSubtasks.toMutableList().apply {
                                                this[index] = subtask.copy(isCompleted = isChecked)
                                            }
                                            onSubtasksChanged(updatedList)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = subtask.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (subtask.isCompleted) {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        textDecoration = if (subtask.isCompleted) {
                                            TextDecoration.LineThrough
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val updatedList = localSubtasks.toMutableList().apply {
                                                removeAt(index)
                                            }
                                            onSubtasksChanged(updatedList)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete_subtask_desc),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (title.isNotBlank() && onGenerateSubtasksWithAI != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            val magColor = Color(0xFF6366F1)
                            OutlinedButton(
                                onClick = {
                                    val existingSubtasksText = localSubtasks.joinToString(separator = "\n") { "- ${it.title} (Completada: ${it.isCompleted})" }
                                    onGenerateSubtasksWithAI(
                                        title,
                                        description,
                                        existingSubtasksText.takeIf { it.isNotBlank() },
                                        categories,
                                        { genDesc, genCat, genPriority, subtasks ->
                                            if (genDesc.isNotBlank()) {
                                                onDescriptionChanged(genDesc)
                                            }
                                            if (genCat.isNotBlank()) {
                                                onCategoryChanged(genCat)
                                            }
                                            if (genPriority.isNotBlank()) {
                                                onPriorityChanged(genPriority)
                                            }
                                            onSubtasksChanged(localSubtasks + subtasks.map { Subtask(title = it) })
                                        },
                                        { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                enabled = !isGeneratingAI,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                border = BorderStroke(1.2.dp, magColor.copy(alpha = 0.45f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = magColor.copy(alpha = 0.08f),
                                    contentColor = magColor
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isGeneratingAI) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = magColor, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.subtasks_generating), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = magColor)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.subtasks_generate_magic), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Outlined subtask addition layout
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newSubtaskText,
                            onValueChange = { newSubtaskText = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.subtasks_add_placeholder),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (newSubtaskText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            if (newSubtaskText.isNotBlank()) {
                                                onSubtasksChanged(localSubtasks + Subtask(title = newSubtaskText.trim()))
                                                newSubtaskText = ""
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Subtask Button",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (newSubtaskText.isNotBlank()) {
                                        onSubtasksChanged(localSubtasks + Subtask(title = newSubtaskText.trim()))
                                        newSubtaskText = ""
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
