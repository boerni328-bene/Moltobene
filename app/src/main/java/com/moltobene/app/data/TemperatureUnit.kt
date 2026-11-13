package com.moltobene.app.data

import kotlin.math.roundToInt

/**
 * „Temperaturen“ im Teigrechner (Wunsch vom 05.11.2026): Grad Celsius oder Fahrenheit. Gerechnet und gemerkt wird
 * immer in °C; die Einheit gilt nur für Anzeige und das gespeicherte Rezept. Gespeichert wird mit fester englischer
 * Kennung in [AppPreferences], nicht in der Sicherung; unbekannte Werte werden zum Standard „Celsius“.
 */
enum class TemperatureUnit(val key: String, val label: String) {
    CELSIUS("celsius", "°C"),
    FAHRENHEIT("fahrenheit", "°F"),
    ;

    /** Ganze Zahl in dieser Einheit, z. B. 24 °C = 75 °F. */
    fun fromCelsius(celsius: Double): Int = (if (this == CELSIUS) celsius else celsius * 9.0 / 5.0 + 32.0).roundToInt()

    /** Temperatur als Text, z. B. „24 °C“ oder „75 °F“; Ziffern ohne Dezimalstelle sehen in jeder Sprache gleich aus. */
    fun format(celsius: Double): String = "${fromCelsius(celsius)} $label"

    /** Eine Schrittweite, die schon in dieser Einheit ist, z. B. „25 °F“ (nicht umgerechnet). */
    fun formatStep(step: Double): String = "${step.roundToInt()} $label"

    companion object {
        fun fromKey(key: String?): TemperatureUnit = entries.firstOrNull { it.key == key } ?: CELSIUS
    }
}
