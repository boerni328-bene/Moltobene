package com.moltobene.app.data

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Zeitangaben in einem Schritt der Zubereitung, z. B. „40 Minuten backen“, „1 ½ Std. schmoren“ oder
 * “simmer for 1 hour 15 minutes” – daraus bietet die Rezeptansicht beim aktuellen Schritt einen Timer an.
 *
 * Ziffern werden in allen Sprachen der Texterkennung gelesen, Zahlwörter („eine Stunde“, “ten minutes”) und
 * Ausdrücke wie „halbe Stunde“ oder “half an hour” auf Deutsch und Englisch. Bei einer Spanne („20–25 Minuten“)
 * gilt die kürzere Zeit: lieber früher nachsehen. Reines Kotlin, per Unit-Test prüfbar.
 */
object StepTimes {

    /**
     * Alle Zeitangaben im Schritt als Sekunden, in der Reihenfolge des Textes und jede Dauer nur einmal.
     * @param language Sprache des Schritts („de“, „en“ …); bestimmt, wie „1.5“ oder „1,5“ gelesen wird
     */
    fun find(step: String, language: String?): List<Int> {
        if (step.isBlank()) return emptyList()
        val found = mutableListOf<Time>()
        fun add(time: Time) {
            if (found.none { it.range.first <= time.range.last && time.range.first <= it.range.last }) found += time
        }
        // Zuerst die festen Ausdrücke, damit „half an hour“ nicht als “an hour” gelesen wird.
        PHRASES.forEach { (regex, seconds) -> regex.findAll(step).forEach { add(Time(seconds.toDouble(), HOUR, it.range)) } }
        COMPACT.findAll(step).forEach { match ->
            val seconds = match.groupValues[1].toDouble() * HOUR + match.groupValues[2].toDouble() * MINUTE
            add(Time(seconds, MINUTE, match.range))
        }
        DURATION.findAll(step).forEach { match ->
            val unit = unitSeconds(match.groupValues[3])
            val from = value(match.groupValues[1], language) ?: return@forEach
            val to = match.groups[2]?.let { value(it.value, language) } ?: from
            add(Time(minOf(from, to) * unit, unit, match.range))
        }
        // „1 Stunde 30 Minuten“, „1 Std., 15 Min.“ oder “1 hour and 20 minutes” ist eine Dauer.
        val joined = mutableListOf<Time>()
        found.sortedBy { it.range.first }.forEach { time ->
            val last = joined.lastOrNull()
            if (last != null && last.unit > time.unit && JOIN.matches(step.substring(last.range.last + 1, time.range.first))) {
                joined[joined.lastIndex] = Time(last.seconds + time.seconds, time.unit, last.range.first..time.range.last)
            } else {
                joined += time
            }
        }
        return joined.map { it.seconds.roundToInt() }.filter { it in 1..MAX_SECONDS }.distinct()
    }

    /** Eine gefundene Zeit; [unit] ist die kleinste Einheit darin (Sekunden je Einheit). */
    private class Time(val seconds: Double, val unit: Int, val range: IntRange)

    private fun value(text: String, language: String?): Double? =
        WORDS[text.lowercase(Locale.ROOT)] ?: AmountScaling.parseValue(text, language)?.takeIf { it > 0 }

    private fun unitSeconds(unit: String): Int {
        val lower = unit.lowercase(Locale.ROOT)
        return when {
            HOUR_UNITS.matches(lower) -> HOUR
            SECOND_UNITS.matches(lower) -> SECOND
            else -> MINUTE
        }
    }

    private const val SECOND = 1
    private const val MINUTE = 60
    private const val HOUR = 3600

    /** Höchstens ein Tag; länger („48 Stunden marinieren“) ist kein Timer. */
    private const val MAX_SECONDS = 24 * HOUR

    /** Zahlwörter auf Deutsch und Englisch, dazu „un/una/une“ aus den anderen Sprachen der Texterkennung. */
    private val WORDS = mapOf(
        "ein" to 1.0, "eine" to 1.0, "einer" to 1.0, "einen" to 1.0, "einem" to 1.0,
        "zwei" to 2.0, "drei" to 3.0, "vier" to 4.0, "fünf" to 5.0, "sechs" to 6.0, "sieben" to 7.0, "acht" to 8.0,
        "neun" to 9.0, "zehn" to 10.0, "elf" to 11.0, "zwölf" to 12.0, "fünfzehn" to 15.0, "zwanzig" to 20.0,
        "dreißig" to 30.0, "vierzig" to 40.0, "fünfundvierzig" to 45.0, "fünfzig" to 50.0, "sechzig" to 60.0,
        "neunzig" to 90.0, "eineinhalb" to 1.5, "anderthalb" to 1.5, "zweieinhalb" to 2.5, "dreieinhalb" to 3.5,
        "a" to 1.0, "an" to 1.0, "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0, "eleven" to 11.0,
        "twelve" to 12.0, "fifteen" to 15.0, "twenty" to 20.0, "thirty" to 30.0, "forty" to 40.0,
        "forty-five" to 45.0, "fifty" to 50.0, "sixty" to 60.0, "ninety" to 90.0,
        "un" to 1.0, "una" to 1.0, "une" to 1.0, "uno" to 1.0,
    )

    private const val HOURS = "stunden|stunde|std|hours|hour|hrs|hr|h|ore|ora|heures|heure|horas|hora"
    private const val MINUTES = "minuten|minute|minutes|minuti|minuto|minutos|mins|min"
    private const val SECONDS =
        "sekunden|sekunde|sek|seconds|second|secs|sec|secondi|secondo|secondes|seconde|segundos|segundo"
    private val HOUR_UNITS = Regex(HOURS)
    private val SECOND_UNITS = Regex(SECONDS)

    private const val SPACE = "[ \\u00A0]"

    /** Ziffern wie in Mengen: „40“, „1,5“, „1.5“, „1 1/2“, „1½“, „½“; Zahlwörter nur vor einem Leerzeichen. */
    private val NUMBER = "(?:\\d+$SPACE+\\d+/\\d+|\\d*$SPACE?[½¼¾⅓⅔]|\\d+/\\d+|\\d+(?:[.,]\\d+)?|" +
        "(?:${WORDS.keys.sortedByDescending { it.length }.joinToString("|")})(?=\\s))"

    /** Nicht mitten in einem Wort oder einer Zahl („Typ 405“, „1,5“). */
    private const val START = "(?<![\\p{L}\\p{N}/])(?<!\\p{N}[.,])"

    /**
     * Zahl oder Spanne mit Einheit: „20 Minuten“, „20–25 Min.“, „1 bis 2 Stunden“, “10 to 12 minutes”.
     * Nicht in Wörtern wie „H-Milch“.
     */
    private val DURATION = Regex(
        "$START($NUMBER)(?:(?:\\s*[-–—]\\s*|\\s+(?:bis|to|à|a|o)\\s+)($NUMBER))?\\s*($HOURS|$MINUTES|$SECONDS)(?![\\p{L}-])",
        RegexOption.IGNORE_CASE,
    )

    /** „1h30“, „1 h 30“ (so u. a. im Französischen), „1h30min“. */
    private val COMPACT = Regex(
        "$START(\\d{1,2})$SPACE?h$SPACE?([0-5]\\d)(?:$SPACE?(?:$MINUTES))?(?![\\p{L}\\p{N}])",
        RegexOption.IGNORE_CASE,
    )

    /** Zwischen Stunden und Minuten bzw. Minuten und Sekunden derselben Dauer. */
    private val JOIN = Regex("^\\.?\\s*,?\\s*(?:(?:und|and|e|et|y)\\s+)?$", RegexOption.IGNORE_CASE)

    private fun phrase(pattern: String) = Regex("(?<![\\p{L}])(?:$pattern)(?![\\p{L}])", RegexOption.IGNORE_CASE)

    private val PHRASES = listOf(
        phrase("(?:one|1)\\s+and\\s+a\\s+half\\s+hours?|an\\s+hour\\s+and\\s+a\\s+half") to 90 * MINUTE,
        phrase("three\\s+quarters\\s+of\\s+an\\s+hour|(?:eine[rnm]?\\s+)?dreiviertelstunde") to 45 * MINUTE,
        phrase("half\\s+an\\s+hour|(?:a\\s+)?half[\\s-]+hour|(?:eine[rnm]?\\s+)?halben?\\s+stunde") to 30 * MINUTE,
        phrase("(?:a\\s+)?quarter\\s+(?:of\\s+an\\s+)?hour|(?:eine[rnm]?\\s+)?viertelstunde") to 15 * MINUTE,
    )
}
