package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** „Umrechnen …“ nach Portionen und nach Zutat (#66): Faktor, Grenzen und Merken im Zwischenspeicher. */
class AdjustmentTest {

    private val cake = listOf("200 g Mehl", "100 g Zucker", "3 Eier", "1 Päckchen Backpulver", "Für den Guss:", "Puderzucker")

    @Test
    fun nachPortionen() {
        assertEquals(1.25, Adjustment.factor(Adjustment.Servings(5), 4, cake, "de"), 0.0)
        // Ohne Portionen im Rezept gibt es nichts umzurechnen.
        assertEquals(1.0, Adjustment.factor(Adjustment.Servings(5), null, cake, "de"), 0.0)
        assertEquals(1.0, Adjustment.factor(null, 4, cake, "de"), 0.0)
        // Ein Kuchen „für 1 Springform“ lässt sich bis 999 vervielfachen.
        assertEquals(999.0, Adjustment.factor(Adjustment.Servings(999), 1, cake, "de"), 0.0)
    }

    @Test
    fun nachZutat() {
        // 300 statt 200 g Mehl: alles anderthalbfach – auch bei einem Kuchen ohne Portionsangabe.
        val more = Adjustment.byIngredient(0, "200 g Mehl", 300.0)
        assertEquals(1.5, Adjustment.factor(more, null, cake, "de"), 0.0)
        // Halbieren geht, auch bei „1 Springform“.
        assertEquals(0.5, Adjustment.factor(Adjustment.byIngredient(2, "3 Eier", 1.5), 1, cake, "de"), 0.0)
        // „1.000 g“ ist im Deutschen tausend Gramm.
        val flour = listOf("1.000 g Mehl")
        assertEquals(0.5, Adjustment.factor(Adjustment.byIngredient(0, "1.000 g Mehl", 500.0), 4, flour, "de"), 0.0)
    }

    @Test
    fun geaenderteZeileZaehltNichtMehr() {
        val adjustment = Adjustment.byIngredient(0, "200 g Mehl", 300.0)
        // Inzwischen steht dort eine andere Zutat: nichts umrechnen statt still nach der falschen Zeile.
        assertEquals(1.0, Adjustment.factor(adjustment, null, listOf("250 g Dinkelmehl"), "de"), 0.0)
        assertEquals(1.0, Adjustment.factor(adjustment, null, emptyList(), "de"), 0.0)
    }

    @Test
    fun grenzen() {
        assertTrue(Adjustment.isAllowed(300.0, 200.0))
        assertTrue(Adjustment.isAllowed(20_000.0, 200.0))
        assertFalse(Adjustment.isAllowed(20_001.0, 200.0))
        assertTrue(Adjustment.isAllowed(2.0, 200.0))
        assertFalse(Adjustment.isAllowed(1.0, 200.0))
        assertFalse(Adjustment.isAllowed(0.0, 200.0))
        assertFalse(Adjustment.isAllowed(-5.0, 200.0))
        assertFalse(Adjustment.isAllowed(Double.POSITIVE_INFINITY, 200.0))
        assertFalse(Adjustment.isAllowed(Double.NaN, 200.0))
        // Ein unsinniger gemerkter Wert wird nicht angewendet.
        assertEquals(1.0, Adjustment.factor(Adjustment.ByIngredient(0, "200 g Mehl".hashCode(), 1e9), null, cake, "de"), 0.0)
    }

    @Test
    fun nachBackform() {
        val pan = Adjustment.ByPan(Pan.round(26.0), Pan.round(28.0), inch = false)
        assertEquals(1.1598, Adjustment.factor(pan, 1, cake, "de"), 0.0001)
        // Auch ohne Portionsangabe.
        assertEquals(1.1598, Adjustment.factor(pan, null, cake, "de"), 0.0001)
        assertEquals(pan, Adjustment.decode(Adjustment.encode(pan)))
        val rect = Adjustment.ByPan(Pan.round(22.5), Pan.rectangle(20.0, 30.5), inch = true)
        assertEquals(rect, Adjustment.decode(Adjustment.encode(rect)))
        // Gemerkte Werte außerhalb der Grenzen oder unlesbar werden nicht angewendet.
        assertNull(Adjustment.decode("pan:R:1.0:1.0:R:28.0:28.0:0"))
        assertNull(Adjustment.decode("pan:R:26.0:26.0:R:NaN:NaN:0"))
        assertNull(Adjustment.decode("pan:X:26.0:26.0:R:28.0:28.0:0"))
        assertNull(Adjustment.decode("pan:R:26.0"))
    }

    @Test
    fun merkenUndWiederherstellen() {
        val servings = Adjustment.Servings(6)
        assertEquals(servings, Adjustment.decode(Adjustment.encode(servings)))
        val ingredient = Adjustment.byIngredient(3, "1 Päckchen Backpulver", 1.5)
        assertEquals(ingredient, Adjustment.decode(Adjustment.encode(ingredient)))
        assertNull(Adjustment.decode(null))
        assertNull(Adjustment.decode(""))
        assertNull(Adjustment.decode("servings:0"))
        assertNull(Adjustment.decode("servings:1000"))
        assertNull(Adjustment.decode("ingredient:0:12:-1"))
        assertNull(Adjustment.decode("ingredient:x:12:3"))
        assertNull(Adjustment.decode("etwas anderes"))
    }
}
