pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

// Foojay resolver provides automated JDK fetching and configuration, streamlining multi-platform builds
plugins { 
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" 
}

// Centralized dependency resolution management ensuring consistent Maven/Google library lookup flows
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

// Configuración del nombre del proyecto y módulos de Gradle incluidos
rootProject.name = "FabiToDo"

include(":app")

