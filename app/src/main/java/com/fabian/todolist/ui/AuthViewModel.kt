package com.fabian.todolist.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fabian.todolist.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val firestoreSyncManager: com.fabian.todolist.data.sync.FirestoreSyncManager
) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(application)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val syncStatus = firestoreSyncManager.syncStatus
    val lastSyncTimestamp = firestoreSyncManager.lastSyncTimestamp

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        if (user != null) {
            firestoreSyncManager.startRealtimeSync(user.uid)
        } else {
            firestoreSyncManager.stopRealtimeSync()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        val user = auth.currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    user.reload().await()
                    _currentUser.value = auth.currentUser
                } catch (_: Exception) {}
                firestoreSyncManager.startRealtimeSync(user.uid)
            }
        }
    }

    fun signInWithGoogle(context: Context, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                val serverClientId = if (resId != 0) {
                    context.getString(resId)
                } else {
                    "14026788849-6o166494a1rsfqj0ed5lm849upa9l2ds.apps.googleusercontent.com"
                }
                val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    _currentUser.value = authResult.user
                    onResult(true, null)
                } else {
                    onResult(false, getApplication<Application>().getString(R.string.auth_error_unsupported_credential))
                }
            } catch (e: GetCredentialCancellationException) {
                Log.d("AuthViewModel", "Sign-in cancelled by user")
                onResult(false, null)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In failed", e)
                onResult(false, e.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        firestoreSyncManager.stopRealtimeSync()
        val prefs = context.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("last_firestore_sync_time")
            .remove("last_settings_cloud_sync")
            .remove("last_settings_local_update")
            // CORRECCIÓN: clave nueva del sincronizador (desfase reloj local vs servidor);
            // se recalcula sola en la próxima sincronización.
            .remove("settings_clock_offset")
            .remove("user_display_name")
            .apply()

        viewModelScope.launch {
            try {
                auth.signOut()
                _currentUser.value = null
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign-out error", e)
            } finally {
                onComplete()
            }
        }
    }

    fun triggerSync(onComplete: (Boolean) -> Unit = {}) {
        val user = auth.currentUser
        if (user == null) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            try {
                user.reload().await()
                _currentUser.value = auth.currentUser
            } catch (_: Exception) {}
            val success = firestoreSyncManager.syncAllNow(user.uid)
            onComplete(success)
        }
    }

    fun completeWelcome(onComplete: () -> Unit = {}) {
        onComplete()
    }

    override fun onCleared() {
        super.onCleared()
        firestoreSyncManager.stopRealtimeSync()
        auth.removeAuthStateListener(authStateListener)
    }
}
