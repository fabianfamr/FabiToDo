package com.fabian.todolist.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isCompleted ASC, displayOrder ASC, dueDate ASC, id DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, displayOrder ASC, dueDate ASC, id DESC")
    fun getAllTasksIncludeDeleted(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY id DESC")
    suspend fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1 ORDER BY deletedTimestamp ASC")
    suspend fun getAllTrashedTasksSync(): List<Task>

    @Query("DELETE FROM tasks WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateTasks(tasks: List<Task>)

    @Transaction
    suspend fun updateTasksOrder(tasks: List<Task>) {
        updateTasks(tasks)
    }

    @Query("UPDATE tasks SET displayOrder = :order, updatedAt = :updatedAt, isSynced = 0 WHERE id = :id")
    suspend fun updateDisplayOrder(id: Int, order: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markTasksSynced(ids: List<Int>)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    @Query("SELECT * FROM tasks WHERE reminderTime IS NOT NULL AND isCompleted = 0 AND isDeleted = 0")
    suspend fun getTasksWithActiveReminders(): List<Task>

    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<Task>

    @Query("SELECT COUNT(*) FROM tasks WHERE isSynced = 0 AND isDeleted = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Query("SELECT * FROM tasks WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getTaskByCloudId(cloudId: String): Task?

    @Query("SELECT * FROM tasks WHERE cloudId IN (:cloudIds)")
    suspend fun getTasksByCloudIds(cloudIds: List<String>): List<Task>
}
