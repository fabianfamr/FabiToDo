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

    // Spanish and English UX suggestions to prompt task logging
    private val suggestionsEs = listOf(
        "¿Qué tienes en mente para hoy? Agrega una tarea nueva.",
        "Un paso a la vez. ¡Anota tu pendiente de una vez!",
        "¿Alguna gran idea hoy? Captúrala rápido aquí.",
        "Mente organizada, día productivo. Añade una tarea.",
        "¿Cómo vas con tus metas? Añade un nuevo objetivo hoy."
    )

    private val suggestionsEn = listOf(
        "What's on your mind today? Register a new task.",
        "One step at a time! Record your pending tasks.",
        "Had a brilliant idea? Quick-write it here.",
        "Clear mind, productive day. Add a new task.",
        "How are your goals going? Add a daily task."
    )

    /**
     * Fetches a friendly, dynamic, highly motivating prompt for recording tasks from Gemini API.
     * Returns null if key is not configured or in case of other errors, allowing graceful offline fallback.
     */
    suspend fun fetchDynamicMotivation(context: Context): String? {
        val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
        val userKey = prefs.getString("ai_api_key", "") ?: ""
        val key = if (userKey.isNotBlank()) userKey else com.fabian.todolist.BuildConfig.GEMINI_API_KEY
        
        if (key.isBlank() || key == "PLACEHOLDER_NOT_CONFIGURED" || key == "YOUR_GEMINI_API_KEY") {
            Log.d("QuickAddNotification", "No API Key configured, defaulting to offline list.")
            return null
        }

        val model = prefs.getString("ai_model", "gemini-3.1-flash-lite-preview") ?: "gemini-3.1-flash-lite-preview"
        
        val currentLocaleCode = context.resources.configuration.locales.get(0).language
        val languageName = if (currentLocaleCode.startsWith("es")) "Spanish" else "English"

        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        
        val dayOfWeekName = when (dayOfWeek) {
            java.util.Calendar.SUNDAY -> "Domingo"
            java.util.Calendar.MONDAY -> "Lunes"
            java.util.Calendar.TUESDAY -> "Martes"
            java.util.Calendar.WEDNESDAY -> "Miércoles"
            java.util.Calendar.THURSDAY -> "Jueves"
            java.util.Calendar.FRIDAY -> "Viernes"
            java.util.Calendar.SATURDAY -> "Sábado"
            else -> "Today"
        }
        
        val timeOfDaySegment = when {
            hour in 6..11 -> "Mañana"
            hour in 12..17 -> "Tarde"
            hour in 18..22 -> "Noche"
            else -> "Madrugada"
        }

        // Construct context-aware motivational dynamically tailored prompt
        val prompt = if (languageName == "Spanish") {
            """
            Estamos a día $dayOfWeekName y el reloj marca las $hour:00 h ($timeOfDaySegment).
            Escribe una sola frase fresquísima, empática, alegre, muy creativa y corta (máximo 85 caracteres) que me inspire a abrir la app FabiToDo y registrar mis tareas.
            Ajusta el mood o tono a la hora y día (por ejemplo, energía al arrancar la mañana, enfoque al mediodía, satisfacción o calma de noche, o buena vibra de fin de semana).
            No incluyas hashtags, comillas ni formato markdown.
            """.trimIndent()
        } else {
            """
            Today is $dayOfWeekName and the current local hour is $hour:00 ($timeOfDaySegment).
            Write a single fresh, friendly, encouraging, and brief reminder sentence (maximum 85 characters) to motivate me to open FabiToDo and write my pending tasks of today.
            Tailor the energy to the time of day and the week (e.g., active morning starter, focused afternoon boost, cozy evening wrap-up, or relaxed weekend vibe).
            No hashtags, no quotes, and no markdown format.
            """.trimIndent()
        }

        return try {
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                ),
                systemInstruction = Content(
                    parts = listOf(Part(text = "You are a context-aware, highly empathetic and charismatic task companion. Always write exactly one brief line, strictly under 85 characters, with absolutely no formatting, quotes, or markdown."))
                )
            )
            val response = RetrofitClient.service.generateContent(model, key, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!text.isNullOrBlank()) {
                text.replace("\"", "").replace("'", "").trim()
            } else {
                null
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

        // Action when notification is clicked or button is pressed (ACTION_ADD_TASK triggers AddTaskDialog)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.fabian.todolist.ACTION_ADD_TASK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
        val currentLocaleCode = context.resources.configuration.locales.get(0).language
        val randomSuggestion = customMessage ?: if (currentLocaleCode.startsWith("es")) {
            suggestionsEs[Random.nextInt(suggestionsEs.size)]
        } else {
            suggestionsEn[Random.nextInt(suggestionsEn.size)]
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
    fun schedulePeriodicReminders(context: Context, intervalCode: String) {
        val workManager = WorkManager.getInstance(context)
        
        if (intervalCode == "off") {
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
            else -> 360L // Default fallback to 6 hours
        }

        val periodicWorkRequest = PeriodicWorkRequestBuilder<QuickAddReminderWorker>(
            repeatIntervalMinutes, TimeUnit.MINUTES,
            15L, TimeUnit.MINUTES // Flexible interval allowance for Android optimization
        )
        .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }
}
