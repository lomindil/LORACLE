plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.loracle"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.loracle"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}


dependencies {
    // AppCompat (needed for AppCompatActivity)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Material Components (needed for TextInputLayout)
    implementation("com.google.android.material:material:1.11.0")

    // Kotlin Standard Library
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

    // Core KTX (recommended)
    implementation("androidx.core:core-ktx:1.12.0")

    implementation("ai.picovoice:porcupine-android:3.0.2")
}
