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
        // Seit 0.77.0 kann die längste Ruhezeit beim Start angepasst sein, damit der Backtermin bleibt.
        val times = DoughBakePlan.adjusted(timeline, run.stretchMinutes, run.durations).mapIndexed { index, phase ->
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

    /**
     * Ändert die Dauer der Phase [index] nur für diesen Teig (0.78.0); die folgenden Zeiten rechnen sich neu.
     * Höchstens 14 Tage, mindestens 1 Minute.
     */
    fun setDuration(run: DoughRun, index: Int, minutes: Int): DoughRun =
        run.copy(durations = run.durations + (index to minutes.coerceIn(1, TimelinePhase.MAX_MINUTES)))

    /**
     * Korrigiert, wann die erledigte Phase [index] fertig war (0.78.0), z. B. wenn „Fertig“ erst später angetippt
     * wurde. Nie vor dem Ende der vorigen Phase (bzw. dem Start), nie nach dem Ende der nächsten und nie nach [now].
     */
    fun setDoneAt(run: DoughRun, index: Int, time: Long, now: Long): DoughRun {
        if (index !in run.doneAt.indices) return run
        val lower = run.doneAt.getOrNull(index - 1) ?: run.startedAt
        val upper = run.doneAt.getOrNull(index + 1) ?: now
        val fixed = time.coerceAtMost(maxOf(upper, lower)).coerceAtLeast(lower)
        return run.copy(doneAt = run.doneAt.toMutableList().also { it[index] = fixed })
    }

    /** Nimmt den letzten Abschluss zurück (wenn irrtümlich getippt). */
    fun undo(run: DoughRun): DoughRun = run.copy(doneAt = run.doneAt.dropLast(1))

    /** Die laufende Phase ist über ihr spätestes Ende hinaus (Teig womöglich überreif). */
    fun overdue(state: State, now: Long): Boolean = !state.finished && now > state.phases[state.current].endLatest
}
