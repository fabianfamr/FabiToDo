package com.fabian.todolist.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.todolist.R
import com.fabian.todolist.data.SystemCategory
import com.fabian.todolist.ui.Appicons

/**
 * Adaptive Material 3 Expressive Navigation Rail for Tablets, Foldables & Large Screens.
 * Provides instant ergonomics and category switching without requiring modal drawers.
 */
@Composable
fun TaskNavigationRail(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onOpenCategoriesDrawer: () -> Unit,
    onAddClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .shadow(4.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            ) {
                // App Logo Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.app_name),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expressive FAB attached to Rail
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        painter = painterResource(Appicons.ICON_ADD),
                        contentDescription = stringResource(R.string.add_task),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Todas las tareas
            val isAllSelected = selectedCategory == SystemCategory.ALL_TASKS
            NavigationRailItem(
                selected = isAllSelected,
                onClick = { onCategorySelected(SystemCategory.ALL_TASKS) },
                icon = {
                    Icon(
                        imageVector = if (isAllSelected) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = stringResource(R.string.drawer_all_lists)
                    )
                },
                label = { Text(stringResource(R.string.drawer_all_lists), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )

            // 2. Matriz Eisenhower
            val isEisenSelected = selectedCategory == SystemCategory.EISENHOWER
            NavigationRailItem(
                selected = isEisenSelected,
                onClick = { onCategorySelected(SystemCategory.EISENHOWER) },
                icon = {
                    Icon(
                        imageVector = if (isEisenSelected) Icons.Rounded.GridView else Icons.Outlined.GridView,
                        contentDescription = stringResource(R.string.priority_matrix)
                    )
                },
                label = { Text(stringResource(R.string.priority_matrix), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )

            // 3. Categorías Custom (Abre selector/drawer)
            val isCustomCategory = selectedCategory != SystemCategory.ALL_TASKS &&
                    selectedCategory != SystemCategory.EISENHOWER &&
                    selectedCategory != SystemCategory.COMPLETED &&
                    selectedCategory != SystemCategory.TRASH
            NavigationRailItem(
                selected = isCustomCategory,
                onClick = onOpenCategoriesDrawer,
                icon = {
                    Icon(
                        imageVector = if (isCustomCategory) Icons.Filled.Folder else Icons.Outlined.Folder,
                        contentDescription = stringResource(R.string.nav_categories)
                    )
                },
                label = { Text(stringResource(R.string.nav_categories), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )

            // 4. Completadas
            val isCompSelected = selectedCategory == SystemCategory.COMPLETED
            NavigationRailItem(
                selected = isCompSelected,
                onClick = { onCategorySelected(SystemCategory.COMPLETED) },
                icon = {
                    Icon(
                        imageVector = if (isCompSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.status_completed)
                    )
                },
                label = { Text(stringResource(R.string.status_completed), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )

            // 5. Papelera
            val isTrashSelected = selectedCategory == SystemCategory.TRASH
            NavigationRailItem(
                selected = isTrashSelected,
                onClick = { onCategorySelected(SystemCategory.TRASH) },
                icon = {
                    Icon(
                        imageVector = if (isTrashSelected) Icons.Filled.Delete else Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.trash_title)
                    )
                },
                label = { Text(stringResource(R.string.trash_title), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Acciones inferiores (Estadísticas y Ajustes)
            NavigationRailItem(
                selected = false,
                onClick = onStatsClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = stringResource(R.string.stats_label)
                    )
                },
                label = { Text(stringResource(R.string.stats_label), fontSize = 11.sp, maxLines = 1) }
            )

            NavigationRailItem(
                selected = false,
                onClick = onSettingsClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                },
                label = { Text(stringResource(R.string.settings), fontSize = 11.sp, maxLines = 1) }
            )
        }
    }
}
