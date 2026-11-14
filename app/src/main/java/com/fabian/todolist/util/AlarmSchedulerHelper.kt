package com.fabian.todolist.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.fabian.todolist.data.RepeatType
import com.fabian.todolist.data.Task
import com.fabian.todolist.receiver.ReminderReceiver
import java.util.Calendar

object AlarmSchedulerHelper {

    private const val SNOOZE_CODE_MASK = 15

    val offsets = listOf(
        0L,                        // Exact time
        12 * 60 * 60 * 1000L,      // 12 hours before
        24 * 60 * 60 * 1000L,      // 1 day before
        48 * 60 * 60 * 1000L,      // 2 days before
        72 * 60 * 60 * 1000L       // 3 days before
    )

    fun getOffsetRequestCode(taskId: Int, index: Int): Int = (taskId shl 4) or (index and 0x0F)

    fun getSnoozeRequestCode(taskId: Int): Int = (taskId shl 4) or SNOOZE_CODE_MASK

    fun scheduleTaskAlarms(context: Context, task: Task) {
        if (task.reminderTime == null || task.dueDate == null || task.dueTime == null || task.isCompleted || task.isDeleted) {
            return
        }

        val now = System.currentTimeMillis()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val exactDueTime = try {
            val cal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
            val parts = task.dueTime.split(":")
            if (parts.size == 2) {
                cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                cal.set(Calendar.MINUTE, parts[1].toInt())
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
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

                val requestCode = getOffsetRequestCode(task.id, index)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (offset == 0L) {
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

        for (index in 0 until offsets.size) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_TASK_REMINDER
            }

            val requestCode = getOffsetRequestCode(task.id, index)
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

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_TASK_REMINDER
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            getSnoozeRequestCode(task.id),
            snoozeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePendingIntent != null) {
            alarmManager.cancel(snoozePendingIntent)
            snoozePendingIntent.cancel()
        }
    }

    fun computeNextRepeatSchedule(task: Task): Pair<Long, Long?>? {
        if (!task.isRepeat || task.repeatType == RepeatType.NONE) return null
        val now = System.currentTimeMillis()
        val baseDue = task.dueDate ?: task.reminderTime ?: now
        val reminderOffsetMs = if (task.dueDate != null && task.reminderTime != null) {
            (task.dueDate - task.reminderTime).coerceAtLeast(0L)
        } else {
            0L
        }

        val dueCal = Calendar.getInstance().apply { timeInMillis = baseDue }
        val timeParts = task.dueTime?.split(":")
        val hour = timeParts?.getOrNull(0)?.toIntOrNull()
        val minute = timeParts?.getOrNull(1)?.toIntOrNull()

        var safety = 0
        var effectiveTimeMillis: Long
        do {
            when (task.repeatType) {
                RepeatType.DAILY -> dueCal.add(Calendar.DAY_OF_YEAR, 1)
                RepeatType.WEEKLY -> dueCal.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatType.MONTHLY -> dueCal.add(Calendar.MONTH, 1)
                else -> return null
            }
            effectiveTimeMillis = if (hour != null && minute != null) {
                Calendar.getInstance().apply {
                    timeInMillis = dueCal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else {
                dueCal.timeInMillis
            }
            safety++
        } while (effectiveTimeMillis <= now && safety < 3650)

        if (effectiveTimeMillis <= now) return null
        val nextDue = dueCal.timeInMillis
        val nextReminder = if (task.reminderTime != null) {
            (effectiveTimeMillis - reminderOffsetMs).coerceAtLeast(now + 1000L)
        } else {
            null
        }
        return nextDue to nextReminder
    }
}
