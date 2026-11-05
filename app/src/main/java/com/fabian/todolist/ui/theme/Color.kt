package com.fabian.todolist.ui.theme

import androidx.compose.ui.graphics.Color

// Default template colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// FabiToDo Unified Palette
object AppColors {
    // Priorities
    val PriorityCritical = Color(0xFFB3261E)
    val PriorityHigh = Color(0xFFEA4335)
    val PriorityMedium = Color(0xFFFBBC05)
    val PriorityLow = Color(0xFF34A853)
    val PriorityNone = Color(0xFF808080)

    // Eisenhower Quadrants
    val QuadrantDo = Color(0xFFEA4335)     // Q1: Urgent & Important
    val QuadrantSchedule = Color(0xFF4285F4) // Q2: Important & Not Urgent
    val QuadrantDelegate = Color(0xFFFBBC05) // Q3: Urgent & Not Important
    val QuadrantEliminate = Color(0xFF9E9E9E) // Q4: Not Urgent & Not Important

    // System Status
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFB8C00)
    val Error = Color(0xFFE53935)
    val Info = Color(0xFF2196F3)

    // Settings Categorization
    val SettingsAccount = Color(0xFF4285F4)
    val SettingsAppearance = Color(0xFF915C6D)
    val SettingsLanguage = Color(0xFF1E6370)
    val SettingsDateTime = Color(0xFF448AFF)
    val SettingsBehavior = Color(0xFF673AB7)
    val SettingsAI = Color(0xFF6366F1)
    val SettingsCategories = Color(0xFF5E4856)
    val SettingsNotifications = Color(0xFFD4823B)
    val SettingsBackup = Color(0xFF455A64)
    val SettingsSecurity = Color(0xFFE53935)
}
