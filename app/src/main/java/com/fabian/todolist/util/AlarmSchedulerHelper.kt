package com.fabian.todolist.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.fabian.todolist.data.Task
import com.fabian.todolist.receiver.ReminderReceiver

object AlarmSchedulerHelper {

    val offsets = listOf(
        0L,                        // Exact time
        12 * 60 * 60 * 1000L,      // 12 hours before
        24 * 60 * 60 * 1000L,      // 1 day before
        48 * 60 * 60 * 1000L,      // 2 days before
        72 * 60 * 60 * 1000L       // 3 days before
    )

    fun scheduleTaskAlarms(context: Context, task: Task) {
        if (task.reminderTime == null || task.dueDate == null || task.dueTime == null || task.isCompleted || task.isDeleted) {
            return
        }

        val now = System.currentTimeMillis()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val exactDueTime = try {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = task.dueDate }
            val parts = task.dueTime.split(":")
            if (parts.size == 2) {
                cal.set(java.util.Calendar.HOUR_OF_DAY, parts[0].toInt())
                cal.set(java.util.Calendar.MINUTE, parts[1].toInt())
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            } else null
        } catch (_: Exception) { null }

        if (exactDueTime == null) return

        offsets.forEachIndexed { index, offset ->
            val triggerTime = exactDueTime - offset
            if (triggerTime > now) {
                val intent = Intent(context, ReminderReceiver::class.java).apply {
                    action = ReminderReceiver.ACTION_TASK_REMINDER
                    putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
                }

                val requestCode = task.id * 10 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (offset == 0L) {
                        // Exact time reminder gets high priority exact wakeups
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val hasExactPermission = alarmManager.canScheduleExactAlarms()
                            if (hasExactPermission) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                            } else {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                            }
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        } else {
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        }
                    } else {
                        // Anticipated alerts use standard power-conserving alarms to save battery
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } catch (e: Exception) {
                    Log.e("AlarmSchedulerHelper", "Failed to schedule alarm for task ${task.id} index $index", e)
                }
            }
        }
    }

    fun cancelTaskAlarms(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel all offset indicators
        for (index in 0 until offsets.size) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_TASK_REMINDER
            }

            val requestCode = task.id * 10 + index
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        // Cancel manual snooze triggers
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TASK_REMINDER
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            snoozeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent)
            snoozePendingIntent.cancel()
        }
    }
}
