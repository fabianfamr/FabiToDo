package com.fabian.todolist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.todolist.R
import com.fabian.todolist.util.getLocalizedCategoryName
import com.fabian.todolist.util.getCategoryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTopAppBar(
    selectedCategory: String,
    categoryColors: Map<String, Int>,
    pendingTasksCount: Int,
    onMenuClick: () -> Unit,
    onSortClick: () -> Unit,
    onToggleSearch: () -> Unit,
    onStatsClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Text(
                    text = "FabiToDo", 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val catColorInt = categoryColors[selectedCategory]
                val catColor = getCategoryColor(selectedCategory, catColorInt)
                
                Surface(
                    color = catColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = getLocalizedCategoryName(selectedCategory), 
                        fontSize = 13.sp,
                        color = catColor, 
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_label), tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        actions = {
            IconButton(onClick = onStatsClick) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = stringResource(R.string.stats_label),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(R.string.sort_by),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            if (pendingTasksCount > 0) {
                IconButton(onClick = onToggleSearch, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_title), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.shadow(elevation = 2.dp)
    )
}
