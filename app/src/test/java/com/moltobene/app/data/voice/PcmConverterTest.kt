package com.moltobene.app.data.voice

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmConverterTest {

    @Test
    fun `gleiche Rate und ein Kanal bleibt unveraendert`() {
        val input = shortArrayOf(1, -2, 3, 400)
        assertArrayEquals(input, PcmConverter(16_000, 1).convert(input))
    }

    @Test
    fun `zwei Kanaele werden zu einem gemittelt`() {
        val stereo = shortArrayOf(100, 300, -50, 50, 7, 9)
        assertArrayEquals(shortArrayOf(200, 0, 8), PcmConverter(16_000, 2).convert(stereo))
    }

    @Test
    fun `48 kHz wird auf ein Drittel der Werte gemittelt`() {
        val input = shortArrayOf(3, 6, 9, 30, 60, 90)
        assertArrayEquals(shortArrayOf(6, 60), PcmConverter(48_000, 1).convert(input))
    }

    @Test
    fun `44,1 kHz ergibt etwa 16 kHz und bleibt bei Gleichton gleich`() {
        val input = ShortArray(44_100) { 300 }
        val out = PcmConverter(44_100, 1).convert(input)
        assertEquals(16_000.0, out.size.toDouble(), 3.0)
        assertTrue(out.all { it.toInt() == 300 })
    }

    @Test
    fun `in Stuecken gewandelt ist das Ergebnis dasselbe wie am Stueck`() {
        val input = ShortArray(44_100) { (it % 200 - 100).toShort() }
        val whole = PcmConverter(44_100, 1).convert(input)

        val chunked = PcmConverter(44_100, 1)
        val parts = mutableListOf<Short>()
        var offset = 0
        for (size in listOf(1000, 4097, 333, 20_000, 18_670)) {
            val chunk = input.copyOfRange(offset, offset + size)
            parts += chunked.convert(chunk).toList()
            offset += size
        }
        assertEquals(input.size, offset)
        assertArrayEquals(whole, parts.toShortArray())
    }

    private fun assertTrue(condition: Boolean) = org.junit.Assert.assertTrue(condition)
}
