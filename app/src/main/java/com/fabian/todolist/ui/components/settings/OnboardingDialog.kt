package com.fabian.todolist.ui.components.settings

import android.Manifest
import android.app.Activity
import coil.compose.rememberAsyncImagePainter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fabian.todolist.R
import com.fabian.todolist.ui.SettingsViewModel
import com.fabian.todolist.util.UiUtils

data class OnboardingUserProfile(
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    settingsViewModel: SettingsViewModel,
    authViewModel: com.fabian.todolist.ui.AuthViewModel,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableStateOf(1) }
    val totalSteps = 12

    // --- State Values initialized from SettingsViewModel initially ---
    val userName by settingsViewModel.userName.collectAsState()
    var tempUserName by rememberSaveable { mutableStateOf(userName.ifBlank { "" }) }

    val appLang by settingsViewModel.languageCode.collectAsState()
    var tempLang by rememberSaveable { mutableStateOf(appLang) }

    val themeDark by settingsViewModel.themeDark.collectAsState()
    var tempDark by rememberSaveable { mutableStateOf(themeDark) }

    val themeAccent by settingsViewModel.themeAccent.collectAsState()
    var tempAccent by rememberSaveable { mutableStateOf(themeAccent) }

    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    var tempNotifsEnabled by rememberSaveable { mutableStateOf(notificationsEnabled) }

    val notificationsSound by settingsViewModel.notificationsSound.collectAsState()
    var tempSound by rememberSaveable { mutableStateOf(notificationsSound) }

    val notificationsVibrate by settingsViewModel.notificationsVibrate.collectAsState()
    var tempVibrate by rememberSaveable { mutableStateOf(notificationsVibrate) }

    val confirmDelete by settingsViewModel.confirmOnDelete.collectAsState()
    var tempConfirmDelete by rememberSaveable { mutableStateOf(confirmDelete) }

    val quickAddInterval by settingsViewModel.quickAddNotificationInterval.collectAsState()
    var tempQuickAddInterval by rememberSaveable { mutableStateOf(quickAddInterval) }

    val aiModel by settingsViewModel.aiModel.collectAsState()
    var tempAiModel by rememberSaveable { mutableStateOf(aiModel) }

    val aiSubtaskCount by settingsViewModel.aiSubtaskCount.collectAsState()
    var tempAiSubtaskCount by rememberSaveable { mutableStateOf(aiSubtaskCount) }

    // Auth States from AuthViewModel
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val isGuest by authViewModel.isGuest.collectAsState()
    val isAuthLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.error.collectAsState()

    // Sync Google user details when logged in
    val userProfile = remember(isLoggedIn, isGuest) {
        val fbUser = if (isLoggedIn && !isGuest) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser else null
        if (fbUser != null) {
            OnboardingUserProfile(fbUser.displayName, fbUser.email, fbUser.photoUrl?.toString())
        } else {
            null
        }
    }

    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            tempUserName = userProfile.displayName ?: tempUserName
        }
    }

    // Permission launcher for Push Notifications
    var hasPostNotificationPermission by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPostNotificationPermission = isGranted
            if (isGranted) {
                tempNotifsEnabled = true
                UiUtils.showLongToast(context, R.string.notifications_permission_granted)
            } else {
                // Previously this branch also showed "granted" and enabled notifications —
                // a copy-paste bug that lied to the user about the permission state.
                tempNotifsEnabled = false
                UiUtils.showLongToast(context, R.string.notifications_permission_denied)
            }
        }
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Setup Progress Header
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = totalSteps,
                lang = tempLang,
                onBackClick = {
                    if (currentStep > 1) currentStep--
                }
            )

            // Main setup body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    label = "onboarding_steps"
                ) { step ->
                    when (step) {
                        1 -> StepWelcome(appLang)
                        2 -> StepAccount(authViewModel, isLoggedIn, isGuest, isAuthLoading, authError, userProfile, appLang)
                        3 -> StepLanguage(appLang, { lang -> tempLang = lang; settingsViewModel.setLanguageCode(lang) })
                        4 -> StepTheme(tempDark == "dark", { tempDark = if (it) "dark" else "light"; settingsViewModel.setThemeDark(if (it) "dark" else "light") }, tempAccent, { tempAccent = it; settingsViewModel.setThemeAccent(it) }, appLang)
                        5 -> StepNotifications(tempNotifsEnabled, { tempNotifsEnabled = it }, { 
                            tempNotifsEnabled = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }, appLang)
                        6 -> StepAlerts(tempSound, { tempSound = it }, tempVibrate, { tempVibrate = it }, appLang)
                        7 -> StepHabits(tempQuickAddInterval, { tempQuickAddInterval = it }, appLang)
                        8 -> StepAiModel(tempAiModel, { tempAiModel = it }, appLang)
                        9 -> StepAiComplexity(tempAiSubtaskCount, { tempAiSubtaskCount = it }, appLang)
                        10 -> StepSecurity(tempConfirmDelete, { tempConfirmDelete = it }, appLang)
                        11 -> StepPrivacy(appLang)
                        12 -> StepFinish(tempUserName, { tempUserName = it }, appLang)
                        else -> StepWelcome(appLang)
                    }
                }
            }

            // Bottom Navigation
            OnboardingFooterNavigation(
                currentStep = currentStep,
                totalSteps = totalSteps,
                lang = tempLang,
                onNext = {
                    if (currentStep < totalSteps) {
                        currentStep++
                    } else {
                        // Check if we need to sign in as guest before proceeding
                        if (!isLoggedIn) {
                            authViewModel.continueAsGuest {
                                saveAndFinish(settingsViewModel, tempUserName, tempAccent, tempDark, tempNotifsEnabled, tempSound, tempVibrate, tempConfirmDelete, tempQuickAddInterval, tempAiModel, tempAiSubtaskCount, onFinished)
                            }
                        } else {
                            saveAndFinish(settingsViewModel, tempUserName, tempAccent, tempDark, tempNotifsEnabled, tempSound, tempVibrate, tempConfirmDelete, tempQuickAddInterval, tempAiModel, tempAiSubtaskCount, onFinished)
                        }
                    }
                }
            )
        }
    }
}

private fun saveAndFinish(
    settingsViewModel: SettingsViewModel,
    tempUserName: String,
    tempAccent: String,
    tempDark: String,
    tempNotifsEnabled: Boolean,
    tempSound: Boolean,
    tempVibrate: Boolean,
    tempConfirmDelete: Boolean,
    tempQuickAddInterval: String,
    tempAiModel: String,
    tempAiSubtaskCount: Int,
    onFinished: () -> Unit
) {
    settingsViewModel.setUserName(tempUserName.trim().ifBlank { "Invitado" })
    settingsViewModel.setThemeAccent(tempAccent)
    settingsViewModel.setThemeDark(tempDark)
    settingsViewModel.setNotificationsEnabled(tempNotifsEnabled)
    settingsViewModel.setNotificationsSound(tempSound)
    settingsViewModel.setNotificationsVibrate(tempVibrate)
    settingsViewModel.setConfirmOnDelete(tempConfirmDelete)
    settingsViewModel.setQuickAddNotificationInterval(tempQuickAddInterval)
    settingsViewModel.setAiModel(tempAiModel)
    settingsViewModel.setAiSubtaskCount(tempAiSubtaskCount)
    settingsViewModel.setOnboardingCompleted(true)
    onFinished()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingDialog(
    settingsViewModel: SettingsViewModel,
    onFinished: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Force user to complete onboarding setup */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        val authViewModel: com.fabian.todolist.ui.AuthViewModel = hiltViewModel()
        OnboardingScreen(
            settingsViewModel = settingsViewModel,
            authViewModel = authViewModel,
            onFinished = onFinished
        )
    }
}

@Composable
fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int,
    lang: String,
    onBackClick: () -> Unit
) {
    val progressFraction by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps.toFloat(),
        animationSpec = tween(300),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (currentStep > 1) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${currentStep} / ${totalSteps}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = onboardingString("of_word", lang).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Refined Progress Bar
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(6.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun OnboardingFooterNavigation(
    currentStep: Int,
    totalSteps: Int,
    lang: String,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (currentStep == totalSteps) onboardingString("all_set", lang) else onboardingString("next_step", lang),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (currentStep) {
                            1 -> onboardingString("step_welcome", lang)
                            2 -> onboardingString("step_account", lang)
                            3 -> onboardingString("step_visual", lang)
                            4 -> onboardingString("step_alerts", lang)
                            5 -> onboardingString("step_habits", lang)
                            6 -> onboardingString("step_ai", lang)
                            7 -> onboardingString("step_lang", lang)
                            8 -> onboardingString("step_time", lang)
                            9 -> onboardingString("step_quickadd", lang)
                            10 -> onboardingString("step_security", lang)
                            11 -> onboardingString("step_privacy", lang)
                            12 -> onboardingString("step_finish", lang)
                            else -> onboardingString("step_start", lang)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 20.dp),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentStep == totalSteps) onboardingString("start_excl", lang) else onboardingString("next", lang),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = if (currentStep == totalSteps) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// ======================== STEPS IMPLEMENTATIONS ========================

@Composable
fun StepWelcomeAndLanguage(
    tempUserName: String,
    onUserNameChange: (String) -> Unit,
    selectedLang: String,
    onLangSelected: (String) -> Unit,
    authViewModel: com.fabian.todolist.ui.AuthViewModel,
    isLoggedIn: Boolean,
    isGuest: Boolean,
    isAuthLoading: Boolean,
    authError: String?,
    currentUser: OnboardingUserProfile?
) {
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Hero Illustration
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.img_onboarding_pixel_hero),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(240.dp)
                .clip(RoundedCornerShape(32.dp)),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = onboardingString("welcome_title", selectedLang),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-1.5).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = onboardingString("welcome_desc", selectedLang),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- ACCOUNT CONNECTION CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = onboardingString("account_title", selectedLang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = onboardingString("account_desc", selectedLang),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoggedIn && !isGuest && currentUser != null) {
                    // Connected with Google successfully!
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        if (currentUser.photoUrl != null) {
                            androidx.compose.foundation.Image(
                                painter = rememberAsyncImagePainter(currentUser.photoUrl),
                                contentDescription = "Avatar de Google",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = onboardingString("connected_google", selectedLang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentUser.displayName ?: currentUser.email ?: "Usuario",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { authViewModel.signOut() }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Desconectar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    // Google connect button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            authViewModel.signIn(context) {}
                        },
                        enabled = !isAuthLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isAuthLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = onboardingString("connect_google", selectedLang),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    if (authError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = authError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- NAME / NICKNAME SECTION ---
        Text(
            text = onboardingString("name_question", selectedLang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = tempUserName,
            onValueChange = onUserNameChange,
            placeholder = { Text(onboardingString("name_placeholder", selectedLang)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_name_input"),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        // --- LANGUAGE SELECTION ---
        Text(
            text = onboardingString("app_language_title", selectedLang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        val langOptions = listOf(
            Triple("system", onboardingString("system_default", selectedLang), "🌐"),
            Triple("es", "Español", "🇪🇸"),
            Triple("en", "English", "🇺🇸"),
            Triple("fr", "Français", "🇫🇷"),
            Triple("de", "Deutsch", "🇩🇪"),
            Triple("it", "Italiano", "🇮🇹")
        )

        langOptions.forEach { (code, label, flag) ->
            val isSelected = selectedLang == code
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onLangSelected(code) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                         MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                         MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    }
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = flag, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seleccionado",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepVisualExperience(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    selectedAccent: String,
    onAccentSelected: (String) -> Unit,
    selectedLang: String
) {
    val isDark = when (selectedTheme) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val previewAccentColor = when (selectedAccent) {
        "blue" -> Color(0xFF1E88E5)
        "green" -> Color(0xFF0F9D58)
        "coral" -> Color(0xFFEA4335)
        "purple" -> Color(0xFF8B5CF6)
        "gold" -> Color(0xFFE29500)
        "pink" -> Color(0xFFE91E63)
        else -> MaterialTheme.colorScheme.primary
    }

    val previewSurface = if (isDark) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
    val previewOnSurface = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = onboardingString("visual_title", selectedLang),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-1.5).sp
        )
        Text(
            text = onboardingString("visual_desc", selectedLang),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // --- DYNAMIC PIXEL PREVIEW ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = previewAccentColor.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = onboardingString("step_visual", selectedLang).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = previewAccentColor,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = previewSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(previewAccentColor.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = previewAccentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_appearance_preview_task),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = previewOnSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_appearance_preview_task_desc),
                                fontSize = 13.sp,
                                color = previewOnSurface.copy(alpha = 0.7f)
                            )
                        }
                        Surface(
                            color = previewAccentColor,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.priority_high),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDark) Color.Black else Color.White,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = onboardingString("appearance_mode_title", selectedLang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf(
                Triple("system", onboardingString("theme_system", selectedLang), Icons.Rounded.Contrast),
                Triple("light", onboardingString("theme_light", selectedLang), Icons.Rounded.WbSunny),
                Triple("dark", onboardingString("theme_dark", selectedLang), Icons.Rounded.NightsStay)
            )

            themes.forEach { (mode, label, icon) ->
                val isSelected = selectedTheme == mode
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onThemeSelected(mode) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        }
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = onboardingString("color_accent_title", selectedLang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        val colors = listOf(
            "system" to Color(0xFF0061A4),
            "blue" to Color(0xFF1E88E5),
            "green" to Color(0xFF0F9D58),
            "coral" to Color(0xFFEA4335),
            "purple" to Color(0xFF8B5CF6),
            "gold" to Color(0xFFE29500),
            "pink" to Color(0xFFE91E63)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors) { (accentName, colorValue) ->
                val isSelected = selectedAccent == accentName
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAccentSelected(accentName) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            colorValue.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        }
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) colorValue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(colorValue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = onboardingString("color_$accentName", selectedLang),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) colorValue else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepNotificationsAndAlerts(
    notifsEnabled: Boolean,
    onNotifsToggle: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    vibrateEnabled: Boolean,
    onVibrateToggle: (Boolean) -> Unit,
    requestPermission: () -> Unit,
    selectedLang: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = onboardingString("notifs_title", selectedLang),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-1.5).sp
        )
        Text(
            text = onboardingString("notifs_desc", selectedLang),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Large activation banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = onboardingString("notifs_banner_title", selectedLang),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = onboardingString("notifs_banner_desc", selectedLang),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (notifsEnabled) {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            disabledContentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(onboardingString("notifs_enabled_btn", selectedLang), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    Button(
                        onClick = requestPermission,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(onboardingString("notifs_grant_btn", selectedLang), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = onboardingString("notifs_customize", selectedLang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sound Preference Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (soundEnabled) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            onClick = { onSoundToggle(!soundEnabled) }
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    contentDescription = null,
                    tint = if (soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(onboardingString("sound_title", selectedLang), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(onboardingString("sound_desc", selectedLang), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = soundEnabled, onCheckedChange = onSoundToggle)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Vibrate Preference Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (vibrateEnabled) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            onClick = { onVibrateToggle(!vibrateEnabled) }
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    tint = if (vibrateEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(onboardingString("vibrate_title", selectedLang), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(onboardingString("vibrate_desc", selectedLang), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = vibrateEnabled, onCheckedChange = onVibrateToggle)
            }
        }
    }
}

@Composable
fun StepControlHabit(
    selectedInterval: String,
    onIntervalSelected: (String) -> Unit,
    confirmDelete: Boolean,
    onConfirmToggle: (Boolean) -> Unit,
    selectedLang: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = onboardingString("habits_title", selectedLang),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-1.5).sp
        )
        Text(
            text = onboardingString("habits_desc", selectedLang),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = onboardingString("quick_reminders_title", selectedLang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = onboardingString("quick_reminders_desc", selectedLang),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        val intervals = listOf("off", "3h", "6h", "12h", "24h")

        intervals.forEach { code ->
            val isSelected = selectedInterval == code
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onIntervalSelected(code) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = onboardingString("interval_$code", selectedLang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = onboardingString("security_title", selectedLang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (confirmDelete) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            onClick = { onConfirmToggle(!confirmDelete) }
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (confirmDelete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(onboardingString("confirm_delete_title", selectedLang), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(onboardingString("confirm_delete_desc", selectedLang), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = confirmDelete, onCheckedChange = onConfirmToggle)
            }
        }
    }
}

@Composable
fun StepBrainAi(
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    selectedCount: Int,
    onCountSelected: (Int) -> Unit,
    selectedLang: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = onboardingString("ai_title", selectedLang),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-1.5).sp
        )
        Text(
            text = onboardingString("ai_desc", selectedLang),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = onboardingString("ai_model_title", selectedLang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        val models = listOf(
            "gemini-2.5-flash" to "model_flash",
            "gemini-3.1-flash-lite-preview" to "model_lite"
        )

        models.forEach { (code, translationKey) ->
            val isSelected = selectedModel == code
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onModelSelected(code) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = onboardingString(translationKey, selectedLang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (code.contains("flash")) "Velocidad Máxima" else "Eficiencia Inteligente",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Confirmado",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = onboardingString("ai_complexity_title", selectedLang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = onboardingString("ai_complexity_desc", selectedLang),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val stepsCounts = listOf(3, 5, 7)
            stepsCounts.forEach { count ->
                val isSelected = selectedCount == count
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onCountSelected(count) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = onboardingString("subtasks_count_label", selectedLang).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ... (I will add all 12 functions here)
// I will just add the first few as a test.
@Composable
fun StepWelcome(selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.WavingHand, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_welcome", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("¡Bienvenido a tu nuevo organizador!", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun StepAccount(authViewModel: Any, isLoggedIn: Boolean, isGuest: Boolean, isAuthLoading: Boolean, authError: String?, userProfile: Any?, selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_account", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {},
            shape = RectangleShape,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Iniciar sesión con Google")
        }
    }
}

@Composable
fun StepLanguage(selectedLang: String, onLangSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_lang", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        val languages = listOf("es", "en", "fr")
        languages.forEach { lang ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onLangSelected(lang) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedLang == lang, onClick = { onLangSelected(lang) })
                Text(text = lang.uppercase())
            }
        }
    }
}

@Composable
fun StepTheme(
    tempDark: Boolean,
    onThemeSelected: (Boolean) -> Unit,
    tempAccent: String,
    onAccentSelected: (String) -> Unit,
    selectedLang: String
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_visual", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Modo Oscuro")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = tempDark, onCheckedChange = onThemeSelected)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Color de Acento")
        Spacer(modifier = Modifier.height(8.dp))
        val accents = listOf("blue", "green", "rose")
        accents.forEach { accent ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onAccentSelected(accent) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = tempAccent == accent, onClick = { onAccentSelected(accent) })
                Text(text = accent.uppercase())
            }
        }
    }
}

@Composable
fun StepNotifications(tempNotifsEnabled: Boolean, onNotifsToggle: (Boolean) -> Unit, requestPermission: () -> Unit, selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_alerts", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Habilitar Notificaciones")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = tempNotifsEnabled, onCheckedChange = onNotifsToggle)
        }
        Button(onClick = requestPermission) {
            Text("Solicitar Permiso")
        }
    }
}

@Composable
fun StepAlerts(tempSound: Boolean, onSoundToggle: (Boolean) -> Unit, tempVibrate: Boolean, onVibrateToggle: (Boolean) -> Unit, selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Alertas", style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sonido")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = tempSound, onCheckedChange = onSoundToggle)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vibración")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = tempVibrate, onCheckedChange = onVibrateToggle)
        }
    }
}

@Composable
fun StepHabits(tempQuickAddInterval: String, onIntervalSelected: (String) -> Unit, selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_habits", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        val intervals = listOf("off", "3h", "6h", "12h", "24h")
        intervals.forEach { interval ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onIntervalSelected(interval) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = tempQuickAddInterval == interval, onClick = { onIntervalSelected(interval) })
                Text(text = interval)
            }
        }
    }
}

@Composable
fun StepAiModel(tempAiModel: String, onModelSelected: (String) -> Unit, selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_ai", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        val models = listOf("flash", "lite")
        models.forEach { model ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onModelSelected(model) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = tempAiModel == model, onClick = { onModelSelected(model) })
                Text(text = model)
            }
        }
    }
}

@Composable
fun StepAiComplexity(tempAiSubtaskCount: Int, onCountSelected: (Int) -> Unit, selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Complejidad", style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Número de subtareas: $tempAiSubtaskCount")
        Slider(value = tempAiSubtaskCount.toFloat(), onValueChange = { onCountSelected(it.toInt()) }, valueRange = 1f..10f)
    }
}

@Composable
fun StepSecurity(tempConfirmDelete: Boolean, onConfirmToggle: (Boolean) -> Unit, selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_security", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Confirmar eliminación")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = tempConfirmDelete, onCheckedChange = onConfirmToggle)
        }
    }
}

@Composable
fun StepPrivacy(selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.PrivacyTip, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_privacy", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.onboarding_privacy_data_protected))
    }
}

@Composable
fun StepFinish(tempUserName: String, onUserNameChange: (String) -> Unit, selectedLang: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(onboardingString("step_finish", selectedLang), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = tempUserName, onValueChange = onUserNameChange, label = { Text(stringResource(R.string.onboarding_your_name)) })
    }
}
// etc...

/**
 * Look up an onboarding flow string by its logical key.
 *
 * The translations used to live in a ~600-line inline `Map<String, Map<String, String>>`
 * in this file. They have been migrated to `res/values*/strings_onboarding.xml` so
 * that translators can work with standard string resources and Android's locale
 * fallback handles "system" language automatically.
 *
 * `lang` is kept in the signature for backwards compatibility with existing call
 * sites but is no longer used — the active locale is whatever the OS / app has set
 * via AppCompatDelegate.setApplicationLocales(). Pass any value.
 */
@androidx.compose.runtime.Composable
private fun onboardingString(key: String, lang: String = "system"): String {
    return when (key) {
        "welcome_title" -> stringResource(R.string.onboarding_welcome_title)
        "welcome_desc" -> stringResource(R.string.onboarding_welcome_desc)
        "account_title" -> stringResource(R.string.onboarding_account_title)
        "account_desc" -> stringResource(R.string.onboarding_account_desc)
        "connected_google" -> stringResource(R.string.onboarding_connected_google)
        "guest_btn" -> stringResource(R.string.onboarding_guest_btn)
        "google_btn" -> stringResource(R.string.onboarding_google_btn)
        "back_btn" -> stringResource(R.string.onboarding_back_btn)
        "language_title" -> stringResource(R.string.onboarding_language_title)
        "language_desc" -> stringResource(R.string.onboarding_language_desc)
        "appearance_title" -> stringResource(R.string.onboarding_appearance_title)
        "appearance_desc" -> stringResource(R.string.onboarding_appearance_desc)
        "notifications_title" -> stringResource(R.string.onboarding_notifications_title)
        "notifications_desc" -> stringResource(R.string.onboarding_notifications_desc)
        "habits_title" -> stringResource(R.string.onboarding_habits_title)
        "habits_desc" -> stringResource(R.string.onboarding_habits_desc)
        "ai_title" -> stringResource(R.string.onboarding_ai_title)
        "ai_desc" -> stringResource(R.string.onboarding_ai_desc)
        "name_question" -> stringResource(R.string.onboarding_name_question)
        "name_placeholder" -> stringResource(R.string.onboarding_name_placeholder)
        "app_language_title" -> stringResource(R.string.onboarding_app_language_title)
        "system_default" -> stringResource(R.string.onboarding_system_default)
        "notifs_enabled_btn" -> stringResource(R.string.onboarding_notifs_enabled_btn)
        "appearance_mode_title" -> stringResource(R.string.onboarding_appearance_mode_title)
        "color_accent_title" -> stringResource(R.string.onboarding_color_accent_title)
        "next" -> stringResource(R.string.onboarding_next)
        "start" -> stringResource(R.string.onboarding_start)
        "visual_title" -> stringResource(R.string.onboarding_visual_title)
        "visual_desc" -> stringResource(R.string.onboarding_visual_desc)
        "theme_mode" -> stringResource(R.string.onboarding_theme_mode)
        "theme_system" -> stringResource(R.string.onboarding_theme_system)
        "theme_light" -> stringResource(R.string.onboarding_theme_light)
        "theme_dark" -> stringResource(R.string.onboarding_theme_dark)
        "color_accent" -> stringResource(R.string.onboarding_color_accent)
        "color_system" -> stringResource(R.string.onboarding_color_system)
        "color_blue" -> stringResource(R.string.onboarding_color_blue)
        "color_sunset" -> stringResource(R.string.onboarding_color_sunset)
        "color_green" -> stringResource(R.string.onboarding_color_green)
        "color_rose" -> stringResource(R.string.onboarding_color_rose)
        "color_amber" -> stringResource(R.string.onboarding_color_amber)
        "notifs_title" -> stringResource(R.string.onboarding_notifs_title)
        "notifs_desc" -> stringResource(R.string.onboarding_notifs_desc)
        "notifs_banner_title" -> stringResource(R.string.onboarding_notifs_banner_title)
        "notifs_banner_desc" -> stringResource(R.string.onboarding_notifs_banner_desc)
        "notifs_grant_btn" -> stringResource(R.string.onboarding_notifs_grant_btn)
        "notifs_customize" -> stringResource(R.string.onboarding_notifs_customize)
        "sound_title" -> stringResource(R.string.onboarding_sound_title)
        "sound_desc" -> stringResource(R.string.onboarding_sound_desc)
        "vibrate_title" -> stringResource(R.string.onboarding_vibrate_title)
        "vibrate_desc" -> stringResource(R.string.onboarding_vibrate_desc)
        "quick_reminders_title" -> stringResource(R.string.onboarding_quick_reminders_title)
        "quick_reminders_desc" -> stringResource(R.string.onboarding_quick_reminders_desc)
        "interval_off" -> stringResource(R.string.onboarding_interval_off)
        "security_title" -> stringResource(R.string.onboarding_security_title)
        "confirm_delete_title" -> stringResource(R.string.onboarding_confirm_delete_title)
        "confirm_delete_desc" -> stringResource(R.string.onboarding_confirm_delete_desc)
        "ai_model_title" -> stringResource(R.string.onboarding_ai_model_title)
        "model_flash" -> stringResource(R.string.onboarding_model_flash)
        "model_lite" -> stringResource(R.string.onboarding_model_lite)
        "ai_complexity_title" -> stringResource(R.string.onboarding_ai_complexity_title)
        "ai_complexity_desc" -> stringResource(R.string.onboarding_ai_complexity_desc)
        "subtasks_count_label" -> stringResource(R.string.onboarding_subtasks_count_label)
        "of_word" -> stringResource(R.string.onboarding_of_word)
        "all_set" -> stringResource(R.string.onboarding_all_set)
        "next_step" -> stringResource(R.string.onboarding_next_step)
        "start_excl" -> stringResource(R.string.onboarding_start_excl)
        "steps_label" -> stringResource(R.string.onboarding_steps_label)
        "step_welcome" -> stringResource(R.string.onboarding_step_welcome)
        "step_account" -> stringResource(R.string.onboarding_step_account)
        "step_lang" -> stringResource(R.string.onboarding_step_lang)
        "step_visual" -> stringResource(R.string.onboarding_step_visual)
        "step_alerts" -> stringResource(R.string.onboarding_step_alerts)
        "step_habits" -> stringResource(R.string.onboarding_step_habits)
        "step_ai" -> stringResource(R.string.onboarding_step_ai)
        "step_time" -> stringResource(R.string.onboarding_step_time)
        "step_quickadd" -> stringResource(R.string.onboarding_step_quickadd)
        "step_security" -> stringResource(R.string.onboarding_step_security)
        "step_privacy" -> stringResource(R.string.onboarding_step_privacy)
        "step_finish" -> stringResource(R.string.onboarding_step_finish)
        else -> ""
    }
}
