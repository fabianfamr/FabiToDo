package com.fabian.todolist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.fabian.todolist.R
import com.fabian.todolist.util.getCategoryColor
import com.fabian.todolist.util.getCategoryIconVector
import com.fabian.todolist.util.getLocalizedCategoryName

@Composable
fun TaskDrawerContent(
    drawerState: DrawerState,
    selectedCategory: String,
    categories: List<String>,
    categoryColors: Map<String, Int>,
    categoryIcons: Map<String, String>,
    languageCode: String,
    onCategorySelected: (String) -> Unit,
    onSettingsSelected: () -> Unit,
    showOnlyCategories: Boolean = false,
    onAddCategoryClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        // Top padding space replacing redundant app header
        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable categorized sections with delineated boxes
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(vertical = 4.dp)
        ) {
            if (!showOnlyCategories) {
                // Section 1: Main views
                Text(
                    text = stringResource(R.string.drawer_section_views),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 22.dp, top = 10.dp, bottom = 6.dp)
                )

                // 1. All tasks
                DrawerCardItem(
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedCategory == com.fabian.todolist.data.SystemCategory.ALL_TASKS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = stringResource(R.string.drawer_all_lists),
                    selected = selectedCategory == com.fabian.todolist.data.SystemCategory.ALL_TASKS,
                    onClick = {
                        onCategorySelected(com.fabian.todolist.data.SystemCategory.ALL_TASKS)
                        scope.launch { drawerState.close() }
                    }
                )

                // 2. Calendar view
                DrawerCardItem(
                    icon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedCategory == com.fabian.todolist.data.SystemCategory.CALENDAR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                        )
                    },
                    label = stringResource(R.string.calendar_nav_title),
                    badgeText = stringResource(R.string.calendar_switch_calendar),
                    selected = selectedCategory == com.fabian.todolist.data.SystemCategory.CALENDAR,
                    onClick = {
                        onCategorySelected(com.fabian.todolist.data.SystemCategory.CALENDAR)
                        scope.launch { drawerState.close() }
                    }
                )

                // 3. Priority Matrix (Eisenhower)
                DrawerCardItem(
                    icon = {
                        Icon(
                            Icons.Rounded.GridView,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedCategory == com.fabian.todolist.data.SystemCategory.EISENHOWER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = stringResource(R.string.priority_matrix),
                    selected = selectedCategory == com.fabian.todolist.data.SystemCategory.EISENHOWER,
                    onClick = {
                        onCategorySelected(com.fabian.todolist.data.SystemCategory.EISENHOWER)
                        scope.launch { drawerState.close() }
                    }
                )
            }

            // Section 2: Categories
            val userCategories = categories.filter {
                it != com.fabian.todolist.data.SystemCategory.ALL_TASKS && it != com.fabian.todolist.data.SystemCategory.COMPLETED
            }
            if (userCategories.isNotEmpty() || showOnlyCategories) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.drawer_section_categories),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 22.dp, top = 6.dp, bottom = 6.dp)
                )

                userCategories.forEach { category ->
                    val customColor = getCategoryColor(category, categoryColors[category])
                    val customIcon = getCategoryIconVector(category, categoryIcons[category])
                    DrawerCardItem(
                        icon = {
                            Icon(
                                customIcon,
                                contentDescription = null,
                                tint = customColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = getLocalizedCategoryName(category),
                        selected = selectedCategory == category,
                        tintColor = customColor,
                        onClick = {
                            onCategorySelected(category)
                            scope.launch { drawerState.close() }
                        }
                    )
                }

                // Add Category Button Item
                DrawerCardItem(
                    icon = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = stringResource(R.string.add_category_title),
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onAddCategoryClick()
                    }
                )
            }

            if (!showOnlyCategories) {
                // Section 3: System & Status
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.drawer_section_system),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 22.dp, top = 6.dp, bottom = 6.dp)
                )

                // Completed
                DrawerCardItem(
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedCategory == com.fabian.todolist.data.SystemCategory.COMPLETED) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
                        )
                    },
                    label = stringResource(R.string.status_completed),
                    selected = selectedCategory == com.fabian.todolist.data.SystemCategory.COMPLETED,
                    onClick = {
                        onCategorySelected(com.fabian.todolist.data.SystemCategory.COMPLETED)
                        scope.launch { drawerState.close() }
                    }
                )

                // Trash
                DrawerCardItem(
                    icon = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedCategory == com.fabian.todolist.data.SystemCategory.TRASH) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = stringResource(R.string.trash_title),
                    selected = selectedCategory == com.fabian.todolist.data.SystemCategory.TRASH,
                    onClick = {
                        onCategorySelected(com.fabian.todolist.data.SystemCategory.TRASH)
                        scope.launch { drawerState.close() }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (!showOnlyCategories) {
            // Settings box at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Surface(
                    onClick = {
                        onSettingsSelected()
                        scope.launch { drawerState.close() }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Delineated Box Item for navigation drawers.
 */
@Composable
private fun DrawerCardItem(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    tintColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) tintColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (badgeText != null) {
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

