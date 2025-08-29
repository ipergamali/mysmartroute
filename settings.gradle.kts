// Βασική διαμόρφωση Gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.12.2" apply false
        id("org.jetbrains.kotlin.android") version "2.2.10" apply false
        id("com.google.gms.google-services") version "4.4.3" apply false
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "mysmartroute"
include(":app")
