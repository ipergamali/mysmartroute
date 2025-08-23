diff --git a/app/build.gradle.kts b/app/build.gradle.kts
        index 976dce23a65a6e560a9878a4e0a8d28134900a8f..ad8c34773632437d3990529409e732403a3b9b86 100644
--- a/app/build.gradle.kts
+++ b/app/build.gradle.kts
@@ -59,54 +59,54 @@ android {

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
    // Firebase βιβλιοθήκες
    -
    -    implementation(libs.firebase.auth.ktx)
    -    implementation(libs.firebase.firestore.ktx)
    -    implementation(libs.firebase.dynamic.links.ktx)
    +    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
    +    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
    +    implementation("com.google.firebase:firebase-dynamic-links-ktx:22.1.0")
    +    implementation("com.google.firebase:firebase-common-ktx:21.0.0")
    // Android core
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

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
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.navigation:navigation-compose:2.9.1")
    implementation("androidx.compose.material:material-icons-extended")

    // DataStore για αποθήκευση ρυθμίσεων
    implementation("androidx.datastore:datastore-preferences:1.1.7")
 
