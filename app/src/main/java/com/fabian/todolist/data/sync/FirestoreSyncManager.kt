package com.fabian.todolist.data.sync

import android.content.Context
import android.util.Log
import com.fabian.todolist.data.Subtask
import com.fabian.todolist.data.SystemCategory
import com.fabian.todolist.data.Task
import com.fabian.todolist.data.TaskDao
import com.fabian.todolist.data.subtasksToJson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

/**
 * Sincronización de tareas y ajustes con Firestore.
 *
 * CAMBIOS CLAVE RESPECTO A LA VERSIÓN ANTERIOR (fallos de sincronización de ajustes):
 *
 *  1. `updatedAt` ahora usa [FieldValue.serverTimestamp] (reloj del SERVIDOR) en lugar del
 *     reloj de cada dispositivo. Antes, con dos móviles con relojes desfasados, el dispositivo
 *     con el reloj adelantado ganaba SIEMPRE y los cambios del otro se revertían solos.
 *
 *  2. Ya no se suben los ajustes a ciegas al iniciar sesión/abrir la app. Antes,
 *     `startRealtimeSync()` lanzaba `pushUserSettings()` en paralelo a la descarga inicial:
 *     si el push ganaba la carrera, los ajustes de la nube (más nuevos) se machacaban con los
 *     locales (o con los valores por defecto en una instalación nueva). Ahora primero se
 *     descarga (`reconcileSettings`) y solo se sube si hay ediciones locales pendientes.
 *
 *  3. El marcador "hay ediciones locales sin subir" (`last_settings_local_update`) ahora
 *     SOLO lo escribe una edición real del usuario (`scheduleSettingsPush`). Antes también lo
 *     escribían push/apply, destruyendo la noción de "pendiente" y provocando reversiones.
 *
 *  4. `deviceId` por instalación para reconocer nuestros propios ecos y un debounce de 400 ms
 *     para que cambiar varios ajustes seguidos no genere N escrituras en Firestore.
 *
 *  5. Lecturas defensivas por campo: si un campo llegó con un tipo inesperado (p. ej. de una
 *     versión antigua de la app), se ignora ese campo en vez de abortar TODA la aplicación
 *     de ajustes remotos.
 *
 *  6. `syncAllNow` ya no se cuelga indefinidamente sin conexión (timeout: la escritura queda
 *     encolada por Firestore y se envía al reconectar).
 */
@Singleton
class FirestoreSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirestoreSyncManager"
        private const val PREFS_NAME = "fabitodo_preferences"

        /** Marca de "el usuario tiene ediciones locales sin subir" (reloj LOCAL). */
        private const val KEY_DIRTY = "last_settings_local_update"

        /** Marca de tiempo del SERVIDOR de la última versión remota vista/aplicada. */
        private const val KEY_LAST_SEEN = "last_settings_cloud_sync"

        /** Desfase estimado local vs servidor (serverTime ≈ localTime + offset). */
        private const val KEY_CLOCK_OFFSET = "settings_clock_offset"

        /** Identificador estable por instalación para reconocer ecos propios. */
        private const val KEY_DEVICE_ID = "settings_device_id"

        private const val PUSH_DEBOUNCE_MS = 400L
        private const val SYNC_TIMEOUT_MS = 20_000L

        /** Tolerancia ante desfases residuales al resolver un conflicto simultáneo. */
        private const val CONFLICT_GRACE_MS = 2_000L
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val settingsPushMutex = Mutex()
    private var settingsPushJob: Job? = null
    private var tasksListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var currentActiveUserId: String? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong("last_firestore_sync_time", 0L)
    )
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    // ------------------------------------------------------------------
    // Sincronización en tiempo real
    // ------------------------------------------------------------------

    fun startRealtimeSync(userId: String) {
        if (userId.isBlank()) return
        synchronized(this) {
            if (currentActiveUserId == userId && tasksListener != null) return
            stopRealtimeSyncInternal()
            currentActiveUserId = userId

            // 1. Escuchar cambios en tareas
            val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
            tasksListener = tasksCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Tasks SnapshotListener error", error)
                    _syncStatus.value = SyncStatus.ERROR
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    syncScope.launch {
                        try {
                            for (doc in snapshot.documents) {
                                val remoteTask = parseDocumentToTask(doc) ?: continue
                                val localTask = taskDao.getTaskByCloudId(remoteTask.cloudId)

                                if (localTask == null) {
                                    if (!remoteTask.isDeleted) {
                                        taskDao.insertTask(remoteTask.copy(id = 0, isSynced = true))
                                    }
                                } else {
                                    if (remoteTask.isDeleted) {
                                        if (!localTask.isDeleted) {
                                            taskDao.updateTask(localTask.copy(isDeleted = true, deletedTimestamp = remoteTask.deletedTimestamp, isSynced = true))
                                        } else {
                                            taskDao.markTasksSynced(listOf(localTask.id))
                                        }
                                    } else if (remoteTask.updatedAt > localTask.updatedAt) {
                                        taskDao.updateTask(remoteTask.copy(id = localTask.id, isSynced = true))
                                    } else if (localTask.updatedAt > remoteTask.updatedAt && !localTask.isSynced) {
                                        uploadTaskToFirestore(userId, localTask)
                                    } else if (!localTask.isSynced && remoteTask.updatedAt >= localTask.updatedAt) {
                                        taskDao.markTasksSynced(listOf(localTask.id))
                                    }
                                }
                            }

                            updateLastSyncTimestamp()
                            _syncStatus.value = SyncStatus.SUCCESS
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing remote task snapshot", e)
                            _syncStatus.value = SyncStatus.ERROR
                        }
                    }
                }
            }

            // 2. Escuchar cambios en categorías y ajustes.
            //    - Se ignoran los snapshots con escrituras pendientes propias (eco local).
            //    - Se ignoran los documentos cuyo deviceId sea el nuestro (escritura propia
            //      encolada sin conexión y confirmada más tarde por el servidor).
            val settingsDoc = firestore.collection("users").document(userId).collection("settings").document("preferences")
            settingsListener = settingsDoc.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Settings SnapshotListener error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener
                if (snapshot.getString("deviceId") == getDeviceId()) return@addSnapshotListener
                syncScope.launch {
                    applyRemoteSettings(snapshot)
                }
            }
        }

        // 3. Subir tareas no sincronizadas y RECONCILIAR los ajustes.
        //    Importante: nunca se suben los ajustes a ciegas aquí; primero se descarga
        //    la versión remota y se decide (aplicar la remota o subir la local si hay
        //    ediciones pendientes).
        syncScope.launch {
            uploadUnsyncedLocalTasks(userId)
            normalizeLegacySettingsTimestamps()
            reconcileSettings(userId)
        }
    }

    fun stopRealtimeSync() {
        synchronized(this) { stopRealtimeSyncInternal() }
    }

    private fun stopRealtimeSyncInternal() {
        tasksListener?.remove()
        tasksListener = null
        settingsListener?.remove()
        settingsListener = null
        currentActiveUserId = null
        _syncStatus.value = SyncStatus.IDLE
    }

    // ------------------------------------------------------------------
    // Sincronización manual completa ("Sincronizar ahora")
    // ------------------------------------------------------------------

    suspend fun syncAllNow(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        _syncStatus.value = SyncStatus.SYNCING

        try {
            // 1. Subir tareas pendientes
            uploadUnsyncedLocalTasks(userId)

            // 2. Ajustes: descargar y decidir (aplicar la remota o subir la local).
            //    Con timeout: sin conexión, Firestore deja la escritura en su cola local
            //    y la envía al reconectar, así que no bloqueamos el resto de la sincronización.
            normalizeLegacySettingsTimestamps()
            try {
                val timedOut = withTimeoutOrNull(SYNC_TIMEOUT_MS) {
                    val settingsDocRef = firestore.collection("users").document(userId).collection("settings").document("preferences")
                    val settingsSnap = settingsDocRef.get().await()
                    if (settingsSnap.exists() && !settingsSnap.metadata.hasPendingWrites()) {
                        applyRemoteSettings(settingsSnap)
                    } else if (!settingsSnap.exists()) {
                        pushUserSettings(userId)
                    } else {
                        // Hay una escritura propia en cola (sin conexión): ya está en curso.
                        Unit
                    }
                }
                if (timedOut == null) {
                    Log.w(TAG, "Ajustes: envío pendiente por falta de conexión; se completará al reconectar")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ajustes: no se pudieron sincronizar ahora (${e.message}); el listener reintentará")
            }

            // 3. Descargar todas las tareas remotas
            val tasksCollection = firestore.collection("users").document(userId).collection("tasks")
            val remoteDocs = tasksCollection.get().await()

            for (doc in remoteDocs.documents) {
                val remoteTask = parseDocumentToTask(doc) ?: continue
                val localTask = taskDao.getTaskByCloudId(remoteTask.cloudId)

                if (localTask == null) {
                    if (!remoteTask.isDeleted) {
                        taskDao.insertTask(remoteTask.copy(id = 0, isSynced = true))
                    }
                } else {
                    if (remoteTask.isDeleted) {
                        if (!localTask.isDeleted) {
                            taskDao.updateTask(localTask.copy(isDeleted = true, deletedTimestamp = remoteTask.deletedTimestamp, isSynced = true))
                        } else {
                            taskDao.markTasksSynced(listOf(localTask.id))
                        }
                    } else if (remoteTask.updatedAt > localTask.updatedAt) {
                        taskDao.updateTask(remoteTask.copy(id = localTask.id, isSynced = true))
                    } else if (localTask.updatedAt > remoteTask.updatedAt && !localTask.isSynced) {
                        uploadTaskToFirestore(userId, localTask)
                    } else if (!localTask.isSynced && remoteTask.updatedAt >= localTask.updatedAt) {
                        taskDao.markTasksSynced(listOf(localTask.id))
                    }
                }
            }

            updateLastSyncTimestamp()
            _syncStatus.value = SyncStatus.SUCCESS
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing syncAllNow", e)
            _syncStatus.value = SyncStatus.ERROR
            false
        }
    }

    // ------------------------------------------------------------------
    // Tareas
    // ------------------------------------------------------------------

    suspend fun pushTask(userId: String, task: Task) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        try {
            uploadTaskToFirestore(userId, task)
            taskDao.markTasksSynced(listOf(task.id))
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing task ${task.cloudId}", e)
        }
    }

    suspend fun pushTasks(userId: String, tasks: List<Task>) = withContext(Dispatchers.IO) {
        if (userId.isBlank() || tasks.isEmpty()) return@withContext
        try {
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection("tasks")
            tasks.forEach { task ->
                val docRef = collection.document(task.cloudId)
                batch.set(docRef, taskToFirestoreMap(task), SetOptions.merge())
            }
            batch.commit().await()
            taskDao.markTasksSynced(tasks.map { it.id })
            updateLastSyncTimestamp()
        } catch (e: Exception) {
            Log.e(TAG, "Error batch pushing tasks", e)
        }
    }

    suspend fun deleteRemoteTask(userId: String, cloudId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank() || cloudId.isBlank()) return@withContext
        try {
            firestore.collection("users").document(userId).collection("tasks").document(cloudId).delete().await()
            updateLastSyncTimestamp()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting remote task $cloudId", e)
        }
    }

    suspend fun deleteRemoteTasks(userId: String, cloudIds: List<String>) = withContext(Dispatchers.IO) {
        val validIds = cloudIds.filter { it.isNotBlank() }
        if (userId.isBlank() || validIds.isEmpty()) return@withContext
        try {
            val collection = firestore.collection("users").document(userId).collection("tasks")
            validIds.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { id ->
                    batch.delete(collection.document(id))
                }
                batch.commit().await()
            }
            updateLastSyncTimestamp()
        } catch (e: Exception) {
            Log.e(TAG, "Error batch deleting remote tasks", e)
        }
    }

    // ------------------------------------------------------------------
    // Ajustes: subida
    // ------------------------------------------------------------------

    /**
     * Marca que hay ajustes locales sin subir y programa un push con debounce,
     * de modo que N cambios seguidos del usuario produzcan una sola escritura.
     *
     * Es la única función que escribe [KEY_DIRTY]: el marcador de "ediciones pendientes"
     * refleja exclusivamente ediciones del usuario, nunca operaciones de sincronización.
     */
    fun scheduleSettingsPush() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_DIRTY, System.currentTimeMillis()).commit()
        settingsPushJob?.cancel()
        settingsPushJob = syncScope.launch {
            delay(PUSH_DEBOUNCE_MS)
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            pushUserSettings(uid)
        }
    }

    suspend fun pushUserSettings(userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: "") = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        settingsPushMutex.withLock {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val dirtyAtPush = prefs.getLong(KEY_DIRTY, 0L)
            try {
                // Auto-populate del nombre visible desde FirebaseAuth si no hay valor local
                val currentUser = FirebaseAuth.getInstance().currentUser
                val currentNameInPrefs = prefs.getString("user_display_name", "") ?: ""
                val resolvedName = if (currentNameInPrefs.isNotBlank()) {
                    currentNameInPrefs
                } else {
                    currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: ""
                }
                if (resolvedName.isNotBlank() && currentNameInPrefs.isBlank()) {
                    prefs.edit().putString("user_display_name", resolvedName).apply()
                }

                // Normalización de la lista de categorías (mismo criterio que la UI)
                val rawCatsJson = prefs.getString("custom_categories", null)
                val parsedList = parseCategoryList(rawCatsJson ?: "")
                val userCats = parsedList.filter { !SystemCategory.isSystemCategory(it) }
                val finalUserCats = if (rawCatsJson != null) {
                    userCats
                } else {
                    listOf("Personal", "Trabajo", "Compras", "Hogar", "Salud")
                }
                val safeCategoriesList = listOf(SystemCategory.ALL_TASKS) + finalUserCats + listOf(SystemCategory.COMPLETED)
                val jsonArray = JSONArray()
                safeCategoriesList.forEach { jsonArray.put(it) }
                val finalCatsJson = jsonArray.toString()
                prefs.edit().putString("custom_categories", finalCatsJson).apply()

                val settingsMap = buildSettingsMap(prefs, resolvedName, finalCatsJson).toMutableMap()
                settingsMap["deviceId"] = getDeviceId()
                // Reloj del SERVIDOR: una sola fuente de verdad para todos los dispositivos.
                settingsMap["updatedAt"] = FieldValue.serverTimestamp()

                firestore.collection("users").document(userId).collection("settings").document("preferences")
                    .set(settingsMap, SetOptions.merge()).await()

                // El servidor confirmó la escritura: registramos la versión del servidor
                // y el desfase estimado entre relojes para futuras comparaciones.
                try {
                    val snap = firestore.collection("users").document(userId).collection("settings").document("preferences").get().await()
                    val serverTime = readLongField(snap, "updatedAt")
                    if (serverTime != null && serverTime > 0L) {
                        prefs.edit()
                            .putLong(KEY_LAST_SEEN, serverTime)
                            .putLong(KEY_CLOCK_OFFSET, serverTime - System.currentTimeMillis())
                            .apply()
                    }
                } catch (_: Exception) {
                }

                // Limpiamos el marcador de ediciones pendientes SOLO si el usuario no
                // volvió a editar mientras subíamos (si editó, su push lo limpiará).
                if (prefs.getLong(KEY_DIRTY, 0L) == dirtyAtPush) {
                    prefs.edit().remove(KEY_DIRTY).apply()
                }
                updateLastSyncTimestamp()
            } catch (e: Exception) {
                // Sin conexión: Firestore mantiene la escritura en su cola local y la
                // enviará al reconectar. NO borramos el marcador de ediciones pendientes.
                Log.e(TAG, "Error pushing user settings", e)
            }
        }
    }

    // ------------------------------------------------------------------
    // Ajustes: bajada / resolución de conflictos
    // ------------------------------------------------------------------

    /**
     * Descarga la versión remota de los ajustes y decide:
     *  - Si hay ediciones locales más recientes que la remota → se sube lo local.
     *  - Si no → se aplica la versión remota.
     */
    private suspend fun reconcileSettings(userId: String) {
        try {
            val docRef = firestore.collection("users").document(userId).collection("settings").document("preferences")
            val snap = docRef.get().await()
            if (snap.exists() && !snap.metadata.hasPendingWrites()) {
                applyRemoteSettings(snap)
            } else if (!snap.exists()) {
                // Primera sincronización de la cuenta: no hay ajustes en la nube.
                pushUserSettings(userId)
            }
            // Si existe pero tiene escrituras pendientes propias: ya hay un push en cola.
        } catch (e: Exception) {
            // Sin conexión o reglas de seguridad denegando: el listener reintentará.
            Log.w(TAG, "No se pudo reconciliar los ajustes remotos: ${e.message}")
        }
    }

    private suspend fun applyRemoteSettings(doc: DocumentSnapshot) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val remoteUpdatedAt = readLongField(doc, "updatedAt") ?: return
            if (remoteUpdatedAt <= 0L) return

            val lastSeen = prefs.getLong(KEY_LAST_SEEN, 0L)
            val dirtyAt = prefs.getLong(KEY_DIRTY, 0L)
            val clockOffset = prefs.getLong(KEY_CLOCK_OFFSET, 0L)

            // 0) Versión ya vista o aplicada (eco) → nada que hacer.
            if (remoteUpdatedAt <= lastSeen) return

            // 1) ¿El usuario tiene ediciones locales SIN SUBIR más recientes que la
            //    versión remota entrante? (marca local corregida con el desfase estimado
            //    frente al reloj del servidor). Si sí: gana lo local y se sube.
            if (dirtyAt > 0L) {
                val dirtyServerTime = dirtyAt + clockOffset
                if (dirtyServerTime + CONFLICT_GRACE_MS > remoteUpdatedAt) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (!uid.isNullOrBlank()) {
                        pushUserSettings(uid)
                    }
                    return
                }
            }

            // 2) La versión remota gana: se aplican todos los campos.
            val editor = prefs.edit()

            val localCats = prefs.getString("custom_categories", "") ?: ""
            readStringField(doc, "customCategories")?.let { remoteCats ->
                editor.putString("custom_categories", mergeCategoriesJson(localCats, remoteCats))
            }

            val localColors = prefs.getString("category_colors", "{}") ?: "{}"
            readStringField(doc, "categoryColors")?.let { remoteColors ->
                editor.putString("category_colors", mergeJsonObjectStrings(localColors, remoteColors))
            }

            val localIcons = prefs.getString("category_icons", "{}") ?: "{}"
            readStringField(doc, "categoryIcons")?.let { remoteIcons ->
                editor.putString("category_icons", mergeJsonObjectStrings(localIcons, remoteIcons))
            }

            readStringField(doc, "themeAccent")?.let { editor.putString("accent_color", it) }
            readStringField(doc, "themeDark")?.let { editor.putString("theme_mode", it) }
            readStringField(doc, "languageCode")?.let { editor.putString("app_locale", it) }
            readStringField(doc, "dateFormat")?.let { editor.putString("date_format", it) }
            readStringField(doc, "timeFormat")?.let { editor.putString("time_format", it) }
            readStringField(doc, "longPressAction")?.let { editor.putString("long_press_action", it) }
            readBoolField(doc, "confirmOnDelete")?.let { editor.putBoolean("confirm_on_delete", it) }
            readBoolField(doc, "hapticFeedbackOnComplete")?.let { editor.putBoolean("haptic_feedback_on_complete", it) }
            readBoolField(doc, "requireBiometrics")?.let { editor.putBoolean("require_biometrics", it) }
            readBoolField(doc, "secureScreenMode")?.let { editor.putBoolean("secure_screen_mode", it) }
            readBoolField(doc, "encryptBackups")?.let { editor.putBoolean("encrypt_backups", it) }
            readBoolField(doc, "globalNotificationsEnabled")?.let { editor.putBoolean("global_notifications_enabled", it) }
            readBoolField(doc, "quickAddNotificationEnabled")?.let { editor.putBoolean("quick_add_notification_enabled", it) }
            readStringField(doc, "quickAddNotificationInterval")?.let { editor.putString("quick_add_notification_interval", it) }
            readBoolField(doc, "quickAddQuietHoursEnabled")?.let { editor.putBoolean("quick_add_quiet_hours_enabled", it) }
            readBoolField(doc, "notificationsSound")?.let { editor.putBoolean("notifications_sound", it) }
            readBoolField(doc, "notificationsVibrate")?.let { editor.putBoolean("notifications_vibrate", it) }
            readLongField(doc, "reminderOffsetMinutes")?.let { editor.putInt("reminder_offset_minutes", it.toInt()) }
            readLongField(doc, "snoozeDurationMinutes")?.let { editor.putInt("snooze_duration_minutes", it.toInt()) }
            readStringField(doc, "aiModel")?.let { editor.putString("ai_model", it) }
            readBoolField(doc, "aiLowLatencyMode")?.let { editor.putBoolean("ai_low_latency_mode", it) }
            readLongField(doc, "aiSubtaskCount")?.let { editor.putInt("ai_subtask_count", it.toInt()) }
            readStringField(doc, "aiKeyType")?.let { editor.putString("ai_key_type", it) }
            // Ajuste antes no sincronizado: la clave de API propia de Gemini.
            readStringField(doc, "aiApiKey")?.let { editor.putString("ai_api_key", it) }
            readBoolField(doc, "showAiBanner")?.let { editor.putBoolean("show_ai_banner", it) }
            readBoolField(doc, "showBackupCard")?.let { editor.putBoolean("show_backup_card", it) }
            readStringField(doc, "userDisplayName")?.let { remoteName ->
                if (remoteName.isNotBlank()) {
                    editor.putString("user_display_name", remoteName)
                }
            }
            readLongField(doc, "trashRetentionDays")?.let { editor.putInt("trash_retention_days", it.toInt()) }

            // 3) Anti-carrera: si el usuario editó algo mientras construíamos este editor,
            //    NO aplicamos (el push programado llevará sus valores) para no machacar
            //    su edición con la versión remota.
            if (prefs.getLong(KEY_DIRTY, 0L) != dirtyAt) return

            editor.apply()

            prefs.edit()
                .putLong(KEY_LAST_SEEN, remoteUpdatedAt)
                .putLong(KEY_CLOCK_OFFSET, remoteUpdatedAt - System.currentTimeMillis())
                // La versión remota sustituye a cualquier edición local anterior.
                .remove(KEY_DIRTY)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying remote settings", e)
        }
    }

    // ------------------------------------------------------------------
    // Helpers de ajustes
    // ------------------------------------------------------------------

    private fun buildSettingsMap(
        prefs: android.content.SharedPreferences,
        resolvedName: String,
        finalCatsJson: String
    ): Map<String, Any?> {
        return mapOf(
            "customCategories" to finalCatsJson,
            "categoryColors" to (prefs.getString("category_colors", "{}") ?: "{}"),
            "categoryIcons" to (prefs.getString("category_icons", "{}") ?: "{}"),
            "themeAccent" to (prefs.getString("accent_color", "system") ?: "system"),
            "themeDark" to (prefs.getString("theme_mode", "system") ?: "system"),
            "languageCode" to (prefs.getString("app_locale", "system") ?: "system"),
            "dateFormat" to (prefs.getString("date_format", "DD/MM/AA") ?: "DD/MM/AA"),
            "timeFormat" to (prefs.getString("time_format", "24h") ?: "24h"),
            "longPressAction" to (prefs.getString("long_press_action", "select") ?: "select"),
            "confirmOnDelete" to prefs.getBoolean("confirm_on_delete", true),
            "hapticFeedbackOnComplete" to prefs.getBoolean("haptic_feedback_on_complete", true),
            "requireBiometrics" to prefs.getBoolean("require_biometrics", false),
            "secureScreenMode" to prefs.getBoolean("secure_screen_mode", false),
            "encryptBackups" to prefs.getBoolean("encrypt_backups", false),
            "globalNotificationsEnabled" to prefs.getBoolean("global_notifications_enabled", true),
            "quickAddNotificationEnabled" to prefs.getBoolean("quick_add_notification_enabled", false),
            "quickAddNotificationInterval" to (prefs.getString("quick_add_notification_interval", "off") ?: "off"),
            "quickAddQuietHoursEnabled" to prefs.getBoolean("quick_add_quiet_hours_enabled", true),
            "notificationsSound" to prefs.getBoolean("notifications_sound", true),
            "notificationsVibrate" to prefs.getBoolean("notifications_vibrate", true),
            "reminderOffsetMinutes" to prefs.getInt("reminder_offset_minutes", 0),
            "snoozeDurationMinutes" to prefs.getInt("snooze_duration_minutes", 10),
            "aiModel" to (prefs.getString("ai_model", "gemini-3.8-flash") ?: "gemini-3.8-flash"),
            "aiLowLatencyMode" to prefs.getBoolean("ai_low_latency_mode", true),
            "aiSubtaskCount" to prefs.getInt("ai_subtask_count", 5),
            "aiKeyType" to (prefs.getString("ai_key_type", "public") ?: "public"),
            // Ajuste antes no sincronizado: la clave de API propia de Gemini.
            // NOTA DE SEGURIDAD: se guarda en claro en Firestore (solo legible por el
            // dueño según firestore.rules). Si no quieres subirla, elimina esta línea
            // y su lectura simétrica en applyRemoteSettings().
            "aiApiKey" to (prefs.getString("ai_api_key", "") ?: ""),
            "showAiBanner" to prefs.getBoolean("show_ai_banner", true),
            "showBackupCard" to prefs.getBoolean("show_backup_card", true),
            "userDisplayName" to resolvedName,
            "trashRetentionDays" to prefs.getInt("trash_retention_days", 30)
        )
    }

    /**
     * Versiones anteriores de la app escribían KEY_DIRTY y KEY_LAST_SEEN con el mismo
     * valor tras cada push/apply. Eso ya no significa "ediciones pendientes": lo
     * normalizamos una sola vez para partir de un estado limpio.
     */
    private fun normalizeLegacySettingsTimestamps() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dirty = prefs.getLong(KEY_DIRTY, 0L)
        val seen = prefs.getLong(KEY_LAST_SEEN, 0L)
        if (dirty != 0L && dirty == seen) {
            prefs.edit().remove(KEY_DIRTY).apply()
        }
    }

    private fun getDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).commit()
        }
        return id
    }

    // Lecturas defensivas: un campo con tipo inesperado no aborta el resto.
    private fun readLongField(doc: DocumentSnapshot, key: String): Long? =
        try { doc.getLong(key) } catch (_: Exception) { null }

    private fun readBoolField(doc: DocumentSnapshot, key: String): Boolean? =
        try { doc.getBoolean(key) } catch (_: Exception) { null }

    private fun readStringField(doc: DocumentSnapshot, key: String): String? =
        try { doc.getString(key) } catch (_: Exception) { null }

    private suspend fun mergeCategoriesJson(localJson: String, remoteJson: String): String {
        try {
            val localList = parseCategoryList(localJson)
            val remoteList = parseCategoryList(remoteJson)
            val combinedUserCats = (localList + remoteList).filter { !SystemCategory.isSystemCategory(it) }.distinct()

            val fromTasks = try {
                taskDao.getAllTasksSync().map { it.category.trim() }.filter { it.isNotBlank() && !SystemCategory.isSystemCategory(it) }.distinct()
            } catch (_: Exception) { emptyList() }

            val allUserCats = (combinedUserCats + fromTasks).distinct()
            val finalUserCats = if (allUserCats.isNotEmpty()) {
                allUserCats
            } else {
                listOf("Personal", "Trabajo", "Compras", "Hogar", "Salud")
            }

            val mergedList = mutableListOf<String>()
            mergedList.add(SystemCategory.ALL_TASKS)
            mergedList.addAll(finalUserCats)
            mergedList.add(SystemCategory.COMPLETED)

            val array = JSONArray()
            mergedList.forEach { array.put(it) }
            return array.toString()
        } catch (_: Exception) {
            return if (remoteJson.isNotBlank()) remoteJson else localJson
        }
    }

    private fun parseCategoryList(jsonStr: String): List<String> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.getString(i).trim()
                if (item.isNotEmpty() && !list.contains(item)) {
                    list.add(item)
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun mergeJsonObjectStrings(localJson: String, remoteJson: String): String {
        try {
            val localObj = JSONObject(if (localJson.isBlank()) "{}" else localJson)
            val remoteObj = JSONObject(if (remoteJson.isBlank()) "{}" else remoteJson)
            val merged = JSONObject()

            val localKeys = localObj.keys()
            while (localKeys.hasNext()) {
                val k = localKeys.next()
                merged.put(k, localObj.get(k))
            }

            val remoteKeys = remoteObj.keys()
            while (remoteKeys.hasNext()) {
                val k = remoteKeys.next()
                merged.put(k, remoteObj.get(k))
            }

            return merged.toString()
        } catch (_: Exception) {
            return if (remoteJson.isNotBlank()) remoteJson else localJson
        }
    }

    // ------------------------------------------------------------------
    // Helpers de tareas
    // ------------------------------------------------------------------

    private suspend fun uploadUnsyncedLocalTasks(userId: String) {
        val unsynced = taskDao.getUnsyncedTasks()
        if (unsynced.isEmpty()) return

        try {
            val batch = firestore.batch()
            val collection = firestore.collection("users").document(userId).collection("tasks")
            unsynced.forEach { task ->
                val docRef = collection.document(task.cloudId)
                batch.set(docRef, taskToFirestoreMap(task), SetOptions.merge())
            }
            batch.commit().await()
            taskDao.markTasksSynced(unsynced.map { it.id })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload unsynced tasks", e)
        }
    }

    private suspend fun uploadTaskToFirestore(userId: String, task: Task) {
        val docRef = firestore.collection("users").document(userId).collection("tasks").document(task.cloudId)
        docRef.set(taskToFirestoreMap(task), SetOptions.merge()).await()
    }

    private fun updateLastSyncTimestamp() {
        val now = System.currentTimeMillis()
        _lastSyncTimestamp.value = now
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong("last_firestore_sync_time", now)
            .apply()
    }

    private fun taskToFirestoreMap(task: Task): Map<String, Any?> {
        return mapOf(
            "cloudId" to task.cloudId,
            "title" to task.title.take(500).ifBlank { "Tarea" },
            "description" to task.description,
            "dueDate" to task.dueDate,
            "dueTime" to task.dueTime,
            "isCompleted" to task.isCompleted,
            "category" to task.category,
            "priority" to task.priority,
            "reminderTime" to task.reminderTime,
            "isRepeat" to task.isRepeat,
            "repeatType" to task.repeatType,
            "displayOrder" to task.displayOrder,
            "subtasksJson" to task.subtasksToJson(),
            "attachedImageUri" to task.attachedImageUri,
            "isDeleted" to task.isDeleted,
            "deletedTimestamp" to task.deletedTimestamp,
            "updatedAt" to task.updatedAt
        )
    }

    private fun parseDocumentToTask(doc: DocumentSnapshot): Task? {
        val cloudId = doc.getString("cloudId") ?: doc.id
        val title = doc.getString("title") ?: return null
        val isCompleted = doc.getBoolean("isCompleted") ?: false
        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
        val description = doc.getString("description") ?: ""
        val dueDate = doc.getLong("dueDate")
        val dueTime = doc.getString("dueTime")
        val category = doc.getString("category") ?: "General"
        val priority = doc.getString("priority") ?: "Media"
        val reminderTime = doc.getLong("reminderTime")
        val isRepeat = doc.getBoolean("isRepeat") ?: false
        val repeatType = doc.getString("repeatType") ?: "Ninguno"
        val displayOrder = doc.getLong("displayOrder")?.toInt() ?: 0
        val isDeleted = doc.getBoolean("isDeleted") ?: false
        val deletedTimestamp = doc.getLong("deletedTimestamp") ?: 0L
        val attachedImageUri = doc.getString("attachedImageUri")

        val subtasksJson = doc.getString("subtasksJson")
        val subtasks = parseSubtasks(subtasksJson)

        return Task(
            cloudId = cloudId,
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
            displayOrder = displayOrder,
            subtasks = subtasks,
            attachedImageUri = attachedImageUri,
            isDeleted = isDeleted,
            deletedTimestamp = deletedTimestamp,
            updatedAt = updatedAt,
            isSynced = true
        )
    }

    private fun parseSubtasks(json: String?): List<Subtask> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Subtask>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Subtask(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
