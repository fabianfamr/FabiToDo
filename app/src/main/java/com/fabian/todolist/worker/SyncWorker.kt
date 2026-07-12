package com.fabian.todolist.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabian.todolist.data.AppDatabase
import com.fabian.todolist.data.AuthManager
import com.fabian.todolist.data.CloudSyncManager
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
        val isAutoSyncEnabled = prefs.getBoolean("auto_sync_enabled", true)

        if (!isAutoSyncEnabled) {
            return Result.success()
        }

        try {
            Log.d("SyncWorker", "Starting real sync")

            // Initializing dependencies manually since HiltWorker isn't configured
            val taskDao = AppDatabase.getDatabase(applicationContext).taskDao()
            val authManager = AuthManager(applicationContext)
            val firestore = FirebaseFirestore.getInstance()
            val syncManager = CloudSyncManager(firestore, taskDao, authManager)

            // Real sync process
            syncManager.pullRemoteChanges()
            val pushSucceeded = syncManager.pushLocalChanges()

            // Only reset the unsynced counter if push actually succeeded.
            // Previously the counter was reset to 0 even when push silently swallowed
            // a Firestore/network error, lying to the user about sync state.
            if (pushSucceeded) {
                prefs.edit().putInt("unsynced_tasks_count", 0).apply()
                return Result.success()
            } else {
                Log.w("SyncWorker", "Push failed; keeping unsynced counter for retry.")
                return Result.retry()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            return Result.retry()
        }
    }
}
