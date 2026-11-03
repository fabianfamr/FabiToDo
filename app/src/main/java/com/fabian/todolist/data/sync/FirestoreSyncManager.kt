package com.fabian.todolist.data.sync

import android.content.Context
import android.util.Log
import com.fabian.todolist.data.Subtask
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.TaskDao
import com.fabian.todolist.data.subtasksToJson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

@Singleton
class FirestoreSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tasksListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var currentActiveUserId: String? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(
        context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            .getLong("last_firestore_sync_time", 0L)
    )
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    fun startRealtimeSync(userId: String) {
        if (userId.isBlank()) return
        if (currentActiveUserId == userId && tasksListener != null) return

        stopRealtimeSync()
        currentActiveUserId = userId

        // 1. Escuchar cambios en tareas
        val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
        tasksListener = tasksCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreSyncManager", "Tasks SnapshotListener error", error)
                _syncStatus.value = SyncStatus.ERROR
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                syncScope.launch {
                    try {
                        for (doc in snapshot.documents) {
                            val remoteTask = parseDocumentToTask(doc) ?: continue
                            val localTask = taskDao.getTaskByCloudId(remoteTask.cloudId)

                            if (localTask == null) {
                                taskDao.insertTask(remoteTask.copy(id = 0, isSynced = true))
                            } else {
                                if (remoteTask.updatedAt > localTask.updatedAt) {
                                    taskDao.updateTask(remoteTask.copy(id = localTask.id, isSynced = true))
                                } else if (localTask.updatedAt > remoteTask.updatedAt && !localTask.isSynced) {
                                    uploadTaskToFirestore(userId, localTask)
                                }
                            }
                        }

                        updateLastSyncTimestamp()
                        _syncStatus.value = SyncStatus.SUCCESS
                    } catch (e: Exception) {
                        Log.e("FirestoreSyncManager", "Error processing remote task snapshot", e)
                        _syncStatus.value = SyncStatus.ERROR
                    }
                }
            }
        }

        // 2. Escuchar cambios en categorías y ajustes
        val settingsDoc = firestore.collection("users").document(userId).collection("settings").document("preferences")
        settingsListener = settingsDoc.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            syncScope.launch {
                applyRemoteSettings(snapshot)
            }
        }

        // Subir tareas no sincronizadas e iniciar ajustes
        syncScope.launch {
            uploadUnsyncedLocalTasks(userId)
            pushUserSettings(userId)
        }
    }

    fun stopRealtimeSync() {
        tasksListener?.remove()
        tasksListener = null
        settingsListener?.remove()
        settingsListener = null
        currentActiveUserId = null
        _syncStatus.value = SyncStatus.IDLE
    }

    suspend fun syncAllNow(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        _syncStatus.value = SyncStatus.SYNCING

        try {
            // 1. Subir tareas pendientes
            uploadUnsyncedLocalTasks(userId)

            // 2. Sincronizar ajustes y categorías
            val settingsDocRef = firestore.collection("users").document(userId).collection("settings").document("preferences")
            val settingsSnap = settingsDocRef.get().await()
            if (settingsSnap.exists()) {
                applyRemoteSettings(settingsSnap)
            }
            pushUserSettings(userId)

            // 3. Descargar todas las tareas remotas
            val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
            val remoteDocs = tasksCollection.get().await()

            for (doc in remoteDocs.documents) {
                val remoteTask = parseDocumentToTask(doc) ?: continue
                val localTask = taskDao.getTaskByCloudId(remoteTask.cloudId)

                if (localTask == null) {
                    if (!remoteTask.isDeleted) {
                        taskDao.insertTask(remoteTask.copy(id = 0, isSynced = true))
                    }
                } else {
                    if (remoteTask.isDeleted) {
                        taskDao.deleteTask(localTask)
                    } else if (remoteTask.updatedAt > localTask.updatedAt) {
                        taskDao.updateTask(remoteTask.copy(id = localTask.id, isSynced = true))
                    } else if (localTask.updatedAt > remoteTask.updatedAt && !localTask.isSynced) {
                        uploadTaskToFirestore(userId, localTask)
                    }
                }
            }

            updateLastSyncTimestamp()
            _syncStatus.value = SyncStatus.SUCCESS
            true
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error performing syncAllNow", e)
            _syncStatus.value = SyncStatus.ERROR
            false
        }
    }

    suspend fun pushTask(userId: String, task: Task) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            uploadTaskToFirestore(userId, task)
            taskDao.markTasksSynced(listOf(task.id))
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error pushing task ${task.cloudId}", e)
        }
    }

    suspend fun pushTasks(userId: String, tasks: List<Task>) = withContext(Dispatchers.IO) {
        if (userId.isBlank() || tasks.isEmpty()) return@withContext
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection("tasks")
            tasks.forEach { task ->
                val docRef = collection.document(task.cloudId)
                batch.set(docRef, taskToFirestoreMap(task), SetOptions.merge())
            }
            batch.commit().await()
            taskDao.markTasksSynced(tasks.map { it.id })
            updateLastSyncTimestamp()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error batch pushing tasks", e)
        }
    }

    suspend fun pushUserSettings(userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "") = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            val settingsMap = mapOf(
                "customCategories" to (prefs.getString("custom_categories", "[]") ?: "[]"),
                "categoryColors" to (prefs.getString("category_colors", "{}") ?: "{}"),
                "categoryIcons" to (prefs.getString("category_icons", "{}") ?: "{}"),
                "themeAccent" to (prefs.getString("accent_color", "system") ?: "system"),
                "themeDark" to (prefs.getString("theme_mode", "system") ?: "system"),
                "languageCode" to (prefs.getString("app_locale", "system") ?: "system"),
                "dateFormat" to (prefs.getString("date_format", "DD/MM/AA") ?: "DD/MM/AA"),
                "timeFormat" to (prefs.getString("time_format", "24h") ?: "24h"),
                "globalNotificationsEnabled" to prefs.getBoolean("global_notifications_enabled", true),
                "aiModel" to (prefs.getString("ai_model", "gemini-3.8-flash") ?: "gemini-3.8-flash"),
                "aiLowLatencyMode" to prefs.getBoolean("ai_low_latency_mode", true),
                "aiSubtaskCount" to prefs.getInt("ai_subtask_count", 5),
                "trashRetentionDays" to prefs.getInt("trash_retention_days", 30),
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(userId).collection("settings").document("preferences")
                .set(settingsMap, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error pushing user settings", e)
        }
    }

    private fun applyRemoteSettings(doc: DocumentSnapshot) {
        try {
            val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val localLastSync = prefs.getLong("last_settings_cloud_sync", 0L)

            if (remoteUpdatedAt <= localLastSync) return

            val editor = prefs.edit()

            doc.getString("customCategories")?.let { editor.putString("custom_categories", it) }
            doc.getString("categoryColors")?.let { editor.putString("category_colors", it) }
            doc.getString("categoryIcons")?.let { editor.putString("category_icons", it) }
            doc.getString("themeAccent")?.let { editor.putString("accent_color", it) }
            doc.getString("themeDark")?.let { editor.putString("theme_mode", it) }
            doc.getString("languageCode")?.let { editor.putString("app_locale", it) }
            doc.getString("dateFormat")?.let { editor.putString("date_format", it) }
            doc.getString("timeFormat")?.let { editor.putString("time_format", it) }
            doc.getBoolean("globalNotificationsEnabled")?.let { editor.putBoolean("global_notifications_enabled", it) }
            doc.getString("aiModel")?.let { editor.putString("ai_model", it) }
            doc.getBoolean("aiLowLatencyMode")?.let { editor.putBoolean("ai_low_latency_mode", it) }
            doc.getLong("aiSubtaskCount")?.let { editor.putInt("ai_subtask_count", it.toInt()) }
            doc.getLong("trashRetentionDays")?.let { editor.putInt("trash_retention_days", it.toInt()) }

            editor.putLong("last_settings_cloud_sync", remoteUpdatedAt)
            editor.apply()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error applying remote settings", e)
        }
    }

    private suspend fun uploadUnsyncedLocalTasks(userId: String) {
        val unsynced = taskDao.getUnsyncedTasks()
        if (unsynced.isEmpty()) return

        try {
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection("tasks")
            unsynced.forEach { task ->
                val docRef = collection.document(task.cloudId)
                batch.set(docRef, taskToFirestoreMap(task), SetOptions.merge())
            }
            batch.commit().await()
            taskDao.markTasksSynced(unsynced.map { it.id })
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed to upload unsynced tasks", e)
        }
    }

    private suspend fun uploadTaskToFirestore(userId: String, task: Task) {
        val docRef = firestore.collection("users").document(userId).collection("tasks").document(task.cloudId)
        docRef.set(taskToFirestoreMap(task), SetOptions.merge()).await()
    }

    private fun updateLastSyncTimestamp() {
        val now = System.currentTimeMillis()
        _lastSyncTimestamp.value = now
        context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            .edit()
            .putLong("last_firestore_sync_time", now)
            .apply()
    }

    private fun taskToFirestoreMap(task: Task): Map<String, Any?> {
        return mapOf(
            "cloudId" to task.cloudId,
            "title" to task.title.take(500).ifBlank { "Tarea" },
            "description" to task.description,
            "dueDate" to task.dueDate,
            "dueTime" to task.dueTime,
            "isCompleted" to task.isCompleted,
            "category" to task.category,
            "priority" to task.priority,
            "reminderTime" to task.reminderTime,
            "isRepeat" to task.isRepeat,
            "repeatType" to task.repeatType,
            "displayOrder" to task.displayOrder,
            "subtasksJson" to task.subtasksToJson(),
            "attachedImageUri" to task.attachedImageUri,
            "isDeleted" to task.isDeleted,
            "deletedTimestamp" to task.deletedTimestamp,
            "updatedAt" to task.updatedAt
        )
    }

    private fun parseDocumentToTask(doc: DocumentSnapshot): Task? {
        val cloudId = doc.getString("cloudId") ?: doc.id
        val title = doc.getString("title") ?: return null
        val isCompleted = doc.getBoolean("isCompleted") ?: false
        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
        val description = doc.getString("description") ?: ""
        val dueDate = doc.getLong("dueDate")
        val dueTime = doc.getString("dueTime")
        val category = doc.getString("category") ?: "General"
        val priority = doc.getString("priority") ?: "Media"
        val reminderTime = doc.getLong("reminderTime")
        val isRepeat = doc.getBoolean("isRepeat") ?: false
        val repeatType = doc.getString("repeatType") ?: "Ninguno"
        val displayOrder = doc.getLong("displayOrder")?.toInt() ?: 0
        val isDeleted = doc.getBoolean("isDeleted") ?: false
        val deletedTimestamp = doc.getLong("deletedTimestamp") ?: 0L
        val attachedImageUri = doc.getString("attachedImageUri")

        val subtasksJson = doc.getString("subtasksJson")
        val subtasks = parseSubtasks(subtasksJson)

        return Task(
            cloudId = cloudId,
            title = title,
            description = description,
            dueDate = dueDate,
            dueTime = dueTime,
            isCompleted = isCompleted,
            category = category,
            priority = priority,
            reminderTime = reminderTime,
            isRepeat = isRepeat,
            repeatType = repeatType,
            displayOrder = displayOrder,
            subtasks = subtasks,
            attachedImageUri = attachedImageUri,
            isDeleted = isDeleted,
            deletedTimestamp = deletedTimestamp,
            updatedAt = updatedAt,
            isSynced = true
        )
    }

    private fun parseSubtasks(json: String?): List<Subtask> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Subtask>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Subtask(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
