package com.moltobene.app.data

import com.moltobene.app.data.RecipeUnits.Kind
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Portionen umrechnen (#56): liest Mengen in einer Zutatenzeile so, wie das Rezept sie schreibt, und rechnet sie
 * mit einem Faktor um. Umgerechnet werden nur
 * - die Menge am Anfang der Zeile („1,5 l Milch“, „1 1/2 cups“, „½ TL“, „2–3 Eier“) und
 * - Mengen mit Gewicht, Volumen oder Packung in Klammern („3 EL (45 g)“, „(etwa 3 lb / 1,4 kg)“, „(½ Würfel)“).
 *
 * Gerundet wird je nach Einheit (#65, [RecipeUnits]): Stück und Packungen auf ½ (unter 1 auf ¼), Gramm und Milliliter
 * ab 100 auf 5, ab 10 auf 1, darunter auf 0,5; alles andere bleibt genau. Gerundete Mengen beginnen mit „≈“.
 * Packungen werden nur als Anzahl umgerechnet, nie in Gramm.
 *
 * Zahlen mitten im Text („Saft von 2 Zitronen“, „Springform (26 cm)“, „Mehl Type 405“) bleiben, wie sie sind.
 * Die gespeicherte Zeile ändert sich nie; das Ergebnis dient nur der Anzeige. Reines Kotlin, per Unit-Test prüfbar.
 */
object AmountScaling {

    /** Zeichen vor gerundeten Mengen. */
    const val APPROXIMATE = "≈ "

    /**
     * Ein Stück der Zeile; [scaled] markiert umgerechnete Mengen, damit die Anzeige sie hervorheben kann,
     * [approximate] die gerundeten, deren Text mit [APPROXIMATE] beginnt (bei „2½-4“ nur die erste Zahl).
     */
    data class Part(val text: String, val scaled: Boolean, val approximate: Boolean = false)

    /**
     * @param factor neue Portionen geteilt durch die Portionen des Rezepts
     * @param language Sprache des Rezepts („de“, „en“ …); bestimmt, ob „1.000“ tausend oder eins ist
     */
    fun scale(line: String, factor: Double, language: String?): List<Part> {
        if (factor == 1.0 || factor <= 0.0 || line.isBlank()) return listOf(Part(line, false))
        val replacements = mutableListOf<Replacement>()
        LEADING.find(line)?.let { match ->
            val rest = line.substring(match.range.last + 1).trimStart()
            // „30 % Fett“ oder „180 °C“ sind keine Mengen.
            if (rest.startsWith("%") || rest.startsWith("°")) return@let
            // Ohne bekannte Einheit zählt die Zutat selbst („3 Eier“, „1 Springform“) – wie Stück.
            val kind = UNIT.find(rest)?.let { RecipeUnits.kindOf(it.value) } ?: Kind.PIECES
            val numbers = listOfNotNull(match.groups[2], match.groups[4]).mapNotNull { group ->
                scaleNumber(group.value, factor, language, kind)?.let { group.range to it }
            }
            // Bei einem Bereich steht „≈“ nur vor der ersten Zahl.
            val approximate = numbers.any { it.second.approximate }
            numbers.forEachIndexed { index, (range, scaled) ->
                replacements += Replacement(range, scaled.text, approximate && index == 0)
            }
        }
        BRACKETS.findAll(line).forEach { bracket ->
            AMOUNT_IN_BRACKETS.findAll(bracket.value).forEach { amount ->
                val number = amount.groups[1] ?: return@forEach
                val kind = RecipeUnits.kindOf(amount.groupValues[2]) ?: Kind.OTHER
                val start = bracket.range.first + number.range.first
                val range = start until start + number.value.length
                if (replacements.none { it.range.first <= range.last && range.first <= it.range.last }) {
                    scaleNumber(number.value, factor, language, kind)?.let { scaled ->
                        // „(etwa 1 Würfel)“: Steht „etwa“ schon da, kommt kein „≈“ dazu.
                        val about = ABOUT_BEFORE.containsMatchIn(bracket.value.substring(0, number.range.first))
                        replacements += Replacement(range, scaled.text, scaled.approximate && !about)
                    }
                }
            }
        }
        if (replacements.isEmpty()) return listOf(Part(line, false))
        val parts = mutableListOf<Part>()
        var position = 0
        replacements.sortedBy { it.range.first }.forEach { replacement ->
            val range = replacement.range
            if (range.first > position) parts += Part(line.substring(position, range.first), false)
            parts += if (replacement.approximate) {
                Part(APPROXIMATE + replacement.text, scaled = true, approximate = true)
            } else {
                Part(replacement.text, scaled = true)
            }
            position = range.last + 1
        }
        if (position < line.length) parts += Part(line.substring(position), false)
        return parts
    }

    /** Die umgerechnete Zeile als Text. */
    fun scaleText(line: String, factor: Double, language: String?): String =
        scale(line, factor, language).joinToString("") { it.text }

    /**
     * Die Zeile für den Screenreader: „≈ 335 g Mehl“ wird mit [about] (z. B. „etwa %1$s“ aus strings.xml) zu
     * „etwa 335 g Mehl“, denn „≈“ liest nicht jeder Screenreader verständlich vor.
     */
    fun spoken(parts: List<Part>, about: (String) -> String): String =
        parts.joinToString("") { if (it.approximate) about(it.text.removePrefix(APPROXIMATE)) else it.text }

    private class Replacement(val range: IntRange, val text: String, val approximate: Boolean)

    private class Scaled(val text: String, val approximate: Boolean)

    /** Wie eine Zahl im Rezept geschrieben war – damit das Ergebnis genauso aussieht. */
    private class Number(val value: Double, val fraction: Boolean, val grouping: Boolean, val decimalSeparator: Char?)

    private fun scaleNumber(text: String, factor: Double, language: String?, kind: Kind): Scaled? {
        val number = parse(text, language) ?: return null
        val exact = number.value * factor
        val rounded = round(exact, kind)
        // Stück schreiben Rezepte als Bruch („1½ Zwiebeln“, „¼ Würfel“) – außer das Rezept schreibt Dezimalzahlen.
        val style = if (kind == Kind.PIECES && number.decimalSeparator == null && !number.grouping) {
            Number(number.value, fraction = true, grouping = false, decimalSeparator = null)
        } else {
            number
        }
        return Scaled(format(rounded, style, language), approximate = abs(rounded - exact) >= INTEGER_TOLERANCE)
    }

    /** Auf übliche Kochmengen runden; nie auf 0, dann bleibt die genaue Menge. */
    private fun round(value: Double, kind: Kind): Double {
        val step = when (kind) {
            Kind.PIECES -> if (value < 1) QUARTER else HALF
            Kind.GRAMS -> when {
                value >= 100 -> 5.0
                value >= 10 -> 1.0
                else -> HALF
            }
            Kind.OTHER -> return value
        }
        val rounded = Math.round(value / step) * step
        return if (rounded > 0) rounded else value
    }

    /** Liest „1.000“, „1,5“, „1 1/2“, „1½“, „½“ oder „3/4“; null, wenn es keine Zahl ist. */
    internal fun parseValue(text: String, language: String?): Double? = parse(text, language)?.value

    private fun parse(text: String, language: String?): Number? {
        val token = text.replace(' ', ' ').trim()
        MIXED.matchEntire(token)?.let { match ->
            val fraction = fraction(match.groupValues[2], match.groupValues[3]) ?: return null
            return Number(match.groupValues[1].toDouble() + fraction, fraction = true, grouping = false, decimalSeparator = null)
        }
        WITH_GLYPH.matchEntire(token)?.let { match ->
            val whole = match.groupValues[1].ifEmpty { "0" }.toDouble()
            return Number(whole + GLYPHS.getValue(match.groupValues[2][0]), fraction = true, grouping = false, decimalSeparator = null)
        }
        SIMPLE_FRACTION.matchEntire(token)?.let { match ->
            val fraction = fraction(match.groupValues[1], match.groupValues[2]) ?: return null
            return Number(fraction, fraction = true, grouping = false, decimalSeparator = null)
        }
        val match = DECIMAL.matchEntire(token) ?: return null
        val whole = match.groupValues[1]
        val separator = match.groupValues[2].firstOrNull() ?: return Number(whole.toDouble(), false, false, null)
        val digits = match.groupValues[3]
        // „1.000 g“ ist im Deutschen tausend Gramm, „1,000 g“ im Englischen; „0,250 kg“ bleibt ein Viertel.
        val thousands = digits.length == 3 && whole != "0" && when (language) {
            "en" -> separator == ','
            "de", "it", "fr", "es" -> separator == '.'
            else -> true
        }
        return if (thousands) {
            Number((whole + digits).toDouble(), fraction = false, grouping = true, decimalSeparator = null)
        } else {
            Number("$whole.$digits".toDouble(), fraction = false, grouping = false, decimalSeparator = separator)
        }
    }

    private fun fraction(numerator: String, denominator: String): Double? {
        val bottom = denominator.toDouble()
        return if (bottom == 0.0) null else numerator.toDouble() / bottom
    }

    /** Schreibt den neuen Wert wie das Original: Bruch bleibt Bruch, Komma bleibt Komma, Tausenderpunkt bleibt. */
    private fun format(value: Double, original: Number, language: String?): String {
        val decimalSeparator = original.decimalSeparator ?: if (language == "en") '.' else ','
        val nearest = value.roundToLong()
        if (abs(value - nearest) < INTEGER_TOLERANCE) return integer(nearest, original.grouping, language)
        if (original.fraction && value < FRACTION_LIMIT) {
            val whole = floor(value).toLong()
            val glyph = NICE_FRACTIONS.entries.firstOrNull { abs(value - whole - it.key) < FRACTION_TOLERANCE }?.value
            if (glyph != null) return if (whole == 0L) "$glyph" else "$whole$glyph"
        }
        val decimals = when {
            value >= 100 -> 0
            value >= 10 -> 1
            value >= 0.1 -> 2
            else -> 3
        }
        if (decimals == 0) return integer(nearest, original.grouping, language)
        val text = "%.${decimals}f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')
        return text.replace('.', decimalSeparator)
    }

    private fun integer(value: Long, grouping: Boolean, language: String?): String {
        val digits = value.toString()
        if (!grouping || digits.length <= 3) return digits
        val separator = if (language == "en") ',' else '.'
        return digits.reversed().chunked(3).joinToString(separator.toString()).reversed()
    }

    private const val INTEGER_TOLERANCE = 0.005
    private const val HALF = 0.5
    private const val QUARTER = 0.25
    private const val FRACTION_TOLERANCE = 0.01
    private const val FRACTION_LIMIT = 20.0

    private val GLYPHS = mapOf(
        '½' to 1.0 / 2, '¼' to 1.0 / 4, '¾' to 3.0 / 4, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3,
        '⅛' to 1.0 / 8, '⅜' to 3.0 / 8, '⅝' to 5.0 / 8, '⅞' to 7.0 / 8,
    )

    /** Brüche, die beim Kochen üblich sind – andere Werte werden als Dezimalzahl geschrieben. */
    private val NICE_FRACTIONS = listOf('¼', '⅓', '½', '⅔', '¾').associateBy { GLYPHS.getValue(it) }

    private const val GLYPH_CLASS = "½¼¾⅓⅔⅛⅜⅝⅞"
    private const val SPACE = "[ \\u00A0]"
    private const val NUMBER =
        "(?:\\d+$SPACE+\\d+/\\d+|\\d*$SPACE?[$GLYPH_CLASS]|\\d+/\\d+|\\d+(?:[.,]\\d+)?)"

    private val MIXED = Regex("^(\\d+) +(\\d+)/(\\d+)$")
    private val WITH_GLYPH = Regex("^(\\d*) ?([$GLYPH_CLASS])$")
    private val SIMPLE_FRACTION = Regex("^(\\d+)/(\\d+)$")
    private val DECIMAL = Regex("^(\\d+)(?:([.,])(\\d+))?$")

    /** Menge am Zeilenanfang, auch als Bereich: „2-3“, „2 – 3“, „1 bis 2“, “1 to 2”, „1 à 2“. */
    private val LEADING = Regex(
        "^(\\s*)($NUMBER)(?:(\\s*[-–—]\\s*|\\s+(?:bis|to|à|a|o)\\s+)($NUMBER))?(?![\\d/])",
        RegexOption.IGNORE_CASE,
    )

    private val BRACKETS = Regex("\\([^()]*\\)")

    /** Die Einheit nach der Menge: „g“, „EL“, „Würfel“, „c.à.s.“. */
    private val UNIT = Regex("^\\p{L}[\\p{L}.]*")

    /** Steht davor schon „etwa“, „ca.“ oder “about”? */
    private val ABOUT_BEFORE = Regex(
        "(?:ca\\.|circa|etwa|ungefähr|about|approx\\.?|approximately|around|environ|aproximadamente|~)\\s*$",
        RegexOption.IGNORE_CASE,
    )

    /** Einheiten in Klammern, längere zuerst („Stk.“ vor „Stk“, „Dosen“ vor „Do.“). */
    private val BRACKET_UNITS = (
        listOf("kg", "mg", "gr", "g", "ml", "cl", "dl", "l", "oz", "lbs", "lb", "cups", "cup", "tbsp", "tsp", "el", "tl", "gramm", "liter") +
            RecipeUnits.PIECES
        )
        .distinctBy { it.lowercase() }
        .sortedByDescending { it.length }
        .joinToString("|") { Regex.escape(it) }

    /**
     * Gewicht, Volumen und Packungen („½ Würfel“, „1 Päckchen“, “1 packet”); Längen wie „26 cm“ oder „1 inch“ werden
     * nicht umgerechnet.
     */
    private val AMOUNT_IN_BRACKETS = Regex(
        "(?<![\\p{L}\\d.,/])($NUMBER)$SPACE?($BRACKET_UNITS)(?![\\p{L}])",
        RegexOption.IGNORE_CASE,
    )
}
