package com.moltobene.app.tour

import com.moltobene.app.data.speech.SpeechOutput
import com.moltobene.app.data.speech.SpeechSettings
import com.moltobene.app.data.speech.SpeechState
import com.moltobene.app.data.speech.VoiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Nachgestelltes Vorlesen für den Rundgang (#79): Auf dem Emulator gibt es keine Stimme. Sie merkt sich, was gelesen
 * werden sollte, und liest nie von selbst zu Ende – „Anhalten“ beendet das Vorlesen.
 */
class SampleSpeech(private val noVoice: Boolean = false) : SpeechOutput {
    private val mutableState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    override val state: StateFlow<SpeechState> = mutableState.asStateFlow()

    /** Alles, was zum Vorlesen übergeben wurde, je Aufruf eine Liste von Sätzen. */
    val spoken = CopyOnWriteArrayList<List<String>>()

    /** Mit welchen Einstellungen zuletzt gesprochen wurde (Hörprobe), null für die gemerkten. */
    @Volatile
    var lastSettings: SpeechSettings? = null

    override fun speak(language: String, sentences: List<String>, settings: SpeechSettings?) {
        if (noVoice) {
            mutableState.value = SpeechState.NoVoice(language)
        } else {
            spoken += sentences
            lastSettings = settings
            mutableState.value = SpeechState.Speaking()
        }
    }

    override suspend fun voices(language: String): List<VoiceInfo>? = if (noVoice) {
        emptyList()
    } else {
        listOf(
            VoiceInfo("${language}-${if (language == "de") "de" else "us"}-x-probe-local", language, if (language == "de") "DE" else "US", 400, false),
            VoiceInfo("${language}-${if (language == "de") "at" else "gb"}-x-zweite-local", language, if (language == "de") "AT" else "GB", 300, false),
        )
    }

    override fun stop() {
        mutableState.value = SpeechState.Idle
    }

    override fun release() {
        mutableState.value = SpeechState.Idle
    }
}
