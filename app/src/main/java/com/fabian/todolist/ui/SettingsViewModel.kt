package com.fabian.todolist.ui

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import com.fabian.todolist.data.SystemCategory
import com.fabian.todolist.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import org.json.JSONArray
import org.json.JSONObject
import com.fabian.todolist.util.QuickAddNotificationHelper
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)

    private val _themeAccent = MutableStateFlow(prefs.getString("accent_color", "system") ?: "system")
    val themeAccent: StateFlow<String> = _themeAccent.asStateFlow()

    private val _themeDark = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeDark: StateFlow<String> = _themeDark.asStateFlow()

    private val _languageCode = MutableStateFlow(prefs.getString("app_locale", "system") ?: "system")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _dateFormat = MutableStateFlow(prefs.getString("date_format", "DD/MM/AA") ?: "DD/MM/AA")
    val dateFormat: StateFlow<String> = _dateFormat.asStateFlow()

    private val _timeFormat = MutableStateFlow(prefs.getString("time_format", "24h") ?: "24h")
    val timeFormat: StateFlow<String> = _timeFormat.asStateFlow()

    private val _longPressAction = MutableStateFlow(prefs.getString("long_press_action", "select") ?: "select")
    val longPressAction: StateFlow<String> = _longPressAction.asStateFlow()

    private val _confirmOnDelete = MutableStateFlow(prefs.getBoolean("confirm_on_delete", true))
    val confirmOnDelete: StateFlow<Boolean> = _confirmOnDelete.asStateFlow()

    private val _hapticFeedbackOnComplete = MutableStateFlow(prefs.getBoolean("haptic_feedback_on_complete", true))
    val hapticFeedbackOnComplete: StateFlow<Boolean> = _hapticFeedbackOnComplete.asStateFlow()

    private val _requireBiometrics = MutableStateFlow(prefs.getBoolean("require_biometrics", false))
    val requireBiometrics: StateFlow<Boolean> = _requireBiometrics.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("global_notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _quickAddNotificationEnabled = MutableStateFlow(prefs.getBoolean("quick_add_notification_enabled", false))
    val quickAddNotificationEnabled: StateFlow<Boolean> = _quickAddNotificationEnabled.asStateFlow()

    private val _quickAddNotificationInterval = MutableStateFlow(prefs.getString("quick_add_notification_interval", "off") ?: "off")
    val quickAddNotificationInterval: StateFlow<String> = _quickAddNotificationInterval.asStateFlow()

    private val _quickAddQuietHoursEnabled = MutableStateFlow(prefs.getBoolean("quick_add_quiet_hours_enabled", true))
    val quickAddQuietHoursEnabled: StateFlow<Boolean> = _quickAddQuietHoursEnabled.asStateFlow()

    private val _notificationsSound = MutableStateFlow(prefs.getBoolean("notifications_sound", true))
    val notificationsSound: StateFlow<Boolean> = _notificationsSound.asStateFlow()

    private val _notificationsVibrate = MutableStateFlow(prefs.getBoolean("notifications_vibrate", true))
    val notificationsVibrate: StateFlow<Boolean> = _notificationsVibrate.asStateFlow()

    private val _reminderOffsetMinutes = MutableStateFlow(prefs.getInt("reminder_offset_minutes", 0))
    val reminderOffsetMinutes: StateFlow<Int> = _reminderOffsetMinutes.asStateFlow()

    private val _snoozeDurationMinutes = MutableStateFlow(prefs.getInt("snooze_duration_minutes", 10))
    val snoozeDurationMinutes: StateFlow<Int> = _snoozeDurationMinutes.asStateFlow()

    private val _trashRetentionDays = MutableStateFlow(prefs.getInt("trash_retention_days", 30))
    val trashRetentionDays: StateFlow<Int> = _trashRetentionDays.asStateFlow()

    private val _aiModel = MutableStateFlow(prefs.getString("ai_model", "gemini-3.1-flash-lite-preview") ?: "gemini-3.1-flash-lite-preview")
    val aiModel: StateFlow<String> = _aiModel.asStateFlow()

    private val _aiLowLatencyMode = MutableStateFlow(prefs.getBoolean("ai_low_latency_mode", true))
    val aiLowLatencyMode: StateFlow<Boolean> = _aiLowLatencyMode.asStateFlow()

    private val _aiApiKey = MutableStateFlow(prefs.getString("ai_api_key", "") ?: "")
    val aiApiKey: StateFlow<String> = _aiApiKey.asStateFlow()

    private val _aiSubtaskCount = MutableStateFlow(prefs.getInt("ai_subtask_count", 5))
    val aiSubtaskCount: StateFlow<Int> = _aiSubtaskCount.asStateFlow()

    private val _showAiBanner = MutableStateFlow(prefs.getBoolean("show_ai_banner", true))
    val showAiBanner: StateFlow<Boolean> = _showAiBanner.asStateFlow()

    private val _showBackupCard = MutableStateFlow(prefs.getBoolean("show_backup_card", true))
    val showBackupCard: StateFlow<Boolean> = _showBackupCard.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("user_display_name", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _categories = MutableStateFlow(loadInitialCategories())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _categoryColors = MutableStateFlow(loadCategoryColors())
    val categoryColors: StateFlow<Map<String, Int>> = _categoryColors.asStateFlow()

    private val _categoryIcons = MutableStateFlow(loadCategoryIcons())
    val categoryIcons: StateFlow<Map<String, String>> = _categoryIcons.asStateFlow()

    private fun loadInitialCategories(): List<String> {
        val savedCategoriesString = prefs.getString("custom_categories", null)
        return if (savedCategoriesString != null) {
            val jsonArray = JSONArray(savedCategoriesString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) list.add(jsonArray.getString(i))
            list
        } else listOf(SystemCategory.ALL_TASKS, "Personal", "Trabajo", "Compras", "Hogar", "Salud", SystemCategory.COMPLETED)
    }

    private fun loadCategoryColors(): Map<String, Int> {
        val jsonString = prefs.getString("category_colors", "{}")
        return try {
            val jsonObject = JSONObject(jsonString ?: "{}")
            val map = mutableMapOf<String, Int>()
            val keys = jsonObject.keys()
            while(keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getInt(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun loadCategoryIcons(): Map<String, String> {
        val jsonString = prefs.getString("category_icons", "{}")
        return try {
            val jsonObject = JSONObject(jsonString ?: "{}")
            val map = mutableMapOf<String, String>()
            val keys = jsonObject.keys()
            while(keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun setThemeAccent(accent: String) {
        _themeAccent.value = accent
        prefs.edit().putString("accent_color", accent).apply()
    }

    fun setThemeDark(mode: String) {
        _themeDark.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setLanguageCode(lang: String) {
        _languageCode.value = lang
        prefs.edit().putString("app_locale", lang).apply()
        
        val appLocale = if (lang == "system") {
            androidx.core.os.LocaleListCompat.getEmptyLocaleList()
        } else {
            androidx.core.os.LocaleListCompat.forLanguageTags(lang)
        }
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun setDateFormat(format: String) {
        _dateFormat.value = format
        prefs.edit().putString("date_format", format).apply()
    }

    fun setTimeFormat(format: String) {
        _timeFormat.value = format
        prefs.edit().putString("time_format", format).apply()
    }

    fun setLongPressAction(action: String) {
        _longPressAction.value = action
        prefs.edit().putString("long_press_action", action).apply()
    }

    fun setConfirmOnDelete(confirm: Boolean) {
        _confirmOnDelete.value = confirm
        prefs.edit().putBoolean("confirm_on_delete", confirm).apply()
    }

    fun setHapticFeedbackOnComplete(feedback: Boolean) {
        _hapticFeedbackOnComplete.value = feedback
        prefs.edit().putBoolean("haptic_feedback_on_complete", feedback).apply()
    }

    fun setRequireBiometrics(require: Boolean) {
        _requireBiometrics.value = require
        prefs.edit().putBoolean("require_biometrics", require).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("global_notifications_enabled", enabled).apply()
    }

    fun setQuickAddNotificationEnabled(enabled: Boolean) {
        val interval = if (enabled) "6h" else "off"
        setQuickAddNotificationInterval(interval)
    }

    fun setQuickAddNotificationInterval(intervalCode: String) {
        _quickAddNotificationInterval.value = intervalCode
        prefs.edit().putString("quick_add_notification_interval", intervalCode).apply()
        
        val isEnabled = intervalCode != "off"
        _quickAddNotificationEnabled.value = isEnabled
        prefs.edit().putBoolean("quick_add_notification_enabled", isEnabled).apply()

        val context = getApplication<Application>().applicationContext
        QuickAddNotificationHelper.schedulePeriodicReminders(context, intervalCode)
    }

    fun setQuickAddQuietHoursEnabled(enabled: Boolean) {
        _quickAddQuietHoursEnabled.value = enabled
        prefs.edit().putBoolean("quick_add_quiet_hours_enabled", enabled).apply()
    }

    fun setNotificationsSound(enabled: Boolean) {
        _notificationsSound.value = enabled
        prefs.edit().putBoolean("notifications_sound", enabled).apply()
    }

    fun setNotificationsVibrate(enabled: Boolean) {
        _notificationsVibrate.value = enabled
        prefs.edit().putBoolean("notifications_vibrate", enabled).apply()
    }

    fun setReminderOffsetMinutes(minutes: Int) {
        _reminderOffsetMinutes.value = minutes
        prefs.edit().putInt("reminder_offset_minutes", minutes).apply()
    }

    fun setSnoozeDuration(minutes: Int) {
        _snoozeDurationMinutes.value = minutes
        prefs.edit().putInt("snooze_duration_minutes", minutes).apply()
    }

    fun setTrashRetentionDays(days: Int) {
        _trashRetentionDays.value = days
        prefs.edit().putInt("trash_retention_days", days).apply()
    }

    fun setAiModel(model: String) {
        _aiModel.value = model
        prefs.edit().putString("ai_model", model).apply()
    }

    fun setAiLowLatencyMode(enabled: Boolean) {
        _aiLowLatencyMode.value = enabled
        prefs.edit().putBoolean("ai_low_latency_mode", enabled).apply()
    }

    fun setAiApiKey(key: String) {
        _aiApiKey.value = key
        prefs.edit().putString("ai_api_key", key).apply()
    }

    fun setAiSubtaskCount(count: Int) {
        _aiSubtaskCount.value = count
        prefs.edit().putInt("ai_subtask_count", count).apply()
    }

    fun setShowAiBanner(show: Boolean) {
        _showAiBanner.value = show
        prefs.edit().putBoolean("show_ai_banner", show).apply()
    }

    fun setShowBackupCard(show: Boolean) {
        _showBackupCard.value = show
        prefs.edit().putBoolean("show_backup_card", show).apply()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
    }

    fun setUserName(name: String) {
        _userName.value = name
        prefs.edit().putString("user_display_name", name).apply()
    }

    fun saveCategories(list: List<String>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        prefs.edit().putString("custom_categories", jsonArray.toString()).apply()
        _categories.value = list
    }

    fun addCategory(categoryName: String) {
        if (categoryName.isBlank() || _categories.value.contains(categoryName)) return
        val newList = _categories.value + categoryName
        saveCategories(newList)
    }

    fun deleteCategory(categoryName: String) {
        if (categoryName == SystemCategory.ALL_TASKS || categoryName == SystemCategory.COMPLETED) return
        saveCategories(_categories.value.filter { it != categoryName })
        
        // Move tasks to "General" or some default category
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val tasksToUpdate = repository.allTasks.first()
                .filter { it.category == categoryName }
                .map { it.copy(category = "General") }
            if (tasksToUpdate.isNotEmpty()) {
                repository.updateTasks(tasksToUpdate)
            }
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        if (oldName == SystemCategory.ALL_TASKS || oldName == SystemCategory.COMPLETED) return
        if (newName.isBlank() || _categories.value.contains(newName)) return
        
        saveCategories(_categories.value.map { if (it == oldName) newName else it })
        
        // Update tasks that were in the old category
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val tasksToUpdate = repository.allTasks.first()
                .filter { it.category == oldName }
                .map { it.copy(category = newName) }
            if (tasksToUpdate.isNotEmpty()) {
                repository.updateTasks(tasksToUpdate)
            }
        }

        // Transfer color and icon
        val currentColor = _categoryColors.value[oldName]
        if (currentColor != null) {
            setCategoryColor(newName, currentColor)
        }
        val currentIcon = _categoryIcons.value[oldName]
        if (currentIcon != null) {
            setCategoryIcon(newName, currentIcon)
        }
    }

    fun moveCategory(index: Int, direction: Int) {
        val list = _categories.value.toMutableList()
        val newIndex = index + direction
        if (newIndex >= 0 && newIndex < list.size) {
            val item1 = list[index]
            val item2 = list[newIndex]
            
            // Cannot reorder immutable categories (SystemCategory.ALL_TASKS, SystemCategory.COMPLETED)
            if (item1 == SystemCategory.ALL_TASKS || item1 == SystemCategory.COMPLETED ||
                item2 == SystemCategory.ALL_TASKS || item2 == SystemCategory.COMPLETED) return

            list[index] = item2
            list[newIndex] = item1
            saveCategories(list)
        }
    }

    fun setCategoryColor(category: String, color: Int) {
        val newMap = _categoryColors.value.toMutableMap()
        newMap[category] = color
        _categoryColors.value = newMap
        val jsonObject = JSONObject()
        newMap.forEach { (k, v) -> jsonObject.put(k, v) }
        prefs.edit().putString("category_colors", jsonObject.toString()).apply()
    }

    fun setCategoryIcon(category: String, iconName: String) {
        val newMap = _categoryIcons.value.toMutableMap()
        newMap[category] = iconName
        _categoryIcons.value = newMap
        val jsonObject = JSONObject()
        newMap.forEach { (k, v) -> jsonObject.put(k, v) }
        prefs.edit().putString("category_icons", jsonObject.toString()).apply()
    }
}
