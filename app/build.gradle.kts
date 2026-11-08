plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val rawVersionName = (project.findProperty("versionName") as? String)
    ?: System.getenv("VERSION_NAME")
    ?: System.getenv("GITHUB_REF_NAME")
    ?: "1.0.0"
val resolvedVersionName = rawVersionName.trim().removePrefix("v").ifBlank { "1.0.0" }

val resolvedVersionCode: Int = ((project.findProperty("versionCode") as? String)?.toIntOrNull())
    ?: System.getenv("VERSION_CODE")?.toIntOrNull()
    ?: run {
        val parts = resolvedVersionName.split("-").first().split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        (major * 10000) + (minor * 100) + patch
    }

val keystoreFileEnv = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
val keystorePasswordEnv = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val keyAliasEnv = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
val keyPasswordEnv = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() }

val resolvedKeystoreFile = keystoreFileEnv?.let { path ->
    val fileInApp = file(path)
    val fileInRoot = rootProject.file(path)
    when {
        fileInApp.exists() -> fileInApp
        fileInRoot.exists() -> fileInRoot
        else -> null
    }
}

val hasValidReleaseKeystore = resolvedKeystoreFile != null &&
    keystorePasswordEnv != null &&
    keyAliasEnv != null &&
    keyPasswordEnv != null

android {
    namespace = "id.calltag.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.calltag.app"
        minSdk = 26
        targetSdk = 34
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val apiUrl = (project.findProperty("baseApiUrl") as? String)
            ?: System.getenv("BASE_API_URL")
            ?: "http://10.0.2.2:8080/"
        buildConfigField("String", "BASE_API_URL", "\"$apiUrl\"")
    }

    signingConfigs {
        create("release") {
            if (hasValidReleaseKeystore) {
                storeFile = resolvedKeystoreFile
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasValidReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        buildConfig = true
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
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // Room Database (Offline-First Local Cache)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit + OkHttp (REST API Client ke backend gocroot)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Coroutines & DataStore Preferences (Auto-Block Scammer toggle)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
}
