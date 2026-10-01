package com.moltobene.app.data.ocr

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Rechteck im Bild (rechts und unten ausschließlich). */
data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** Abschnitt einer Seite zwischen zwei Trennlinien. */
sealed interface LayoutBand {
    /** Gewöhnlicher Text, wird als Ganzes gelesen. */
    data class Text(val box: Box) : LayoutBand

    /**
     * Tabellenzeile: links z. B. die Zutat (auch zweizeilig), rechts – falls vorhanden – die Menge.
     * [band] ist der ganze Bereich zwischen den beiden Trennlinien.
     */
    data class Row(val left: Box, val right: Box?, val band: Box) : LayoutBand
}

/**
 * Erkennt Tabellen mit Trennlinien, wie sie viele Rezept-Seiten und -Apps für Zutaten verwenden
 * (links die Zutat, rechts die Menge, dazwischen dünne Linien). Die Texterkennung liest solche Tabellen
 * sonst spaltenweise und verwechselt Mengen, die zwischen zwei Zeilen stehen. Mit den Linien als
 * Zeilengrenzen wird jede Tabellenzeile – und darin Zutat und Menge – einzeln gelesen.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object PageLayout {

    /** So weit muss eine Trennlinie vom Hintergrund abweichen (auch hellgraue Linien zählen). */
    private const val RULE_DIFF = 12

    /** Ab hier zählt ein Bildpunkt als Schrift. */
    private const val INK_DIFF = 60

    /** Eine Tabellenzeile hat höchstens so viele Textzeilen. */
    private const val MAX_ROW_LINES = 3

    /**
     * Liefert die Abschnitte der Seite oder null, wenn sie keine Tabelle mit mindestens zwei
     * waagrechten Trennlinien enthält – dann wird die Seite wie bisher als Ganzes gelesen.
     */
    fun analyze(image: GrayImage): List<LayoutBand>? {
        val width = image.width
        val height = image.height
        if (width < 50 || height < 50) return null
        val background = background(image)
        val rules = findRules(image, background)
        if (rules.size < 2) return null

        val bands = mutableListOf<LayoutBand>()
        var top = 0
        val limits = rules.map { it.first to it.last + 1 } + (height to height)
        limits.forEachIndexed { index, (ruleTop, ruleBottom) ->
            val between = index in 1 until limits.lastIndex
            band(image, background, top, ruleTop, between)?.let { bands += it }
            top = ruleBottom
        }
        return bands
    }

    /**
     * Ausschnitt, weich vergrößert auf etwa [targetHeight] Bildpunkte und mit Rand – kurze Angaben
     * wie „3 EL“ liest Tesseract so oft sicherer.
     */
    fun cropScaled(image: GrayImage, box: Box, targetHeight: Int = 64, border: Int = 20): GrayImage {
        val factor = max(1.0, targetHeight.toDouble() / max(1, box.height))
        val scaledWidth = (box.width * factor).roundToInt().coerceAtLeast(1)
        val scaledHeight = (box.height * factor).roundToInt().coerceAtLeast(1)
        val outWidth = scaledWidth + 2 * border
        val outHeight = scaledHeight + 2 * border
        val background = background(image).toByte()
        val pixels = ByteArray(outWidth * outHeight) { background }
        for (y in 0 until scaledHeight) {
            val sourceY = (box.top + (y + 0.5) / factor - 0.5).coerceIn(box.top.toDouble(), (box.bottom - 1).toDouble())
            val y0 = sourceY.toInt()
            val y1 = min(y0 + 1, box.bottom - 1)
            val fy = sourceY - y0
            for (x in 0 until scaledWidth) {
                val sourceX = (box.left + (x + 0.5) / factor - 0.5).coerceIn(box.left.toDouble(), (box.right - 1).toDouble())
                val x0 = sourceX.toInt()
                val x1 = min(x0 + 1, box.right - 1)
                val fx = sourceX - x0
                val top = gray(image, x0, y0) * (1 - fx) + gray(image, x1, y0) * fx
                val bottom = gray(image, x0, y1) * (1 - fx) + gray(image, x1, y1) * fx
                pixels[(y + border) * outWidth + x + border] = (top * (1 - fy) + bottom * fy).roundToInt().toByte()
            }
        }
        return GrayImage(outWidth, outHeight, pixels)
    }

    /** Bereich zum direkten Lesen einer Menge: die ganze Höhe der Tabellenzeile und etwas Rand zur Seite. */
    fun amountArea(image: GrayImage, row: LayoutBand.Row): Box? {
        val right = row.right ?: return null
        val margin = image.width * 3 / 100
        return Box(
            max(0, right.left - margin),
            min(right.top, row.band.top + 2),
            min(image.width, right.right + margin),
            max(right.bottom, row.band.bottom - 1),
        )
    }

    private fun gray(image: GrayImage, x: Int, y: Int): Int = image.pixels[y * image.width + x].toInt() and 0xFF

    /** Häufigster Grauwert (der Hintergrund) aus einer Stichprobe. */
    private fun background(image: GrayImage): Int {
        val counts = IntArray(256)
        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                counts[gray(image, x, y)]++
                x += 5
            }
            y += 5
        }
        return counts.indices.maxBy { counts[it] }
    }

    /** Waagrechte, dünne Linien über mindestens gut ein Drittel der Breite. */
    private fun findRules(image: GrayImage, background: Int): List<IntRange> {
        val minLength = image.width * 35 / 100
        val maxThickness = max(4, image.height / 400)
        val rules = mutableListOf<IntRange>()
        var start = -1
        for (y in 0..image.height) {
            val isRule = y < image.height && longestRun(image, y, background) >= minLength
            if (isRule && start < 0) start = y
            if (!isRule && start >= 0) {
                if (y - start <= maxThickness) rules += start until y
                start = -1
            }
        }
        return rules
    }

    /** Längste zusammenhängende Strecke in Zeile [y], die sich vom Hintergrund abhebt (Lücken bis 2 px zählen mit). */
    private fun longestRun(image: GrayImage, y: Int, background: Int): Int {
        var best = 0
        var current = 0
        var gap = 0
        for (x in 0 until image.width) {
            if (abs(gray(image, x, y) - background) >= RULE_DIFF) {
                current += 1 + gap
                gap = 0
                if (current > best) best = current
            } else if (current > 0 && gap < 2) {
                gap++
            } else {
                current = 0
                gap = 0
            }
        }
        return best
    }

    private fun band(image: GrayImage, background: Int, top: Int, bottom: Int, between: Boolean): LayoutBand? {
        val lines = inkLines(image, background, top, bottom)
        if (lines.isEmpty()) return null
        val inkTop = lines.first().first
        val inkBottom = lines.last().last + 1
        val padding = max(2, (inkBottom - inkTop) / 10)
        val boxTop = max(top, inkTop - padding)
        val boxBottom = min(bottom, inkBottom + padding)
        if (!between || lines.size > MAX_ROW_LINES) return LayoutBand.Text(Box(0, top, image.width, bottom))

        val columns = BooleanArray(image.width) { x -> (inkTop until inkBottom).any { y -> isInk(image, x, y, background) } }
        val first = columns.indexOfFirst { it }
        val last = columns.indexOfLast { it }
        if (first < 0) return null
        // Breiteste Lücke zwischen Zutat und Menge; rechts davon darf nur ein schmaler Teil stehen.
        var gapStart = -1
        var gapWidth = 0
        var run = 0
        for (x in first..last) {
            if (!columns[x]) {
                run++
            } else {
                if (run > gapWidth) {
                    gapWidth = run
                    gapStart = x - run
                }
                run = 0
            }
        }
        val rightStart = gapStart + gapWidth
        val isTwoColumns = gapStart > 0 && gapWidth >= image.width * 5 / 100 && (last - rightStart) <= (last - first) * 45 / 100
        val band = Box(0, top, image.width, bottom)
        if (!isTwoColumns) {
            return LayoutBand.Row(Box(max(0, first - padding), boxTop, min(image.width, last + 1 + padding), boxBottom), null, band)
        }
        return LayoutBand.Row(
            left = Box(max(0, first - padding), boxTop, gapStart + padding, boxBottom),
            right = Box(rightStart - padding, boxTop, min(image.width, last + 1 + padding), boxBottom),
            band = band,
        )
    }

    private fun isInk(image: GrayImage, x: Int, y: Int, background: Int): Boolean =
        abs(gray(image, x, y) - background) >= INK_DIFF

    /** Zeilenbereiche mit Schrift zwischen [top] und [bottom]. */
    private fun inkLines(image: GrayImage, background: Int, top: Int, bottom: Int): List<IntRange> {
        val lines = mutableListOf<IntRange>()
        var start = -1
        for (y in top..bottom) {
            val hasInk = y < bottom && (0 until image.width).any { x -> isInk(image, x, y, background) }
            if (hasInk && start < 0) start = y
            if (!hasInk && start >= 0) {
                lines += start until y
                start = -1
            }
        }
        return lines
    }
}

/** Setzt eine Tabellenzeile zu einer Zutat zusammen: Menge nach vorne, mehrzeilige Zutat in eine Zeile. */
object RowText {

    private val AMOUNT = Regex("\\d|[½¼¾⅓⅔⅛]|^(${AmountText.AMOUNT_WORDS})$", RegexOption.IGNORE_CASE)

    fun compose(left: String, right: String?): String {
        val name = joinLines(left)
        val amount = right?.let { AmountText.clean(joinLines(it)) }.orEmpty()
        return when {
            amount.isEmpty() -> name
            name.isEmpty() -> amount
            AMOUNT.containsMatchIn(amount) -> "${AmountText.normalize(amount)} $name"
            else -> "$name $amount"
        }
    }

    private fun joinLines(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val result = StringBuilder()
        for (line in lines) {
            if (result.isEmpty()) {
                result.append(line)
            } else if (result.endsWith("-") && line.first().isLowerCase()) {
                result.setLength(result.length - 1)
                result.append(line)
            } else {
                result.append(' ').append(line)
            }
        }
        return result.toString()
    }
}
