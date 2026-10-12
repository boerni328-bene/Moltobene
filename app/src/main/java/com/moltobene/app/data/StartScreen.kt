package com.moltobene.app.data

/**
 * „Beim Öffnen der App“ (Wunsch des Projektinhabers vom 12.10.2026, #81): Startseite mit Kacheln oder gleich die
 * Sammlung. Gespeichert mit fester englischer Kennung in [AppPreferences], nicht in der Sicherung; unbekannte Werte
 * werden zum Standard „Startseite“.
 */
enum class StartScreen(val key: String) {
    HOME("home"),
    COLLECTION("collection");

    companion object {
        fun fromKey(key: String?): StartScreen = entries.firstOrNull { it.key == key } ?: HOME
    }
}
