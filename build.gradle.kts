// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.9.3" apply false
    // Το plugin Compose απαιτείται σε Kotlin 2.x για να ενεργοποιηθεί ο
    // compiler του Jetpack Compose.
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false
    kotlin("kapt") version "2.1.21" apply false

    id("org.jetbrains.kotlin.android") version "2.1.21" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
