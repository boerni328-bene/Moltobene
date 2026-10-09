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

    /**
     * Die Zahlen eines Textes als Werte: „1,5“ = „1.5“ = „1½“ = „1 1/2“, „½“ = „1/2“ = „0,5“ – das Modell schreibt
     * Brüche oft anders als die Vorlage, die Menge bleibt aber dieselbe.
     */
    internal fun numbers(text: String): List<String> =
        NUMBER.findAll(text).map { value(it.value) }.sorted().toList()

    private fun value(token: String): String {
        val number = parse(token) ?: return token
        return "%.3f".format(java.util.Locale.ROOT, number).trimEnd('0').trimEnd('.')
    }

    /** „1½“, „1 1/2“, „1/2“, „½“, „1,5“ → Zahl; null, wenn es keine ist. */
    private fun parse(token: String): Double? {
        MIXED.matchEntire(token)?.let { m ->
            val bottom = m.groupValues[3].toDouble()
            if (bottom == 0.0) return null
            return m.groupValues[1].ifEmpty { "0" }.toDouble() + m.groupValues[2].toDouble() / bottom
        }
        WITH_GLYPH.matchEntire(token)?.let { m -> return m.groupValues[1].ifEmpty { "0" }.toDouble() + GLYPHS.getValue(m.groupValues[2][0]) }
        return token.replace(',', '.').toDoubleOrNull()
    }
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
    private const val GLYPH_CLASS = "½¼¾⅓⅔⅛⅜⅝⅞"
    private val NUMBER = Regex("\\d+[ \\u00A0]\\d+/\\d+|\\d+/\\d+|\\d*[$GLYPH_CLASS]|\\d+(?:[.,]\\d+)*")
    private val MIXED = Regex("(\\d*)[ \\u00A0]?(\\d+)/(\\d+)")
    private val WITH_GLYPH = Regex("(\\d*)([$GLYPH_CLASS])")
    private val GLYPHS = mapOf(
        '½' to 0.5, '¼' to 0.25, '¾' to 0.75, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3,
        '⅛' to 0.125, '⅜' to 0.375, '⅝' to 0.625, '⅞' to 0.875,
    )
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

    // Mehltypen werden nie gleichgesetzt (#64): Type 405, Tipo 00 oder „all-purpose“ haben keine genauen
    // Gegenstücke. „all-purpose flour“ wird deshalb nur „Weizenmehl“, nie eine Type.
    // Ins Englische mit amerikanischen Wörtern (#64): „powdered sugar“, „heavy cream“, „green onions“, „pan“.
    // Back- und Teigbegriffe wie in den festen Begriffen von CLAUDE.md, aber nur, wo das Modell sie nachweislich falsch
    // übersetzt (Vergleich mit und ohne Wörterbuch in `LanguagePackTest`, .github/workflows/sprachpaket.yml). Hefen,
    // „Vorteig“, „Teigkugel“, „Päckchen“, „Gugelhupfform“ und „final dough“ fehlen absichtlich: Eingesetzt verdrehte
    // das Modell sie („instant yet“, „preference“, „Teichkugeln“, „fret pan“, „Hauptsteig“), ohne Wörterbuch übersetzt
    // es sie richtig oder wenigstens verständlich.
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
            "heavy whipping cream" to "Schlagsahne",
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
            "cake pan" to "Backform",
            "baking pan" to "Backform",
            "tart pan" to "Tarteform",
            "Bundt pan" to "Gugelhupfform",
            "muffin pan" to "Muffinblech",
            "muffin tin" to "Muffinblech",
            "sheet pan" to "Backblech",
            "parchment paper" to "Backpapier",
            "convection oven" to "Umluftofen",
            "fan oven" to "Umluftofen",
            "instant yeast" to "Trockenhefe",
            "instant dry yeast" to "Trockenhefe",
            "rapid-rise yeast" to "Trockenhefe",
            "sourdough starter" to "Sauerteig",
            "preferment" to "Vorteig",
            "pre-ferment" to "Vorteig",
            "rising time" to "Gehzeit",
            "proofing time" to "Gehzeit",
            "dough hook" to "Knethaken",
            "stand mixer" to "Küchenmaschine",
            "rolling pin" to "Nudelholz",
            "zest of" to "Abrieb von",
        ),
        "de-en" to entries(
            "Tellerlinsen" to "brown lentils",
            "Ober-/Unterhitze" to "conventional oven",
            "Ober- und Unterhitze" to "conventional oven",
            "Umluft" to "convection oven",
            "Springform" to "springform pan",
            "Kastenform" to "loaf pan",
            "Tarteform" to "tart pan",
            "Muffinblech" to "muffin pan",
            "Muffinform" to "muffin pan",
            "Backform" to "baking pan",
            "Backblech" to "baking sheet",
            "Blech" to "baking sheet",
            "Speisestärke" to "cornstarch",
            "Puderzucker" to "powdered sugar",
            "Backpulver" to "baking powder",
            "Natron" to "baking soda",
            "Schmand" to "sour cream",
            "Schlagsahne" to "heavy cream",
            "Frühlingszwiebeln" to "green onions",
            "Lauchzwiebeln" to "green onions",
            "Tomatenmark" to "tomato paste",
            "Sauerteig" to "sourdough starter",
            "Hauptteig" to "final dough",
            "Teigkugeln" to "dough balls",
            "Teigkugel" to "dough ball",
            "Gehzeit" to "rising time",
            "Knethaken" to "dough hook",
            "Küchenmaschine" to "stand mixer",
            "Nudelholz" to "rolling pin",
            "Bund" to "bunch",
        ),
    )
}
