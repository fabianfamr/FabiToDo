package com.fabian.todolist.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory = uiState.selectedCategory
    val selectedStatus = uiState.selectedStatus
    val sortBy = uiState.sortBy
    val searchQuery = uiState.searchQuery
    
    val pendingTasksCount by viewModel.pendingTasksCount.collectAsStateWithLifecycle()
    val groupedTasks by viewModel.groupedTasks.collectAsStateWithLifecycle()
    var isSearchExpanded by remember { mutableStateOf(false) }

    if (selectedCategory == com.fabian.todolist.data.SystemCategory.CALENDAR ||
        selectedCategory == com.fabian.todolist.data.SystemCategory.EISENHOWER) {
        androidx.activity.compose.BackHandler {
            viewModel.setSelectedCategory(com.fabian.todolist.data.SystemCategory.ALL_TASKS)
        }
    }

    // Preferences states
    val themeAccent by settingsViewModel.themeAccent.collectAsStateWithLifecycle()
    val themeDark by settingsViewModel.themeDark.collectAsStateWithLifecycle()
    val languageCode by settingsViewModel.languageCode.collectAsStateWithLifecycle()
    val aiModel by settingsViewModel.aiModel.collectAsStateWithLifecycle()
    val aiApiKey by settingsViewModel.aiApiKey.collectAsStateWithLifecycle()
    val aiKeyType by settingsViewModel.aiKeyType.collectAsStateWithLifecycle()
    val aiSubtaskCount by settingsViewModel.aiSubtaskCount.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showStatsDialog by rememberSaveable { mutableStateOf(false) }
    var showFocusDrawer by rememberSaveable { mutableStateOf(false) }
    var showEmptyTrashDialog by rememberSaveable { mutableStateOf(false) }

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

    val prefilledTask by viewModel.prefilledTask.collectAsStateWithLifecycle()
    LaunchedEffect(shouldShowAddDialog, prefilledTask) {
        if (shouldShowAddDialog) {
            taskToEdit = prefilledTask
            showAddEditDialog = true
            viewModel.setShouldShowAddDialog(false)
            viewModel.consumePrefilledTask()
        }
    }

    val allTasksListState by viewModel.allTasksListState.collectAsStateWithLifecycle()
    val pendingTaskIdFromNotification by viewModel.pendingTaskIdFromNotification.collectAsStateWithLifecycle()
    LaunchedEffect(pendingTaskIdFromNotification, allTasksListState) {
        val targetId = pendingTaskIdFromNotification ?: return@LaunchedEffect
        val targetTask = allTasksListState.find { it.id == targetId }
        if (targetTask != null) {
            taskToEdit = targetTask
            showAddEditDialog = true
            viewModel.consumePendingTaskIdFromNotification()
        }
    }

    // Request notification permission if SDK >= 33 on arriving at main screen
    var hasPostNotificationPermission by remember {
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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = screenWidth > screenHeight
        val isTablet = screenWidth >= 600.dp || (isLandscape && screenHeight >= 600.dp)
        val isExpandedScreen = screenWidth >= 840.dp
        val isDualPane = isLandscape && screenWidth >= 780.dp
        var showCalendarPaneOnDual by rememberSaveable { mutableStateOf(true) }

        BackHandler(enabled = drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }

        BackHandler(enabled = selectedTaskIds.isNotEmpty()) {
            selectedTaskIds.clear()
        }

        BackHandler(enabled = isSearchExpanded || searchQuery.isNotEmpty()) {
            isSearchExpanded = false
            viewModel.setSearchQuery("")
        }

        BackHandler(enabled = !isDualPane && selectedCategory == com.fabian.todolist.data.SystemCategory.CALENDAR) {
            viewModel.setSelectedCategory(com.fabian.todolist.data.SystemCategory.ALL_TASKS)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isTablet) {
                    com.fabian.todolist.ui.components.TaskNavigationRail(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { cat ->
                            if (cat == com.fabian.todolist.data.SystemCategory.CALENDAR && isDualPane) {
                                showCalendarPaneOnDual = true
                                if (selectedCategory == com.fabian.todolist.data.SystemCategory.CALENDAR) {
                                    viewModel.setSelectedCategory(com.fabian.todolist.data.SystemCategory.ALL_TASKS)
                                } else {
                                    viewModel.setSelectedCategory(cat)
                                }
                            } else {
                                viewModel.setSelectedCategory(cat)
                            }
                        },
                        onOpenCategoriesDrawer = {
                            scope.launch {
                                if (drawerState.isOpen) drawerState.close() else drawerState.open()
                            }
                        },
                        onAddClick = {
                            taskToEdit = null
                            showAddEditDialog = true
                        },
                        onStatsClick = { showStatsDialog = true },
                        onSettingsClick = { showSettingsDialog = true }
                    )
                }

                Scaffold(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
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
                                        val textToShare = com.fabian.todolist.util.TaskShareUtils.formatMultipleTasksForSharing(context, selectedTasks)
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
                                    onMenuClick = {
                                        scope.launch {
                                            if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                        }
                                    },
                                    onSortClick = { showFocusDrawer = true },
                                    onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                                    onStatsClick = { showStatsDialog = true },
                                    onCalendarClick = {
                                        if (isDualPane) {
                                            showCalendarPaneOnDual = !showCalendarPaneOnDual
                                        } else {
                                            if (selectedCategory == com.fabian.todolist.data.SystemCategory.CALENDAR) {
                                                viewModel.setSelectedCategory(com.fabian.todolist.data.SystemCategory.ALL_TASKS)
                                            } else {
                                                viewModel.setSelectedCategory(com.fabian.todolist.data.SystemCategory.CALENDAR)
                                            }
                                        }
                                    },
                                    isCalendarActive = selectedCategory == com.fabian.todolist.data.SystemCategory.CALENDAR,
                                    isDualPane = isDualPane,
                                    showCalendarPane = showCalendarPaneOnDual,
                                    showNavigationIcon = !isTablet
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (!isTablet && selectedCategory != com.fabian.todolist.data.SystemCategory.TRASH) {
                            val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
                            ExtendedFloatingActionButton(
                                text = { Text(stringResource(R.string.add_task), modifier = Modifier.animateContentSize()) },
                                icon = { Icon(painterResource(com.fabian.todolist.ui.Appicons.ICON_ADD), contentDescription = stringResource(R.string.content_desc_add_tasks)) },
                                onClick = {
                                    taskToEdit = null
                                    showAddEditDialog = true
                                },
                                expanded = isFabExpanded,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(paddingValues),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Task statistics details with accurate active and completed counts
                        val (categoryTotalCount, categoryCompletedCount) = remember(allTasksListState, selectedCategory) {
                            val activeTasks = allTasksListState.filter { !it.isDeleted }
                            when (selectedCategory) {
                                com.fabian.todolist.data.SystemCategory.ALL_TASKS,
                                com.fabian.todolist.data.SystemCategory.COMPLETED,
                                com.fabian.todolist.data.SystemCategory.EISENHOWER,
                                com.fabian.todolist.data.SystemCategory.CALENDAR -> {
                                    activeTasks.size to activeTasks.count { it.isCompleted }
                                }
                                com.fabian.todolist.data.SystemCategory.TRASH -> {
                                    allTasksListState.count { it.isDeleted } to 0
                                }
                                else -> {
                                    val catTasks = activeTasks.filter { it.category == selectedCategory }
                                    catTasks.size to catTasks.count { it.isCompleted }
                                }
                            }
                        }

                        val eisenhowerContent: @Composable () -> Unit = {
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
                        }

                        val groupOrder = listOf(
                            "group_vencidos", "group_hoy",
                            "group_manana", "group_pasado_manana", "group_esta_semana", "group_este_mes", "group_next_month", "group_futuro", "group_not_set"
                        )

                        val taskListContent: @Composable () -> Unit = {
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

                                if (!isTablet && selectedCategory != com.fabian.todolist.data.SystemCategory.TRASH && selectedCategory != com.fabian.todolist.data.SystemCategory.EISENHOWER) {
                                    com.fabian.todolist.ui.components.TaskViewToggle(
                                        selectedCategory = selectedCategory,
                                        onViewChange = { viewModel.setSelectedCategory(it) }
                                    )
                                }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("task_list"),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
                    ) {
                        item {
                            if (selectedCategory == com.fabian.todolist.data.SystemCategory.TRASH) {
                                com.fabian.todolist.ui.components.TrashBanner(
                                    hasTasks = tasks.isNotEmpty(),
                                    onEmptyTrash = { showEmptyTrashDialog = true }
                                )
                            } else {
                                if (categoryTotalCount > 0) {
                                    com.fabian.todolist.ui.components.TaskProgressHeaderCard(
                                        totalCount = categoryTotalCount,
                                        completedCount = categoryCompletedCount,
                                        categoryName = selectedCategory,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }

                        if (selectedCategory == com.fabian.todolist.data.SystemCategory.TRASH && tasks.isEmpty()) {
                            item {
                                com.fabian.todolist.ui.components.EmptyTrashState()
                            }
                        }

                        if (sortBy != com.fabian.todolist.data.SortOrder.MANUAL) {
                            groupOrder.forEach { groupKey ->
                                val tasksInGroup = groupedTasks[groupKey] ?: emptyMap()
                                if (tasksInGroup.isNotEmpty()) {
                                    item {
                                        com.fabian.todolist.ui.components.TaskGroupHeader(groupKey = groupKey)
                                    }

                                    val priorityKeys = listOf(com.fabian.todolist.data.TaskPriority.CRITICAL, com.fabian.todolist.data.TaskPriority.HIGH, com.fabian.todolist.data.TaskPriority.MEDIUM, com.fabian.todolist.data.TaskPriority.LOW, com.fabian.todolist.data.TaskPriority.NONE)
                                    priorityKeys.forEach { prio ->
                                        val tasksOfPrio = tasksInGroup[prio] ?: emptyList()
                                        if (tasksOfPrio.isNotEmpty()) {
                                            item {
                                                com.fabian.todolist.ui.components.TaskPriorityHeader(priority = prio)
                                            }

                                            itemsIndexed(tasksOfPrio, key = { _, it -> it.id }) { index, task ->
                                                var isVisible by remember(task.id, task.isCompleted, selectedCategory) { mutableStateOf(true) }
                                                var isExpanded by rememberSaveable(task.id) { mutableStateOf(false) }

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
                                                                if (selectedCategory != com.fabian.todolist.data.SystemCategory.COMPLETED && selectedCategory != com.fabian.todolist.data.SystemCategory.TRASH) {
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
                                var isVisible by remember(task.id, task.isCompleted, selectedCategory) { mutableStateOf(true) }
                                var isExpanded by rememberSaveable(task.id) { mutableStateOf(false) }

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
                                                if (selectedCategory != com.fabian.todolist.data.SystemCategory.COMPLETED && selectedCategory != com.fabian.todolist.data.SystemCategory.TRASH) {
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

        val calendarPaneContent: @Composable (Boolean) -> Unit = { dual ->
            com.fabian.todolist.ui.components.TaskCalendarPane(
                tasks = tasks,
                categoryColors = categoryColors,
                onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                onEditTask = {
                    taskToEdit = it
                    showAddEditDialog = true
                },
                onDeleteTask = { task ->
                    if (confirmOnDelete) {
                        taskToDelete = task
                    } else {
                        viewModel.deleteTask(task)
                        showUndoSnackbar(task)
                    }
                },
                onAddTaskForDate = { dateMillis ->
                    taskToEdit = Task(
                        title = "",
                        description = "",
                        category = if (selectedCategory in listOf(
                                com.fabian.todolist.data.SystemCategory.ALL_TASKS,
                                com.fabian.todolist.data.SystemCategory.COMPLETED,
                                com.fabian.todolist.data.SystemCategory.TRASH,
                                com.fabian.todolist.data.SystemCategory.EISENHOWER,
                                com.fabian.todolist.data.SystemCategory.CALENDAR
                            )) "General" else selectedCategory,
                        dueDate = dateMillis
                    )
                    showAddEditDialog = true
                },
                isDualPane = dual,
                onBackToTasks = {
                    viewModel.setSelectedCategory(com.fabian.todolist.data.SystemCategory.ALL_TASKS)
                }
            )
        }

        if (isDualPane && showCalendarPaneOnDual && selectedCategory != com.fabian.todolist.data.SystemCategory.CALENDAR) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                ) {
                    if (selectedCategory == com.fabian.todolist.data.SystemCategory.EISENHOWER) {
                        eisenhowerContent()
                    } else if (tasks.isEmpty() && selectedCategory != com.fabian.todolist.data.SystemCategory.TRASH) {
                        TaskEmptyState(
                            onAddTask = {
                                taskToEdit = null
                                showAddEditDialog = true
                            },
                            onOpenCategories = { scope.launch { drawerState.open() } },
                            onOpenStats = { showStatsDialog = true }
                        )
                    } else {
                        taskListContent()
                    }
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Box(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                ) {
                    calendarPaneContent(true)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (selectedCategory != com.fabian.todolist.data.SystemCategory.EISENHOWER &&
                            selectedCategory != com.fabian.todolist.data.SystemCategory.CALENDAR &&
                            isTablet
                        ) {
                            Modifier.widthIn(max = if (isExpandedScreen) 920.dp else 740.dp)
                        } else {
                            Modifier
                        }
                    )
            ) {
                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.98f, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.8f))) togetherWith
                        (fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.98f, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.8f)))
                    },
                    label = "CategoryViewTransition",
                    modifier = Modifier.fillMaxSize()
                ) { currentCategory ->
                    if (currentCategory == com.fabian.todolist.data.SystemCategory.CALENDAR) {
                        calendarPaneContent(false)
                    } else if (currentCategory == com.fabian.todolist.data.SystemCategory.EISENHOWER) {
                        eisenhowerContent()
                    } else if (tasks.isEmpty() && currentCategory != com.fabian.todolist.data.SystemCategory.TRASH) {
                        TaskEmptyState(
                            onAddTask = {
                                taskToEdit = null
                                showAddEditDialog = true
                            },
                            onOpenCategories = { scope.launch { drawerState.open() } },
                            onOpenStats = { showStatsDialog = true }
                        )
                    } else {
                        taskListContent()
                    }
                }
            }
        }
    }

        // Quick Access Drawer Overlay (Does NOT close when tapping outside space)
        androidx.compose.animation.AnimatedVisibility(
            visible = drawerState.isOpen,
            enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.8f)) + androidx.compose.animation.fadeIn(animationSpec = tween(250)),
            exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.8f)) + androidx.compose.animation.fadeOut(animationSpec = tween(200))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                TaskDrawerContent(
                    drawerState = drawerState,
                    selectedCategory = selectedCategory,
                    categories = categories,
                    categoryColors = categoryColors,
                    categoryIcons = categoryIcons,
                    languageCode = languageCode,
                    onCategorySelected = { viewModel.setSelectedCategory(it) },
                    onSettingsSelected = { showSettingsDialog = true },
                    showOnlyCategories = isTablet,
                    onAddCategoryClick = { showAddCategoryDialog = true },
                    onStatsClick = { showStatsDialog = true }
                )

                // Translucent backdrop covering remaining space on right
                // Tapping this empty space DOES NOT close the menu, fulfilling user's quick access requirement!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            scope.launch { drawerState.close() }
                        }
                )
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
        val defaultCategoryForNewTask = if (selectedCategory in listOf(
                com.fabian.todolist.data.SystemCategory.ALL_TASKS,
                com.fabian.todolist.data.SystemCategory.COMPLETED,
                com.fabian.todolist.data.SystemCategory.TRASH,
                com.fabian.todolist.data.SystemCategory.EISENHOWER,
                com.fabian.todolist.data.SystemCategory.CALENDAR
            )) "General" else selectedCategory

        AddEditTaskDialog(
            task = taskToEdit,
            categories = if (filteredListCategories.isEmpty()) listOf("General") else filteredListCategories,
            initialCategory = defaultCategoryForNewTask,
            defaultOffsetMinutes = defaultOffsetMinutes,
            categoryColors = categoryColors,
            categoryIcons = categoryIcons,
            isGeneratingAI = isGeneratingAI,
            onAddCategory = { settingsViewModel.addCategory(it) },
            onGenerateSubtasksWithAI = { title, desc, existing, cats, onSuccess, onError ->
                viewModel.generateSubtasksWithAI(
                    title, desc, existing, cats,
                    aiModel, if (aiKeyType == "public") "" else aiApiKey, aiSubtaskCount, languageCode,
                    onSuccess, onError
                )
            },
            onSave = { finishedTask ->
                if (taskToEdit == null || taskToEdit?.id == 0) {
                    viewModel.insertTask(finishedTask)
                } else {
                    viewModel.updateTask(finishedTask)
                }
                if (searchQuery.isNotEmpty()) {
                    viewModel.setSearchQuery("")
                }
                showAddEditDialog = false
            },
            onSaveBatch = { tasks ->
                viewModel.insertTasks(tasks)
                if (searchQuery.isNotEmpty()) {
                    viewModel.setSearchQuery("")
                }
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
        onConfirm = { settingsViewModel.addCategory(it) },
        onConfirmWithCustomization = { name, color, icon -> settingsViewModel.addCategory(name, color, icon) }
    )

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.empty_trash_confirm_title)) },
            text = { Text(stringResource(R.string.empty_trash_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        if (hapticFeedbackOnComplete) {
                            com.fabian.todolist.util.HapticUtil.performActionHaptic(localView)
                        }
                        viewModel.emptyTrash()
                        selectedTaskIds.clear()
                        showEmptyTrashDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.trash_cleared))
                        }
                    },
                    modifier = Modifier.testTag("confirm_empty_trash_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.empty_trash_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

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

