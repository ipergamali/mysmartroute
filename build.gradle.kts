// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.6.0" apply false
    // Το plugin Compose απαιτείται σε Kotlin 2.x για να ενεργοποιηθεί ο
    // compiler του Jetpack Compose.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    kotlin("kapt") version "2.0.20" apply false

    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}


