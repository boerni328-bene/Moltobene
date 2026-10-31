package com.moltobene.app.data.dough

import com.moltobene.app.data.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gewichte in Gramm oder Unzen: Umrechnen, Anzeige und das gespeicherte Rezept. */
class DoughUnitsTest {

    private val pizza = DoughLimits.defaults(DoughType.PIZZA)

    @Test
    fun eineUnzeSindGut28Gramm() {
        assertEquals(28.349523125, WeightUnit.OUNCES.toGrams(1.0), 1e-9)
        assertEquals(1.0, WeightUnit.OUNCES.fromGrams(28.349523125), 1e-9)
        assertEquals(250.0, WeightUnit.GRAMS.fromGrams(250.0), 0.0)
    }

    @Test
    fun unbekannteKennungWirdGramm() {
        assertEquals(WeightUnit.GRAMS, WeightUnit.fromKey(null))
        assertEquals(WeightUnit.GRAMS, WeightUnit.fromKey("stones"))
        assertEquals(WeightUnit.OUNCES, WeightUnit.fromKey("ounces"))
    }

    @Test
    fun anzeigeMitEinheit() {
        assertEquals("1000 g", DoughNumbers.weight(1000.0, 0, "de", WeightUnit.GRAMS))
        assertEquals("35,3 oz", DoughNumbers.weight(1000.0, 0, "de", WeightUnit.OUNCES))
        assertEquals("35.3 oz", DoughNumbers.weight(1000.0, 0, "en", WeightUnit.OUNCES))
        assertEquals("0,04 oz", DoughNumbers.weight(1.2, LineRole.YEAST, "de", WeightUnit.OUNCES))
        assertEquals("1,2 g", DoughNumbers.weight(1.2, LineRole.YEAST, "de", WeightUnit.GRAMS))
    }

    @Test
    fun rezeptInUnzen() {
        val result = DoughCalculator.calculate(pizza) as DoughResult.Success
        val draft = DoughDraft.from(result, pizza, GermanTexts, "de", WeightUnit.OUNCES)
        assertTrue(draft.ingredients.all { " g " !in it.text })
        assertTrue(draft.ingredients.first().text.contains(" oz Mehl"))
        assertEquals("Teigkugeln (je 8,8 oz)", draft.servingsUnit)
        val inGrams = DoughDraft.from(result, pizza, GermanTexts, "de")
        assertFalse(inGrams.ingredients.any { "oz" in it.text })
        assertEquals("Teigkugeln (je 250 g)", inGrams.servingsUnit)
    }
}
