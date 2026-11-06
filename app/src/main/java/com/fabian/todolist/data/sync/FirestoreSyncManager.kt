package com.fabian.todolist.data.sync

import android.content.Context
import android.util.Log
import com.fabian.todolist.data.Subtask
import com.fabian.todolist.data.SystemCategory
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
import org.json.JSONObject
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
                                if (!remoteTask.isDeleted) {
                                    taskDao.insertTask(remoteTask.copy(id = 0, isSynced = true))
                                }
                            } else {
                                if (remoteTask.isDeleted) {
                                    if (!localTask.isDeleted) {
                                        taskDao.updateTask(localTask.copy(isDeleted = true, deletedTimestamp = remoteTask.deletedTimestamp, isSynced = true))
                                    } else {
                                        taskDao.markTasksSynced(listOf(localTask.id))
                                    }
                                } else if (remoteTask.updatedAt > localTask.updatedAt) {
                                    taskDao.updateTask(remoteTask.copy(id = localTask.id, isSynced = true))
                                } else if (localTask.updatedAt > remoteTask.updatedAt && !localTask.isSynced) {
                                    uploadTaskToFirestore(userId, localTask)
                                } else if (!localTask.isSynced && remoteTask.updatedAt >= localTask.updatedAt) {
                                    taskDao.markTasksSynced(listOf(localTask.id))
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
            if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
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
                        if (!localTask.isDeleted) {
                            taskDao.updateTask(localTask.copy(isDeleted = true, deletedTimestamp = remoteTask.deletedTimestamp, isSynced = true))
                        } else {
                            taskDao.markTasksSynced(listOf(localTask.id))
                        }
                    } else if (remoteTask.updatedAt > localTask.updatedAt) {
                        taskDao.updateTask(remoteTask.copy(id = localTask.id, isSynced = true))
                    } else if (localTask.updatedAt > remoteTask.updatedAt && !localTask.isSynced) {
                        uploadTaskToFirestore(userId, localTask)
                    } else if (!localTask.isSynced && remoteTask.updatedAt >= localTask.updatedAt) {
                        taskDao.markTasksSynced(listOf(localTask.id))
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

    suspend fun deleteRemoteTask(userId: String, cloudId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank() || cloudId.isBlank()) return@withContext
        try {
            firestore.collection("users").document(userId).collection("tasks").document(cloudId).delete().await()
            updateLastSyncTimestamp()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error deleting remote task $cloudId", e)
        }
    }

    suspend fun deleteRemoteTasks(userId: String, cloudIds: List<String>) = withContext(Dispatchers.IO) {
        val validIds = cloudIds.filter { it.isNotBlank() }
        if (userId.isBlank() || validIds.isEmpty()) return@withContext
        try {
            val collection = firestore.collection("users").document(userId).collection("tasks")
            validIds.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { id ->
                    batch.delete(collection.document(id))
                }
                batch.commit().await()
            }
            updateLastSyncTimestamp()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error batch deleting remote tasks", e)
        }
    }

    suspend fun pushUserSettings(userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "") = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val localUpdate = prefs.getLong("last_settings_local_update", now)
            val updatedAt = maxOf(now, localUpdate)

            // Auto-populate user display name from FirebaseAuth if not set in prefs
            val currentUser = FirebaseAuth.getInstance().currentUser
            val currentNameInPrefs = prefs.getString("user_display_name", "") ?: ""
            val resolvedName = if (currentNameInPrefs.isNotBlank()) {
                currentNameInPrefs
            } else {
                currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: ""
            }
            if (resolvedName.isNotBlank() && currentNameInPrefs.isBlank()) {
                prefs.edit().putString("user_display_name", resolvedName).apply()
            }

            // Category configuration as defined by the user
            val rawCatsJson = prefs.getString("custom_categories", null)
            val parsedList = parseCategoryList(rawCatsJson ?: "")
            val userCats = parsedList.filter { !SystemCategory.isSystemCategory(it) }
            val finalUserCats = if (rawCatsJson != null) {
                userCats
            } else {
                listOf("Personal", "Trabajo", "Compras", "Hogar", "Salud")
            }

            val safeCategoriesList = listOf(SystemCategory.ALL_TASKS) + finalUserCats + listOf(SystemCategory.COMPLETED)
            val jsonArray = JSONArray()
            safeCategoriesList.forEach { jsonArray.put(it) }
            val finalCatsJson = jsonArray.toString()
            prefs.edit().putString("custom_categories", finalCatsJson).apply()

            val settingsMap = mapOf(
                "customCategories" to finalCatsJson,
                "categoryColors" to (prefs.getString("category_colors", "{}") ?: "{}"),
                "categoryIcons" to (prefs.getString("category_icons", "{}") ?: "{}"),
                "themeAccent" to (prefs.getString("accent_color", "system") ?: "system"),
                "themeDark" to (prefs.getString("theme_mode", "system") ?: "system"),
                "languageCode" to (prefs.getString("app_locale", "system") ?: "system"),
                "dateFormat" to (prefs.getString("date_format", "DD/MM/AA") ?: "DD/MM/AA"),
                "timeFormat" to (prefs.getString("time_format", "24h") ?: "24h"),
                "longPressAction" to (prefs.getString("long_press_action", "select") ?: "select"),
                "confirmOnDelete" to prefs.getBoolean("confirm_on_delete", true),
                "hapticFeedbackOnComplete" to prefs.getBoolean("haptic_feedback_on_complete", true),
                "requireBiometrics" to prefs.getBoolean("require_biometrics", false),
                "secureScreenMode" to prefs.getBoolean("secure_screen_mode", false),
                "encryptBackups" to prefs.getBoolean("encrypt_backups", false),
                "globalNotificationsEnabled" to prefs.getBoolean("global_notifications_enabled", true),
                "quickAddNotificationEnabled" to prefs.getBoolean("quick_add_notification_enabled", false),
                "quickAddNotificationInterval" to (prefs.getString("quick_add_notification_interval", "off") ?: "off"),
                "quickAddQuietHoursEnabled" to prefs.getBoolean("quick_add_quiet_hours_enabled", true),
                "notificationsSound" to prefs.getBoolean("notifications_sound", true),
                "notificationsVibrate" to prefs.getBoolean("notifications_vibrate", true),
                "reminderOffsetMinutes" to prefs.getInt("reminder_offset_minutes", 0),
                "snoozeDurationMinutes" to prefs.getInt("snooze_duration_minutes", 10),
                "aiModel" to (prefs.getString("ai_model", "gemini-3.8-flash") ?: "gemini-3.8-flash"),
                "aiLowLatencyMode" to prefs.getBoolean("ai_low_latency_mode", true),
                "aiSubtaskCount" to prefs.getInt("ai_subtask_count", 5),
                "aiKeyType" to (prefs.getString("ai_key_type", "public") ?: "public"),
                "showAiBanner" to prefs.getBoolean("show_ai_banner", true),
                "showBackupCard" to prefs.getBoolean("show_backup_card", true),
                "userDisplayName" to resolvedName,
                "trashRetentionDays" to prefs.getInt("trash_retention_days", 30),
                "updatedAt" to updatedAt
            )
            firestore.collection("users").document(userId).collection("settings").document("preferences")
                .set(settingsMap, SetOptions.merge()).await()

            prefs.edit()
                .putLong("last_settings_cloud_sync", updatedAt)
                .putLong("last_settings_local_update", updatedAt)
                .apply()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error pushing user settings", e)
        }
    }

    private suspend fun applyRemoteSettings(doc: DocumentSnapshot) {
        try {
            val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
            val localLastUpdate = prefs.getLong("last_settings_local_update", 0L)
            val localLastSync = prefs.getLong("last_settings_cloud_sync", 0L)

            if (localLastUpdate >= remoteUpdatedAt && localLastUpdate > 0) {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (currentUserId.isNotBlank()) {
                    syncScope.launch {
                        pushUserSettings(currentUserId)
                    }
                }
                return
            }

            if (remoteUpdatedAt <= localLastSync && localLastSync > 0) return

            val editor = prefs.edit()

            val localCats = prefs.getString("custom_categories", "") ?: ""
            doc.getString("customCategories")?.let { remoteCats ->
                val mergedCats = mergeCategoriesJson(localCats, remoteCats)
                editor.putString("custom_categories", mergedCats)
            }

            val localColors = prefs.getString("category_colors", "{}") ?: "{}"
            doc.getString("categoryColors")?.let { remoteColors ->
                val mergedColors = mergeJsonObjectStrings(localColors, remoteColors)
                editor.putString("category_colors", mergedColors)
            }

            val localIcons = prefs.getString("category_icons", "{}") ?: "{}"
            doc.getString("categoryIcons")?.let { remoteIcons ->
                val mergedIcons = mergeJsonObjectStrings(localIcons, remoteIcons)
                editor.putString("category_icons", mergedIcons)
            }

            doc.getString("themeAccent")?.let { editor.putString("accent_color", it) }
            doc.getString("themeDark")?.let { editor.putString("theme_mode", it) }
            doc.getString("languageCode")?.let { editor.putString("app_locale", it) }
            doc.getString("dateFormat")?.let { editor.putString("date_format", it) }
            doc.getString("timeFormat")?.let { editor.putString("time_format", it) }
            doc.getString("longPressAction")?.let { editor.putString("long_press_action", it) }
            doc.getBoolean("confirmOnDelete")?.let { editor.putBoolean("confirm_on_delete", it) }
            doc.getBoolean("hapticFeedbackOnComplete")?.let { editor.putBoolean("haptic_feedback_on_complete", it) }
            doc.getBoolean("requireBiometrics")?.let { editor.putBoolean("require_biometrics", it) }
            doc.getBoolean("secureScreenMode")?.let { editor.putBoolean("secure_screen_mode", it) }
            doc.getBoolean("encryptBackups")?.let { editor.putBoolean("encrypt_backups", it) }
            doc.getBoolean("globalNotificationsEnabled")?.let { editor.putBoolean("global_notifications_enabled", it) }
            doc.getBoolean("quickAddNotificationEnabled")?.let { editor.putBoolean("quick_add_notification_enabled", it) }
            doc.getString("quickAddNotificationInterval")?.let { editor.putString("quick_add_notification_interval", it) }
            doc.getBoolean("quickAddQuietHoursEnabled")?.let { editor.putBoolean("quick_add_quiet_hours_enabled", it) }
            doc.getBoolean("notificationsSound")?.let { editor.putBoolean("notifications_sound", it) }
            doc.getBoolean("notificationsVibrate")?.let { editor.putBoolean("notifications_vibrate", it) }
            doc.getLong("reminderOffsetMinutes")?.let { editor.putInt("reminder_offset_minutes", it.toInt()) }
            doc.getLong("snoozeDurationMinutes")?.let { editor.putInt("snooze_duration_minutes", it.toInt()) }
            doc.getString("aiModel")?.let { editor.putString("ai_model", it) }
            doc.getBoolean("aiLowLatencyMode")?.let { editor.putBoolean("ai_low_latency_mode", it) }
            doc.getLong("aiSubtaskCount")?.let { editor.putInt("ai_subtask_count", it.toInt()) }
            doc.getString("aiKeyType")?.let { editor.putString("ai_key_type", it) }
            doc.getBoolean("showAiBanner")?.let { editor.putBoolean("show_ai_banner", it) }
            doc.getBoolean("showBackupCard")?.let { editor.putBoolean("show_backup_card", it) }
            doc.getString("userDisplayName")?.let { remoteName ->
                if (remoteName.isNotBlank()) {
                    editor.putString("user_display_name", remoteName)
                }
            }
            doc.getLong("trashRetentionDays")?.let { editor.putInt("trash_retention_days", it.toInt()) }

            val newSyncTime = maxOf(remoteUpdatedAt, System.currentTimeMillis())
            editor.putLong("last_settings_cloud_sync", newSyncTime)
            editor.putLong("last_settings_local_update", newSyncTime)
            editor.apply()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error applying remote settings", e)
        }
    }

    private suspend fun mergeCategoriesJson(localJson: String, remoteJson: String): String {
        try {
            val localList = parseCategoryList(localJson)
            val remoteList = parseCategoryList(remoteJson)
            val combinedUserCats = (localList + remoteList).filter { !SystemCategory.isSystemCategory(it) }.distinct()

            val fromTasks = try {
                taskDao.getAllTasksSync().map { it.category.trim() }.filter { it.isNotBlank() && !SystemCategory.isSystemCategory(it) }.distinct()
            } catch (_: Exception) { emptyList() }

            val allUserCats = (combinedUserCats + fromTasks).distinct()
            val finalUserCats = if (allUserCats.isNotEmpty()) {
                allUserCats
            } else {
                listOf("Personal", "Trabajo", "Compras", "Hogar", "Salud")
            }

            val mergedList = mutableListOf<String>()
            mergedList.add(SystemCategory.ALL_TASKS)
            mergedList.addAll(finalUserCats)
            mergedList.add(SystemCategory.COMPLETED)

            val array = JSONArray()
            mergedList.forEach { array.put(it) }
            return array.toString()
        } catch (_: Exception) {
            return if (remoteJson.isNotBlank()) remoteJson else localJson
        }
    }

    private fun parseCategoryList(jsonStr: String): List<String> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.getString(i).trim()
                if (item.isNotEmpty() && !list.contains(item)) {
                    list.add(item)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun mergeJsonObjectStrings(localJson: String, remoteJson: String): String {
        try {
            val localObj = JSONObject(if (localJson.isBlank()) "{}" else localJson)
            val remoteObj = JSONObject(if (remoteJson.isBlank()) "{}" else remoteJson)
            val merged = JSONObject()

            val localKeys = localObj.keys()
            while (localKeys.hasNext()) {
                val k = localKeys.next()
                merged.put(k, localObj.get(k))
            }

            val remoteKeys = remoteObj.keys()
            while (remoteKeys.hasNext()) {
                val k = remoteKeys.next()
                merged.put(k, remoteObj.get(k))
            }

            return merged.toString()
        } catch (_: Exception) {
            return if (remoteJson.isNotBlank()) remoteJson else localJson
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
