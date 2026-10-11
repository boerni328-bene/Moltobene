package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** „Backform umrechnen“ (#67): Faktor über die Fläche und Backformen aus der Angabe der Rezeptmenge lesen. */
class PanScalingTest {

    @Test
    fun faktorUeberDieFlaeche() {
        // 26 → 28 cm sind etwa 16 % mehr, nicht 28/26 = 7,7 %.
        assertEquals(1.1598, PanScaling.factor(Pan.round(26.0), Pan.round(28.0))!!, 0.0001)
        assertEquals(1.0, PanScaling.factor(Pan.round(26.0), Pan.round(26.0))!!, 1e-12)
        // Rund auf eckig: 26 cm rund hat etwa 531 cm², ein Blech 30 × 40 cm 1.200 cm².
        assertEquals(1200.0 / (Math.PI * 169), PanScaling.factor(Pan.round(26.0), Pan.rectangle(30.0, 40.0))!!, 1e-9)
        assertEquals(2.0, PanScaling.factor(Pan.rectangle(20.0, 20.0), Pan.rectangle(20.0, 40.0))!!, 1e-12)
    }

    @Test
    fun grenzen() {
        assertNull(PanScaling.factor(Pan.round(0.0), Pan.round(26.0)))
        assertNull(PanScaling.factor(Pan.round(26.0), Pan.round(-5.0)))
        assertNull(PanScaling.factor(Pan.round(26.0), Pan.round(Double.NaN)))
        assertNull(PanScaling.factor(Pan.round(26.0), Pan.round(Double.POSITIVE_INFINITY)))
        assertNull(PanScaling.factor(Pan.round(4.0), Pan.round(26.0)))
        assertNull(PanScaling.factor(Pan.round(26.0), Pan.round(101.0)))
        // Faktor über 10: 10 cm auf 60 cm.
        assertNull(PanScaling.factor(Pan.round(10.0), Pan.round(60.0)))
        assertTrue(PanScaling.isValid(Pan.rectangle(30.0, 40.0)))
        assertFalse(PanScaling.isValid(Pan.rectangle(30.0, 0.0)))
    }

    @Test
    fun zollInCm() {
        assertEquals(22.86, PanScaling.toCm(9.0, inch = true), 1e-9)
        assertEquals(26.0, PanScaling.toCm(26.0, inch = false), 0.0)
    }

    @Test
    fun formAusDerAngabe() {
        fun parse(text: String) = PanScaling.parse(text)
        assertEquals(Pan.round(26.0) to false, parse("Springform (Ø 26 cm)"))
        assertEquals(Pan.round(26.0) to false, parse("Springform Ø 26 cm"))
        assertEquals(Pan.round(26.0) to false, parse("26er Springform"))
        assertEquals(Pan.round(24.0) to false, parse("Tarteform 24 cm"))
        assertEquals(Pan.rectangle(30.0, 40.0) to false, parse("Blech 30 x 40 cm"))
        assertEquals(Pan.rectangle(30.0, 40.0) to false, parse("Backblech 30×40"))
        val round = parse("9-inch round pan")!!
        assertTrue(round.second && round.first.round)
        assertEquals(22.86, round.first.a, 1e-9)
        val rect = parse("9x13-inch pan")!!
        assertTrue(rect.second)
        assertEquals(22.86, rect.first.a, 1e-9)
        assertEquals(33.02, rect.first.b, 1e-9)
        assertEquals(Pan.round(24.0) to false, parse("tortiera da 24 cm"))
        assertEquals(Pan.round(22.0) to false, parse("moule de 22 cm"))
        // Unklar: kein Wort für eine runde Form – die App rät nie.
        assertNull(parse("Kastenform 25 cm"))
        assertNull(parse("12 Stück"))
        assertNull(parse("Stück"))
        assertNull(parse(""))
        assertNull(PanScaling.parse(null))
        // Unsinnige Maße werden nicht gelesen.
        assertNull(parse("Springform Ø 2 cm"))
        assertNotNull(parse("Springform Ø 30 cm"))
    }
}
