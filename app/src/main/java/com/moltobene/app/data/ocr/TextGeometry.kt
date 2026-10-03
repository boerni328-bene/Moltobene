package com.moltobene.app.data.ocr

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Eine gefundene Textzeile als gedrehtes Rechteck: Mitte, Breite entlang der Schrift (immer die lange Seite),
 * Höhe und Winkel der Schriftrichtung im Bogenmaß (−π/2 bis π/2, 0 = waagrecht).
 */
data class TextBox(val centerX: Float, val centerY: Float, val width: Float, val height: Float, val angle: Float) {

    /** Dieselbe Zeile in einem Bild, das um [scaleX] und [scaleY] größer ist. */
    fun scaled(scaleX: Float, scaleY: Float): TextBox {
        val c = cos(angle)
        val s = sin(angle)
        return TextBox(
            centerX = centerX * scaleX,
            centerY = centerY * scaleY,
            width = width * hypot(scaleX * c, scaleY * s),
            height = height * hypot(scaleX * s, scaleY * c),
            angle = atan2(scaleY * s, scaleX * c),
        )
    }
}

/** Geometrie für die Zeilensuche: konvexe Hülle und kleinstes gedrehtes Rechteck. Reines Kotlin, per Unit-Test prüfbar. */
object TextGeometry {

    /**
     * Konvexe Hülle (Andrew-Verfahren) von Punkten, abwechselnd x und y in [points]; liefert die Ecken
     * gegen den Uhrzeigersinn, ebenfalls abwechselnd x und y.
     */
    fun convexHull(points: FloatArray): FloatArray {
        val count = points.size / 2
        if (count < 3) return points.copyOf()
        val order = (0 until count).sortedWith(compareBy({ points[2 * it] }, { points[2 * it + 1] }))
        val hull = IntArray(2 * count)
        var size = 0
        fun cross(o: Int, a: Int, b: Int): Float =
            (points[2 * a] - points[2 * o]) * (points[2 * b + 1] - points[2 * o + 1]) -
                (points[2 * a + 1] - points[2 * o + 1]) * (points[2 * b] - points[2 * o])
        for (index in order) {
            while (size >= 2 && cross(hull[size - 2], hull[size - 1], index) <= 0f) size--
            hull[size++] = index
        }
        val lowerSize = size + 1
        for (i in order.indices.reversed().drop(1)) {
            val index = order[i]
            while (size >= lowerSize && cross(hull[size - 2], hull[size - 1], index) <= 0f) size--
            hull[size++] = index
        }
        size-- // Der letzte Punkt ist wieder der erste.
        val result = FloatArray(2 * size)
        for (i in 0 until size) {
            result[2 * i] = points[2 * hull[i]]
            result[2 * i + 1] = points[2 * hull[i] + 1]
        }
        return result
    }

    /**
     * Kleinstes gedrehtes Rechteck um eine konvexe Hülle: Eine Seite des kleinsten Rechtecks liegt immer auf
     * einer Kante der Hülle; deshalb wird jede Kante ausprobiert. Die Breite ist die lange Seite.
     */
    fun minAreaRect(hull: FloatArray): TextBox {
        val count = hull.size / 2
        require(count >= 1) { "Keine Punkte" }
        var bestArea = Float.MAX_VALUE
        var best = TextBox(hull[0], hull[1], 0f, 0f, 0f)
        for (i in 0 until count) {
            val j = (i + 1) % count
            val dx = hull[2 * j] - hull[2 * i]
            val dy = hull[2 * j + 1] - hull[2 * i + 1]
            val length = sqrt(dx * dx + dy * dy)
            if (length == 0f) continue
            val ux = dx / length
            val uy = dy / length
            var minA = Float.MAX_VALUE
            var maxA = -Float.MAX_VALUE
            var minB = Float.MAX_VALUE
            var maxB = -Float.MAX_VALUE
            for (k in 0 until count) {
                val x = hull[2 * k]
                val y = hull[2 * k + 1]
                val a = x * ux + y * uy
                val b = -x * uy + y * ux
                if (a < minA) minA = a
                if (a > maxA) maxA = a
                if (b < minB) minB = b
                if (b > maxB) maxB = b
            }
            val area = (maxA - minA) * (maxB - minB)
            if (area < bestArea) {
                bestArea = area
                val midA = (maxA + minA) / 2
                val midB = (maxB + minB) / 2
                best = TextBox(
                    centerX = midA * ux - midB * uy,
                    centerY = midA * uy + midB * ux,
                    width = maxA - minA,
                    height = maxB - minB,
                    angle = atan2(uy, ux),
                )
            }
        }
        if (count < 3 && best.width == 0f) return best
        return normalized(best)
    }

    /** Breite = lange Seite, Winkel zwischen −π/2 und π/2 – sonst stünde die Zeile beim Lesen auf dem Kopf. */
    fun normalized(box: TextBox): TextBox {
        var width = box.width
        var height = box.height
        var angle = box.angle
        if (width < height) {
            width = box.height
            height = box.width
            angle += (PI / 2).toFloat()
        }
        while (angle > PI / 2) angle -= PI.toFloat()
        while (angle <= -PI / 2) angle += PI.toFloat()
        return TextBox(box.centerX, box.centerY, width, height, angle)
    }
}
