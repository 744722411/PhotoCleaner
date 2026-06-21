plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.photocleaner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.photocleaner"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "1.6.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    // Compose BOM - Latest
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Core - Latest
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Navigation - Latest
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt - Latest
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room - Latest
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + OkHttp + Moshi - Latest
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)

    // Coil 3 - Latest
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // ExifInterface - Latest
    implementation(libs.androidx.exifinterface)

    // Coroutines - Latest
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // DataStore Preferences - Latest
    implementation(libs.androidx.datastore.preferences)

    // Security Crypto
    implementation(libs.androidx.security.crypto)
    
    // ML Kit Image Labeling
    implementation(libs.mlkit.image.labeling)
    implementation(libs.kotlinx.coroutines.play.services)

    // Baseline Profiles
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
}
