package com.fabian.todolist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fabian.todolist.data.AuthManager
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.fabian.todolist.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application)
    
    private val _isLoggedIn = MutableStateFlow(authManager.isUserLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _isGuest = MutableStateFlow(authManager.isGuestUser())
    val isGuest = _isGuest.asStateFlow()

    private val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
        _isGuest.value = authManager.isGuestUser()
        _isLoggedIn.value = firebaseAuth.currentUser != null || authManager.isGuestUser()
    }

    init {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Firebase Auth initialization failed", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun signIn(context: android.content.Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authManager.signInWithGoogle(context)
            if (result.isSuccess) {
                authManager.setGuestUser(false)
                _isGuest.value = false
                _isLoggedIn.value = true
                onSuccess()
            } else {
                val exception = result.exceptionOrNull()
                val message = exception?.message ?: ""
                if (exception is GetCredentialException && message.contains("cancel", ignoreCase = true)) {
                    _error.value = getApplication<Application>().getString(R.string.error_login_canceled)
                } else {
                    _error.value = exception?.message ?: getApplication<Application>().getString(R.string.error_login_canceled)
                }
            }
            _isLoading.value = false
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        authManager.setGuestUser(true)
        _isGuest.value = true
        _isLoggedIn.value = true
        onSuccess()
    }

    fun signOut() {
        authManager.signOut()
        _isGuest.value = false
        _isLoggedIn.value = false
    }
}
