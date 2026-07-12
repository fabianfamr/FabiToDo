package com.fabian.todolist.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    /**
     * Formats a due date in milliseconds into a user-friendly relative string or a localized absolute date.
     * Handles "Today" and "Tomorrow" labels automatically if provided.
     */
    fun formatDueDateLocal(millis: Long, todayLabel: String, tomorrowLabel: String): String {
        val cal = Calendar.getInstance()
        val today = cal.apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0) 
        }.timeInMillis
        
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = cal.timeInMillis
        
        val formatToday = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayStr = formatToday.format(Date(today))
        val tomorrowStr = formatToday.format(Date(tomorrow))
        val targetStr = formatToday.format(Date(millis))
        
        return when {
            targetStr == todayStr -> todayLabel
            targetStr == tomorrowStr -> tomorrowLabel
            else -> {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val targetYear = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)
                val pattern = if (currentYear == targetYear) "d MMMM" else "d MMM, yyyy"
                SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
            }
        }
    }

    /**
     * Formats a timestamp into a standard "d MMM, yyyy" format (e.g., "3 Jul, 2026").
     */
    fun formatDateStandard(millis: Long): String {
        val formatText = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
        return formatText.format(Date(millis))
    }

    /**
     * Formats a date timestamp into "dd/MM/yyyy" format.
     */
    fun formatDateSimpleSlash(millis: Long): String {
        val formatText = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatText.format(Date(millis))
    }

    /**
     * Formats a date timestamp into "dd/MM/yy" format.
     */
    fun formatDateSimpleSlashShort(millis: Long): String {
        val formatText = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        return formatText.format(Date(millis))
    }

    /**
     * Converts hour and minute into a clean 24-hour string like "08:30" or "15:45".
     */
    fun formatTime24(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    /**
     * Parses a "HH:mm" 24-hour time string into a 12-hour AM/PM presentation string for improved UX.
     */
    fun formatTimeTo12Hour(timeString: String): String {
        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm)
            } else {
                timeString
            }
        } catch (_: Exception) {
            timeString
        }
    }
}
