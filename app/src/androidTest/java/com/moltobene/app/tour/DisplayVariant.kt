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
) {
    LIGHT(folder = "hell", language = "de", dark = false, fontScale = 1f),
    DARK(folder = "dunkel", language = "de", dark = true, fontScale = 1f),
    LARGE_TEXT(folder = "schrift-200", language = "de", dark = false, fontScale = 2f),
    ENGLISH(folder = "englisch", language = "en", dark = false, fontScale = 1f),
    /** Pseudosprache „en-XA“: Texte etwa 30 % länger, mit Akzenten und Klammern – zeigt abgeschnittene Texte. */
    PSEUDO(folder = "pseudo", language = "en-XA", dark = false, fontScale = 1f, contentLanguage = "en"),
}
