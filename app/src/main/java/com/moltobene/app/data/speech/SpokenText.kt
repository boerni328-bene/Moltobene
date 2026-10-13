package com.moltobene.app.data.speech

/**
 * Macht Rezepttext zum Vorlesen verständlich (#79): Mengen und Einheiten ausschreiben („2 EL“ → „2 Esslöffel“),
 * Brüche und „≈“ in Worte fassen, Tausenderpunkte und Bereiche lesbar machen und in Sätze teilen. Die gesprochenen
 * Wörter folgen der Sprache des Rezepts, nicht der Oberfläche; deshalb stehen sie hier und nicht in strings.xml.
 * Für andere Sprachen als Deutsch und Englisch wird nur in Sätze geteilt.
 */
object SpokenText {

    private const val MAX_SENTENCE = 200
    private val DOTTED_ABBREVIATIONS = setOf("Msp", "Pck", "Min", "Std", "min", "hr", "hrs", "mins", "tbsp", "tsp", "oz", "lb", "lbs")

    private class Words(
        val about: String,
        val to: String,
        val and: String,
        val half: String,
        val quarter: String,
        val threeQuarters: String,
        val degreesC: String,
        val degreesF: String,
        val degrees: String,
        val step: String,
        val units: Map<String, Pair<String, String>>,
        val abbreviations: List<Pair<Regex, String>>,
    )

    private val GERMAN = Words(
        about = "etwa", to = "bis", and = "und", half = "ein halb", quarter = "ein viertel", threeQuarters = "drei viertel",
        degreesC = "Grad Celsius", degreesF = "Grad Fahrenheit", degrees = "Grad", step = "Schritt",
        units = mapOf(
            "kg" to ("Kilogramm" to "Kilogramm"), "mg" to ("Milligramm" to "Milligramm"), "g" to ("Gramm" to "Gramm"),
            "ml" to ("Milliliter" to "Milliliter"), "cl" to ("Zentiliter" to "Zentiliter"), "dl" to ("Deziliter" to "Deziliter"),
            "l" to ("Liter" to "Liter"), "EL" to ("Esslöffel" to "Esslöffel"), "TL" to ("Teelöffel" to "Teelöffel"),
            "Msp" to ("Messerspitze" to "Messerspitzen"), "Pck" to ("Päckchen" to "Päckchen"),
            "Min" to ("Minute" to "Minuten"), "min" to ("Minute" to "Minuten"), "Std" to ("Stunde" to "Stunden"),
            "h" to ("Stunde" to "Stunden"), "cm" to ("Zentimeter" to "Zentimeter"), "mm" to ("Millimeter" to "Millimeter"),
        ),
        abbreviations = listOf(
            Regex("""\bz\.\s?B\.""") to "zum Beispiel", Regex("""\bca\.""") to "circa", Regex("""\bevtl\.""") to "eventuell",
            Regex("""\bbzw\.""") to "beziehungsweise", Regex("""\bggf\.""") to "gegebenenfalls", Regex("""\busw\.""") to "und so weiter",
        ),
    )

    private val ENGLISH = Words(
        about = "about", to = "to", and = "and", half = "a half", quarter = "a quarter", threeQuarters = "three quarters",
        degreesC = "degrees Celsius", degreesF = "degrees Fahrenheit", degrees = "degrees", step = "Step",
        units = mapOf(
            "kg" to ("kilogram" to "kilograms"), "mg" to ("milligram" to "milligrams"), "g" to ("gram" to "grams"),
            "ml" to ("milliliter" to "milliliters"), "cl" to ("centiliter" to "centiliters"), "dl" to ("deciliter" to "deciliters"),
            "l" to ("liter" to "liters"), "tbsp" to ("tablespoon" to "tablespoons"), "tsp" to ("teaspoon" to "teaspoons"),
            "oz" to ("ounce" to "ounces"), "lb" to ("pound" to "pounds"), "lbs" to ("pound" to "pounds"),
            "min" to ("minute" to "minutes"), "mins" to ("minute" to "minutes"), "hr" to ("hour" to "hours"),
            "hrs" to ("hour" to "hours"), "h" to ("hour" to "hours"), "cm" to ("centimeter" to "centimeters"),
            "mm" to ("millimeter" to "millimeters"),
        ),
        abbreviations = listOf(
            Regex("""\be\.g\.""") to "for example", Regex("""\bi\.e\.""") to "that is", Regex("""\bapprox\.""") to "about",
            Regex("""\betc\.""") to "et cetera", Regex("""\bca\.""") to "about",
        ),
    )

    private fun words(language: String): Words? = when (language.lowercase().take(2)) {
        "de" -> GERMAN
        "en" -> ENGLISH
        else -> null
    }

    /** „etwa“ / „about“ für Mengen mit „≈“; für andere Sprachen das Zeichen selbst. */
    fun about(language: String): String = words(language)?.about ?: "≈"

    /** Der Satz der Hörprobe (#85); er folgt der Sprache der Stimme, nicht der Oberfläche. */
    fun sample(language: String): String = when (language.lowercase().take(2)) {
        "de" -> "So klingt die Stimme beim Vorlesen eines Rezepts. Schritt 1: Die Zwiebeln fein würfeln."
        else -> "This is how the voice sounds when reading a recipe aloud. Step 1: Dice the onions finely."
    }

    /** „Schritt 3“ vor dem Vorlesen eines Schritts; für andere Sprachen nur die Zahl. */
    fun stepLabel(language: String, number: Int): String = "${words(language)?.step ?: ""} $number".trim() + "."

    /** Ein Schritt oder eine Zutat als Sätze, bereit zum Vorlesen. */
    fun sentences(text: String, language: String): List<String> =
        split(prepare(text, language))

    internal fun prepare(text: String, language: String): String {
        val w = words(language) ?: return text.replace(Regex("""[ \t]+"""), " ").trim()
        var s = text.replace(' ', ' ').replace(Regex("""[ \t]+"""), " ").trim()
        for ((pattern, replacement) in w.abbreviations) s = s.replace(pattern, replacement)
        s = s.replace("≈", "${w.about} ")
        s = fractions(s, w)
        // Tausenderzeichen: „1.000“ (deutsch) bzw. „1,000“ (englisch) wird „1000“.
        val grouping = if (w === GERMAN) Regex("""(?<=\d)\.(?=\d{3}(?!\d))""") else Regex("""(?<=\d),(?=\d{3}(?!\d))""")
        s = s.replace(grouping, "")
        s = s.replace(Regex("""(\d)\s?[–-]\s?(?=\d)""")) { "${it.groupValues[1]} ${w.to} " }
        s = s.replace(Regex("""(\d)\s?°\s?C\b""")) { "${it.groupValues[1]} ${w.degreesC}" }
        s = s.replace(Regex("""(\d)\s?°\s?F\b""")) { "${it.groupValues[1]} ${w.degreesF}" }
        s = s.replace(Regex("""(\d)\s?°""")) { "${it.groupValues[1]} ${w.degrees}" }
        val unitNames = w.units.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        val fractionList = listOf(w.half, w.quarter, w.threeQuarters)
        val fractionWords = fractionList.joinToString("|") { Regex.escape(it) }
        val unit = Regex("""(\d+(?:[.,]\d+)?|$fractionWords)\s?($unitNames)(\.?)(?![\p{L}\d])""")
        val before = s
        s = s.replace(unit) { match ->
            val name = match.groupValues[2]
            val (singular, plural) = w.units.getValue(name)
            val number = match.groupValues[1]
            // Der Punkt gehört nur bei Kürzeln wie „Min.“ zum Wort; sonst beendet er den Satz.
            val dot = if (name in DOTTED_ABBREVIATIONS) "" else match.groupValues[3]
            // „ein halb Esslöffel“ ist einzeln, „1 und ein halb Esslöffel“ mehrere.
            val single = number == "1" || (number in fractionList && !before.substring(0, match.range.first).endsWith("${w.and} "))
            "$number ${if (single) singular else plural}$dot"
        }
        return s.replace(Regex(""" {2,}"""), " ").trim()
    }

    private fun fractions(text: String, w: Words): String {
        fun word(glyph: String): String? = when (glyph) {
            "½", "1/2" -> w.half
            "¼", "1/4" -> w.quarter
            "¾", "3/4" -> w.threeQuarters
            else -> null
        }
        // „1½“, „1 ½“ und „1 1/2“: ganze Zahl plus Bruch.
        var s = text.replace(Regex("""(\d+)\s?(½|¼|¾|[13]/[24])(?![\d/])""")) { match ->
            word(match.groupValues[2])?.let { "${match.groupValues[1]} ${w.and} $it" } ?: match.value
        }
        s = s.replace(Regex("""(?<![\d/])(½|¼|¾|1/2|1/4|3/4)(?![\d/])""")) { match -> word(match.value) ?: match.value }
        return s
    }

    private fun split(text: String): List<String> {
        val result = mutableListOf<String>()
        for (line in text.split('\n')) {
            for (sentence in line.split(Regex("""(?<=[.!?…])\s+"""))) {
                result += chunk(sentence.trim())
            }
        }
        return result.filter { it.any(Char::isLetterOrDigit) }
    }

    /** Sehr lange Sätze werden an Kommas und Semikolons geteilt, damit das Vorlesen nicht stockt. */
    private fun chunk(sentence: String): List<String> {
        if (sentence.length <= MAX_SENTENCE) return listOf(sentence)
        val parts = sentence.split(Regex("""(?<=[,;:])\s+"""))
        val chunks = mutableListOf<String>()
        var current = ""
        for (part in parts) {
            current = if (current.isEmpty()) part else if ((current.length + 1 + part.length) <= MAX_SENTENCE) "$current $part" else {
                chunks += current
                part
            }
        }
        if (current.isNotEmpty()) chunks += current
        return chunks
    }
}
