package com.fabian.todolist.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val formatterCache = ThreadLocal.withInitial {
        mutableMapOf<Pair<String, String>, SimpleDateFormat>()
    }

    private fun formatWithPattern(millis: Long, pattern: String, locale: Locale = Locale.getDefault()): String {
        val map = formatterCache.get() ?: mutableMapOf()
        val key = pattern to locale.toLanguageTag()
        val sdf = map.getOrPut(key) { SimpleDateFormat(pattern, locale) }
        return sdf.format(Date(millis))
    }

    fun formatDueDateLocal(millis: Long, todayLabel: String, tomorrowLabel: String): String {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val today = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = cal.timeInMillis

        val todayStr = formatWithPattern(today, "yyyyMMdd")
        val tomorrowStr = formatWithPattern(tomorrow, "yyyyMMdd")
        val targetStr = formatWithPattern(millis, "yyyyMMdd")

        return when (targetStr) {
            todayStr -> todayLabel
            tomorrowStr -> tomorrowLabel
            else -> {
                cal.timeInMillis = millis
                val targetYear = cal.get(Calendar.YEAR)
                val pattern = if (currentYear == targetYear) "d MMMM" else "d MMM, yyyy"
                formatWithPattern(millis, pattern)
            }
        }
    }

    fun formatDateStandard(millis: Long): String = formatWithPattern(millis, "d MMM, yyyy")

    fun formatDateSimpleSlash(millis: Long): String = formatWithPattern(millis, "dd/MM/yyyy")

    fun formatDateSimpleSlashShort(millis: Long): String = formatWithPattern(millis, "dd/MM/yy")

    fun formatTime24(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

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

    fun formatDateWithPreference(millis: Long, dateFormatPref: String): String {
        val pattern = when (dateFormatPref) {
            "MM/DD/AA" -> "MM/dd/yy"
            "AA/MM/DD" -> "yy/MM/dd"
            else -> "dd/MM/yy"
        }
        return try {
            formatWithPattern(millis, pattern)
        } catch (_: Exception) {
            formatDateSimpleSlashShort(millis)
        }
    }

    fun formatTimeWithPreference(timeString: String?, timeFormatPref: String): String {
        if (timeString.isNullOrBlank()) return ""
        return if (timeFormatPref == "12h") {
            formatTimeTo12Hour(timeString)
        } else {
            timeString
        }
    }
}
