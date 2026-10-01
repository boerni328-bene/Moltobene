package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ergebnis einer Texterkennung, das fertig wurde, während die App im Hintergrund war (Issue #37). */
class SavedResultTest {

    @Test
    fun ergebnisBleibtVollstaendigErhalten() {
        val parts = listOf("Crème brûlée", "Zutaten:\n500 ml Sahne\n\n4 Eigelb", "")
        val saved = SavedResult.decode(SavedResult.encode(TextRecognizer.Result(parts, "fr")))
        assertNotNull(saved)
        assertEquals("fr", saved!!.language)
        assertEquals(parts, saved.parts)
        assertTrue(saved.detected)
    }

    @Test
    fun vermuteteSpracheBleibtVermutet() {
        val saved = SavedResult.decode(SavedResult.encode(TextRecognizer.Result(listOf("Salz"), "de", detected = false)))
        assertEquals("de", saved!!.language)
        assertFalse(saved.detected)
    }

    @Test
    fun dateienAelterVersionenBleibenLesbar() {
        val old = SavedResult.decode("it?\nTorta di mele\n\n200 g di farina")
        assertEquals("it", old!!.language)
        assertFalse(old.detected)
        assertEquals("Torta di mele\n\n200 g di farina", old.text)
        assertTrue(SavedResult.decode("de\nApfelkuchen")!!.detected)
    }

    @Test
    fun unvollstaendigeDateiGiltAlsKeinErgebnis() {
        assertNull(SavedResult.decode(""))
        assertNull(SavedResult.decode("de"))
        assertNull(SavedResult.decode("de\n"))
        assertNull(SavedResult.decode("de\n  \n"))
        assertNull(SavedResult.decode("\nNur Text ohne Sprache"))
        assertNull(SavedResult.decode("{kaputt"))
        assertNull(SavedResult.decode("""{"language":"de","parts":["", " "]}"""))
    }
}
