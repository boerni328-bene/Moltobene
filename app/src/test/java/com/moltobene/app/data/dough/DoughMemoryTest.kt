package com.moltobene.app.data.dough

import org.junit.Assert.assertEquals
import org.junit.Test

/** Gemerkte Werte des Teigrechners (#69): hin und zurück, und kaputte Werte werden nie angewendet. */
class DoughMemoryTest {

    @Test
    fun hinUndZurueck() {
        val input = DoughLimits.defaults(DoughType.PIZZA).copy(
            count = 6, pieceGrams = 270.0, water = 62.5, yeastKind = YeastKind.INSTANT_DRY, yeast = 0.15,
            preferment = Preferment.BIGA, prefermentFlour = 40.0, prefermentWater = 45.0,
        )
        assertEquals(input, DoughMemory.decode(DoughType.PIZZA, DoughMemory.encode(input)))
    }

    @Test
    fun ohneOderKaputtGibtStandardwerte() {
        val defaults = DoughLimits.defaults(DoughType.PIZZA)
        assertEquals(defaults, DoughMemory.decode(DoughType.PIZZA, null))
        assertEquals(defaults, DoughMemory.decode(DoughType.PIZZA, ""))
        assertEquals(defaults, DoughMemory.decode(DoughType.PIZZA, "Unsinn ohne Gleichheitszeichen;=;;a=b=c"))
    }

    @Test
    fun einzelneKaputteWerteFallenAufDenStandardZurueck() {
        val defaults = DoughLimits.defaults(DoughType.PIZZA)
        val broken = "count=abc;piece=1e99;water=NaN;salt=-3;oil=Infinity;yeastKind=xyz;yeast=;preferment=zzz;prefermentFlour=500"
        assertEquals(defaults, DoughMemory.decode(DoughType.PIZZA, broken))
        // Gültige Werte daneben bleiben erhalten.
        val mixed = DoughMemory.decode(DoughType.PIZZA, "count=8;water=1e99;salt=2")
        assertEquals(8, mixed.count)
        assertEquals(defaults.water, mixed.water, 0.0)
        assertEquals(2.0, mixed.salt, 0.0)
    }

    @Test
    fun grenzenGeltenJeTeigartUndBeiSpaeterAenderung() {
        // 3 kg pro Laib gibt es bei Brot, nicht bei Pizza.
        val text = "piece=3000"
        assertEquals(3000.0, DoughMemory.decode(DoughType.BREAD, text).pieceGrams, 0.0)
        assertEquals(DoughLimits.defaults(DoughType.PIZZA).pieceGrams, DoughMemory.decode(DoughType.PIZZA, text).pieceGrams, 0.0)
        // Hefe 0 gilt nur mit Sauerteig.
        assertEquals(0.0, DoughMemory.decode(DoughType.BREAD, "preferment=sourdough;yeast=0.0").yeast, 0.0)
        assertEquals(DoughLimits.defaults(DoughType.BREAD).yeast, DoughMemory.decode(DoughType.BREAD, "yeast=0.0").yeast, 0.0)
    }
}
