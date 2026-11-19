package com.moltobene.app.data.dough

import com.moltobene.app.data.DoughRun
import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.TimelinePhase
import java.time.Instant
import java.time.ZoneId

/**
 * Uhrzeiten rückwärts vom Backtermin (0.77.0, Entwürfe 7A und 7D, Wahl des Projektinhabers vom 18.11.2026 „Backzeit
 * bleibt“). Der Backtermin ist der Beginn der letzten Phase („Backen“). Geplant wird mit der längsten Dauer jeder Phase,
 * damit der Teig rechtzeitig fertig ist. Wer früher oder später startet, behält den Backtermin: Die längste Ruhezeit
 * (Biga oder Poolish reifen, sonst meist die Teigkugeln im Kühlschrank) wird entsprechend länger oder kürzer, höchstens
 * auf die Hälfte bzw. das Dreifache. Reicht das nicht, verschiebt sich der Backtermin, und die App sagt es.
 * Reines Kotlin ohne Uhr: „jetzt“ und die Zeitzone werden übergeben.
 */
object DoughBakePlan {

    private const val MINUTE_MS = 60_000L
    private const val HALF_HOUR_MS = 30 * MINUTE_MS

    /** Nachtschritte beginnen zwischen 22 und 7 Uhr. */
    const val NIGHT_FROM = 22
    const val NIGHT_UNTIL = 7

    /** Eine Phase mit geplantem Beginn und Ende. */
    data class Slot(val start: Long, val end: Long, val night: Boolean, val past: Boolean)

    /**
     * Die Phase, die beim Start den Backtermin hält: die längste Ruhezeit mit fester Dauer bei Raumtemperatur oder im
     * Kühlschrank; null, wenn es keine gibt (dann verschiebt sich der Backtermin mit dem Start).
     */
    fun flexIndex(timeline: List<TimelinePhase>): Int? = timeline.indices
        .filter { i ->
            val phase = timeline[i]
            i < timeline.lastIndex && phase.minMinutes == phase.maxMinutes && phase.minMinutes > 0 &&
                (phase.place == PhasePlace.ROOM || phase.place == PhasePlace.FRIDGE)
        }
        .maxByOrNull { timeline[it].maxMinutes }

    /** Kürzeste und längste Dauer der Phase [flexIndex] in Minuten: die Hälfte bis das Dreifache. */
    fun flexRange(phase: TimelinePhase): IntRange =
        (phase.maxMinutes / 2).coerceAtLeast(1)..(phase.maxMinutes * 3).coerceAtMost(TimelinePhase.MAX_MINUTES)

    /** Minuten vom Start bis zum Backen (alle Phasen außer der letzten, jeweils die längste Dauer), mit [stretch]. */
    fun minutesBeforeBake(timeline: List<TimelinePhase>, stretch: Int = 0): Int =
        timeline.dropLast(1).sumOf { it.maxMinutes } + stretch

    /** Geplanter Start für den Backtermin [bakeAt]. */
    fun startFor(timeline: List<TimelinePhase>, bakeAt: Long): Long = bakeAt - minutesBeforeBake(timeline) * MINUTE_MS

    /** Frühester Backtermin bei Start [now], auf die nächste halbe Stunde aufgerundet. */
    fun earliestBake(timeline: List<TimelinePhase>, now: Long): Long {
        val bake = now + minutesBeforeBake(timeline) * MINUTE_MS
        return (bake + HALF_HOUR_MS - 1) / HALF_HOUR_MS * HALF_HOUR_MS
    }

    /**
     * Wie viele Minuten die Phase [flexIndex] länger (oder kürzer) sein muss, damit ein Start bei [now] beim Backtermin
     * [bakeAt] ankommt; begrenzt auf [flexRange]. 0 ohne solche Phase.
     */
    fun stretchFor(timeline: List<TimelinePhase>, bakeAt: Long, now: Long): Int {
        val index = flexIndex(timeline) ?: return 0
        val phase = timeline[index]
        val needed = ((bakeAt - now) / MINUTE_MS - minutesBeforeBake(timeline)).toInt()
        val range = flexRange(phase)
        return (phase.maxMinutes + needed).coerceIn(range.first, range.last) - phase.maxMinutes
    }

    /** Backtermin bei Start [now] mit [stretch]. */
    fun bakeWhenStarted(timeline: List<TimelinePhase>, now: Long, stretch: Int): Long =
        now + minutesBeforeBake(timeline, stretch) * MINUTE_MS

    /** Ein Teig, der jetzt beginnt und den Backtermin [bakeAt] so gut wie möglich hält. */
    fun startNow(timeline: List<TimelinePhase>, run: DoughRun, bakeAt: Long, now: Long): DoughRun {
        val stretch = stretchFor(timeline, bakeAt, now)
        val bake = bakeWhenStarted(timeline, now, stretch)
        // Reicht die Ruhezeit, bleibt der Backtermin genau (die Rechnung in Minuten verliert sonst Sekunden).
        val kept = flexIndex(timeline) != null && kotlin.math.abs(bake - bakeAt) < MINUTE_MS
        return run.copy(
            startedAt = now,
            planned = false,
            doneAt = emptyList(),
            stretchMinutes = stretch,
            bakeAt = if (kept) bakeAt else bake,
        )
    }

    /** Die Phasen eines Teigs mit angepasster Ruhezeit ([DoughRun.stretchMinutes]). */
    fun adjusted(timeline: List<TimelinePhase>, stretch: Int): List<TimelinePhase> {
        if (stretch == 0) return timeline
        val index = flexIndex(timeline) ?: return timeline
        return timeline.mapIndexed { i, phase ->
            if (i != index) {
                phase
            } else {
                val minutes = (phase.maxMinutes + stretch).coerceIn(0, TimelinePhase.MAX_MINUTES)
                phase.copy(minMinutes = minutes, maxMinutes = minutes)
            }
        }
    }

    /** Uhrzeiten aller Phasen rückwärts vom Backtermin [bakeAt]; Nachtschritte und schon vergangene Beginne markiert. */
    fun slots(timeline: List<TimelinePhase>, bakeAt: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Slot> {
        var start = startFor(timeline, bakeAt)
        return timeline.map { phase ->
            val end = start + phase.maxMinutes * MINUTE_MS
            val slot = Slot(start, end, night = isNight(start, zone), past = start < now)
            start = end
            slot
        }
    }

    /** Beginnt um [time] in der Nacht (22 bis 7 Uhr)? */
    fun isNight(time: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val hour = Instant.ofEpochMilli(time).atZone(zone).hour
        return hour >= NIGHT_FROM || hour < NIGHT_UNTIL
    }
}
