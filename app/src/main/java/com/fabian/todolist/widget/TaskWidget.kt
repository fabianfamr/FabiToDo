package com.fabian.todolist.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

import com.fabian.todolist.MainActivity
import com.fabian.todolist.data.SystemCategory
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.TaskRepository
import com.fabian.todolist.data.TaskPriority

import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.updateAll
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import com.fabian.todolist.R

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
}

val taskIdKey = ActionParameters.Key<Int>("taskId")

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[taskIdKey] ?: return
        
        // Haptic feedback
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
        
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repo = entryPoint.taskRepository()
        val task = repo.getAllTasksSync().find { it.id == taskId } ?: return
        repo.update(task.copy(isCompleted = true))
        TaskWidget().updateAll(context)
    }
}

class TaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.taskRepository()

        val allTasks = repository.getAllTasksSync()
        // Sort by priority then by dueDate mostly
        val pendingTasks = allTasks.filter { !it.isCompleted && it.category != SystemCategory.TRASH }
            .sortedByDescending { it.priority }

        provideContent {
            TaskWidgetContent(pendingTasks)
        }
    }

    @Composable
    private fun TaskWidgetContent(tasks: List<Task>) {
        val context = LocalContext.current
        val addIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.fabian.todolist.ACTION_ADD_TASK"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val backgroundProvider = ColorProvider(day = Color(0xFFF0F4F8), night = Color(0xFF1E1E1E))
        val surfaceProvider = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF2D2D2D))
        val textPrimaryProvider = ColorProvider(day = Color(0xFF1C1B1F), night = Color(0xFFFFFFFF))
        val textSecondaryProvider = ColorProvider(day = Color(0xFF49454F), night = Color(0xFFCAC4D0))
        val primaryProvider = ColorProvider(day = Color(0xFF6750A4), night = Color(0xFFD0BCFF))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(backgroundProvider)
                .cornerRadius(16.dp)
                .padding(16.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.app_name),
                        style = TextStyle(
                            color = textPrimaryProvider,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(mainIntent))
                    )

                    // Compact "Add" Button
                    Box(
                        modifier = GlanceModifier
                            .background(primaryProvider)
                            .cornerRadius(12.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable(actionStartActivity(addIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ ${context.getString(R.string.add)}",
                            style = TextStyle(
                                color = ColorProvider(day = Color.White, night = Color(0xFF381E72)),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                if (tasks.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${context.getString(R.string.empty_state_title)} 🎉",
                            style = TextStyle(color = textSecondaryProvider, fontSize = 15.sp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent))
                    ) {
                        items(tasks.take(10)) { task ->
                            val priorityColor = when (task.priority) {
                                TaskPriority.HIGH -> ColorProvider(day = Color(0xFFB3261E), night = Color(0xFFF2B8B5))
                                TaskPriority.MEDIUM -> ColorProvider(day = Color(0xFFF29900), night = Color(0xFFFFCC80))
                                TaskPriority.LOW -> ColorProvider(day = Color(0xFF1E88E5), night = Color(0xFF90CAF9))
                                else -> ColorProvider(day = Color.Transparent, night = Color.Transparent)
                            }
                            
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(surfaceProvider)
                                    .cornerRadius(12.dp)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = GlanceModifier.fillMaxWidth()
                                ) {
                                    Image(
                                        provider = ImageProvider(R.drawable.ic_widget_circle),
                                        contentDescription = context.getString(R.string.finish),
                                        colorFilter = ColorFilter.tint(textPrimaryProvider),
                                        modifier = GlanceModifier
                                            .size(24.dp)
                                            .clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(taskIdKey to task.id)))
                                    )
                                    Spacer(modifier = GlanceModifier.width(12.dp))
                                    
                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            text = task.title,
                                            style = TextStyle(
                                                color = textPrimaryProvider,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1
                                        )
                                        if (task.dueDate != null) {
                                            Text(
                                                text = com.fabian.todolist.util.DateTimeUtils.formatDateSimpleSlashShort(task.dueDate),
                                                style = TextStyle(
                                                    color = textSecondaryProvider,
                                                    fontSize = 12.sp
                                                )
                                            )
                                        }
                                    }

                                    if (task.priority != TaskPriority.NONE) {
                                        val priorityColor = when (task.priority) {
                                            TaskPriority.HIGH -> ColorProvider(day = Color(0xFFB3261E), night = Color(0xFFF2B8B5))
                                            TaskPriority.MEDIUM -> ColorProvider(day = Color(0xFFF29900), night = Color(0xFFFFCC80))
                                            TaskPriority.LOW -> ColorProvider(day = Color(0xFF1E88E5), night = Color(0xFF90CAF9))
                                            else -> ColorProvider(day = Color.Transparent, night = Color.Transparent)
                                        }
                                        Box(
                                            modifier = GlanceModifier
                                                .size(10.dp)
                                                .background(priorityColor)
                                                .cornerRadius(5.dp)
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}