package com.fabian.todolist.ui.components.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabian.todolist.R
import com.fabian.todolist.data.SystemCategory
import com.fabian.todolist.ui.TaskViewModel
import com.fabian.todolist.ui.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsDialog(
    viewModel: SettingsViewModel,
    taskViewModel: TaskViewModel, // Keep TaskViewModel for testing API calls if needed
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val aiModel by viewModel.aiModel.collectAsStateWithLifecycle()
    val aiLowLatencyMode by viewModel.aiLowLatencyMode.collectAsStateWithLifecycle()
    val aiApiKey by viewModel.aiApiKey.collectAsStateWithLifecycle()
    val aiSubtaskCount by viewModel.aiSubtaskCount.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val showAiBanner by viewModel.showAiBanner.collectAsStateWithLifecycle()
    val currentLangCode by viewModel.languageCode.collectAsStateWithLifecycle()
    // Need showAiBanner in SettingsViewModel or keep in TaskViewModel? 
    // Usually banner vis is a preference. Moving it to SettingsViewModel soon.
    // For now, let's assume it's moved or accessible via settingsViewModel.
    
    // I need to add showAiBanner to SettingsViewModel if I want to remove it from TaskViewModel.
    
    var showModelDropdown by remember { mutableStateOf(false) }
    var keyVisible by remember { mutableStateOf(false) }
    
    // Testing states
    var testTaskTitle by remember { mutableStateOf("Preparar presentación importante") }
    var testInProgress by remember { mutableStateOf(false) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }
    var testErrorMsg by remember { mutableStateOf<String?>(null) }
    var testResultDesc by remember { mutableStateOf("") }
    var testResultCat by remember { mutableStateOf("") }
    var testResultPriority by remember { mutableStateOf("") }
    var testResultSubtasks by remember { mutableStateOf<List<String>>(emptyList()) }

    val models = listOf(
        "gemini-3.5-flash",
        "gemini-3.1-pro-preview",
        "gemini-flash-latest",
        "gemini-3.1-flash-lite-preview",
        "gemini-2.5-flash-preview",
        "gpt-4o",
        "gpt-4o-mini",
        "claude-3-5-sonnet",
        "deepseek-chat",
        "llama-3.3-70b"
    )

    // Detallar qué tipo de API Key se necesita según el modelo
    val keyRequirementText = when {
        aiModel.startsWith("gemini") -> stringResource(R.string.ai_key_req_gemini)
        aiModel.startsWith("gpt") -> stringResource(R.string.ai_key_req_gpt)
        aiModel.startsWith("claude") -> stringResource(R.string.ai_key_req_claude)
        aiModel.startsWith("deepseek") -> stringResource(R.string.ai_key_req_deepseek)
        aiModel.startsWith("llama") -> stringResource(R.string.ai_key_req_llama)
        else -> stringResource(R.string.ai_key_req_generic)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            SettingsSubmenuScaffold(
                title = stringResource(R.string.ai_center_title),
                onBack = onDismiss
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Beautiful top visual header card with gradient
                    AnimatedVisibility(
                        visible = showAiBanner,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(end = 28.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = stringResource(R.string.ai_banner_magic),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.ai_banner_title),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(R.string.ai_banner_body),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Interactive close X button in top right of the box
                                IconButton(
                                    onClick = { viewModel.setShowAiBanner(false) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.ai_banner_hide),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SECTION 1: PROVEEDOR Y MODELO
                    Text(
                        text = stringResource(R.string.ai_section_model),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.ai_select_motor_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.ai_select_motor_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = showModelDropdown,
                                onExpandedChange = { showModelDropdown = !showModelDropdown }
                            ) {
                                OutlinedTextField(
                                    value = aiModel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showModelDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    leadingIcon = {
                                        Box(modifier = Modifier.padding(start = 12.dp, end = 4.dp)) {
                                            ModelBrandBadge(model = aiModel)
                                        }
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = showModelDropdown,
                                    onDismissRequest = { showModelDropdown = false }
                                ) {
                                    models.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                ModelBrandBadge(model = selectionOption)
                                            },
                                            text = { 
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(
                                                        text = selectionOption,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = getModelCreator(selectionOption),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.setAiModel(selectionOption)
                                                showModelDropdown = false
                                                // Reset testing states upon switching models to avoid confusion
                                                testSuccess = null
                                                testErrorMsg = null
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.ai_ultra_low_latency_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.ai_ultra_low_latency_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = aiLowLatencyMode,
                                onCheckedChange = { viewModel.setAiLowLatencyMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: CLAVE API CREDENTIALS
                    Text(
                        text = stringResource(R.string.ai_section_access),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_provider_key_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (aiModel.startsWith("gemini")) stringResource(R.string.ai_badge_recommended) else stringResource(R.string.ai_badge_required),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = keyRequirementText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = aiApiKey,
                                onValueChange = { viewModel.setAiApiKey(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.ai_key_placeholder)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = stringResource(R.string.ai_key_icon_desc),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { keyVisible = !keyVisible }) {
                                        Icon(
                                            imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (keyVisible) stringResource(R.string.ai_banner_hide) else stringResource(R.string.ai_key_show),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Secure notice
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = stringResource(R.string.ai_security_desc),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.ai_security_body),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION: AJUSTE DE SUBTAREAS
                    Text(
                        text = stringResource(R.string.ai_subtasks_settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_subtasks_amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.ai_subtasks_count, aiSubtaskCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = stringResource(R.string.ai_subtasks_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Slider(
                                value = aiSubtaskCount.toFloat(),
                                onValueChange = { viewModel.setAiSubtaskCount(it.toInt()) },
                                valueRange = 1f..15f,
                                steps = 13,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 3: PRUEBA DE CONEXIÓN (Interactive Tester)
                    Text(
                        text = stringResource(R.string.ai_section_lab),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.ai_lab_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.ai_lab_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = testTaskTitle,
                                onValueChange = { testTaskTitle = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.ai_lab_field_label)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (testTaskTitle.isBlank()) {
                                        Toast.makeText(context, context.getString(R.string.ai_lab_err_empty), Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    testInProgress = true
                                    testSuccess = null
                                    testErrorMsg = null
                                    
                                    taskViewModel.generateSubtasksWithAI(
                                        taskTitle = testTaskTitle,
                                        taskDescription = context.getString(R.string.ai_lab_test_desc_default),
                                        existingSubtasksText = null,
                                        categories = categories.filter { it != SystemCategory.ALL_TASKS && it != SystemCategory.COMPLETED },
                                        model = aiModel,
                                        apiKey = aiApiKey,
                                        subtaskCount = aiSubtaskCount,
                                        langCode = currentLangCode,
                                        onSuccess = { desc, cat, prio, subs ->
                                            testInProgress = false
                                            testSuccess = true
                                            testResultDesc = desc
                                            testResultCat = cat
                                            testResultPriority = prio
                                            testResultSubtasks = subs
                                        },
                                        onError = { error ->
                                            testInProgress = false
                                            testSuccess = false
                                            testErrorMsg = error
                                        }
                                    )
                                },
                                enabled = !testInProgress,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                if (testInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.ai_btn_connecting))
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.ai_btn_test_desc))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.ai_btn_test_now))
                                }
                            }

                            // Dynamic Testing Outputs Area
                            AnimatedVisibility(
                                visible = testSuccess != null || testInProgress,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 14.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (testSuccess == true) {
                                        // GORGEOUS SUCCESS PREVIEW
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4CAF50)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Success",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = stringResource(R.string.ai_result_success),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50),
                                                fontSize = 14.sp
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // Structured breakdown container
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = stringResource(R.string.ai_result_parsed_header),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))

                                                if (testResultDesc.isNotBlank()) {
                                                    Text(
                                                        text = stringResource(R.string.ai_result_suggested_desc),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = testResultDesc,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    if (testResultCat.isNotBlank()) {
                                                        Column {
                                                            Text(
                                                                text = stringResource(R.string.ai_result_suggested_cat),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "📁 $testResultCat",
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    }
                                                    if (testResultPriority.isNotBlank()) {
                                                        Column {
                                                            Text(
                                                                text = stringResource(R.string.ai_result_suggested_prio),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "🚩 $testResultPriority",
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    }
                                                }

                                                if (testResultSubtasks.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = stringResource(R.string.ai_result_subtasks_count, testResultSubtasks.size),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    testResultSubtasks.forEachIndexed { idx, sub ->
                                                        Text(
                                                            text = "${idx + 1}. $sub",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                    } else if (testSuccess == false) {
                                        // CRISP RED ERROR BOX
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Error,
                                                    contentDescription = "Error",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = stringResource(R.string.ai_result_error_title),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                        fontSize = 14.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = testErrorMsg ?: stringResource(R.string.ai_result_error_generic),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = stringResource(R.string.ai_result_error_tips),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ModelBrandBadge(model: String, modifier: Modifier = Modifier) {
    val lower = model.lowercase()
    
    Card(
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.size(28.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val cx = width / 2f
            val cy = height / 2f
            
            when {
                lower.contains("gemini") -> {
                    // Google Gemini Deep Space Gradient with authentic sparkling 4-point star shapes
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2E2A75), Color(0xFF0F1026)),
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            radius = width * 0.7f
                        )
                    )
                    
                    // Main Sparkle Star
                    val starPath = Path().apply {
                        val scx = width * 0.44f
                        val scy = height * 0.44f
                        val r = width * 0.35f
                        moveTo(scx, scy - r)
                        quadraticTo(scx, scy, scx + r, scy)
                        quadraticTo(scx, scy, scx, scy + r)
                        quadraticTo(scx, scy, scx - r, scy)
                        quadraticTo(scx, scy, scx, scy - r)
                    }
                    drawPath(
                        path = starPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFEC4899))
                        )
                    )
                    
                    // Secondary Sparkle
                    val smallStarPath = Path().apply {
                        val scx = width * 0.74f
                        val scy = height * 0.26f
                        val r = width * 0.15f
                        moveTo(scx, scy - r)
                        quadraticTo(scx, scy, scx + r, scy)
                        quadraticTo(scx, scy, scx, scy + r)
                        quadraticTo(scx, scy, scx - r, scy)
                        quadraticTo(scx, scy, scx, scy - r)
                    }
                    drawPath(
                        path = smallStarPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF22D3EE), Color(0xFFA5B4FC))
                        )
                    )
                }
                lower.contains("gpt") -> {
                    // Official OpenAI Emerald Green Backdrop
                    drawRect(color = Color(0xFF10A37F))
                    
                    // Create the exact official OpenAI 6-segment rosette spiral!
                    val scale = width / 100f
                    val strokeW = scale * 5.5f
                    for (i in 0 until 6) {
                        val angle = i * 60f
                        rotate(angle, androidx.compose.ui.geometry.Offset(cx, cy)) {
                            val path = Path().apply {
                                val sX = cx
                                val sY = cy - 3f * scale
                                moveTo(sX, sY)
                                // Smooth official curve styling of the interlocking OpenAI petal loops
                                cubicTo(
                                    cx - 16f * scale, cy - 23f * scale,
                                    cx + 4f * scale, cy - 38f * scale,
                                    cx + 17f * scale, cy - 24f * scale
                                )
                                cubicTo(
                                    cx + 25f * scale, cy - 14f * scale,
                                    cx + 14f * scale, cy - 5f * scale,
                                    cx, cy - 3f * scale
                                )
                            }
                            drawPath(
                                path = path,
                                color = Color.White,
                                style = Stroke(width = strokeW)
                            )
                        }
                    }
                }
                lower.contains("claude") -> {
                    // Official Anthropic Warm Coral/Terracotta hue
                    drawRect(color = Color(0xFFD97706))
                    
                    val scale = width / 100f
                    // Sleek 3-petal organic Anthropic brand silhouette
                    val claudePath = Path().apply {
                        // Left hand-brushed organic petal
                        moveTo(cx - 16f * scale, cy + 18f * scale)
                        quadraticTo(cx - 25f * scale, cy + 9f * scale, cx - 18f * scale, cy - 6f * scale)
                        quadraticTo(cx - 11f * scale, cy - 19f * scale, cx - 5f * scale, cy - 21f * scale)
                        quadraticTo(cx + 1f * scale, cy - 23f * scale, cx, cy - 13f * scale)
                        quadraticTo(cx - 1f * scale, cy + 1f * scale, cx - 16f * scale, cy + 18f * scale)
                    }
                    drawPath(path = claudePath, color = Color.White)

                    val claudeRightPath = Path().apply {
                        // Right hand-brushed organic petal
                        moveTo(cx + 16f * scale, cy + 18f * scale)
                        quadraticTo(cx + 25f * scale, cy + 9f * scale, cx + 18f * scale, cy - 6f * scale)
                        quadraticTo(cx + 11f * scale, cy - 19f * scale, cx + 5f * scale, cy - 21f * scale)
                        quadraticTo(cx - 1f * scale, cy - 23f * scale, cx, cy - 13f * scale)
                        quadraticTo(cx + 1f * scale, cy + 1f * scale, cx + 16f * scale, cy + 18f * scale)
                    }
                    drawPath(path = claudeRightPath, color = Color.White)
                    
                    // Center circular joint
                    drawCircle(
                        color = Color.White,
                        radius = 8.5f * scale,
                        center = androidx.compose.ui.geometry.Offset(cx, cy + 10f * scale)
                    )
                }
                lower.contains("deepseek") -> {
                    // Official DeepSeek Deep Blue Gradient
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1652F0), Color(0xFF003AB3))
                        )
                    )
                    
                    val scale = width / 100f
                    // Professional 'd' monogram logo loop
                    val dOutlinePath = Path().apply {
                        moveTo(cx - 15f * scale, cy + 18f * scale)
                        lineTo(cx + 12f * scale, cy + 18f * scale)
                        quadraticTo(cx + 26f * scale, cy + 18f * scale, cx + 26f * scale, cy)
                        quadraticTo(cx + 26f * scale, cy - 18f * scale, cx + 12f * scale, cy - 18f * scale)
                        lineTo(cx - 12f * scale, cy - 18f * scale)
                        lineTo(cx - 12f * scale, cy + 5f * scale)
                        // Inner loop hollow curve
                        quadraticTo(cx - 12f * scale, cy - 6f * scale, cx + 2f * scale, cy - 6f * scale)
                        quadraticTo(cx + 13f * scale, cy - 6f * scale, cx + 13f * scale, cy + 2f * scale)
                        quadraticTo(cx + 13f * scale, cy + 8f * scale, cx + 2f * scale, cy + 8f * scale)
                        lineTo(cx - 15f * scale, cy + 8f * scale)
                        close()
                    }
                    drawPath(
                        path = dOutlinePath,
                        color = Color.White
                    )
                    
                    // Glowing cyan intelligent center node representing Deep Search core
                    drawCircle(
                        color = Color(0xFF00F2FE),
                        radius = 4.5f * scale,
                        center = androidx.compose.ui.geometry.Offset(cx + 2.5f * scale, cy + 1.5f * scale)
                    )
                }
                lower.contains("llama") -> {
                    // Meta AI Obsidian Gradient & Neon Infinity Energy Loop
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF131524), Color(0xFF0B0C14)),
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                            radius = width * 0.7f
                        )
                    )
                    
                    val lw = width * 0.28f
                    val lh = width * 0.13f
                    val infinityPath = Path().apply {
                        moveTo(cx, cy)
                        cubicTo(cx - lw, cy - lh, cx - lw, cy + lh, cx, cy)
                        cubicTo(cx + lw, cy - lh, cx + lw, cy + lh, cx, cy)
                    }
                    drawPath(
                        path = infinityPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF9333EA), Color(0xFF2563EB), Color(0xFF06B6D4))
                        ),
                        style = Stroke(width = width * 0.11f)
                    )
                }
                else -> {
                    // Tech Microchip generic logo representation
                    drawRect(color = Color(0xFF6B7280))
                    drawRect(
                        color = Color.White,
                        size = androidx.compose.ui.geometry.Size(width * 0.36f, height * 0.36f),
                        topLeft = androidx.compose.ui.geometry.Offset(width * 0.32f, height * 0.32f),
                        style = Stroke(width = width * 0.08f)
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(cx, 0f),
                        end = androidx.compose.ui.geometry.Offset(cx, height),
                        strokeWidth = width * 0.07f
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(0f, cy),
                        end = androidx.compose.ui.geometry.Offset(width, cy),
                        strokeWidth = width * 0.07f
                    )
                }
            }
        }
    }
}

fun getModelCreator(model: String): String {
    val lower = model.lowercase()
    return when {
        lower.contains("gemini") -> "Google DeepMind"
        lower.contains("gpt") -> "OpenAI"
        lower.contains("claude") -> "Anthropic"
        lower.contains("deepseek") -> "DeepSeek AI"
        lower.contains("llama") -> "Meta AI"
        else -> "Inteligencia Artificial"
    }
}
