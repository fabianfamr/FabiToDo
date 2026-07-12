package com.fabian.todolist.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.fabian.todolist.MainActivity
import com.fabian.todolist.data.AppDatabase
import com.fabian.todolist.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("global_notifications_enabled", true)

        // Defense in depth: reject any internal action coming from a different package.
        // System actions (BOOT_COMPLETED, SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        // are always delivered by the OS with a null/empty sender package.
        val action = intent.action
        val isInternalAction = action == ACTION_TASK_REMINDER ||
            action == ACTION_COMPLETE_TASK ||
            action == ACTION_SNOOZE_TASK
        if (isInternalAction) {
            val sender = intent.`package`
            if (sender != null && sender != context.packageName) {
                Log.w("ReminderReceiver", "Rejected internal action from foreign package: $sender")
                return
            }
        }

        // Handle device reboots or exact alarm permission state changes to avoid losing user alarms/reminders
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED") {
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val tasks = db.taskDao().getTasksWithActiveReminders()
                    for (task in tasks) {
                        com.fabian.todolist.util.AlarmSchedulerHelper.scheduleTaskAlarms(context, task)
                    }
                } catch (e: Exception) {
                    Log.e("ReminderReceiver", "Error rescheduling reminders on background event", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)

                if (task != null) {
                    if (intent.action == ACTION_COMPLETE_TASK) {
                        db.taskDao().updateTask(task.copy(isCompleted = true))
                        cancelReminder(context, task)
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(taskId)
                    } else if (intent.action == ACTION_SNOOZE_TASK) {
                        val snoozeMinutes = prefs.getInt("snooze_duration_minutes", 10)
                        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000)
                        
                        db.taskDao().updateTask(task.copy(dueDate = snoozeTime, reminderTime = snoozeTime))
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(taskId)
                        
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                        val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
                            action = ACTION_TASK_REMINDER
                            putExtra(EXTRA_TASK_ID, task.id)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            task.id,
                            reminderIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    alarmManager.setExactAndAllowWhileIdle(
                                        android.app.AlarmManager.RTC_WAKEUP,
                                        snoozeTime,
                                        pendingIntent
                                    )
                                } else {
                                    alarmManager.setAndAllowWhileIdle(
                                        android.app.AlarmManager.RTC_WAKEUP,
                                        snoozeTime,
                                        pendingIntent
                                    )
                                }
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    android.app.AlarmManager.RTC_WAKEUP,
                                    snoozeTime,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setExact(
                                    android.app.AlarmManager.RTC_WAKEUP,
                                    snoozeTime,
                                    pendingIntent
                                )
                            }
                        } catch (e: SecurityException) {
                            Log.e("ReminderReceiver", "Cannot schedule exact alarm for snooze", e)
                        }
                    } else {
                        if (!notificationsEnabled) {
                            Log.d("ReminderReceiver", "Notifications globally disabled")
                            return@launch
                        }
                        if (!task.isCompleted) {
                            val vibrateEnabled = prefs.getBoolean("notifications_vibrate", true)
                            val soundEnabled = prefs.getBoolean("notifications_sound", true)
                            showNotification(context, task.id, task.title, task.description, task.priority, vibrateEnabled, soundEnabled)

                            // Reschedule the next occurrence for repeating reminders.
                            if (task.isRepeat && task.repeatType != "Ninguno") {
                                val nextTrigger = computeNextRepeatTrigger(task)
                                if (nextTrigger != null) {
                                    val updatedTask = task.copy(
                                        dueDate = nextTrigger,
                                        reminderTime = nextTrigger,
                                        updatedAt = System.currentTimeMillis(),
                                        isSynced = false
                                    )
                                    db.taskDao().updateTask(updatedTask)
                                    com.fabian.todolist.util.AlarmSchedulerHelper.scheduleTaskAlarms(context, updatedTask)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error processing reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        taskId: Int,
        title: String,
        description: String,
        priority: String,
        vibrateEnabled: Boolean,
        soundEnabled: Boolean
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Use a single static channel ID. Previously the channel ID included the
        // sound/vibrate flags, creating a new channel every time the user toggled
        // those settings in NotificationsSettingsDialog — channel proliferation
        // in the system settings UI and never-deleted stale channels.
        val channelId = "fabitodo_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    this.description = context.getString(R.string.notification_channel_desc)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Action when notification clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTIFICATION_TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set Priority Label in notification
        val priorityTag = when (priority) {
            "Alta" -> context.getString(R.string.priority_high_label) + ": "
            "Media" -> context.getString(R.string.priority_medium_label) + ": "
            "Baja" -> context.getString(R.string.priority_low_label) + ": "
            else -> ""
        }

        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_checkmark)
            .setContentTitle("$priorityTag$title")
            .setContentText(description.ifEmpty { context.getString(R.string.notification_default_desc) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_edit, context.getString(R.string.snooze), snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_save, context.getString(R.string.finish), completePendingIntent)

        if (vibrateEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 250, 500))
        } else {
            notificationBuilder.setVibrate(null)
        }

        notificationManager.notify(taskId, notificationBuilder.build())
    }

    private fun cancelReminder(context: Context, task: com.fabian.todolist.data.Task) {
        com.fabian.todolist.util.AlarmSchedulerHelper.cancelTaskAlarms(context, task)
    }

    /**
     * Compute the next trigger timestamp for a repeating reminder.
     * Returns null if [task.repeatType] is unknown or if the result would be in the past.
     *
     * Baseline is the current due date (or reminderTime, falling back to "now").
     * The next occurrence is calculated by adding the repeat interval once; if that
     * is still in the past (e.g., the device was asleep for a long time), we keep
     * advancing until we land on a future timestamp.
     */
    private fun computeNextRepeatTrigger(task: com.fabian.todolist.data.Task): Long? {
        val baseline = task.dueDate ?: task.reminderTime ?: System.currentTimeMillis()
        val now = System.currentTimeMillis()
        var next = baseline
        // Cap iterations to avoid infinite loops on pathological timestamps.
        var safety = 0
        while (next <= now && safety < 365) {
            next = advanceRepeat(next, task.repeatType) ?: return null
            safety++
        }
        return if (next > now) next else null
    }

    private fun advanceRepeat(timestamp: Long, repeatType: String): Long? {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return when (repeatType) {
            "Diario" -> { cal.add(Calendar.DAY_OF_YEAR, 1); cal.timeInMillis }
            "Semanal" -> { cal.add(Calendar.WEEK_OF_YEAR, 1); cal.timeInMillis }
            "Mensual" -> { cal.add(Calendar.MONTH, 1); cal.timeInMillis }
            else -> null
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val ACTION_TASK_REMINDER = "com.fabian.todolist.ACTION_TASK_REMINDER"
        const val ACTION_COMPLETE_TASK = "com.fabian.todolist.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.fabian.todolist.ACTION_SNOOZE_TASK"
    }
}
