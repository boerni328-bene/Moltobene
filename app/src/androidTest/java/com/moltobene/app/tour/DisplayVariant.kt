package com.moltobene.app.tour

/**
 * Darstellungen, in denen der Rundgang die App bedient und Bildschirmfotos macht.
 * [folder] ist der Ordner der Bildschirmfotos im Ergebnis des Testlaufs.
 */
enum class DisplayVariant(val folder: String, val language: String, val dark: Boolean, val fontScale: Float) {
    LIGHT(folder = "hell", language = "de", dark = false, fontScale = 1f),
    DARK(folder = "dunkel", language = "de", dark = true, fontScale = 1f),
    LARGE_TEXT(folder = "schrift-200", language = "de", dark = false, fontScale = 2f),
    ENGLISH(folder = "englisch", language = "en", dark = false, fontScale = 1f),
}
