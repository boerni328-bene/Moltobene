package com.moltobene.app.data.voice

/**
 * Macht aus dem, was die freie Spracherkennung liefert (eine Äußerung je Sprechpause, alles klein, ohne Satzzeichen),
 * einen Text, den „Aus Text übernehmen“ lesen kann: gesprochene Zahlen als Ziffern ([SpokenNumbers]), je Äußerung eine
 * Zeile mit großem Anfangsbuchstaben. Überschriften wie „Zutaten“ und „Zubereitung“ stehen so auf eigener Zeile, wenn man
 * sie mit Pause davor und danach spricht. Reines Kotlin, per Unit-Test prüfbar.
 */
object SpokenRecipe {

    /** Der Text zu den erkannten [utterances]; leere Äußerungen fallen weg. */
    fun text(utterances: List<String>): String = utterances
        .map { SpokenNumbers.convert(it.trim()) }
        .filter { it.isNotEmpty() }
        .joinToString("\n") { line -> line.replaceFirstChar { it.uppercase() } }
}
