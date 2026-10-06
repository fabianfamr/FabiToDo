package com.fabian.todolist.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.fabian.todolist.data.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application)

    private val _welcomeSeen = MutableStateFlow(authManager.isWelcomeSeen())
    val welcomeSeen = _welcomeSeen.asStateFlow()

    fun completeWelcome(onSuccess: () -> Unit) {
        authManager.setWelcomeSeen(true)
        _welcomeSeen.value = true
        onSuccess()
    }
}
