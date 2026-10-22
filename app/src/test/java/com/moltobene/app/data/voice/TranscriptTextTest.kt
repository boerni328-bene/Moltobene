package com.moltobene.app.data.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptTextTest {

    @Test
    fun `liest den Text aus der Antwort`() {
        assertEquals("zwei eier", TranscriptText.fromJson("""{"text" : "zwei eier"}"""))
        assertEquals("zwei eier", TranscriptText.fromJson("""{"text":"  zwei eier "}"""))
    }

    @Test
    fun `nichts erkannt oder kaputte Antwort ergibt leeren Text`() {
        assertEquals("", TranscriptText.fromJson("""{"text" : ""}"""))
        assertEquals("", TranscriptText.fromJson("""{"partial": "zwei"}"""))
        assertEquals("", TranscriptText.fromJson("kein json"))
        assertEquals("", TranscriptText.fromJson(null))
    }
}
