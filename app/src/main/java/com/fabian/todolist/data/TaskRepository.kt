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
    private val context: Context
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

    // Only return tasks that are not marked as deleted
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    
    suspend fun getAllTasksSync(): List<Task> = taskDao.getAllTasksSync()

    suspend fun getAllTrashedTasksSync(): List<Task> = taskDao.getAllTrashedTasksSync()

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
        return id
    }

    suspend fun update(task: Task) {
        val updatedTask = task.copy(
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        taskDao.updateTask(updatedTask)
        notifyWidget()
    }

    suspend fun updateTasks(tasks: List<Task>) {
        val updatedTasks = tasks.map { 
            it.copy(updatedAt = System.currentTimeMillis(), isSynced = false) 
        }
        taskDao.updateTasks(updatedTasks)
        notifyWidget()
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
    }

    suspend fun hardDelete(task: Task) {
        taskDao.deleteTask(task)
        notifyWidget()
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
