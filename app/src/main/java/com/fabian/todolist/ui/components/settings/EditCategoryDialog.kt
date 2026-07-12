package com.fabian.todolist.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fabian.todolist.R
import com.fabian.todolist.ui.TaskViewModel
import com.fabian.todolist.ui.SettingsViewModel

@Composable
fun EditCategoryDialog(
    category: String,
    viewModel: TaskViewModel,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf(category) }
    var selectedColor by remember { mutableStateOf(android.graphics.Color.GRAY) } // default
    var selectedIconName by remember { mutableStateOf("List") } // default
    
    // Try to load existing color and icon
    val colors by settingsViewModel.categoryColors.collectAsStateWithLifecycle()
    val iconsMap by settingsViewModel.categoryIcons.collectAsStateWithLifecycle()
    
    LaunchedEffect(category) {
        val loadedColor = colors[category]
        if (loadedColor != null) selectedColor = loadedColor
        val loadedIcon = iconsMap[category]
        if (loadedIcon != null) selectedIconName = loadedIcon
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_category)) },
        text = {
            Column {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(R.string.list_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.category_color), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colorOptions = listOf(
                        android.graphics.Color.GRAY,
                        android.graphics.Color.RED,
                        android.graphics.Color.BLUE,
                        0xFF4CAF50.toInt(), // Green
                        0xFFFF9800.toInt(), // Orange
                        0xFF9C27B0.toInt(), // Purple
                        0xFFE91E63.toInt()  // Pink
                    )
                    items(colorOptions, key = { it }) { c ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(c), CircleShape)
                                .border(
                                    width = if (selectedColor == c) 3.dp else 0.dp,
                                    color = if (selectedColor == c) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = c },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == c) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.category_icon), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val iconOptions = listOf(
                        "List" to Icons.AutoMirrored.Filled.List,
                        "Work" to Icons.Default.Work,
                        "Person" to Icons.Default.Person,
                        "ShoppingCart" to Icons.Default.ShoppingCart,
                        "Home" to Icons.Default.Home,
                        "Favorite" to Icons.Default.Favorite,
                        "Star" to Icons.Default.Star,
                        "School" to Icons.Default.School
                    )
                    items(iconOptions, key = { it.first }) { (name, vector) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (selectedIconName == name) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                                .clickable { selectedIconName = name },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                vector, 
                                contentDescription = name, 
                                tint = if (selectedIconName == name) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                settingsViewModel.renameCategory(category, newCategoryName)
                settingsViewModel.setCategoryColor(newCategoryName, selectedColor)
                settingsViewModel.setCategoryIcon(newCategoryName, selectedIconName)
                onDismiss()
            }) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
