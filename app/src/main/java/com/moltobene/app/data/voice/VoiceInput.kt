package com.moltobene.app.data.voice

import kotlinx.coroutines.flow.StateFlow

/** Warum die Spracheingabe nicht läuft (#80). */
enum class VoiceProblem {
    /** Das Erkennungspaket ist nicht (vollständig) auf dem Handy. */
    NO_PACK,

    /** Das Mikrofon ist nicht freigegeben oder wird gerade von einer anderen App benutzt. */
    MICROPHONE,

    /** Zu wenig Arbeitsspeicher für das Erkennungsmodell. */
    LOW_MEMORY,

    /** Sonstiger Fehler beim Laden oder Zuhören. */
    OTHER,
}

/** Zustand der Spracheingabe (#80). */
sealed interface VoiceState {
    data object Off : VoiceState

    /** Das Modell wird geladen. */
    data object Starting : VoiceState

    /** Das Mikrofon hört zu. */
    data object Listening : VoiceState

    data class Failed(val problem: VoiceProblem) : VoiceState
}

/**
 * Spracheingabe in der Kochansicht (#80): hört dauerhaft zu, bis sie ausgeschaltet wird, und meldet erkannte Befehle.
 * Alles läuft auf dem Handy; die Erkennung kennt nur die Befehle aus [VoiceCommands]. Der Rundgang auf dem Emulator ersetzt
 * sie über `AppContainer.voice` durch eine nachgestellte Fassung.
 */
interface VoiceInput {
    val state: StateFlow<VoiceState>

    /** Ist das Erkennungspaket da, sodass [start] etwas tun kann? */
    fun isAvailable(): Boolean

    /**
     * Beginnt zuzuhören. Die Berechtigung „Mikrofon“ muss vorher erteilt sein. Befehle kommen auf dem Hauptthread an.
     */
    fun start(onCommand: (VoiceCommand) -> Unit)

    /** Hört nicht mehr zu und gibt Mikrofon und Speicher frei. */
    fun stop()
}
