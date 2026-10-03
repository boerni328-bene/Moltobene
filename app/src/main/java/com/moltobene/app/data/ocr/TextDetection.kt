package com.moltobene.app.data.ocr

import kotlin.math.min

/**
 * Macht aus der Wahrscheinlichkeitskarte des Erkennungsmodells (je Bildpunkt: „hier steht Text“) die einzelnen
 * Textzeilen. Zusammenhängende Flächen über [threshold] sind je eine Zeile; ihr kleinstes gedrehtes Rechteck wird
 * etwas vergrößert ([unclipRatio]), weil das Modell die Zeilen schmaler meldet, als die Schrift ist.
 * Verfahren wie bei PaddleOCR (DB), aber ohne OpenCV. Reines Kotlin, per Unit-Test prüfbar.
 */
object TextDetection {

    const val THRESHOLD = 0.3f
    const val BOX_THRESHOLD = 0.45f
    const val UNCLIP_RATIO = 1.4f

    /** Kleinere Flecken sind Staub oder Reste von Bildern. */
    private const val MIN_PIXELS = 6

    /** Zeilen, die schmaler sind (in Bildpunkten der Karte), lassen sich nicht lesen. */
    private const val MIN_SIDE = 3f

    /**
     * @param probability Karte Zeile für Zeile, [width] × [height] Werte zwischen 0 und 1
     * @return Zeilen in Koordinaten der Karte
     */
    fun boxes(
        probability: FloatArray,
        width: Int,
        height: Int,
        threshold: Float = THRESHOLD,
        boxThreshold: Float = BOX_THRESHOLD,
        unclipRatio: Float = UNCLIP_RATIO,
    ): List<TextBox> {
        require(probability.size >= width * height) { "Karte zu klein" }
        val labels = label(probability, width, height, threshold)
        val components = collect(labels.first, labels.second, probability, width, height)
        return components.mapNotNull { component ->
            if (component.pixels < MIN_PIXELS) return@mapNotNull null
            if (component.probabilitySum / component.pixels < boxThreshold) return@mapNotNull null
            val rect = TextGeometry.minAreaRect(TextGeometry.convexHull(component.corners()))
            if (min(rect.width, rect.height) < MIN_SIDE) return@mapNotNull null
            // Vergrößern um denselben Abstand an allen Seiten (wie das Verfahren „unclip“ von PaddleOCR).
            val distance = rect.width * rect.height * unclipRatio / (2 * (rect.width + rect.height))
            TextGeometry.normalized(rect.copy(width = rect.width + 2 * distance, height = rect.height + 2 * distance))
        }
    }

    /** Fläche aus Bildpunkten: Anzahl, Summe der Wahrscheinlichkeiten und die Ecken ihrer waagrechten Läufe. */
    private class Component {
        var pixels = 0
        var probabilitySum = 0f
        private var corners = FloatArray(32)
        private var size = 0

        /** Ein Lauf von [fromX] bis einschließlich [toX] in Zeile [y]: seine vier Ecken bestimmen die Hülle. */
        fun addRun(fromX: Int, toX: Int, y: Int) {
            if (size + 8 > corners.size) corners = corners.copyOf(corners.size * 2)
            val left = fromX.toFloat()
            val right = (toX + 1).toFloat()
            val top = y.toFloat()
            val bottom = (y + 1).toFloat()
            floatArrayOf(left, top, right, top, left, bottom, right, bottom).copyInto(corners, size)
            size += 8
        }

        fun corners(): FloatArray = corners.copyOf(size)
    }

    /**
     * Zusammenhängende Flächen (auch über Ecken) in zwei Durchgängen mit Union-Find.
     * @return Kennung je Bildpunkt (0 = kein Text) und Anzahl der Flächen
     */
    private fun label(probability: FloatArray, width: Int, height: Int, threshold: Float): Pair<IntArray, Int> {
        val labels = IntArray(width * height)
        var parent = IntArray(1024)
        var next = 1
        fun find(x: Int): Int {
            var root = x
            while (parent[root] != root) root = parent[root]
            var node = x
            while (parent[node] != root) {
                val up = parent[node]
                parent[node] = root
                node = up
            }
            return root
        }
        fun union(a: Int, b: Int) {
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) {
                if (ra < rb) parent[rb] = ra else parent[ra] = rb
            }
        }
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (probability[row + x] <= threshold) continue
                var current = 0
                // Schon besuchte Nachbarn: links, oben links, oben, oben rechts.
                if (x > 0) current = merge(current, labels[row + x - 1], ::union)
                if (y > 0) {
                    val up = row - width
                    if (x > 0) current = merge(current, labels[up + x - 1], ::union)
                    current = merge(current, labels[up + x], ::union)
                    if (x + 1 < width) current = merge(current, labels[up + x + 1], ::union)
                }
                if (current == 0) {
                    if (next >= parent.size) parent = parent.copyOf(parent.size * 2)
                    parent[next] = next
                    current = next++
                }
                labels[row + x] = current
            }
        }
        // Zweiter Durchgang: jede Fläche bekommt ihre endgültige, fortlaufende Kennung.
        val compact = IntArray(next)
        var count = 0
        for (i in 1 until next) {
            val root = find(i)
            if (compact[root] == 0) compact[root] = ++count
            compact[i] = compact[root]
        }
        for (i in labels.indices) if (labels[i] != 0) labels[i] = compact[labels[i]]
        return labels to count
    }

    private inline fun merge(current: Int, neighbour: Int, union: (Int, Int) -> Unit): Int = when {
        neighbour == 0 -> current
        current == 0 -> neighbour
        else -> {
            if (current != neighbour) union(current, neighbour)
            current
        }
    }

    private fun collect(labels: IntArray, count: Int, probability: FloatArray, width: Int, height: Int): List<Component> {
        val components = Array(count) { Component() }
        for (y in 0 until height) {
            val row = y * width
            var x = 0
            while (x < width) {
                val label = labels[row + x]
                if (label == 0) {
                    x++
                    continue
                }
                val component = components[label - 1]
                val start = x
                while (x < width && labels[row + x] == label) {
                    component.pixels++
                    component.probabilitySum += probability[row + x]
                    x++
                }
                component.addRun(start, x - 1, y)
            }
        }
        return components.toList()
    }
}
