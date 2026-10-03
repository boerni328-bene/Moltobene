package com.moltobene.app.data.ocr

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Bereitet Graustufenbilder für die Modelle von PP-OCRv6 vor: die ganze Seite für die Zeilensuche und jede
 * gefundene Zeile, gerade gerichtet, für das Lesen. Die Werte liegen außerhalb des Java-Speichers (direkte
 * Puffer), damit auch große Seiten auf günstigen Handys Platz haben. Reines Kotlin, per Unit-Test prüfbar.
 */
object OcrInput {

    /** Höhe einer Zeile beim Lesen – damit wurde das Lesemodell trainiert. */
    const val LINE_HEIGHT = 48

    /** Längere Zeilen werden zusammengedrückt; so viel Text steht kaum in einer Zeile. */
    private const val MAX_LINE_RATIO = 64

    // Mittelwert und Streuung, mit denen das Suchmodell trainiert wurde (je Farbkanal).
    private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    /** Eingabe für die Zeilensuche und die Größe, auf die das Bild dafür gebracht wurde (Vielfache von 32). */
    class Scaled(val values: FloatBuffer, val width: Int, val height: Int)

    /** Seitenmaße für die Zeilensuche: höchstens [maxSide] lang, beide Seiten Vielfache von 32. */
    fun detectionSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val scale = min(1f, maxSide.toFloat() / max(width, height))
        fun fit(side: Int) = max(32, (side * scale / 32f).roundToInt() * 32)
        return fit(width) to fit(height)
    }

    /** Die ganze Seite für die Zeilensuche: verkleinert, in drei gleiche Farbkanäle umgerechnet. */
    fun forDetection(image: GrayImage, maxSide: Int): Scaled {
        val (width, height) = detectionSize(image.width, image.height, maxSide)
        val plane = width * height
        val values = floatBuffer(3 * plane)
        val scaleX = image.width.toFloat() / width
        val scaleY = image.height.toFloat() / height
        for (y in 0 until height) {
            val sourceY = (y + 0.5f) * scaleY - 0.5f
            for (x in 0 until width) {
                val gray = sample(image, (x + 0.5f) * scaleX - 0.5f, sourceY, areaScale = max(scaleX, scaleY)) / 255f
                val index = y * width + x
                for (channel in 0 until 3) values.put(channel * plane + index, (gray - MEAN[channel]) / STD[channel])
            }
        }
        return Scaled(values, width, height)
    }

    /** Breite einer Zeile beim Lesen: Höhe [LINE_HEIGHT], Seitenverhältnis wie im Bild. */
    fun lineWidth(box: TextBox): Int =
        ceil(LINE_HEIGHT * box.width / max(box.height, 1f)).toInt().coerceIn(8, LINE_HEIGHT * MAX_LINE_RATIO)

    /** Eine Zeile gerade gerichtet, [LINE_HEIGHT] hoch, für das Lesemodell aufbereitet (−1 = schwarz, 1 = weiß). */
    fun forLine(image: GrayImage, box: TextBox): Scaled {
        val width = lineWidth(box)
        val height = LINE_HEIGHT
        val plane = width * height
        val values = floatBuffer(3 * plane)
        val c = cos(box.angle)
        val s = sin(box.angle)
        val stepU = box.width / width
        val stepV = box.height / height
        val areaScale = max(stepU, stepV)
        for (y in 0 until height) {
            val v = (y + 0.5f) * stepV - box.height / 2
            for (x in 0 until width) {
                val u = (x + 0.5f) * stepU - box.width / 2
                val sourceX = box.centerX + u * c - v * s - 0.5f
                val sourceY = box.centerY + u * s + v * c - 0.5f
                val value = sample(image, sourceX, sourceY, areaScale) / 127.5f - 1f
                val index = y * width + x
                values.put(index, value)
                values.put(plane + index, value)
                values.put(2 * plane + index, value)
            }
        }
        return Scaled(values, width, height)
    }

    /**
     * Grauwert an einer Stelle, bilinear; außerhalb des Bildes weiß. Beim deutlichen Verkleinern ([areaScale] ≥ 2)
     * wird über die umliegenden Punkte gemittelt, damit feine Schrift nicht flimmert.
     */
    internal fun sample(image: GrayImage, x: Float, y: Float, areaScale: Float = 1f): Float {
        if (areaScale >= 2f) {
            val half = areaScale / 4
            return (bilinear(image, x - half, y - half) + bilinear(image, x + half, y - half) +
                bilinear(image, x - half, y + half) + bilinear(image, x + half, y + half)) / 4
        }
        return bilinear(image, x, y)
    }

    private fun bilinear(image: GrayImage, x: Float, y: Float): Float {
        if (x < -0.5f || y < -0.5f || x > image.width - 0.5f || y > image.height - 0.5f) return 255f
        val x0 = floor(x).toInt().coerceIn(0, image.width - 1)
        val y0 = floor(y).toInt().coerceIn(0, image.height - 1)
        val x1 = min(x0 + 1, image.width - 1)
        val y1 = min(y0 + 1, image.height - 1)
        val fx = (x - x0).coerceIn(0f, 1f)
        val fy = (y - y0).coerceIn(0f, 1f)
        val pixels = image.pixels
        val w = image.width
        fun at(px: Int, py: Int) = (pixels[py * w + px].toInt() and 0xFF).toFloat()
        val top = at(x0, y0) * (1 - fx) + at(x1, y0) * fx
        val bottom = at(x0, y1) * (1 - fx) + at(x1, y1) * fx
        return top * (1 - fy) + bottom * fy
    }

    private fun floatBuffer(size: Int): FloatBuffer =
        ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
}
