plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.shipmonitoring"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.shipmonitoring"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val debugBaseUrl = providers
        .gradleProperty("DEBUG_BASE_URL")
        .orElse("http://10.0.2.2:3000/api/")
    val releaseBaseUrl = providers
        .gradleProperty("RELEASE_BASE_URL")
        .orElse("https://ship-monitoring-be.vercel.app/api/")

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"${debugBaseUrl.get()}\"")
            // More secure than BODY: do not print payload by default.
            buildConfigField("String", "HTTP_LOG_LEVEL", "\"BASIC\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"${releaseBaseUrl.get()}\"")
            buildConfigField("String", "HTTP_LOG_LEVEL", "\"NONE\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Networking & API
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")
    // Coroutines untuk proses Asynchronous
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    // Navigation Compose (untuk pindah antar layar)
    implementation("androidx.navigation:navigation-compose:2.9.8")
    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.compose.material:material-icons-extended")
    // Google Play Services Location (Untuk Nahkoda mengambil koordinat GPS)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // Google Maps Compose (Untuk Manager melihat Peta)
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // Coil untuk memuat gambar dari URL URL
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
