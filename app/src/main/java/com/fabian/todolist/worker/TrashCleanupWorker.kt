package com.fabian.todolist.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabian.todolist.di.AppSingletonEntryPoint
import com.fabian.todolist.util.AlarmSchedulerHelper
import dagger.hilt.android.EntryPointAccessors

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

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AppSingletonEntryPoint::class.java
        )
        val repository = entryPoint.taskRepository()

        val limitTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000)

        try {
            val trashedTasks = repository.getAllTrashedTasksSync()
            trashedTasks.forEach { task ->
                if (task.deletedTimestamp == 0L || task.deletedTimestamp < limitTime) {
                    AlarmSchedulerHelper.cancelTaskAlarms(applicationContext, task)
                    repository.hardDeleteSync(task)
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
