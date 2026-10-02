package com.moltobene.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextLinksTest {

    @Test
    fun geteilterLinkAusDemBrowser() {
        assertEquals(
            TextLinks.SharedLink("https://www.example.org/rezepte/apfelkuchen", null),
            TextLinks.sharedLink("https://www.example.org/rezepte/apfelkuchen"),
        )
        // Viele Browser setzen den Titel der Seite davor.
        assertEquals(
            TextLinks.SharedLink("https://www.example.org/rezepte/apfelkuchen", "Apfelkuchen vom Blech"),
            TextLinks.sharedLink("Apfelkuchen vom Blech\nhttps://www.example.org/rezepte/apfelkuchen"),
        )
        assertEquals("Apfelkuchen", TextLinks.sharedLink("Apfelkuchen – https://example.org/a")?.title)
    }

    @Test
    fun rezepttextMitLinkIstKeinGeteilterLink() {
        val text = """
            Omas Apfelkuchen
            Zutaten
            200 g Mehl
            100 g Zucker
            3 Äpfel
            Zubereitung
            Alles verrühren und 40 Minuten backen.
            Gefunden auf https://example.org/apfelkuchen
        """.trimIndent()
        assertNull(TextLinks.sharedLink(text))
        assertEquals("https://example.org/apfelkuchen", TextLinks.first(text))
        assertNull(TextLinks.sharedLink("https://example.org/a und https://example.org/b"))
    }

    @Test
    fun satzzeichenAmEndeGehoerenNichtZumLink() {
        assertEquals(listOf("https://example.org/rezept"), TextLinks.all("Siehe https://example.org/rezept."))
        assertEquals(listOf("https://example.org/rezept"), TextLinks.all("(https://example.org/rezept)"))
    }

    @Test
    fun nurHttpUndHttps() {
        assertEquals(emptyList<String>(), TextLinks.all("ftp://example.org/datei javascript:alert(1) www.example.org"))
        assertNull(TextLinks.first("Kein Link hier"))
    }
}
