plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.eunomie.focus.spike"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.eunomie.focus.spike"
        minSdk = 33
        targetSdk = 36
        versionCode = 3
        versionName = "0.1-spike"
    }

    // Each Dagger build runs in a fresh container, which would otherwise generate a
    // new debug keystore every time and make `adb install -r` fail with
    // INSTALL_FAILED_UPDATE_INCOMPATIBLE. This keystore is the standard Android debug
    // key -- well-known password, no secret in it -- committed so rebuilds stay
    // signature-stable and reinstall over the top.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.17.0")
}
