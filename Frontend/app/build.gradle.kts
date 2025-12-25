plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // --- ADDED PLUGINS ---
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.nexuschat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nexuschat"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Existing Dependencies ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    // --- ADDED FOR NEXUS CHAT ---

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Networking (Retrofit + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation)

    // DataStore (Token Storage)
    implementation(libs.androidx.datastore.preferences)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Gson (Manual parsing for WebSocket frames)
    implementation("com.google.code.gson:gson:2.10.1")

    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Allow references to generated code (Required for Hilt)
kapt {
    correctErrorTypes = true
}