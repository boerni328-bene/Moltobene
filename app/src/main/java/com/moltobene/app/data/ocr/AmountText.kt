package com.moltobene.app.data.ocr

/**
 * Mengenangaben in erkanntem Text: erkennt sie am Anfang und am Ende einer Zutat und korrigiert
 * typische Lesefehler bei Einheiten („500 q“ → „500 g“, „2 Sik.“ → „2 Stk.“, „1Pr“ → „1 Pr“).
 * Reines Kotlin, per Unit-Test prüfbar.
 */
object AmountText {

    /** Mengen ohne Zahl, z. B. „etwas Salz“. */
    const val AMOUNT_WORDS =
        "etwas|n\\. ?b\\.|nach belieben|nach geschmack|einige|some|a pinch|to taste|q\\. ?b\\.|un po'|un pizzico|" +
            "un peu|une pincée|un poco|una pizca|al gusto"

    private const val NUMBER = "(?:\\d+(?:[.,/]\\d+)?|[½¼¾⅓⅔⅛])"

    /** Bekannte Einheiten (Deutsch, Englisch, Italienisch, Französisch, Spanisch). */
    private val UNITS = listOf(
        "g", "kg", "mg", "ml", "cl", "dl", "l", "EL", "TL", "Msp.", "Pr", "Pr.", "Prise", "Prisen", "Stk.", "Stk", "Stück",
        "Do.", "Dose", "Dosen", "Pck.", "Pkg.", "Päckchen", "Bund", "Becher", "Tasse", "Tassen", "Glas", "Zehe", "Zehen",
        "Scheibe", "Scheiben", "Würfel", "Blatt", "Blätter", "Zweig", "Zweige", "Handvoll",
        "cup", "cups", "tbsp", "tsp", "oz", "lb", "lbs", "pinch", "can", "cans", "clove", "cloves", "slice", "slices",
        "cucchiaio", "cucchiai", "cucchiaino", "cucchiaini", "bicchiere", "pizzico", "spicchio", "spicchi",
        "cuillère", "cuillères", "pincée", "gousse", "gousses", "cucharada", "cucharadas", "cucharadita", "cucharaditas",
        "pizca", "taza", "tazas", "diente", "dientes", "lata", "latas",
    )

    /** Steht davor eines dieser Wörter, ist die Zahl keine Menge („Mehl Type 405“). */
    private val NOT_AN_AMOUNT_BEFORE = setOf("type", "typ", "größe", "gr.", "nr.", "no.", "size", "n.")

    /** Zeichen, die Tesseract in kurzen Angaben häufig verwechselt (gelesen → gemeint). */
    private val CONFUSIONS = mapOf('q' to 'g', 'i' to 't', 'l' to 't', '1' to 'l', '0' to 'o', 'I' to 'l')

    private val LEADING = Regex("^($NUMBER(?:\\s*-\\s*\\d+)?)\\s*([\\p{L}.]+)?", RegexOption.IGNORE_CASE)
    private val LEADING_WORD = Regex("^(?:$AMOUNT_WORDS)\\b", RegexOption.IGNORE_CASE)
    private val TRAILING = Regex(
        "^(.*\\p{L}.*?)[\\s;:]+($NUMBER(?:\\s*-\\s*\\d+)?\\s*[\\p{L}.]{0,12}|$AMOUNT_WORDS)$",
        RegexOption.IGNORE_CASE,
    )

    private val AMOUNT_ONLY = Regex("^$NUMBER(?:\\s*-\\s*\\d+)?\\s*([\\p{L}.]+)?$", RegexOption.IGNORE_CASE)
    private val AMOUNT_WORD_ONLY = Regex("^(?:$AMOUNT_WORDS)$", RegexOption.IGNORE_CASE)

    /** Entfernt Zeichen, die in Mengenangaben nicht vorkommen („1: Pr“ → „1 Pr“). */
    fun clean(text: String): String =
        text.replace(Regex("[^\\p{L}\\d.,/½¼¾⅓⅔⅛\\s-]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Ist der Text eine vollständige, plausible Menge („500 g“, „1 Pr“, „etwas“)? */
    fun isAmount(text: String): Boolean {
        val cleaned = clean(text)
        if (AMOUNT_WORD_ONLY.matches(cleaned)) return true
        val match = AMOUNT_ONLY.matchEntire(cleaned) ?: return false
        val unit = match.groupValues[1]
        return unit.isEmpty() || fixUnit(unit) != null
    }

    /** Beginnt mit einer Menge (Zahl oder Wort wie „etwas“)? */
    fun startsWithAmount(line: String): Boolean =
        Regex("^$NUMBER\\s*(?:-\\s*\\d+)?\\s*\\S").containsMatchIn(line) || LEADING_WORD.containsMatchIn(line)

    /**
     * „Oregano getrocknet 1 Pr“ → „1 Pr Oregano getrocknet“. Nur wenn am Ende wirklich eine Menge steht
     * (Zahl mit bekannter oder keiner Einheit, oder ein Mengenwort).
     */
    fun moveTrailingAmountToFront(line: String): String {
        if (startsWithAmount(line)) return line
        val match = TRAILING.matchEntire(line.trim()) ?: return line
        val name = match.groupValues[1].trim().trimEnd(',', ';', ':')
        val amount = match.groupValues[2].trim()
        if (!LEADING_WORD.containsMatchIn(amount)) {
            val unit = LEADING.find(amount)?.groupValues?.get(2).orEmpty()
            if (unit.isNotEmpty() && fixUnit(unit) == null) return line
            if (unit.isEmpty() && name.substringAfterLast(' ').lowercase() in NOT_AN_AMOUNT_BEFORE) return line
        }
        return "${normalize(amount)} $name"
    }

    /** Leerzeichen zwischen Zahl und Einheit, offensichtliche Lesefehler in der Einheit korrigieren. */
    fun normalize(text: String): String {
        val match = LEADING.find(text) ?: return text
        val number = match.groupValues[1]
        val unit = match.groupValues[2]
        if (unit.isEmpty()) return text
        val fixed = fixUnit(unit) ?: return text
        val rest = text.substring(match.range.last + 1)
        return "$number $fixed$rest"
    }

    /** Bekannte Einheit, ggf. nach Korrektur eines einzelnen verwechselten Zeichens; sonst null. */
    private fun fixUnit(unit: String): String? {
        known(unit)?.let { return it }
        for (index in unit.indices) {
            val replacement = CONFUSIONS[unit[index]] ?: continue
            known(unit.substring(0, index) + replacement + unit.substring(index + 1))?.let { return it }
        }
        return null
    }

    private fun known(unit: String): String? =
        UNITS.firstOrNull { it == unit } ?: UNITS.firstOrNull { it.equals(unit, ignoreCase = true) }
}
