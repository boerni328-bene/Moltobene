package com.moltobene.app.data.dough

import com.moltobene.app.data.DoughRun
import com.moltobene.app.data.TimelinePhase

/**
 * Rechnet aus Zeitplan und Start eines laufenden Teigs (#71) aus, wann welche Phase frühestens und spätestens zu Ende
 * ist. Alles sind Zeitpunkte (Epoch-Millisekunden): Das Drehen des Handys oder das Beenden der App ändert nichts.
 * Rein rechnerisch, ohne Uhr: „jetzt“ wird übergeben.
 */
object DoughRunPlanner {

    private const val MINUTE_MS = 60_000L

    /**
     * Zeiten einer Phase. Ist sie abgeschlossen, sind [endEarliest] und [endLatest] der wirkliche Abschluss.
     * Folgende Phasen beginnen frühestens beim frühesten, spätestens beim spätesten Ende der vorigen.
     */
    data class PhaseTime(val startEarliest: Long, val startLatest: Long, val endEarliest: Long, val endLatest: Long, val done: Boolean)

    /** [current] ist die Nummer der laufenden Phase; ist sie gleich der Anzahl der Phasen, ist der Teig fertig. */
    data class State(val phases: List<PhaseTime>, val current: Int) {
        val finished: Boolean get() = current >= phases.size
    }

    fun state(timeline: List<TimelinePhase>, run: DoughRun): State {
        var earliest = run.startedAt
        var latest = run.startedAt
        val times = timeline.mapIndexed { index, phase ->
            val doneAt = run.doneAt.getOrNull(index)
            val time = if (doneAt != null) {
                PhaseTime(earliest, latest, doneAt, doneAt, done = true)
            } else {
                PhaseTime(earliest, latest, earliest + phase.minMinutes * MINUTE_MS, latest + phase.maxMinutes * MINUTE_MS, done = false)
            }
            earliest = time.endEarliest
            latest = time.endLatest
            time
        }
        return State(times, run.doneAt.size.coerceAtMost(timeline.size))
    }

    /** Schließt die laufende Phase zum Zeitpunkt [now] ab; ein fertiger Teig bleibt, wie er ist. */
    fun complete(run: DoughRun, phaseCount: Int, now: Long): DoughRun {
        if (run.doneAt.size >= phaseCount) return run
        val previous = run.doneAt.lastOrNull() ?: run.startedAt
        // Nie vor dem Ende der vorigen Phase, auch wenn die Uhr des Handys umgestellt wurde.
        return run.copy(doneAt = run.doneAt + maxOf(now, previous))
    }

    /** Nimmt den letzten Abschluss zurück (wenn irrtümlich getippt). */
    fun undo(run: DoughRun): DoughRun = run.copy(doneAt = run.doneAt.dropLast(1))

    /** Die laufende Phase ist über ihr spätestes Ende hinaus (Teig womöglich überreif). */
    fun overdue(state: State, now: Long): Boolean = !state.finished && now > state.phases[state.current].endLatest
}
