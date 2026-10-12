package com.moltobene.app.data.dough

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plus und Minus im Teigrechner (#83): genaue Schritte, Grenzen, Standardwert. */
class DoughStepperTest {

    private val water = DoughLimits.Range(45.0, 100.0)
    private val yeast = DoughLimits.Range(0.01, 5.0)

    @Test
    fun einSchrittNachObenUndUnten() {
        assertEquals(66.0, DoughStepper.next(65.0, 1.0, +1, water), 0.0)
        assertEquals(64.0, DoughStepper.next(65.0, 1.0, -1, water), 0.0)
    }

    @Test
    fun kommazahlenDriftenNicht() {
        var value = 0.0
        repeat(3) { value = DoughStepper.next(value, 0.1, +1, null) }
        assertEquals(0.3, value, 0.0)
        var salt = 2.8
        repeat(5) { salt = DoughStepper.next(salt, 0.1, -1, DoughLimits.Range(0.0, 5.0)) }
        assertEquals(2.3, salt, 0.0)
        assertEquals(0.25, DoughStepper.next(0.2, 0.05, +1, yeast), 0.0)
    }

    @Test
    fun anDerGrenzeBleibtEsStehen() {
        assertEquals(100.0, DoughStepper.next(100.0, 1.0, +1, water), 0.0)
        assertEquals(45.0, DoughStepper.next(45.5, 1.0, -1, water), 0.0)
        assertFalse(DoughStepper.canStep(100.0, +1, water))
        assertTrue(DoughStepper.canStep(100.0, -1, water))
        assertFalse(DoughStepper.canStep(45.0, -1, water))
    }

    @Test
    fun ohneGrenzenNieUnterNull() {
        assertEquals(0.0, DoughStepper.next(0.0, 1.0, -1, null), 0.0)
        assertFalse(DoughStepper.canStep(0.0, -1, null))
        assertTrue(DoughStepper.canStep(0.0, +1, null))
    }

    @Test
    fun keineZahlBeginntBeimStandardOderDerUnterenGrenze() {
        assertEquals(66.0, DoughStepper.next(Double.NaN, 1.0, +1, water, default = 65.0), 0.0)
        assertEquals(46.0, DoughStepper.next(Double.NaN, 1.0, +1, water), 0.0)
        assertTrue(DoughStepper.canStep(Double.NaN, +1, water))
    }

    @Test
    fun standardwertErkennen() {
        assertTrue(DoughStepper.isDefault(65.0, 65.0))
        assertTrue(DoughStepper.isDefault(2.8000000001, 2.8))
        assertFalse(DoughStepper.isDefault(65.5, 65.0))
        assertFalse(DoughStepper.isDefault(Double.NaN, 65.0))
    }

    @Test
    fun schrittweitenSindEndlichUndPositiv() {
        for (field in DoughField.entries) {
            val step = DoughStepper.stepFor(field)
            assertTrue("$field", step > 0 && step.isFinite())
        }
    }
}
