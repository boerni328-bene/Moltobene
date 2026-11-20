package com.moltobene.app.data.dough

import com.moltobene.app.data.DoughRun
import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.TimelinePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zeitplan eines laufenden Teigs (#71): Zeiten werden aus Startzeit und Abschlüssen gerechnet. */
class DoughRunPlannerTest {

    private val minute = 60_000L
    private val hour = 60 * minute

    private val timeline = listOf(
        TimelinePhase("Biga reifen", 24 * 60, 72 * 60, PhasePlace.FRIDGE),
        TimelinePhase("Kneten", 10, 30, PhasePlace.NONE),
        TimelinePhase("Backen", 30, 50, PhasePlace.OVEN),
    )

    private fun run(vararg doneAt: Long) = DoughRun("r", "rezept", startedAt = 1000 * hour, doneAt = doneAt.toList())

    @Test
    fun frischGestarteterTeigHatFruehestesUndSpaetestesEnde() {
        val state = DoughRunPlanner.state(timeline, run())
        assertEquals(0, state.current)
        assertFalse(state.finished)
        assertEquals(1000 * hour + 24 * hour, state.phases[0].endEarliest)
        assertEquals(1000 * hour + 72 * hour, state.phases[0].endLatest)
        // Die nächste Phase beginnt frühestens beim frühesten, spätestens beim spätesten Ende der vorigen.
        assertEquals(state.phases[0].endEarliest, state.phases[1].startEarliest)
        assertEquals(state.phases[0].endLatest, state.phases[1].startLatest)
        assertEquals(state.phases[0].endLatest + 30 * minute, state.phases[1].endLatest)
    }

    @Test
    fun fruehererAbschlussZiehtDieFolgezeitenNach() {
        val early = 1000 * hour + 26 * hour
        val state = DoughRunPlanner.state(timeline, run(early))
        assertEquals(1, state.current)
        assertTrue(state.phases[0].done)
        assertEquals(early, state.phases[0].endEarliest)
        assertEquals(early, state.phases[0].endLatest)
        assertEquals(early + 10 * minute, state.phases[1].endEarliest)
        assertEquals(early + 30 * minute, state.phases[1].endLatest)
        assertEquals(early + 10 * minute + 30 * minute, state.phases[2].endEarliest)
    }

    @Test
    fun alleAbgeschlossenIstFertig() {
        val state = DoughRunPlanner.state(timeline, run(1001 * hour, 1002 * hour, 1003 * hour))
        assertTrue(state.finished)
        assertEquals(3, state.current)
        assertFalse(DoughRunPlanner.overdue(state, now = 5000 * hour))
    }

    @Test
    fun ueberschrittenesSpaetestesEndeWirdGemeldet() {
        val state = DoughRunPlanner.state(timeline, run())
        assertFalse(DoughRunPlanner.overdue(state, now = 1000 * hour + 72 * hour))
        assertTrue(DoughRunPlanner.overdue(state, now = 1000 * hour + 72 * hour + 1))
    }

    @Test
    fun abschliessenHaengtAnUndKenntKeinZurueck() {
        val first = DoughRunPlanner.complete(run(), 3, now = 1030 * hour)
        assertEquals(listOf(1030 * hour), first.doneAt)
        // Die Uhr wurde zurückgestellt: nie vor dem Ende der vorigen Phase.
        val second = DoughRunPlanner.complete(first, 3, now = 900 * hour)
        assertEquals(listOf(1030 * hour, 1030 * hour), second.doneAt)
        val done = DoughRunPlanner.complete(DoughRunPlanner.complete(second, 3, 1040 * hour), 3, 1050 * hour)
        assertEquals(3, done.doneAt.size)
    }

    @Test
    fun zuruecknehmenEntferntNurDenLetztenAbschluss() {
        val back = DoughRunPlanner.undo(run(1010 * hour, 1020 * hour))
        assertEquals(listOf(1010 * hour), back.doneAt)
        assertTrue(DoughRunPlanner.undo(run()).doneAt.isEmpty())
    }

    @Test
    fun mehrAbschluesseAlsPhasenWerdenBegrenzt() {
        // Wurde das Rezept nach dem Start gekürzt, bleibt der Lauf lesbar.
        val state = DoughRunPlanner.state(timeline.take(1), run(1001 * hour, 1002 * hour))
        assertEquals(1, state.current)
        assertTrue(state.finished)
    }

    @Test
    fun eigeneDauerGiltNurFuerDiesenTeig() {
        // Biga 48 statt 24 bis 72 Stunden (0.78.0): fest, die Folgezeiten rechnen ab da.
        val changed = DoughRunPlanner.setDuration(run(), 0, 48 * 60)
        assertEquals(mapOf(0 to 48 * 60), changed.durations)
        val state = DoughRunPlanner.state(timeline, changed)
        assertEquals(1000 * hour + 48 * hour, state.phases[0].endEarliest)
        assertEquals(1000 * hour + 48 * hour, state.phases[0].endLatest)
        assertEquals(1000 * hour + 48 * hour, state.phases[1].startEarliest)
        // Grenzen: mindestens 1 Minute, höchstens 14 Tage.
        assertEquals(1, DoughRunPlanner.setDuration(run(), 1, 0).durations[1])
        assertEquals(TimelinePhase.MAX_MINUTES, DoughRunPlanner.setDuration(run(), 1, 99_999).durations[1])
    }

    @Test
    fun eigeneDauerGehtVorDerAnpassungBeimStart() {
        val fixed = listOf(TimelinePhase("Biga", 600, 600, PhasePlace.FRIDGE), TimelinePhase("Backen", 10, 15, PhasePlace.OVEN))
        val adjusted = DoughBakePlan.adjusted(fixed, stretch = 120, durations = mapOf(0 to 300))
        assertEquals(300, adjusted[0].maxMinutes)
        assertEquals(720, DoughBakePlan.adjusted(fixed, stretch = 120)[0].maxMinutes)
    }

    @Test
    fun fertigUhrzeitKorrigierenBleibtInDerReihenfolge() {
        val first = 1000 * hour + 25 * hour
        val second = 1000 * hour + 26 * hour
        val now = 1000 * hour + 30 * hour
        val r = run(first, second)
        // Später fertig als angetippt: die neue Uhrzeit gilt.
        assertEquals(listOf(first + 20 * minute, second), DoughRunPlanner.setDoneAt(r, 0, first + 20 * minute, now).doneAt)
        // Nie vor dem Start, nie nach dem nächsten Abschluss, nie in der Zukunft.
        assertEquals(1000 * hour, DoughRunPlanner.setDoneAt(r, 0, 0, now).doneAt[0])
        assertEquals(second, DoughRunPlanner.setDoneAt(r, 0, second + hour, now).doneAt[0])
        assertEquals(now, DoughRunPlanner.setDoneAt(r, 1, now + hour, now).doneAt[1])
        // Eine nicht erledigte Phase bleibt unverändert.
        assertEquals(r, DoughRunPlanner.setDoneAt(r, 2, now, now))
    }
}
