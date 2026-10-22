package com.moltobene.app.data.voice

import kotlinx.coroutines.flow.StateFlow

/** Zustand von „Aufnehmen“ (Rezept einsprechen, Beta). */
sealed interface DictationState {
    data object Off : DictationState

    /** Das Modell wird geladen. */
    data object Starting : DictationState

    /** Das Mikrofon hört zu; [text] ist das bisher Erkannte (fertige Sätze und der gerade gesprochene Teil). */
    data class Listening(val text: String) : DictationState

    /** Der Rest wird noch fertig erkannt, gleich ist der Text da. */
    data object Finishing : DictationState

    data class Failed(val problem: VoiceProblem) : DictationState
}

/**
 * Rezept einsprechen („Aufnehmen“, Beta): Das Mikrofon hört zu, die Sprache wird frei erkannt (ohne Befehlsliste) und als Text
 * mit Zeilen geliefert, den die Oberfläche zum Entwurf macht. Alles läuft auf dem Handy mit dem Erkennungspaket; es wird kein
 * Ton gespeichert oder gesendet. Der Rundgang auf dem Emulator ersetzt sie über `AppContainer.dictation` durch eine
 * nachgestellte Fassung.
 */
interface Dictation {
    val state: StateFlow<DictationState>

    /** Ist das Erkennungspaket da, sodass [start] etwas tun kann? */
    fun isAvailable(): Boolean

    /** Beginnt zuzuhören. Die Berechtigung „Mikrofon“ muss vorher erteilt sein. */
    fun start()

    /** Beendet das Zuhören und liefert die erkannten Äußerungen (eine je Sprechpause) auf dem Hauptthread. */
    fun finish(onResult: (List<String>) -> Unit)

    /** Bricht ab und verwirft alles; Mikrofon und Speicher werden frei. */
    fun cancel()
}
