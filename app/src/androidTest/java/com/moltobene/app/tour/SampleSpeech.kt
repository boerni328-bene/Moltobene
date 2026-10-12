package com.moltobene.app.tour

import com.moltobene.app.data.speech.SpeechOutput
import com.moltobene.app.data.speech.SpeechState
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

    override fun speak(language: String, sentences: List<String>) {
        if (noVoice) {
            mutableState.value = SpeechState.NoVoice(language)
        } else {
            spoken += sentences
            mutableState.value = SpeechState.Speaking
        }
    }

    override fun stop() {
        mutableState.value = SpeechState.Idle
    }

    override fun release() {
        mutableState.value = SpeechState.Idle
    }
}
