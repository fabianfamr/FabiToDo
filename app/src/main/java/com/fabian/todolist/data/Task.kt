package com.fabian.todolist.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["isCompleted"]),
        Index(value = ["isDeleted"]),
        Index(value = ["dueDate"]),
        Index(value = ["cloudId"]),
        Index(value = ["updatedAt"])
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long? = null,
    val dueTime: String? = null, // "HH:mm" formatted
    val isCompleted: Boolean = false,
    val category: String = "General",
    val priority: String = "Media", // Baja, Media, Alta
    val reminderTime: Long? = null, // specific timestamp for exact alarm trigger
    val isRepeat: Boolean = false,
    val repeatType: String = "Ninguno", // Ninguno, Diario, Semanal, Mensual
    val displayOrder: Int = 0,
    @androidx.room.ColumnInfo(name = "subtasksJson") val subtasks: List<Subtask> = emptyList(),
    val attachedImageUri: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false,
    @androidx.room.ColumnInfo(defaultValue = "0") val deletedTimestamp: Long = 0L,
    val cloudId: String = java.util.UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    @androidx.room.ColumnInfo(defaultValue = "0") val isSynced: Boolean = false
)

@Immutable
data class Subtask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false
)

fun Task.getSubtasks(): List<Subtask> = subtasks

fun Task.withSubtasks(list: List<Subtask>): Task = this.copy(subtasks = list)

fun Task.subtasksToJson(): String {
    val arr = org.json.JSONArray()
    for (sub in subtasks) {
        val obj = org.json.JSONObject().apply {
            put("id", sub.id)
            put("title", sub.title)
            put("isCompleted", sub.isCompleted)
        }
        arr.put(obj)
    }
    return arr.toString()
}
