package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTextTest {

    @Test
    fun zutatenWerdenZeilenweiseGelesenLeerzeilenIgnoriert() {
        val result = RecipeText.parseIngredients("200 g Mehl\n\n  2 Eier  \n1 Prise Salz\n")
        assertEquals(listOf("200 g Mehl", "2 Eier", "1 Prise Salz"), result.map { it.text })
        assertTrue(result.none { it.isHeading })
    }

    @Test
    fun zeileMitDoppelpunktIstZwischenueberschrift() {
        val result = RecipeText.parseIngredients("Für den Teig:\n200 g Mehl\nFür die Soße:\n1 Becher Sahne")
        assertEquals(
            listOf(
                Ingredient("Für den Teig", isHeading = true),
                Ingredient("200 g Mehl"),
                Ingredient("Für die Soße", isHeading = true),
                Ingredient("1 Becher Sahne"),
            ),
            result,
        )
    }

    @Test
    fun zutatenBleibenBeimHinUndHerUnveraendert() {
        val text = "Für den Teig:\n200 g Mehl\n2 Eier"
        assertEquals(text, RecipeText.formatIngredients(RecipeText.parseIngredients(text)))
    }

    @Test
    fun schritteWerdenZeilenweiseGelesen() {
        assertEquals(listOf("Ofen vorheizen.", "Teig kneten."), RecipeText.parseSteps("Ofen vorheizen.\n\nTeig kneten."))
    }

    @Test
    fun quelleMitLinkWirdAlsLinkErkannt() {
        val source = RecipeText.parseSource(" https://example.org/rezept ")
        assertEquals(SourceType.WEB, source?.type)
        assertEquals("https://example.org/rezept", source?.url)
    }

    @Test
    fun quelleOhneLinkIstName() {
        val source = RecipeText.parseSource("Omas Kochbuch, S. 42")
        assertEquals("Omas Kochbuch, S. 42", source?.name)
        assertNull(source?.url)
        assertNull(RecipeText.parseSource("   "))
    }

    @Test
    fun nurHttpUndHttpsSindLinks() {
        assertTrue(RecipeText.isWebLink("http://example.org"))
        assertFalse(RecipeText.isWebLink("javascript:alert(1)"))
        assertFalse(RecipeText.isWebLink("intent://example"))
        assertFalse(RecipeText.isWebLink("https://example.org/mit leerzeichen"))
    }
}
