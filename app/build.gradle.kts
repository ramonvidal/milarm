plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.milarm"
    compileSdk = 35

    // Every CI build gets a higher number, so a new APK always installs over the old one.
    val buildNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").map(String::toInt).getOrElse(1)

    defaultConfig {
        applicationId = "com.milarm"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.$buildNumber"
    }

    // Android only installs an update signed with the same key as the installed app, so every
    // published build must use one fixed key. CI writes it from the SIGNING_KEYSTORE_BASE64
    // secret (see README); it is never committed. Without it, builds fall back to the machine's
    // debug key, which is fine locally but must never be published.
    val milarmKeystore = file("milarm-release.keystore")
    if (milarmKeystore.exists()) {
        signingConfigs.create("milarm") {
            storeFile = milarmKeystore
            storePassword = "milarm"
            keyAlias = "milarm"
            keyPassword = "milarm"
        }
    }
    val signing = signingConfigs.findByName("milarm") ?: signingConfigs.getByName("debug")

    buildTypes {
        debug {
            // Same key as releases, so either installs over the other without uninstalling.
            signingConfig = signing
        }
        release {
            signingConfig = signing
            isMinifyEnabled = false
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
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
