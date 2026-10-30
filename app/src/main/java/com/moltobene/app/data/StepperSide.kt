package com.moltobene.app.data

/**
 * „Plus und Minus“ im Teigrechner: auf welcher Seite des Werts beide Knöpfe nebeneinander stehen, zum Beispiel für
 * Linkshänder. Gespeichert wird mit fester englischer Kennung in [AppPreferences], nicht in der Sicherung; unbekannte
 * Werte werden zum Standard „rechts“.
 */
enum class StepperSide(val key: String) {
    RIGHT("right"),
    LEFT("left");

    companion object {
        fun fromKey(key: String?): StepperSide = entries.firstOrNull { it.key == key } ?: RIGHT
    }
}
