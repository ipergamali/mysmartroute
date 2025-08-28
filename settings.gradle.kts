// Βασική διαμόρφωση Gradle
pluginManagement {
    plugins {
        id("com.android.application") version "8.12.2"
        kotlin("android") version "2.2.10"
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "mysmartroute"
include(":app")
