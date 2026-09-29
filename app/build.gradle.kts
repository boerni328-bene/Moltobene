plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.moltobene.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moltobene.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.2.5"

        // Nur die unterstützten Sprachen mitliefern (Englisch als Rückfall, Deutsch).
        resourceConfigurations += listOf("en", "de")
    }

    androidResources {
        // Sprachliste für die Android-Einstellung „App-Sprache“ (ab Android 13) automatisch erzeugen.
        generateLocaleConfig = true
    }

    // Der Signaturschlüssel kommt aus Umgebungsvariablen (auf GitHub aus den Secrets).
    // Auf GitHub (CI=true) ist er Pflicht, damit nie eine unsignierte APK veröffentlicht wird.
    val keystoreFile = System.getenv("KEYSTORE_FILE")
    if (System.getenv("CI") == "true" && keystoreFile == null) {
        throw GradleException("Signaturschlüssel fehlt (KEYSTORE_FILE). Ohne Signatur wird auf GitHub nicht gebaut.")
    }
    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (keystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            // R8 entfernt ungenutzten Code und ungenutzte Ressourcen und optimiert den Rest.
            // Stürzt die Release-APK ab, fehlt meist eine Schutzregel in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
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
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
