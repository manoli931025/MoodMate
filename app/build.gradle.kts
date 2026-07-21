plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.manoli.moodmate"
    compileSdk = 34

    signingConfigs {
        create("release") {
            storeFile = file("../moodmate.jks")
            storePassword = "moodmate123"   // cambia por tu contraseña real
            keyAlias = "moodmate"
            keyPassword = "moodmate123"    // suele ser la misma
        }
    }

    defaultConfig {
        applicationId = "com.manoli.moodmate"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.0")

    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.5")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}