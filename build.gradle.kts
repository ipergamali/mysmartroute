// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") apply false
    // Το plugin Compose απαιτείται σε Kotlin 2.x για να ενεργοποιηθεί ο
    // compiler του Jetpack Compose.
    id("org.jetbrains.kotlin.plugin.compose") apply false
    id("org.jetbrains.kotlin.kapt") apply false

    id("org.jetbrains.kotlin.android") apply false
    // Plugin Google Services για Firebase
    id("com.google.gms.google-services") apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
