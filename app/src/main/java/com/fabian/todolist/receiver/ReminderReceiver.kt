package com.fabian.todolist.receiver

import android.app.AlarmManager
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
import com.fabian.todolist.R
import com.fabian.todolist.data.RepeatType
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.TaskPriority
import com.fabian.todolist.di.AppSingletonEntryPoint
import com.fabian.todolist.util.AlarmSchedulerHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("global_notifications_enabled", true)

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

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        ) {
            if (!notificationsEnabled) return
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        AppSingletonEntryPoint::class.java
                    ).taskRepository()
                    val tasks = repository.getTasksWithActiveReminders()
                    for (task in tasks) {
                        AlarmSchedulerHelper.scheduleTaskAlarms(context, task)
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
                val repository = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    AppSingletonEntryPoint::class.java
                ).taskRepository()
                val task = repository.getTaskById(taskId)

                if (task != null) {
                    if (action == ACTION_COMPLETE_TASK) {
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(taskId)
                        cancelReminder(context, task)

                        if (task.isRepeat && task.repeatType != RepeatType.NONE) {
                            val nextSchedule = AlarmSchedulerHelper.computeNextRepeatSchedule(task)
                            if (nextSchedule != null) {
                                val nextTask = task.copy(
                                    dueDate = nextSchedule.first,
                                    reminderTime = nextSchedule.second,
                                    isCompleted = false
                                )
                                repository.update(nextTask)
                                if (notificationsEnabled) {
                                    AlarmSchedulerHelper.scheduleTaskAlarms(context, nextTask)
                                }
                            } else {
                                repository.update(task.copy(isCompleted = true))
                            }
                        } else {
                            repository.update(task.copy(isCompleted = true))
                        }
                    } else if (action == ACTION_SNOOZE_TASK) {
                        val snoozeMinutes = prefs.getInt("snooze_duration_minutes", 10)
                        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

                        repository.update(task.copy(reminderTime = snoozeTime))
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(taskId)

                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val reminderIntent = Intent(context, ReminderReceiver::class.java).apply {
                            setAction(ACTION_TASK_REMINDER)
                            putExtra(EXTRA_TASK_ID, task.id)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            AlarmSchedulerHelper.getSnoozeRequestCode(task.id),
                            reminderIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        snoozeTime,
                                        pendingIntent
                                    )
                                } else {
                                    alarmManager.setAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        snoozeTime,
                                        pendingIntent
                                    )
                                }
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    snoozeTime,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setExact(
                                    AlarmManager.RTC_WAKEUP,
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
                        if (!task.isCompleted && !task.isDeleted) {
                            val vibrateEnabled = prefs.getBoolean("notifications_vibrate", true)
                            val soundEnabled = prefs.getBoolean("notifications_sound", true)
                            showNotification(context, task.id, task.title, task.description, task.priority, vibrateEnabled, soundEnabled)
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
        val channelId = ensureReminderChannel(context, soundEnabled)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("NOTIFICATION_TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityTag = when (priority) {
            TaskPriority.CRITICAL -> context.getString(R.string.priority_critical_label) + ": "
            TaskPriority.HIGH -> context.getString(R.string.priority_high_label) + ": "
            TaskPriority.MEDIUM -> context.getString(R.string.priority_medium_label) + ": "
            TaskPriority.LOW -> context.getString(R.string.priority_low_label) + ": "
            else -> ""
        }

        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId shl 4) or 13,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId shl 4) or 14,
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

        if (!soundEnabled) {
            notificationBuilder.setSilent(true)
        }
        if (vibrateEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 500, 250, 500))
        } else {
            notificationBuilder.setVibrate(null)
        }

        notificationManager.notify(taskId, notificationBuilder.build())
    }

    private fun cancelReminder(context: Context, task: Task) {
        AlarmSchedulerHelper.cancelTaskAlarms(context, task)
    }

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val ACTION_TASK_REMINDER = "com.fabian.todolist.ACTION_TASK_REMINDER"
        const val ACTION_COMPLETE_TASK = "com.fabian.todolist.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.fabian.todolist.ACTION_SNOOZE_TASK"
        private const val CHANNEL_SOUND = "fabitodo_reminders_sound"
        private const val CHANNEL_SILENT = "fabitodo_reminders_silent"

        fun ensureReminderChannel(context: Context, soundEnabled: Boolean): String {
            val channelId = if (soundEnabled) CHANNEL_SOUND else CHANNEL_SILENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.getNotificationChannel(channelId) == null) {
                    val channel = NotificationChannel(
                        channelId,
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.notification_channel_desc)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 500, 250, 500)
                        if (!soundEnabled) {
                            setSound(null, null)
                        }
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
            return channelId
        }
    }
}
