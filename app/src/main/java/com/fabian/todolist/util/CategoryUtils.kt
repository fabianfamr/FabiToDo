package com.fabian.todolist.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fabian.todolist.R
import com.fabian.todolist.data.SystemCategory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconCatalog {
    val ALL_ICONS: List<Pair<String, ImageVector>> = listOf(
        "List" to Icons.AutoMirrored.Filled.List,
        "Work" to Icons.Default.Work,
        "Person" to Icons.Default.Person,
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "Home" to Icons.Default.Home,
        "Favorite" to Icons.Default.Favorite,
        "Star" to Icons.Default.Star,
        "School" to Icons.Default.School,
        "Book" to Icons.Default.Book,
        "Build" to Icons.Default.Build,
        "Call" to Icons.Default.Call,
        "Camera" to Icons.Default.CameraAlt,
        "Car" to Icons.Default.DirectionsCar,
        "Run" to Icons.Default.DirectionsRun,
        "Bike" to Icons.Default.DirectionsBike,
        "Flight" to Icons.Default.Flight,
        "Restaurant" to Icons.Default.Restaurant,
        "Music" to Icons.Default.MusicNote,
        "Pets" to Icons.Default.Pets,
        "Fitness" to Icons.Default.FitnessCenter,
        "Brush" to Icons.Default.Brush,
        "Code" to Icons.Default.Code,
        "Computer" to Icons.Default.Computer,
        "Lightbulb" to Icons.Default.Lightbulb,
        "Notifications" to Icons.Default.Notifications,
        "Settings" to Icons.Default.Settings,
        "Security" to Icons.Default.Security,
        "Shield" to Icons.Default.Shield,
        "ShoppingBag" to Icons.Default.ShoppingBag,
        "Store" to Icons.Default.Store,
        "Payments" to Icons.Default.Payments,
        "Money" to Icons.Default.AttachMoney,
        "Bank" to Icons.Default.AccountBalance,
        "Card" to Icons.Default.CreditCard,
        "TrendingUp" to Icons.Default.TrendingUp,
        "BarChart" to Icons.Default.BarChart,
        "PieChart" to Icons.Default.PieChart,
        "Folder" to Icons.Default.Folder,
        "Cloud" to Icons.Default.Cloud,
        "Email" to Icons.Default.Email,
        "Chat" to Icons.Default.Chat,
        "Forum" to Icons.Default.Forum,
        "Headphones" to Icons.Default.Headphones,
        "Movie" to Icons.Default.Movie,
        "Videocam" to Icons.Default.Videocam,
        "Image" to Icons.Default.Image,
        "Palette" to Icons.Default.Palette,
        "Eco" to Icons.Default.Eco,
        "Sunny" to Icons.Default.WbSunny,
        "Night" to Icons.Default.Nightlight,
        "Cafe" to Icons.Default.LocalCafe,
        "Bar" to Icons.Default.LocalBar,
        "Cake" to Icons.Default.Cake,
        "Fastfood" to Icons.Default.Fastfood,
        "Medication" to Icons.Default.Medication,
        "Hospital" to Icons.Default.LocalHospital,
        "Event" to Icons.Default.Event,
        "Schedule" to Icons.Default.Schedule,
        "Alarm" to Icons.Default.Alarm,
        "Timer" to Icons.Default.Timer,
        "Flag" to Icons.Default.Flag,
        "Bookmark" to Icons.Default.Bookmark,
        "PushPin" to Icons.Default.PushPin,
        "Label" to Icons.Default.Label,
        "Tag" to Icons.Default.Tag,
        "Lock" to Icons.Default.Lock,
        "Key" to Icons.Default.Key,
        "Visibility" to Icons.Default.Visibility,
        "Search" to Icons.Default.Search,
        "Explore" to Icons.Default.Explore,
        "Map" to Icons.Default.Map,
        "Place" to Icons.Default.Place,
        "Navigation" to Icons.Default.Navigation,
        "Location" to Icons.Default.LocationOn,
        "Phone" to Icons.Default.Phone,
        "Share" to Icons.Default.Share,
        "Send" to Icons.Default.Send,
        "Inbox" to Icons.Default.Inbox,
        "Mail" to Icons.Default.Mail,
        "Archive" to Icons.Default.Archive,
        "Delete" to Icons.Default.Delete,
        "Refresh" to Icons.Default.Refresh,
        "Sync" to Icons.Default.Sync,
        "CloudUpload" to Icons.Default.CloudUpload,
        "CloudDownload" to Icons.Default.CloudDownload,
        "File" to Icons.Default.Description,
        "Task" to Icons.Default.Task,
        "Assignment" to Icons.Default.Assignment,
        "CheckCircle" to Icons.Default.CheckCircle,
        "Warning" to Icons.Default.Warning,
        "Info" to Icons.Default.Info,
        "Help" to Icons.Default.Help,
        "ThumbUp" to Icons.Default.ThumbUp,
        "Public" to Icons.Default.Public,
        "Language" to Icons.Default.Language,
        "Group" to Icons.Default.Group,
        "People" to Icons.Default.People,
        "Badge" to Icons.Default.Badge,
        "Rocket" to Icons.Default.Rocket,
        "Celebration" to Icons.Default.Celebration
    )

    private val iconMap by lazy { ALL_ICONS.toMap() }

    fun getIcon(name: String?): ImageVector? {
        if (name == null) return null
        return iconMap[name]
    }
}

val CATEGORY_COLOR_PALETTE: List<Int> = listOf(
    0xFF9C27B0.toInt(), // Purple
    0xFF673AB7.toInt(), // Deep Purple
    0xFF3F51B5.toInt(), // Indigo
    0xFF2196F3.toInt(), // Blue
    0xFF03A9F4.toInt(), // Light Blue
    0xFF00BCD4.toInt(), // Cyan
    0xFF009688.toInt(), // Teal
    0xFF4CAF50.toInt(), // Green
    0xFF8BC34A.toInt(), // Light Green
    0xFFCDDC39.toInt(), // Lime
    0xFFFFEB3B.toInt(), // Yellow
    0xFFFFC107.toInt(), // Amber
    0xFFFF9800.toInt(), // Orange
    0xFFFF5722.toInt(), // Deep Orange
    0xFFF44336.toInt(), // Red
    0xFFE91E63.toInt(), // Pink
    0xFF795548.toInt(), // Brown
    0xFF607D8B.toInt(), // Blue Gray
    0xFF9E9E9E.toInt(), // Gray
    0xFF333333.toInt()  // Dark Slate
)

@Composable
fun getLocalizedCategoryName(category: String): String {
    return when (category) {
        "Todas", "All", "Toutes", "Todos", "Alle", "Todas las tareas", "All tasks" -> stringResource(R.string.drawer_all_lists)
        "Finalizadas", "Finished", "Completadas", "Done", "Completed" -> stringResource(R.string.status_completed)
        "Papelera", "Trash", "Corbeille", "Papierkorb", "Cestino", "Lixeira" -> stringResource(R.string.trash_title)
        "General", "Général", "Allgemein" -> stringResource(R.string.settings_category_general)
        "Personal", "Personnel" -> stringResource(R.string.category_personal)
        "Trabajo", "Work", "Travail", "Arbeit" -> stringResource(R.string.category_work)
        "Compras", "Shopping", "Achats", "Einkaufen" -> stringResource(R.string.category_shopping)
        "Hogar", "Home", "Maison", "Casa", "Zuhause" -> stringResource(R.string.category_home)
        "Salud", "Health", "Santé", "Saúde", "Gesundheit" -> stringResource(R.string.category_health)
        "Matriz de Eisenhower", "Matriz de Prioridades" -> stringResource(R.string.priority_matrix)
        "Calendario", "Calendar", "Agenda" -> stringResource(R.string.calendar_nav_title)
        else -> category
    }
}

fun getCategoryIconVector(category: String, iconName: String?): ImageVector {
    val catalogIcon = CategoryIconCatalog.getIcon(iconName)
    if (catalogIcon != null) {
        return catalogIcon
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
