package com.moltobene.app.data.dough

import com.moltobene.app.data.DoughRun
import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.TimelinePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Uhrzeiten rückwärts vom Backtermin (0.77.0, Wahl des Projektinhabers „Backzeit bleibt“). */
class DoughBakePlanTest {

    private val min = 60_000L

    // Pizza mit Biga wie aus dem Teigrechner: feste Ruhezeiten, kurze Handgriffe mit Spanne.
    private val pizza = listOf(
        TimelinePhase("Biga ansetzen", 5, 15, PhasePlace.NONE),
        TimelinePhase("Biga reifen lassen", 1440, 1440, PhasePlace.FRIDGE),
        TimelinePhase("Teig kneten", 10, 20, PhasePlace.NONE),
        TimelinePhase("Stockgare", 120, 120, PhasePlace.ROOM),
        TimelinePhase("Teigkugeln formen", 10, 20, PhasePlace.NONE),
        TimelinePhase("Teigkugeln im Kühlschrank", 360, 360, PhasePlace.FRIDGE),
        TimelinePhase("Aufwärmen", 120, 120, PhasePlace.ROOM),
        TimelinePhase("Backen", 8, 15, PhasePlace.OVEN),
    )

    // 15 + 1440 + 20 + 120 + 20 + 360 + 120 Minuten bis zum Backen.
    private val before = 2095

    private fun at(text: String): Long = LocalDateTime.parse(text).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun startRueckwaertsVomBacktermin() {
        assertEquals(before, DoughBakePlan.minutesBeforeBake(pizza))
        val bake = at("2026-11-21T18:00")
        assertEquals(bake - before * min, DoughBakePlan.startFor(pizza, bake))
        // 2095 Minuten (34 h 55 min) vor Sa. 18:00 ist Fr. 07:05.
        assertEquals(at("2026-11-20T07:05"), DoughBakePlan.startFor(pizza, bake))
    }

    @Test
    fun fruehesterBackterminAufHalbeStundeAufgerundet() {
        val now = at("2026-11-19T10:00")
        // 10:00 + 34 h 55 min = Fr. 20:55, aufgerundet 21:00.
        assertEquals(at("2026-11-20T21:00"), DoughBakePlan.earliestBake(pizza, now))
        // Genau auf der halben Stunde bleibt es dabei.
        assertEquals(at("2026-11-20T21:00"), DoughBakePlan.earliestBake(pizza, at("2026-11-19T10:05")))
    }

    @Test
    fun laengsteRuhezeitHaeltDenBacktermin() {
        assertEquals(1, DoughBakePlan.flexIndex(pizza))
        assertEquals(720..4320, DoughBakePlan.flexRange(pizza[1]))
        // Ohne feste Ruhezeit gibt es nichts anzupassen; die letzte Phase zählt nie.
        assertNull(DoughBakePlan.flexIndex(listOf(TimelinePhase("Kneten", 10, 20), TimelinePhase("Backen", 600, 600, PhasePlace.ROOM))))
    }

    @Test
    fun spaeterStartenMachtDieBigaKuerzerDerBackterminBleibt() {
        val bake = at("2026-11-21T18:00")
        val now = DoughBakePlan.startFor(pizza, bake) + 300 * min
        val run = DoughBakePlan.startNow(pizza, DoughRun("r", "x", startedAt = 0, planned = true, doneAt = listOf(1L)), bake, now)
        assertEquals(-300, run.stretchMinutes)
        assertEquals(bake, run.bakeAt)
        assertEquals(now, run.startedAt)
        assertFalse(run.planned)
        assertTrue(run.doneAt.isEmpty())
        // Die laufende Rechnung kommt mit der kürzeren Biga genau beim Backtermin an.
        val state = DoughRunPlanner.state(pizza, run)
        assertEquals(bake, state.phases.last().startLatest)
        assertEquals(1140, DoughBakePlan.adjusted(pizza, run.stretchMinutes)[1].maxMinutes)
    }

    @Test
    fun frueherStartenMachtDieBigaLaenger() {
        val bake = at("2026-11-21T18:00")
        val now = DoughBakePlan.startFor(pizza, bake) - 600 * min
        val run = DoughBakePlan.startNow(pizza, DoughRun("r", "x", startedAt = 0), bake, now)
        assertEquals(600, run.stretchMinutes)
        assertEquals(bake, run.bakeAt)
    }

    @Test
    fun reichtDieRuhezeitNichtVerschiebtSichDerBacktermin() {
        val bake = at("2026-11-21T18:00")
        // 1000 Minuten zu spät: Die Biga darf höchstens auf 720 Minuten schrumpfen.
        val now = DoughBakePlan.startFor(pizza, bake) + 1000 * min
        val run = DoughBakePlan.startNow(pizza, DoughRun("r", "x", startedAt = 0), bake, now)
        assertEquals(-720, run.stretchMinutes)
        assertEquals(now + (before - 720) * min, run.bakeAt)
        assertTrue(run.bakeAt!! > bake)
    }

    @Test
    fun uhrzeitenMitNachtUndVergangenheit() {
        val bake = at("2026-11-21T12:00")
        val now = at("2026-11-20T12:00")
        val slots = DoughBakePlan.slots(pizza, bake, now, ZoneOffset.UTC)
        assertEquals(pizza.size, slots.size)
        // Start Fr. 01:05, Biga in den Kühlschrank 01:20, Kneten Sa. 01:20, Aufwärmen Sa. 10:00, Backen 12:00.
        assertEquals(at("2026-11-20T01:05"), slots[0].start)
        assertEquals(at("2026-11-20T01:20"), slots[1].start)
        assertEquals(at("2026-11-21T01:20"), slots[2].start)
        assertEquals(at("2026-11-21T10:00"), slots[6].start)
        assertEquals(bake, slots.last().start)
        assertTrue(slots[0].past)
        assertTrue(slots[1].past)
        assertFalse(slots[2].past)
        assertTrue(slots[0].night)
        assertTrue(slots[2].night)
        assertFalse(slots[6].night)
        assertFalse(slots.last().night)
        assertTrue(DoughBakePlan.isNight(at("2026-11-20T06:59"), ZoneOffset.UTC))
        assertFalse(DoughBakePlan.isNight(at("2026-11-20T07:00"), ZoneOffset.UTC))
        assertTrue(DoughBakePlan.isNight(at("2026-11-20T22:00"), ZoneOffset.UTC))
    }
}
