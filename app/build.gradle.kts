plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.google.services)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.fabian.todolist"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.fabian.todolist"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "GEMINI_API_KEY", "\"\"")
  }

  // Optional debug signing config — only loaded if a keystore.properties file
  // (gitignored) is present at the repo root. Never hardcode signing passwords
  // in a public repository.
  val keystorePropsFile = rootProject.file("keystore.properties")
  val keystoreProps = java.util.Properties()
  if (keystorePropsFile.exists()) {
    keystoreProps.load(keystorePropsFile.inputStream())
    signingConfigs {
      create("debugConfig") {
        storeFile = file(keystoreProps.getProperty("debugStoreFile"))
        storePassword = keystoreProps.getProperty("debugStorePassword")
        keyAlias = keystoreProps.getProperty("debugKeyAlias")
        keyPassword = keystoreProps.getProperty("debugKeyPassword")
      }
    }
  }

  // Release signing config — populated by CI environment variables
  // (KEYSTORE_PATH / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD). When these
  // env vars are absent (e.g., local dev), the release build is left unsigned
  // and `./gradlew assembleRelease` will produce an unsigned APK.
  val ciKeystorePath = (System.getenv("KEYSTORE_PATH") ?: "").ifEmpty { null }
  val ciStorePass = (System.getenv("STORE_PASSWORD") ?: "").ifEmpty { null }
  val ciKeyAlias = (System.getenv("KEY_ALIAS") ?: "").ifEmpty { null }
  val ciKeyPass = (System.getenv("KEY_PASSWORD") ?: "").ifEmpty { null }
  if (ciKeystorePath != null && ciStorePass != null && ciKeyAlias != null && ciKeyPass != null) {
    signingConfigs {
      create("releaseConfig") {
        storeFile = file(ciKeystorePath)
        storePassword = ciStorePass
        keyAlias = ciKeyAlias
        keyPassword = ciKeyPass
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // Enable R8 (code shrinking + obfuscation) and resource shrinking for
      // release builds. The proguard-rules.pro file keeps the classes that
      // Room / Moshi / Hilt / Firebase / Compose need via reflection.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Sign with the CI-provided keystore when available.
      if (ciKeystorePath != null && ciStorePass != null && ciKeyAlias != null && ciKeyPass != null) {
        signingConfig = signingConfigs.getByName("releaseConfig")
      }
    }
    debug {
      if (keystorePropsFile.exists() && keystoreProps.getProperty("debugStoreFile") != null) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// Forced configuration refresh 2
// Forced configuration refresh
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation("libs.accompanist.permissions")
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation("androidx.core:core-splashscreen:1.0.1")
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation("androidx.biometric:biometric:1.1.0")
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.play.services.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  implementation(libs.hilt.android)
  "ksp"(libs.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
}

tasks.register<Copy>("copyApk") {
  dependsOn("assembleDebug")
  from(file("build/outputs/apk/debug/app-debug.apk"))
  into(file("${rootDir}/apk"))
}


