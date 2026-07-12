package com.fabian.todolist.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.fabian.todolist.ui.SettingsViewModel
import com.fabian.todolist.ui.components.FabiSectionHeader
import com.fabian.todolist.ui.components.FabiPriorityBadge
import com.fabian.todolist.ui.components.FabiSettingRow
import com.fabian.todolist.ui.components.settings.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.DialogProperties
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import coil.compose.rememberAsyncImagePainter
import com.fabian.todolist.MainActivity
import com.fabian.todolist.R
import com.fabian.todolist.data.AuthManager
import com.fabian.todolist.util.getLocalizedCategoryName
import com.fabian.todolist.util.getCategoryIconVector
import com.fabian.todolist.util.getCategoryColor

// Destination constants as strings for rememberSaveable support
object SettingsDestinations {
    const val NONE = "none"
    const val THEME = "theme"
    const val LANGUAGE = "language"
    const val CATEGORIES = "categories"
    const val ACCOUNT = "account"
    const val BACKUP = "backup"
    const val NOTIFICATIONS = "notifications"
    const val DATETIME_FORMAT = "datetime"
    const val BEHAVIOR = "behavior"
    const val AI = "ai"
}

object SettingsDimens {
    val CardRadiusRoot = RoundedCornerShape(18.dp)
    val CardRadiusLarge = RoundedCornerShape(24.dp)
    val CardRadiusMedium = RoundedCornerShape(20.dp)
    val CardRadiusSmall = RoundedCornerShape(12.dp)
}





@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsPreferencesDialog(
    currentAccent: String,
    currentDark: String,
    currentLang: String,
    onSaveAccent: (String) -> Unit,
    onSaveDark: (String) -> Unit,
    onSaveLang: (String) -> Unit,
    viewModel: TaskViewModel,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    onNavigateToGoogleLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    val categories by settingsViewModel.categories.collectAsStateWithLifecycle()
    val categoryColors by settingsViewModel.categoryColors.collectAsStateWithLifecycle()
    val categoryIcons by settingsViewModel.categoryIcons.collectAsStateWithLifecycle()
    var currentDestination by rememberSaveable { mutableStateOf(SettingsDestinations.NONE) }

    val globalNotificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val user = authManager.getCurrentUser()
    val isGuest = authManager.isGuestUser()
    
    val titleAppearance = stringResource(R.string.settings_category_appearance)
    val descAppearance = stringResource(R.string.settings_appearance_desc)
    
    val titleLang = stringResource(R.string.app_language)
    val descLang = stringResource(R.string.settings_language_desc)
    
    val titleDateTime = stringResource(R.string.settings_datetime_title)
    val descDateTime = stringResource(R.string.settings_datetime_desc)
    
    val titleBehavior = stringResource(R.string.settings_behavior_title)
    val descBehavior = stringResource(R.string.settings_behavior_desc)
    
    val titleAI = stringResource(R.string.settings_ai_title)
    val descAI = stringResource(R.string.settings_ai_desc)
    
    val titleCategories = stringResource(R.string.dialog_task_list_header)
    val descCategories = stringResource(R.string.settings_categories_desc)
    
    val titleNotifications = stringResource(R.string.settings_category_notifications)
    val descNotifications = if (globalNotificationsEnabled) stringResource(R.string.desc_active_alert) else stringResource(R.string.desc_inactive_alert)
    
    val titleSync = stringResource(R.string.backup_restore_title)
    val descSync = stringResource(R.string.settings_sync_desc)
    
    val titleAccount = stringResource(R.string.accounts_title)
    val descAccount = when {
        user != null -> user.displayName ?: user.email ?: stringResource(R.string.settings_account_desc)
        isGuest -> stringResource(R.string.guest_user)
        else -> stringResource(R.string.settings_account_desc)
    }

    val userAvatarDescription = stringResource(R.string.content_desc_user_avatar)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // High-fidelity elegant top actions bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.nav_back), 
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        SettingsIntroCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }

                    // --- Grupo 1: Personalización ---
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp, 6.dp)
                                    .background(Color(0xFF915C6D), RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.settings_section_personalization),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                        SettingsSectionCard {
                            SettingsPreferenceItem(
                                title = titleAppearance,
                                subtitle = descAppearance,
                                icon = Icons.Rounded.Palette,
                                iconContainerColor = Color(0xFF915C6D),
                                onClick = { currentDestination = SettingsDestinations.THEME }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            SettingsPreferenceItem(
                                title = titleLang,
                                subtitle = descLang,
                                icon = Icons.Rounded.Language,
                                iconContainerColor = Color(0xFF1E6370),
                                onClick = { currentDestination = SettingsDestinations.LANGUAGE }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            SettingsPreferenceItem(
                                title = titleDateTime,
                                subtitle = descDateTime,
                                icon = Icons.Default.DateRange,
                                iconContainerColor = Color(0xFF448AFF),
                                onClick = { currentDestination = SettingsDestinations.DATETIME_FORMAT }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            SettingsPreferenceItem(
                                title = titleBehavior,
                                subtitle = descBehavior,
                                icon = Icons.Rounded.TouchApp,
                                iconContainerColor = Color(0xFF673AB7),
                                onClick = { currentDestination = SettingsDestinations.BEHAVIOR }
                            )
                        }
                    }

                    // --- Grupo 2: Inteligencia Artificial ---
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp, 6.dp)
                                    .background(Color(0xFF6366F1), RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.settings_section_ai),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                        SettingsSectionCard {
                            SettingsPreferenceItem(
                                title = titleAI,
                                subtitle = descAI,
                                icon = Icons.Rounded.AutoAwesome,
                                iconContainerColor = Color(0xFF6366F1),
                                onClick = { currentDestination = SettingsDestinations.AI }
                            )
                        }
                    }

                    // --- Grupo 3: Organización ---
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp, 6.dp)
                                    .background(Color(0xFFD4823B), RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.settings_section_organization),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                        SettingsSectionCard {
                            SettingsPreferenceItem(
                                title = titleCategories,
                                subtitle = descCategories,
                                icon = Icons.AutoMirrored.Rounded.List,
                                iconContainerColor = Color(0xFF5E4856),
                                onClick = { currentDestination = SettingsDestinations.CATEGORIES }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            SettingsPreferenceItem(
                                title = titleNotifications,
                                subtitle = descNotifications,
                                icon = Icons.Rounded.Notifications,
                                iconContainerColor = Color(0xFFD4823B),
                                onClick = { currentDestination = SettingsDestinations.NOTIFICATIONS }
                            )
                        }
                    }

                    // --- Grupo 4: Cuenta, Sincronización y Seguridad ---
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp, 6.dp)
                                    .background(Color(0xFF607D8B), RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.settings_section_account_backup),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                        SettingsSectionCard {
                            SettingsPreferenceItem(
                                title = titleSync,
                                subtitle = descSync,
                                icon = Icons.Filled.CloudSync,
                                iconContainerColor = Color(0xFF455A64),
                                onClick = { currentDestination = SettingsDestinations.BACKUP }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            SettingsPreferenceItem(
                                title = titleAccount,
                                subtitle = descAccount,
                                icon = Icons.Rounded.AccountCircle,
                                iconContainerColor = Color(0xFF607D8B),
                                onClick = { currentDestination = SettingsDestinations.ACCOUNT },
                                trailingContent = if (user?.photoUrl != null) {
                                    {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(user.photoUrl),
                                            contentDescription = userAvatarDescription,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    // --- Designer Brand Brand Footer ---
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.settings_brand_footer),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    SettingsDialogRouter(
        currentDestination = currentDestination,
        onDestinationChanged = { currentDestination = it },
        currentAccent = currentAccent,
        currentDark = currentDark,
        onSaveAccent = onSaveAccent,
        onSaveDark = onSaveDark,
        currentLang = currentLang,
        onSaveLang = onSaveLang,
        viewModel = viewModel,
        settingsViewModel = settingsViewModel,
        authViewModel = authViewModel,
        onNavigateToGoogleLogin = onNavigateToGoogleLogin,
        categories = categories,
        categoryColors = categoryColors,
        categoryIcons = categoryIcons
    )
}