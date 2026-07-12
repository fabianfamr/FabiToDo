package com.fabian.todolist.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabian.todolist.data.AppDatabase
import com.fabian.todolist.data.TaskRepository
import java.util.Calendar

class TrashCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
        val inputRetention = inputData.getInt(KEY_RETENTION_DAYS, -1)
        val retentionDays = if (inputRetention != -1) {
            inputRetention
        } else {
            prefs.getInt("trash_retention_days", 30)
        }
        if (retentionDays <= 0) return Result.success()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao(), applicationContext)

        val limitTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000)
        
        try {
            // Query ONLY trashed tasks. The previous implementation called getAllTasksSync()
            // which filters `isDeleted = 0`, so the cleanup loop never matched anything
            // and the trash bin grew forever.
            val trashedTasks = repository.getAllTrashedTasksSync()
            trashedTasks.forEach { task ->
                if (task.deletedTimestamp == 0L) {
                    // Compatibility: if deleted without timestamp, delete immediately
                    repository.hardDelete(task)
                } else if (task.deletedTimestamp < limitTime) {
                    // Past retention period
                    repository.hardDelete(task)
                    com.fabian.todolist.util.AlarmSchedulerHelper.cancelTaskAlarms(applicationContext, task)
                }
            }
            return Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        const val KEY_RETENTION_DAYS = "RETENTION_DAYS"
    }
}
