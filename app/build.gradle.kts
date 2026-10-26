import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.mylittlefinancontrol"
    compileSdk = 35

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }
    val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: System.getenv("GEMINI_API_KEY") ?: ""

    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.example.mylittlefinancontrol"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiApiKey}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: keystoreProperties.getProperty("storeFile") ?: "release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProperties.getProperty("storePassword") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProperties.getProperty("keyAlias") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProperties.getProperty("keyPassword") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.fragment.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.firebase:firebase-storage")
    implementation("uk.co.samuelwall:material-tap-target-prompt:3.3.2")

    // Google Drive & Backup
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.gson)
    implementation(libs.google.http.client.android.ext)

    // Google ML Kit Text Recognition (Offline)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

    // Gemini AI
    implementation(libs.generativeai)
    implementation(libs.firebase.vertexai)

    // Exclude conflicting libraries from Google API
    configurations.all {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
}
