plugins {
    id("kotlin-kapt")
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
}



android {
    namespace = "com.ioannapergamali.mysmartroute"
    // Χρησιμοποιούμε την πιο πρόσφατη έκδοση του Android SDK
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ioannapergamali.mysmartroute"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui:1.6.4")
    implementation("androidx.compose.ui:ui-text:1.6.4")

    implementation("androidx.compose.ui:ui-tooling-preview:1.6.4")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.4")
    implementation("androidx.navigation:navigation-compose:2.7.1")
    // Provides additional Material icons such as `Logout`
    implementation("androidx.compose.material:material-icons-extended:1.6.4")

    // Firebase
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.maps.android:maps-compose:6.6.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Networking for Directions API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
apply(plugin = "com.google.gms.google-services")
