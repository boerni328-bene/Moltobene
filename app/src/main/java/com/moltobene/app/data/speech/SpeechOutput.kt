package com.moltobene.app.data.speech

import kotlinx.coroutines.flow.StateFlow

/** Was das Vorlesen (#79) gerade tut. */
sealed interface SpeechState {
    data object Idle : SpeechState

    data object Speaking : SpeechState

    /** Für [language] gibt es keine Stimme, die ohne Internet funktioniert. */
    data class NoVoice(val language: String) : SpeechState

    /** Auf dem Handy ist keine Sprachausgabe eingerichtet oder sie startet nicht. */
    data object Unavailable : SpeechState
}

/**
 * Vorlesen (#79): nur auf Antippen, nur mit Stimmen, die ohne Internet funktionieren, und nur in der Sprache des
 * gezeigten Rezepttexts – nie mit einer falschen Stimme. Die App fragt dafür weder Berechtigungen an noch läuft ein
 * Dienst. Im Rundgang ersetzt eine nachgestellte Fassung die echte Sprachausgabe (`AppContainer.speech`).
 */
interface SpeechOutput {
    val state: StateFlow<SpeechState>

    /** Liest [sentences] satzweise vor und ersetzt, was gerade gelesen wird. */
    fun speak(language: String, sentences: List<String>)

    /** Hält das Vorlesen an und setzt Meldungen („keine Stimme“) zurück. */
    fun stop()

    /** Gibt die Sprachausgabe frei, z. B. beim Verlassen der Rezeptansicht. */
    fun release()
}
