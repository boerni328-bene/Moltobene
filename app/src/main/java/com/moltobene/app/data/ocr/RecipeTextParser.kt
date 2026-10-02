package com.moltobene.app.data.ocr

import java.text.Normalizer

/**
 * Ergebnis der Aufteilung: Zutaten im Format des Eingabefelds (Zwischenüberschriften enden mit „:“).
 * [pageNumber] ist die Seitenzahl oben oder unten auf der ersten Seite – ein Vorschlag für die Quelle (#40).
 */
data class ParsedRecipe(
    val title: String?,
    val servings: Int?,
    val servingsUnit: String?,
    val ingredients: List<String>,
    val steps: List<String>,
    val pageNumber: String? = null,
)

/**
 * Teilt erkannten Text (Texterkennung, später auch geteilten Text) in Titel, Portionen, Zutaten und
 * Zubereitung auf. Kennt Überschriften auf Deutsch, Englisch, Italienisch, Französisch und Spanisch;
 * ohne Überschriften wird nach dem Aussehen der Zeilen entschieden. Das Ergebnis ist immer nur ein
 * Vorschlag – der vollständige Text bleibt zusätzlich erhalten. Reines Kotlin, per Unit-Test prüfbar.
 */
object RecipeTextParser {

    // Überschriften werden ohne Akzente verglichen (siehe [isIngredientHeading]): Mit einem falschen Sprachpaket
    // gelesen, wird aus „Elaboración“ z. B. „Elaboraciön“.
    private val INGREDIENT_HEADING = Regex(
        "^(zutaten|ingredients?|ingredienti|ingredientes|einkaufsliste)\\b.{0,40}$",
        RegexOption.IGNORE_CASE,
    )
    private val STEPS_HEADING = Regex(
        "^(zubereitung|anleitung|so geht'?s|so wird'?s gemacht|arbeitsschritte|instructions?|directions?|method|" +
            "preparation|steps|preparazione|procedimento|realisation|preparacion|elaboracion|" +
            "instrucciones|modo de preparacion)\\b[^0-9]{0,30}$",
        RegexOption.IGNORE_CASE,
    )
    private val TIME_WORDS = Regex("(zeit|time|tempo|temps|tiempo)", RegexOption.IGNORE_CASE)

    private const val QUANTITY = "(?:\\d+(?:[.,/]\\d+)?|[½¼¾⅓⅔⅛])"
    // (?!\d): Eine Zahl allein ist keine Menge – sonst gälte „47“ als „4“ plus „7“.
    private val STARTS_WITH_QUANTITY = Regex("^$QUANTITY(?!\\d)\\s*(?:-\\s*\\d+)?\\s*\\S")
    private val BULLET = Regex("^[•·▪◦●○■□\\-–*]\\s+")
    private val NUMBERED_STEP = Regex("^(\\d{1,2})[.)]\\s+(\\S.*)$")
    private val SECOND_QUANTITY = Regex("(?<=\\s)$QUANTITY(?:\\s*-\\s*\\d+)?\\s*(?=\\p{L})")
    private val CONNECTORS = setOf(
        "und", "oder", "mit", "von", "zu", "je", "à", "a", "ca.", "circa", "etwa", "x", "bzw.",
        "and", "or", "with", "of", "about", "approx.", "e", "o", "con", "di", "da", "et", "ou", "avec", "de",
        "y", "con", "del", "plus",
    )
    private val CONJUNCTIONS = setOf("und", "oder", "bzw.", "and", "or", "e", "o", "et", "ou", "y")
    private val SUBHEADING = Regex(
        "^(für|for|per|pour|para)\\s+(den|die|das|the|il|lo|la|i|gli|le|les|l'|el|los|las)\\b.{0,30}$",
        RegexOption.IGNORE_CASE,
    )

    private const val PERSON_WORDS =
        "personen|portionen|pers\\.?|people|persons|servings?|portions?|persone|porzioni|personnes|parts|personas|porciones|raciones"
    private const val PIECE_WORDS = "stück|stücke|stk\\.?|pieces?|pezzi|pièces|piezas"
    private val SERVINGS_PATTERNS = listOf(
        Regex("\\b(?:für|fuer|for|per|pour|para)\\s+(\\d{1,3})(?:\\s*-\\s*\\d{1,3})?\\s*($PERSON_WORDS|$PIECE_WORDS)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:serves|makes|ergibt|reicht für|dosi per|rend|rinde)\\s*:?\\s*(\\d{1,3})\\s*($PIECE_WORDS)?", RegexOption.IGNORE_CASE),
        // Nur eine eigene Zeile wie „12 Stück“ – „1 Stk. Zwiebel“ ist eine Zutat.
        Regex("^(\\d{1,3})\\s+($PERSON_WORDS|$PIECE_WORDS)\\s*$", RegexOption.IGNORE_CASE),
        Regex("\\b(?:$PERSON_WORDS)\\s*:\\s*(\\d{1,3})", RegexOption.IGNORE_CASE),
    )
    private val PIECES = Regex("^($PIECE_WORDS)$", RegexOption.IGNORE_CASE)

    /** Werbung auf Rezept-Seiten („Anzeige“) und Zeilen mit Zeichen, die in Rezepten kaum vorkommen. */
    private val AD_MARKERS = Regex("\\b(anzeige|werbung|advertisement|sponsored|pubblicità|publicité|publicidad)\\b", RegexOption.IGNORE_CASE)
    private val ODD_SYMBOLS = Regex("[=\\\\|<>{}\\[\\]~^_]")

    /**
     * @param language Sprache des Textes (z. B. „de“), falls bekannt. Im Deutschen beginnen Zutaten mit
     * einem großen Buchstaben; eine klein beginnende Zeile gehört dann zur Zutat davor.
     */
    fun parse(text: String, language: String? = null): ParsedRecipe {
        val lines = cleanLines(text)
        val content = lines.indices.filter { lines[it].isNotEmpty() }
        if (content.isEmpty()) return EMPTY

        val ingredientHeading = content.firstOrNull { isIngredientHeading(lines[it]) }
        val stepsHeading = content.firstOrNull { isStepsHeading(lines[it]) }

        val found = findServings(lines, content, ingredientHeading, stepsHeading)
        val servings = found?.count
        val servingsUnit = found?.unit
        val servingsLines = setOfNotNull(found?.line)

        val firstHeading = listOfNotNull(ingredientHeading, stepsHeading).minOrNull()
        val titleIndex = content.firstOrNull { index ->
            (firstHeading == null || index < firstHeading) && index !in servingsLines
        }?.takeIf { isTitleCandidate(lines[it]) }
        val title = titleIndex?.let { lines[it] }

        val skip = servingsLines + listOfNotNull(titleIndex, ingredientHeading, stepsHeading)
        fun region(from: Int, to: Int): List<String> =
            (from until to).map { if (it in skip) "" else lines[it] }

        val ingredientLines: List<String>
        val stepLines: List<String>
        when {
            ingredientHeading != null && stepsHeading != null && ingredientHeading < stepsHeading -> {
                ingredientLines = region(ingredientHeading + 1, stepsHeading)
                stepLines = region(stepsHeading + 1, lines.size)
            }
            ingredientHeading != null && stepsHeading != null -> {
                stepLines = region(stepsHeading + 1, ingredientHeading)
                ingredientLines = region(ingredientHeading + 1, lines.size)
            }
            ingredientHeading != null -> {
                val rest = region(ingredientHeading + 1, lines.size)
                val end = rest.indexOfFirst { it.isNotEmpty() && isStepLike(it) }.let { if (it < 0) rest.size else it }
                ingredientLines = rest.subList(0, end)
                stepLines = rest.subList(end, rest.size)
            }
            stepsHeading != null -> {
                ingredientLines = region(0, stepsHeading).filter { it.isEmpty() || !isStepLike(it) }
                stepLines = region(stepsHeading + 1, lines.size)
            }
            else -> {
                val rest = region(0, lines.size)
                var start = rest.indexOfFirst { it.isNotEmpty() && isIngredientLike(it) }
                // Eine Zwischenüberschrift direkt davor („Für die Fülle:“) gehört zu den Zutaten.
                val previous = (start - 1 downTo 0).firstOrNull { rest[it].isNotEmpty() }
                if (start > 0 && previous != null && rest[previous].endsWith(":") && rest[previous].length <= 40) start = previous
                if (start < 0) {
                    ingredientLines = emptyList()
                    stepLines = rest
                } else {
                    val end = (start until rest.size).firstOrNull { rest[it].isNotEmpty() && isStepLike(rest[it]) } ?: rest.size
                    ingredientLines = rest.subList(start, end)
                    stepLines = rest.subList(0, start) + rest.subList(end, rest.size)
                }
            }
        }

        return ParsedRecipe(
            title = title,
            servings = servings,
            servingsUnit = servingsUnit,
            ingredients = formatIngredients(ingredientLines, language),
            steps = formatSteps(stepLines),
        )
    }

    /**
     * Setzt ein Rezept aus den Bereichen von „Bereich auswählen“ zusammen (#39): Bezeichnete Bereiche gehen
     * ohne Raten in ihr Feld, alle Bereiche „Alles“ werden gemeinsam wie bisher mit [parse] aufgeteilt.
     * Ein Bereich „Titel“ geht vor einem gefundenen Titel.
     */
    fun parseParts(parts: List<Pair<AreaKind, String>>, language: String? = null): ParsedRecipe {
        // Jeder Bereich stammt von einer Seite: Seitenzahlen oben und unten werden je Bereich entfernt,
        // sonst blieben sie beim Zusammenfügen mehrerer Seiten mitten im Text stehen.
        val allText = parts.filter { it.first == AreaKind.ALL }.map { withoutPageNumbers(it.second) }.filter { it.isNotEmpty() }
        val whole = if (allText.isEmpty()) EMPTY else parse(allText.joinToString("\n\n"), language)
        val sections = parts.filter { it.first != AreaKind.ALL }.map { (kind, text) -> parseSection(kind, text, language) }
        val servingsFrom = if (whole.servings != null) whole else sections.firstOrNull { it.servings != null }
        return ParsedRecipe(
            title = sections.firstNotNullOfOrNull { it.title } ?: whole.title,
            servings = servingsFrom?.servings,
            servingsUnit = servingsFrom?.servingsUnit,
            ingredients = whole.ingredients + sections.flatMap { it.ingredients },
            steps = whole.steps + sections.flatMap { it.steps },
            pageNumber = parts.firstNotNullOfOrNull { pageNumberOf(it.second) },
        )
    }

    /** Seitenzahl in der ersten oder letzten Zeile, sonst null. */
    internal fun pageNumberOf(text: String): String? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return listOfNotNull(lines.firstOrNull(), lines.lastOrNull()).firstOrNull { PAGE_NUMBER.matches(it) }
    }

    /** Ein bezeichneter Bereich: Es wird nicht geraten, was darin steht, nur aufbereitet und Überschriften weggelassen. */
    internal fun parseSection(kind: AreaKind, text: String, language: String?): ParsedRecipe {
        val lines = cleanLines(text)
        val content = lines.indices.filter { lines[it].isNotEmpty() }
        if (content.isEmpty()) return EMPTY
        fun without(skip: Set<Int>) = lines.mapIndexed { index, line -> if (index in skip) "" else line }
        return when (kind) {
            AreaKind.ALL -> parse(text, language)
            AreaKind.TITLE -> EMPTY.copy(title = content.joinToString(" ") { lines[it] })
            AreaKind.INGREDIENTS -> {
                val heading = content.firstOrNull { isIngredientHeading(lines[it]) }
                val found = findServings(lines, content, heading, stepsHeading = null)
                EMPTY.copy(
                    servings = found?.count,
                    servingsUnit = found?.unit,
                    ingredients = formatIngredients(without(setOfNotNull(heading, found?.line)), language),
                )
            }
            AreaKind.STEPS -> {
                val heading = content.firstOrNull { isStepsHeading(lines[it]) }
                EMPTY.copy(steps = formatSteps(without(setOfNotNull(heading))))
            }
        }
    }

    /** Portionen und – wenn sie in einer eigenen Zeile stehen – diese Zeile, damit sie keine Zutat wird. */
    private class Servings(val count: Int, val unit: String?, val line: Int?)

    /** Portionen: aus einer eigenen Zeile oder aus der Überschrift „Zutaten für 4 Personen“. */
    private fun findServings(lines: List<String>, content: List<Int>, ingredientHeading: Int?, stepsHeading: Int?): Servings? {
        for (index in content) {
            val line = lines[index]
            val isHeading = index == ingredientHeading
            if (!isHeading && (line.length > 40 || index == stepsHeading)) continue
            val match = SERVINGS_PATTERNS.firstNotNullOfOrNull { it.find(line) } ?: continue
            val count = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..999 } ?: continue
            val unit = match.groupValues.getOrNull(2)?.takeIf { PIECES.matches(it) }
            return Servings(count, unit, if (isHeading) null else index)
        }
        return null
    }

    private val EMPTY = ParsedRecipe(null, null, null, emptyList(), emptyList())

    /** Text ohne eine Seitenzahl in der ersten und letzten Zeile. */
    internal fun withoutPageNumbers(text: String): String {
        val lines = text.trim().lines().toMutableList()
        if (lines.firstOrNull()?.trim()?.matches(PAGE_NUMBER) == true) lines.removeAt(0)
        if (lines.lastOrNull()?.trim()?.matches(PAGE_NUMBER) == true) lines.removeAt(lines.lastIndex)
        return lines.joinToString("\n").trim()
    }

    private val PAGE_NUMBER = Regex("\\d{1,3}")

    /** Bereinigt die Zeilen: Ligaturen, Rauschen, Seitenzahlen und Trennstriche am Zeilenende. */
    internal fun cleanLines(text: String): List<String> {
        val raw = text
            .replace("\r", "")
            .replace("ﬁ", "fi").replace("ﬂ", "fl").replace("ﬀ", "ff")
            .replace(' ', ' ').replace('\t', ' ')
            .lines()
            .map { it.replace(Regex(" {2,}"), " ").trim() }
            .map { if (isNoise(it)) "" else it }
            .toMutableList()

        // Seitenzahlen am Anfang oder Ende
        raw.indexOfFirst { it.isNotEmpty() }.takeIf { it >= 0 && PAGE_NUMBER.matches(raw[it]) }?.let { raw[it] = "" }
        raw.indexOfLast { it.isNotEmpty() }.takeIf { it >= 0 && PAGE_NUMBER.matches(raw[it]) }?.let { raw[it] = "" }

        // Trennstriche: „Vanille-“ + „zucker“ → „Vanillezucker“, aber „Salz-“ + „und …“ bleibt getrennt.
        val result = mutableListOf<String>()
        var i = 0
        while (i < raw.size) {
            var line = raw[i]
            while (line.length > 2 && line.endsWith("-") && line[line.length - 2].isLetter()) {
                val next = (i + 1 until raw.size).firstOrNull { raw[it].isNotEmpty() } ?: break
                val nextLine = raw[next]
                if (!nextLine.first().isLowerCase()) break
                val firstWord = nextLine.substringBefore(' ')
                line = if (firstWord.lowercase() in CONJUNCTIONS) "$line $nextLine" else line.dropLast(1) + nextLine
                for (j in i + 1..next) raw[j] = ""
                i = next
            }
            result += line
            i++
        }
        return result
    }

    private fun isNoise(line: String): Boolean {
        if (line.isEmpty()) return true
        if (AD_MARKERS.containsMatchIn(line)) return true
        // Kurze Zeilen mit seltenen Zeichen sind meist Bildreste – außer sie enthalten ein richtiges Wort.
        if (line.length < 30 && ODD_SYMBOLS.containsMatchIn(line) &&
            line.split(Regex("[^\\p{L}]+")).none { it.length >= 5 }
        ) {
            return true
        }
        val visible = line.filterNot { it.isWhitespace() }
        if (visible.length < 2 && !visible.all { it.isDigit() }) return true
        val alnum = visible.count { it.isLetterOrDigit() }
        if (alnum < visible.length * 0.6) return true
        if (Regex("(\\p{L})\\1{4,}").containsMatchIn(line)) return true
        return false
    }

    private fun isIngredientHeading(line: String): Boolean = INGREDIENT_HEADING.matches(withoutAccents(line.trimEnd(':')))

    private fun isStepsHeading(line: String): Boolean {
        val text = line.trimEnd(':').trim()
        return STEPS_HEADING.matches(withoutAccents(text)) && !TIME_WORDS.containsMatchIn(text)
    }

    /** „Préparation“ → „Preparation“, „Elaboraciön“ → „Elaboracion“. */
    private fun withoutAccents(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    private fun isTitleCandidate(line: String): Boolean =
        line.length in 2..70 && !isIngredientLike(line) && !isStepLike(line) && !line.endsWith(":")

    private fun isIngredientLike(line: String): Boolean =
        STARTS_WITH_QUANTITY.containsMatchIn(line) || BULLET.containsMatchIn(line) ||
            AmountText.startsWithAmount(line) ||
            (line.length <= 50 && AmountText.moveTrailingAmountToFront(line) != line)

    private fun isStepLike(line: String): Boolean {
        if (NUMBERED_STEP.matches(line)) return true
        if (isIngredientLike(line) && line.length <= 50) return false
        val words = line.split(' ').size
        return line.length > 50 || (words >= 5 && line.endsWith("."))
    }

    private fun formatIngredients(lines: List<String>, language: String?): List<String> {
        val result = mutableListOf<String>()
        for (rawLine in lines) {
            if (rawLine.isEmpty()) continue
            val line = AmountText.normalize(AmountText.moveTrailingAmountToFront(rawLine.replace(BULLET, "").trim()))
            if (line.isEmpty()) continue
            val previous = result.lastOrNull()
            // Fortsetzung einer umgebrochenen Zeile, z. B. „1 Dose Tomaten,“ + „gehackt“;
            // im Deutschen auch „500 g Hackfleisch“ + „gemischt“ (Zutaten beginnen dort groß).
            val continues = previous != null && !previous.endsWith(":") && (
                previous.endsWith(",") || previous.endsWith("-") ||
                    previous.count { it == '(' } > previous.count { it == ')' } ||
                    (language == "de" && isGermanContinuation(line))
                )
            if (continues) {
                result[result.lastIndex] = "$previous $line"
                continue
            }
            if (line.endsWith(":") || (SUBHEADING.matches(line) && !isIngredientLike(line))) {
                result += line.trimEnd(':').trim() + ":"
                continue
            }
            result += splitColumns(line)
        }
        return result
    }

    /**
     * Deutsch: Eine Zeile ohne Menge, die klein beginnt und kein Hauptwort enthält („gemischt“,
     * „klein“, „getrocknet“), beschreibt die Zutat davor. „frische Kräuter“ bleibt eine eigene Zutat.
     */
    private fun isGermanContinuation(line: String): Boolean =
        line.first().isLowerCase() && !AmountText.startsWithAmount(line) &&
            line.split(' ').none { it.firstOrNull()?.isUpperCase() == true }

    /** Zwei Spalten, die in einer Zeile gelandet sind: „1 cipolla 60 g di parmigiano“ → zwei Zutaten. */
    private fun splitColumns(line: String): List<String> {
        if (!STARTS_WITH_QUANTITY.containsMatchIn(line)) return listOf(line)
        val parts = mutableListOf<String>()
        var rest = line
        while (true) {
            val match = SECOND_QUANTITY.findAll(rest).firstOrNull { candidate ->
                val before = rest.substring(0, candidate.range.first).trim()
                val wordsBefore = before.split(' ')
                wordsBefore.size >= 2 &&
                    wordsBefore.last().lowercase() !in CONNECTORS &&
                    !wordsBefore.last().endsWith(",") &&
                    !before.endsWith("(")
            } ?: break
            parts += rest.substring(0, match.range.first).trim()
            rest = rest.substring(match.range.first).trim()
        }
        parts += rest
        return parts
    }

    private fun formatSteps(lines: List<String>): List<String> {
        val content = lines.filter { it.isNotEmpty() }
        if (content.isEmpty()) return emptyList()
        val steps = mutableListOf<String>()
        val numbered = content.count { NUMBERED_STEP.matches(it) } >= 2
        var current = StringBuilder()

        fun close() {
            val step = current.toString().trim()
            if (step.isNotEmpty()) steps += step
            current = StringBuilder()
        }

        if (numbered) {
            for (line in content) {
                val match = NUMBERED_STEP.matchEntire(line)
                if (match != null) {
                    close()
                    current.append(match.groupValues[2])
                } else {
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(line)
                }
            }
        } else {
            // Leerzeilen der Texterkennung sind unzuverlässig: Ein Schritt endet erst nach einem Satzende.
            for (line in lines) {
                if (line.isEmpty()) {
                    val text = current.trimEnd()
                    if (text.isNotEmpty() && text.last() in ".!?:)…") close()
                } else {
                    if (line.endsWith(":") && line.length <= 40) {
                        close()
                        steps += line
                        continue
                    }
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(line)
                }
            }
        }
        close()
        return steps
    }
}
