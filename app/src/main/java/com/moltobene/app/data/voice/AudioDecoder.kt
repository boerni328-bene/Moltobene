package com.moltobene.app.data.voice

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.IOException
import java.nio.ByteOrder

/**
 * Liest eine Audiodatei (mp3, m4a, ogg, opus, wav, Sprachnachrichten …) mit der Technik, die in Android eingebaut ist, und
 * gibt sie als Rohton für die Spracherkennung weiter: 16 Bit, ein Kanal, 16.000 Abtastwerte je Sekunde ([PcmConverter]).
 * Die Datei wird nur gelesen, nie kopiert, verändert oder gespeichert; es braucht weder eine Berechtigung noch eine
 * Bibliothek.
 */
class AudioDecoder(private val context: Context) {

    class DecodeException(val problem: Problem, cause: Throwable? = null) : IOException(problem.name, cause) {
        enum class Problem {
            /** Kein Audio in der Datei oder ein Format, das Android nicht lesen kann. */
            UNSUPPORTED,

            /** Länger als [MAX_SECONDS]. */
            TOO_LONG,

            FAILED,
        }
    }

    /**
     * Liest [uri] und gibt den Rohton in Stücken an [onChunk] (Bytes, Anzahl); [onProgress] meldet 0 bis 1, wenn die Länge
     * bekannt ist. Läuft auf dem aufrufenden Thread; [onChunk] darf eine Abbruch-Ausnahme werfen.
     * @throws DecodeException wenn die Datei sich nicht lesen lässt oder zu lang ist
     */
    fun decode(uri: Uri, onChunk: (ByteArray, Int) -> Unit, onProgress: (Float) -> Unit = {}) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            try {
                extractor.setDataSource(context, uri, null)
            } catch (e: Exception) {
                throw DecodeException(DecodeException.Problem.FAILED, e)
            }
            val track = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw DecodeException(DecodeException.Problem.UNSUPPORTED)
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw DecodeException(DecodeException.Problem.UNSUPPORTED)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L
            if (durationUs > MAX_SECONDS * 1_000_000L) throw DecodeException(DecodeException.Problem.TOO_LONG)

            val decoder = try {
                MediaCodec.createDecoderByType(mime).also {
                    it.configure(format, null, null, 0)
                    it.start()
                }
            } catch (e: Exception) {
                throw DecodeException(DecodeException.Problem.UNSUPPORTED, e)
            }
            codec = decoder
            run(extractor, decoder, format, durationUs, onChunk, onProgress)
        } catch (e: DecodeException) {
            throw e
        } catch (e: IllegalStateException) {
            throw DecodeException(DecodeException.Problem.FAILED, e)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun run(
        extractor: MediaExtractor,
        codec: MediaCodec,
        format: MediaFormat,
        durationUs: Long,
        onChunk: (ByteArray, Int) -> Unit,
        onProgress: (Float) -> Unit,
    ) {
        var converter = converterFor(format)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var producedSamples = 0L
        while (!outputDone) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(TIMEOUT_US)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index) ?: throw DecodeException(DecodeException.Problem.FAILED)
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                        if (durationUs > 0) onProgress((extractor.sampleTime.toFloat() / durationUs).coerceIn(0f, 1f))
                        extractor.advance()
                    }
                }
            }
            val out = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            when {
                out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> converter = converterFor(codec.outputFormat)
                out >= 0 -> {
                    if (info.size > 0) {
                        val buffer = codec.getOutputBuffer(out) ?: throw DecodeException(DecodeException.Problem.FAILED)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val shorts = ShortArray(info.size / 2)
                        buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                        val converted = converter.convert(shorts)
                        if (converted.isNotEmpty()) {
                            producedSamples += converted.size
                            if (producedSamples > MAX_SECONDS.toLong() * PcmConverter.TARGET_RATE) {
                                throw DecodeException(DecodeException.Problem.TOO_LONG)
                            }
                            onChunk(toBytes(converted), converted.size * 2)
                        }
                    }
                    codec.releaseOutputBuffer(out, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }
        onProgress(1f)
    }

    private fun converterFor(format: MediaFormat): PcmConverter = PcmConverter(
        sourceRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1),
    )

    private fun toBytes(samples: ShortArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            bytes[i * 2] = samples[i].toInt().toByte()
            bytes[i * 2 + 1] = (samples[i].toInt() shr 8).toByte()
        }
        return bytes
    }

    companion object {
        /** Längere Aufnahmen werden nicht gelesen: Die Erkennung dauert sonst zu lange und füllt den Akku. */
        const val MAX_SECONDS = 30 * 60

        private const val TIMEOUT_US = 10_000L
    }
}
