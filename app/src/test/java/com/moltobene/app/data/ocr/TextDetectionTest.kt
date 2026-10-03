package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDetectionTest {

    private val width = 200
    private val height = 100

    private fun map(fill: (x: Int, y: Int) -> Float): FloatArray =
        FloatArray(width * height) { index -> fill(index % width, index / width) }

    @Test
    fun zweiZeilenWerdenZweiKaesten() {
        val probability = map { x, y ->
            when {
                y in 10..19 && x in 20..179 -> 0.9f
                y in 50..57 && x in 20..99 -> 0.8f
                else -> 0f
            }
        }
        val boxes = TextDetection.boxes(probability, width, height).sortedBy { it.centerY }
        assertEquals(2, boxes.size)
        // Etwas größer als die Fläche, weil das Modell Zeilen schmaler meldet, als die Schrift ist.
        assertTrue(boxes[0].width > 160f && boxes[0].height > 10f)
        assertEquals(15f, boxes[0].centerY, 0.5f)
        assertEquals(100f, boxes[0].centerX, 0.5f)
        assertEquals(0f, boxes[0].angle, 0.001f)
    }

    @Test
    fun schwacheUndWinzigeFleckenFallenWeg() {
        val probability = map { x, y ->
            when {
                y in 10..19 && x in 20..179 -> 0.35f // über der Schwelle, aber im Mittel zu unsicher
                y in 60..61 && x in 60..61 -> 0.99f // Staubkorn
                else -> 0f
            }
        }
        assertTrue(TextDetection.boxes(probability, width, height).isEmpty())
    }

    @Test
    fun ueberEckVerbundenIstEineZeile() {
        // Zwei Flächen, die sich nur an einer Ecke berühren, gehören zusammen (8er-Nachbarschaft).
        val probability = map { x, y ->
            if ((y in 10..19 && x in 20..59) || (y in 20..29 && x in 60..99)) 0.9f else 0f
        }
        assertEquals(1, TextDetection.boxes(probability, width, height).size)
    }
}
