package com.fabian.todolist.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fabian.todolist.R
import com.fabian.todolist.data.SystemCategory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun getLocalizedCategoryName(category: String): String {
    return when (category) {
        "Todas", "All", "Toutes", "Todos", "Alle", "Todas las tareas", "All tasks" -> stringResource(R.string.drawer_all_lists)
        "Finalizadas", "Finished", "Completadas", "Done" -> stringResource(R.string.status_completed)
        "General", "Général", "Allgemein" -> stringResource(R.string.settings_category_general)
        "Personal", "Personnel" -> stringResource(R.string.category_personal)
        "Trabajo", "Work", "Travail", "Arbeit" -> stringResource(R.string.category_work)
        "Compras", "Shopping", "Achats", "Einkaufen" -> stringResource(R.string.category_shopping)
        "Hogar", "Home", "Maison", "Casa", "Zuhause" -> stringResource(R.string.category_home)
        "Salud", "Health", "Santé", "Saúde", "Gesundheit" -> stringResource(R.string.category_health)
        "Matriz de Eisenhower", "Matriz de Prioridades" -> stringResource(R.string.priority_matrix)
        else -> category
    }
}

fun getCategoryIconVector(category: String, iconName: String?): ImageVector {
    if (iconName != null) {
        return when (iconName) {
            "List" -> Icons.AutoMirrored.Filled.List
            "Work" -> Icons.Default.Work
            "Person" -> Icons.Default.Person
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "Home" -> Icons.Default.Home
            "Favorite" -> Icons.Default.Favorite
            "Star" -> Icons.Default.Star
            "School" -> Icons.Default.School
            else -> Icons.AutoMirrored.Filled.List
        }
    }
    return when (category) {
        "Personal" -> Icons.Default.Person
        "Trabajo", "Work" -> Icons.Default.Work
        "Compras", "Shopping" -> Icons.Default.ShoppingCart
        "Hogar", "Home" -> Icons.Default.Home
        "Salud", "Health" -> Icons.Default.Favorite
        SystemCategory.COMPLETED -> Icons.Default.CheckCircle
        else -> Icons.AutoMirrored.Filled.List
    }
}

fun getCategoryColor(category: String, colorInt: Int?): Color {
    if (colorInt != null) {
        return Color(colorInt)
    }
    return when (category) {
        "Personal" -> Color(0xFF9C27B0) // Purple
        "Trabajo", "Work" -> Color(0xFFFF9800) // Orange
        "Compras", "Shopping" -> Color(0xFFE91E63) // Pink
        "Hogar", "Home" -> Color(0xFF4CAF50) // Green
        "Salud", "Health" -> Color(0xFFF44336) // Red
        SystemCategory.COMPLETED -> Color(0xFF009688) // Teal
        else -> Color(0xFF9E9E9E) // Gray
    }
}
