package com.fabian.todolist.di

import com.fabian.todolist.data.TaskRepository
import com.fabian.todolist.data.sync.FirestoreSyncManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppSingletonEntryPoint {
    fun taskRepository(): TaskRepository
    fun firestoreSyncManager(): FirestoreSyncManager
}
