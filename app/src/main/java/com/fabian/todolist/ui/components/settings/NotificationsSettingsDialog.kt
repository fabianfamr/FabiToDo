package com.fabian.todolist.ui.components.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabian.todolist.MainActivity
import com.fabian.todolist.R
import com.fabian.todolist.ui.TaskViewModel
import com.fabian.todolist.ui.SettingsViewModel

    private fun triggerTestNotification(context: Context, soundEnabled: Boolean, vibrateEnabled: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "fabitodo_reminders_${soundEnabled}_${vibrateEnabled}"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                enableVibration(vibrateEnabled)
                if (vibrateEnabled) {
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                }
                if (!soundEnabled) {
                    setSound(null, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra("NOTIFICATION_TASK_ID", -99) // test ID
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        -99,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification_checkmark)
        .setContentTitle(context.getString(R.string.notification_test_title))
        .setContentText(context.getString(R.string.notification_test_content))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

    if (vibrateEnabled) {
        notificationBuilder.setVibrate(longArrayOf(0, 500, 250, 500))
    } else {
        notificationBuilder.setVibrate(null)
    }

    notificationManager.notify(-99, notificationBuilder.build())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            val context = LocalContext.current
            val globalNotificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
            val quickAddNotificationEnabled by viewModel.quickAddNotificationEnabled.collectAsStateWithLifecycle()
            val quickAddNotificationInterval by viewModel.quickAddNotificationInterval.collectAsStateWithLifecycle()
            val quickAddQuietHoursEnabled by viewModel.quickAddQuietHoursEnabled.collectAsStateWithLifecycle()
            val notificationsSoundEnabled by viewModel.notificationsSound.collectAsStateWithLifecycle()
            val notificationsVibrateEnabled by viewModel.notificationsVibrate.collectAsStateWithLifecycle()
            val currentOffsetMinutes by viewModel.reminderOffsetMinutes.collectAsStateWithLifecycle()
            val currentSnoozeMinutes by viewModel.snoozeDurationMinutes.collectAsStateWithLifecycle()

            var hasPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                )
            }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasPermission = isGranted
                if (!isGranted) {
                    Toast.makeText(context, context.getString(R.string.toast_permission_denied), Toast.LENGTH_LONG).show()
                } else {
                    viewModel.setNotificationsEnabled(true)
                    Toast.makeText(context, context.getString(R.string.toast_permission_granted), Toast.LENGTH_SHORT).show()
                }
            }

            SettingsSubmenuScaffold(
                title = stringResource(R.string.settings_alerts_title),
                onBack = onDismiss
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header visual segment
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.settings_alerts_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.settings_notifications_desc_detailed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                        }
                    }

                    // Global Switch Card
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (globalNotificationsEnabled) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                }
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (globalNotificationsEnabled) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (globalNotificationsEnabled) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.NotificationsActive,
                                                contentDescription = null,
                                                tint = if (globalNotificationsEnabled) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.settings_alerts_global),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.settings_notifications_auto_desc),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = globalNotificationsEnabled,
                                        onCheckedChange = { isEnabled ->
                                            if (isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                                                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                viewModel.setNotificationsEnabled(isEnabled)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (hasPermission) {
                                        stringResource(R.string.notifications_permission_granted)
                                    } else {
                                        stringResource(R.string.no_active_reminders_permission)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasPermission) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (!globalNotificationsEnabled) {
                                            Toast.makeText(context, context.getString(R.string.toast_test_notification_req), Toast.LENGTH_SHORT).show()
                                        } else {
                                            triggerTestNotification(context, notificationsSoundEnabled, notificationsVibrateEnabled)
                                            Toast.makeText(context, context.getString(R.string.toast_test_notification_sent), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_alerts_test_btn),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Sound, Vibration, Offset Options with Smooth Animation FadeIn
                    item {
                        AnimatedVisibility(
                            visible = globalNotificationsEnabled,
                            enter = fadeIn(animationSpec = tween(250)) + expandVertically(),
                            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

                                // Sound and Vibration Card
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Sound Alert Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { viewModel.setNotificationsSound(!notificationsSoundEnabled) }
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.settings_notifications_sound_title),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(R.string.settings_notifications_sound_desc),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                            Switch(
                                                checked = notificationsSoundEnabled,
                                                onCheckedChange = { viewModel.setNotificationsSound(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            )
                                        }

                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                                        )

                                        // Vibration Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { viewModel.setNotificationsVibrate(!notificationsVibrateEnabled) }
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                     .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Vibration,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.settings_notifications_vibrate_title),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(R.string.settings_notifications_vibrate_desc),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                            Switch(
                                                checked = notificationsVibrateEnabled,
                                                onCheckedChange = { viewModel.setNotificationsVibrate(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            )
                                        }
                                    }
                                }

                                // Quick Add Interval Section
                                FormatSectionCard(
                                    title = stringResource(R.string.settings_quick_add_interval_title),
                                    description = stringResource(R.string.settings_quick_add_interval_desc),
                                    icon = Icons.Rounded.AddAlert,
                                    options = listOf(
                                        FormatOption("off", stringResource(R.string.settings_quick_add_off)),
                                        FormatOption("1h", stringResource(R.string.settings_quick_add_1h)),
                                        FormatOption("2h", stringResource(R.string.settings_quick_add_2h)),
                                        FormatOption("6h", stringResource(R.string.settings_quick_add_6h)),
                                        FormatOption("12h", stringResource(R.string.settings_quick_add_12h)),
                                        FormatOption("24h", stringResource(R.string.settings_quick_add_24h))
                                    ),
                                    selectedOption = quickAddNotificationInterval,
                                    onOptionSelected = { interval ->
                                        viewModel.setQuickAddNotificationInterval(interval)
                                    }
                                )

                                if (quickAddNotificationInterval != "off") {
                                    Card(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { viewModel.setQuickAddQuietHoursEnabled(!quickAddQuietHoursEnabled) }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.AccessTime,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Horas de Silencio",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "No sugerir de 10 PM a 8 AM",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                            Switch(
                                                checked = quickAddQuietHoursEnabled,
                                                onCheckedChange = { viewModel.setQuickAddQuietHoursEnabled(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            )
                                        }
                                    }
                                }

                                // Interactive Reminder Offset Selection Section removed and moved to fixed values.
                                // Snooze Duration Selection Section
                                FormatSectionCard(
                                    title = stringResource(R.string.settings_snooze_title),
                                    description = stringResource(R.string.settings_snooze_desc),
                                    icon = Icons.Rounded.Snooze,
                                    options = listOf(
                                        FormatOption("5", stringResource(R.string.settings_snooze_5)),
                                        FormatOption("10", stringResource(R.string.settings_snooze_10)),
                                        FormatOption("15", stringResource(R.string.settings_snooze_15)),
                                        FormatOption("30", stringResource(R.string.settings_snooze_30))
                                    ),
                                    selectedOption = currentSnoozeMinutes.toString(),
                                    onOptionSelected = { snoozeStr ->
                                        snoozeStr.toIntOrNull()?.let { viewModel.setSnoozeDuration(it) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
