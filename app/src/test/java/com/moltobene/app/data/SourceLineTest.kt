package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Die Zeile „Quelle: …“, mit der Moltobene geteilte Rezepttexte beendet (#54). */
class SourceLineTest {

    @Test
    fun linkAlsQuelle() {
        val split = SourceLine.split("Linsensuppe\n\nZutaten\n• 250 g Linsen\n\nQuelle: https://example.org/linsensuppe\n")
        assertEquals("Linsensuppe\n\nZutaten\n• 250 g Linsen", split.text)
        assertEquals("https://example.org/linsensuppe", split.source)
        assertNull(split.page)
    }

    @Test
    fun buchMitSeiteAufDeutschUndEnglisch() {
        val de = SourceLine.split("Gulasch\n\nQuelle: Omas Kochbuch, S. 47")
        assertEquals("Omas Kochbuch", de.source)
        assertEquals("47", de.page)
        val en = SourceLine.split("Goulash\n\nSource: Grandma's cookbook, p. 47")
        assertEquals("Grandma's cookbook", en.source)
        assertEquals("47", en.page)
    }

    @Test
    fun ohneQuelleBleibtAlles() {
        val text = "Pfannkuchen\n\nZubereitung\n1. Alles verrühren."
        assertEquals(SourceLine.Split(text, null), SourceLine.split(text))
        // Nur die letzte Zeile zählt.
        assertNull(SourceLine.split("Quelle: Omas Kochbuch\n\nZubereitung\n1. Backen.").source)
    }

    @Test
    fun seiteAbtrennen() {
        assertEquals("Familienkochbuch" to "12", SourceLine.withPage("Familienkochbuch, S. 12"))
        assertEquals("Familienkochbuch" to null, SourceLine.withPage("Familienkochbuch"))
        assertEquals("https://example.org/a, S. 3" to null, SourceLine.withPage("https://example.org/a, S. 3"))
    }
}
