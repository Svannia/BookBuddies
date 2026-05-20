import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("kotlin-kapt")
}

val secretsPropsFile = rootProject.file("secrets.properties")
val secretsProps = Properties().apply {
    if (secretsPropsFile.exists()) {
        load(FileInputStream(secretsPropsFile))
    }
}

android {
    namespace = "com.example.bookbuddies"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.bookbuddies"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"${secretsProps.getProperty("telegramBotToken") ?: ""}\"")
        buildConfigField("String", "TELEGRAM_CHAT_ID", "\"${secretsProps.getProperty("telegramChatId") ?: ""}\"")
        buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"${secretsProps.getProperty("googlebooksAPIkey") ?: ""}\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.compose.ui.ui.graphics)
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // material3
    implementation(libs.material)

    // compose
    implementation(platform(libs.androidx.compose.bom.v20250800))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.androidx.ui.text)
    implementation(libs.material3)

    // navigation
    implementation(libs.androidx.navigation.compose)

    // datastore
    implementation(libs.androidx.datastore.preferences)

    // timber
    implementation(libs.timber)

    // viewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // permissions
    implementation(libs.androidx.activity.ktx)

    // room
    implementation(libs.androidx.room.runtime) // Or the latest version
    kapt(libs.androidx.room.compiler)
    annotationProcessor(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.opencsv)

    // images
    implementation(libs.coil.compose)

    // http requests
    implementation(libs.okhttp)


    // ML Kit Barcode Scanning
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.camera.mlkit.vision)

    // CameraX dependencies for camera integration
    implementation(libs.androidx.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
}