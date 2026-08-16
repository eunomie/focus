plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.eunomie.focus"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.eunomie.focus"
        // AutomaticZenRule.Builder and ZenPolicy's people-type constants are API 34+.
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    // Every Dagger build runs in a fresh container, which would otherwise generate a new
    // debug keystore each time and make `adb install -r` fail. This is the standard Android
    // debug key -- well-known password, nothing secret -- committed so rebuilds install over
    // the top.
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
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
}
