package com.moltobene.app.data.basics

import com.moltobene.app.data.PredefinedTags
import com.moltobene.app.data.RecipeIds
import com.moltobene.app.data.RecipeTitle
import com.moltobene.app.data.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Grundrezepte (#82): Aufbau der Datei und die Umwandlung in ein normales Rezept. */
class BasicRecipesTest {

    private val sample = """
        {"version": 1, "recipes": [{
          "id": "demo", "tags": ["basics", "sauce", "gibt_es_nicht"], "servings": 4, "totalMinutes": 15,
          "texts": {
            "de": {"title": "Beispielsoße", "summary": "Ein Satz.", "ingredients": ["Für die Soße:", "250 ml Milch", "1 Prise Salz"], "steps": ["Rühren.", " Kochen. "], "notes": "Notiz"},
            "en": {"title": "Sample sauce", "summary": "One sentence.", "ingredients": ["250 ml milk"], "steps": ["Stir."]}
          }}]}
    """.trimIndent()

    @Test
    fun wirdZumNormalenRezeptMitQuelleMoltobene() {
        val basic = BasicRecipes.parse(sample).single()
        val recipe = BasicRecipes.toRecipe(basic, "de", RecipeIds.newId(), now = 5)
        assertEquals("Beispielsoße", recipe.title)
        assertEquals("de", recipe.language)
        assertEquals(4, recipe.servings)
        assertEquals(15, recipe.totalMinutes)
        assertEquals(SourceType.OTHER, recipe.source?.type)
        assertEquals("Moltobene", recipe.source?.name)
        assertEquals(listOf("Für die Soße", "250 ml Milch", "1 Prise Salz"), recipe.ingredients.map { it.text })
        assertEquals(listOf(true, false, false), recipe.ingredients.map { it.isHeading })
        assertEquals(listOf("Rühren.", "Kochen."), recipe.steps)
        assertEquals("Notiz", recipe.notes)
        // Unbekannte Schlagwörter werden nicht übernommen.
        assertEquals(listOf("basics", "sauce"), recipe.tags.map { it.name })
        assertFalse(recipe.isDraft)
        assertEquals(5, recipe.createdAt)
    }

    @Test
    fun spracheDerOberflaecheBestimmtDenText() {
        val basic = BasicRecipes.parse(sample).single()
        assertEquals("Sample sauce", BasicRecipes.text(basic, "en").title)
        assertEquals("Sample sauce", BasicRecipes.text(basic, "fr").title)
        assertEquals("Beispielsoße", BasicRecipes.text(basic, "de-AT").title)
        assertEquals("en", BasicRecipes.toRecipe(basic, "fr", "x", 0).language)
    }

    @Test
    fun schonUebernommenErkenntTitelInBeidenSprachen() {
        val basic = BasicRecipes.parse(sample).single()
        fun found(vararg titles: String) = BasicRecipes.existingRecipe(basic, titles.mapIndexed { i, t -> RecipeTitle("id$i", t) })?.id
        assertEquals("id0", found("  beispielsoße "))
        assertEquals("id1", found("Andere Soße", "Sample Sauce"))
        assertNull(found("Andere Soße"))
        assertNull(found())
    }

    /** Die mitgelieferte Datei ist vollständig und stimmig – in beiden Sprachen, mit Mengen und bekannten Schlagwörtern. */
    @Test
    fun mitgelieferteDateiIstVollstaendig() {
        val file = File("src/main/assets/${BasicRecipes.ASSET}")
        if (!file.exists()) return
        val all = BasicRecipes.parse(file.readText())
        assertTrue(all.size in 8..12)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        val titlesByLanguage = mapOf("de" to HashSet<String>(), "en" to HashSet<String>())
        for (basic in all) {
            assertTrue("Portionen bei ${basic.id}", basic.servings in 1..24)
            assertTrue("Schlagwörter bei ${basic.id}", basic.tags.isNotEmpty() && basic.tags.all(PredefinedTags::isKnown))
            assertEquals(setOf("de", "en"), basic.texts.keys)
            for ((language, text) in basic.texts) {
                val where = "${basic.id}/$language"
                assertTrue("Titel $where", text.title.isNotBlank() && titlesByLanguage.getValue(language).add(text.title.lowercase()))
                assertTrue("Zusammenfassung $where", text.summary.length in 10..160)
                assertTrue("Zutaten $where", text.ingredients.count { !it.endsWith(":") } >= 2)
                assertTrue("Schritte $where", text.steps.size in 2..12)
                assertTrue("Schritt leer $where", text.steps.all { it.isNotBlank() })
                // Texte ohne direkte Anrede (Projektregel).
                val joined = (text.ingredients + text.steps + text.summary + text.notes).joinToString(" ")
                val address = if (language == "de") Regex("""\b(du|dich|dein\w*|ihr|euch|sie\s+(sollten|können|müssen))\b""", RegexOption.IGNORE_CASE) else Regex("""\b(you|your)\b""", RegexOption.IGNORE_CASE)
                assertFalse("Anrede $where: ${address.find(joined)?.value}", address.containsMatchIn(joined))
            }
            // Das Rezept lässt sich daraus bauen.
            assertNotNull(BasicRecipes.toRecipe(basic, "de", "id", 0))
        }
    }
}
