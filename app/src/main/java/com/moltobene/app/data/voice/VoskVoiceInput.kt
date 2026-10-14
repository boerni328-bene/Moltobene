package com.moltobene.app.data.voice

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Spracheingabe mit Vosk (Alpha Cephei, Apache 2.0) und dem Erkennungspaket (#80): hört über das Mikrofon zu, erkennt nur die
 * Befehle aus [VoiceCommands] und meldet sie. Alles auf dem Handy; es wird nichts aufgenommen, gespeichert oder gesendet.
 * Die Berechtigung „Mikrofon“ holt die Oberfläche vor [start] ein.
 *
 * @param packDirectory Verzeichnis des installierten Erkennungspakets (`VoicePackManager.directory`)
 * @param scope läuft so lange wie die App; Laden und Aufräumen laufen nie auf dem Hauptthread
 */
class VoskVoiceInput(
    private val packDirectory: () -> File,
    private val scope: CoroutineScope,
) : VoiceInput {
    private val mutableState = MutableStateFlow<VoiceState>(VoiceState.Off)
    override val state: StateFlow<VoiceState> = mutableState.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var job: Job? = null
    private var session: Session? = null

    /** Zählt die Starts, damit ein spät fertig gewordenes Laden nach Stopp und neuem Start nichts durcheinanderbringt. */
    private var generation = 0

    /** Eine laufende Spracheingabe; [live] wird beim Ausschalten zuerst falsch, damit nach dem Stopp kein Befehl mehr ankommt. */
    private class Session(val model: Model, val recognizer: Recognizer, val service: SpeechService, val live: AtomicBoolean) {
        fun close() {
            live.set(false)
            runCatching { service.stop() }
            runCatching { service.shutdown() }
            runCatching { recognizer.close() }
            runCatching { model.close() }
        }
    }

    override fun isAvailable(): Boolean = VoicePack.germanModel(packDirectory()) != null

    override fun start(onCommand: (VoiceCommand) -> Unit) {
        if (job?.isActive == true || session != null) return
        val model = VoicePack.germanModel(packDirectory())
        if (model == null) {
            mutableState.value = VoiceState.Failed(VoiceProblem.NO_PACK)
            return
        }
        mutableState.value = VoiceState.Starting
        val mine = ++generation
        job = scope.launch(Dispatchers.Default) {
            val opened = try {
                open(model, onCommand)
            } catch (e: OutOfMemoryError) {
                failStart(mine, VoiceProblem.LOW_MEMORY)
                return@launch
            } catch (e: IOException) {
                // Das Mikrofon lässt sich nicht öffnen: nicht erteilt oder von einer anderen App belegt.
                failStart(mine, VoiceProblem.MICROPHONE)
                return@launch
            } catch (e: Exception) {
                failStart(mine, VoiceProblem.OTHER)
                return@launch
            }
            // Wurde inzwischen wieder ausgeschaltet (oder neu gestartet), gleich aufräumen.
            main.post {
                if (generation == mine && mutableState.value == VoiceState.Starting) {
                    session = opened
                    mutableState.value = VoiceState.Listening
                } else {
                    scope.launch(Dispatchers.Default) { opened.close() }
                }
            }
        }
    }

    private fun failStart(mine: Int, problem: VoiceProblem) {
        main.post { if (generation == mine && mutableState.value == VoiceState.Starting) mutableState.value = VoiceState.Failed(problem) }
    }

    private fun open(modelDirectory: File, onCommand: (VoiceCommand) -> Unit): Session {
        val live = AtomicBoolean(true)
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        val model = Model(modelDirectory.absolutePath)
        var recognizer: Recognizer? = null
        try {
            recognizer = Recognizer(model, SAMPLE_RATE, VoiceCommands.GERMAN_GRAMMAR)
            val service = SpeechService(recognizer, SAMPLE_RATE)
            service.startListening(
                object : RecognitionListener {
                    // Teilergebnisse bleiben ungenutzt: Erst das Ende einer Äußerung (Pause) löst einen Befehl aus.
                    override fun onPartialResult(hypothesis: String?) = Unit

                    override fun onResult(hypothesis: String?) = deliver(hypothesis, live, onCommand)

                    override fun onFinalResult(hypothesis: String?) = deliver(hypothesis, live, onCommand)

                    override fun onError(exception: Exception?) {
                        main.post { fail(VoiceProblem.MICROPHONE) }
                    }

                    override fun onTimeout() = Unit
                },
            )
            return Session(model, recognizer, service, live)
        } catch (e: Throwable) {
            recognizer?.close()
            model.close()
            throw e
        }
    }

    private fun deliver(hypothesis: String?, live: AtomicBoolean, onCommand: (VoiceCommand) -> Unit) {
        if (!live.get()) return
        val text = runCatching { Json.parseToJsonElement(hypothesis.orEmpty()).jsonObject["text"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        val command = VoiceCommands.parse(text) ?: return
        // SpeechService liefert auf dem Hauptthread; falls nicht, wird hier dorthin gewechselt.
        if (Looper.myLooper() == Looper.getMainLooper()) onCommand(command) else main.post { if (live.get()) onCommand(command) }
    }

    private fun fail(problem: VoiceProblem) {
        closeSession()
        mutableState.value = VoiceState.Failed(problem)
    }

    override fun stop() {
        generation++
        job?.cancel()
        job = null
        closeSession()
        mutableState.value = VoiceState.Off
    }

    private fun closeSession() {
        val current = session ?: return
        session = null
        // Aufräumen kann dauern; nie auf dem Hauptthread.
        scope.launch(Dispatchers.Default) { current.close() }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000f
    }
}
