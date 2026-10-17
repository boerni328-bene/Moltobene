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
    fun bigaReiftEinsBisDreiTageImKuehlschrank() {
        val ripen = phases(DoughType.PIZZA, Preferment.BIGA).first { it.text.startsWith("PREFERMENT_RIPEN") }
        assertEquals(24 * 60, ripen.minMinutes)
        assertEquals(3 * 24 * 60, ripen.maxMinutes)
        assertEquals(PhasePlace.FRIDGE, ripen.place)
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
