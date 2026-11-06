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
    private val repository: TaskRepository,
    private val firestoreSyncManager: com.fabian.todolist.data.sync.FirestoreSyncManager
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)

    private fun triggerSettingsCloudPush() {
        // CORRECCIÓN: el marcador de "ediciones locales pendientes"
        // (last_settings_local_update) y el envío con debounce los gestiona ahora el
        // FirestoreSyncManager. Antes se lanzaba un push inmediato por cada ajuste y
        // el marcador se sobrescribía también desde el propio sincronizador, lo que
        // provocaba que se perdiera la noción de "cambios sin subir" y que algunos
        // ajustes se revirtieran entre dispositivos.
        firestoreSyncManager.scheduleSettingsPush()
    }

    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        when (key) {
            "accent_color" -> _themeAccent.value = p.getString("accent_color", "system") ?: "system"
            "theme_mode" -> _themeDark.value = p.getString("theme_mode", "system") ?: "system"
            "app_locale" -> _languageCode.value = p.getString("app_locale", "system") ?: "system"
            "date_format" -> _dateFormat.value = p.getString("date_format", "DD/MM/AA") ?: "DD/MM/AA"
            "time_format" -> _timeFormat.value = p.getString("time_format", "24h") ?: "24h"
            "long_press_action" -> _longPressAction.value = p.getString("long_press_action", "select") ?: "select"
            "confirm_on_delete" -> _confirmOnDelete.value = p.getBoolean("confirm_on_delete", true)
            "haptic_feedback_on_complete" -> _hapticFeedbackOnComplete.value = p.getBoolean("haptic_feedback_on_complete", true)
            "require_biometrics" -> _requireBiometrics.value = p.getBoolean("require_biometrics", false)
            "secure_screen_mode" -> _secureScreenMode.value = p.getBoolean("secure_screen_mode", false)
            "encrypt_backups" -> _encryptBackups.value = p.getBoolean("encrypt_backups", false)
            "global_notifications_enabled" -> _notificationsEnabled.value = p.getBoolean("global_notifications_enabled", true)
            "quick_add_notification_enabled" -> _quickAddNotificationEnabled.value = p.getBoolean("quick_add_notification_enabled", false)
            "quick_add_notification_interval" -> _quickAddNotificationInterval.value = p.getString("quick_add_notification_interval", "off") ?: "off"
            "quick_add_quiet_hours_enabled" -> _quickAddQuietHoursEnabled.value = p.getBoolean("quick_add_quiet_hours_enabled", true)
            "notifications_sound" -> _notificationsSound.value = p.getBoolean("notifications_sound", true)
            "notifications_vibrate" -> _notificationsVibrate.value = p.getBoolean("notifications_vibrate", true)
            "reminder_offset_minutes" -> _reminderOffsetMinutes.value = p.getInt("reminder_offset_minutes", 0)
            "snooze_duration_minutes" -> _snoozeDurationMinutes.value = p.getInt("snooze_duration_minutes", 10)
            "trash_retention_days" -> _trashRetentionDays.value = p.getInt("trash_retention_days", 30)
            "ai_model" -> _aiModel.value = p.getString("ai_model", "gemini-3.8-flash") ?: "gemini-3.8-flash"
            "ai_low_latency_mode" -> _aiLowLatencyMode.value = p.getBoolean("ai_low_latency_mode", true)
            "ai_subtask_count" -> _aiSubtaskCount.value = p.getInt("ai_subtask_count", 5)
            "ai_key_type" -> _aiKeyType.value = p.getString("ai_key_type", "public") ?: "public"
            "show_ai_banner" -> _showAiBanner.value = p.getBoolean("show_ai_banner", true)
            "show_backup_card" -> _showBackupCard.value = p.getBoolean("show_backup_card", true)
            "onboarding_completed" -> _onboardingCompleted.value = p.getBoolean("onboarding_completed", true)
            "user_display_name" -> _userName.value = p.getString("user_display_name", "") ?: ""
            "custom_categories" -> _categories.value = loadInitialCategories()
            "category_colors" -> _categoryColors.value = loadCategoryColors()
            "category_icons" -> _categoryIcons.value = loadCategoryIcons()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

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

    private val _secureScreenMode = MutableStateFlow(prefs.getBoolean("secure_screen_mode", false))
    val secureScreenMode: StateFlow<Boolean> = _secureScreenMode.asStateFlow()

    private val _encryptBackups = MutableStateFlow(prefs.getBoolean("encrypt_backups", false))
    val encryptBackups: StateFlow<Boolean> = _encryptBackups.asStateFlow()

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

    private val _aiModel = MutableStateFlow(prefs.getString("ai_model", "gemini-3.8-flash") ?: "gemini-3.8-flash")
    val aiModel: StateFlow<String> = _aiModel.asStateFlow()

    private val _aiLowLatencyMode = MutableStateFlow(prefs.getBoolean("ai_low_latency_mode", true))
    val aiLowLatencyMode: StateFlow<Boolean> = _aiLowLatencyMode.asStateFlow()

    private val _aiApiKey = MutableStateFlow(prefs.getString("ai_api_key", "") ?: "")
    val aiApiKey: StateFlow<String> = _aiApiKey.asStateFlow()

    private val _aiKeyType = MutableStateFlow(prefs.getString("ai_key_type", "public") ?: "public")
    val aiKeyType: StateFlow<String> = _aiKeyType.asStateFlow()

    private val _aiSubtaskCount = MutableStateFlow(prefs.getInt("ai_subtask_count", 5))
    val aiSubtaskCount: StateFlow<Int> = _aiSubtaskCount.asStateFlow()

    private val _showAiBanner = MutableStateFlow(prefs.getBoolean("show_ai_banner", true))
    val showAiBanner: StateFlow<Boolean> = _showAiBanner.asStateFlow()

    private val _showBackupCard = MutableStateFlow(prefs.getBoolean("show_backup_card", true))
    val showBackupCard: StateFlow<Boolean> = _showBackupCard.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", true))
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
        val rawList = if (savedCategoriesString != null) {
            try {
                val jsonArray = JSONArray(savedCategoriesString)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val cat = jsonArray.getString(i).trim()
                    if (cat.isNotEmpty() && !list.contains(cat)) {
                        list.add(cat)
                    }
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        val defaultUserList = listOf("Personal", "Trabajo", "Compras", "Hogar", "Salud")
        val baseList = if (rawList.isNotEmpty()) rawList else (listOf(SystemCategory.ALL_TASKS) + defaultUserList + listOf(SystemCategory.COMPLETED))

        val userCats = baseList.filter { !SystemCategory.isSystemCategory(it) }

        val resolvedUserCats = if (userCats.isNotEmpty()) {
            userCats
        } else {
            defaultUserList
        }

        val result = mutableListOf<String>()
        result.add(SystemCategory.ALL_TASKS)
        result.addAll(resolvedUserCats)
        result.add(SystemCategory.COMPLETED)
        return result
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
        prefs.edit().putString("accent_color", accent).commit()
        triggerSettingsCloudPush()
    }

    fun setThemeDark(mode: String) {
        _themeDark.value = mode
        prefs.edit().putString("theme_mode", mode).commit()
        triggerSettingsCloudPush()
    }

    fun setLanguageCode(lang: String) {
        _languageCode.value = lang
        prefs.edit().putString("app_locale", lang).commit()
        triggerSettingsCloudPush()
        
        val appLocale = if (lang == "system") {
            androidx.core.os.LocaleListCompat.getEmptyLocaleList()
        } else {
            androidx.core.os.LocaleListCompat.forLanguageTags(lang)
        }
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun setDateFormat(format: String) {
        _dateFormat.value = format
        prefs.edit().putString("date_format", format).commit()
        triggerSettingsCloudPush()
    }

    fun setTimeFormat(format: String) {
        _timeFormat.value = format
        prefs.edit().putString("time_format", format).commit()
        triggerSettingsCloudPush()
    }

    fun setLongPressAction(action: String) {
        _longPressAction.value = action
        prefs.edit().putString("long_press_action", action).commit()
        triggerSettingsCloudPush()
    }

    fun setConfirmOnDelete(confirm: Boolean) {
        _confirmOnDelete.value = confirm
        prefs.edit().putBoolean("confirm_on_delete", confirm).commit()
        triggerSettingsCloudPush()
    }

    fun setHapticFeedbackOnComplete(feedback: Boolean) {
        _hapticFeedbackOnComplete.value = feedback
        prefs.edit().putBoolean("haptic_feedback_on_complete", feedback).commit()
        triggerSettingsCloudPush()
    }

    fun setRequireBiometrics(require: Boolean) {
        _requireBiometrics.value = require
        prefs.edit().putBoolean("require_biometrics", require).commit()
        triggerSettingsCloudPush()
    }

    fun setSecureScreenMode(enabled: Boolean) {
        _secureScreenMode.value = enabled
        prefs.edit().putBoolean("secure_screen_mode", enabled).commit()
        triggerSettingsCloudPush()
    }

    fun setEncryptBackups(enabled: Boolean) {
        _encryptBackups.value = enabled
        prefs.edit().putBoolean("encrypt_backups", enabled).commit()
        triggerSettingsCloudPush()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("global_notifications_enabled", enabled).commit()
        triggerSettingsCloudPush()
    }

    fun setQuickAddNotificationEnabled(enabled: Boolean) {
        val interval = if (enabled) "6h" else "off"
        setQuickAddNotificationInterval(interval)
    }

    fun setQuickAddNotificationInterval(intervalCode: String) {
        _quickAddNotificationInterval.value = intervalCode
        prefs.edit().putString("quick_add_notification_interval", intervalCode).commit()
        
        val isEnabled = intervalCode != "off"
        _quickAddNotificationEnabled.value = isEnabled
        prefs.edit().putBoolean("quick_add_notification_enabled", isEnabled).commit()
        triggerSettingsCloudPush()

        val context = getApplication<Application>().applicationContext
        QuickAddNotificationHelper.schedulePeriodicReminders(context, intervalCode)
    }

    fun setQuickAddQuietHoursEnabled(enabled: Boolean) {
        _quickAddQuietHoursEnabled.value = enabled
        prefs.edit().putBoolean("quick_add_quiet_hours_enabled", enabled).commit()
        triggerSettingsCloudPush()
    }

    fun setNotificationsSound(enabled: Boolean) {
        _notificationsSound.value = enabled
        prefs.edit().putBoolean("notifications_sound", enabled).commit()
        triggerSettingsCloudPush()
    }

    fun setNotificationsVibrate(enabled: Boolean) {
        _notificationsVibrate.value = enabled
        prefs.edit().putBoolean("notifications_vibrate", enabled).commit()
        triggerSettingsCloudPush()
    }

    fun setReminderOffsetMinutes(minutes: Int) {
        _reminderOffsetMinutes.value = minutes
        prefs.edit().putInt("reminder_offset_minutes", minutes).commit()
        triggerSettingsCloudPush()
    }

    fun setSnoozeDuration(minutes: Int) {
        _snoozeDurationMinutes.value = minutes
        prefs.edit().putInt("snooze_duration_minutes", minutes).commit()
        triggerSettingsCloudPush()
    }

    fun setTrashRetentionDays(days: Int) {
        _trashRetentionDays.value = days
        prefs.edit().putInt("trash_retention_days", days).commit()
        triggerSettingsCloudPush()
    }

    fun setAiModel(model: String) {
        _aiModel.value = model
        prefs.edit().putString("ai_model", model).commit()
        triggerSettingsCloudPush()
    }

    fun setAiLowLatencyMode(enabled: Boolean) {
        _aiLowLatencyMode.value = enabled
        prefs.edit().putBoolean("ai_low_latency_mode", enabled).commit()
        triggerSettingsCloudPush()
    }

    fun setAiApiKey(key: String) {
        _aiApiKey.value = key
        prefs.edit().putString("ai_api_key", key).commit()
        triggerSettingsCloudPush()
    }

    fun setAiKeyType(type: String) {
        _aiKeyType.value = type
        prefs.edit().putString("ai_key_type", type).commit()
        triggerSettingsCloudPush()
    }

    fun setAiSubtaskCount(count: Int) {
        _aiSubtaskCount.value = count
        prefs.edit().putInt("ai_subtask_count", count).commit()
        triggerSettingsCloudPush()
    }

    fun setShowAiBanner(show: Boolean) {
        _showAiBanner.value = show
        prefs.edit().putBoolean("show_ai_banner", show).commit()
        triggerSettingsCloudPush()
    }

    fun setShowBackupCard(show: Boolean) {
        _showBackupCard.value = show
        prefs.edit().putBoolean("show_backup_card", show).commit()
        triggerSettingsCloudPush()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
        prefs.edit().putBoolean("onboarding_completed", completed).commit()
        triggerSettingsCloudPush()
    }

    fun setUserName(name: String) {
        _userName.value = name
        prefs.edit().putString("user_display_name", name).commit()
        triggerSettingsCloudPush()
    }

    fun saveCategories(list: List<String>) {
        val userCats = list.filter { !SystemCategory.isSystemCategory(it) }.distinct()

        val resolvedUserCats = if (userCats.isNotEmpty()) {
            userCats
        } else {
            listOf("Personal", "Trabajo", "Compras", "Hogar", "Salud")
        }

        val fullList = mutableListOf<String>()
        fullList.add(SystemCategory.ALL_TASKS)
        fullList.addAll(resolvedUserCats)
        fullList.add(SystemCategory.COMPLETED)

        val jsonArray = JSONArray()
        fullList.forEach { jsonArray.put(it) }
        prefs.edit().putString("custom_categories", jsonArray.toString()).commit()
        _categories.value = fullList
        triggerSettingsCloudPush()
    }

    fun addCategory(categoryName: String, color: Int? = null, iconName: String? = null) {
        val trimmed = categoryName.trim()
        if (trimmed.isBlank() || _categories.value.contains(trimmed) || SystemCategory.isSystemCategory(trimmed)) return
        val currentList = _categories.value.toMutableList()
        val completedIndex = currentList.indexOf(SystemCategory.COMPLETED)
        if (completedIndex != -1) {
            // Insert right before SystemCategory.COMPLETED so it stays in the custom categories block
            currentList.add(completedIndex, trimmed)
        } else {
            currentList.add(trimmed)
        }
        if (color != null) {
            setCategoryColor(trimmed, color)
        }
        if (iconName != null) {
            setCategoryIcon(trimmed, iconName)
        }
        saveCategories(currentList)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.reorderTasksByCategory(currentList)
        }
    }

    fun deleteCategory(categoryName: String) {
        if (SystemCategory.isSystemCategory(categoryName)) return
        
        // 1. Reassign tasks in Room from deleted category to General
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val tasksToUpdate = repository.allTasks.first()
                .filter { it.category == categoryName }
                .map { it.copy(category = "General") }
            if (tasksToUpdate.isNotEmpty()) {
                repository.updateTasks(tasksToUpdate)
            }
        }

        // 2. Clean up color & icon maps for deleted category
        val colorMap = _categoryColors.value.toMutableMap()
        if (colorMap.containsKey(categoryName)) {
            colorMap.remove(categoryName)
            _categoryColors.value = colorMap
            val jsonObject = JSONObject()
            colorMap.forEach { (k, v) -> jsonObject.put(k, v) }
            prefs.edit().putString("category_colors", jsonObject.toString()).commit()
        }

        val iconMap = _categoryIcons.value.toMutableMap()
        if (iconMap.containsKey(categoryName)) {
            iconMap.remove(categoryName)
            _categoryIcons.value = iconMap
            val jsonObject = JSONObject()
            iconMap.forEach { (k, v) -> jsonObject.put(k, v) }
            prefs.edit().putString("category_icons", jsonObject.toString()).commit()
        }

        // 3. Save remaining categories and push to cloud
        saveCategories(_categories.value.filter { it != categoryName })
    }

    fun renameCategory(oldName: String, newName: String) {
        if (SystemCategory.isSystemCategory(oldName) || SystemCategory.isSystemCategory(newName)) return
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank() || (trimmedNew != oldName && _categories.value.contains(trimmedNew))) return
        if (trimmedNew == oldName) return
        
        // 1. Update tasks in Room so oldName is no longer associated with tasks
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val tasksToUpdate = repository.allTasks.first()
                .filter { it.category == oldName }
                .map { it.copy(category = trimmedNew) }
            if (tasksToUpdate.isNotEmpty()) {
                repository.updateTasks(tasksToUpdate)
            }
        }

        // 2. Transfer color & icon maps cleanly
        val colorMap = _categoryColors.value.toMutableMap()
        val oldColor = colorMap.remove(oldName)
        if (oldColor != null) {
            colorMap[trimmedNew] = oldColor
            _categoryColors.value = colorMap
            val jsonObject = JSONObject()
            colorMap.forEach { (k, v) -> jsonObject.put(k, v) }
            prefs.edit().putString("category_colors", jsonObject.toString()).commit()
        }

        val iconMap = _categoryIcons.value.toMutableMap()
        val oldIcon = iconMap.remove(oldName)
        if (oldIcon != null) {
            iconMap[trimmedNew] = oldIcon
            _categoryIcons.value = iconMap
            val jsonObject = JSONObject()
            iconMap.forEach { (k, v) -> jsonObject.put(k, v) }
            prefs.edit().putString("category_icons", jsonObject.toString()).commit()
        }

        // 3. Update category list and push to cloud
        saveCategories(_categories.value.map { if (it == oldName) trimmedNew else it })
    }

    fun moveUserCategory(category: String, direction: Int) {
        val currentList = _categories.value.toMutableList()
        val userCats = currentList.filter { !SystemCategory.isSystemCategory(it) }.toMutableList()
        val userIndex = userCats.indexOf(category)
        if (userIndex == -1) return

        val targetIndex = userIndex + direction
        if (targetIndex in 0 until userCats.size) {
            val temp = userCats[userIndex]
            userCats[userIndex] = userCats[targetIndex]
            userCats[targetIndex] = temp

            val result = mutableListOf<String>()
            if (currentList.contains(SystemCategory.ALL_TASKS)) {
                result.add(SystemCategory.ALL_TASKS)
            }
            result.addAll(userCats)
            if (currentList.contains(SystemCategory.COMPLETED)) {
                result.add(SystemCategory.COMPLETED)
            }
            saveCategories(result)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.reorderTasksByCategory(result)
            }
        }
    }

    fun moveCategory(index: Int, direction: Int) {
        val list = _categories.value
        if (index in list.indices) {
            val category = list[index]
            moveUserCategory(category, direction)
        }
    }

    fun setCategoryColor(category: String, color: Int) {
        val newMap = _categoryColors.value.toMutableMap()
        newMap[category] = color
        _categoryColors.value = newMap
        val jsonObject = JSONObject()
        newMap.forEach { (k, v) -> jsonObject.put(k, v) }
        prefs.edit().putString("category_colors", jsonObject.toString()).commit()
        triggerSettingsCloudPush()
    }

    fun setCategoryIcon(category: String, iconName: String) {
        val newMap = _categoryIcons.value.toMutableMap()
        newMap[category] = iconName
        _categoryIcons.value = newMap
        val jsonObject = JSONObject()
        newMap.forEach { (k, v) -> jsonObject.put(k, v) }
        prefs.edit().putString("category_icons", jsonObject.toString()).commit()
        triggerSettingsCloudPush()
    }

    data class StorageStats(
        val dbSizeBytes: Long = 0L,
        val cacheSizeBytes: Long = 0L,
        val dbFormatted: String = "0 KB",
        val cacheFormatted: String = "0 KB",
        val isCalculating: Boolean = false
    )

    private val _storageStats = MutableStateFlow(StorageStats(isCalculating = true))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _storageStats.value = _storageStats.value.copy(isCalculating = true)
            val context = getApplication<Application>().applicationContext
            
            // Database file size (including WAL and SHM)
            val dbFile = context.getDatabasePath("fabitodo_database")
            val walFile = context.getDatabasePath("fabitodo_database-wal")
            val shmFile = context.getDatabasePath("fabitodo_database-shm")
            var dbSize = 0L
            if (dbFile.exists()) dbSize += dbFile.length()
            if (walFile.exists()) dbSize += walFile.length()
            if (shmFile.exists()) dbSize += shmFile.length()

            // Cache size
            val cacheSize = getDirectorySize(context.cacheDir) + getDirectorySize(context.codeCacheDir)

            _storageStats.value = StorageStats(
                dbSizeBytes = dbSize,
                cacheSizeBytes = cacheSize,
                dbFormatted = formatBytes(dbSize),
                cacheFormatted = formatBytes(cacheSize),
                isCalculating = false
            )
        }
    }

    private fun getDirectorySize(dir: java.io.File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            total += if (child.isDirectory) getDirectorySize(child) else child.length()
        }
        return total
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes <= 0 -> "0 KB"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> String.format(Locale.US, "%.2f MB", bytes / (1024f * 1024f))
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearAppCache(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            var success = true
            try {
                deleteDirContent(context.cacheDir)
                coil.Coil.imageLoader(context).diskCache?.clear()
                coil.Coil.imageLoader(context).memoryCache?.clear()
            } catch (e: Exception) {
                success = false
            }
            refreshStorageStats()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    private fun deleteDirContent(dir: java.io.File?): Boolean {
        if (dir == null || !dir.exists()) return true
        val children = dir.listFiles() ?: return true
        var allDeleted = true
        for (child in children) {
            if (child.isDirectory) {
                deleteDirContent(child)
            }
            if (!child.delete()) {
                allDeleted = false
            }
        }
        return allDeleted
    }

    fun optimizeDatabase(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            var success = true
            try {
                val db = com.fabian.todolist.data.AppDatabase.getDatabase(context)
                val writableDb = db.openHelper.writableDatabase

                // 1. Flush and truncate Write-Ahead Log (WAL) journal file
                try {
                    writableDb.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                } catch (e: Exception) {
                    try {
                        writableDb.execSQL("PRAGMA wal_checkpoint(FULL)")
                    } catch (e2: Exception) {
                        android.util.Log.w("SettingsViewModel", "WAL checkpoint warning: ${e2.message}")
                    }
                }

                // 2. Attempt VACUUM to reclaim unused pages if active read cursors allow
                try {
                    writableDb.execSQL("VACUUM")
                } catch (e: Exception) {
                    android.util.Log.w("SettingsViewModel", "VACUUM skipped due to active readers: ${e.message}")
                }

                // 3. Re-analyze statistics and optimize query planning
                try {
                    writableDb.execSQL("PRAGMA optimize")
                } catch (e: Exception) {
                    android.util.Log.w("SettingsViewModel", "PRAGMA optimize warning: ${e.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error optimizing database", e)
                success = false
            }
            refreshStorageStats()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(success)
            }
        }
    }
}
