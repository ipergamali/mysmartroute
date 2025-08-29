// Βασική διαμόρφωση Gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.11.0" apply false
        id("org.jetbrains.kotlin.android") version "2.2.10" apply false
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
