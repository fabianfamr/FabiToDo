package com.fabian.todolist.data

import android.util.Log
import androidx.glance.appwidget.updateAll
import com.fabian.todolist.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val taskDao: TaskDao,
    private val authManager: AuthManager
) {
    private val TAG = "CloudSyncManager"
    private val PREFS_NAME = "fabitodo_sync_prefs"
    private val KEY_LAST_SYNC = "last_sync_timestamp"

    private val appContext = authManager.getContext().applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    private fun getLastSyncTimestamp(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)
    private fun saveLastSyncTimestamp(timestamp: Long) = prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()

    /**
     * Push local unsynced tasks to Firestore.
     * @return true if every chunk was pushed and marked synced successfully;
     *         false if there was any failure (network, Firestore, etc.).
     */
    suspend fun pushLocalChanges(): Boolean = withContext(Dispatchers.IO) {
        if (!authManager.isUserLoggedIn() || authManager.isGuestUser()) return@withContext true
        val user = authManager.getCurrentUser() ?: return@withContext true

        try {
            val unsyncedTasks = taskDao.getUnsyncedTasks()
            if (unsyncedTasks.isEmpty()) return@withContext true

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Pushing ${unsyncedTasks.size} tasks to cloud.")
            }

            val collectionRef = firestore.collection("users").document(user.uid).collection("tasks")

            var latestUpdate = 0L

            // Firestore limit is 500 operations per batch
            unsyncedTasks.chunked(500).forEach { chunk ->
                val batch = firestore.batch()

                for (task in chunk) {
                    if (task.updatedAt > latestUpdate) latestUpdate = task.updatedAt

                    val taskMap = mapOf(
                        "cloudId" to task.cloudId,
                        "title" to task.title,
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

                    batch.set(collectionRef.document(task.cloudId), taskMap, SetOptions.merge())
                }

                batch.commit().await()

                // Mark chunk as synced locally — batched transaction for performance and atomicity.
                taskDao.markTasksSynced(chunk.map { it.id })
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Push completed.")
            }
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error pushing changes", e)
            }
            false
        }
    }

    suspend fun pullRemoteChanges() = withContext(Dispatchers.IO) {
        if (!authManager.isUserLoggedIn() || authManager.isGuestUser()) return@withContext
        val user = authManager.getCurrentUser() ?: return@withContext

        try {
            val lastSync = getLastSyncTimestamp()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Pulling tasks from cloud updated after $lastSync.")
            }
            val collectionRef = firestore.collection("users").document(user.uid).collection("tasks")

            // Incremental pull: only fetch tasks that were updated after our last successful sync, limited to 250 per pass
            val snapshot = collectionRef.whereGreaterThan("updatedAt", lastSync)
                .limit(250)
                .get()
                .await()
            val remoteTasks = snapshot.documents

            if (remoteTasks.isEmpty()) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "No remote changes found.")
                }
                return@withContext
            }

            var maxUpdatedAt = lastSync

            // Pre-fetch all corresponding local tasks in batch instead of N individual queries
            val validRemoteDocs = remoteTasks.mapNotNull { doc ->
                val cloudId = doc.getString("cloudId")
                if (cloudId != null) cloudId to doc else null
            }
            val cloudIds = validRemoteDocs.map { it.first }
            val localTasksMap = if (cloudIds.isNotEmpty()) {
                cloudIds.chunked(500)
                    .flatMap { taskDao.getTasksByCloudIds(it) }
                    .associateBy { it.cloudId }
            } else {
                emptyMap()
            }

            // Partition remote tasks into inserts vs. updates in a single pass, then apply
            // them in a single Room transaction per batch. This avoids N individual
            // transactions and prevents partial-state on crash.
            val toInsert = mutableListOf<Task>()
            val toUpdate = mutableListOf<Task>()

            for ((cloudId, doc) in validRemoteDocs) {
                val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L
                if (remoteUpdatedAt > maxUpdatedAt) maxUpdatedAt = remoteUpdatedAt

                val isDeleted = doc.getBoolean("isDeleted") ?: false
                val localTask = localTasksMap[cloudId]

                // Last-Write-Wins logic
                if (localTask == null) {
                    if (!isDeleted) {
                        val newTask = Task(
                            cloudId = cloudId,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            dueDate = doc.getLong("dueDate"),
                            dueTime = doc.getString("dueTime"),
                            isCompleted = doc.getBoolean("isCompleted") ?: false,
                            category = doc.getString("category") ?: "General",
                            priority = doc.getString("priority") ?: "Media",
                            reminderTime = doc.getLong("reminderTime"),
                            isRepeat = doc.getBoolean("isRepeat") ?: false,
                            repeatType = doc.getString("repeatType") ?: "Ninguno",
                            displayOrder = doc.getLong("displayOrder")?.toInt() ?: 0,
                            subtasks = parseSubtasks(doc.getString("subtasksJson") ?: "[]"),
                            attachedImageUri = doc.getString("attachedImageUri"),
                            isDeleted = false,
                            deletedTimestamp = 0L,
                            updatedAt = remoteUpdatedAt,
                            isSynced = true
                        )
                        toInsert.add(newTask)
                    }
                } else {
                    if (remoteUpdatedAt > localTask.updatedAt) {
                        val updatedTask = localTask.copy(
                            title = doc.getString("title") ?: localTask.title,
                            description = doc.getString("description") ?: localTask.description,
                            dueDate = doc.getLong("dueDate"),
                            dueTime = doc.getString("dueTime"),
                            isCompleted = doc.getBoolean("isCompleted") ?: localTask.isCompleted,
                            category = doc.getString("category") ?: localTask.category,
                            priority = doc.getString("priority") ?: localTask.priority,
                            reminderTime = doc.getLong("reminderTime"),
                            isRepeat = doc.getBoolean("isRepeat") ?: localTask.isRepeat,
                            repeatType = doc.getString("repeatType") ?: localTask.repeatType,
                            displayOrder = doc.getLong("displayOrder")?.toInt() ?: localTask.displayOrder,
                            subtasks = parseSubtasks(doc.getString("subtasksJson") ?: localTask.subtasksToJson()),
                            attachedImageUri = doc.getString("attachedImageUri"),
                            isDeleted = isDeleted,
                            deletedTimestamp = doc.getLong("deletedTimestamp") ?: localTask.deletedTimestamp,
                            updatedAt = remoteUpdatedAt,
                            isSynced = true
                        )
                        toUpdate.add(updatedTask)
                    }
                }
            }

            // Apply all changes atomically per batch of 500 (SQLite transaction).
            toInsert.chunked(500).forEach { taskDao.insertTasks(it) }
            toUpdate.chunked(500).forEach { taskDao.updateTasks(it) }

            saveLastSyncTimestamp(maxUpdatedAt)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Pull completed. Last sync updated to $maxUpdatedAt.")
            }

            // Notify widget of changes
            try {
                com.fabian.todolist.widget.TaskWidget().updateAll(appContext)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Error updating widgets from sync manager", e)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error pulling changes", e)
            }
        }
    }

    private fun parseSubtasks(json: String): List<Subtask> {
        return try {
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<Subtask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Subtask(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
