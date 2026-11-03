package com.fabian.todolist.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.TaskRepository
import com.fabian.todolist.data.SystemCategory
import com.fabian.todolist.data.TaskPriority
import com.fabian.todolist.data.SortOrder
import com.fabian.todolist.data.Subtask
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.fabian.todolist.receiver.ReminderReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import java.util.Calendar
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

import kotlinx.coroutines.isActive

data class TaskUiState(
    val searchQuery: String = "",
    val selectedStatus: String = "Todas",
    val selectedCategory: String = SystemCategory.ALL_TASKS,
    val sortBy: String = SortOrder.DUE_DATE
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    private val allTasks: Flow<List<Task>> = repository.allTasks

    val allTasksListState: StateFlow<List<Task>> = allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // SharedPreferences for local settings persistence
    private val prefs = application.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val _isGeneratingAI = MutableStateFlow(false)
    val isGeneratingAI: StateFlow<Boolean> = _isGeneratingAI.asStateFlow()

    // Consolidated UI State
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    fun setSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }
    fun setSelectedStatus(status: String) { _uiState.update { it.copy(selectedStatus = status) } }
    fun setSelectedCategory(category: String) { _uiState.update { it.copy(selectedCategory = category) } }
    fun setSortBy(sorting: String) { _uiState.update { it.copy(sortBy = sorting) } }

    private val _showBackupCard = MutableStateFlow(prefs.getBoolean("show_backup_card", true))
    val showBackupCard: StateFlow<Boolean> = _showBackupCard.asStateFlow()

    private val _showAiBanner = MutableStateFlow(prefs.getBoolean("show_ai_banner", true))
    val showAiBanner: StateFlow<Boolean> = _showAiBanner.asStateFlow()

    val pendingTasksCount: StateFlow<Int> = allTasksListState.map { tasks ->
        tasks.filter { !it.isDeleted }.count { !it.isCompleted }
    }
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _shouldShowSettings = MutableStateFlow(false)
    val shouldShowSettings: StateFlow<Boolean> = _shouldShowSettings.asStateFlow()

    private val _shouldShowAddDialog = MutableStateFlow(false)
    val shouldShowAddDialog: StateFlow<Boolean> = _shouldShowAddDialog.asStateFlow()

    fun setShouldShowSettings(show: Boolean) {
        _shouldShowSettings.value = show
    }

    fun setShouldShowAddDialog(show: Boolean) {
        _shouldShowAddDialog.value = show
    }

    // Task id surfaced to the UI when the user taps a reminder notification.
    // The UI observes this and opens the task detail / scrolls to it.
    private val _pendingTaskIdFromNotification = MutableStateFlow<Int?>(null)
    val pendingTaskIdFromNotification: StateFlow<Int?> = _pendingTaskIdFromNotification.asStateFlow()

    fun setPendingTaskIdFromNotification(taskId: Int) {
        _pendingTaskIdFromNotification.value = taskId
    }

    fun consumePendingTaskIdFromNotification() {
        _pendingTaskIdFromNotification.value = null
    }

    // Synchronization
    private val _unsyncedTasksCount = MutableStateFlow(prefs.getInt("unsynced_tasks_count", 0))
    val unsyncedTasksCount: StateFlow<Int> = _unsyncedTasksCount.asStateFlow()

    private fun incrementUnsynced() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val next = _unsyncedTasksCount.updateAndGet { it + 1 }
            prefs.edit().putInt("unsynced_tasks_count", next).apply()
        }
    }

    private var isAppInForeground = false
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun onAppForegroundStateChanged(isForeground: Boolean) {
        isAppInForeground = isForeground
    }

    fun markAllSynced() {
        _unsyncedTasksCount.value = 0
        prefs.edit().putInt("unsynced_tasks_count", 0).apply()
    }

    fun generateSubtasksWithAI(
        taskTitle: String,
        taskDescription: String,
        existingSubtasksText: String?,
        categories: List<String>,
        model: String,
        apiKey: String,
        subtaskCount: Int,
        langCode: String,
        onSuccess: (description: String, category: String, priority: String, subtasks: List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isGeneratingAI.value) return
        
        viewModelScope.launch {
            _isGeneratingAI.value = true
            try {
                val key = if (apiKey.isNotBlank()) apiKey else com.fabian.todolist.BuildConfig.GEMINI_API_KEY
            
                val categoriesListStr = if (categories.isEmpty()) "[\"General\"]" else categories.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
                val languageName = if (langCode == "system") Locale.getDefault().displayLanguage else java.util.Locale.forLanguageTag(langCode).displayLanguage

                val promptText = """
                    Act as an exceptionally smart, highly organized, and deeply empathetic personal productivity strategist.
                    Task Title: "$taskTitle"
                    Task Description: "$taskDescription"
                    ${if (!existingSubtasksText.isNullOrBlank()) "The task already has these components, generate the NEXT steps: \n$existingSubtasksText" else ""}

                    Your goals:
                    1. Reason about the scope of the task. Break it down chronologically and logically into $subtaskCount highly actionable, concrete, and concise subtasks. Do NOT include any durations, times, or parenthetical estimates in the titles.
                    2. Write a clear, encouraging, and serious 1-sentence description that summarizes the task goal. At the end, append a practical tip: "Consejo: <useful advice>".
                    3. Determine the natural priority of this task (Baja / Media / Alta / Critica) based on its title and urgency keywords.
                    4. Pick the most suitable category for this task from the available categories list: $categoriesListStr. If NONE of the categories in that list naturally fit the task's theme, provide a concise and suitable new category (maximum 2 words, capitalized).

                    Analyze step-by-step and output strictly a valid RAW JSON object matching this structure. Translate values to language: $languageName.
                    
                    Expected JSON schema:
                    {
                      "description": "Clear descriptive goal with practical advice.",
                      "category": "Selected or creative category name",
                      "priority": "Baja" or "Media" or "Alta" or "Critica",
                      "subtasks": [
                        "Step 1 name",
                        "Step 2 name", ...
                      ]
                    }

                    Output ONLY the raw JSON. Absolutely no formatting, prefix, or markdown backticks!
                """.trimIndent()
                
                val useLowLatency = prefs.getBoolean("ai_low_latency_mode", true)
                val finalModel = if (useLowLatency) "gemini-3.5-flash-lite" else com.fabian.todolist.data.gemini.FirebaseAiManager.mapToSupportedModel(model)
                
                Log.d("TaskViewModel", "Calling AI with model: $finalModel (low-latency enabled: $useLowLatency)")
                val responseText = callAiApi(finalModel, key, promptText)
                
                if (responseText != null) {
                    val rawText = responseText.trim()
                    
                    // Optimization: Parse JSON in a background thread to avoid jank
                    val parsedData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        val startIndex = rawText.indexOf('{')
                        val endIndex = rawText.lastIndexOf('}')
                        
                        val cleanedJSON = if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                            rawText.substring(startIndex, endIndex + 1)
                        } else {
                            rawText.replace("```json", "").replace("```", "").trim()
                        }
                        
                        try {
                            val jsonObject = org.json.JSONObject(cleanedJSON)
                            val generatedDescription = jsonObject.optString("description", "")
                            
                            val rawCategory = jsonObject.optString("category", "General").trim()
                            val generatedCategory = if (rawCategory.length in 1..25) {
                                rawCategory.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                            } else {
                                "General"
                            }
                            
                            val rawPriority = jsonObject.optString("priority", "Media").uppercase(java.util.Locale.ROOT)
                            val generatedPriority = when {
                                rawPriority.contains("CRITIC") || rawPriority.contains("URGENT") || rawPriority.contains("CRÍT") -> TaskPriority.CRITICAL
                                rawPriority.contains("HIGH") || rawPriority.contains("ALT") -> TaskPriority.HIGH
                                rawPriority.contains("MEDIUM") || rawPriority.contains("MED") -> TaskPriority.MEDIUM
                                rawPriority.contains("LOW") || rawPriority.contains("BAJ") -> TaskPriority.LOW
                                else -> TaskPriority.MEDIUM
                            }
                            
                            val subtasksArray = jsonObject.optJSONArray("subtasks")
                            val resultList = mutableListOf<String>()
                            if (subtasksArray != null) {
                                for (i in 0 until subtasksArray.length()) {
                                    resultList.add(subtasksArray.getString(i))
                                }
                            }
                            Triple(generatedDescription, generatedCategory, Pair(generatedPriority, resultList))
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (parsedData != null) {
                        onSuccess(parsedData.first, parsedData.second, parsedData.third.first, parsedData.third.second)
                    } else {
                        onError("Invalid response format from AI")
                    }
                } else {
                    onError("No response from AI")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TaskViewModel", "AI Generation Error", e)
                onError("Error: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _isGeneratingAI.value = false
            }
        }
    }

    private suspend fun callAiApi(model: String, key: String, prompt: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val geminiModel = com.fabian.todolist.data.gemini.FirebaseAiManager.mapToSupportedModel(model)
        try {
            return@withContext com.fabian.todolist.data.gemini.FirebaseAiManager.generateContent(
                modelName = geminiModel,
                prompt = prompt,
                systemInstructionText = "You are a professional, helpful, and empathetic personal productivity strategist. You must output ONLY a valid RAW JSON object.",
                timeoutMs = 60_000L
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Firebase AI Logic call failed, attempting direct Gemini call if key available", e)
            if (key.isNotBlank() && key != "PLACEHOLDER_NOT_CONFIGURED" && key != "YOUR_GEMINI_API_KEY") {
                val request = com.fabian.todolist.data.gemini.GenerateContentRequest(
                    contents = listOf(
                        com.fabian.todolist.data.gemini.Content(
                            parts = listOf(com.fabian.todolist.data.gemini.Part(text = prompt))
                        )
                    ),
                    systemInstruction = com.fabian.todolist.data.gemini.Content(
                        parts = listOf(com.fabian.todolist.data.gemini.Part(text = "You are a professional, helpful, and empathetic personal productivity strategist. You must output ONLY a valid RAW JSON object."))
                    )
                )
                val response = com.fabian.todolist.data.gemini.RetrofitClient.service.generateContent(geminiModel, key, request)
                return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            }
            throw e
        }
    }

    fun runTrashAutoCleanup() {
        val retentionDays = prefs.getInt("trash_retention_days", 30)
        if (retentionDays <= 0) return 

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.fabian.todolist.worker.TrashCleanupWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiresCharging(true)
                    .build()
            )
            .setInputData(
                androidx.work.workDataOf(com.fabian.todolist.worker.TrashCleanupWorker.KEY_RETENTION_DAYS to retentionDays)
            )
            .build()
            
        androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "TrashCleanupWork",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    init {
        runTrashAutoCleanup()
    }

    // Combined filtered tasks
    val tasksState: StateFlow<List<Task>> = combine(
        allTasksListState,
        _uiState
    ) { tasks, state ->
        val queryLower = state.searchQuery.trim().lowercase()
        val isTrash = state.selectedCategory == SystemCategory.TRASH
        
        val filtered = tasks.filter { task ->
            // Phase 1: Basic structural filters (Trash vs Normal)
            if (isTrash) {
                if (!task.isDeleted) return@filter false
            } else {
                if (task.isDeleted) return@filter false
                
                // Category filter
                val categoryMatch = when (state.selectedCategory) {
                    SystemCategory.COMPLETED -> task.isCompleted
                    SystemCategory.ALL_TASKS,
                    SystemCategory.EISENHOWER -> !task.isCompleted
                    SystemCategory.CALENDAR -> !task.isDeleted
                    else -> task.category == state.selectedCategory && !task.isCompleted
                }
                if (!categoryMatch) return@filter false
            }

            // Phase 2: User filters (Search & Status)
            if (!isTrash) {
                val statusMatch = when (state.selectedStatus) {
                    "Pendientes" -> !task.isCompleted
                    "Completadas" -> task.isCompleted
                    else -> true
                }
                if (!statusMatch) return@filter false
            }

            if (queryLower.isNotBlank()) {
                val searchMatch = task.title.lowercase().contains(queryLower) ||
                                 task.description.lowercase().contains(queryLower)
                if (!searchMatch) return@filter false
            }

            true
        }

        // Phase 3: Sorters (done once on the already filtered sub-list)
        when (state.sortBy) {
            SortOrder.DUE_DATE -> {
                filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenBy { it.dueDate ?: Long.MAX_VALUE }
                        .thenBy { it.dueTime ?: "23:59" }
                        .thenByDescending { it.id }
                )
            }
            SortOrder.PRIORITY -> {
                val priorityWeight = mapOf(TaskPriority.CRITICAL to 4, TaskPriority.HIGH to 3, TaskPriority.MEDIUM to 2, TaskPriority.LOW to 1)
                filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenByDescending { priorityWeight[it.priority] ?: 0 }
                        .thenBy { it.dueDate ?: Long.MAX_VALUE }
                )
            }
            SortOrder.ALPHABETICAL -> {
                filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenBy { it.title.lowercase() }
                )
            }
            SortOrder.MANUAL -> {
                filtered.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenBy { it.displayOrder }
                )
            }
            else -> filtered
        }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Grouped tasks
    val groupedTasks: StateFlow<Map<String, Map<String, List<Task>>>> = tasksState.map { tasks ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterTomorrow = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterDayAfterTomorrow = cal.timeInMillis

        cal.timeInMillis = today
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        if (cal.timeInMillis < dayAfterDayAfterTomorrow) {
             cal.add(Calendar.WEEK_OF_YEAR, 1)
        }
        val endOfWeek = cal.timeInMillis

        cal.timeInMillis = today
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, 1)
        val endOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endOfNextMonth = cal.timeInMillis

        val groups = mutableMapOf<String, MutableMap<String, MutableList<Task>>>()
        
        tasks.forEach { task ->
            val due = task.dueDate
            
            val key = when {
                due == null -> "group_not_set"
                due < today -> if (task.isCompleted) "group_hoy" else "group_vencidos"
                due < tomorrow -> "group_hoy"
                due < dayAfterTomorrow -> "group_manana"
                due < dayAfterDayAfterTomorrow -> "group_pasado_manana"
                due < endOfWeek -> "group_esta_semana"
                due < endOfMonth -> "group_este_mes"
                due < endOfNextMonth -> "group_siguiente_mes"
                else -> "group_futuro"
            }
            
            val prio = task.priority
            val priorityKey = when {
                prio.equals(TaskPriority.CRITICAL, ignoreCase = true) -> TaskPriority.CRITICAL
                prio.equals(TaskPriority.HIGH, ignoreCase = true) -> TaskPriority.HIGH
                prio.equals(TaskPriority.MEDIUM, ignoreCase = true) -> TaskPriority.MEDIUM
                prio.equals(TaskPriority.LOW, ignoreCase = true) -> TaskPriority.LOW
                else -> TaskPriority.NONE
            }
            
            val dateGroupMap = groups.getOrPut(key) { mutableMapOf() }
            dateGroupMap.getOrPut(priorityKey) { mutableListOf() }.add(task)
        }
        groups
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun insertTask(task: Task) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val insertedId = repository.insert(task)
            val savedTask = task.copy(id = insertedId.toInt())
            if (savedTask.reminderTime != null && !savedTask.isCompleted) {
                scheduleReminder(savedTask)
            }
            incrementUnsynced()
        }
    }

    fun insertTasks(tasks: List<Task>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            tasks.forEach { task ->
                val insertedId = repository.insert(task)
                val savedTask = task.copy(id = insertedId.toInt())
                if (savedTask.reminderTime != null && !savedTask.isCompleted) {
                    scheduleReminder(savedTask)
                }
                incrementUnsynced()
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.update(task)
            if (task.isCompleted) {
                cancelReminder(task)
            } else if (task.reminderTime != null) {
                scheduleReminder(task)
            } else {
                cancelReminder(task)
            }
            incrementUnsynced()
        }
    }

    fun restoreTask(task: Task) {
        updateTask(task.copy(isDeleted = false))
    }

    fun emptyTrash() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val trashedTasks = repository.getAllTrashedTasksSync()
            trashedTasks.forEach { cancelReminder(it) }
            repository.emptyTrash()
            incrementUnsynced()
        }
    }

    fun deleteTask(task: Task, force: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (task.isDeleted || force) {
                cancelReminder(task)
                repository.hardDelete(task)
            } else {
                cancelReminder(task)
                updateTask(task.copy(isDeleted = true, deletedTimestamp = System.currentTimeMillis()))
            }
            incrementUnsynced()
        }
    }

    private fun computeNextRepeatTrigger(task: Task): Long? {
        if (!task.isRepeat || task.repeatType == "Ninguno") return null
        val baseline = task.dueDate ?: task.reminderTime ?: System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = baseline }
        when (task.repeatType) {
            "Diario" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "Semanal" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "Mensual" -> cal.add(Calendar.MONTH, 1)
            else -> return null
        }
        return cal.timeInMillis
    }

    fun toggleTaskCompletion(task: Task) {
        if (!task.isCompleted && task.isRepeat && task.repeatType != "Ninguno") {
            val nextDueDate = computeNextRepeatTrigger(task)
            if (nextDueDate != null) {
                val updatedTask = task.copy(
                    dueDate = nextDueDate,
                    reminderTime = if (task.reminderTime != null) nextDueDate else null,
                    isCompleted = false,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                updateTask(updatedTask)
                return
            }
        }
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        updateTask(updatedTask)
    }

    fun duplicateTask(task: Task) {
        // Use the max displayOrder in the current in-memory list + 1 so the
        // duplicated task gets a fresh slot. Previously it inherited the
        // original task's displayOrder, corrupting Manual sort (two tasks at
        // the same slot → unstable compareBy ordering).
        val nextDisplayOrder = (allTasksListState.value.maxOfOrNull { it.displayOrder } ?: -1) + 1
        val copySuffix = getApplication<Application>().getString(com.fabian.todolist.R.string.copy_suffix)
        val duplicatedTask = task.copy(
            id = 0,
            cloudId = java.util.UUID.randomUUID().toString(),
            title = "${task.title} ($copySuffix)",
            isCompleted = false,
            displayOrder = nextDisplayOrder,
            attachedImageUri = null, // do NOT clone the original's image-uri reference (orphaned file)
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        insertTask(duplicatedTask)
    }

    fun reorderTasks(fromIndex: Int, toIndex: Int, currentList: List<Task>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val list = currentList.toMutableList()
            if (fromIndex >= 0 && fromIndex < list.size && toIndex >= 0 && toIndex < list.size) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)

                // Fetch all tasks currently in Room to prevent index collisions
                val allTasks = repository.getAllTasksSync()
                val reorderedIds = list.map { it.id }.toSet()

                // Preserve base offset in the global list
                val minDisplayOrder = currentList.minOfOrNull { it.displayOrder } ?: 0
                val updatedSublist = list.mapIndexed { index, task ->
                    task.copy(displayOrder = minDisplayOrder + index)
                }
                val updatedMap = updatedSublist.associateBy { it.id }

                // Merge with global tasks and re-index sequentially
                val merged = allTasks.map { task ->
                    updatedMap[task.id] ?: task
                }.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenBy { it.displayOrder }
                        .thenBy { it.id }
                )

                val finalToUpdate = mutableListOf<Task>()
                merged.forEachIndexed { globalIndex, task ->
                    if (task.displayOrder != globalIndex || reorderedIds.contains(task.id)) {
                        finalToUpdate.add(task.copy(displayOrder = globalIndex))
                    }
                }

                if (finalToUpdate.isNotEmpty()) {
                    repository.updateTasks(finalToUpdate)
                }
                incrementUnsynced()
            }
        }
    }

    private fun scheduleReminder(task: Task) {
        val context = getApplication<Application>()
        com.fabian.todolist.util.AlarmSchedulerHelper.scheduleTaskAlarms(context, task)
    }

    private fun cancelReminder(task: Task) {
        val context = getApplication<Application>()
        com.fabian.todolist.util.AlarmSchedulerHelper.cancelTaskAlarms(context, task)
    }

    // Stream-based memory-safe backup export (prevents OutOfMemory on large databases)
    suspend fun exportBackupToStream(
        outputStream: java.io.OutputStream,
        tasks: List<Task>,
        customCategories: List<String>
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            val writer = android.util.JsonWriter(java.io.OutputStreamWriter(outputStream, Charsets.UTF_8))
            writer.setIndent("  ")
            writer.beginObject()

            // 1. Categories
            writer.name("categories")
            writer.beginArray()
            for (cat in customCategories) {
                writer.value(cat)
            }
            writer.endArray()

            // 2. Category Colors & Icons
            writer.name("categoryColors").value(prefs.getString("category_colors", "{}") ?: "{}")
            writer.name("categoryIcons").value(prefs.getString("category_icons", "{}") ?: "{}")

            // 3. Settings & Preferences
            writer.name("settings")
            writer.beginObject()
            writer.name("themeAccent").value(prefs.getString("accent_color", "system") ?: "system")
            writer.name("themeDark").value(prefs.getString("theme_mode", "system") ?: "system")
            writer.name("languageCode").value(prefs.getString("app_locale", "system") ?: "system")
            writer.name("dateFormat").value(prefs.getString("date_format", "DD/MM/AA") ?: "DD/MM/AA")
            writer.name("timeFormat").value(prefs.getString("time_format", "24h") ?: "24h")
            writer.name("globalNotificationsEnabled").value(prefs.getBoolean("global_notifications_enabled", true))
            writer.name("aiModel").value(prefs.getString("ai_model", "gemini-3.8-flash") ?: "gemini-3.8-flash")
            writer.name("aiLowLatencyMode").value(prefs.getBoolean("ai_low_latency_mode", true))
            writer.name("aiSubtaskCount").value(prefs.getInt("ai_subtask_count", 5))
            writer.name("trashRetentionDays").value(prefs.getInt("trash_retention_days", 30))
            writer.endObject()

            // 4. Tasks
            writer.name("tasks")
            writer.beginArray()
            for (task in tasks) {
                writer.beginObject()
                writer.name("title").value(task.title)
                writer.name("description").value(task.description)
                if (task.dueDate != null) writer.name("dueDate").value(task.dueDate)
                if (task.dueTime != null) writer.name("dueTime").value(task.dueTime)
                writer.name("isCompleted").value(task.isCompleted)
                writer.name("category").value(task.category)
                writer.name("priority").value(task.priority)
                if (task.reminderTime != null) writer.name("reminderTime").value(task.reminderTime)
                writer.name("isRepeat").value(task.isRepeat)
                writer.name("repeatType").value(task.repeatType)
                writer.name("isDeleted").value(task.isDeleted)
                writer.name("displayOrder").value(task.displayOrder)

                writer.name("subtasks")
                writer.beginArray()
                for (sub in task.subtasks) {
                    writer.beginObject()
                    writer.name("id").value(sub.id)
                    writer.name("title").value(sub.title)
                    writer.name("isCompleted").value(sub.isCompleted)
                    writer.endObject()
                }
                writer.endArray()

                writer.endObject()
            }
            writer.endArray()

            writer.endObject()
            writer.flush()
            true
        } catch (e: Exception) {
            if (com.fabian.todolist.BuildConfig.DEBUG) {
                Log.e("TaskViewModel", "Error streaming backup export", e)
            }
            false
        }
    }

    // Stream-based memory-safe backup import (prevents OutOfMemory and blocks on IO thread)
    suspend fun importBackupFromStream(
        inputStream: java.io.InputStream,
        onCategoriesImported: (List<String>) -> Unit = {}
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, Charsets.UTF_8))
            reader.beginObject()
            val importedTasks = mutableListOf<Task>()
            val importedCategories = mutableListOf<String>()

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "categories" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            val cat = reader.nextString().trim()
                            if (cat.isNotEmpty() && !importedCategories.contains(cat)) {
                                importedCategories.add(cat)
                            }
                        }
                        reader.endArray()
                    }
                    "categoryColors", "category_colors" -> {
                        if (reader.peek() == android.util.JsonToken.STRING) {
                            editor.putString("category_colors", reader.nextString())
                        } else {
                            reader.skipValue()
                        }
                    }
                    "categoryIcons", "category_icons" -> {
                        if (reader.peek() == android.util.JsonToken.STRING) {
                            editor.putString("category_icons", reader.nextString())
                        } else {
                            reader.skipValue()
                        }
                    }
                    "settings", "preferences" -> {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "themeAccent", "accent_color" -> editor.putString("accent_color", reader.nextString())
                                "themeDark", "theme_mode" -> editor.putString("theme_mode", reader.nextString())
                                "languageCode", "app_locale" -> editor.putString("app_locale", reader.nextString())
                                "dateFormat", "date_format" -> editor.putString("date_format", reader.nextString())
                                "timeFormat", "time_format" -> editor.putString("time_format", reader.nextString())
                                "globalNotificationsEnabled", "global_notifications_enabled" -> editor.putBoolean("global_notifications_enabled", reader.nextBoolean())
                                "aiModel", "ai_model" -> editor.putString("ai_model", reader.nextString())
                                "aiLowLatencyMode", "ai_low_latency_mode" -> editor.putBoolean("ai_low_latency_mode", reader.nextBoolean())
                                "aiSubtaskCount", "ai_subtask_count" -> editor.putInt("ai_subtask_count", reader.nextInt())
                                "trashRetentionDays", "trash_retention_days" -> editor.putInt("trash_retention_days", reader.nextInt())
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                    }
                    "tasks" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            reader.beginObject()
                            var title = ""
                            var description = ""
                            var dueDate: Long? = null
                            var dueTime: String? = null
                            var isCompleted = false
                            var category = "General"
                            var priority = TaskPriority.MEDIUM
                            var reminderTime: Long? = null
                            var isRepeat = false
                            var repeatType = "Ninguno"
                            var isDeleted = false
                            var displayOrder = 0
                            val subtasks = mutableListOf<Subtask>()

                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "title" -> title = reader.nextString()
                                    "description" -> description = reader.nextString()
                                    "dueDate" -> {
                                        if (reader.peek() == android.util.JsonToken.NULL) {
                                            reader.nextNull()
                                        } else {
                                            dueDate = reader.nextLong()
                                        }
                                    }
                                    "dueTime" -> {
                                        if (reader.peek() == android.util.JsonToken.NULL) {
                                            reader.nextNull()
                                        } else {
                                            dueTime = reader.nextString()
                                        }
                                    }
                                    "isCompleted" -> isCompleted = reader.nextBoolean()
                                    "category" -> category = reader.nextString()
                                    "priority" -> priority = reader.nextString()
                                    "reminderTime" -> {
                                        if (reader.peek() == android.util.JsonToken.NULL) {
                                            reader.nextNull()
                                        } else {
                                            reminderTime = reader.nextLong()
                                        }
                                    }
                                    "isRepeat" -> isRepeat = reader.nextBoolean()
                                    "repeatType" -> repeatType = reader.nextString()
                                    "isDeleted" -> isDeleted = reader.nextBoolean()
                                    "displayOrder" -> displayOrder = reader.nextInt()
                                    "subtasks" -> {
                                        reader.beginArray()
                                        while (reader.hasNext()) {
                                            reader.beginObject()
                                            var subId = java.util.UUID.randomUUID().toString()
                                            var subTitle = ""
                                            var subCompleted = false
                                            while (reader.hasNext()) {
                                                when (reader.nextName()) {
                                                    "id" -> subId = reader.nextString()
                                                    "title" -> subTitle = reader.nextString()
                                                    "isCompleted" -> subCompleted = reader.nextBoolean()
                                                    else -> reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                            if (subTitle.isNotBlank()) {
                                                subtasks.add(Subtask(id = subId, title = subTitle, isCompleted = subCompleted))
                                            }
                                        }
                                        reader.endArray()
                                    }
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()

                            if (title.isNotBlank()) {
                                importedTasks.add(
                                    Task(
                                        title = title,
                                        description = description,
                                        dueDate = dueDate,
                                        dueTime = dueTime,
                                        isCompleted = isCompleted,
                                        category = category,
                                        priority = priority,
                                        reminderTime = reminderTime,
                                        isRepeat = isRepeat,
                                        repeatType = repeatType,
                                        isDeleted = isDeleted,
                                        displayOrder = displayOrder,
                                        subtasks = subtasks
                                    )
                                )
                            }
                        }
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            editor.apply()

            if (importedCategories.isNotEmpty()) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onCategoriesImported(importedCategories)
                }
            }

            if (importedTasks.isNotEmpty()) {
                importedTasks.chunked(250).forEach { chunk ->
                    repository.insertTasks(chunk)
                }
                incrementUnsynced()
            }
            true
        } catch (e: Exception) {
            if (com.fabian.todolist.BuildConfig.DEBUG) {
                Log.e("TaskViewModel", "Error streaming backup import", e)
            }
            false
        }
    }

    // Export data to a custom JSON string structure
    fun exportBackupToString(tasks: List<Task>, customCategories: List<String>): String {
        return try {
            val root = JSONObject()
            
            // Serialize custom lists/categories
            val catsArray = JSONArray()
            customCategories.forEach { catsArray.put(it) }
            root.put("categories", catsArray)

            // Serialize tasks
            val tasksArray = JSONArray()
            tasks.forEach { task ->
                val tObj = JSONObject().apply {
                    put("title", task.title)
                    put("description", task.description)
                    if (task.dueDate != null) put("dueDate", task.dueDate)
                    if (task.dueTime != null) put("dueTime", task.dueTime)
                    put("isCompleted", task.isCompleted)
                    put("category", task.category)
                    put("priority", task.priority)
                    if (task.reminderTime != null) put("reminderTime", task.reminderTime)
                    put("isRepeat", task.isRepeat)
                    put("repeatType", task.repeatType)
                    put("isDeleted", task.isDeleted)
                    // We can now use the subtasks list directly
                    val subtasksArray = JSONArray()
                    task.subtasks.forEach { sub ->
                        subtasksArray.put(JSONObject().apply {
                            put("id", sub.id)
                            put("title", sub.title)
                            put("isCompleted", sub.isCompleted)
                        })
                    }
                    put("subtasks", subtasksArray)
                }
                tasksArray.put(tObj)
            }
            root.put("tasks", tasksArray)
            root.toString(2) // Pretty printed JSON
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Error exporting database", e)
            ""
        }
    }

    // Import tasks and custom categories from backup JSON string
    fun importBackupFromString(jsonStr: String, onCategoriesImported: (List<String>) -> Unit = {}): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            
            // 1. Restore categories if present
            if (root.has("categories")) {
                val catsArray = root.getJSONArray("categories")
                val list = mutableListOf<String>()
                for (i in 0 until catsArray.length()) {
                    val cat = catsArray.getString(i)
                    if (cat.isNotBlank() && !list.contains(cat)) {
                        list.add(cat)
                    }
                }
                if (list.isNotEmpty()) {
                    onCategoriesImported(list)
                }
            }

            // 2. Restore tasks if present
            if (root.has("tasks")) {
                val tasksArray = root.getJSONArray("tasks")
                val importedTasks = mutableListOf<Task>()
                for (i in 0 until tasksArray.length()) {
                    val tObj = tasksArray.getJSONObject(i)
                    val title = tObj.optString("title", "")
                    if (title.isBlank()) continue

                    val description = tObj.optString("description", "")
                    val dueDate = if (tObj.isNull("dueDate")) null else tObj.optLong("dueDate")
                    val dueTime = if (tObj.isNull("dueTime")) null else tObj.optString("dueTime")
                    val isCompleted = tObj.optBoolean("isCompleted", false)
                    val category = tObj.optString("category", "General")
                    val priority = tObj.optString("priority", TaskPriority.MEDIUM)
                    val reminderTime = if (tObj.isNull("reminderTime")) null else tObj.optLong("reminderTime")
                    val isRepeat = tObj.optBoolean("isRepeat", false)
                    val repeatType = tObj.optString("repeatType", "Ninguno")
                    val isDeleted = tObj.optBoolean("isDeleted", false)
                    
                    val subtasks = mutableListOf<Subtask>()
                    if (tObj.has("subtasks")) {
                        val subArr = tObj.getJSONArray("subtasks")
                        for (j in 0 until subArr.length()) {
                            val sObj = subArr.getJSONObject(j)
                            subtasks.add(Subtask(
                                id = sObj.optString("id", java.util.UUID.randomUUID().toString()),
                                title = sObj.getString("title"),
                                isCompleted = sObj.optBoolean("isCompleted", false)
                            ))
                        }
                    } else {
                        // Compatibility with old backup format
                        val subtasksJson = tObj.optString("subtasksJson", "[]")
                        try {
                            val arr = JSONArray(subtasksJson)
                            for (j in 0 until arr.length()) {
                                val sObj = arr.getJSONObject(j)
                                subtasks.add(Subtask(
                                    id = sObj.optString("id", java.util.UUID.randomUUID().toString()),
                                    title = sObj.getString("title"),
                                    isCompleted = sObj.optBoolean("isCompleted", false)
                                ))
                            }
                        } catch (_: Exception) {}
                    }

                    importedTasks.add(
                        Task(
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            isCompleted = isCompleted,
                            category = category,
                            priority = priority,
                            reminderTime = reminderTime,
                            isRepeat = isRepeat,
                            repeatType = repeatType,
                            isDeleted = isDeleted,
                            subtasks = subtasks
                        )
                    )
                }

                if (importedTasks.isNotEmpty()) {
                    insertTasks(importedTasks)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Import backup failed", e)
            false
        }
    }
}

