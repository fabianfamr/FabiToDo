package com.fabian.todolist.ui.components.addtask

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.todolist.R
import com.fabian.todolist.util.getCategoryColor
import com.fabian.todolist.util.getCategoryIconVector
import com.fabian.todolist.util.getLocalizedCategoryName

@Composable
fun CategorySelectionRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onAddCategory: () -> Unit,
    categoryColors: Map<String, Int>,
    categoryIcons: Map<String, String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.dialog_task_list_header),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Option: None
            item {
                val isSelected = selectedCategory.isBlank()
                CategoryChip(
                    isSelected = isSelected,
                    label = stringResource(R.string.category_none),
                    icon = Icons.Default.Folder,
                    onClick = { onCategorySelected("") }
                )
            }

            // Active category lists
            items(categories, key = { it }) { category ->
                val isSelected = selectedCategory == category
                val customColor = getCategoryColor(category, categoryColors[category])
                val customIcon = getCategoryIconVector(category, categoryIcons[category])
                
                CategoryChip(
                    isSelected = isSelected,
                    label = getLocalizedCategoryName(category),
                    icon = customIcon,
                    color = customColor,
                    onClick = { onCategorySelected(category) }
                )
            }
            
            // Add category button
            item {
                CategoryChip(
                    isSelected = false,
                    label = stringResource(R.string.add),
                    icon = Icons.Default.Add,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onAddCategory
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    isSelected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    color: Color? = null
) {
    val baseColor = color ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) (color?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.primaryContainer) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) (color ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        ),
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) (color ?: MaterialTheme.colorScheme.onPrimaryContainer) else baseColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) (color ?: MaterialTheme.colorScheme.onPrimaryContainer) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PrioritySelectionRow(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.priority),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val priorities = listOf(
                Triple(com.fabian.todolist.data.TaskPriority.LOW, R.drawable.ic_priority_low, com.fabian.todolist.ui.theme.AppColors.PriorityLow),
                Triple(com.fabian.todolist.data.TaskPriority.MEDIUM, R.drawable.ic_priority_medium, com.fabian.todolist.ui.theme.AppColors.PriorityMedium),
                Triple(com.fabian.todolist.data.TaskPriority.HIGH, R.drawable.ic_priority_high, com.fabian.todolist.ui.theme.AppColors.PriorityHigh),
                Triple(com.fabian.todolist.data.TaskPriority.CRITICAL, R.drawable.ic_priority_urgent, com.fabian.todolist.ui.theme.AppColors.PriorityCritical)
            )

            priorities.forEach { (pName, pDrawable, baseColor) ->
                val isSelected = selectedPriority == pName
                val pLabel = when(pName) {
                    com.fabian.todolist.data.TaskPriority.CRITICAL -> stringResource(R.string.priority_critical)
                    com.fabian.todolist.data.TaskPriority.HIGH -> stringResource(R.string.priority_high)
                    com.fabian.todolist.data.TaskPriority.MEDIUM -> stringResource(R.string.priority_medium)
                    com.fabian.todolist.data.TaskPriority.LOW -> stringResource(R.string.priority_low)
                    else -> pName
                }
                Card(
                    onClick = { onPrioritySelected(pName) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) baseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) baseColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(
                                painter = painterResource(pDrawable),
                                contentDescription = null,
                                tint = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Option below row to completely clear priorities (Sin prioridad option)
        val isNoPriority = selectedPriority == com.fabian.todolist.data.TaskPriority.NONE || selectedPriority == ""
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            onClick = { onPrioritySelected(com.fabian.todolist.data.TaskPriority.NONE) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNoPriority) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isNoPriority) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
            ),
            modifier = Modifier.align(Alignment.End).wrapContentSize()
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = if (isNoPriority) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.priority_none_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isNoPriority) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
