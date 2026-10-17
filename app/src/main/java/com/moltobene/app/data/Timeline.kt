package com.moltobene.app.data

/** Wo eine Phase des Zeitplans stattfindet (feste englische Kennung in der Datenbank und in der Sicherung). */
enum class PhasePlace(val key: String) {
    NONE("none"),
    ROOM("room"),
    FRIDGE("fridge"),
    OVEN("oven"),
    ;

    companion object {
        fun fromKey(key: String?): PhasePlace = entries.firstOrNull { it.key == key } ?: NONE
    }
}

/**
 * Eine Phase im Zeitplan eines Teigs (z. B. „Biga im Kühlschrank reifen lassen“, 12 bis 72 Stunden).
 * [minMinutes] ist der frühestmögliche, [maxMinutes] der späteste sinnvolle Abschluss ab Beginn der Phase.
 */
data class TimelinePhase(
    val text: String,
    val minMinutes: Int,
    val maxMinutes: Int,
    val place: PhasePlace = PhasePlace.NONE,
) {
    init {
        require(minMinutes in 0..MAX_MINUTES && maxMinutes in minMinutes..MAX_MINUTES) { "Dauer ungültig" }
    }

    companion object {
        /** Eine Phase dauert höchstens 14 Tage. */
        const val MAX_MINUTES = 14 * 24 * 60

        /** Höchstens so viele Phasen je Rezept; Fremdes (Sicherungsdatei) wird darauf begrenzt. */
        const val MAX_PHASES = 20

        /** Baut eine Phase aus gespeicherten Werten und rückt unmögliche Dauern zurecht, statt zu scheitern. */
        fun lenient(text: String, minMinutes: Int, maxMinutes: Int, place: PhasePlace): TimelinePhase {
            val min = minMinutes.coerceIn(0, MAX_MINUTES)
            return TimelinePhase(text, min, maxMinutes.coerceIn(min, MAX_MINUTES), place)
        }
    }
}

/**
 * Ein laufender Teig (#71): ein Rezept mit Zeitplan, das zu einem Zeitpunkt gestartet wurde.
 * Gespeichert werden Zeitpunkte (Epoch-Millisekunden), nie eine Restzeit. [doneAt] nennt je abgeschlossener Phase,
 * wann sie wirklich zu Ende war; die Phasen selbst kommen aus dem Rezept.
 */
data class DoughRun(
    val id: String,
    val recipeId: String,
    val startedAt: Long,
    val doneAt: List<Long> = emptyList(),
)
