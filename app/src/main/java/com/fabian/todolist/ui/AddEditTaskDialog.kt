package com.fabian.todolist.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fabian.todolist.R
import com.fabian.todolist.data.Subtask
import com.fabian.todolist.data.Task
import com.fabian.todolist.ui.components.addtask.SubtaskSection
import com.fabian.todolist.ui.components.addtask.DateTimeSection
import com.fabian.todolist.data.getSubtasks
import com.fabian.todolist.data.withSubtasks
import com.fabian.todolist.util.getCategoryColor
import com.fabian.todolist.util.getCategoryIconVector
import com.fabian.todolist.util.getLocalizedCategoryName
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskDialog(
    task: Task?,
    categories: List<String>,
    onSave: (Task) -> Unit,
    onSaveBatch: (List<Task>) -> Unit = {},
    defaultOffsetMinutes: Int = 0,
    categoryColors: Map<String, Int> = emptyMap(),
    categoryIcons: Map<String, String> = emptyMap(),
    onAddCategory: ((String) -> Unit)? = null,
    isGeneratingAI: Boolean = false,
    onGenerateSubtasksWithAI: ((String, String, String?, List<String>, (String, String, String, List<String>) -> Unit, (String) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(task?.category ?: "") }
    var localSubtasks by remember { mutableStateOf(task?.getSubtasks() ?: emptyList()) }
    var isSubtasksExpanded by remember { mutableStateOf(localSubtasks.isNotEmpty()) }
    var newSubtaskText by remember { mutableStateOf("") }
    
    var selectedPriority by remember { mutableStateOf(task?.priority ?: com.fabian.todolist.data.TaskPriority.MEDIUM) }
    val attachedImageUri: String? = null

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    // Alarm properties
    var selectedDateInMillis by remember { mutableStateOf(task?.dueDate) }
    var selectedTimeStr by remember { mutableStateOf(task?.dueTime) }
    var setupReminder by remember { mutableStateOf(true) }
    var isRepeat by remember { mutableStateOf(task?.isRepeat ?: false) }
    var repeatType by remember { mutableStateOf(task?.repeatType ?: "Ninguno") }

    var selectedOffsetMinutes by remember {
        mutableStateOf(
            if (task?.reminderTime != null && task.dueDate != null && task.dueTime != null) {
                val calDue = Calendar.getInstance().apply {
                    timeInMillis = task.dueDate!!
                }
                val parts = task.dueTime!!.split(":")
                var parsedOk = false
                if (parts.size == 2) {
                    try {
                        calDue.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                        calDue.set(Calendar.MINUTE, parts[1].toInt())
                        calDue.set(Calendar.SECOND, 0)
                        calDue.set(Calendar.MILLISECOND, 0)
                        parsedOk = true
                    } catch (_: Exception) {}
                }
                if (parsedOk) {
                    val diffMins = (calDue.timeInMillis - task.reminderTime!!) / 60000L
                    diffMins.toInt().coerceAtLeast(0)
                } else {
                    defaultOffsetMinutes
                }
            } else {
                defaultOffsetMinutes
            }
        )
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = remember(title, description, selectedCategory, selectedPriority, selectedDateInMillis, selectedTimeStr, setupReminder, isRepeat, repeatType) {
        val originalTitle = task?.title ?: ""
        val originalDescription = task?.description ?: ""
        val originalCategory = task?.category ?: ""
        val originalPriority = task?.priority ?: com.fabian.todolist.data.TaskPriority.MEDIUM
        val originalDate = task?.dueDate
        val originalTime = task?.dueTime
        val originalRepeat = task?.isRepeat ?: false
        val originalRepeatType = task?.repeatType ?: "Ninguno"
        
        title != originalTitle || description != originalDescription || selectedCategory != originalCategory ||
        selectedPriority != originalPriority || selectedDateInMillis != originalDate || selectedTimeStr != originalTime ||
        isRepeat != originalRepeat || repeatType != originalRepeatType
    }

    val attemptDismiss = {
        if (hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onDismiss()
        }
    }

    val calendar = remember { Calendar.getInstance() }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                if (title.isBlank()) {
                    title = spokenText
                } else {
                    title = "$title $spokenText"
                }
            }
        }
    }

    fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.dialog_add_task_hint))
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            com.fabian.todolist.util.UiUtils.showShortToast(context, R.string.search_voice_error)
        }
    }

    fun launchDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDateInMillis = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun launchTimePicker() {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val hourFormatted = String.format(Locale.getDefault(), "%02d", hourOfDay)
                val minuteFormatted = String.format(Locale.getDefault(), "%02d", minute)
                selectedTimeStr = "$hourFormatted:$minuteFormatted"
                setupReminder = true // Auto-enable reminder when time is picked
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    fun onSaveClicked() {
        if (title.isBlank()) {
            com.fabian.todolist.util.UiUtils.showShortToast(context, R.string.title_empty_err)
            return
        }

        var alarmTime: Long? = null
        if (setupReminder && selectedDateInMillis != null && selectedTimeStr != null) {
            val calendarForAlarm = Calendar.getInstance().apply {
                timeInMillis = selectedDateInMillis!!
            }
            val timeParts = selectedTimeStr!!.split(":")
            if (timeParts.size == 2) {
                try {
                    calendarForAlarm.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    calendarForAlarm.set(Calendar.MINUTE, timeParts[1].toInt())
                    calendarForAlarm.set(Calendar.SECOND, 0)
                    calendarForAlarm.set(Calendar.MILLISECOND, 0)
                    alarmTime = calendarForAlarm.timeInMillis - (selectedOffsetMinutes * 60000L)
                } catch (_: Exception) {}
            }
        }

        val constructedTask = Task(
            id = task?.id ?: 0,
            title = title.trim(),
            description = description.trim(),
            dueDate = selectedDateInMillis,
            dueTime = selectedTimeStr,
            // Default to "General" — saving an empty-string category previously
            // orphaned tasks in the UI (drawer has no "uncategorized" entry,
            // getLocalizedCategoryName("") returns "", getCategoryColor returns Gray).
            category = selectedCategory.ifBlank { "General" },
            priority = selectedPriority,
            reminderTime = alarmTime,
            isRepeat = isRepeat,
            repeatType = repeatType,
            displayOrder = task?.displayOrder ?: 0,
            attachedImageUri = attachedImageUri
        ).withSubtasks(localSubtasks)
        onSave(constructedTask)
    }

    fun formatDate(timestamp: Long): String {
        return com.fabian.todolist.util.DateTimeUtils.formatDateStandard(timestamp)
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = if (task == null) stringResource(R.string.discard_task_title) else stringResource(R.string.discard_changes_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.discard_dialog_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.accept), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = attemptDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .systemBarsPadding()
            ) {
                // Background beautiful radial aura to elevate visuals
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.03f),
                                    Color.Transparent
                                ),
                                radius = 1200f
                            )
                        )
                )

                // Outer Layout Container
                Column(modifier = Modifier.fillMaxSize()) {
                    
                    // Elegant Header Area
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        IconButton(
                            onClick = attemptDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close, 
                                contentDescription = stringResource(R.string.cancel), 
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = if (task == null) stringResource(R.string.new_task) else stringResource(R.string.edit_task),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Action: AI Auto-fill
    val aiTitleRequiredError = stringResource(R.string.ai_title_required_error)
                        val aiCompletedToast = stringResource(R.string.ai_completed_toast)
                        IconButton(
                            onClick = {
                                if (title.isBlank()) {
                                    com.fabian.todolist.util.UiUtils.showShortToast(context, aiTitleRequiredError)
                                    return@IconButton
                                }
                                val existingSubtasksText = localSubtasks.joinToString(separator = "\n") { "- ${it.title} (Completada: ${it.isCompleted})" }
                                onGenerateSubtasksWithAI?.invoke(
                                    title,
                                    description,
                                    existingSubtasksText.takeIf { it.isNotBlank() },
                                    categories,
                                    { genDesc, genCat, genPriority, subtasks ->
                                        if (genDesc.isNotBlank()) {
                                            description = genDesc
                                        }
                                        if (genCat.isNotBlank()) {
                                            if (!categories.contains(genCat)) {
                                                onAddCategory?.invoke(genCat)
                                            }
                                            selectedCategory = genCat
                                        }
                                        if (genPriority.isNotBlank()) {
                                            selectedPriority = genPriority
                                        }
                                        localSubtasks = localSubtasks + subtasks.map { Subtask(title = it) }
                                        isSubtasksExpanded = true
                                        com.fabian.todolist.util.UiUtils.showShortToast(context, aiCompletedToast)
                                    },
                                    { error ->
                                        com.fabian.todolist.util.UiUtils.showLongToast(context, error)
                                    }
                                )
                            },
                            enabled = !isGeneratingAI
                        ) {
                            if (isGeneratingAI) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.ai_icon_content_desc), tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        TextButton(
                            onClick = { onSaveClicked() }
                        ) {
                            Text(
                                text = stringResource(R.string.save),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Main Scrollable Area containing beautifully arranged form sections
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // SECTION 1: "What is to be done?" Super-Card
                        Text(
                            text = stringResource(R.string.dialog_add_task_title_prompt),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val focusRequester = remember { FocusRequester() }
                                    LaunchedEffect(Unit) { focusRequester.requestFocus() }

                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusRequester(focusRequester),
                                        textStyle = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        placeholder = {
                                            Text(
                                                text = stringResource(R.string.dialog_add_task_hint),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            )
                                        },
                                        minLines = 1,
                                        maxLines = 6,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                        ),
                                        trailingIcon = {
                                            if (title.isNotEmpty()) {
                                                IconButton(onClick = { title = "" }) {
                                                    Icon(
                                                        Icons.Default.Clear,
                                                        contentDescription = "Limpiar",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            imeAction = ImeAction.Default,
                                            keyboardType = KeyboardType.Text
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = { launchVoiceInput() },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            .size(46.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = stringResource(R.string.desc_mic),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                    
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    placeholder = {
                                        Text(
                                            text = stringResource(id = R.string.task_description_hint),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 5,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Default,
                                        keyboardType = KeyboardType.Text
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 2: Task Categories scrolling ribbon list
                        Text(
                            text = stringResource(R.string.dialog_task_list_header),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Option: None
                            item {
                                val isSelected = selectedCategory.isBlank()
                                val noneColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                Surface(
                                    onClick = { selectedCategory = "" },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else noneColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.category_none),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Active category lists
                            items(categories, key = { it }) { category ->
                                val isSelected = selectedCategory == category
                                val customColor = getCategoryColor(category, categoryColors[category])
                                val customIcon = getCategoryIconVector(category, categoryIcons[category])
                                
                                Surface(
                                    onClick = { selectedCategory = category },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) customColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) customColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    ) {
                                        Icon(
                                            imageVector = customIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) customColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = getLocalizedCategoryName(category),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) customColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            
                            // Add category button
                            item {
                                Surface(
                                    onClick = { showAddCategoryDialog = true },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.add),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 3: Priority selection with modern segmented buttons grid
                        Text(
                            text = stringResource(R.string.priority),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val priorities = listOf(com.fabian.todolist.data.TaskPriority.LOW, com.fabian.todolist.data.TaskPriority.MEDIUM, com.fabian.todolist.data.TaskPriority.HIGH, com.fabian.todolist.data.TaskPriority.CRITICAL)
                            items(priorities.size) { index ->
                                val p = priorities[index]
                                val isSelected = selectedPriority == p
                                val baseColor = when(p) {
                                    com.fabian.todolist.data.TaskPriority.CRITICAL -> Color(0xFFB3261E)
                                    com.fabian.todolist.data.TaskPriority.HIGH -> Color(0xFFEA4335)
                                    com.fabian.todolist.data.TaskPriority.MEDIUM -> Color(0xFFFBBC05)
                                    com.fabian.todolist.data.TaskPriority.LOW -> Color(0xFF34A853)
                                    else -> Color.Gray
                                }
                                val pName = when(p) {
                                    com.fabian.todolist.data.TaskPriority.CRITICAL -> stringResource(R.string.priority_critical_label).split(" ")[0]
                                    com.fabian.todolist.data.TaskPriority.HIGH -> stringResource(R.string.priority_high_label).split(" ")[0]
                                    com.fabian.todolist.data.TaskPriority.MEDIUM -> stringResource(R.string.priority_medium_label).split(" ")[0]
                                    com.fabian.todolist.data.TaskPriority.LOW -> stringResource(R.string.priority_low_label).split(" ")[0]
                                    else -> ""
                                }
                                
                                Card(
                                    onClick = { selectedPriority = p },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) baseColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) baseColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
                                    ),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = pName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Option below row to completely clear priorities (Sin prioridad option)
                        val isNoPriority = selectedPriority == com.fabian.todolist.data.TaskPriority.NONE || selectedPriority == ""
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            onClick = { selectedPriority = com.fabian.todolist.data.TaskPriority.NONE },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNoPriority) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isNoPriority) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .align(Alignment.End)
                                .wrapContentSize()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (isNoPriority) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.priority_none_label),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNoPriority) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 4: Subtasks Custom Checklist
                        SubtaskSection(
                            localSubtasks = localSubtasks,
                            onSubtasksChanged = { localSubtasks = it },
                            isSubtasksExpanded = isSubtasksExpanded,
                            onExpandedChanged = { isSubtasksExpanded = it },
                            title = title,
                            description = description,
                            categories = categories,
                            isGeneratingAI = isGeneratingAI,
                            onGenerateSubtasksWithAI = onGenerateSubtasksWithAI,
                            onDescriptionChanged = { description = it },
                            onCategoryChanged = { selectedCategory = it },
                            onPriorityChanged = { selectedPriority = it }
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 5: Notifications & Date/Time Options
                        DateTimeSection(
                            selectedDateInMillis = selectedDateInMillis,
                            onDateChanged = { selectedDateInMillis = it },
                            selectedTimeStr = selectedTimeStr,
                            onTimeChanged = { selectedTimeStr = it },
                            isRepeat = isRepeat,
                            onRepeatChanged = { isRepeat = it },
                            repeatType = repeatType,
                            onRepeatTypeChanged = { repeatType = it },
                            launchDatePicker = { launchDatePicker() },
                            launchTimePicker = { launchTimePicker() },
                            formatDate = { formatDate(it) }
                        )

                        // Generous space at the bottom 
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddCategoryDialog = false 
                newCategoryText = ""
            },
            title = {
                Text(text = stringResource(R.string.add_category_title))
            },
            text = {
                OutlinedTextField(
                    value = newCategoryText,
                    onValueChange = { newCategoryText = it },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val txt = newCategoryText.trim()
                        if (txt.isNotEmpty()) {
                            onAddCategory?.invoke(txt)
                            selectedCategory = txt
                        }
                        showAddCategoryDialog = false
                        newCategoryText = ""
                    }
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCategoryDialog = false
                        newCategoryText = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

