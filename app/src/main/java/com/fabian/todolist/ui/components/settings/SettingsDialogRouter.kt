package com.fabian.todolist.ui.components.settings

import androidx.compose.runtime.*
import com.fabian.todolist.ui.SettingsViewModel
import com.fabian.todolist.ui.TaskViewModel
import com.fabian.todolist.ui.AuthViewModel
import com.fabian.todolist.ui.SettingsDestinations

@Composable
fun SettingsDialogRouter(
    currentDestination: String,
    onDestinationChanged: (String) -> Unit,
    currentAccent: String,
    currentDark: String,
    onSaveAccent: (String) -> Unit,
    onSaveDark: (String) -> Unit,
    currentLang: String,
    onSaveLang: (String) -> Unit,
    viewModel: TaskViewModel,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    onNavigateToGoogleLogin: () -> Unit,
    categories: List<String>,
    categoryColors: Map<String, Int>,
    categoryIcons: Map<String, String>
) {
    var editingCategory by remember { mutableStateOf<String?>(null) }

    if (currentDestination == SettingsDestinations.DATETIME_FORMAT) {
        DateTimeFormatSettingsDialog(
            viewModel = settingsViewModel,
            onDismiss = { onDestinationChanged(SettingsDestinations.NONE) }
        )
    }

    if (currentDestination == SettingsDestinations.THEME) {
        AppearanceSettingsDialog(
            currentAccent = currentAccent,
            currentDark = currentDark,
            onSaveAccent = onSaveAccent,
            onSaveDark = onSaveDark,
            onDismiss = { onDestinationChanged(SettingsDestinations.NONE) }
        )
    }

    if (currentDestination == SettingsDestinations.LANGUAGE) {
        LanguageSettingsDialog(
            currentLang = currentLang,
            onSaveLang = onSaveLang,
            onDismiss = { onDestinationChanged(SettingsDestinations.NONE) }
        )
    }

    if (currentDestination == SettingsDestinations.CATEGORIES) {
        CategoriesSettingsDialog(
            categories = categories,
            categoryColors = categoryColors,
            categoryIcons = categoryIcons,
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            onEditCategory = { category ->
                editingCategory = category
            },
            onDismiss = { onDestinationChanged(SettingsDestinations.NONE) }
        )
    }

    if (currentDestination == SettingsDestinations.ACCOUNT) {
        AccountSettingsDialog(
            authViewModel = authViewModel,
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            onNavigateToGoogleLogin = onNavigateToGoogleLogin,
            onDismiss = { onDestinationChanged(SettingsDestinations.NONE) }
        )
    }

    if (currentDestination == SettingsDestinations.BACKUP) {
        BackupSettingsDialog(
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            onDismiss = { onDestinationChanged(SettingsDestinations.NONE) }
        )
    }

    editingCategory?.let { cat ->
        EditCategoryDialog(
            category = cat,
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            onDismiss = { editingCategory = null }
        )
    }

    if (currentDestination == SettingsDestinations.NOTIFICATIONS) {
        NotificationsSettingsDialog(
            viewModel = settingsViewModel,
            onDismiss = { onDestinationChanged(SettingsDestinations.NONE) }
        )
    }

    if (currentDestination == SettingsDestinations.BEHAVIOR) {
        BehaviorSettingsDialog(
            viewModel = settingsViewModel,
            onDismiss = { onDestinationChanged(SettingsDestinations.NONE) }
        )
    }
    
    if (currentDestination == SettingsDestinations.AI) {
        AISettingsDialog(
            viewModel = settingsViewModel,
            taskViewModel = viewModel,
            onDismiss = { onDestinationChanged(SettingsDestinations.NONE) }
        )
    }
}
