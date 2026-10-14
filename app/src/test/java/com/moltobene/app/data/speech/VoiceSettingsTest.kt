package com.moltobene.app.data.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stimmenwahl und Einstellungen des Vorlesens (#85): nur passende, installierte Offline-Stimmen; Rückfall mit Hinweis. */
class VoiceSettingsTest {

    private fun voice(
        name: String,
        language: String = "de",
        country: String = "DE",
        quality: Int = 300,
        network: Boolean = false,
        installed: Boolean = true,
    ) = VoiceInfo(name, language, country, quality, network, installed)

    private val voices = listOf(
        voice("de-de-x-a-local", quality = 300),
        voice("de-de-x-b-local", quality = 400),
        voice("de-at-x-c-local", country = "AT", quality = 500),
        voice("de-de-x-d-network", network = true, quality = 500),
        voice("de-ch-x-e-local", country = "CH", installed = false),
        voice("en-us-x-f-local", language = "en", country = "US"),
        voice("en-gb-x-g-local", language = "en", country = "GB", quality = 400),
    )

    @Test
    fun nurSprachePassendInstalliertUndOhneInternet() {
        val names = VoiceSelection.usable(voices, "de").map { it.name }
        assertEquals(setOf("de-de-x-a-local", "de-de-x-b-local", "de-at-x-c-local"), names.toSet())
        assertTrue(VoiceSelection.usable(voices, "fr").isEmpty())
        assertTrue(VoiceSelection.usable(emptyList(), "de").isEmpty())
    }

    @Test
    fun landDesHandysUndGueteBestimmenDieReihenfolge() {
        assertEquals("de-de-x-b-local", VoiceSelection.usable(voices, "de", "DE").first().name)
        assertEquals("de-at-x-c-local", VoiceSelection.usable(voices, "de", "AT").first().name)
        // Ohne Land des Handys zählt die Güte.
        assertEquals("de-at-x-c-local", VoiceSelection.usable(voices, "de").first().name)
        assertEquals("en-gb-x-g-local", VoiceSelection.usable(voices, "en").first().name)
    }

    @Test
    fun gewuenschteStimmeWirdGenommen() {
        val choice = VoiceSelection.choose(voices, "de", "de-de-x-a-local", "DE")!!
        assertEquals("de-de-x-a-local", choice.voice.name)
        assertFalse(choice.fellBack)
    }

    @Test
    fun fehlendeStimmeFuehrtZurBestenPassendenMitHinweis() {
        val choice = VoiceSelection.choose(voices, "de", "de-xx-gibt-es-nicht", "DE")!!
        assertEquals("de-de-x-b-local", choice.voice.name)
        assertTrue(choice.fellBack)
        // Eine Stimme einer anderen Sprache oder mit Internet wird nie genommen, auch wenn sie gewünscht war.
        assertTrue(VoiceSelection.choose(voices, "de", "en-us-x-f-local", "DE")!!.fellBack)
        assertTrue(VoiceSelection.choose(voices, "de", "de-de-x-d-network", "DE")!!.fellBack)
        assertTrue(VoiceSelection.choose(voices, "de", "de-ch-x-e-local", "DE")!!.fellBack)
    }

    @Test
    fun ohneWunschKeinHinweis() {
        assertFalse(VoiceSelection.choose(voices, "en", null)!!.fellBack)
        assertFalse(VoiceSelection.choose(voices, "en", "")!!.fellBack)
    }

    @Test
    fun keineStimmeGibtNull() {
        assertNull(VoiceSelection.choose(voices, "fr", null))
        assertNull(VoiceSelection.choose(voices.filter { it.networkRequired }, "de", null))
    }

    @Test
    fun tempoUndTonhoeheBleibenInDenGrenzen() {
        var s = SpeechSettings()
        repeat(50) { s = s.steppedRate(+1) }
        assertEquals(SpeechSettings.RATE_MAX, s.rate)
        repeat(50) { s = s.steppedRate(-1) }
        assertEquals(SpeechSettings.RATE_MIN, s.rate)
        repeat(50) { s = s.steppedPitch(+1) }
        assertEquals(SpeechSettings.PITCH_MAX, s.pitch)
        repeat(50) { s = s.steppedPitch(-1) }
        assertEquals(SpeechSettings.PITCH_MIN, s.pitch)
        assertEquals(105, SpeechSettings().steppedRate(+1).rate)
        assertEquals(100, SpeechSettings().steppedRate(+99).rate - 5 + 0) // Schrittrichtung wird auf ±1 begrenzt
    }

    @Test
    fun voreinstellungenWerdenErkannt() {
        assertEquals(SpeechPreset.NORMAL, SpeechSettings().preset())
        for (preset in SpeechPreset.entries) {
            val s = SpeechSettings().withPreset(preset)
            assertEquals(preset, s.preset())
            assertTrue(s.rate in SpeechSettings.RATE_MIN..SpeechSettings.RATE_MAX)
            assertTrue(s.pitch in SpeechSettings.PITCH_MIN..SpeechSettings.PITCH_MAX)
        }
        assertNull(SpeechSettings().withRate(95).preset())
        assertEquals(setOf("relaxed", "normal", "lively"), SpeechPreset.entries.map { it.key }.toSet())
    }

    @Test
    fun stimmeJeSpracheMerkenUndLoeschen() {
        val s = SpeechSettings().withVoice("de", "de-at-x-c-local").withVoice("EN", "en-gb-x-g-local")
        assertEquals("de-at-x-c-local", s.voiceFor("de"))
        assertEquals("en-gb-x-g-local", s.voiceFor("en"))
        assertNull(s.withVoice("de", null).voiceFor("de"))
        assertNull(s.withVoice("de", "").voiceFor("de"))
    }

    @Test
    fun hinUndZurueck() {
        val s = SpeechSettings().withVoice("de", "de-at-x-c-local").withVoice("en", "en-gb-x-g-local").withRate(85).withPitch(95)
        assertEquals(s, SpeechSettings.decode(SpeechSettings.encode(s)))
    }

    @Test
    fun kaputtesWirdNieZurAusnahme() {
        assertEquals(SpeechSettings(), SpeechSettings.decode(null))
        assertEquals(SpeechSettings(), SpeechSettings.decode(""))
        assertEquals(SpeechSettings(), SpeechSettings.decode("Unsinn"))
        assertEquals(SpeechSettings(), SpeechSettings.decode("[1,2]"))
        val wild = """{"voices":{"de":"","fr":"x","en":"${"n".repeat(500)}","xx":"y"},"rate":9999,"pitch":-5}"""
        val s = SpeechSettings.decode(wild)
        assertTrue(s.voices.isEmpty())
        assertEquals(SpeechSettings.RATE_MAX, s.rate)
        assertEquals(SpeechSettings.PITCH_MIN, s.pitch)
        // Steuerzeichen im Namen werden verworfen.
        assertTrue(SpeechSettings.decode("""{"voices":{"de":"a\nb"}}""").voices.isEmpty())
    }

    @Test
    fun kopfhoerertastenSindStandardmaessigAnUndBleibenAus() {
        assertTrue(SpeechSettings().headsetButtons)
        // Ältere gemerkte Einstellungen ohne das Feld haben die Tasten an.
        assertTrue(SpeechSettings.decode("""{"rate":90}""").headsetButtons)
        val off = SpeechSettings().withHeadsetButtons(false)
        assertEquals(off, SpeechSettings.decode(SpeechSettings.encode(off)))
        assertTrue(!SpeechSettings.decode(SpeechSettings.encode(off)).headsetButtons)
    }

    @Test
    fun belegungDerKopfhoerertastenIstEinstellbarUndRobust() {
        val standard = SpeechSettings().headsetMapping()
        assertEquals(HeadsetAction.TOGGLE, standard.actionFor(HeadsetButton.PLAY_PAUSE))
        assertEquals(HeadsetAction.NEXT, standard.actionFor(HeadsetButton.NEXT))
        assertEquals(HeadsetAction.PREVIOUS, standard.actionFor(HeadsetButton.PREVIOUS))
        // Belegung wie bei Kopfhörern, die Doppeltippen als Wiedergabe/Pause und Dreifachtippen als Weiter senden.
        val own = SpeechSettings()
            .withHeadsetAction(HeadsetButton.PLAY_PAUSE, HeadsetAction.NEXT)
            .withHeadsetAction(HeadsetButton.NEXT, HeadsetAction.PREVIOUS)
        val back = SpeechSettings.decode(SpeechSettings.encode(own)).headsetMapping()
        assertEquals(HeadsetAction.NEXT, back.actionFor(HeadsetButton.PLAY_PAUSE))
        assertEquals(HeadsetAction.PREVIOUS, back.actionFor(HeadsetButton.NEXT))
        assertEquals(HeadsetAction.PREVIOUS, back.actionFor(HeadsetButton.PREVIOUS))
        // Ältere Einstellungen ohne Belegung und unbekannte Werte ergeben die Standardbelegung.
        assertEquals(standard, SpeechSettings.decode("""{"rate":90}""").headsetMapping())
        assertEquals(standard, SpeechSettings.decode("""{"headsetNext":"explode","headsetPlayPause":"","headsetPrevious":"1"}""").headsetMapping())
        assertEquals(HeadsetAction.NONE, SpeechSettings().withHeadsetAction(HeadsetButton.NEXT, HeadsetAction.NONE).headsetMapping().next)
    }
}
