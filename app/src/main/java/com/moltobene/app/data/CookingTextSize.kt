package com.moltobene.app.data

/**
 * Schriftgröße für Zutaten, Schritte und Notizen in der Rezeptansicht (#61): klein, mittel (Standard) oder groß.
 * Gespeichert wird die feste englische Kennung [key].
 */
enum class CookingTextSize(val key: String) {
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large"),
    ;

    companion object {
        /** Die Größe zu einer gespeicherten Kennung; unbekannt oder leer ergibt [MEDIUM]. */
        fun fromKey(key: String?): CookingTextSize = entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}
