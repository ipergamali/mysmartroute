// Top-level build file where you can add configuration options common to all sub-projects/modules.

    plugins {
        id("com.android.application") version "8.9.2" apply false
        id("org.jetbrains.kotlin.android") version "2.0.0" apply false
        //id("org.jetbrains.kotlin.plugin.compose") version "1.5.11" apply false
    // Compose plugin is not required for this project

        id("com.google.gms.google-services") version "4.4.2" apply false
    }
