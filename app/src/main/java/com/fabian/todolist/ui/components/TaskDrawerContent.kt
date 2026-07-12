package com.fabian.todolist.ui.components

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
    onSettingsSelected: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        // Transparent background header to integrate smoothly
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Scrollable lists elements with proper spacing (avoid "boxy" rects)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. All tasks category
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text(text = stringResource(R.string.drawer_all_lists)) },
                selected = selectedCategory == com.fabian.todolist.data.SystemCategory.ALL_TASKS,
                shape = RoundedCornerShape(16.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { 
                    onCategorySelected(com.fabian.todolist.data.SystemCategory.ALL_TASKS)
                    scope.launch { drawerState.close() }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
            )

            // 2. Custom User categories
            categories.filter { it != com.fabian.todolist.data.SystemCategory.ALL_TASKS && it != com.fabian.todolist.data.SystemCategory.COMPLETED }.forEach { category ->
                val customColor = getCategoryColor(category, categoryColors[category])
                val customIcon = getCategoryIconVector(category, categoryIcons[category])
                NavigationDrawerItem(
                    icon = { Icon(customIcon, contentDescription = null, tint = customColor) },
                    label = { Text(getLocalizedCategoryName(category)) },
                    selected = selectedCategory == category,
                    shape = RoundedCornerShape(16.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = {
                        onCategorySelected(category)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                )
            }

            // 3. Completed group
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                label = { Text(text = stringResource(R.string.status_completed)) },
                selected = selectedCategory == com.fabian.todolist.data.SystemCategory.COMPLETED,
                shape = RoundedCornerShape(16.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { 
                    onCategorySelected(com.fabian.todolist.data.SystemCategory.COMPLETED)
                    scope.launch { drawerState.close() }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
            )

            // 4. Trash bin
            NavigationDrawerItem(
                icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                label = { Text(text = stringResource(R.string.trash_title)) },
                selected = selectedCategory == com.fabian.todolist.data.SystemCategory.TRASH,
                shape = RoundedCornerShape(16.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = { 
                    onCategorySelected(com.fabian.todolist.data.SystemCategory.TRASH)
                    scope.launch { drawerState.close() }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
            )

            // Divider before matrix
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 5. Priority Matrix (Eisenhower)
            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.GridView, contentDescription = null) },
                label = { Text(text = stringResource(R.string.priority_matrix)) },
                selected = selectedCategory == com.fabian.todolist.data.SystemCategory.EISENHOWER,
                shape = RoundedCornerShape(16.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                onClick = {
                    onCategorySelected(com.fabian.todolist.data.SystemCategory.EISENHOWER)
                    scope.launch { drawerState.close() }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Settings option at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                label = { Text(text = stringResource(R.string.settings)) },
                selected = false,
                shape = RoundedCornerShape(16.dp),
                onClick = {
                    onSettingsSelected()
                    scope.launch { drawerState.close() }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

