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
        val text = "Crème brûlée\n\nZutaten:\n500 ml Sahne\n\n4 Eigelb\n"
        val saved = SavedResult.decode(SavedResult.encode(TextRecognizer.Result(text, "fr")))
        assertNotNull(saved)
        assertEquals("fr", saved!!.language)
        assertEquals(text, saved.text)
        assertTrue(saved.detected)
    }

    @Test
    fun vermuteteSpracheBleibtVermutet() {
        val saved = SavedResult.decode(SavedResult.encode(TextRecognizer.Result("Salz", "de", detected = false)))
        assertEquals("de", saved!!.language)
        assertFalse(saved.detected)
    }

    @Test
    fun unvollstaendigeDateiGiltAlsKeinErgebnis() {
        assertNull(SavedResult.decode(""))
        assertNull(SavedResult.decode("de"))
        assertNull(SavedResult.decode("de\n"))
        assertNull(SavedResult.decode("de\n  \n"))
        assertNull(SavedResult.decode("\nNur Text ohne Sprache"))
    }
}
