package com.fabian.todolist.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fabian.todolist.util.QuickAddNotificationHelper

class QuickAddReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
        val quietHoursEnabled = prefs.getBoolean("quick_add_quiet_hours_enabled", true)
        
        if (quietHoursEnabled) {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (hour >= 22 || hour < 8) {
                android.util.Log.d("QuickAddReminderWorker", "Skipping notification: Quiet Hours are active ($hour:00).")
                return Result.success()
            }
        }

        try {
            // Fetch dynamic user-contextualized motivational notification text using Gemini
            val dynamicMessage = QuickAddNotificationHelper.fetchDynamicMotivation(applicationContext)

            // Trigger the non-permanent suggestion notification with optional dynamic text
            QuickAddNotificationHelper.showQuickAddNotification(applicationContext, dynamicMessage)
            return Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Do NOT swallow coroutine cancellation — WorkManager relies on it to
            // honor cancellations when the job is replaced or constraints no longer hold.
            throw e
        } catch (e: Exception) {
            // Ensure notification still triggers with offline fallback
            QuickAddNotificationHelper.showQuickAddNotification(applicationContext, null)
            return Result.success()
        }
    }
}
