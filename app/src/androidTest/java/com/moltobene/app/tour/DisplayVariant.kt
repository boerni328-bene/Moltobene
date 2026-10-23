package com.moltobene.app.tour

/**
 * Darstellungen, in denen der Rundgang die App bedient und Bildschirmfotos macht.
 * [folder] ist der Ordner der Bildschirmfotos im Ergebnis des Testlaufs.
 */
enum class DisplayVariant(
    val folder: String,
    val language: String,
    val dark: Boolean,
    val fontScale: Float,
    /** Sprache der mitgelieferten Inhalte (Grundrezepte, Teigwissen); bei der Pseudosprache Englisch. */
    val contentLanguage: String = language,
    /** Größe des Bildschirms in Pixeln („Breite x Höhe“) für Tablet-Darstellungen; sonst der Bildschirm des Emulators. */
    val displaySize: String? = null,
    /** Pixeldichte der Tablet-Darstellung; 160 heißt: ein Pixel ist ein dp. */
    val density: Int = 160,
) {
    LIGHT(folder = "hell", language = "de", dark = false, fontScale = 1f),
    DARK(folder = "dunkel", language = "de", dark = true, fontScale = 1f),
    LARGE_TEXT(folder = "schrift-200", language = "de", dark = false, fontScale = 2f),
    ENGLISH(folder = "englisch", language = "en", dark = false, fontScale = 1f),
    /** Pseudosprache „en-XA“: Texte etwa 30 % länger, mit Akzenten und Klammern – zeigt abgeschnittene Texte. */
    PSEUDO(folder = "pseudo", language = "en-XA", dark = false, fontScale = 1f, contentLanguage = "en"),
    /** Tablet quer, 1280 × 800 dp (#91). Läuft nur im [TabletTourTest]. */
    TABLET_LANDSCAPE(folder = "tablet-quer", language = "de", dark = false, fontScale = 1f, displaySize = "1280x800"),
    /** Tablet hoch, 800 × 1280 dp (#91). Läuft nur im [TabletTourTest]. */
    TABLET_PORTRAIT(folder = "tablet-hoch", language = "de", dark = false, fontScale = 1f, displaySize = "800x1280"),
    ;

    val isTablet: Boolean get() = displaySize != null

    /** Breite des Fensters in dp, die diese Darstellung erwartet (nur Tablet). */
    val widthDp: Int? get() = displaySize?.substringBefore('x')?.toInt()?.let { it * 160 / density }
}
