// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.12.2" apply false

    // Χρήση της τελευταίας σταθερής έκδοσης 1.9.24 του Kotlin
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false

    // KSP για συμβατότητα με Kotlin 1.9.24
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false

    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
}
