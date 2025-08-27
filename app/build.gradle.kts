import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // compose
    implementation(platform("androidx.compose:compose-bom:2025.08.00")) // optional but recommended
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text") // includes AnnotatedString & LinkAnnotation
    implementation("androidx.compose.material3:material3")

    // navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // datastore
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // viewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
}