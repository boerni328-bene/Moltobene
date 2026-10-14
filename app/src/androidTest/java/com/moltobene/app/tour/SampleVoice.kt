package com.moltobene.app.tour

import android.os.Handler
import android.os.Looper
import com.moltobene.app.data.voice.VoiceCommand
import com.moltobene.app.data.voice.VoiceInput
import com.moltobene.app.data.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Nachgestellte Spracheingabe für den Rundgang (#80): Es wird nichts gehört. Der Test „spricht“ Befehle mit [say]. Echte
 * Erkennung braucht das Erkennungspaket und ein echtes Mikrofon; sie wird auf GitHub getrennt geprüft (sprachtest.py).
 */
class SampleVoice(private val available: Boolean = true) : VoiceInput {
    private val mutableState = MutableStateFlow<VoiceState>(VoiceState.Off)
    override val state: StateFlow<VoiceState> = mutableState.asStateFlow()

    @Volatile
    private var onCommand: ((VoiceCommand) -> Unit)? = null

    /** Wie oft die Spracheingabe gestartet wurde. */
    @Volatile
    var starts = 0

    override fun isAvailable(): Boolean = available

    override fun start(onCommand: (VoiceCommand) -> Unit) {
        starts++
        this.onCommand = onCommand
        mutableState.value = VoiceState.Listening
    }

    override fun stop() {
        onCommand = null
        mutableState.value = VoiceState.Off
    }

    /** Ein erkannter Befehl kommt – wie bei der echten Erkennung – auf dem Hauptthread an. */
    fun say(command: VoiceCommand) {
        val callback = onCommand ?: return
        Handler(Looper.getMainLooper()).post { callback(command) }
    }
}
