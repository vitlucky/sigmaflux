plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.sigmaflux.market"
    compileSdk = 35

    // Чтение локального backend URL из local.properties (для физ. устройства), иначе — дефолты ниже.
    val localProps = java.util.Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { localProps.load(it) }
    val localBackendUrl: String? = localProps.getProperty("sigmaflux.backendUrl")?.trim()?.takeIf { value -> value.isNotEmpty() }

    defaultConfig {
        applicationId = "com.sigmaflux.market"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"

        buildConfigField("boolean", "AI_ENABLED", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // В релизе — только https. Локальный http — только для debug/эмулятора.
            val releaseUrl = localBackendUrl?.takeIf { it.startsWith("https://") } ?: "https://api.sigmaflux.example/"
            buildConfigField("String", "BACKEND_BASE_URL", "\"$releaseUrl\"")
        }
        debug {
            val debugUrl = localBackendUrl ?: "http://10.0.2.2:8000/"
            // debug допускает http для эмулятора, но релиз — usesCleartextTraffic=false
            buildConfigField("String", "BACKEND_BASE_URL", "\"$debugUrl\"")
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Compose (Material 3)
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking: Retrofit + OkHttp + kotlinx.serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Glance widget
    implementation("androidx.glance:glance-appwidget:1.1.0")

    // Unit tests (JVM, run in CI)
    testImplementation("junit:junit:4.13.2")
}
