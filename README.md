# 📝 FabiTodo — Gestor de Tareas Inteligente, Moderno y Multilingüe

**FabiTodo** es un organizador de tareas de última generación diseñado para Android, construido desde cero utilizando **Jetpack Compose**, **Material 3 (Material You)**, y reforzado con una arquitectura reactiva industrial de alto rendimiento.

La aplicación ofrece un balance perfecto entre privacidad absoluta (funcionamiento local-first con persistencia en **Room Database**) y la versatilidad de la nube (respaldos automáticos continuos con **Firebase Auth & Firestore** y acceso biométrico con Google Credentials Manager). Además, cuenta con un potente motor de categorización automática de tareas en lenguaje natural potenciado por **Gemini AI**.

---

## ✨ Características Principales

### 🌐 1. Localización y Soporte Multilingüe Completo
FabiTodo cuenta con traducciones nativas y completas integradas para un alcance verdaderamente global. La interfaz de usuario, formatos de fecha, alertas y notificaciones se adaptan automáticamente a:
* **Inglés** (Predeterminado)
* **Español** (es)
* **Francés** (fr)
* **Alemán** (de)
* **Italiano** (it)
* **Portugués** (pt)
* **Ruso** (ru)
* **Chino Simplificado** (zh)
* **Japonés** (ja)
* **Árabe** (ar - con soporte RTL completo)

### 🤖 2. Inteligencia Artificial (Gemini AI Companion)
No es un simple To-Do; FabiTodo incluye un motor semántico que:
* **Autocategoriza** tus elementos de texto en carpetas lógicas.
* **Sugiere Prioridades** automáticamente basándose en la fecha de vencimiento y el contexto.
* **Genera Subtareas** complejas de manera inteligente a partir de una única e intuitiva línea escrita.

### 🔒 3. Seguridad, Auth y Sincronización en la Nube
* **Login sin Fricción:** Acceso rápido con un toque mediante **Google Tap (Credentials Manager API)** o inicio rápido como **Invitado (Offline Completo)**.
* **Políticas de Respaldo:** Tus datos locales del dispositivo se empaquetan de forma encriptada y se sincronizan en tiempo real con **Firestore** si decides iniciar sesión.
* **Harding del Manifiesto:** Bloqueo explícito de tráfico sin cifrar (`android:usesCleartextTraffic="false"`) para garantizar que todas las transacciones de red viajen cifradas de extremo a extremo a través de HTTPS.

### 🔔 4. Recordatorios Exactos y Alarmas del Sistema
* Programación exacta de alertas críticas mediante el uso de `AlarmManager` con el permiso `SCHEDULE_EXACT_ALARM`.
* Registro robusto ante reinicios de batería gracias al receptor de arranque asíncrono (`BOOT_COMPLETED`), garantizando que nunca te pierdas una tarea programada incluso si el móvil se apaga.
* Corbeille inteligente con políticas de autorentención configurable (ej. 30 días) para restaurar tareas eliminadas por accidente.

---

## 🎨 Especificaciones Visuales y de Diseño (Material 3)

* **Tema Visual "Cosmic Slate":** Colores oscuros profundos combinados con tonalidades modernas que reducen la fatiga visual.
* **Espaciado y Grid Estricto:** Diseño adaptativo basado plenamente en la cuadrícula de **8dp** de Material Design 3, ofreciendo un lienzo limpio, espacioso y sumamente escaneable.
* **Dinámico y Personalizado:** Soporte nativo para la recolección de colores dinámicos del sistema (Material You en Android 12+), complementado con una paleta de acentos estéticos seleccionables a mano.
* **Micro-interacciones Gratificantes:** Transiciones suaves de entrada y salida, retroalimentación táctil refinada, ripple effects nativos y estados interactivos accesibles con objetivos táctiles mínimos de **48dp x 48dp**.

---

## 🏗️ Navegación por la Carpeta de Pruebas (`app/src/test/`)

¿Qué es este directorio? Aquí reside la suite de pruebas automatizadas del proyecto. FabiTodo utiliza un entorno ágil de simulación que permite correr pruebas robustas de sistema en tu ordenador local sin necesidad de arrancar un emulador Android físico lento o conectar un dispositivo real.

La suite incluye los siguientes archivos clave en `app/src/test/java/com/fabian/todolist/`:

1.  **`ExampleRobolectricTest.kt`** (CUJ Testing):
    *   Utiliza **Robolectric** para recrear el entorno de ejecución del SDK de Android 14+ (API 36) directamente sobre la Máquina Virtual de Java (JVM).
    *   Verifica la lectura correcta de recursos del sistema (`strings.xml`, `R.string.app_name`).
    *   Simula la inicialización asíncrona de los servicios de Firebase (`FirebaseApp`).
    *   Prueba el lanzamiento controlado de la pantalla de inicio para confirmar que no ocurran crasheos o problemas de dependencias en el arranque (`launch main activity`).

2.  **`GreetingScreenshotTest.kt`** (Pruebas de Regresión Visual):
    *   Combina el framework **Roborazzi** con el motor nativo de renderizado gráfico de Robolectric (`@GraphicsMode(GraphicsMode.Mode.NATIVE)`).
    *   Dibuja composables específicos de manera idéntica a como se verían en la pantalla física de un dispositivo de gama alta (por ejemplo, un *Google Pixel 8*).
    *   Captura una imagen PNG del widget y la compara con la línea base almacenada en el repositorio (`src/test/screenshots/`), alertando de manera analítica en caso de que un cambio accidental de código rompa la apariencia, tipografía, bordes o padding diseñados.

### 🚀 Cómo Correr las Pruebas
*   **Ejecutar todas las pruebas unitarias y de arquitectura:**
    ```bash
    gradle :app:testDebugUnitTest
    ```
*   **Verificar que los elementos visuales sigan siendo perfectos:**
    ```bash
    gradle :app:verifyRoborazziDebug
    ```
*   **Volver a capturar las capturas base (si cambiaste el diseño intencionalmente):**
    ```bash
    gradle :app:recordRoborazziDebug
    ```

---

## ⚡ Optimización y Compilación (ProGuard)

### 🔒 Reglas de ProGuard Profesional (`app/proguard-rules.pro`)
El compilador utiliza reglas avanzadas de empaquetado para reducir el tamaño del APK e impedir la ingeniería inversa. Se ha configurado soporte preciso para:
*   Evitar la ofuscación de los modelos y entidades centrales del dominio (`Task`, `Subtask`).
*   Mantener íntegros los metadatos de Jetpack Compose Runtime e interactores de flujos.
*   Preservar las firmas asíncronas de base de datos **Room**, parsers reflexivos de **Moshi JSON**, drivers de la red interna de **Retrofit/OkHttp**, APIs de autenticación de **Firebase**, cargadores asíncronos de imágenes **Coil**, y despachadores asíncronos en segundo plano de **WorkManager**.

---

## 💻 Comandos Rápidos de Consola

Usa los siguientes comandos listos desde el directorio raíz de la app en la terminal:

| Acción | Comando | Ruta del APK resultante |
| :--- | :--- | :--- |
| **Compilar en modo de prueba (Debug)** | `gradle assembleDebug` | `/app/build/outputs/apk/debug/app-debug.apk` |
| **Pruebas de Código y Lint** | `gradle compileDebugKotlin` | Verifica sintaxis e importaciones en toda la app |
