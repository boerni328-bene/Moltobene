package com.moltobene.app.data.web

import org.junit.Assert.assertEquals
import org.junit.Test

class TagWordsTest {

    private fun read(fields: String): WebRecipe =
        requireNotNull(
            RecipeJsonLdReader.read("""{"@context":"https://schema.org","@type":"Recipe","name":"Kuchen",$fields "recipeIngredient":["1 Ei"]}"""),
        )

    @Test
    fun keywordsAsCommaText() {
        val recipe = read(""""keywords":"Kuchen, Sommer ; Dessert,, kuchen",""")
        assertEquals(listOf("Kuchen", "Sommer", "Dessert"), recipe.keywords)
    }

    @Test
    fun categoriesAsListOrText() {
        assertEquals(listOf("Dessert", "Backen"), read(""""recipeCategory":["Dessert","Backen"],""").categories)
        assertEquals(listOf("Hauptgericht"), read(""""recipeCategory":"Hauptgericht",""").categories)
    }

    @Test
    fun limitsCountAndLength() {
        val many = (1..30).joinToString(",") { "wort$it" }
        assertEquals(20, read(""""keywords":"$many",""").keywords.size)
        val longWord = "x".repeat(31)
        assertEquals(listOf("kurz"), read(""""keywords":"$longWord, kurz",""").keywords)
    }

    @Test
    fun missingFieldsGiveEmptyLists() {
        val recipe = read("")
        assertEquals(emptyList<String>(), recipe.keywords)
        assertEquals(emptyList<String>(), recipe.categories)
    }
}
