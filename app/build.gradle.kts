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
        // AutomaticZenRule.Builder is API 35, not 34 — lint caught this, and on a 34
        // device it would have been a NoSuchMethodError the moment focus mode started.
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    signingConfigs {
        // Supplied by the Dagger build as a secret, because every container would otherwise
        // generate a fresh debug key and `adb install -r` would fail. Absent, Gradle uses
        // its own generated debug key. The password is the Android default; the key file
        // is the part worth keeping private.
        System.getenv("FOCUS_DEBUG_KEYSTORE")?.let { keystore ->
            getByName("debug") {
                storeFile = file(keystore)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        // Supplied by the Dagger release pipeline as secrets. Absent for ordinary builds,
        // in which case the release variant is simply left unsigned rather than failing.
        System.getenv("FOCUS_KEYSTORE")?.let { keystore ->
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("FOCUS_STORE_PASSWORD")
                keyAlias = System.getenv("FOCUS_KEY_ALIAS")
                keyPassword = System.getenv("FOCUS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
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

    testImplementation("junit:junit:4.13.2")
}
