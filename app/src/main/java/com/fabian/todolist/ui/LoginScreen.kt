package com.fabian.todolist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fabian.todolist.ui.components.auth.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToGoogle: () -> Unit = onLoginSuccess
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AmbientBackground()

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

            LogoSegment(isLoading = false)

            Spacer(modifier = Modifier.height(44.dp))
            
            LoginHeader()

            Spacer(modifier = Modifier.height(48.dp))

            LoginButtonSection(
                isLoading = false,
                onStartClick = { viewModel.completeWelcome(onLoginSuccess) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            LoginFooter()

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
