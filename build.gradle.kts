// Top-level build file where you can add configuration options common to all sub-projects/modules.

    plugins {
        id("com.android.application") version "8.9.2" apply false
        id("org.jetbrains.kotlin.android") version "2.1.0" apply false
        //id("org.jetbrains.kotlin.plugin.compose") version "1.5.11" apply false
        alias(libs.plugins.compose.compiler) apply false

        id("com.google.gms.google-services") version "4.4.2" apply false
    }
