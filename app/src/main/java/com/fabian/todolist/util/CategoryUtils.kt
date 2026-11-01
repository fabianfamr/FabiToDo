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
    val ENTRIES: List<IconEntry> = listOf(
        IconEntry("List", Icons.AutoMirrored.Filled.List, listOf("lista", "list", "tareas", "tasks", "orden", "liste")),
        IconEntry("Work", Icons.Default.Work, listOf("trabajo", "work", "empleo", "job", "oficina", "office", "empresa", "travail", "arbeit")),
        IconEntry("Person", Icons.Default.Person, listOf("persona", "person", "usuario", "user", "perfil", "profile", "miembro")),
        IconEntry("ShoppingCart", Icons.Default.ShoppingCart, listOf("compras", "shopping", "carrito", "cart", "supermercado", "tienda", "store", "buy", "achats")),
        IconEntry("Home", Icons.Default.Home, listOf("hogar", "home", "casa", "house", "residencia", "maison", "zuhause")),
        IconEntry("Favorite", Icons.Default.Favorite, listOf("favorito", "favorite", "corazon", "heart", "salud", "health", "love", "amor")),
        IconEntry("Star", Icons.Default.Star, listOf("estrella", "star", "destacado", "favorite", "rating", "etoile")),
        IconEntry("School", Icons.Default.School, listOf("escuela", "school", "colegio", "estudios", "educacion", "education", "clase", "universidad", "ecole")),
        IconEntry("Book", Icons.Default.Book, listOf("libro", "book", "lectura", "read", "estudio", "library", "livre")),
        IconEntry("Build", Icons.Default.Build, listOf("herramientas", "tools", "construccion", "build", "reparar", "fix", "work")),
        IconEntry("Call", Icons.Default.Call, listOf("llamada", "call", "telefono", "phone", "contacto", "contact")),
        IconEntry("Camera", Icons.Default.CameraAlt, listOf("camara", "camera", "foto", "photo", "imagen", "picture")),
        IconEntry("Car", Icons.Default.DirectionsCar, listOf("coche", "car", "auto", "vehiculo", "vehicle", "conducir", "drive", "voiture")),
        IconEntry("Run", Icons.Default.RunCircle, listOf("correr", "run", "ejercicio", "exercise", "deporte", "sport", "fitness", "corredor")),
        IconEntry("Bike", Icons.Default.DirectionsBike, listOf("bicicleta", "bike", "ciclo", "cycling", "transporte", "bici", "velo")),
        IconEntry("Flight", Icons.Default.Flight, listOf("vuelo", "flight", "avion", "airplane", "plane", "viaje", "travel", "vacaciones", "vol")),
        IconEntry("Restaurant", Icons.Default.Restaurant, listOf("restaurante", "restaurant", "comida", "food", "cenar", "dinner", "almuerzo", "eat")),
        IconEntry("Music", Icons.Default.MusicNote, listOf("musica", "music", "cancion", "song", "audio", "nota", "note")),
        IconEntry("Pets", Icons.Default.Pets, listOf("mascotas", "pets", "perro", "dog", "gato", "cat", "animal")),
        IconEntry("Fitness", Icons.Default.FitnessCenter, listOf("gimnasio", "gym", "fitness", "pesas", "weights", "ejercicio", "exercise", "entrenamiento")),
        IconEntry("Brush", Icons.Default.Brush, listOf("pincel", "brush", "arte", "art", "pintura", "paint", "diseño", "design")),
        IconEntry("Code", Icons.Default.Code, listOf("codigo", "code", "programacion", "programming", "dev", "desarrollo", "software")),
        IconEntry("Computer", Icons.Default.Computer, listOf("ordenador", "computer", "pc", "laptop", "computadora", "tecnologia")),
        IconEntry("Lightbulb", Icons.Default.Lightbulb, listOf("idea", "lightbulb", "bombilla", "luz", "creatividad", "creativity")),
        IconEntry("Notifications", Icons.Default.Notifications, listOf("notificaciones", "notifications", "campana", "bell", "alerta", "alert")),
        IconEntry("Settings", Icons.Default.Settings, listOf("configuracion", "settings", "ajustes", "opciones", "gear", "parametres")),
        IconEntry("Security", Icons.Default.Security, listOf("seguridad", "security", "candado", "escudo", "proteccion", "protection")),
        IconEntry("Shield", Icons.Default.Shield, listOf("escudo", "shield", "proteccion", "protection", "seguridad", "security")),
        IconEntry("ShoppingBag", Icons.Default.ShoppingBag, listOf("bolsa", "bag", "compras", "shopping", "tienda", "store")),
        IconEntry("Store", Icons.Default.Store, listOf("tienda", "store", "comercio", "shop", "mercado", "market")),
        IconEntry("Payments", Icons.Default.Payments, listOf("pagos", "payments", "dinero", "money", "tarjeta", "finanzas", "finance")),
        IconEntry("Money", Icons.Default.AttachMoney, listOf("dinero", "money", "dolar", "dollar", "moneda", "cash", "efectivo", "finanzas")),
        IconEntry("Bank", Icons.Default.AccountBalance, listOf("banco", "bank", "finanzas", "finance", "cuenta", "ahorro", "banque")),
        IconEntry("Card", Icons.Default.CreditCard, listOf("tarjeta", "card", "credito", "credit", "pago", "payment", "carte")),
        IconEntry("TrendingUp", Icons.Default.TrendingUp, listOf("tendencia", "trending", "estadisticas", "stats", "grafico", "crecimiento")),
        IconEntry("BarChart", Icons.Default.BarChart, listOf("grafico", "chart", "barras", "bars", "estadisticas", "stats")),
        IconEntry("PieChart", Icons.Default.PieChart, listOf("pastel", "pie", "grafico", "chart", "estadisticas", "stats")),
        IconEntry("Folder", Icons.Default.Folder, listOf("carpeta", "folder", "archivos", "files", "documentos", "dossier")),
        IconEntry("Cloud", Icons.Default.Cloud, listOf("nube", "cloud", "sync", "sincronizacion", "online", "almacenamiento")),
        IconEntry("Email", Icons.Default.Email, listOf("email", "correo", "mail", "mensaje", "message")),
        IconEntry("Chat", Icons.Default.Chat, listOf("chat", "mensaje", "message", "conversacion", "hablar")),
        IconEntry("Forum", Icons.Default.Forum, listOf("foro", "forum", "comunidad", "community", "discusion")),
        IconEntry("Headphones", Icons.Default.Headphones, listOf("auriculares", "headphones", "musica", "audio", "cascos", "casque")),
        IconEntry("Movie", Icons.Default.Movie, listOf("pelicula", "movie", "cine", "cinema", "video", "film")),
        IconEntry("Videocam", Icons.Default.Videocam, listOf("video", "videocam", "camara", "grabacion", "record")),
        IconEntry("Image", Icons.Default.Image, listOf("imagen", "image", "foto", "picture", "galeria")),
        IconEntry("Palette", Icons.Default.Palette, listOf("paleta", "palette", "colores", "colors", "arte", "art", "diseño")),
        IconEntry("Eco", Icons.Default.Eco, listOf("eco", "hoja", "leaf", "naturaleza", "nature", "planta", "verde", "environment")),
        IconEntry("Sunny", Icons.Default.WbSunny, listOf("sol", "sun", "sunny", "dia", "day", "tiempo", "clima", "weather", "soleil")),
        IconEntry("Night", Icons.Default.Nightlight, listOf("noche", "night", "luna", "moon", "dormir", "sleep", "oscuridad", "nuit")),
        IconEntry("Cafe", Icons.Default.LocalCafe, listOf("cafe", "coffee", "cafeteria", "bebida", "drink", "desayuno")),
        IconEntry("Bar", Icons.Default.LocalBar, listOf("bar", "copa", "drink", "cerveza", "beer", "fiesta", "party")),
        IconEntry("Cake", Icons.Default.Cake, listOf("pastel", "cake", "tarta", "cumpleaños", "birthday", "fiesta", "celebracion", "gateau")),
        IconEntry("Fastfood", Icons.Default.Fastfood, listOf("comida", "food", "hamburguesa", "burger", "fastfood", "snack")),
        IconEntry("Medication", Icons.Default.Medication, listOf("medicina", "medication", "pastilla", "pill", "salud", "health", "farmacia", "médicament")),
        IconEntry("Hospital", Icons.Default.LocalHospital, listOf("hospital", "medico", "doctor", "salud", "health", "emergencia")),
        IconEntry("Event", Icons.Default.Event, listOf("evento", "event", "calendario", "calendar", "fecha", "date")),
        IconEntry("Schedule", Icons.Default.Schedule, listOf("horario", "schedule", "tiempo", "time", "plan", "reloj")),
        IconEntry("Alarm", Icons.Default.Alarm, listOf("alarma", "alarm", "despertador", "reloj", "clock")),
        IconEntry("Timer", Icons.Default.Timer, listOf("temporizador", "timer", "cronometro", "tiempo", "time")),
        IconEntry("Flag", Icons.Default.Flag, listOf("bandera", "flag", "meta", "goal", "marca", "prioridad", "drapeau")),
        IconEntry("Bookmark", Icons.Default.Bookmark, listOf("marcador", "bookmark", "guardar", "save", "favorito")),
        IconEntry("PushPin", Icons.Default.PushPin, listOf("chincheta", "pin", "fijar", "sticky", "nota")),
        IconEntry("Label", Icons.Default.Label, listOf("etiqueta", "label", "categoria", "tag")),
        IconEntry("Tag", Icons.Default.Tag, listOf("etiqueta", "tag", "categoria", "precio", "price")),
        IconEntry("Lock", Icons.Default.Lock, listOf("candado", "lock", "bloqueo", "seguridad", "privado", "private")),
        IconEntry("Key", Icons.Default.Key, listOf("llave", "key", "clave", "password", "acceso", "clef")),
        IconEntry("Visibility", Icons.Default.Visibility, listOf("ojo", "eye", "ver", "see", "mostrar", "show")),
        IconEntry("Search", Icons.Default.Search, listOf("buscar", "search", "lupa", "find", "encontrar", "cherche")),
        IconEntry("Explore", Icons.Default.Explore, listOf("explorar", "explore", "brujula", "compass", "descubrir")),
        IconEntry("Map", Icons.Default.Map, listOf("mapa", "map", "ubicacion", "location", "viaje", "travel", "carte")),
        IconEntry("Place", Icons.Default.Place, listOf("lugar", "place", "ubicacion", "location", "sitio")),
        IconEntry("Navigation", Icons.Default.Navigation, listOf("navegacion", "navigation", "gps", "direccion", "route")),
        IconEntry("Location", Icons.Default.LocationOn, listOf("ubicacion", "location", "gps", "mapa", "map", "sitio")),
        IconEntry("Phone", Icons.Default.Phone, listOf("telefono", "phone", "llamar", "call", "contacto")),
        IconEntry("Share", Icons.Default.Share, listOf("compartir", "share", "enviar", "send", "redes", "partager")),
        IconEntry("Send", Icons.Default.Send, listOf("enviar", "send", "correo", "mail", "mensaje")),
        IconEntry("Inbox", Icons.Default.Inbox, listOf("bandeja", "inbox", "correos", "emails", "mensajes")),
        IconEntry("Mail", Icons.Default.Mail, listOf("mail", "correo", "email", "mensaje", "carta")),
        IconEntry("Archive", Icons.Default.Archive, listOf("archivar", "archive", "guardar", "save", "caja")),
        IconEntry("Delete", Icons.Default.Delete, listOf("eliminar", "delete", "borrar", "basura", "trash")),
        IconEntry("Refresh", Icons.Default.Refresh, listOf("recargar", "refresh", "actualizar", "update", "repetir")),
        IconEntry("Sync", Icons.Default.Sync, listOf("sincronizar", "sync", "nube", "cloud", "actualizar")),
        IconEntry("CloudUpload", Icons.Default.CloudUpload, listOf("subir", "upload", "nube", "cloud", "cargar")),
        IconEntry("CloudDownload", Icons.Default.CloudDownload, listOf("descargar", "download", "nube", "cloud", "bajar")),
        IconEntry("File", Icons.Default.Description, listOf("archivo", "file", "documento", "document", "texto")),
        IconEntry("Task", Icons.Default.Task, listOf("tarea", "task", "checklist", "pendiente", "todo")),
        IconEntry("Assignment", Icons.Default.Assignment, listOf("tarea", "assignment", "proyecto", "project", "trabajo")),
        IconEntry("CheckCircle", Icons.Default.CheckCircle, listOf("hecho", "check", "completado", "done", "correcto")),
        IconEntry("Warning", Icons.Default.Warning, listOf("advertencia", "warning", "alerta", "alert", "peligro")),
        IconEntry("Info", Icons.Default.Info, listOf("informacion", "info", "ayuda", "help", "detalles")),
        IconEntry("Help", Icons.Default.Help, listOf("ayuda", "help", "pregunta", "question", "soporte")),
        IconEntry("ThumbUp", Icons.Default.ThumbUp, listOf("me gusta", "like", "aprobado", "ok", "good")),
        IconEntry("Public", Icons.Default.Public, listOf("publico", "public", "mundo", "world", "web", "internet")),
        IconEntry("Language", Icons.Default.Language, listOf("idioma", "language", "lenguaje", "traduccion", "global")),
        IconEntry("Group", Icons.Default.Group, listOf("grupo", "group", "equipo", "team", "personas", "people")),
        IconEntry("People", Icons.Default.People, listOf("personas", "people", "usuarios", "users", "gente")),
        IconEntry("Badge", Icons.Default.Badge, listOf("insignia", "badge", "medalla", "premio", "award")),
        IconEntry("Rocket", Icons.Default.Rocket, listOf("cohete", "rocket", "lanzamiento", "launch", "rapido", "fast")),
        IconEntry("Celebration", Icons.Default.Celebration, listOf("celebracion", "celebration", "fiesta", "party", "evento", "event"))
    )

    val ALL_ICONS: List<Pair<String, ImageVector>> by lazy {
        ENTRIES.map { it.name to it.vector }
    }

    private val iconMap by lazy { ALL_ICONS.toMap() }

    fun getIcon(name: String?): ImageVector? {
        if (name == null) return null
        return iconMap[name]
    }

    fun searchIcons(query: String): List<Pair<String, ImageVector>> {
        if (query.isBlank()) return ALL_ICONS
        val cleanQuery = query.trim().stripAccents().lowercase()
        return ENTRIES.filter { entry ->
            entry.name.stripAccents().lowercase().contains(cleanQuery) ||
            entry.tags.any { it.stripAccents().lowercase().contains(cleanQuery) }
        }.map { it.name to it.vector }
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
