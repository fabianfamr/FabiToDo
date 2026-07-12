package com.fabian.todolist

import android.os.Bundle
import android.os.Build
import android.content.Intent
import com.fabian.todolist.data.Task
import java.util.UUID
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.fabian.todolist.ui.TaskScreen
import com.fabian.todolist.ui.TaskViewModel
import com.fabian.todolist.ui.SettingsViewModel
import com.fabian.todolist.ui.theme.FabiToDoTheme

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fabian.todolist.ui.LoginScreen
import com.fabian.todolist.ui.AuthViewModel
import com.fabian.todolist.ui.GoogleLoginScreen

import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  private var initialAppliedLanguage: String = "system"

  private fun Context.findActivity(): androidx.appcompat.app.AppCompatActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
      if (currentContext is androidx.appcompat.app.AppCompatActivity) {
        return currentContext
      }
      currentContext = currentContext.baseContext
    }
    return null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    // Apply language on startup using modern AppCompat API
    val prefs = getSharedPreferences("fabitodo_preferences", MODE_PRIVATE)
    val savedLang = prefs.getString("app_locale", "system") ?: "system"
    initialAppliedLanguage = savedLang
    
    if (savedLang != "system") {
      try {
        val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags(savedLang)
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
      } catch (e: Exception) {
        android.util.Log.e("MainActivity", "Error setting locale using AppCompatDelegate", e)
      }
    }

    super.onCreate(savedInstanceState)
    
    val quickAddInterval = prefs.getString("quick_add_notification_interval", "off") ?: "off"
    if (quickAddInterval != "off") {
      com.fabian.todolist.util.QuickAddNotificationHelper.schedulePeriodicReminders(this, quickAddInterval)
    }

    enableEdgeToEdge()
    setContent {
      val viewModel: TaskViewModel = hiltViewModel()
      val settingsViewModel: SettingsViewModel = hiltViewModel()
      
      val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
      androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                viewModel.onAppForegroundStateChanged(true)
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.onAppForegroundStateChanged(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
      }

      // Handle intent if it carries shared text or a notification tap.
      // We wrap the intent in a MutableState so onNewIntent can re-trigger the
      // LaunchedEffect — previously the effect was keyed on the activity's
      // intent property which Compose does not observe.
      val intentState = remember { androidx.compose.runtime.mutableStateOf(intent) }
      currentIntentHolder = intentState
      androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { currentIntentHolder = null }
      }
      val currentIntent by intentState
      LaunchedEffect(currentIntent) {
        handleIntent(currentIntent, viewModel)
      }
      val accentColor by settingsViewModel.themeAccent.collectAsStateWithLifecycle()
      val themeDark by settingsViewModel.themeDark.collectAsStateWithLifecycle()

      FabiToDoTheme(
        selectedColor = accentColor,
        selectedDarkTheme = themeDark
      ) {
        val authViewModel: AuthViewModel = hiltViewModel()
        val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
        val isGuest by authViewModel.isGuest.collectAsStateWithLifecycle()
        val onboardingCompleted by settingsViewModel.onboardingCompleted.collectAsStateWithLifecycle()
        val navController = rememberNavController()

        val transition = remember { com.fabian.todolist.ui.PixelTransitionState() }
        LaunchedEffect(Unit) {
          transition.start()
        }

        val currentBackStackEntry by navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(initialValue = null)
        val currentRoute = currentBackStackEntry?.destination?.route

        LaunchedEffect(isLoggedIn, isGuest, currentRoute, onboardingCompleted) {
          if (currentRoute != null) {
            val shouldRedirectToHome = if (currentRoute == "google_login") {
              isLoggedIn && !isGuest
            } else {
              isLoggedIn
            }

            if (shouldRedirectToHome && (currentRoute == "login" || (currentRoute == "google_login" && !isGuest))) {
              navController.navigate("home") {
                popUpTo("login") { inclusive = true }
                popUpTo("google_login") { inclusive = true }
              }
            } else if (!isLoggedIn && currentRoute == "home" && onboardingCompleted) {
              navController.navigate("login") {
                popUpTo(0) { inclusive = true }
              }
            }
          }
        }

        val startDestination = remember(onboardingCompleted, isLoggedIn) {
          if (!onboardingCompleted || isLoggedIn) "home" else "login"
        }
        val appContent: @Composable () -> Unit = {
          NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
          ) {
            composable("login") {
              LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                  if (navController.currentDestination?.route == "login") {
                    navController.navigate("home") {
                      popUpTo("login") { inclusive = true }
                    }
                  }
                },
                onNavigateToGoogle = {
                  navController.navigate("google_login")
                }
              )
            }
            composable("google_login") {
              GoogleLoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                  if (navController.currentDestination?.route == "google_login") {
                    if (!navController.popBackStack("home", false)) {
                      navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                      }
                    }
                  }
                },
                onBack = { navController.popBackStack() }
              )
            }
            composable("home") {
              TaskScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                authViewModel = authViewModel,
                onNavigateToGoogleLogin = { navController.navigate("google_login") },
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        }

        Box(modifier = Modifier.fillMaxSize()) {
          val isRunning = transition.running.value

          Box(
            modifier = Modifier
              .fillMaxSize()
              .then(
                if (isRunning) {
                  Modifier.graphicsLayer {
                    val t = transition.progress.value
                    alpha = 1f // Solid reveal behind the dissolving black splash to avoid any transparency gaps
                    scaleX = com.fabian.todolist.ui.lerp(0.99f, 1f, t)
                    scaleY = com.fabian.todolist.ui.lerp(0.99f, 1f, t)
                  }
                } else Modifier
              )
              .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
          ) {
            val requireBiometrics by settingsViewModel.requireBiometrics.collectAsStateWithLifecycle()
            com.fabian.todolist.ui.components.BiometricAuthOverlay(requireBiometrics = requireBiometrics) {
              appContent()
            }
          }

          if (isRunning) {
            com.fabian.todolist.ui.PixelSplashScene(transition.progress.value)
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    // Forward the new intent to the Composable layer so LaunchedEffect
    // re-fires. Without this, ACTION_SEND / ACTION_ADD_TASK arriving while
    // the app is already open were silently dropped.
    currentIntentHolder?.value = intent
  }

  // Holds a reference to the Compose-side MutableState<Intent> so onNewIntent
  // can update it. Set in setContent, cleared on dispose.
  private var currentIntentHolder: androidx.compose.runtime.MutableState<Intent?>? = null

  private fun handleIntent(intent: Intent?, viewModel: TaskViewModel? = null) {
    if (intent == null) return

    if (intent.action == "com.fabian.todolist.ACTION_ADD_TASK") {
      viewModel?.setShouldShowAddDialog(true)
      intent.action = null
      return
    }

    if (intent.action == Intent.ACTION_VIEW) {
      val title = intent.getStringExtra("title")
      val description = intent.getStringExtra("description")

      if (!title.isNullOrEmpty()) {
         viewModel?.insertTask(
           Task(
             title = title,
             description = description ?: "",
             category = getString(R.string.category_general)
           )
         )
         intent.action = null
         return
      }
    }

    if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
      intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
        viewModel?.insertTask(
          Task(
            title = sharedText.take(50),
            description = if (sharedText.length > 50) sharedText else "",
            category = getString(R.string.category_general)
          )
        )
      }
      // Consume the intent action so it doesn't get processed again on recomposition/restart
      intent.action = null
      return
    }

    // Tapping a reminder notification opens MainActivity with NOTIFICATION_TASK_ID extra.
    // Surface the requested task id to the ViewModel so the UI can scroll to it / open edit.
    val notificationTaskId = intent.getIntExtra("NOTIFICATION_TASK_ID", -1)
    if (notificationTaskId != -1) {
      viewModel?.setPendingTaskIdFromNotification(notificationTaskId)
      intent.removeExtra("NOTIFICATION_TASK_ID")
    }
  }
}
