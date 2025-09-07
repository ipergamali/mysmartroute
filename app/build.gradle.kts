import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

// Διαβάζουμε το MAPS_API_KEY από το local.properties με την νέα υπογραφή που
// απαιτεί τον παράγοντα providers στο AGP 8.5+
val mapsApiKey: String = gradleLocalProperties(rootDir, providers)
    .getProperty("MAPS_API_KEY") ?: ""

    plugins {
        id("com.android.application")
        id("org.jetbrains.kotlin.android")
        id("kotlin-kapt")
        id("com.google.gms.google-services")
        id("org.jetbrains.kotlin.plugin.compose")
    }

android {
    // Απαραίτητο namespace για AGP 8+
    namespace = "com.ioannapergamali.mysmartroute"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ioannapergamali.mysmartroute"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "2.14"

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildToolsVersion = "34.0.0"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        // Τελευταία σταθερή έκδοση του compiler για Kotlin 2.0.21
        kotlinCompilerExtensionVersion = "1.6.11"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    sourceSets.named("main") {
        java.exclude("**/caches/**")
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
kotlin {
    jvmToolchain(21)
}
dependencies {
    // Firebase (BoM για αυτόματες εκδόσεις όλων των modules)
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Το Dynamic Links δεν καλύπτεται από το BoM, δηλώνουμε ρητά την έκδοση
    implementation("com.google.firebase:firebase-dynamic-links:22.1.0")


    // Android core
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Jetpack Compose
    // Jetpack Compose BOM (σταθερή έκδοση 1.6.7)
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Χρήση της σταθερής έκδοσης Material3
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.navigation:navigation-compose:2.9.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")

    // DataStore για αποθήκευση ρυθμίσεων
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Room
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.maps.android:maps-compose:6.6.0")
    implementation("com.google.maps.android:maps-ktx:5.2.0")
    implementation("com.google.maps.android:maps-utils-ktx:5.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:3.4.0")

    // Coroutines (νεότερη σταθερή έκδοση)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // HTTP Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.13.1")

    // Crash reporting με ACRA
    implementation("ch.acra:acra-mail:5.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.room:room-testing:2.7.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}


kapt {
    correctErrorTypes = true
}
