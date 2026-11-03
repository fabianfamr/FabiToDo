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
    fun provideTaskRepository(
        taskDao: TaskDao,
        @ApplicationContext context: Context,
        firestoreSyncManager: com.fabian.todolist.data.sync.FirestoreSyncManager
    ): TaskRepository {
        return TaskRepository(taskDao, context, firestoreSyncManager)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(@ApplicationContext context: Context): com.google.firebase.firestore.FirebaseFirestore {
        val databaseId = context.getString(com.fabian.todolist.R.string.firestore_database_id)
        return try {
            val app = com.google.firebase.FirebaseApp.getInstance()
            com.google.firebase.firestore.FirebaseFirestore.getInstance(app, databaseId)
        } catch (_: Exception) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
        }
    }
}
