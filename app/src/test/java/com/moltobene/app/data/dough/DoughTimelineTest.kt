package com.moltobene.app.data.dough

import com.moltobene.app.data.PhasePlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zeitplan für Pizza und Brot (#71). */
class DoughTimelineTest {

    private fun input(type: DoughType, preferment: Preferment) =
        LeavenedInput(type, 4, 250.0, 62.0, 2.5, 0.0, YeastKind.FRESH, 0.2, preferment, 30.0, 100.0)

    private fun phases(type: DoughType, preferment: Preferment) = DoughTimeline.build(input(type, preferment), GermanTexts)

    @Test
    fun bigaReiftStandardmaessigEinenTagImKuehlschrank() {
        val ripen = phases(DoughType.PIZZA, Preferment.BIGA).first { it.text.startsWith("PREFERMENT_RIPEN") }
        assertEquals(24 * 60, ripen.minMinutes)
        assertEquals(24 * 60, ripen.maxMinutes)
        assertEquals(PhasePlace.FRIDGE, ripen.place)
    }

    /** Die Gehzeiten aus dem Teigrechner stehen als eine Dauer im Zeitplan (Wunsch vom 05.11.2026). */
    @Test
    fun gewaehlteGehzeitenStehenImZeitplan() {
        val process = DoughProcessRules.defaults(DoughType.PIZZA, Preferment.BIGA).copy(ripen = 48.0, bulk = 1.5, proof = 3.0)
        val list = DoughTimeline.build(input(DoughType.PIZZA, Preferment.BIGA), GermanTexts, process, Kneading.MACHINE)
        fun minutes(prefix: String) = list.first { it.text.startsWith(prefix) }.let { it.minMinutes to it.maxMinutes }
        assertEquals(48 * 60 to 48 * 60, minutes("PREFERMENT_RIPEN"))
        assertEquals(90 to 90, minutes("BULK"))
        assertEquals(180 to 180, minutes("PROOF"))
        // Mit der Maschine geht das Kneten schneller.
        assertEquals(8 to 12, minutes("KNEAD"))
    }

    @Test
    fun teigkugelnAusDemKuehlschrankWaermenVorDemBackenAuf() {
        val room = DoughProcessRules.defaults(DoughType.PIZZA, Preferment.BIGA)
        val cold = DoughProcessRules.withPlace(room, DoughType.PIZZA, Preferment.BIGA, RestPlace.FRIDGE)
        val kinds = DoughTimeline.build(input(DoughType.PIZZA, Preferment.BIGA), GermanTexts, cold).map { it.text.substringBefore(':') }
        assertEquals(listOf("PREFERMENT_MIX", "PREFERMENT_RIPEN", "KNEAD", "BULK", "SHAPE", "PROOF", "WARM_UP", "BAKE"), kinds)
        val proof = DoughTimeline.build(input(DoughType.PIZZA, Preferment.BIGA), GermanTexts, cold).first { it.text.startsWith("PROOF") }
        assertEquals(PhasePlace.FRIDGE, proof.place)
        // Brot aus dem Kühlschrank backt direkt, ohne Aufwärmen.
        val bread = DoughProcessRules.withPlace(DoughProcessRules.defaults(DoughType.BREAD, Preferment.NONE), DoughType.BREAD, Preferment.NONE, RestPlace.FRIDGE)
        val breadKinds = DoughTimeline.build(input(DoughType.BREAD, Preferment.NONE), GermanTexts, bread).map { it.text.substringBefore(':') }
        assertEquals(listOf("KNEAD", "BULK", "SHAPE", "PROOF", "BAKE"), breadKinds)
    }

    @Test
    fun ohneVorteigGibtEsKeineVorteigPhasen() {
        val kinds = phases(DoughType.BREAD, Preferment.NONE).map { it.text.substringBefore(':') }
        assertEquals(listOf("KNEAD", "BULK", "SHAPE", "PROOF", "BAKE"), kinds)
    }

    @Test
    fun jedePhaseHatGueltigeDauerUndBackenSchliesstAb() {
        for (type in listOf(DoughType.PIZZA, DoughType.BREAD)) {
            for (preferment in Preferment.entries) {
                val list = phases(type, preferment)
                assertTrue("$type $preferment", list.all { it.minMinutes in 1..it.maxMinutes })
                assertEquals(PhasePlace.OVEN, list.last().place)
            }
        }
    }

    @Test
    fun dauerEinheitIstGanzeTageStundenOderMinuten() {
        assertEquals(DurationUnit.DAYS, DurationUnit.of(24 * 60, 3 * 24 * 60))
        assertEquals(DurationUnit.HOURS, DurationUnit.of(8 * 60, 16 * 60))
        assertEquals(DurationUnit.HOURS, DurationUnit.of(60, 3 * 24 * 60 + 60 * 5))
        assertEquals(DurationUnit.MINUTES, DurationUnit.of(45, 90))
        assertEquals(3, DurationUnit.DAYS.value(3 * 24 * 60))
    }
}
