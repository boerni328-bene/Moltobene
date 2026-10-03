package com.moltobene.app.data.ocr

import java.nio.FloatBuffer

/**
 * Liest das Ergebnis des Lesemodells (CTC): Für jeden Schritt entlang der Zeile gibt es eine Wahrscheinlichkeit
 * je Zeichen. Genommen wird je Schritt das wahrscheinlichste; Wiederholungen und „kein Zeichen“ (Index 0) fallen weg.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object CtcDecoder {

    /** Gelesener Text einer Zeile und wie sicher er im Mittel gelesen wurde (0 bis 1). */
    data class Decoded(val text: String, val confidence: Float)

    /**
     * @param keys Zeichen des Modells: Index 0 ist „kein Zeichen“, danach die Zeichen aus der Zeichenliste,
     * zuletzt das Leerzeichen
     */
    fun decode(values: FloatBuffer, steps: Int, classes: Int, keys: List<String>): Decoded {
        val text = StringBuilder()
        var confidenceSum = 0f
        var emitted = 0
        var previous = -1
        for (step in 0 until steps) {
            val offset = step * classes
            var best = 0
            var bestValue = values.get(offset)
            for (index in 1 until classes) {
                val value = values.get(offset + index)
                if (value > bestValue) {
                    bestValue = value
                    best = index
                }
            }
            if (best != 0 && best != previous && best < keys.size) {
                text.append(keys[best])
                confidenceSum += bestValue
                emitted++
            }
            previous = best
        }
        return Decoded(text.toString().trim().replace(Regex("\\s{2,}"), " "), if (emitted == 0) 0f else confidenceSum / emitted)
    }

    /** Zeichen des Modells aus der Zeichenliste (ein Zeichen je Zeile): „kein Zeichen“ vorn, Leerzeichen hinten. */
    fun keys(dictionary: List<String>): List<String> = listOf("") + dictionary + listOf(" ")
}
