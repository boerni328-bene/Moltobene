package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class TextGeometryTest {

    @Test
    fun huelleLaesstInnerePunkteWeg() {
        val points = floatArrayOf(0f, 0f, 10f, 0f, 10f, 4f, 0f, 4f, 5f, 2f, 3f, 1f)
        val hull = TextGeometry.convexHull(points)
        assertEquals(4, hull.size / 2)
    }

    @Test
    fun schraegesRechteckWirdGenauGefunden() {
        val angle = (8 * PI / 180).toFloat()
        val c = cos(angle)
        val s = sin(angle)
        // Ecken eines 200 × 30 großen Rechtecks um (100, 50), um 8° gedreht.
        val corners = listOf(-100f to -15f, 100f to -15f, 100f to 15f, -100f to 15f)
        val points = corners.flatMap { (u, v) -> listOf(100 + u * c - v * s, 50 + u * s + v * c) }.toFloatArray()
        val box = TextGeometry.minAreaRect(TextGeometry.convexHull(points))
        assertEquals(100f, box.centerX, 0.01f)
        assertEquals(50f, box.centerY, 0.01f)
        assertEquals(200f, box.width, 0.01f)
        assertEquals(30f, box.height, 0.01f)
        assertEquals(angle, box.angle, 0.001f)
    }

    @Test
    fun breiteIstImmerDieLangeSeiteUndDerWinkelFlach() {
        val box = TextGeometry.normalized(TextBox(0f, 0f, 20f, 100f, (PI / 2).toFloat()))
        assertEquals(100f, box.width, 0.001f)
        assertEquals(20f, box.height, 0.001f)
        assertEquals(true, abs(box.angle) <= PI / 2)
    }

    @Test
    fun skalierenUebertraegtMitteUndGroesse() {
        val box = TextBox(10f, 20f, 100f, 10f, 0f).scaled(2f, 3f)
        assertEquals(20f, box.centerX, 0.001f)
        assertEquals(60f, box.centerY, 0.001f)
        assertEquals(200f, box.width, 0.001f)
        assertEquals(30f, box.height, 0.001f)
    }
}
