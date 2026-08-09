package com.fabian.todolist.ui.components.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabian.todolist.ui.AuthViewModel
import com.fabian.todolist.ui.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 12

    // Collect settings defaults
    val savedUserName by settingsViewModel.userName.collectAsStateWithLifecycle()
    val savedThemeAccent by settingsViewModel.themeAccent.collectAsStateWithLifecycle()
    val savedThemeDark by settingsViewModel.themeDark.collectAsStateWithLifecycle()
    val savedLanguageCode by settingsViewModel.languageCode.collectAsStateWithLifecycle()
    val savedNotifsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val savedSound by settingsViewModel.notificationsSound.collectAsStateWithLifecycle()
    val savedVibrate by settingsViewModel.notificationsVibrate.collectAsStateWithLifecycle()
    val savedConfirmDelete by settingsViewModel.confirmOnDelete.collectAsStateWithLifecycle()
    val savedQuickAddInterval by settingsViewModel.quickAddNotificationInterval.collectAsStateWithLifecycle()
    val savedAiModel by settingsViewModel.aiModel.collectAsStateWithLifecycle()
    val savedAiSubtaskCount by settingsViewModel.aiSubtaskCount.collectAsStateWithLifecycle()

    // Temporary step states
    var tempUserName by remember { mutableStateOf(savedUserName.ifBlank { "" }) }
    var tempAccent by remember { mutableStateOf(savedThemeAccent) }
    var tempDark by remember { mutableStateOf(savedThemeDark) }
    var tempLang by remember { mutableStateOf(savedLanguageCode) }
    var tempNotifsEnabled by remember { mutableStateOf(savedNotifsEnabled) }
    var tempSound by remember { mutableStateOf(savedSound) }
    var tempVibrate by remember { mutableStateOf(savedVibrate) }
    var tempConfirmDelete by remember { mutableStateOf(savedConfirmDelete) }
    var tempQuickAddInterval by remember { mutableStateOf(savedQuickAddInterval) }
    var tempAiModel by remember { mutableStateOf(savedAiModel) }
    var tempAiSubtaskCount by remember { mutableIntStateOf(savedAiSubtaskCount) }

    // Auth states
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isGuest by authViewModel.isGuest.collectAsStateWithLifecycle()
    val isAuthLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val authError by authViewModel.error.collectAsStateWithLifecycle()

    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val userProfile = remember(firebaseUser) {
        firebaseUser?.let { fb ->
            OnboardingUserProfile(fb.displayName, fb.email, fb.photoUrl?.toString())
        }
    }

    // Permission Launcher for Notifications
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        tempNotifsEnabled = isGranted
        settingsViewModel.setNotificationsEnabled(isGranted)
    }

    fun saveAndFinishAll() {
        settingsViewModel.setUserName(tempUserName.trim().ifBlank { "Amigo" })
        settingsViewModel.setThemeAccent(tempAccent)
        settingsViewModel.setThemeDark(tempDark)
        settingsViewModel.setLanguageCode(tempLang)
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Header with Progress & Title
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = totalSteps,
                selectedLang = tempLang,
                onBackClick = {
                    if (currentStep > 1) currentStep--
                }
            )

            // Step Body with Smooth Animated Transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300))) togetherWith
                                    (slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300))) togetherWith
                                    (slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
                        }
                    },
                    label = "StepContentAnimation"
                ) { step ->
                    when (step) {
                        1 -> StepWelcome(
                            userName = tempUserName,
                            onUserNameChange = { tempUserName = it },
                            selectedLang = tempLang
                        )
                        2 -> StepAccount(
                            authViewModel = authViewModel,
                            isLoggedIn = isLoggedIn,
                            isGuest = isGuest,
                            isAuthLoading = isAuthLoading,
                            authError = authError,
                            userProfile = userProfile,
                            selectedLang = tempLang
                        )
                        3 -> StepLanguage(
                            selectedLang = tempLang,
                            onLangSelected = {
                                tempLang = it
                                settingsViewModel.setLanguageCode(it)
                            }
                        )
                        4 -> StepTheme(
                            tempDark = tempDark,
                            onThemeSelected = { tempDark = it },
                            tempAccent = tempAccent,
                            onAccentSelected = { tempAccent = it },
                            selectedLang = tempLang
                        )
                        5 -> StepNotifications(
                            tempNotifsEnabled = tempNotifsEnabled,
                            onNotifsToggle = { tempNotifsEnabled = it },
                            requestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    tempNotifsEnabled = true
                                }
                            },
                            selectedLang = tempLang
                        )
                        6 -> StepAlerts(
                            tempSound = tempSound,
                            onSoundToggle = { tempSound = it },
                            tempVibrate = tempVibrate,
                            onVibrateToggle = { tempVibrate = it },
                            selectedLang = tempLang
                        )
                        7 -> StepHabits(
                            tempQuickAddInterval = tempQuickAddInterval,
                            onIntervalSelected = { tempQuickAddInterval = it },
                            selectedLang = tempLang
                        )
                        8 -> StepAiModel(
                            tempAiModel = tempAiModel,
                            onModelSelected = { tempAiModel = it },
                            selectedLang = tempLang
                        )
                        9 -> StepAiComplexity(
                            tempAiSubtaskCount = tempAiSubtaskCount,
                            onCountSelected = { tempAiSubtaskCount = it },
                            selectedLang = tempLang
                        )
                        10 -> StepSecurity(
                            tempConfirmDelete = tempConfirmDelete,
                            onConfirmToggle = { tempConfirmDelete = it },
                            selectedLang = tempLang
                        )
                        11 -> StepPrivacy(selectedLang = tempLang)
                        12 -> StepFinish(
                            tempUserName = tempUserName,
                            selectedLang = tempLang
                        )
                    }
                }
            }

            // Bottom Navigation Footer
            OnboardingFooterNavigation(
                currentStep = currentStep,
                totalSteps = totalSteps,
                selectedLang = tempLang,
                onBackClick = {
                    if (currentStep > 1) currentStep--
                },
                onNextClick = {
                    if (currentStep < totalSteps) {
                        currentStep++
                    } else {
                        saveAndFinishAll()
                    }
                }
            )
        }
    }
}

/**
 * Backwards compatible dialog wrapper for OnboardingScreen.
 */
@Composable
fun OnboardingDialog(
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    onFinished: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Modal: require completion */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            OnboardingScreen(
                settingsViewModel = settingsViewModel,
                authViewModel = authViewModel,
                onFinished = onFinished
            )
        }
    }
}
