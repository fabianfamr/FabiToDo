package com.fabian.todolist.ui.components.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.BuildCircle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabian.todolist.R
import com.fabian.todolist.ui.TaskViewModel
import com.fabian.todolist.ui.SettingsViewModel
import com.fabian.todolist.util.BackupImportResult
import kotlinx.coroutines.launch

@Composable
fun BackupSettingsDialog(
    viewModel: TaskViewModel,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val showBackupCard by settingsViewModel.showBackupCard.collectAsStateWithLifecycle()
    val encryptBackups by settingsViewModel.encryptBackups.collectAsStateWithLifecycle()
    val storageStats by settingsViewModel.storageStats.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedExportFormat by rememberSaveable { mutableStateOf("json") }
    var isFormatMenuExpanded by remember { mutableStateOf(false) }

    var showExportPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var pendingExportPassphrase by rememberSaveable { mutableStateOf<String?>(null) }

    var showImportPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var pendingImportUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var importPasswordWrongError by rememberSaveable { mutableStateOf(false) }

    // Launcher for exporting backup
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val passphraseToUse = pendingExportPassphrase
        pendingExportPassphrase = null
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val liveTasks = viewModel.getAllTasksForBackup()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val categoriesList = settingsViewModel.categories.value
                        val success = viewModel.exportBackupToStream(
                            outputStream = outputStream,
                            tasks = liveTasks,
                            customCategories = categoriesList,
                            passphrase = passphraseToUse
                        )
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.toast_backup_export_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.toast_backup_export_error, context.getString(R.string.toast_write_error)), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.toast_backup_export_error, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher for importing backup
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val result = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        viewModel.importBackupFromStream(inputStream, passphrase = null) { importedCats ->
                            settingsViewModel.saveCategories(importedCats)
                        }
                    } ?: BackupImportResult.InvalidFile

                    when (result) {
                        BackupImportResult.Success -> {
                            Toast.makeText(context, context.getString(R.string.toast_backup_import_success), Toast.LENGTH_SHORT).show()
                        }
                        BackupImportResult.RequiresPassword -> {
                            pendingImportUriString = uri.toString()
                            importPasswordWrongError = false
                            showImportPasswordDialog = true
                        }
                        BackupImportResult.WrongPassword -> {
                            Toast.makeText(context, context.getString(R.string.backup_password_wrong_error), Toast.LENGTH_SHORT).show()
                        }
                        BackupImportResult.InvalidFile -> {
                            Toast.makeText(context, context.getString(R.string.toast_backup_import_invalid), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.toast_backup_import_error, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SettingsSubmenuScaffold(
        title = stringResource(R.string.backup_restore_title),
        onBack = onDismiss
    ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header card instruction
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.backup_restore_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.settings_backup_desc_detailed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = showBackupCard,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(18.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudSync,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.backup_how_works_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.backup_how_works_desc),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { settingsViewModel.setShowBackupCard(false) },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.08f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.understood),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Local Backup Box
                    item {
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Backup,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.backup_create_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.settings_backup_local_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.backup_export_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                // Format Selector Dropdown
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.backup_export_format_label),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Box {
                                            FilledTonalButton(
                                                onClick = { isFormatMenuExpanded = true },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(38.dp)
                                            ) {
                                                Text(
                                                    text = if (selectedExportFormat == "fabitodo") ".fabitodo" else ".json",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = isFormatMenuExpanded,
                                                onDismissRequest = { isFormatMenuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(".json", fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(stringResource(R.string.backup_export_format_json_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedExportFormat = "json"
                                                        isFormatMenuExpanded = false
                                                    },
                                                    leadingIcon = {
                                                        if (selectedExportFormat == "json") {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(".fabitodo", fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(stringResource(R.string.backup_export_format_fabitodo_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedExportFormat = "fabitodo"
                                                        isFormatMenuExpanded = false
                                                    },
                                                    leadingIcon = {
                                                        if (selectedExportFormat == "fabitodo") {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (encryptBackups) {
                                            showExportPasswordDialog = true
                                        } else {
                                            pendingExportPassphrase = null
                                            val fileName = if (selectedExportFormat == "fabitodo") "fabitodo_backup.fabitodo" else "fabitodo_backup.json"
                                            createDocumentLauncher.launch(fileName)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.settings_export_backup),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Local Restore Box
                    item {
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudDownload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.backup_restore_header),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.settings_restore_local_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.backup_import_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                
                                Button(
                                    onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.settings_import_backup),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Storage & Cache Management Card
                    item {
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Storage,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.storage_management_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.storage_management_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.storage_db_size),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (storageStats.isCalculating) stringResource(R.string.storage_calculating) else storageStats.dbFormatted,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = stringResource(R.string.storage_cache_size),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (storageStats.isCalculating) stringResource(R.string.storage_calculating) else storageStats.cacheFormatted,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            settingsViewModel.clearAppCache { success ->
                                                val msg = if (success) context.getString(R.string.toast_cache_cleared) else context.getString(R.string.toast_clear_cache_error)
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.storage_clear_cache), fontSize = 12.sp, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            settingsViewModel.optimizeDatabase { success ->
                                                val msg = if (success) context.getString(R.string.toast_db_optimized) else context.getString(R.string.toast_optimize_db_error)
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Rounded.BuildCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.storage_optimize_db), fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

    if (showExportPasswordDialog) {
        BackupPassphraseDialog(
            description = stringResource(R.string.backup_password_export_desc),
            externalError = null,
            onDismiss = { showExportPasswordDialog = false },
            onConfirm = { enteredPassphrase ->
                showExportPasswordDialog = false
                pendingExportPassphrase = enteredPassphrase
                val fileName = if (selectedExportFormat == "fabitodo") "fabitodo_backup.fabitodo" else "fabitodo_backup.json"
                createDocumentLauncher.launch(fileName)
            }
        )
    }

    if (showImportPasswordDialog && pendingImportUriString != null) {
        BackupPassphraseDialog(
            description = stringResource(R.string.backup_password_import_desc),
            externalError = if (importPasswordWrongError) stringResource(R.string.backup_password_wrong_error) else null,
            onDismiss = {
                showImportPasswordDialog = false
                pendingImportUriString = null
                importPasswordWrongError = false
            },
            onConfirm = { enteredPassphrase ->
                val targetUri = android.net.Uri.parse(pendingImportUriString)
                coroutineScope.launch {
                    try {
                        val result = context.contentResolver.openInputStream(targetUri)?.use { inputStream ->
                            viewModel.importBackupFromStream(inputStream, passphrase = enteredPassphrase) { importedCats ->
                                settingsViewModel.saveCategories(importedCats)
                            }
                        } ?: BackupImportResult.InvalidFile

                        when (result) {
                            BackupImportResult.Success -> {
                                showImportPasswordDialog = false
                                pendingImportUriString = null
                                importPasswordWrongError = false
                                Toast.makeText(context, context.getString(R.string.toast_backup_import_success), Toast.LENGTH_SHORT).show()
                            }
                            BackupImportResult.WrongPassword,
                            BackupImportResult.RequiresPassword -> {
                                importPasswordWrongError = true
                            }
                            BackupImportResult.InvalidFile -> {
                                showImportPasswordDialog = false
                                pendingImportUriString = null
                                importPasswordWrongError = false
                                Toast.makeText(context, context.getString(R.string.toast_backup_import_invalid), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        showImportPasswordDialog = false
                        pendingImportUriString = null
                        importPasswordWrongError = false
                        Toast.makeText(context, context.getString(R.string.toast_backup_import_error, e.message), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun BackupPassphraseDialog(
    description: String,
    externalError: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var emptyError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.backup_password_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        emptyError = false
                    },
                    label = { Text(stringResource(R.string.backup_password_hint)) },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    isError = emptyError || externalError != null,
                    supportingText = {
                        when {
                            emptyError -> Text(stringResource(R.string.backup_password_empty_error))
                            externalError != null -> Text(externalError)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passphrase.isBlank()) {
                        emptyError = true
                    } else {
                        onConfirm(passphrase)
                    }
                }
            ) {
                Text(stringResource(R.string.backup_password_confirm_btn), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
