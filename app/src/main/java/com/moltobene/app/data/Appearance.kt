package com.moltobene.app.data

/**
 * Darstellung der App aus den Einstellungen: hell, dunkel oder wie das Handy, und die Farbwelt. Gespeichert wird
 * mit fester englischer Kennung ([ThemeMode.key], [Palette.key]); unbekannte Werte werden zum Standard.
 */
data class Appearance(val mode: ThemeMode = ThemeMode.SYSTEM, val palette: Palette = Palette.SLATE)

enum class ThemeMode(val key: String) {
    /** Folgt der Einstellung des Handys (Standard). */
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    /** @param systemDark ist das Handy gerade dunkel eingestellt? */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** Farbwelten, in der Reihenfolge der Auswahl; „Schiefer“ ist der Standard. */
enum class Palette(val key: String) {
    SLATE("slate"),
    COBALT("cobalt"),
    SAGE("sage"),
    TERRACOTTA("terracotta"),
    SAFFRON("saffron");

    companion object {
        fun fromKey(key: String?): Palette = entries.firstOrNull { it.key == key } ?: SLATE
    }
}
