package com.moltobene.app.data.translate

/**
 * Schutzregeln für „Rezept übersetzen“ (#60): Eine maschinelle Übersetzung wird nur übernommen, wenn sie
 * plausibel ist – sonst bleibt die Originalzeile stehen. Aus der Machbarkeitsprobe vom 07.10.2026:
 * - Zahlen müssen erhalten bleiben (Mengen, Zeiten, Temperaturen); „1,5“ und „1.5“ gelten als gleich.
 * - Eine Überschrift mit Doppelpunkt bleibt eine Überschrift („Gremolata:“ wurde einmal zu „- Ich weiß.“).
 * - Viel zu lange Ausgaben und Wiederholungen („der der der …“) sind Unsinn.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object TranslationGuard {

    fun accept(original: String, translated: String): Boolean {
        val source = original.trim()
        val result = translated.trim()
        if (result.isEmpty() || !source.any { it.isLetter() }) return false
        if (numbers(source) != numbers(result)) return false
        if (source.endsWith(":") != result.endsWith(":")) return false
        if (result.length > source.length * MAX_LENGTH_FACTOR + MAX_LENGTH_EXTRA) return false
        return !repeats(result)
    }

    /** Die Übersetzung, wenn sie die Regeln erfüllt, sonst das Original. */
    fun choose(original: String, translated: String): String = if (accept(original, translated)) translated.trim() else original

    internal fun numbers(text: String): List<String> =
        NUMBER.findAll(text).map { it.value.replace(',', '.') }.sorted().toList()

    /** Dasselbe Wort viermal hintereinander oder eine Folge von drei Wörtern dreimal. */
    internal fun repeats(text: String): Boolean {
        val words = text.lowercase().split(WORD_SEPARATOR).filter { it.isNotEmpty() }
        if (words.windowed(SAME_WORD_LIMIT).any { window -> window.all { it == window.first() } }) return true
        val triples = words.windowed(3).map { it.joinToString(" ") }
        return triples.groupingBy { it }.eachCount().values.any { it >= SAME_TRIPLE_LIMIT }
    }

    private const val MAX_LENGTH_FACTOR = 3
    private const val MAX_LENGTH_EXTRA = 20
    private const val SAME_WORD_LIMIT = 4
    private const val SAME_TRIPLE_LIMIT = 3
    private val NUMBER = Regex("\\d+(?:[.,]\\d+)*|[½¼¾⅓⅔⅛⅜⅝⅞]")
    private val WORD_SEPARATOR = Regex("[^\\p{L}\\d]+")
}

/**
 * Küchenwörterbuch für „Rezept übersetzen“ (#60): typische Begriffe, die das Modell falsch übersetzt, werden vor
 * dem Übersetzen durch das Wort der Zielsprache ersetzt. Nur Nomen – bei Verben und Wendungen hat das in der
 * Machbarkeitsprobe geschadet („fork-tender“ → „weich“ ergab „Bis zu zwei Stunden“). Reines Kotlin.
 */
object KitchenGlossary {

    /** Ersetzt bekannte Begriffe; am Zeilenanfang mit großem Anfangsbuchstaben. */
    fun apply(text: String, direction: String): String {
        val entries = ENTRIES[direction] ?: return text
        var result = text
        entries.forEach { (pattern, replacement) ->
            result = pattern.replace(result) { match ->
                if (match.range.first == 0) replacement.replaceFirstChar { it.uppercase() } else replacement
            }
        }
        return result
    }

    private fun entries(vararg pairs: Pair<String, String>): List<Pair<Regex, String>> =
        pairs.sortedByDescending { it.first.length }.map { (term, replacement) ->
            Regex("(?<![\\p{L}])${Regex.escape(term)}(?![\\p{L}])", RegexOption.IGNORE_CASE) to replacement
        }

    private val ENTRIES = mapOf(
        "en-de" to entries(
            "icing sugar" to "Puderzucker",
            "powdered sugar" to "Puderzucker",
            "confectioners' sugar" to "Puderzucker",
            "self-raising flour" to "Mehl mit Backpulver",
            "self-rising flour" to "Mehl mit Backpulver",
            "all-purpose flour" to "Weizenmehl",
            "plain flour" to "Weizenmehl",
            "baking soda" to "Natron",
            "bicarbonate of soda" to "Natron",
            "baking powder" to "Backpulver",
            "cornstarch" to "Speisestärke",
            "cornflour" to "Speisestärke",
            "tomato paste" to "Tomatenmark",
            "tomato purée" to "Tomatenmark",
            "heavy cream" to "Schlagsahne",
            "double cream" to "Schlagsahne",
            "whipping cream" to "Schlagsahne",
            "kosher salt" to "grobes Salz",
            "scallions" to "Frühlingszwiebeln",
            "spring onions" to "Frühlingszwiebeln",
            "green onions" to "Frühlingszwiebeln",
            "cilantro" to "Koriandergrün",
            "eggplant" to "Aubergine",
            "zucchini" to "Zucchini",
            "loaf tin" to "Kastenform",
            "loaf pan" to "Kastenform",
            "springform pan" to "Springform",
            "zest of" to "Abrieb von",
        ),
        "de-en" to entries(
            "Tellerlinsen" to "brown lentils",
            "Ober-/Unterhitze" to "top and bottom heat",
            "Umluft" to "fan oven",
            "Springform" to "springform pan",
            "Speisestärke" to "cornstarch",
            "Puderzucker" to "icing sugar",
            "Backpulver" to "baking powder",
            "Natron" to "baking soda",
            "Schmand" to "sour cream",
            "Schlagsahne" to "whipping cream",
            "Frühlingszwiebeln" to "spring onions",
            "Tomatenmark" to "tomato paste",
            "Lauchzwiebeln" to "spring onions",
            "Bund" to "bunch",
        ),
    )
}
