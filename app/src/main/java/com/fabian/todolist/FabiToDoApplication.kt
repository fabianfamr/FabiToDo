package com.fabian.todolist

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fabian.todolist.worker.TrashCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class FabiToDoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleTrashCleanup()
    }

    private fun scheduleTrashCleanup() {
        try {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<TrashCleanupWorker>(
                24, TimeUnit.HOURS
            )
            .setConstraints(constraints)
            .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "TrashCleanupPeriodicWork",
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
        } catch (e: Exception) {
            android.util.Log.e("Application", "Failed to schedule periodic trash cleanup", e)
        }
    }
}
