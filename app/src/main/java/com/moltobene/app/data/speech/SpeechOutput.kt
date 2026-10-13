package com.moltobene.app.data.speech

import kotlinx.coroutines.flow.StateFlow

/** Was das Vorlesen (#79) gerade tut. */
sealed interface SpeechState {
    data object Idle : SpeechState

    /** Es wird vorgelesen; [usedFallback]: die gewählte Stimme gibt es nicht mehr, die beste passende wurde genommen (#85). */
    data class Speaking(val usedFallback: Boolean = false) : SpeechState

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

    /**
     * Liest [sentences] satzweise vor und ersetzt, was gerade gelesen wird. Stimme, Tempo und Tonhöhe kommen aus den
     * Einstellungen (#85) oder, für die Hörprobe, aus [settings].
     */
    fun speak(language: String, sentences: List<String>, settings: SpeechSettings? = null)

    /**
     * Die auf dem Handy installierten Stimmen für [language], die ohne Internet funktionieren, die passendste zuerst
     * (#85). null, wenn keine Sprachausgabe eingerichtet ist. Startet die Sprachausgabe bei Bedarf.
     */
    suspend fun voices(language: String): List<VoiceInfo>?

    /** Hält das Vorlesen an und setzt Meldungen („keine Stimme“) zurück. */
    fun stop()

    /** Gibt die Sprachausgabe frei, z. B. beim Verlassen der Rezeptansicht. */
    fun release()
}
