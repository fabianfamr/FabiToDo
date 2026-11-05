package com.fabian.todolist.util

import android.content.Context
import com.fabian.todolist.R
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.TaskPriority
import com.fabian.todolist.data.getSubtasks

object TaskShareUtils {

    /**
     * Formats a complete single task with all its properties in a clean, human-readable structure.
     */
    fun formatTaskForSharing(context: Context, task: Task): String {
        val sb = StringBuilder()

        // 📋 Title
        sb.append("📋 ").append(context.getString(R.string.share_field_task)).append(": ").append(task.title.trim()).append("\n")

        // 📝 Description / Notes
        if (task.description.isNotBlank()) {
            sb.append("📝 ").append(context.getString(R.string.share_field_notes)).append(": ").append(task.description.trim()).append("\n")
        }

        // 📁 Category
        if (task.category.isNotBlank()) {
            val localizedCat = getLocalizedCategoryName(context, task.category)
            sb.append("📁 ").append(context.getString(R.string.share_field_category)).append(": ").append(localizedCat).append("\n")
        }

        // ⚡ Priority
        val priorityLabel = when (task.priority) {
            TaskPriority.CRITICAL -> context.getString(R.string.priority_critical_label)
            TaskPriority.HIGH -> context.getString(R.string.priority_high_label)
            TaskPriority.MEDIUM -> context.getString(R.string.priority_medium_label)
            TaskPriority.LOW -> context.getString(R.string.priority_low_label)
            else -> context.getString(R.string.priority_none_label)
        }
        sb.append("⚡ ").append(context.getString(R.string.share_field_priority)).append(": ").append(priorityLabel).append("\n")

        // 📅 Due Date
        if (task.dueDate != null) {
            val formattedDate = DateTimeUtils.formatDateSimpleSlash(task.dueDate)
            sb.append("📅 ").append(context.getString(R.string.share_field_date)).append(": ").append(formattedDate).append("\n")
        }

        // ⏰ Time
        if (!task.dueTime.isNullOrBlank()) {
            sb.append("⏰ ").append(context.getString(R.string.share_field_time)).append(": ").append(task.dueTime).append("\n")
        }

        // 🔔 Reminder
        if (task.reminderTime != null) {
            sb.append("🔔 ").append(context.getString(R.string.share_field_reminder)).append(": ").append(context.getString(R.string.share_reminder_active)).append("\n")
        }

        // 🔁 Repeat
        if (task.isRepeat) {
            val repeatLabel = when (task.repeatType) {
                "Diario" -> context.getString(R.string.repeat_daily)
                "Semanal" -> context.getString(R.string.repeat_weekly)
                "Mensual" -> context.getString(R.string.repeat_monthly)
                else -> task.repeatType
            }
            sb.append("🔁 ").append(context.getString(R.string.share_field_repeat)).append(": ").append(repeatLabel).append("\n")
        }

        // ✅ Status
        val statusLabel = if (task.isCompleted) {
            context.getString(R.string.share_status_completed)
        } else {
            context.getString(R.string.share_status_pending)
        }
        val statusIcon = if (task.isCompleted) "✅ " else "⏳ "
        sb.append(statusIcon).append(context.getString(R.string.share_field_status)).append(": ").append(statusLabel).append("\n")

        // ☑️ Subtasks
        val subtasks = task.getSubtasks()
        if (subtasks.isNotEmpty()) {
            val completedCount = subtasks.count { it.isCompleted }
            sb.append("\n☑️ ").append(context.getString(R.string.share_field_subtasks))
                .append(" (").append(completedCount).append("/").append(subtasks.size).append("):\n")
            subtasks.forEach { sub ->
                val mark = if (sub.isCompleted) "[✔]" else "[ ]"
                sb.append("   ").append(mark).append(" ").append(sub.title.trim()).append("\n")
            }
        }

        return sb.toString().trimEnd()
    }

    /**
     * Formats multiple selected tasks for sharing.
     */
    fun formatMultipleTasksForSharing(context: Context, tasks: List<Task>): String {
        return tasks.joinToString("\n\n═══════════════════════\n\n") { task ->
            formatTaskForSharing(context, task)
        }
    }
}
