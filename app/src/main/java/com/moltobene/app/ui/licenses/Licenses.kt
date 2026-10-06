package com.moltobene.app.ui.licenses

import androidx.annotation.StringRes

/**
 * Quelloffene Bausteine der App und ihre Lizenzen (Lizenztexte unter assets/licenses/).
 * Kommt ein Baustein hinzu, gehört er hier in die Liste. Namen von Bausteinen, Urhebern und
 * Lizenzen sind Eigennamen und werden nicht übersetzt.
 */
internal data class LicenseGroup(
    val licenseName: String,
    val file: String,
    val components: List<Component>,
    /** Pflichthinweis der Lizenz, falls sie einen verlangt. */
    @StringRes val note: Int? = null,
)

internal data class Component(val name: String, val holder: String)

internal val LICENSE_GROUPS = listOf(
    LicenseGroup(
        licenseName = "Apache License 2.0",
        file = "apache-2.0.txt",
        components = listOf(
            Component("PaddleOCR (PP-OCRv6)", "PaddlePaddle Authors"),
            Component("OnnxOCR", "jingsongliujing"),
            Component("Android Jetpack (AndroidX, Compose, Room, Navigation)", "The Android Open Source Project"),
            Component("Material Icons", "Google"),
            Component("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains s.r.o. and Kotlin contributors"),
            Component("JetBrains Java Annotations", "JetBrains s.r.o."),
            Component("Coil", "Coil Contributors"),
            Component("Okio", "Square, Inc."),
        ),
    ),
    LicenseGroup(
        licenseName = "MIT License",
        file = "mit-onnxruntime.txt",
        components = listOf(Component("ONNX Runtime", "Microsoft Corporation")),
    ),
    LicenseGroup(
        licenseName = "MIT License",
        file = "mit-jsoup.txt",
        components = listOf(Component("jsoup", "Jonathan Hedley")),
    ),
    LicenseGroup(
        licenseName = "Third Party Notices",
        file = "onnxruntime-third-party.txt",
        components = listOf(Component("ONNX Runtime", "Microsoft Corporation")),
    ),
)
