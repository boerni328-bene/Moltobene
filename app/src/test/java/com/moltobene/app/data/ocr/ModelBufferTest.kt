package com.moltobene.app.data.ocr

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.IOException

/** Modelle werden außerhalb des Java-Speichers geladen (Issue #50). */
class ModelBufferTest {

    @Test
    fun modellLiegtVollstaendigImDirektenPuffer() {
        val bytes = ByteArray(20_000) { (it % 251).toByte() }
        val buffer = PaddleOcr.modelBuffer(ByteArrayInputStream(bytes), bytes.size)
        assertTrue(buffer.isDirect)
        assertArrayEquals(bytes, ByteArray(buffer.remaining()).also { buffer.get(it) })
    }

    @Test(expected = EOFException::class)
    fun zuKurzesModellWirdErkannt() {
        PaddleOcr.modelBuffer(ByteArrayInputStream(ByteArray(10)), 20)
    }

    @Test(expected = IOException::class)
    fun zuLangesModellWirdErkannt() {
        PaddleOcr.modelBuffer(ByteArrayInputStream(ByteArray(30)), 20)
    }
}
