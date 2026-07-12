package com.fabian.todolist.di

import android.content.Context
import com.fabian.todolist.data.AppDatabase
import com.fabian.todolist.data.TaskDao
import com.fabian.todolist.data.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao, @ApplicationContext context: Context): TaskRepository {
        return TaskRepository(taskDao, context)
    }

    @Provides
    @Singleton
    fun provideAuthManager(@ApplicationContext context: Context): com.fabian.todolist.data.AuthManager {
        return com.fabian.todolist.data.AuthManager(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): com.google.firebase.firestore.FirebaseFirestore {
        return com.google.firebase.firestore.FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideCloudSyncManager(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        taskDao: TaskDao,
        authManager: com.fabian.todolist.data.AuthManager
    ): com.fabian.todolist.data.CloudSyncManager {
        return com.fabian.todolist.data.CloudSyncManager(firestore, taskDao, authManager)
    }
}
