package com.fabian.todolist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
    onStatsClick: () -> Unit,
    onCalendarClick: () -> Unit = {},
    isCalendarActive: Boolean = false,
    isDualPane: Boolean = false,
    showCalendarPane: Boolean = true,
    showNavigationIcon: Boolean = true
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
            if (showNavigationIcon) {
                Surface(
                    onClick = onMenuClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(start = 12.dp).size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.menu_label),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        actions = {
            // Sort Button
            Surface(
                onClick = onSortClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.padding(end = 6.dp).size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.sort_by),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Search Button
            Surface(
                onClick = onToggleSearch,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.padding(end = 12.dp).size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_title),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.drawBehind {
            val strokeWidth = 1.dp.toPx()
            drawLine(
                color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f),
                start = androidx.compose.ui.geometry.Offset(0f, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                strokeWidth = strokeWidth
            )
        }
    )
}
