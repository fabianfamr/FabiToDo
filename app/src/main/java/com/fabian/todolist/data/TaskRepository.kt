package com.fabian.todolist.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.fabian.todolist.widget.TaskWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TaskRepository(
    private val taskDao: TaskDao,
    private val context: Context,
    private val firestoreSyncManager: com.fabian.todolist.data.sync.FirestoreSyncManager? = null
) {
    // Application-scoped coroutine scope for fire-and-forget widget refreshes.
    // Previously notifyWidget() spawned a brand-new CoroutineScope per write
    // call — every insert/update/delete leaked a Job + context. SupervisorJob
    // ensures a failure in one refresh doesn't cancel the scope for the next.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Singleton widget instance — instantiating a GlanceAppWidget per call is wasteful.
    private val taskWidget = TaskWidget()

    private fun notifyWidget() {
        applicationScope.launch {
            try {
                taskWidget.updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getUserIdOrNull(): String? {
        return try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    private fun syncTaskToCloud(task: Task) {
        val syncManager = firestoreSyncManager ?: return
        val userId = getUserIdOrNull() ?: return
        applicationScope.launch {
            syncManager.pushTask(userId, task)
        }
    }

    private fun syncTasksToCloud(tasks: List<Task>) {
        val syncManager = firestoreSyncManager ?: return
        val userId = getUserIdOrNull() ?: return
        applicationScope.launch {
            syncManager.pushTasks(userId, tasks)
        }
    }

    // Returns all tasks (active and trashed) so ViewModel flows can route/filter correctly
    val allTasks: Flow<List<Task>> = taskDao.getAllTasksIncludeDeleted()
    
    suspend fun getAllTasksSync(): List<Task> = taskDao.getAllTasksSync()

    suspend fun getAllTrashedTasksSync(): List<Task> = taskDao.getAllTrashedTasksSync()

    suspend fun emptyTrash() {
        val trashed = taskDao.getAllTrashedTasksSync()
        taskDao.emptyTrash()
        notifyWidget()
        val userId = getUserIdOrNull()
        if (userId != null && firestoreSyncManager != null && trashed.isNotEmpty()) {
            applicationScope.launch {
                firestoreSyncManager.deleteRemoteTasks(userId, trashed.map { it.cloudId })
            }
        }
    }

    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun insert(task: Task): Long {
        val newTask = task.copy(
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        val id = taskDao.insertTask(newTask)
        notifyWidget()
        syncTaskToCloud(newTask.copy(id = id.toInt()))
        return id
    }

    suspend fun insertTasks(tasks: List<Task>) {
        val newTasks = tasks.map {
            it.copy(
                updatedAt = if (it.updatedAt == 0L) System.currentTimeMillis() else it.updatedAt,
                isSynced = false
            )
        }
        taskDao.insertTasks(newTasks)
        notifyWidget()
        syncTasksToCloud(newTasks)
    }

    suspend fun update(task: Task) {
        val updatedTask = task.copy(
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        taskDao.updateTask(updatedTask)
        notifyWidget()
        syncTaskToCloud(updatedTask)
    }

    suspend fun updateTasks(tasks: List<Task>) {
        val updatedTasks = tasks.map { 
            it.copy(updatedAt = System.currentTimeMillis(), isSynced = false) 
        }
        taskDao.updateTasksOrder(updatedTasks)
        notifyWidget()
        syncTasksToCloud(updatedTasks)
    }

    suspend fun reorderTasksByCategory(categoryOrder: List<String>) {
        val allTasks = taskDao.getAllTasksSync()
        if (allTasks.isEmpty()) return

        val categoryRank = categoryOrder.mapIndexed { index, cat -> cat to index }.toMap()

        // Sort tasks according to the new category order while preserving relative displayOrder
        val sortedTasks = allTasks.sortedWith(
            compareBy<Task> { categoryRank[it.category] ?: Int.MAX_VALUE }
                .thenBy { it.displayOrder }
                .thenBy { it.id }
        )

        val toUpdate = mutableListOf<Task>()
        sortedTasks.forEachIndexed { newIndex, task ->
            if (task.displayOrder != newIndex) {
                toUpdate.add(task.copy(displayOrder = newIndex, updatedAt = System.currentTimeMillis(), isSynced = false))
            }
        }

        if (toUpdate.isNotEmpty()) {
            taskDao.updateTasksOrder(toUpdate)
            notifyWidget()
            syncTasksToCloud(toUpdate)
        }
    }

    suspend fun delete(task: Task) {
        // Soft delete instead of hard delete for offline-first sync
        val deletedTask = task.copy(
            isDeleted = true,
            deletedTimestamp = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        taskDao.updateTask(deletedTask)
        notifyWidget()
        syncTaskToCloud(deletedTask)
    }

    suspend fun hardDelete(task: Task) {
        taskDao.deleteTask(task)
        notifyWidget()
        val userId = getUserIdOrNull()
        if (userId != null && firestoreSyncManager != null) {
            applicationScope.launch {
                firestoreSyncManager.deleteRemoteTask(userId, task.cloudId)
            }
        }
    }

    suspend fun deleteById(id: Int) {
        // Find task and soft delete
        val task = taskDao.getTaskById(id)
        if (task != null) {
            delete(task)
        }
    }

    suspend fun getTasksWithActiveReminders(): List<Task> {
        return taskDao.getTasksWithActiveReminders()
    }
}
