package com.fabian.todolist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabian.todolist.ui.components.auth.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToGoogle: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (!isLoading) {
            AmbientBackground()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            LogoSegment(isLoading)

            Spacer(modifier = Modifier.height(44.dp))
            
            LoginHeader()
            
            error?.let { LoginErrorCard(it) }

            Spacer(modifier = Modifier.height(56.dp))

            LoginButtonSection(
                isLoading = isLoading,
                onGuestClick = { viewModel.continueAsGuest(onLoginSuccess) },
                onGoogleClick = onNavigateToGoogle
            )

            Spacer(modifier = Modifier.height(24.dp))

            LoginFooter()

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
