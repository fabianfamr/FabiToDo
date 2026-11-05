package com.fabian.todolist.data

object SystemCategory {
    const val ALL_TASKS = "Todas las tareas"
    const val COMPLETED = "Finalizadas"
    const val TRASH = "Papelera"
    const val EISENHOWER = "Matriz de Eisenhower"
    const val CALENDAR = "Calendario"
    const val GENERAL = "General"

    fun isSystemCategory(category: String): Boolean {
        val trimmed = category.trim()
        return trimmed.equals(ALL_TASKS, ignoreCase = true) ||
               trimmed.equals(COMPLETED, ignoreCase = true) ||
               trimmed.equals("Completed", ignoreCase = true) ||
               trimmed.equals("Completada", ignoreCase = true) ||
               trimmed.equals("Completadas", ignoreCase = true) ||
               trimmed.equals("Todas", ignoreCase = true) ||
               trimmed.equals(TRASH, ignoreCase = true) ||
               trimmed.equals("Trash", ignoreCase = true) ||
               trimmed.equals("Papelera", ignoreCase = true) ||
               trimmed.equals(CALENDAR, ignoreCase = true) ||
               trimmed.equals("Calendar", ignoreCase = true) ||
               trimmed.equals("Calendario", ignoreCase = true) ||
               trimmed.equals(EISENHOWER, ignoreCase = true) ||
               trimmed.equals("Eisenhower", ignoreCase = true)
    }
}

object TaskPriority {
    const val CRITICAL = "Critica"
    const val HIGH = "Alta"
    const val MEDIUM = "Media"
    const val LOW = "Baja"
    const val NONE = "Sin prioridad"
}

object SortOrder {
    const val MANUAL = "Manual"
    const val DUE_DATE = "Fecha límite"
    const val PRIORITY = "Prioridad"
    const val ALPHABETICAL = "Alfabético"
}
