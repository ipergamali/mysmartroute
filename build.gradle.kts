// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.12.2" apply false

    // Χρήση της τελευταίας σταθερής έκδοσης 1.9.25 του Kotlin για συμβατότητα με το KAPT
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false

    id("com.google.gms.google-services") version "4.4.3" apply false
}
