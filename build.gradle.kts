// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.9.3" apply false
    // Compose compiler plugin is bundled with Kotlin 1.9.x so we don't need the
    // separate Compose plugin. Use the Kotlin version from the version catalog
    // to keep it aligned with the rest of the project.
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
