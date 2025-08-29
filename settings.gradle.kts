// Βασική διαμόρφωση Gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.6.1" apply false
        id("org.jetbrains.kotlin.android") version "2.0.21" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
        id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
        id("com.google.gms.google-services") version "4.4.3" apply false
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.google.com") // Επίλυση βιβλιοθηκών Firebase
    }
}

rootProject.name = "mysmartroute"
include(":app")
