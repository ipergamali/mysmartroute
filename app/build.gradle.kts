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
    }

android {
    // Απαραίτητο namespace για AGP 8+
    namespace = "com.ioannapergamali.mysmartroute"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ioannapergamali.mysmartroute"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        // Τελευταία σταθερή έκδοση του compiler για Compose και Kotlin 1.9.23
        kotlinCompilerExtensionVersion = "1.5.14"
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
    // 🔹 Firebase BOM (versions managed centrally)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // 🔹 Firebase dependencies
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-dynamic-links-ktx")

    // 🔹 AndroidX & Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

}


kapt {
    correctErrorTypes = true
}