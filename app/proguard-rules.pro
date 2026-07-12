# ProGuard/R8 Rules configuration for FabiTodo
# Optimizaciones y reglas de preservación para un compilado de producción altamente seguro, estable y eficiente.

# -------------------------------------------------------------
# 1. REGLAS GENERALES DEL FRAMEWORK Y TRAZADO DE ERRORES
# -------------------------------------------------------------
# Preservar anotaciones genéricas de Java/Kotlin útiles para la reflexión e introspección de bibliotecas.
-keepattributes Exception*, Signature, InnerClasses, EnclosingMethod, Deprecated, Annotation*, SourceFile, LineNumberTable

# Mantener la información de nombres de clase originales en las trazas de error (útil para informes de crash como Crashlytics).
-keepattributes SourceFile, LineNumberTable

# Evitar advertencias molestas sobre tipos inexistentes que no afectan la ejecución regular de la aplicación o sus librerías.
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**

# -------------------------------------------------------------
# 2. JETPACK COMPOSE & KOTLIN COROUTINES
# -------------------------------------------------------------
# Preservar metadatos y constructores clave de la composición interactiva y flujos de estado.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Evitar que se remuevan metadatos internos de Compose runtime
-keep class androidx.compose.runtime.** { *; }

# Coroutines: Evitar la ofuscación de despachadores de hilos de fondo y manejo de excepciones.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    private volatile <fields>;
}

# -------------------------------------------------------------
# 3. ROOM DATABASE (PERSISTENCIA LOCAL)
# -------------------------------------------------------------
# Evitar que se remuevan o alteren los mapeadores internos generados por Room en tiempo de compilación.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Garantizar que los drivers e hilos nativos SQLite no sufran ofuscación.
-keep class androidx.room.db.SupportSQLite* { *; }

# Mantener los DAOs intactos para que las querys mapeadas mediante reflexión o KSP funcionen sin fallas.
-keep @interface androidx.room.Dao
-keep @interface androidx.room.Database
-keep @interface androidx.room.Entity

# -------------------------------------------------------------
# 4. MICRO-SERIALIZACIÓN Y PARSEO: MOSHI JSON & KOTLINX SERIALIZATION
# -------------------------------------------------------------
# Moshi: No eliminar ni alterar clases anotadas con @JsonClass de Moshi.
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class *JsonAdapter { *; }
-keep class *JsonAdapter$* { *; }

# Evitar la ofuscación de campos que utilicen anotaciones de Moshi.
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# KotlinX Serialization (Asegura la compatibilidad con rutas de Jetpack Navigation type-safe)
-keepattributes *Annotation*,enclosingmethod,innerclasses
-keepclassmembers class * {
    *** Companion;
}
-keep class *$$serializer { *; }
-keepclassmembers class * {
    *** writeSelf(...);
}

# -------------------------------------------------------------
# 5. CONECTIVIDAD NETWORK: RETROFIT & OKHTTP
# -------------------------------------------------------------
# Retrofit: Impedir que se eliminen los parámetros de cabeceras, rutas y cuerpos mapeados.
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp: Preservar las firmas criptográficas nativas y evitar advertencias sobre dependencias opcionales.
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# -------------------------------------------------------------
# 6. AUTENTICACIÓN FIREBASE Y PROVEEDOR CREDENTIALS
# -------------------------------------------------------------
# Firebase Auth & Google Sign-In
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# Android Jetpack Credentials API & Google ID
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**
-keep class com.google.android.libraries.identity.googleid.** { *; }

# -------------------------------------------------------------
# 7. COIL (CARGA DE IMÁGENES EFICIENTE)
# -------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# -------------------------------------------------------------
# 8. WORKMANAGER (PLANIFICADOR DE TAREAS EN SEGUNDO PLANO)
# -------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    <init>(...);
}
-dontwarn androidx.work.**

# -------------------------------------------------------------
# 9. MODELOS DE DATOS PROPIOS DE LA APLICACIÓN (FABITODO)
# -------------------------------------------------------------
# Proteger las entidades de persistencia y modelos del dominio para evitar que Room o Moshi rompan el parseo de datos.
-keep class com.fabian.todolist.data.Task { *; }
-keep class com.fabian.todolist.data.Subtask { *; }
-keep class com.fabian.todolist.data.gemini.** { *; }

# -------------------------------------------------------------
# 10. HILT / DAGGER (INYECT DE DEPENDENCIAS POR REFLEXIÓN)
# -------------------------------------------------------------
# Hilt genera código via KSP pero las clases anotadas con @Inject, @Module,
# @InstallIn, @HiltViewModel y @AndroidEntryPoint son accedidas por reflexión
# en tiempo de ejecución. Sin estas reglas, R8 elimina constructores anotados
# y la app crashea con "cannot be provided without an @Inject constructor".
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}
# Keep @Keep annotated classes (used by some AndroidX libs)
-keep @androidx.annotation.Keep class * { *; }

# -------------------------------------------------------------
# 11. GLANCE (APP WIDGET) — METADATOS DE COMPOSE RUNTIME
# -------------------------------------------------------------
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# -------------------------------------------------------------
# 12. BIOMETRIC PROMPT
# -------------------------------------------------------------
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# -------------------------------------------------------------
# 13. STRIP LOG.d/Log.v CALLS IN RELEASE (NO-OPS FOR ERROR LOGS)
# Assumenos que Log.d y Log.v no tienen side effects y elimínalos.
# Esto reduce tamaño del APK y evita filtrar logs de debug en producción.
# -------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

