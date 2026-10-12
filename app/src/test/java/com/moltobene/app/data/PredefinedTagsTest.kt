package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Schlagwörter (#57): mitgelieferte mit Kennung, eigene mit Namen, Filter mit Anzahl. */
class PredefinedTagsTest {

    private val labels = mapOf("sauce" to "Saucen", "main_course" to "Hauptgerichte")

    @Test
    fun mitgelieferteHabenKennungAlsNamen() {
        val tag = PredefinedTags.tag("sauce")
        assertEquals("sauce", tag.name)
        assertEquals("sauce", tag.predefinedKey)
        assertEquals(13, PredefinedTags.KEYS.size)
        assertEquals(PredefinedTags.KEYS.size, PredefinedTags.KEYS.toSet().size)
    }

    @Test
    fun eigenesSchlagwortWirdZumMitgelieferten() {
        assertEquals(PredefinedTags.tag("sauce"), PredefinedTags.resolve("sauce"))
        assertEquals(PredefinedTags.tag("main_course"), PredefinedTags.resolve("Main  Course"))
        assertEquals(PredefinedTags.tag("sauce"), PredefinedTags.resolve("  saucen ", labels))
        assertNull(PredefinedTags.resolve("Omas Klassiker", labels).predefinedKey)
        assertEquals("Omas Klassiker", PredefinedTags.resolve("  Omas   Klassiker ", labels).name)
    }

    @Test
    fun speichernUndLesen() {
        val tags = listOf(PredefinedTags.tag("dessert"), Tag("Weihnachten"), Tag("Weihnachten"), Tag("weihnachten"))
        val decoded = TagCodec.decode(TagCodec.encode(tags))
        assertEquals(listOf(PredefinedTags.tag("dessert"), Tag("Weihnachten")), decoded)
        // Unbekannte Kennungen und Zeilen ohne Art werden ignoriert.
        assertEquals(emptyList<Tag>(), TagCodec.decode("p:gibt_es_nicht\nirgendwas\nc: \n"))
    }

    @Test
    fun hoechstensZwanzigSchlagwoerter() {
        val many = (1..30).map { Tag("Eigenes $it") }
        assertEquals(TagCodec.MAX_TAGS, TagCodec.decode(TagCodec.encode(many)).size)
    }

    @Test
    fun anzahlJeSchlagwortMitgelieferteZuerst() {
        val byRecipe = mapOf(
            "a" to listOf(PredefinedTags.tag("dessert"), Tag("Weihnachten")),
            "b" to listOf(PredefinedTags.tag("sauce"), PredefinedTags.tag("dessert")),
            "c" to listOf(Tag("Ärmel hochkrempeln"), Tag("Weihnachten")),
        )
        val counts = TagFilter.counts(byRecipe)
        assertEquals(listOf("sauce", "dessert", "Ärmel hochkrempeln", "Weihnachten"), counts.map { it.tag.name })
        assertEquals(listOf(1, 2, 1, 2), counts.map { it.count })
    }

    @Test
    fun filterBrauchtAlleGewaehlten() {
        val recipe = listOf(PredefinedTags.tag("sauce"), Tag("Weihnachten"))
        assertTrue(TagFilter.matches(recipe, emptySet()))
        assertTrue(TagFilter.matches(recipe, setOf("sauce")))
        assertTrue(TagFilter.matches(recipe, setOf("sauce", "Weihnachten")))
        assertFalse(TagFilter.matches(recipe, setOf("sauce", "dessert")))
        assertFalse(TagFilter.matches(emptyList(), setOf("sauce")))
    }
}
