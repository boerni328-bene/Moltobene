package com.moltobene.app.data.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Befehle der Spracheingabe (#80): Zuordnung der erkannten Wörter und Grammatik der Erkennung. */
class VoiceCommandsTest {

    @Test
    fun jederBefehlWirdErkannt() {
        assertEquals(VoiceCommand.NEXT, VoiceCommands.parse("weiter"))
        assertEquals(VoiceCommand.PREVIOUS, VoiceCommands.parse("zurück"))
        assertEquals(VoiceCommand.REPEAT, VoiceCommands.parse("wiederholen"))
        assertEquals(VoiceCommand.READ, VoiceCommands.parse("vorlesen"))
        assertEquals(VoiceCommand.STOP, VoiceCommands.parse("stopp"))
    }

    @Test
    fun mikrofonAusMitAllenWendungen() {
        for (text in listOf("mikro aus", "mikrofon aus", "ausschalten", "ende", "beenden", "fertig")) {
            assertEquals(text, VoiceCommand.MICROPHONE_OFF, VoiceCommands.parse(text))
        }
    }

    @Test
    fun grossschreibungUndLeerraumSindEgal() {
        assertEquals(VoiceCommand.NEXT, VoiceCommands.parse("  Weiter "))
        assertEquals(VoiceCommand.MICROPHONE_OFF, VoiceCommands.parse("Mikrofon   aus"))
    }

    @Test
    fun keinBefehlBleibtLeer() {
        assertNull(VoiceCommands.parse(null))
        assertNull(VoiceCommands.parse(""))
        assertNull(VoiceCommands.parse("[unk]"))
        // „aus“ allein ist kein Befehl, nur zusammen mit „mikro“ oder „mikrofon“.
        assertNull(VoiceCommands.parse("aus"))
        assertNull(VoiceCommands.parse("mikro"))
        // Ein Wort, das nur Teil eines anderen ist, zählt nicht.
        assertNull(VoiceCommands.parse("weitere"))
        assertNull(VoiceCommands.parse("endlich"))
    }

    @Test
    fun mehrereBefehleZaehlenAlsEinerDerErsteGewinnt() {
        assertEquals(VoiceCommand.NEXT, VoiceCommands.parse("weiter weiter"))
        assertEquals(VoiceCommand.PREVIOUS, VoiceCommands.parse("zurück weiter"))
        assertEquals(VoiceCommand.NEXT, VoiceCommands.parse("[unk] weiter"))
    }

    @Test
    fun grammatikEnthaeltAlleBefehleUndDenPlatzhalter() {
        val grammar = VoiceCommands.GERMAN_GRAMMAR
        assertTrue(grammar.startsWith("[") && grammar.endsWith("]"))
        for (word in listOf("weiter", "zurück", "wiederholen", "vorlesen", "stopp", "mikro aus", "mikrofon aus", "ausschalten", "ende", "beenden", "fertig", "[unk]")) {
            assertTrue(word, "\"$word\"" in grammar)
        }
    }
}
