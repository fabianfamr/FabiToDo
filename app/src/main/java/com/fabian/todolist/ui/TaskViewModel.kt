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
            // Atomic CAS — two concurrent increments previously could both read
            // value=5, both compute next=6, and both write 6, losing an increment.
            val next = _unsyncedTasksCount.update { it + 1 }
            prefs.edit().putInt("unsynced_tasks_count", next).apply()
            if (isAppInForeground) {
                triggerEventDrivenSync()
            }
        }
    }

    private var isAppInForeground = false
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun onAppForegroundStateChanged(isForeground: Boolean) {
        isAppInForeground = isForeground
        if (isForeground) {
            triggerEventDrivenSync()
        }
    }

    private fun triggerEventDrivenSync() {
        // Use WorkManager for all sync operations to ensure reliability and battery efficiency
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.fabian.todolist.worker.SyncWorker>()
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()
            
        androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "SyncWork_Event",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun markAllSynced() {
        _unsyncedTasksCount.value = 0
        prefs.edit().putInt("unsynced_tasks_count", 0).apply()
        triggerEventDrivenSync()
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
                // Reject blank keys AND the placeholder text used by .env.example
                // and the CI workflow when the GEMINI_API_KEY secret is not set.
                if (key.isBlank() || key == "PLACEHOLDER_NOT_CONFIGURED" || key == "YOUR_GEMINI_API_KEY") {
                    onError("API Key is missing. Please configure it in settings.")
                    return@launch
                }
            
                val categoriesListStr = if (categories.isEmpty()) "[\"General\"]" else categories.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
                val languageName = if (langCode == "system") Locale.getDefault().displayLanguage else java.util.Locale.forLanguageTag(langCode).displayLanguage

                val promptText = """
                    Act as an exceptionally smart, highly organized, and deeply empathetic personal productivity strategist.
                    Task Title: "$taskTitle"
                    Task Description: "$taskDescription"
                    ${if (!existingSubtasksText.isNullOrBlank()) "The task already has these components, generate the NEXT steps: \n$existingSubtasksText" else ""}

                    Your goals:
                    1. Reason about the scope of the task. Break it down chronologically and logically into $subtaskCount highly actionable, concrete, and concise subtasks. Do NOT include any durations, times, or parenthetical estimates in the titles.
                    2. Write a highly motivational, encouraging, and clear 1-sentence description that summarizes the task goal. At the end, append a cozy "Tip: <useful advice>" related to this specific task (e.g., "Tip: Haz esto por la mañana cuando tu energía esté alta!" or "Tip: Divide y vencerás, enfócate en la primera subtarea.").
                    3. Determine the natural priority of this task (Baja / Media / Alta / Critica) based on its title and urgency keywords (like "asap", "pronto", "urgente", "importante", "luego", "cuando pueda").
                    4. Pick the most suitable category for this task from the available categories list: $categoriesListStr. If NONE of the categories in that list naturally fit the task's theme, you MUST invent a new, highly specific, and creative category (maximum 2 words, capitalized) that perfectly represents it.

                    Analyze step-by-step and output strictly a valid RAW JSON object matching this structure. Translate values to language: $languageName.
                    
                    Expected JSON schema:
                    {
                      "description": "Revised motivational description with Tip.",
                      "category": "Selected or creative category name",
                      "priority": "Baja" or "Media" or "Alta" or "Critica",
                      "subtasks": [
                        "Step 1 name",
                        "Step 2 name", ...
                      ]
                    }

                    Output ONLY the raw JSON. Absolutely no formatting, prefix, or markdown backticks!
                """.trimIndent()
                
                // Force gemini-3.1-flash-lite-preview for ultra low-latency if preference is enabled and model is Gemini
                val useLowLatency = prefs.getBoolean("ai_low_latency_mode", true)
                val finalModel = if (useLowLatency && model.startsWith("gemini")) {
                    "gemini-3.1-flash-lite-preview"
                } else {
                    model
                }
                
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
            } catch (e: Exception) {
                Log.e("TaskViewModel", "AI Generation Error", e)
                onError("Error: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _isGeneratingAI.value = false
            }
        }
    }

    private suspend fun callAiApi(model: String, key: String, prompt: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val isGemini = model.startsWith("gemini")
        if (isGemini) {
            val request = com.fabian.todolist.data.gemini.GenerateContentRequest(
                contents = listOf(
                    com.fabian.todolist.data.gemini.Content(
                        parts = listOf(com.fabian.todolist.data.gemini.Part(text = prompt))
                    )
                ),
                systemInstruction = com.fabian.todolist.data.gemini.Content(
                    parts = listOf(com.fabian.todolist.data.gemini.Part(text = "You are a helpful AI task intelligence assistant. You must output ONLY a valid RAW JSON object."))
                )
            )
            val response = com.fabian.todolist.data.gemini.RetrofitClient.service.generateContent(model, key, request)
            return@withContext response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } else {
            val (url, body) = prepareExternalAiRequest(model, key, prompt)
            val reqBody = body.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val builder = okhttp3.Request.Builder().url(url).post(reqBody)
            
            // Set Headers
            when {
                model.contains("gpt") || model.contains("deepseek") || model.contains("llama") -> builder.addHeader("Authorization", "Bearer $key")
                model.contains("claude") -> {
                    builder.addHeader("x-api-key", key)
                    builder.addHeader("anthropic-version", "2023-06-01")
                }
            }
            builder.addHeader("Content-Type", "application/json")
            
            // Wrap in .use { } so the Response (and its underlying connection) is
            // always closed, even on exception. Previously the Response was never
            // explicitly closed, leaking a connection per AI call.
            return@withContext client.newCall(builder.build()).execute().use { result ->
                if (!result.isSuccessful) return@use null

                val respStr = result.body?.string() ?: ""
                val parsed = JSONObject(respStr)

                if (model.contains("claude")) {
                    parsed.getJSONArray("content").getJSONObject(0).getString("text")
                } else {
                    parsed.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                }
            }
        }
    }

    private fun prepareExternalAiRequest(model: String, key: String, prompt: String): Pair<String, JSONObject> {
        val body = JSONObject()
        val url = when {
            model.contains("gpt") -> "https://api.openai.com/v1/chat/completions"
            model.contains("claude") -> "https://api.anthropic.com/v1/messages"
            model.contains("deepseek") -> "https://api.deepseek.com/v1/chat/completions"
            model.contains("llama") -> "https://api.groq.com/openai/v1/chat/completions"
            else -> ""
        }

        if (model.contains("claude")) {
            body.put("model", "claude-3-5-sonnet-20241022")
            body.put("system", "Output ONLY raw JSON.")
            body.put("max_tokens", 1024)
            body.put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        } else {
            body.put("model", when(model) {
                "llama-3.3-70b" -> "llama-3.3-70b-versatile"
                else -> model
            })
            val msgs = JSONArray()
            msgs.put(JSONObject().put("role", "system").put("content", "Output ONLY raw JSON."))
            msgs.put(JSONObject().put("role", "user").put("content", prompt))
            body.put("messages", msgs)
            if (!model.contains("llama")) {
                body.put("response_format", JSONObject().put("type", "json_object"))
            }
        }
        return url to body
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
                    SystemCategory.ALL_TASKS -> !task.isCompleted
                    else -> task.category == state.selectedCategory && !task.isCompleted
                }
                if (!categoryMatch) return@filter false
            }

            // Phase 2: User filters (Search & Status)
            val statusMatch = when (state.selectedStatus) {
                "Pendientes" -> !task.isCompleted
                "Completadas" -> task.isCompleted
                else -> true
            }
            if (!statusMatch) return@filter false

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

        val groups = mutableMapOf<String, MutableMap<String, MutableList<Task>>>()
        
        tasks.forEach { task ->
            val due = task.dueDate
            
            val key = when {
                due == null -> "group_not_set"
                due < today -> "group_vencidos"
                due < tomorrow -> "group_hoy"
                due < dayAfterTomorrow -> "group_manana"
                due < dayAfterDayAfterTomorrow -> "group_pasado_manana"
                due < endOfWeek -> "group_esta_semana"
                due < endOfMonth -> "group_este_mes"
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

    fun toggleTaskCompletion(task: Task) {
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        updateTask(updatedTask)
    }

    fun duplicateTask(task: Task) {
        // Use the max displayOrder in the current in-memory list + 1 so the
        // duplicated task gets a fresh slot. Previously it inherited the
        // original task's displayOrder, corrupting Manual sort (two tasks at
        // the same slot → unstable compareBy ordering).
        val nextDisplayOrder = (allTasksListState.value.maxOfOrNull { it.displayOrder } ?: -1) + 1
        val duplicatedTask = task.copy(
            id = 0,
            cloudId = java.util.UUID.randomUUID().toString(),
            title = "${task.title} (Copia)",
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
                val updated = list.mapIndexed { index, task ->
                    task.copy(displayOrder = index)
                }
                repository.updateTasks(updated)
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

