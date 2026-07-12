package com.fabian.todolist.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlin.math.roundToInt
import com.fabian.todolist.R
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.getSubtasks
import com.fabian.todolist.data.withSubtasks
import com.fabian.todolist.util.getLocalizedCategoryName
import com.fabian.todolist.ui.components.TaskTopAppBar
import com.fabian.todolist.ui.components.TaskDrawerContent
import com.fabian.todolist.ui.components.TaskSearchBar
import com.fabian.todolist.ui.components.TaskSortBottomSheet
import com.fabian.todolist.ui.components.TaskEmptyState
import com.fabian.todolist.ui.components.TaskDeleteDialog
import com.fabian.todolist.ui.components.TaskAddCategoryDialog
import com.fabian.todolist.ui.components.MySelectionTopAppBar
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    onNavigateToGoogleLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.tasksState.collectAsStateWithLifecycle()
    val categories by settingsViewModel.categories.collectAsStateWithLifecycle()
    val categoryColors by settingsViewModel.categoryColors.collectAsStateWithLifecycle()
    val categoryIcons by settingsViewModel.categoryIcons.collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory = uiState.selectedCategory
    val selectedStatus = uiState.selectedStatus
    val sortBy = uiState.sortBy
    val searchQuery = uiState.searchQuery
    
    val pendingTasksCount by viewModel.pendingTasksCount.collectAsStateWithLifecycle()
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Preferences states
    val themeAccent by settingsViewModel.themeAccent.collectAsStateWithLifecycle()
    val themeDark by settingsViewModel.themeDark.collectAsStateWithLifecycle()
    val languageCode by settingsViewModel.languageCode.collectAsStateWithLifecycle()
    val aiModel by settingsViewModel.aiModel.collectAsStateWithLifecycle()
    val aiApiKey by settingsViewModel.aiApiKey.collectAsStateWithLifecycle()
    val aiSubtaskCount by settingsViewModel.aiSubtaskCount.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showStatsDialog by rememberSaveable { mutableStateOf(false) }
    var showFocusDrawer by rememberSaveable { mutableStateOf(false) }

    val selectedTaskIds = remember { mutableStateListOf<Int>() }
    val longPressAction by settingsViewModel.longPressAction.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(selectedCategory) {
        selectedTaskIds.clear()
    }

    val shouldShowSettings by viewModel.shouldShowSettings.collectAsStateWithLifecycle()
    val shouldShowAddDialog by viewModel.shouldShowAddDialog.collectAsStateWithLifecycle()
    val confirmOnDelete by settingsViewModel.confirmOnDelete.collectAsStateWithLifecycle()
    val hapticFeedbackOnComplete by settingsViewModel.hapticFeedbackOnComplete.collectAsStateWithLifecycle()
    val onboardingCompleted by settingsViewModel.onboardingCompleted.collectAsStateWithLifecycle()
    if (!onboardingCompleted) {
        com.fabian.todolist.ui.components.settings.OnboardingScreen(
            settingsViewModel = settingsViewModel,
            authViewModel = authViewModel,
            onFinished = {
                // Refresh the orientation or layout to trigger standard setup
                var currentContext = context
                while (currentContext is android.content.ContextWrapper) {
                    if (currentContext is android.app.Activity) {
                        currentContext.recreate()
                        break
                    }
                    currentContext = currentContext.baseContext
                }
            }
        )
        return
    }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var tasksToDeleteBatch by remember { mutableStateOf<List<Task>?>(null) }
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    val localView = androidx.compose.ui.platform.LocalView.current

    LaunchedEffect(shouldShowSettings) {
        if (shouldShowSettings) {
            showSettingsDialog = true
            viewModel.setShouldShowSettings(false)
        }
    }

    LaunchedEffect(shouldShowAddDialog) {
        if (shouldShowAddDialog) {
            taskToEdit = null
            showAddEditDialog = true
            viewModel.setShouldShowAddDialog(false)
        }
    }

    // Request notification permission if SDK >= 33
    var hasPostNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                false
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotificationPermission = isGranted
        if (!isGranted) {
            com.fabian.todolist.util.UiUtils.showLongToast(context, R.string.no_active_reminders_permission)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPostNotificationPermission) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var isProgressVisible by rememberSaveable { mutableStateOf(false) }

    val showUndoSnackbar = { task: Task ->
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.task_deleted_msg),
                actionLabel = context.getString(R.string.undo_action),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreTask(task)
            }
        }
    }

    val showUndoBatchSnackbar = { tasks: List<Task> ->
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.task_deleted_msg),
                actionLabel = context.getString(R.string.undo_action),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                tasks.forEach { viewModel.restoreTask(it) }
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 15f) {
                    isProgressVisible = true
                }
                return super.onPostScroll(consumed, available, source)
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15f) {
                    isProgressVisible = false
                }
                return super.onPreScroll(available, source)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TaskDrawerContent(
        drawerState = drawerState,
        selectedCategory = selectedCategory,
        categories = categories,
        categoryColors = categoryColors,
        categoryIcons = categoryIcons,
        languageCode = languageCode,
        onCategorySelected = { viewModel.setSelectedCategory(it) },
        onSettingsSelected = { showSettingsDialog = true }
    )
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AnimatedContent(
                    targetState = selectedTaskIds.isNotEmpty(),
                    transitionSpec = {
                        slideInVertically { height -> -height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
                    },
                    label = "TopAppBarTransition"
                ) { isSelectionActive ->
                    if (isSelectionActive) {
                        MySelectionTopAppBar(
                            selectedCount = selectedTaskIds.size,
                            onClearSelection = { selectedTaskIds.clear() },
                            onEditSelected = {
                                val singleTaskId = selectedTaskIds.firstOrNull()
                                val singleTask = tasks.find { it.id == singleTaskId }
                                if (singleTask != null) {
                                    taskToEdit = singleTask
                                    showAddEditDialog = true
                                    selectedTaskIds.clear()
                                }
                            },
                            onShareSelected = {
                                val selectedTasks = tasks.filter { it.id in selectedTaskIds }
                                val textToShare = selectedTasks.joinToString("\n\n---\n\n") { t ->
                                    val subtasks = t.getSubtasks()
                                    val subtasksText = if (subtasks.isNotEmpty()) {
                                        "\n\nSubtareas:\n" + subtasks.joinToString("\n") { "- " + (if (it.isCompleted) "[✔] " else "[ ] ") + it.title }
                                    } else ""
                                    
                                    "Tarea: ${t.title}\n" +
                                    (if (t.description.isNotEmpty()) "Descripción: ${t.description}\n" else "") +
                                    "Categoría: ${t.category}\n" +
                                    "Prioridad: ${t.priority}\n" +
                                    (t.dueDate?.let { context.getString(R.string.due_date_prefix) + com.fabian.todolist.util.DateTimeUtils.formatDateSimpleSlash(it) + "\n" } ?: "") +
                                    (if (t.isRepeat) "Repetición: ${t.repeatType}\n" else "") +
                                    subtasksText
                                }
                                val sendIntent: android.content.Intent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, textToShare)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            onDeleteSelected = {
                                if (hapticFeedbackOnComplete) {
                                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                                }
                                val selectedTasks = tasks.filter { it.id in selectedTaskIds }
                                if (confirmOnDelete) {
                                    tasksToDeleteBatch = selectedTasks
                                } else {
                                    selectedTasks.forEach { t -> viewModel.deleteTask(t) }
                                    showUndoBatchSnackbar(selectedTasks)
                                    selectedTaskIds.clear()
                                }
                            },
                            isTrashCategory = selectedCategory == com.fabian.todolist.data.SystemCategory.TRASH
                        )
                    } else {
                        TaskTopAppBar(
                            selectedCategory = selectedCategory,
                            categoryColors = categoryColors,
                            pendingTasksCount = pendingTasksCount,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onSortClick = { showFocusDrawer = true },
                            onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                            onStatsClick = { showStatsDialog = true }
                        )
                    }
                }
            },
            floatingActionButton = {
                val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.add_task), modifier = Modifier.animateContentSize()) },
                    icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_add_tasks)) },
                    onClick = {
                        taskToEdit = null
                        showAddEditDialog = true
                    },
                    expanded = isFabExpanded,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // Task statistics details
            val totalCount = tasks.size
            val completedCount = tasks.count { it.isCompleted }

            if (tasks.isEmpty()) {
                TaskEmptyState()
            } else if (selectedCategory == com.fabian.todolist.data.SystemCategory.EISENHOWER) {
                com.fabian.todolist.ui.components.EisenhowerMatrixView(
                    tasks = tasks,
                    categoryColors = categoryColors,
                    onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                    onRestore = { viewModel.restoreTask(it) },
                    onEdit = {
                        taskToEdit = it
                        showAddEditDialog = true
                    },
                    onDelete = { task ->
                        if (confirmOnDelete) {
                            taskToDelete = task
                        } else {
                            viewModel.deleteTask(task)
                            showUndoSnackbar(task)
                        }
                    },
                    onUpdateSubtasks = { task, updatedSubtasks ->
                        viewModel.updateTask(task.withSubtasks(updatedSubtasks))
                    },
                    selectedTaskIds = selectedTaskIds,
                    onToggleSelect = { task ->
                        if (selectedTaskIds.contains(task.id)) {
                            selectedTaskIds.remove(task.id)
                        } else {
                            selectedTaskIds.add(task.id)
                        }
                    },
                    longPressAction = longPressAction,
                    hapticFeedbackOnComplete = hapticFeedbackOnComplete,
                    confirmOnDelete = confirmOnDelete
                )
            } else {
                val groupedTasks by viewModel.groupedTasks.collectAsStateWithLifecycle()

                val groupOrder = listOf(
                    "group_vencidos", "group_hoy",
                    "group_manana", "group_pasado_manana", "group_esta_semana", "group_este_mes", "group_futuro", "group_not_set"
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    TaskSearchBar(
                        isSearchExpanded = isSearchExpanded,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) }
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("task_list"),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                    ) {
                        item {
                            AnimatedVisibility(
                                visible = isProgressVisible,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                com.fabian.todolist.ui.components.TaskProgressHeaderCard(
                                    totalCount = totalCount,
                                    completedCount = completedCount,
                                    categoryName = selectedCategory,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        if (sortBy != com.fabian.todolist.data.SortOrder.MANUAL) {
                            groupOrder.forEach { groupKey ->
                                val tasksInGroup = groupedTasks[groupKey] ?: emptyMap()
                                if (tasksInGroup.isNotEmpty()) {
                                    item {
                                        val groupTitle = when (groupKey) {
                                            "group_vencidos" -> stringResource(R.string.group_vencidos)
                                            "group_hoy" -> stringResource(R.string.group_hoy)
                                            "group_manana" -> stringResource(R.string.group_manana)
                                            "group_pasado_manana" -> stringResource(R.string.group_pasado_manana)
                                            "group_esta_semana" -> stringResource(R.string.group_esta_semana)
                                            "group_este_mes" -> stringResource(R.string.group_este_mes)
                                            "group_futuro" -> stringResource(R.string.group_futuro)
                                            "group_not_set" -> stringResource(R.string.group_not_set)
                                            else -> ""
                                        }
                                        Text(
                                            text = groupTitle,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                                        )
                                    }

                                    val priorityKeys = listOf(com.fabian.todolist.data.TaskPriority.CRITICAL, com.fabian.todolist.data.TaskPriority.HIGH, com.fabian.todolist.data.TaskPriority.MEDIUM, com.fabian.todolist.data.TaskPriority.LOW, com.fabian.todolist.data.TaskPriority.NONE)
                                    priorityKeys.forEach { prio ->
                                        val tasksOfPrio = tasksInGroup[prio] ?: emptyList()
                                        if (tasksOfPrio.isNotEmpty()) {
                                            item {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val (dotColor, labelText) = when (prio) {
                                                        com.fabian.todolist.data.TaskPriority.CRITICAL -> Color(0xFFB3261E) to stringResource(R.string.priority_critical_label)
                                                        com.fabian.todolist.data.TaskPriority.HIGH -> Color(0xFFEA4335) to stringResource(R.string.priority_high_label)
                                                        com.fabian.todolist.data.TaskPriority.MEDIUM -> Color(0xFFFBBC05) to stringResource(R.string.priority_medium_label)
                                                        com.fabian.todolist.data.TaskPriority.LOW -> Color(0xFF34A853) to stringResource(R.string.priority_low_label)
                                                        else -> Color.Gray to stringResource(R.string.priority_none_label)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(dotColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = labelText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                        letterSpacing = 0.8.sp
                                                    )
                                                }
                                            }

                                            itemsIndexed(tasksOfPrio, key = { _, it -> it.id }) { index, task ->
                                                var isVisible by rememberSaveable { mutableStateOf(true) }
                                                var isExpanded by rememberSaveable { mutableStateOf(false) }

                                                AnimatedVisibility(
                                                    visible = isVisible,
                                                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                                                            expandVertically(animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)) +
                                                            scaleIn(initialScale = 0.82f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)),
                                                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                                           shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                                           scaleOut(targetScale = 0.82f, animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                                                    modifier = Modifier.animateItem(
                                                        placementSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
                                                    )
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                    ) {
                                                        TaskDetailedRow(
                                                            task = task,
                                                            isExpanded = isExpanded,
                                                            onToggleComplete = {
                                                                if (hapticFeedbackOnComplete && !task.isCompleted) {
                                                                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                                                                }
                                                                if (selectedCategory != com.fabian.todolist.data.SystemCategory.COMPLETED) {
                                                                    isVisible = false
                                                                    // Wait for animation before updating DB
                                                                    scope.launch {
                                                                        kotlinx.coroutines.delay(300)
                                                                        viewModel.toggleTaskCompletion(task)
                                                                    }
                                                                } else {
                                                                    viewModel.toggleTaskCompletion(task)
                                                                }
                                                            },
                                                            onRestore = {
                                                                viewModel.restoreTask(task)
                                                                isVisible = false
                                                            },
                                                            onEdit = {
                                                                taskToEdit = task
                                                                showAddEditDialog = true
                                                            },
                                                            onDelete = {
                                                                if (hapticFeedbackOnComplete) {
                                                                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                                                                }
                                                                if (confirmOnDelete) {
                                                                    taskToDelete = task
                                                                } else {
                                                                    isVisible = false
                                                                    scope.launch {
                                                                        kotlinx.coroutines.delay(300)
                                                                        viewModel.deleteTask(task)
                                                                        showUndoSnackbar(task)
                                                                    }
                                                                }
                                                            },
                                                            onClick = { isExpanded = !isExpanded },
                                                            categoryColors = categoryColors,
                                                            onUpdateSubtasks = { updatedSubtasks ->
                                                                viewModel.updateTask(task.withSubtasks(updatedSubtasks))
                                                            },
                                                            isSelected = selectedTaskIds.contains(task.id),
                                                            isSelectionModeActive = selectedTaskIds.isNotEmpty(),
                                                            onToggleSelect = {
                                                                if (selectedTaskIds.contains(task.id)) {
                                                                    selectedTaskIds.remove(task.id)
                                                                } else {
                                                                    selectedTaskIds.add(task.id)
                                                                }
                                                            },
                                                            longPressAction = longPressAction
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Flat list of tasks for Manual sorting mode!
                            itemsIndexed(tasks, key = { _, it -> it.id }) { index, task ->
                                var isVisible by rememberSaveable { mutableStateOf(true) }
                                var isExpanded by rememberSaveable { mutableStateOf(false) }

                                AnimatedVisibility(
                                    visible = isVisible,
                                    enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                                            expandVertically(animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)) +
                                            scaleIn(initialScale = 0.82f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)),
                                    exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                           shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                                           scaleOut(targetScale = 0.82f, animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                                    modifier = Modifier.animateItem(
                                        placementSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                    ) {
                                        TaskDetailedRow(
                                            task = task,
                                            isExpanded = isExpanded,
                                            onToggleComplete = {
                                                if (hapticFeedbackOnComplete && !task.isCompleted) {
                                                    com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                                                }
                                                if (selectedCategory != com.fabian.todolist.data.SystemCategory.COMPLETED) {
                                                    isVisible = false
                                                    scope.launch {
                                                        kotlinx.coroutines.delay(300)
                                                        viewModel.toggleTaskCompletion(task)
                                                    }
                                                } else {
                                                    viewModel.toggleTaskCompletion(task)
                                                }
                                            },
                                            onRestore = {
                                                viewModel.restoreTask(task)
                                                isVisible = false
                                            },
                                            onEdit = {
                                                taskToEdit = task
                                                showAddEditDialog = true
                                            },
                                                onDelete = {
                                                    if (hapticFeedbackOnComplete) {
                                                        com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                                                    }
                                                    if (confirmOnDelete) {
                                                        taskToDelete = task
                                                    } else {
                                                        isVisible = false
                                                        scope.launch {
                                                            kotlinx.coroutines.delay(300)
                                                            viewModel.deleteTask(task)
                                                            showUndoSnackbar(task)
                                                        }
                                                    }
                                                },
                                            onClick = { isExpanded = !isExpanded },
                                            categoryColors = categoryColors,
                                            isManualOrderEnabled = sortBy == com.fabian.todolist.data.SortOrder.MANUAL,
                                            onMoveUp = {
                                                if (index > 0) {
                                                    viewModel.reorderTasks(index, index - 1, tasks)
                                                }
                                            },
                                            onMoveDown = {
                                                if (index < tasks.size - 1) {
                                                    viewModel.reorderTasks(index, index + 1, tasks)
                                                }
                                            },
                                            onUpdateSubtasks = { updatedSubtasks ->
                                                viewModel.updateTask(task.withSubtasks(updatedSubtasks))
                                            },
                                            isSelected = selectedTaskIds.contains(task.id),
                                            isSelectionModeActive = selectedTaskIds.isNotEmpty(),
                                            onToggleSelect = {
                                                if (selectedTaskIds.contains(task.id)) {
                                                    selectedTaskIds.remove(task.id)
                                                } else {
                                                    selectedTaskIds.add(task.id)
                                                }
                                            },
                                            longPressAction = longPressAction
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsPreferencesDialog(
            currentAccent = themeAccent,
            currentDark = themeDark,
            currentLang = languageCode,
            onSaveAccent = { settingsViewModel.setThemeAccent(it) },
            onSaveDark = { settingsViewModel.setThemeDark(it) },
            onSaveLang = { lang ->
                settingsViewModel.setLanguageCode(lang)
                var currentContext = context
                while (currentContext is android.content.ContextWrapper) {
                    if (currentContext is android.app.Activity) {
                        currentContext.recreate()
                        break
                    }
                    currentContext = currentContext.baseContext
                }
            },
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            authViewModel = authViewModel,
            onNavigateToGoogleLogin = onNavigateToGoogleLogin,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showStatsDialog) {
        com.fabian.todolist.ui.components.ProductivityStatsDialog(
            tasks = tasks,
            onDismiss = { showStatsDialog = false }
        )
    }

    if (showAddEditDialog) {
        val filteredListCategories = categories.filter { it != com.fabian.todolist.data.SystemCategory.ALL_TASKS && it != com.fabian.todolist.data.SystemCategory.COMPLETED }
        val defaultOffsetMinutes by settingsViewModel.reminderOffsetMinutes.collectAsStateWithLifecycle()
        val isGeneratingAI by viewModel.isGeneratingAI.collectAsStateWithLifecycle()
        AddEditTaskDialog(
            task = taskToEdit,
            categories = if (filteredListCategories.isEmpty()) listOf("General") else filteredListCategories,
            defaultOffsetMinutes = defaultOffsetMinutes,
            categoryColors = categoryColors,
            categoryIcons = categoryIcons,
            isGeneratingAI = isGeneratingAI,
            onAddCategory = { settingsViewModel.addCategory(it) },
            onGenerateSubtasksWithAI = { title, desc, existing, cats, onSuccess, onError ->
                viewModel.generateSubtasksWithAI(
                    title, desc, existing, cats,
                    aiModel, aiApiKey, aiSubtaskCount, languageCode,
                    onSuccess, onError
                )
            },
            onSave = { finishedTask ->
                if (taskToEdit == null) {
                    viewModel.insertTask(finishedTask)
                } else {
                    viewModel.updateTask(finishedTask)
                }
                showAddEditDialog = false
            },
            onSaveBatch = { tasks ->
                viewModel.insertTasks(tasks)
                showAddEditDialog = false
            },
            onDismiss = { showAddEditDialog = false }
        )
    }

    TaskDeleteDialog(
        taskToDelete = taskToDelete,
        onDismiss = { taskToDelete = null },
        onConfirm = { 
            viewModel.deleteTask(it)
            showUndoSnackbar(it)
            taskToDelete = null
        }
    )

    com.fabian.todolist.ui.components.TaskDeleteBatchDialog(
        tasksToDelete = tasksToDeleteBatch,
        onDismiss = { tasksToDeleteBatch = null },
        onConfirm = { tasks ->
            tasks.forEach { viewModel.deleteTask(it) }
            showUndoBatchSnackbar(tasks)
            selectedTaskIds.clear()
            tasksToDeleteBatch = null
        }
    )

    TaskAddCategoryDialog(
        showDialog = showAddCategoryDialog,
        onDismiss = { showAddCategoryDialog = false },
        onConfirm = { settingsViewModel.addCategory(it) }
    )

    if (showFocusDrawer) {
        TaskSortBottomSheet(
            sortBy = sortBy,
            languageCode = languageCode,
            onSortSelected = { viewModel.setSortBy(it) },
            onDismiss = { showFocusDrawer = false }
        )
    }
}
}

