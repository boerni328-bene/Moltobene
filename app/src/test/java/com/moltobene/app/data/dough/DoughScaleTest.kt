package com.moltobene.app.data.dough

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Menge oben antippen und ändern (Wunsch vom 18.11.2026): Der ganze Teig wird im gleichen Verhältnis umgerechnet. */
class DoughScaleTest {

    private val pizza = DoughLimits.range(DoughType.PIZZA, DoughField.PIECE_GRAMS)

    @Test
    fun gesamtteigAendertDasGewichtJeStueck() {
        // 4 × 250 g = 1000 g; 1500 g ergibt 375 g je Teigkugel.
        assertEquals(375.0, DoughScale.pieceGrams(250.0, 1000.0, 1500.0, pizza)!!, 1e-9)
    }

    @Test
    fun mehlRechnetDenGanzenTeigUm() {
        val input = LeavenedInput(DoughType.PIZZA, 4, 250.0, 60.0, 2.8, 0.0, YeastKind.FRESH, 0.2, Preferment.BIGA, 30.0, 50.0)
        val before = DoughCalculator.calculate(input) as DoughResult.Success
        val piece = DoughScale.pieceGrams(input.pieceGrams, before.flourGrams, 500.0, pizza)!!
        val after = DoughCalculator.calculate(input.copy(pieceGrams = piece)) as DoughResult.Success
        // Auf 0,1 g je Stück gerundet: das Mehl liegt danach bei 500 g.
        assertEquals(500.0, after.flourGrams, 0.5)
        // Die Prozente bleiben: Wasser weiter 60 % vom Mehl.
        val water = after.sections.flatMap { it.lines }.filter { it.role == LineRole.WATER }.sumOf { it.grams }
        assertEquals(0.6, water / after.flourGrams, 1e-9)
    }

    @Test
    fun bleibtInDenGrenzenUndLeereWerteRechnenNicht() {
        assertEquals(pizza.max, DoughScale.pieceGrams(250.0, 1000.0, 100_000.0, pizza)!!, 1e-9)
        assertEquals(pizza.min, DoughScale.pieceGrams(250.0, 1000.0, 1.0, pizza)!!, 1e-9)
        assertNull(DoughScale.pieceGrams(250.0, 1000.0, 0.0, pizza))
        assertNull(DoughScale.pieceGrams(250.0, 0.0, 500.0, pizza))
        assertNull(DoughScale.pieceGrams(250.0, 1000.0, Double.NaN, pizza))
    }

    @Test
    fun minusUndPlusAufDemRaster() {
        assertEquals(1010.0, DoughScale.next(1000.0, +1), 1e-9)
        assertEquals(990.0, DoughScale.next(1000.0, -1), 1e-9)
        assertEquals(610.0, DoughScale.next(607.0, +1), 1e-9)
        assertEquals(600.0, DoughScale.next(607.0, -1), 1e-9)
        assertEquals(18.0, DoughScale.next(17.0, +1), 1e-9)
        assertEquals(13.0, DoughScale.next(12.4, +1), 1e-9)
        assertEquals(12.0, DoughScale.next(12.4, -1), 1e-9)
        assertEquals(9.9, DoughScale.next(10.0, -1), 1e-9)
        assertEquals(99.0, DoughScale.next(100.0, -1), 1e-9)
        assertEquals(0.0, DoughScale.next(0.1, -1), 1e-9)
    }
}
