plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ioannapergamali.mysmartroute"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ioannapergamali.mysmartroute"
        minSdk = 33
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        localeFilters.add("en")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // Matches Compose Compiler for Kotlin 1.9.x
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")

    // Android core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose
    implementation("androidx.compose.material3:material3-android:1.3.2")
    implementation("androidx.compose.ui:ui:1.6.4")
    implementation("androidx.compose.ui:ui-text:1.6.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.4")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.4")
    implementation("androidx.navigation:navigation-compose:2.7.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.4")

    // DataStore για αποθήκευση ρυθμίσεων
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Firebase
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    implementation("com.google.firebase:firebase-firestore-ktx:24.11.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.maps.android:maps-compose:6.2.0") // ✅ downgrade
    implementation("com.google.maps.android:maps-ktx:4.3.0")     // ✅ downgrade
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // ✅ downgrade
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // ✅ downgrade
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3") // ✅ downgrade

    // HTTP Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
