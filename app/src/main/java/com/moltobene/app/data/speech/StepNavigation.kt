package com.moltobene.app.data.speech

/** „Weiter“ und „Zurück“ in der Kochleiste (#79): von einem Schritt zum nächsten, ohne Schleife. */
object StepNavigation {

    /** Der nächste Schritt; ohne Markierung der erste, null nach dem letzten (und wenn es keine Schritte gibt). */
    fun next(current: Int?, count: Int): Int? = when {
        count <= 0 -> null
        current == null -> 0
        current + 1 < count -> current + 1
        else -> null
    }

    /** Der Schritt davor; ohne Markierung und beim ersten bleibt es der erste, null nur ohne Schritte. */
    fun previous(current: Int?, count: Int): Int? = when {
        count <= 0 -> null
        current == null || current <= 0 -> 0
        else -> minOf(current - 1, count - 1)
    }
}
