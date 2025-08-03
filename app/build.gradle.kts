import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

// Διαβάζουμε τα API keys από το local.properties ή από μεταβλητή περιβάλλοντος

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(FileInputStream(localPropsFile))
}

val MAPS_API_KEY: String =
    (localProps.getProperty("MAPS_API_KEY"))
        ?: System.getenv("MAPS_API_KEY")
        ?: ""

android {
    namespace = "com.ioannapergamali.mysmartroute"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ioannapergamali.mysmartroute"
        minSdk = 33
        targetSdk = 35
        versionCode = 18
        versionName = "2.10"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAPS_API_KEY", "\"$MAPS_API_KEY\"")
        manifestPlaceholders["MAPS_API_KEY"] = MAPS_API_KEY
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Χρήση της νεότερης σταθερής έκδοσης του compiler
        kotlinCompilerExtensionVersion = "1.6.7"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
        }
    }

}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.21")

    // Android core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2025.07.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.07.00"))

    // Χρήση της σταθερής έκδοσης Material3
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.navigation:navigation-compose:2.9.1")
    implementation("androidx.compose.material:material-icons-extended")

    // DataStore για αποθήκευση ρυθμίσεων
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Firebase
    // Χρήση της πιο πρόσφατης έκδοσης BOM
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.maps.android:maps-compose:6.6.0")
    implementation("com.google.maps.android:maps-ktx:5.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:3.4.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // ✅ downgrade
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // ✅ downgrade
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3") // ✅ downgrade

    // HTTP Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.13.1")

    // Crash reporting με ACRA
    implementation("ch.acra:acra-mail:5.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

kapt {
    correctErrorTypes = true
}
