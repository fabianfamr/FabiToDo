package com.fabian.todolist.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fabian.todolist.MainActivity
import com.fabian.todolist.R
import com.fabian.todolist.data.gemini.Content
import com.fabian.todolist.data.gemini.GenerateContentRequest
import com.fabian.todolist.data.gemini.Part
import com.fabian.todolist.data.gemini.RetrofitClient
import com.fabian.todolist.worker.QuickAddReminderWorker
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object QuickAddNotificationHelper {
    private const val CHANNEL_ID = "fabitodo_quick_add_channel"
    private const val NOTIFICATION_ID = 9999
    private const val UNIQUE_WORK_NAME = "fabitodo_quick_add_periodic_work"

    // Dynamic suggestions are now pulled from strings.xml (quick_add_suggestions)


    /**
     * Fetches a friendly, dynamic, highly motivating prompt for recording tasks from Gemini API.
     * Returns null if key is not configured or in case of other errors, allowing graceful offline fallback.
     */
    suspend fun fetchDynamicMotivation(context: Context): String? {
        val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
        val aiKeyType = prefs.getString("ai_key_type", "public") ?: "public"
        val userKey = SecurityCryptoHelper.readAndMigrateApiKey(prefs)
        val key = if (userKey.isNotBlank()) userKey else com.fabian.todolist.BuildConfig.GEMINI_API_KEY
        val model = prefs.getString("ai_model", "gemini-3.5-flash-lite") ?: "gemini-3.5-flash-lite"
        val useLowLatency = prefs.getBoolean("ai_low_latency_mode", true)

        val currentLocale = context.resources.configuration.locales.get(0)
        val currentLocaleCode = currentLocale.language
        val isSpanish = currentLocaleCode.startsWith("es")
        val responseLanguageName = currentLocale.getDisplayName(java.util.Locale.ENGLISH).ifBlank { "English" }

        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        val dayOfWeekName = java.text.SimpleDateFormat("EEEE", currentLocale).format(calendar.time)

        val timeOfDaySegment = when {
            hour in 6..11 -> if (isSpanish) "Mañana" else "Morning"
            hour in 12..17 -> if (isSpanish) "Tarde" else "Afternoon"
            hour in 18..22 -> if (isSpanish) "Noche" else "Evening"
            else -> if (isSpanish) "Madrugada" else "Late night"
        }

        val prompt = if (isSpanish) {
            """
            Estamos a día $dayOfWeekName y el reloj marca las $hour:00 h ($timeOfDaySegment).
            Escribe una sola frase empática, alegre y breve (máximo 85 caracteres) que me inspire a abrir la app FabiToDo y registrar mis tareas.
            Ajusta el tono a la hora y día (energía al arrancar la mañana, enfoque al mediodía, calma de noche o serenidad de fin de semana).
            No incluyas hashtags, comillas ni formato markdown.
            """.trimIndent()
        } else {
            """
            Today is $dayOfWeekName and the current local hour is $hour:00 ($timeOfDaySegment).
            Write a single brief, encouraging, and serious reminder sentence (maximum 85 characters) to motivate me to open FabiToDo and record my pending tasks for today.
            Tailor the tone to the time of day and the week (active morning starter, focused afternoon boost, calm evening wrap-up, or relaxed weekend rhythm).
            IMPORTANT: Write your answer in $responseLanguageName.
            No hashtags, no quotes, and no markdown format.
            """.trimIndent()
        }

        val systemInstruction = "You are a context-aware, empathetic task companion. Always write exactly one brief line, strictly under 85 characters, with no formatting, quotes, or markdown."

        return try {
            val hasValidCustomKey = key.isNotBlank() &&
                key != "PLACEHOLDER_NOT_CONFIGURED" &&
                key != "YOUR_GEMINI_API_KEY"

            if (aiKeyType == "custom") {
                if (!hasValidCustomKey) return null
                val restModel = com.fabian.todolist.data.gemini.FirebaseAiManager.mapToRestApiModel(model, useLowLatency)
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                )
                val response = RetrofitClient.service.generateContent(restModel, key, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!text.isNullOrBlank()) text.replace("\"", "").replace("'", "").trim() else null
            } else {
                val text = com.fabian.todolist.data.gemini.FirebaseAiManager.generateContent(
                    modelName = model,
                    prompt = prompt,
                    systemInstructionText = systemInstruction,
                    lowLatency = useLowLatency,
                    timeoutMs = 15_000L,
                    userContext = context
                )
                text.replace("\"", "").replace("'", "").trim()
            }
        } catch (e: Exception) {
            Log.e("QuickAddNotification", "Failed to fetch motivation from Gemini API", e)
            null
        }
    }

    fun showQuickAddNotification(context: Context, customMessage: String? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.quick_add_notification_channel_name)
            val descriptionText = context.getString(R.string.settings_quick_add_notification_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT // Default so it triggers a light alert if configured
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.fabian.todolist.ACTION_ADD_TASK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val buttonTitle = context.getString(R.string.quick_add_notification_button)
        val buttonPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Choose a random dynamic suggestion based on app language context if no custom AI message is provided
        val suggestions = context.resources.getStringArray(R.array.quick_add_suggestions)
        val randomSuggestion = customMessage ?: if (suggestions.isNotEmpty()) {
            suggestions[Random.nextInt(suggestions.size)]
        } else {
            context.getString(R.string.quick_add_fallback_message)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_checkmark)
            .setContentTitle(context.getString(R.string.quick_add_notification_title))
            .setContentText(randomSuggestion)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false) // Permite descartar deslizando (swipe to dismiss)
            .setAutoCancel(true) // Cierra al hacer click en ella
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_input_add,
                buttonTitle,
                buttonPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun cancelQuickAddNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Schedules the non-permanent reminders based on the chosen interval value.
     * Available intervals: "off", "1h", "2h", "6h", "12h", "24h".
     */
    fun schedulePeriodicReminders(
        context: Context,
        intervalCode: String,
        replaceExisting: Boolean = false
    ) {
        val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("global_notifications_enabled", true)
        val workManager = WorkManager.getInstance(context)

        if (intervalCode == "off" || !notificationsEnabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            cancelQuickAddNotification(context)
            return
        }

        val repeatIntervalMinutes = when (intervalCode) {
            "1h" -> 60L
            "2h" -> 120L
            "6h" -> 360L
            "12h" -> 720L
            "24h" -> 1440L
            else -> 360L
        }

        val periodicWorkRequest = PeriodicWorkRequestBuilder<QuickAddReminderWorker>(
            repeatIntervalMinutes, TimeUnit.MINUTES,
            15L, TimeUnit.MINUTES
        ).build()

        val policy = if (replaceExisting) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            policy,
            periodicWorkRequest
        )
    }
}
