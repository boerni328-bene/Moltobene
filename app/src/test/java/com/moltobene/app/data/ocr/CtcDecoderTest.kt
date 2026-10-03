package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.FloatBuffer

class CtcDecoderTest {

    private val keys = CtcDecoder.keys(listOf("M", "e", "h", "l"))

    /** Je Schritt die Wahrscheinlichkeiten aller Zeichen; [best] ist das wahrscheinlichste. */
    private fun steps(vararg best: Int): FloatBuffer {
        val values = FloatArray(best.size * keys.size) { 0.01f }
        best.forEachIndexed { step, index -> values[step * keys.size + index] = 0.9f }
        return FloatBuffer.wrap(values)
    }

    @Test
    fun wiederholungenUndLueckenFallenWeg() {
        // M M _ e h h _ l → „Mehl“
        val decoded = CtcDecoder.decode(steps(1, 1, 0, 2, 3, 3, 0, 4), 8, keys.size, keys)
        assertEquals("Mehl", decoded.text)
        assertEquals(0.9f, decoded.confidence, 0.0001f)
    }

    @Test
    fun gleichesZeichenZweimalBrauchtEineLueckeDazwischen() {
        // l _ l → „ll“, aber l l → „l“
        assertEquals("ll", CtcDecoder.decode(steps(4, 0, 4), 3, keys.size, keys).text)
        assertEquals("l", CtcDecoder.decode(steps(4, 4), 2, keys.size, keys).text)
    }

    @Test
    fun leerzeichenStehtAmEndeDerZeichenliste() {
        val space = keys.size - 1
        assertEquals("M e", CtcDecoder.decode(steps(1, space, 2), 3, keys.size, keys).text)
        assertEquals(0f, CtcDecoder.decode(steps(0, 0), 2, keys.size, keys).confidence, 0f)
    }
}
