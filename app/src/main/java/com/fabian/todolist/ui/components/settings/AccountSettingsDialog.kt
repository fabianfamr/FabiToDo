package com.fabian.todolist.ui.components.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.fabian.todolist.R
import com.fabian.todolist.data.AuthManager
import com.fabian.todolist.ui.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.fabian.todolist.ui.TaskViewModel
import com.fabian.todolist.ui.SettingsViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsDialog(
    authViewModel: AuthViewModel,
    viewModel: TaskViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToGoogleLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val unsyncedCount by viewModel.unsyncedTasksCount.collectAsStateWithLifecycle()
            val authManager = remember { AuthManager(context) }
            val user = authManager.getCurrentUser()
            val isGuest = authManager.isGuestUser()
            
            var isSyncingByHand by remember { mutableStateOf(false) }

            SettingsSubmenuScaffold(
                title = stringResource(R.string.settings_account_header),
                onBack = onDismiss
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                        // Title Visual Segments
                        item {
                            Column {
                                Text(
                                    text = if (user != null) stringResource(R.string.settings_account_data_protected) else stringResource(R.string.settings_account_register),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = if (user != null) {
                                        stringResource(R.string.settings_account_backed_up_desc)
                                    } else {
                                        stringResource(R.string.settings_account_register_desc)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Connected User card OR Guest overview layout
                        if (user != null) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(26.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(22.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Avatar frame with green pulse status
                                            Box(
                                                modifier = Modifier.size(64.dp),
                                                contentAlignment = Alignment.BottomEnd
                                            ) {
                                                // Profile Image
                                                if (user.photoUrl != null) {
                                                    androidx.compose.foundation.Image(
                                                        painter = rememberAsyncImagePainter(user.photoUrl),
                                                        contentDescription = "User photo",
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    val letter = (user.displayName ?: user.email ?: "?")
                                                        .take(1)
                                                        .uppercase()
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape)
                                                            .background(
                                                                Brush.linearGradient(
                                                                    colors = listOf(
                                                                        MaterialTheme.colorScheme.primary,
                                                                        MaterialTheme.colorScheme.tertiary
                                                                    )
                                                                )
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = letter,
                                                            style = MaterialTheme.typography.headlineSmall,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                // Clean pulse dot indicator
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.background)
                                                        .padding(2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF34A853)) // Google healthy green
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = user.displayName ?: stringResource(R.string.settings_account_google_user_placeholder),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = user.email ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                                        Spacer(modifier = Modifier.height(18.dp))

                                        // Status Details
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE6F4EA)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF137333),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.settings_account_sync_active),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(R.string.settings_account_sync_active_desc),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Interactive force-sync button
                                        Button(
                                            onClick = {
                                                if (!isSyncingByHand) {
                                                    coroutineScope.launch {
                                                        isSyncingByHand = true
                                                        delay(1200)
                                                        isSyncingByHand = false
                                                        viewModel.markAllSynced()
                                                        val successMsg = context.getString(R.string.toast_manual_sync_success)
                                                        Toast.makeText(
                                                            context,
                                                            successMsg,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            enabled = !isSyncingByHand && unsyncedCount > 0
                                        ) {
                                            if (isSyncingByHand) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(R.string.syncing), fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Rounded.Sync,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                val syncText = if (unsyncedCount > 0) {
                                                    stringResource(R.string.sync_now)
                                                } else {
                                                    stringResource(R.string.status_synced)
                                                }
                                                Text(syncText, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        // Auto-sync switch
                                        val prefs = context.getSharedPreferences("fabitodo_preferences", android.content.Context.MODE_PRIVATE)
                                        var isAutoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("auto_sync_enabled", true)) }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.settings_sync_auto),
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Switch(
                                                checked = isAutoSyncEnabled,
                                                onCheckedChange = {
                                                    isAutoSyncEnabled = it
                                                    prefs.edit().putBoolean("auto_sync_enabled", it).apply()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Guest state user with beautiful benefits checklist cards
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    // 1. Storage security
                                    BenefitItemCard(
                                        title = stringResource(R.string.benefit_cloud_access),
                                        description = stringResource(R.string.benefit_cloud_access_desc),
                                        icon = Icons.Rounded.Security,
                                        iconColor = MaterialTheme.colorScheme.primary
                                    )

                                    // 2. Multidevice Sync
                                    BenefitItemCard(
                                        title = stringResource(R.string.benefit_multidevice),
                                        description = stringResource(R.string.benefit_multidevice_desc),
                                        icon = Icons.Rounded.Phonelink,
                                        iconColor = MaterialTheme.colorScheme.secondary
                                    )

                                    // 3. Automated Backup
                                    BenefitItemCard(
                                        title = stringResource(R.string.benefit_bg_backup),
                                        description = stringResource(R.string.benefit_bg_backup_desc),
                                        icon = Icons.Rounded.CloudDone,
                                        iconColor = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                val isLoadingAuth by authViewModel.isLoading.collectAsStateWithLifecycle()
                                val authError by authViewModel.error.collectAsStateWithLifecycle()

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onNavigateToGoogleLogin()
                                        },
                                        enabled = !isLoadingAuth,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        if (isLoadingAuth) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.5.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.AccountCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = stringResource(R.string.settings_account_login),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    authError?.let { err ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = err,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Close session block (only for logged-in accounts)
                        if (!isGuest && user != null) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.danger_zone),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        authViewModel.signOut()
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_account_logout),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(30.dp))
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun BenefitItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
