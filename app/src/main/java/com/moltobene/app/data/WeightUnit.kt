package com.moltobene.app.data

/**
 * „Gewichte“ im Teigrechner: Gramm oder Unzen. Gerechnet wird immer in Gramm; die Einheit gilt nur für Eingabe, Anzeige
 * und das gespeicherte Rezept. Gespeichert wird mit fester englischer Kennung in [AppPreferences], nicht in der
 * Sicherung; unbekannte Werte werden zum Standard „Gramm“.
 */
enum class WeightUnit(val key: String, val label: String, val gramsPerUnit: Double) {
    GRAMS("grams", "g", 1.0),
    OUNCES("ounces", "oz", 28.349523125),
    ;

    fun fromGrams(grams: Double): Double = grams / gramsPerUnit

    fun toGrams(value: Double): Double = value * gramsPerUnit

    companion object {
        fun fromKey(key: String?): WeightUnit = entries.firstOrNull { it.key == key } ?: GRAMS
    }
}
