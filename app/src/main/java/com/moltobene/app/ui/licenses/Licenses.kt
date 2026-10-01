package com.moltobene.app.ui.licenses

import androidx.annotation.StringRes
import com.moltobene.app.R

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
            Component("Tesseract OCR", "Google, Tesseract contributors"),
            Component("tessdata_fast", "Google, Tesseract contributors"),
            Component("Tesseract4Android", "Robert Pösel, Robert Theis (tess-two)"),
            Component("Android Jetpack (AndroidX, Compose, Room, Navigation)", "The Android Open Source Project"),
            Component("Material Icons", "Google"),
            Component("Kotlin, kotlinx.coroutines, kotlinx.serialization", "JetBrains s.r.o. and Kotlin contributors"),
            Component("JetBrains Java Annotations", "JetBrains s.r.o."),
            Component("Coil", "Coil Contributors"),
            Component("Okio", "Square, Inc."),
        ),
    ),
    LicenseGroup(
        licenseName = "BSD 2-Clause License",
        file = "leptonica.txt",
        components = listOf(Component("Leptonica", "Leptonica")),
    ),
    LicenseGroup(
        licenseName = "Independent JPEG Group License",
        file = "libjpeg.txt",
        components = listOf(Component("libjpeg", "Thomas G. Lane, Guido Vollbeding")),
        note = R.string.license_ijg_note,
    ),
    LicenseGroup(
        licenseName = "PNG Reference Library License version 2",
        file = "libpng.txt",
        components = listOf(Component("libpng", "The PNG Reference Library Authors")),
    ),
)
