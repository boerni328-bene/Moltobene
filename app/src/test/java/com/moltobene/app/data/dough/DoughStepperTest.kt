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
    fun reglerZeigtDenUeblichenBereichInnerhalbDerGrenzen() {
        val water = DoughStepper.sliderBounds(DoughField.WATER, DoughLimits.Range(45.0, 100.0))!!
        assertEquals(50.0, water.min, 0.0)
        assertEquals(90.0, water.max, 0.0)
        // Engere Grenzen gewinnen (Pasta-Wasser 35 bis 70).
        val pasta = DoughStepper.sliderBounds(DoughField.WATER, DoughLimits.Range(35.0, 70.0))!!
        assertEquals(50.0, pasta.min, 0.0)
        assertEquals(70.0, pasta.max, 0.0)
        // Ohne üblichen Bereich gelten die Grenzen; Backform und leere Grenzen bekommen keinen Regler.
        val fat = DoughStepper.sliderBounds(DoughField.FAT, DoughLimits.Range(20.0, 120.0))!!
        assertEquals(20.0, fat.min, 0.0)
        assertEquals(null, DoughStepper.sliderBounds(DoughField.PAN_SIZE, null))
        assertEquals(null, DoughStepper.sliderBounds(DoughField.WHOLE_EGG, DoughLimits.Range(0.0, 0.0)))
    }

    @Test
    fun reglerstellungUndWertGehenHinUndZurueck() {
        val bounds = DoughLimits.Range(50.0, 90.0)
        assertEquals(0f, DoughStepper.positionOf(50.0, bounds, false), 0f)
        assertEquals(1f, DoughStepper.positionOf(90.0, bounds, false), 0f)
        assertEquals(0.5f, DoughStepper.positionOf(70.0, bounds, false), 0.0001f)
        assertEquals(0f, DoughStepper.positionOf(30.0, bounds, false), 0f)
        assertEquals(1f, DoughStepper.positionOf(150.0, bounds, false), 0f)
        for (value in listOf(50.0, 62.0, 65.0, 78.0, 90.0)) {
            val position = DoughStepper.positionOf(value, bounds, false)
            assertEquals(value, DoughStepper.valueAt(position, bounds, false, 1.0, DoughLimits.Range(45.0, 100.0)), 0.0)
        }
    }

    @Test
    fun hefeHatEineGestauchteSkalaUndBleibtInDenGrenzen() {
        val bounds = DoughStepper.sliderBounds(DoughField.YEAST, DoughLimits.Range(0.01, 5.0))!!
        val step = DoughStepper.sliderStepFor(DoughField.YEAST)
        assertTrue(DoughStepper.isCurved(DoughField.YEAST))
        // Die Mitte des Reglers liegt weit unten, nicht bei der Hälfte des Bereichs.
        val middle = DoughStepper.valueAt(0.5f, bounds, true, step, DoughLimits.Range(0.01, 5.0))
        assertTrue("Mitte $middle", middle < 0.5)
        // Ganz links ist der Wert nie unter der unteren Grenze (0 wäre ungültig).
        assertEquals(0.01, DoughStepper.valueAt(0f, bounds, true, step, DoughLimits.Range(0.01, 5.0)), 0.0)
        assertEquals(2.0, DoughStepper.valueAt(1f, bounds, true, step, DoughLimits.Range(0.01, 5.0)), 0.0)
        // Hin und zurück ändert den Wert höchstens um einen Schritt.
        val position = DoughStepper.positionOf(0.2, bounds, true)
        assertEquals(0.2, DoughStepper.valueAt(position, bounds, true, step, null), 0.011)
    }

    @Test
    fun jedesFeldMitReglerHatSinnvolleSchritte() {
        for (field in DoughField.entries) {
            val bounds = DoughStepper.sliderBounds(field, DoughLimits.Range(0.0, 200.0)) ?: continue
            val step = DoughStepper.sliderStepFor(field)
            assertTrue("$field", bounds.max > bounds.min && step > 0 && (bounds.max - bounds.min) / step >= 2)
        }
    }

    @Test
    fun schrittweitenSindEndlichUndPositiv() {
        for (field in DoughField.entries) {
            val step = DoughStepper.stepFor(field)
            assertTrue("$field", step > 0 && step.isFinite())
        }
    }
}
