package com.moltobene.app.data.voice

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

/**
 * Freie Spracherkennung mit Vosk (Alpha Cephei, Apache 2.0) und dem Erkennungspaket (#80), diesmal ohne Befehlsliste:
 * [AudioDecoder] liefert den Rohton einer Audiodatei, Vosk schreibt ihn mit. Das kleine Modell macht Fehler, vor allem bei
 * Hintergrundgeräuschen, schnellem Sprechen und Eigennamen; deshalb ist der Weg als Beta gekennzeichnet und der Text wird
 * immer zum Prüfen angeboten.
 *
 * @param packDirectory Verzeichnis des installierten Erkennungspakets (`VoicePackManager.directory`)
 */
class VoskTranscriber(
    private val decoder: AudioDecoder,
    private val packDirectory: () -> File,
) : Transcriber {

    override fun isAvailable(): Boolean = VoicePack.germanModel(packDirectory()) != null

    override suspend fun transcribe(uri: Uri, onProgress: (Float) -> Unit): List<String> = withContext(Dispatchers.Default) {
        val modelDirectory = VoicePack.germanModel(packDirectory()) ?: throw TranscribeException(TranscribeException.Problem.NO_PACK)
        val lines = mutableListOf<String>()
        var model: Model? = null
        var recognizer: Recognizer? = null
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS)
            model = Model(modelDirectory.absolutePath)
            val active = Recognizer(model, PcmConverter.TARGET_RATE.toFloat()).also { recognizer = it }
            decoder.decode(
                uri,
                onChunk = { bytes, length ->
                    ensureActive()
                    // true am Ende einer Äußerung (Sprechpause): dann steht ihr Text bereit.
                    if (active.acceptWaveForm(bytes, length)) TranscriptText.fromJson(active.result).takeIf { it.isNotEmpty() }?.let(lines::add)
                },
                onProgress = onProgress,
            )
            TranscriptText.fromJson(active.finalResult).takeIf { it.isNotEmpty() }?.let(lines::add)
            lines
        } catch (e: AudioDecoder.DecodeException) {
            throw TranscribeException(
                when (e.problem) {
                    AudioDecoder.DecodeException.Problem.UNSUPPORTED -> TranscribeException.Problem.UNSUPPORTED
                    AudioDecoder.DecodeException.Problem.TOO_LONG -> TranscribeException.Problem.TOO_LONG
                    AudioDecoder.DecodeException.Problem.FAILED -> TranscribeException.Problem.FAILED
                },
                e,
            )
        } catch (e: OutOfMemoryError) {
            throw TranscribeException(TranscribeException.Problem.LOW_MEMORY, e)
        } finally {
            runCatching { recognizer?.close() }
            runCatching { model?.close() }
        }
    }
}
