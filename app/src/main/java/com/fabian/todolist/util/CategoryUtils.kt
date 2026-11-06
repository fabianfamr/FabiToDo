package com.fabian.todolist.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fabian.todolist.R
import com.fabian.todolist.data.SystemCategory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

private fun String.stripAccents(): String {
    val temp = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    return Regex("\\p{InCombiningDiacriticalMarks}+").replace(temp, "")
}

data class IconEntry(
    val name: String,
    val vector: ImageVector,
    val tags: List<String>
)

object CategoryIconCatalog {
    // Optimized curated set of 80 high-utility icons for maximum speed and zero tablet lag
    val ENTRIES: List<IconEntry> by lazy {
        listOf(
            IconEntry("List", Icons.AutoMirrored.Filled.List, listOf("lista", "list", "tareas", "tasks", "orden")),
            IconEntry("Work", Icons.Default.Work, listOf("trabajo", "work", "empleo", "job", "oficina")),
            IconEntry("Person", Icons.Default.Person, listOf("persona", "person", "usuario", "user", "perfil")),
            IconEntry("ShoppingCart", Icons.Default.ShoppingCart, listOf("compras", "shopping", "carrito", "cart", "tienda")),
            IconEntry("Home", Icons.Default.Home, listOf("hogar", "home", "casa", "house", "residencia")),
            IconEntry("Favorite", Icons.Default.Favorite, listOf("favorito", "favorite", "corazon", "heart", "amor")),
            IconEntry("Star", Icons.Default.Star, listOf("estrella", "star", "destacado", "rating")),
            IconEntry("School", Icons.Default.School, listOf("escuela", "school", "colegio", "estudios", "educacion")),
            IconEntry("Book", Icons.Default.Book, listOf("libro", "book", "lectura", "read", "estudio")),
            IconEntry("Build", Icons.Default.Build, listOf("herramientas", "tools", "construccion", "reparar")),
            IconEntry("Call", Icons.Default.Call, listOf("llamada", "call", "telefono", "phone", "contacto")),
            IconEntry("Camera", Icons.Default.CameraAlt, listOf("camara", "camera", "foto", "photo", "imagen")),
            IconEntry("Car", Icons.Default.DirectionsCar, listOf("coche", "car", "auto", "vehiculo", "conducir")),
            IconEntry("Run", Icons.Default.RunCircle, listOf("correr", "run", "ejercicio", "deporte", "fitness")),
            IconEntry("Bike", Icons.AutoMirrored.Filled.DirectionsBike, listOf("bicicleta", "bike", "ciclo", "transporte")),
            IconEntry("Flight", Icons.Default.Flight, listOf("vuelo", "flight", "avion", "airplane", "viaje")),
            IconEntry("Restaurant", Icons.Default.Restaurant, listOf("restaurante", "restaurant", "comida", "food", "cenar")),
            IconEntry("Music", Icons.Default.MusicNote, listOf("musica", "music", "cancion", "song", "audio")),
            IconEntry("Pets", Icons.Default.Pets, listOf("mascotas", "pets", "perro", "dog", "gato", "animal")),
            IconEntry("Fitness", Icons.Default.FitnessCenter, listOf("gimnasio", "gym", "fitness", "pesas", "ejercicio")),
            IconEntry("Brush", Icons.Default.Brush, listOf("pincel", "brush", "arte", "art", "pintura", "diseño")),
            IconEntry("Code", Icons.Default.Code, listOf("codigo", "code", "programacion", "dev", "desarrollo")),
            IconEntry("Computer", Icons.Default.Computer, listOf("ordenador", "computer", "pc", "laptop", "tecnologia")),
            IconEntry("Lightbulb", Icons.Default.Lightbulb, listOf("idea", "lightbulb", "bombilla", "luz", "creatividad")),
            IconEntry("Notifications", Icons.Default.Notifications, listOf("notificaciones", "notifications", "campana", "alerta")),
            IconEntry("Settings", Icons.Default.Settings, listOf("configuracion", "settings", "ajustes", "opciones")),
            IconEntry("Security", Icons.Default.Security, listOf("seguridad", "security", "candado", "escudo", "proteccion")),
            IconEntry("Shield", Icons.Default.Shield, listOf("escudo", "shield", "proteccion", "seguridad")),
            IconEntry("ShoppingBag", Icons.Default.ShoppingBag, listOf("bolsa", "bag", "compras", "shopping", "tienda")),
            IconEntry("Store", Icons.Default.Store, listOf("tienda", "store", "comercio", "shop", "mercado")),
            IconEntry("Payments", Icons.Default.Payments, listOf("pagos", "payments", "dinero", "money", "tarjeta")),
            IconEntry("Money", Icons.Default.AttachMoney, listOf("dinero", "money", "dolar", "dollar", "moneda")),
            IconEntry("Bank", Icons.Default.AccountBalance, listOf("banco", "bank", "finanzas", "cuenta", "ahorro")),
            IconEntry("Card", Icons.Default.CreditCard, listOf("tarjeta", "card", "credito", "pago")),
            IconEntry("Calendar", Icons.Default.CalendarToday, listOf("calendario", "calendar", "fecha", "date", "agenda")),
            IconEntry("Schedule", Icons.Default.Schedule, listOf("horario", "schedule", "reloj", "clock", "tiempo")),
            IconEntry("Alarm", Icons.Default.Alarm, listOf("alarma", "alarm", "despertador", "tiempo")),
            IconEntry("Timer", Icons.Default.Timer, listOf("temporizador", "timer", "cronometro")),
            IconEntry("Email", Icons.Default.Email, listOf("correo", "email", "mensaje", "message", "mail")),
            IconEntry("Chat", Icons.AutoMirrored.Filled.Chat, listOf("chat", "mensaje", "message", "conversacion")),
            IconEntry("Forum", Icons.Default.Forum, listOf("foro", "forum", "debate", "comentarios")),
            IconEntry("Folder", Icons.Default.Folder, listOf("carpeta", "folder", "archivo", "documento")),
            IconEntry("Description", Icons.Default.Description, listOf("documento", "description", "texto", "archivo")),
            IconEntry("Note", Icons.Default.Note, listOf("nota", "note", "apunte", "memo")),
            IconEntry("Task", Icons.Default.Task, listOf("tarea", "task", "check", "completado")),
            IconEntry("CheckCircle", Icons.Default.CheckCircle, listOf("completado", "check", "correcto", "success")),
            IconEntry("DoneAll", Icons.Default.DoneAll, listOf("todo listo", "done", "completado")),
            IconEntry("Flag", Icons.Default.Flag, listOf("bandera", "flag", "meta", "objetivo", "prioridad")),
            IconEntry("Label", Icons.Default.Label, listOf("etiqueta", "label", "tag")),
            IconEntry("Bookmark", Icons.Default.Bookmark, listOf("marcador", "bookmark", "guardado")),
            IconEntry("Coffee", Icons.Default.LocalCafe, listOf("cafe", "coffee", "desayuno", "break")),
            IconEntry("RestaurantMenu", Icons.Default.RestaurantMenu, listOf("menu", "comida", "receta")),
            IconEntry("LocalDining", Icons.Default.LocalDining, listOf("cenar", "dining", "plato", "comida")),
            IconEntry("Fastfood", Icons.Default.Fastfood, listOf("comida rapida", "fastfood", "hamburguesa")),
            IconEntry("Cake", Icons.Default.Cake, listOf("pastel", "cake", "cumpleaños", "fiesta")),
            IconEntry("Sports", Icons.Default.SportsBasketball, listOf("deporte", "sports", "baloncesto", "juego")),
            IconEntry("Game", Icons.Default.SportsEsports, listOf("juego", "game", "videojuego", "consola")),
            IconEntry("Movie", Icons.Default.Movie, listOf("pelicula", "movie", "cine", "film")),
            IconEntry("Tv", Icons.Default.Tv, listOf("television", "tv", "pantalla")),
            IconEntry("Headphones", Icons.Default.Headphones, listOf("auriculares", "headphones", "musica", "audio")),
            IconEntry("Mic", Icons.Default.Mic, listOf("microfono", "mic", "grabacion", "voz")),
            IconEntry("CameraIndoor", Icons.Default.CameraIndoor, listOf("casa", "interior", "camara")),
            IconEntry("Medical", Icons.Default.LocalHospital, listOf("hospital", "salud", "medico", "doctor")),
            IconEntry("Medication", Icons.Default.Medication, listOf("medicamento", "medicina", "pastilla", "salud")),
            IconEntry("LocalPharmacy", Icons.Default.LocalPharmacy, listOf("farmacia", "pharmacy", "medicamentos")),
            IconEntry("Healing", Icons.Default.Healing, listOf("curar", "healing", "tirita", "salud")),
            IconEntry("Science", Icons.Default.Science, listOf("ciencia", "science", "laboratorio", "quimica")),
            IconEntry("Psychology", Icons.Default.Psychology, listOf("psicologia", "psychology", "mente", "pensamiento")),
            IconEntry("Language", Icons.Default.Language, listOf("idioma", "language", "web", "mundo", "global")),
            IconEntry("Public", Icons.Default.Public, listOf("mundo", "public", "global", "planeta")),
            IconEntry("Location", Icons.Default.LocationOn, listOf("ubicacion", "location", "mapa", "gps", "lugar")),
            IconEntry("Map", Icons.Default.Map, listOf("mapa", "map", "direccion", "ruta")),
            IconEntry("Navigation", Icons.Default.Navigation, listOf("navegacion", "navigation", "ruta", "gps")),
            IconEntry("Hotel", Icons.Default.Hotel, listOf("hotel", "dormitorio", "cama", "viaje")),
            IconEntry("Luggage", Icons.Default.Luggage, listOf("maleta", "luggage", "viaje", "equipaje")),
            IconEntry("Train", Icons.Default.Train, listOf("tren", "train", "transporte", "viaje")),
            IconEntry("Bus", Icons.Default.DirectionsBus, listOf("autobus", "bus", "transporte")),
            IconEntry("Boat", Icons.Default.DirectionsBoat, listOf("barco", "boat", "mar", "navegar")),
            IconEntry("LocalGasStation", Icons.Default.LocalGasStation, listOf("gasolinera", "gas", "combustible", "auto")),
            IconEntry("CarRepair", Icons.Default.CarRepair, listOf("reparacion", "mecanico", "coche"))
        )
    }

    fun getIcon(name: String?): ImageVector? {
        if (name.isNullOrBlank()) return null
        return ENTRIES.find { it.name.equals(name, ignoreCase = true) }?.vector
    }

    fun searchIcons(query: String): List<IconEntry> {
        if (query.isBlank()) return ENTRIES
        val q = query.trim().lowercase().stripAccents()
        return ENTRIES.filter { entry ->
            entry.name.lowercase().contains(q) || entry.tags.any { it.stripAccents().contains(q) }
        }
    }
}

@Composable
fun getLocalizedCategoryName(category: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return getLocalizedCategoryName(context, category)
}

fun getLocalizedCategoryName(context: Context, category: String): String {
    return when (category.trim()) {
        SystemCategory.ALL_TASKS -> context.getString(R.string.category_all)
        SystemCategory.COMPLETED, "Completed" -> context.getString(R.string.status_completed)
        SystemCategory.TRASH, "Trash" -> context.getString(R.string.trash_title)
        SystemCategory.CALENDAR, "Calendar" -> context.getString(R.string.calendar_nav_title)
        SystemCategory.EISENHOWER, "Eisenhower" -> context.getString(R.string.priority_matrix)
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
