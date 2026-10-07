package com.moltobene.app.data.translate

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.LongBuffer

/**
 * Übersetzt eine Zeile mit einem Modell OPUS-MT (Marian) über ONNX Runtime, nur auf dem Handy (#60).
 * Gleicher Ablauf wie in der Machbarkeitsprobe: Der Encoder liest die Zeile einmal, der Decoder erzeugt die
 * Übersetzung Wortteil für Wortteil und nimmt jeweils den wahrscheinlichsten („einfache Suche“). Ohne
 * Android-Abhängigkeiten, damit es auch in Unit-Tests mit der PC-Fassung von ONNX Runtime läuft.
 *
 * @param directory ein Verzeichnis des Sprachpakets, z. B. `en-de` (siehe [LanguagePack])
 */
class MarianTranslator(directory: File, threads: Int = DEFAULT_THREADS) : AutoCloseable {

    /** Einstellungen aus `config.json` des Sprachpakets. */
    data class Config(val eos: Int, val pad: Int, val start: Int, val unk: Int, val maxLength: Int)

    private val config = LanguagePack.readConfig(File(directory, LanguagePack.CONFIG))
    private val encoder = File(directory, LanguagePack.SOURCE_PIECES).useLines {
        SentencePieceEncoder.read(it, config.unk)
    }
    private val vocabulary = File(directory, LanguagePack.VOCABULARY).useLines {
        Vocabulary.read(it, setOf(config.eos, config.pad, config.unk))
    }
    private val environment = OrtEnvironment.getEnvironment()
    private val options = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(threads)
        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
    }
    private val encoderSession = environment.createSession(File(directory, LanguagePack.ENCODER).path, options)
    private val decoderSession = try {
        environment.createSession(File(directory, LanguagePack.DECODER).path, options)
    } catch (e: Exception) {
        encoderSession.close()
        throw e
    }

    /** Übersetzt eine Zeile, ohne Schutzregeln (die prüft [TranslationGuard]). */
    fun translate(text: String, run: OrtSession.RunOptions? = null): String {
        val input = encode(text)
        if (input.size <= 1) return text
        return decode(generate(input, maxSteps(input.size), run))
    }

    /** Kennungen der Wortteile mit dem Endzeichen, so wie das Modell sie erwartet. */
    fun encode(text: String): List<Int> = encoder.encode(text) + config.eos

    /** Text aus den Kennungen einer Übersetzung; Start-, End- und Füllzeichen fallen weg. */
    fun decode(ids: List<Int>): String = vocabulary.decode(ids)

    /**
     * Erzeugt die Übersetzung als Kennungen, beginnend mit dem Startzeichen und – wenn erreicht – mit dem Endzeichen.
     * @param maxSteps höchstens so viele Wortteile; schützt vor endlosen Wiederholungen
     */
    fun generate(input: List<Int>, maxSteps: Int = config.maxLength, run: OrtSession.RunOptions? = null): List<Int> {
        val shape = longArrayOf(1, input.size.toLong())
        val ids = input.map { it.toLong() }.toLongArray()
        val mask = LongArray(input.size) { 1L }
        val output = arrayListOf(config.start)
        tensor(ids, shape).use { idsTensor ->
            tensor(mask, shape).use { maskTensor ->
                runSession(encoderSession, mapOf("input_ids" to idsTensor, "attention_mask" to maskTensor), run).use { encoded ->
                    val hidden = encoded.get(0) as OnnxTensor
                    repeat(maxSteps) {
                        val next = nextToken(output, hidden, maskTensor, run)
                        output += next
                        if (next == config.eos) return output
                    }
                }
            }
        }
        return output
    }

    private fun nextToken(output: List<Int>, hidden: OnnxTensor, mask: OnnxTensor, run: OrtSession.RunOptions?): Int {
        val ids = output.map { it.toLong() }.toLongArray()
        tensor(ids, longArrayOf(1, ids.size.toLong())).use { idsTensor ->
            val inputs = mapOf("input_ids" to idsTensor, "encoder_hidden_states" to hidden, "encoder_attention_mask" to mask)
            runSession(decoderSession, inputs, run).use { result ->
                val logits = (result.get(0) as OnnxTensor)
                val size = vocabulary.size.coerceAtMost(logits.info.shape[2].toInt())
                val buffer = logits.floatBuffer
                val offset = (ids.size - 1) * logits.info.shape[2].toInt()
                var best = -1
                var bestScore = Float.NEGATIVE_INFINITY
                for (index in 0 until size) {
                    if (index == config.pad) continue
                    val score = buffer.get(offset + index)
                    if (score > bestScore) {
                        bestScore = score
                        best = index
                    }
                }
                return best
            }
        }
    }

    private fun tensor(values: LongArray, shape: LongArray): OnnxTensor =
        OnnxTensor.createTensor(environment, LongBuffer.wrap(values), shape)

    private fun runSession(session: OrtSession, inputs: Map<String, OnnxTensor>, run: OrtSession.RunOptions?): OrtSession.Result =
        if (run == null) session.run(inputs) else session.run(inputs, run)

    /** Übersetzungen sind selten mehr als doppelt so lang wie das Original; mehr ist meist eine Wiederholung. */
    private fun maxSteps(inputLength: Int): Int = (inputLength * 2 + EXTRA_STEPS).coerceAtMost(config.maxLength)

    override fun close() {
        decoderSession.close()
        encoderSession.close()
        options.close()
    }

    private companion object {
        const val EXTRA_STEPS = 10
        val DEFAULT_THREADS = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
    }
}
