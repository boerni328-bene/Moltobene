package com.moltobene.app.data.voice

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Wandelt Rohton (16 Bit, mehrere Kanäle, beliebige Abtastrate) in das, was die Spracherkennung erwartet: ein Kanal,
 * 16.000 Abtastwerte je Sekunde. Arbeitet in Stücken, der Rest zwischen zwei Stücken bleibt erhalten. Beim Verkleinern der
 * Abtastrate wird über die zusammengehörenden Werte gemittelt, das dämpft Störungen durch das Weglassen von Werten.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
class PcmConverter(
    private val sourceRate: Int,
    private val channels: Int,
    private val targetRate: Int = TARGET_RATE,
) {
    private val step = sourceRate.toDouble() / targetRate
    private val window = max(1, step.roundToInt())

    /** Noch nicht verbrauchte Werte aus dem letzten Stück. */
    private var tail = ShortArray(0)

    /** Stelle des nächsten Ausgabewerts, gemessen vom Anfang von [tail]. */
    private var phase = 0.0

    /**
     * Wandelt die ersten [length] Werte von [input] (abwechselnd je Kanal) um; liefert die fertigen Werte mit
     * [TARGET_RATE] und einem Kanal.
     */
    fun convert(input: ShortArray, length: Int = input.size): ShortArray {
        val frames = length / channels
        val mono = ShortArray(frames) { frame ->
            var sum = 0
            for (channel in 0 until channels) sum += input[frame * channels + channel]
            (sum / channels).toShort()
        }
        if (sourceRate == targetRate) return mono

        val buffer = tail + mono
        val out = ArrayList<Short>(buffer.size / window + 1)
        var position = phase
        while (true) {
            val index = position.toInt()
            if (index + window > buffer.size) break
            var sum = 0
            for (i in index until index + window) sum += buffer[i]
            out += (sum.toDouble() / window).roundToInt().toShort()
            position += step
        }
        val consumed = position.toInt().coerceAtMost(buffer.size)
        tail = buffer.copyOfRange(consumed, buffer.size)
        phase = position - consumed
        return out.toShortArray()
    }

    companion object {
        /** Abtastrate, die das Erkennungsmodell erwartet. */
        const val TARGET_RATE = 16_000
    }
}
