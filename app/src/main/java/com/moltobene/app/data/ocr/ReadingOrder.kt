package com.moltobene.app.data.ocr

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Eine gelesene Zeile mit ihrer Lage auf der Seite. */
data class TextLine(val box: TextBox, val text: String)

/**
 * Bringt die gelesenen Zeilen einer Seite in Lesereihenfolge und fasst sie zu Abschnitten zusammen
 * (XY-Schnitt): Die Seite wird an der breitesten freien Fuge geteilt – waagrecht zwischen Abschnitten, senkrecht
 * zwischen Spalten –, und das so lange, bis kein Abschnitt mehr eine Fuge hat. So kommt bei zweispaltigen
 * Kochbuchseiten erst die ganze Zutaten-Spalte, dann die Zubereitung. Eine Tabelle (links die Zutat, rechts
 * kurz die Menge) wird nicht in Spalten zerlegt, sondern Zeile für Zeile gelesen; die Menge kommt nach vorn.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object ReadingOrder {

    /** Spalten brauchen eine Fuge von mindestens so vielen Zeilenhöhen. */
    private const val COLUMN_GAP = 1.2f

    /** Abschnitte brauchen eine Lücke von mindestens so vielen Zeilenhöhen. */
    private const val SECTION_GAP = 0.8f

    /** Rechte Seite einer Tabelle: höchstens so viele Zeilenhöhen breit (Mengen sind kurz). */
    private const val TABLE_CELL_WIDTH = 6f

    /** So weit (in Zeilenhöhen) darf eine Zutat links von ihrer Menge rechts entfernt sein. */
    private const val TABLE_ROW_DISTANCE = 1.2f

    /** Kleinste Lücke (in Zeilenhöhen) zwischen einer Überschrift und Spalten darunter. */
    private const val HEADER_GAP = 0.25f

    /** Ab diesem Abstand (in Zeilenhöhen) beginnt ein neuer Absatz. */
    private const val PARAGRAPH_GAP = 0.4f

    /** Echte Spalten haben auf jeder Seite mindestens so viele Zeilen. */
    private const val MIN_COLUMN_LINES = 3

    private class Item(val x0: Float, val x1: Float, val y0: Float, val y1: Float, val height: Float, val text: String) {
        val centerX get() = (x0 + x1) / 2
        val centerY get() = (y0 + y1) / 2
    }

    /** Abschnitte in Lesereihenfolge, jeder als Text mit einer Zeile je Textzeile. */
    fun blocks(lines: List<TextLine>): List<String> {
        if (lines.isEmpty()) return emptyList()
        // Leicht schräge Fotos: alles um die mittlere Schriftrichtung zurückdrehen.
        val angle = lines.map { it.box.angle }.sorted()[lines.size / 2]
        val c = cos(-angle)
        val s = sin(-angle)
        val items = lines.map { line ->
            val box = line.box
            val x = box.centerX * c - box.centerY * s
            val y = box.centerX * s + box.centerY * c
            Item(x - box.width / 2, x + box.width / 2, y - box.height / 2, y + box.height / 2, box.height, line.text)
        }
        val unit = items.map { it.height }.sorted()[items.size / 2].coerceAtLeast(1f)
        val blocks = mutableListOf<String>()
        cut(items, unit, blocks)
        return blocks.filter { it.isNotBlank() }
    }

    /** Ganzer Text der Seite: Abschnitte durch eine Leerzeile getrennt. */
    fun text(lines: List<TextLine>): String = blocks(lines).joinToString("\n\n")

    private fun cut(items: List<Item>, unit: Float, blocks: MutableList<String>) {
        if (items.size <= 1) {
            if (items.isNotEmpty()) blocks += rows(items, unit)
            return
        }
        val horizontal = widestGap(items.map { it.y0 to it.y1 })
        val vertical = widestGap(items.map { it.x0 to it.x1 })
        val horizontalSize = horizontal?.let { (it.second - it.first) / unit } ?: 0f
        val verticalSize = vertical?.let { (it.second - it.first) / unit } ?: 0f
        val columns = vertical != null && verticalSize >= COLUMN_GAP && (
            verticalSize >= horizontalSize || isLongGutter(items, (vertical.first + vertical.second) / 2)
            )
        when {
            vertical != null && columns -> {
                val middle = (vertical.first + vertical.second) / 2
                if (isTable(items, middle, unit)) {
                    // Eine Tabelle wird nicht weiter zerlegt, sonst stünden Zutat und Menge getrennt.
                    blocks += table(items, middle, unit)
                } else {
                    cut(items.filter { it.centerX < middle }, unit, blocks)
                    cut(items.filter { it.centerX >= middle }, unit, blocks)
                }
            }
            horizontal != null && horizontalSize >= SECTION_GAP -> {
                val middle = (horizontal.first + horizontal.second) / 2
                cut(items.filter { it.centerY < middle }, unit, blocks)
                cut(items.filter { it.centerY >= middle }, unit, blocks)
            }
            else -> {
                val header = headerCut(items, unit)
                if (header != null) {
                    cut(items.filter { it.centerY < header }, unit, blocks)
                    cut(items.filter { it.centerY >= header }, unit, blocks)
                } else {
                    blocks += rows(items, unit)
                }
            }
        }
    }

    /** Eine Spaltenfuge mit mehreren Zeilen auf beiden Seiten geht vor einer Lücke zwischen Abschnitten. */
    private fun isLongGutter(items: List<Item>, middle: Float): Boolean =
        items.count { it.centerX < middle } >= MIN_COLUMN_LINES && items.count { it.centerX >= middle } >= MIN_COLUMN_LINES

    /**
     * Überschrift über zwei Spalten: Reicht der Titel über die Fuge, gibt es oben keine Spaltenfuge, und der
     * Abstand zum Rest ist oft klein. Gesucht wird deshalb die oberste kleine Lücke, unter der eine Spaltenfuge liegt.
     */
    private fun headerCut(items: List<Item>, unit: Float): Float? {
        for ((start, end) in gaps(items.map { it.y0 to it.y1 })) {
            if ((end - start) / unit < HEADER_GAP) continue
            val middle = (start + end) / 2
            val below = items.filter { it.centerY >= middle }
            if (below.size < 2 * MIN_COLUMN_LINES) return null
            val gutter = widestGap(below.map { it.x0 to it.x1 }) ?: continue
            if ((gutter.second - gutter.first) / unit < COLUMN_GAP) continue
            val gutterMiddle = (gutter.first + gutter.second) / 2
            if (isLongGutter(below, gutterMiddle) && !isTable(below, gutterMiddle, unit)) return middle
        }
        return null
    }

    /** Breiteste Lücke zwischen den Strecken [spans] (Anfang, Ende); null, wenn sie sich alle überlappen. */
    private fun widestGap(spans: List<Pair<Float, Float>>): Pair<Float, Float>? =
        gaps(spans).maxByOrNull { it.second - it.first }

    /** Alle Lücken zwischen den Strecken [spans], von vorn nach hinten. */
    private fun gaps(spans: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        if (spans.isEmpty()) return emptyList()
        val sorted = spans.sortedBy { it.first }
        var reach = sorted.first().second
        val result = mutableListOf<Pair<Float, Float>>()
        for ((start, end) in sorted.drop(1)) {
            if (start > reach) result += reach to start
            if (end > reach) reach = end
        }
        return result
    }

    /** Stehen rechts der Fuge fast nur kurze Angaben, jede neben einer Zeile links, ist es eine Tabelle. */
    private fun isTable(items: List<Item>, middle: Float, unit: Float): Boolean {
        val left = items.filter { it.centerX < middle }
        val right = items.filter { it.centerX >= middle }
        if (right.isEmpty()) return false
        // Neben einer Zeile links: Die Zeilen überlappen sich in der Höhe (auch bei zweizeiligen Zutaten).
        val paired = right.count { r -> left.any { l -> r.y0 < l.y1 && r.y1 > l.y0 } }
        val short = right.count { it.x1 - it.x0 < TABLE_CELL_WIDTH * unit }
        return paired >= 0.7f * right.size && short >= 0.7f * right.size
    }

    /**
     * Tabelle: Jede Angabe rechts bekommt die Zeilen links neben sich (nächste Mitte, höchstens
     * [TABLE_ROW_DISTANCE] Zeilenhöhen entfernt) – so bleiben zweizeilige Zutaten bei ihrer Menge.
     * Zeilen links ohne Angabe (Titel, „Für die Fülle:“, Zubereitung) bleiben für sich.
     */
    private fun table(items: List<Item>, middle: Float, unit: Float): String {
        val rights = items.filter { it.centerX >= middle }
        val groups = rights.associateWith { mutableListOf<Item>() }
        val rows = mutableListOf<Pair<Float, String>>()
        for (left in items.filter { it.centerX < middle }.sortedBy { it.centerY }) {
            val nearest = rights.minBy { abs(it.centerY - left.centerY) }
            if (abs(nearest.centerY - left.centerY) <= TABLE_ROW_DISTANCE * unit) {
                groups.getValue(nearest) += left
            } else {
                rows += left.y0 to left.text
            }
        }
        for ((right, lefts) in groups) {
            val top = (lefts.map { it.y0 } + right.y0).min()
            rows += top to RowText.compose(lefts.joinToString("\n") { it.text }, right.text)
        }
        return rows.sortedBy { it.first }.joinToString("\n") { it.second }
    }

    /**
     * Zeilen eines Abschnitts von oben nach unten; was nebeneinander steht, wird eine Zeile. Ein deutlich
     * größerer Abstand (Absatz) wird zur Leerzeile – so bleiben getrennte Schritte der Zubereitung getrennt.
     */
    private fun rows(items: List<Item>, unit: Float): String {
        val rows = mutableListOf<MutableList<Item>>()
        for (item in items.sortedBy { it.centerY }) {
            val last = rows.lastOrNull()?.last()
            if (last != null && abs(item.centerY - last.centerY) < 0.5f * min(item.height, last.height)) {
                rows.last() += item
            } else {
                rows += mutableListOf(item)
            }
        }
        val text = StringBuilder()
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                val gap = row.minOf { it.y0 } - rows[index - 1].maxOf { it.y1 }
                text.append(if (gap >= PARAGRAPH_GAP * unit) "\n\n" else "\n")
            }
            text.append(row.sortedBy { it.x0 }.joinToString(" ") { it.text })
        }
        return text.toString()
    }
}

/** Eine Tabellenzeile wird zur Zutat wie getippt: „Hackfleisch / gemischt | 500 q“ → „500 g Hackfleisch gemischt“. */
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
