package com.fabian.todolist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.content.ContextWrapper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fabian.todolist.R

@Composable
fun BiometricAuthOverlay(
    requireBiometrics: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isAuthenticated by remember(requireBiometrics) { mutableStateOf(!requireBiometrics) }
    var attemptingAuth by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-lock when the app goes to the background. Without this, the user can
    // authenticate, switch to another app, then return minutes later and the
    // activity is merely resumed (not recreated) — isAuthenticated stays true
    // and the biometric lock is silently bypassed.
    DisposableEffect(lifecycleOwner, requireBiometrics) {
        val observer = LifecycleEventObserver { _, event ->
            if (requireBiometrics && event == Lifecycle.Event.ON_STOP) {
                isAuthenticated = false
                attemptingAuth = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (isAuthenticated) {
        content()
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_locked_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.app_locked_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = {
                    attemptingAuth = true
                }) {
                    Text(stringResource(R.string.unlock_button))
                }
            }
        }
    }

    LaunchedEffect(requireBiometrics, attemptingAuth) {
        if (requireBiometrics && (!isAuthenticated || attemptingAuth)) {
            var fragmentActivity: FragmentActivity? = null
            var currentContext = context
            while (currentContext is ContextWrapper) {
                if (currentContext is FragmentActivity) {
                    fragmentActivity = currentContext
                    break
                }
                currentContext = currentContext.baseContext
            }

            if (fragmentActivity != null) {
                val executor = ContextCompat.getMainExecutor(fragmentActivity)
                val biometricPromptTitle = fragmentActivity.getString(R.string.biometric_prompt_title)
                val biometricPromptSubtitle = fragmentActivity.getString(R.string.biometric_prompt_subtitle)
                val biometricPrompt = BiometricPrompt(
                    fragmentActivity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            attemptingAuth = false
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            isAuthenticated = true
                            attemptingAuth = false
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            attemptingAuth = false
                        }
                    }
                )

                // Allow BIOMETRIC_WEAK or DEVICE_CREDENTIAL so users without enrolled
                // biometrics can still unlock with their device PIN/pattern/password.
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(biometricPromptTitle)
                    .setSubtitle(biometricPromptSubtitle)
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()

                biometricPrompt.authenticate(promptInfo)
            } else {
                // Safe fallback: STAY LOCKED. Previously this branch auto-granted
                // access, defeating the entire purpose of the biometric lock.
                android.util.Log.e(
                    "BiometricAuthOverlay",
                    "FragmentActivity not found; staying locked."
                )
                attemptingAuth = false
            }
            attemptingAuth = false
        }
    }
}
