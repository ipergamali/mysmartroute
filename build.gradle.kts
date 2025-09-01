// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.12.2" apply false

    // Χρήση της τελευταίας σταθερής έκδοσης 2.0.21 του Kotlin
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false

    id("com.google.gms.google-services") version "4.4.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" // Compose Compiler plugin
}
