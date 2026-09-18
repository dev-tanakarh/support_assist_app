import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

// google-services requires app/google-services.json to exist, and fails the
// whole build immediately if it doesn't. That file has to come from your own
// Firebase project (Firebase Console → Project Settings → your Android app →
// download google-services.json) — nobody can hand you a working one, it's
// tied to your project's credentials. Applying the plugin only when the file
// is actually present means the rest of the app still builds and runs before
// you've done that step; FCM just won't initialize until you have.
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Reads API_BASE_URL from local.properties (gitignored, machine-specific) so
// nobody has to hardcode a URL into source that then gets committed — see
// RUNNING.md's note about the ngrok URL that used to be here and had already
// expired. Defaults to 10.0.2.2, which is the Android emulator's alias for
// "the host machine's localhost" — matches the backend's docker-compose
// setup (port 8080) out of the box for local development.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(FileInputStream(file))
}
val apiBaseUrl = localProperties.getProperty("API_BASE_URL") ?: "http://10.0.2.2:8080/api/"

android {
    namespace = "com.example.supportassist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.supportassist"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.fragment.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.rxjava3)
    implementation(libs.paging.compose)

    // WorkManager
    implementation(libs.work.runtime)

    // Biometric
    implementation(libs.biometric)

    // Lottie
    implementation(libs.lottie)
    implementation(libs.lottie.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.runtime.livedata)
    debugImplementation(libs.compose.ui.tooling)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // RxJava Support
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.11.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
