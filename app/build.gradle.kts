plugins {
    // Kotlin übersetzt das Android-Bauwerkzeug seit AGP 9 selbst; ein eigenes Kotlin-Plugin ist nicht mehr nötig.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.moltobene.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.moltobene.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 35
        versionName = "0.17.0"

        // Rundgang durch die App auf dem Emulator (app/src/androidTest).
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Texterkennung (ONNX Runtime): nur die Prozessoren heutiger Handys, das hält die App kleiner.
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    androidResources {
        // Nur die unterstützten Sprachen mitliefern (Englisch als Rückfall, Deutsch).
        localeFilters += listOf("en", "de")
        // Sprachliste für die Android-Einstellung „App-Sprache“ (ab Android 13) automatisch erzeugen.
        generateLocaleConfig = true
    }

    packaging {
        // Programmbibliotheken der Texterkennung gepackt ausliefern (#45): Download und Updates werden
        // deutlich kleiner; Android entpackt bei der Installation nur die passende Variante.
        jniLibs {
            useLegacyPackaging = true
        }
    }

    // Der Signaturschlüssel kommt aus Umgebungsvariablen (auf GitHub aus den Secrets).
    // Auf GitHub (CI=true) ist er Pflicht, damit nie eine unsignierte APK veröffentlicht wird.
    // Ausnahme: Der Rundgang auf dem Emulator baut nur Debug-Versionen und bekommt keine Secrets.
    val keystoreFile = System.getenv("KEYSTORE_FILE")
    val onlyDebugTasks = gradle.startParameter.taskNames.let { tasks -> tasks.isNotEmpty() && tasks.all { "Debug" in it } }
    if (System.getenv("CI") == "true" && keystoreFile == null && !onlyDebugTasks) {
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
        debug {
            // Nur für die Debug-Version (Rundgang auf dem Emulator): Die Texterkennung läuft dort ohne
            // Übersetzung der Handy-Prozessorbefehle. Die veröffentlichte App bleibt bei den Handy-Prozessoren.
            ndk {
                abiFilters += "x86_64"
            }
        }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Ohne Animationen laufen die Tests auf dem Emulator gleichmäßiger.
        animationsDisabled = true
    }
}

// Unit-Tests laufen auf dem PC: Dort braucht ONNX Runtime die PC-Fassung, die Android-Fassung würde sie verdecken.
configurations.configureEach {
    if (name.endsWith("UnitTestRuntimeClasspath")) {
        exclude(group = "com.microsoft.onnxruntime", module = "onnxruntime-android")
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
    implementation(libs.onnxruntime.android)
    implementation(libs.jsoup)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.onnxruntime.jvm)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
}
