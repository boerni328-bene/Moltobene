package com.moltobene.app.data.dough

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Hefe-Automatik (Entwurf 8, 0.75.0): Hefe nach Gehzeit und Temperatur. */
class DoughYeastTest {

    private val pizza = DoughType.PIZZA
    private val bread = DoughType.BREAD

    private fun auto(
        preferment: Preferment,
        type: DoughType = pizza,
        kitchen: DoughKitchen = DoughKitchen(),
        process: DoughProcess = DoughProcessRules.defaults(type, preferment),
        prefermentFlour: Double = 30.0,
        kind: YeastKind = YeastKind.FRESH,
    ): Double? {
        val effective = DoughProcessRules.effective(process, kitchen, type, preferment)
        return DoughYeast.auto(type, preferment, prefermentFlour, kind, effective, kitchen)
    }

    @Test
    fun standardwerteErgebenWieBisher02Prozent() {
        // Pizza ohne Vorteig: 2 Std. Raum, 24 Std. Kühlschrank, 2 Std. aufwärmen ≈ 8 Std. bei 24 °C.
        assertEquals(0.2, auto(Preferment.NONE)!!, 1e-9)
        assertEquals(0.2, auto(Preferment.BIGA)!!, 1e-9)
        assertEquals(0.2, auto(Preferment.POOLISH)!!, 1e-9)
    }

    @Test
    fun gaerfaktor() {
        assertEquals(1.0, DoughYeast.factor(24.0), 1e-9)
        assertEquals(2.0, DoughYeast.factor(32.0), 1e-9)
        assertEquals(0.5, DoughYeast.factor(16.0), 1e-9)
    }

    @Test
    fun laengereGehzeitBrauchtWenigerHefe() {
        val base = DoughProcessRules.defaults(pizza, Preferment.NONE)
        val longer = base.copy(proof = 48.0, manual = setOf(RestTime.PROOF))
        assertTrue(auto(Preferment.NONE, process = longer)!! < auto(Preferment.NONE)!!)
        val bigaLonger = DoughProcessRules.defaults(pizza, Preferment.BIGA).copy(ripen = 48.0)
        assertEquals(0.1, auto(Preferment.BIGA, process = bigaLonger)!!, 1e-9)
    }

    @Test
    fun waermererKuehlschrankBrauchtWenigerHefe() {
        val warm = auto(Preferment.BIGA, kitchen = DoughKitchen(fridge = 8.0))!!
        assertTrue(warm < 0.2)
    }

    @Test
    fun mehrVorteigMehrHefe() {
        assertEquals(0.33, auto(Preferment.BIGA, prefermentFlour = 50.0)!!, 1e-9)
    }

    @Test
    fun hefeartWirdUmgerechnet() {
        // Instanthefe: ein Drittel der Frischhefe, auf 0,01 gerundet.
        assertEquals(0.07, auto(Preferment.NONE, kind = YeastKind.INSTANT_DRY)!!, 1e-9)
        assertEquals(0.08, auto(Preferment.NONE, kind = YeastKind.ACTIVE_DRY)!!, 1e-9)
    }

    @Test
    fun brotMitRaumGehzeiten() {
        // 2 Std. + 1 Std. bei 22 °C ≈ 2,52 Std. bei 24 °C → 0,63 %.
        assertEquals(0.63, auto(Preferment.NONE, type = bread)!!, 1e-9)
    }

    @Test
    fun sauerteigUndFehlendeWerteOhneAutomatik() {
        assertFalse(DoughYeast.applies(Preferment.SOURDOUGH))
        assertNull(auto(Preferment.SOURDOUGH))
        assertNull(auto(Preferment.BIGA, prefermentFlour = Double.NaN))
        assertNull(auto(Preferment.POOLISH, prefermentFlour = 0.0))
    }

    @Test
    fun bleibtInDenGrenzen() {
        // Sehr kurz und kalt: mehr als die Grenze von 5 % wird auf 5 % begrenzt.
        val short = DoughProcessRules.defaults(pizza, Preferment.NONE).copy(
            bulk = 0.5, proof = 6.0, warm = 0.5, manual = setOf(RestTime.BULK, RestTime.PROOF, RestTime.WARM),
        )
        val cold = DoughKitchen(room = 10.0, fridge = 0.0)
        val value = auto(Preferment.NONE, kitchen = cold, process = short)!!
        assertTrue(value <= 5.0 && value >= 0.01)
    }

    @Test
    fun automatikWirdGemerktUndAlteWerteBeginnenMitIhr() {
        val off = DoughProcessRules.defaults(pizza, Preferment.NONE).copy(yeastAuto = false)
        assertFalse(DoughProcessMemory.decode(pizza, Preferment.NONE, DoughProcessMemory.encode(off)).yeastAuto)
        assertTrue(DoughProcessMemory.decode(pizza, Preferment.NONE, "bulk=2.0;proof=24.0").yeastAuto)
        // Ein anderer Vorteig behält die Wahl.
        assertFalse(DoughProcessRules.forPreferment(off, pizza, Preferment.BIGA).yeastAuto)
    }
}
