package com.moltobene.app.data.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        for (text in listOf("stopp", "stop", "halt")) assertEquals(text, VoiceCommand.STOP, VoiceCommands.parse(text))
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
    fun nurDerGanzeSatzAlsBefehlZaehlt() {
        assertNull(VoiceCommands.parse(null))
        assertNull(VoiceCommands.parse(""))
        assertNull(VoiceCommands.parse("[unk]"))
        // „aus“ allein ist kein Befehl, nur zusammen mit „mikro“ oder „mikrofon“.
        assertNull(VoiceCommands.parse("aus"))
        assertNull(VoiceCommands.parse("mikro"))
        // Ein Befehl mitten im Gerede löst nichts aus, auch nicht mit einem unbekannten Wort davor.
        assertNull(VoiceCommands.parse("wir gehen weiter einkaufen"))
        assertNull(VoiceCommands.parse("[unk] weiter"))
        assertNull(VoiceCommands.parse("weiter weiter"))
        assertNull(VoiceCommands.parse("mikrofon weiter aus ende"))
        // Ein Wort, das nur Teil eines anderen ist, zählt nicht.
        assertNull(VoiceCommands.parse("weitere"))
        assertNull(VoiceCommands.parse("endlich"))
    }

    @Test
    fun grammatikEnthaeltBefehleKoederUndPlatzhalter() {
        val grammar = VoiceCommands.GERMAN_GRAMMAR
        assertTrue(grammar.startsWith("[") && grammar.endsWith("]"))
        for (word in listOf("weiter", "zurück", "wiederholen", "vorlesen", "stopp", "halt", "mikro aus", "mikrofon aus", "ausschalten", "ende", "beenden", "fertig", "[unk]", "ich", "salz")) {
            assertTrue(word, "\"$word\"" in grammar)
        }
    }

    @Test
    fun keinKoederIstEinBefehlswort() {
        val decoys = VoiceCommands.DECOY_WORDS.trim().split(Regex("\\s+"))
        assertTrue(decoys.size > 100)
        for (word in decoys) assertNull("Köder $word ist ein Befehl", VoiceCommands.parse(word))
        // Auch kein Köder ist ein Teil eines Befehlsworts, das so mehrdeutig würde.
        for (command in listOf("weiter", "zurück", "wiederholen", "vorlesen", "stopp", "stop", "halt", "mikro", "mikrofon", "ausschalten", "ende", "beenden", "fertig")) {
            assertFalse(command, command in decoys)
        }
    }
}
