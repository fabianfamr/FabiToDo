package com.fabian.todolist.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class TaskConverters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val subtaskListType = Types.newParameterizedType(List::class.java, Subtask::class.java)
    private val adapter = moshi.adapter<List<Subtask>>(subtaskListType)

    @TypeConverter
    fun fromSubtaskList(subtasks: List<Subtask>?): String {
        return adapter.toJson(subtasks ?: emptyList())
    }

    @TypeConverter
    fun toSubtaskList(json: String?): List<Subtask> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
