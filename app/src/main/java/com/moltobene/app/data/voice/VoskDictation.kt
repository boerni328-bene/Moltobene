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
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.IOException

/**
 * „Aufnehmen“ mit Vosk (Alpha Cephei, Apache 2.0) und dem Erkennungspaket: Das Mikrofon hört zu, [TranscriptText] liest die
 * Ergebnisse. Wie bei [VoskTranscriber] ohne Befehlsliste, aber mit Teilergebnissen für die Anzeige beim Sprechen. Nichts wird
 * aufgenommen oder gespeichert. Die Berechtigung „Mikrofon“ holt die Oberfläche vor [start] ein.
 *
 * @param packDirectory Verzeichnis des installierten Erkennungspakets (`VoicePackManager.directory`)
 * @param scope läuft so lange wie die App; Laden und Aufräumen laufen nie auf dem Hauptthread
 */
class VoskDictation(
    private val packDirectory: () -> File,
    private val scope: CoroutineScope,
) : Dictation {
    private val mutableState = MutableStateFlow<DictationState>(DictationState.Off)
    override val state: StateFlow<DictationState> = mutableState.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var job: Job? = null
    private var session: Session? = null

    /** Zählt die Starts, damit ein spät fertig gewordenes Laden nach Abbruch und neuem Start nichts durcheinanderbringt. */
    private var generation = 0

    /** Fertige Äußerungen und der gerade gesprochene Teil; nur auf dem Hauptthread angefasst. */
    private val lines = mutableListOf<String>()
    private var partial = ""
    private var onFinished: ((List<String>) -> Unit)? = null

    private class Session(val model: Model, val recognizer: Recognizer, val service: SpeechService) {
        fun close() {
            runCatching { service.stop() }
            runCatching { service.shutdown() }
            runCatching { recognizer.close() }
            runCatching { model.close() }
        }
    }

    override fun isAvailable(): Boolean = VoicePack.germanModel(packDirectory()) != null

    override fun start() {
        if (job?.isActive == true || session != null) return
        val model = VoicePack.germanModel(packDirectory())
        if (model == null) {
            mutableState.value = DictationState.Failed(VoiceProblem.NO_PACK)
            return
        }
        lines.clear()
        partial = ""
        onFinished = null
        mutableState.value = DictationState.Starting
        val mine = ++generation
        job = scope.launch(Dispatchers.Default) {
            val opened = try {
                open(model, mine)
            } catch (e: OutOfMemoryError) {
                failStart(mine, VoiceProblem.LOW_MEMORY)
                return@launch
            } catch (e: IOException) {
                failStart(mine, VoiceProblem.MICROPHONE)
                return@launch
            } catch (e: Exception) {
                failStart(mine, VoiceProblem.OTHER)
                return@launch
            }
            main.post {
                if (generation == mine && mutableState.value == DictationState.Starting) {
                    session = opened
                    mutableState.value = DictationState.Listening("")
                } else {
                    scope.launch(Dispatchers.Default) { opened.close() }
                }
            }
        }
    }

    private fun failStart(mine: Int, problem: VoiceProblem) {
        main.post { if (generation == mine && mutableState.value == DictationState.Starting) mutableState.value = DictationState.Failed(problem) }
    }

    private fun open(modelDirectory: File, mine: Int): Session {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        val model = Model(modelDirectory.absolutePath)
        var recognizer: Recognizer? = null
        try {
            recognizer = Recognizer(model, SAMPLE_RATE)
            val service = SpeechService(recognizer, SAMPLE_RATE)
            service.startListening(
                object : RecognitionListener {
                    override fun onPartialResult(hypothesis: String?) = update(mine, TranscriptText.partialFromJson(hypothesis.orEmpty()), false)

                    override fun onResult(hypothesis: String?) = update(mine, TranscriptText.fromJson(hypothesis.orEmpty()), true)

                    override fun onFinalResult(hypothesis: String?) {
                        update(mine, TranscriptText.fromJson(hypothesis.orEmpty()), true)
                        main.post { complete(mine) }
                    }

                    override fun onError(exception: Exception?) {
                        main.post { fail(mine, VoiceProblem.MICROPHONE) }
                    }

                    override fun onTimeout() = Unit
                },
            )
            return Session(model, recognizer, service)
        } catch (e: Throwable) {
            recognizer?.close()
            model.close()
            throw e
        }
    }

    /** Meldet Erkanntes weiter; [done] = ein fertiger Satz (am Ende einer Sprechpause), sonst nur der laufende Teil. */
    private fun update(mine: Int, text: String, done: Boolean) {
        main.post {
            if (generation != mine) return@post
            if (done) {
                if (text.isNotBlank()) lines.add(text)
                partial = ""
            } else {
                partial = text
            }
            val current = mutableState.value
            if (current is DictationState.Listening) mutableState.value = DictationState.Listening((lines + partial).filter { it.isNotBlank() }.joinToString(" "))
        }
    }

    private fun fail(mine: Int, problem: VoiceProblem) {
        if (generation != mine) return
        closeSession()
        mutableState.value = DictationState.Failed(problem)
    }

    override fun finish(onResult: (List<String>) -> Unit) {
        val current = session
        if (current == null || mutableState.value !is DictationState.Listening) {
            onResult(emptyList())
            return
        }
        onFinished = onResult
        mutableState.value = DictationState.Finishing
        val mine = generation
        // Das Ende der Erkennung kommt mit dem Schlussergebnis; kommt es nicht, gilt nach kurzer Zeit das bisher Erkannte.
        main.postDelayed({ complete(mine) }, FINISH_TIMEOUT_MILLIS)
        scope.launch(Dispatchers.Default) { runCatching { current.service.stop() } }
    }

    /** Schließt ab: liefert die Äußerungen an den Wartenden und gibt Mikrofon und Speicher frei. */
    private fun complete(mine: Int) {
        if (generation != mine) return
        val callback = onFinished ?: return
        onFinished = null
        val result = (lines + partial).filter { it.isNotBlank() }
        closeSession()
        generation++
        mutableState.value = DictationState.Off
        callback(result)
    }

    override fun cancel() {
        generation++
        job?.cancel()
        job = null
        onFinished = null
        lines.clear()
        partial = ""
        closeSession()
        mutableState.value = DictationState.Off
    }

    private fun closeSession() {
        val current = session ?: return
        session = null
        scope.launch(Dispatchers.Default) { current.close() }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000f
        const val FINISH_TIMEOUT_MILLIS = 3_000L
    }
}
