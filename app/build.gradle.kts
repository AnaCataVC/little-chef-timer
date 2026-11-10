plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.littlechef.timer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.littlechef.timer"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("keystore.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "androiddebugkey"
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: "android"
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)

    testImplementation(libs.junit)
    // Android's org.json is a stub in local unit tests; the real implementation makes JSON parsing testable on the JVM.
    testImplementation(libs.org.json)
}
