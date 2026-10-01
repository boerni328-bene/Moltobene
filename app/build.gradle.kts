plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.moltobene.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moltobene.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "0.5.0"

        // Nur die unterstützten Sprachen mitliefern (Englisch als Rückfall, Deutsch).
        resourceConfigurations += listOf("en", "de")

        // Texterkennung: nur die Prozessoren heutiger Handys, das hält die App klein.
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
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

// Baupläne der Datenbank (für spätere Umbauten/Migrationen) werden in app/schemas abgelegt.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Nur Anzeige lokaler Fotos – bewusst ohne das Internet-Modul von Coil.
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.tesseract4android)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
