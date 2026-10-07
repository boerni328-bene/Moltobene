package com.moltobene.app.data.translate

import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Ein Rezept übersetzen und speichern (#60), mit einem nachgestellten Übersetzer statt der Modelle. */
class RecipeTranslatorTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val recipe = Recipe(
        id = "0b0f6c0a-0000-4000-8000-000000000060",
        title = "Linsensuppe",
        language = "de",
        servings = 4,
        servingsUnit = "Teller",
        notes = "Eigene Notiz",
        ingredients = listOf(
            Ingredient("Für die Suppe:", isHeading = true),
            Ingredient("250 g Tellerlinsen"),
            Ingredient("1,5 l Gemüsebrühe"),
            Ingredient("1–2"),
        ),
        steps = listOf("Gemüse würfeln. Mit Brühe ca. 30 Minuten köcheln lassen."),
        createdAt = 0,
        updatedAt = 0,
    )

    private val seen = mutableListOf<String>()

    /** Kennt nur ein paar Sätze; alles andere kommt als Unsinn zurück, den die Schutzregeln verwerfen. */
    private val fake = LineTranslator { text ->
        seen += text
        when (text) {
            "Linsensuppe" -> "Lentil soup"
            "Teller" -> "plates"
            "Für die Suppe:" -> "For the soup:"
            "250 g brown lentils" -> "250 g brown lentils"
            "1,5 l Gemüsebrühe" -> "1.5 l vegetable broth"
            "Gemüse würfeln." -> "Dice the vegetables."
            "Mit Brühe ca. 30 Minuten köcheln lassen." -> "Simmer with broth for about thirty minutes."
            else -> "der der der der"
        }
    }

    @Test
    fun rezeptZeileFuerZeile() {
        val progress = mutableListOf<Pair<Int, Int>>()
        val result = RecipeTranslator.translate(recipe, "de", "en", fake, onProgress = { done, total -> progress += done to total })
        assertEquals("en", result.language)
        assertEquals("Lentil soup", result.title)
        assertEquals("plates", result.servingsUnit)
        assertEquals(listOf("For the soup:", "250 g brown lentils", "1.5 l vegetable broth", "1–2"), result.ingredients)
        // Im zweiten Satz fehlt die Zahl 30: Er bleibt im Original.
        assertEquals(listOf("Dice the vegetables. Mit Brühe ca. 30 Minuten köcheln lassen."), result.steps)
        // Küchenwörterbuch vor dem Modell; Zeilen ohne Buchstaben gehen gar nicht ans Modell; Notizen nie.
        assertEquals(true, "250 g brown lentils" in seen)
        assertEquals(false, "1–2" in seen)
        assertEquals(false, seen.any { it.contains("Notiz") })
        assertEquals(RecipeTranslator.workCount(recipe), progress.last().first)
        assertEquals(progress.size, progress.last().second)
    }

    @Test
    fun saetzeTrennen() {
        assertEquals(listOf("Gemüse würfeln.", "Mit Brühe ca. 30 Minuten köcheln lassen."), RecipeTranslator.sentences("Gemüse würfeln. Mit Brühe ca. 30 Minuten köcheln lassen."))
        assertEquals(listOf("Mit Gewürzen, z. B. Kreuzkümmel, abschmecken."), RecipeTranslator.sentences("Mit Gewürzen, z. B. Kreuzkümmel, abschmecken."))
        assertEquals(listOf("Add 2 tbsp. Sugar and stir.", "Done!"), RecipeTranslator.sentences("Add 2 tbsp. Sugar and stir. Done!"))
        assertEquals(listOf("Bake at 180 °C."), RecipeTranslator.sentences("  Bake at 180 °C.  "))
    }

    @Test
    fun speichernUndNachAenderungNeu() {
        val store = TranslationStore(folder.root)
        val translation = RecipeTranslator.translate(recipe, "de", "en", fake)
        store.save(recipe.id, translation)
        assertEquals(translation, store.load(recipe.id, "en", RecipeTranslator.sourceHash(recipe, "de", "en")))
        // Geändertes Original: Die alte Übersetzung passt nicht mehr.
        val changed = recipe.copy(steps = listOf("Gemüse fein würfeln."))
        assertNotEquals(RecipeTranslator.sourceHash(recipe, "de", "en"), RecipeTranslator.sourceHash(changed, "de", "en"))
        assertNull(store.load(recipe.id, "en", RecipeTranslator.sourceHash(changed, "de", "en")))
        // Notizen und Favorit gehören nicht dazu.
        assertEquals(
            RecipeTranslator.sourceHash(recipe, "de", "en"),
            RecipeTranslator.sourceHash(recipe.copy(notes = "Anders", favorite = true), "de", "en"),
        )
    }

    @Test
    fun gezeigteSpracheUndLoeschen() {
        val store = TranslationStore(folder.root)
        assertNull(store.shownLanguage(recipe.id))
        store.setShownLanguage(recipe.id, "en")
        assertEquals("en", store.shownLanguage(recipe.id))
        store.save(recipe.id, RecipeTranslator.translate(recipe, "de", "en", fake))
        store.save("anderes-rezept", RecipeTranslator.translate(recipe, "de", "en", fake))
        store.deleteUnused(setOf(recipe.id))
        assertNull(store.load("anderes-rezept", "en", RecipeTranslator.sourceHash(recipe, "de", "en")))
        assertEquals("en", store.shownLanguage(recipe.id))
        store.delete(recipe.id)
        assertNull(store.shownLanguage(recipe.id))
        assertEquals(0, folder.root.listFiles()?.size ?: 0)
    }
}
