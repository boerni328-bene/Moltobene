package com.moltobene.app.data.translate

import org.junit.Assert.assertEquals
import org.junit.Test

/** Zerlegung in Wortteile (#60) mit einem kleinen, selbst geschriebenen Wortschatz. */
class SentencePieceTest {

    private val pieces = mapOf(
        "▁Hallo" to SentencePieceEncoder.Piece(-1f, 10),
        "▁Wel" to SentencePieceEncoder.Piece(-2f, 11),
        "t" to SentencePieceEncoder.Piece(-1f, 12),
        "▁Welt" to SentencePieceEncoder.Piece(-2.5f, 13),
        "▁" to SentencePieceEncoder.Piece(-3f, 14),
        "▁ab" to SentencePieceEncoder.Piece(-2f, 15),
        "▁a" to SentencePieceEncoder.Piece(-1f, 16),
        "b" to SentencePieceEncoder.Piece(-1f, 17),
        "fi" to SentencePieceEncoder.Piece(-1f, 18),
        "▁Ka" to SentencePieceEncoder.Piece(-1f, 19),
    )
    private val encoder = SentencePieceEncoder(pieces, unknownId = 1)

    @Test
    fun besteZerlegungGewinnt() {
        // „▁Welt“ (-2,5) ist besser als „▁Wel“ + „t“ (-3).
        assertEquals(listOf(10, 13), encoder.encode("Hallo  Welt"))
        assertEquals(listOf(10, 13), encoder.encode("  Hallo\tWelt \n"))
    }

    @Test
    fun beiGleichemWertGewinntDerLaengereWortteil() {
        // „▁ab“ (-2) und „▁a“ + „b“ (-2) sind gleich gut; wie im Original gewinnt der längere Wortteil.
        assertEquals(listOf(15), encoder.encode("ab"))
    }

    @Test
    fun unbekannteZeichen() {
        assertEquals(listOf(10, 14, 1), encoder.encode("Hallo ☃"))
        // Zeichen außerhalb der Grundebene zählen als ein Zeichen.
        assertEquals(listOf(10, 14, 1), encoder.encode("Hallo 🍅"))
        assertEquals(emptyList<Int>(), encoder.encode("   "))
    }

    @Test
    fun unicodeWirdVereinheitlicht() {
        assertEquals("▁Ka▁fi", SentencePieceEncoder.normalize("Ka ﬁ"))
        assertEquals(listOf(19, 14, 18), encoder.encode("Ka ﬁ"))
    }

    @Test
    fun wortschatzMachtWiederText() {
        val vocabulary = Vocabulary.read(sequenceOf("0\t</s>", "1\t<unk>", "2\t▁Guten", "3\t▁Appetit", "4\t!"), skip = setOf(0, 1))
        assertEquals("Guten Appetit!", vocabulary.decode(listOf(2, 3, 4, 0)))
        assertEquals("Guten", vocabulary.decode(listOf(2, 1, 99)))
    }

    @Test
    fun wortteileAusDemSprachpaketLesen() {
        val read = SentencePieceEncoder.read(sequenceOf("▁Hallo\t-1.0\t10", "▁Welt\t-2.5\t13", ""), unknownId = 1)
        assertEquals(listOf(10, 13), read.encode("Hallo Welt"))
    }
}
