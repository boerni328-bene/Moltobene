package com.moltobene.app.data

/**
 * Die Rezeptmenge (#63): wie viele Portionen, Stück oder Backformen ein Rezept ergibt – eine gemeinsame Lese-Stelle
 * für Internetseiten, Rezeptdateien, Text und Fotos. Beispiele:
 *  - „4 Portionen“, „Für 6 Personen“, „Serves 4“ → 4 bzw. 6, keine eigene Einheit
 *  - „12 Stück“, „1 Springform (Ø 26 cm)“, „Für eine 26er Springform“, „One 9-inch cake“, „4 Pizzen à 250 g“
 *    → Anzahl und Einheit
 *  - „Springform Ø 26 cm“, „Blech 30 x 40 cm“ → keine Anzahl, nur die Einheit
 *
 * Größen (Ø, cm, Zoll, „26er“, „30 x 40“, „je 250 g“) sind nie eine Anzahl. Ist etwas unklar, bleibt die Anzahl leer,
 * statt geraten zu werden; die Angabe bleibt dann als Einheit erhalten. Reines Kotlin, per Unit-Test prüfbar.
 */
object RecipeYield {

    /** @param count Anzahl, null ohne Zahl; @param unit eigene Einheit, null bei Portionen oder Personen */
    data class Servings(val count: Int?, val unit: String?)

    /**
     * Eine Mengenangabe, wie sie im Rezept steht, z. B. aus schema.org `recipeYield` oder der Zeile „Für 4 Personen“.
     * null, wenn darin weder eine Anzahl noch eine Einheit steht.
     */
    fun parse(text: String?): Servings? {
        var value = text?.replace(SPACES, " ")?.trim()?.trimEnd(':', '.', ';', ',')?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        value = value.replace(INTRO, "").trim()
        PERSONS_FIRST.matchEntire(value)?.let { return Servings(it.groupValues[1].toInt().takeIf { count -> count in 1..MAX }, null) }
        val count: Int?
        val rest: String
        val number = LEADING_NUMBER.find(value)
        val word = if (number == null) LEADING_WORD.find(value) else null
        when {
            number != null -> {
                count = number.groupValues[1].toInt().takeIf { it in 1..MAX } ?: return null
                rest = value.substring(number.range.last + 1).trim()
            }
            word != null -> {
                count = NUMBER_WORDS.getValue(word.groupValues[1].lowercase())
                rest = value.substring(word.range.last + 1).trim()
            }
            else -> {
                count = null
                rest = value
            }
        }
        val unit = rest.replace(ARTICLE, "").trim().trim(',', ':', ';').trim()
            .takeIf { it.isNotEmpty() && it.any(Char::isLetterOrDigit) && it.length <= MAX_UNIT }
            ?.takeIf { !PERSONS.matches(it) }
        if (count == null && unit == null) return null
        // Ohne Zahl zählt nur etwas mit Buchstaben, z. B. „Springform Ø 26 cm“ – nicht „einige“ oder „nach Belieben“.
        if (count == null && (unit == null || NOT_A_YIELD.matches(unit))) return null
        return Servings(count, unit)
    }

    /** Enthält die Angabe eine Größe wie „Ø 26 cm“, „30 x 40“, „26er“ oder „9-inch“? */
    fun hasSize(text: String): Boolean = SIZE.containsMatchIn(text)

    /** Wörter für Portionen und Personen in den Sprachen der App und der Texterkennung – keine eigene Einheit. */
    internal const val PERSON_WORDS =
        "portion|portionen|person|personen|pers\\.?|serving|servings|people|persons|portions|porzione|porzioni|persona|" +
            "persone|personne|personnes|couverts|parts|ración|raciones|porción|porciones|personas"

    private const val MAX = 999
    private const val MAX_UNIT = 60

    private val SPACES = Regex("[\\s\\u00A0]+")

    /** „Für“, „Zutaten für“, „Serves“, „Makes:“, „Ergibt“, „Yield:“ … am Anfang. */
    private val INTRO = Regex(
        "^(?:(?:zutaten|ingredients?|ingredienti|ingrédients|ingredientes|teig|dough|impasto|pâte|masa)\\s+)?" +
            "(?:für|fuer|for|per|pour|para|serves|makes|ergibt|reicht für|dosi per|rend|rinde|yields?|ricetta per)\\s*:?\\s+",
        RegexOption.IGNORE_CASE,
    )

    /** „Portionen: 4“ */
    private val PERSONS_FIRST = Regex("(?:$PERSON_WORDS)\\s*:?\\s*(\\d{1,3})", RegexOption.IGNORE_CASE)
    private val PERSONS = Regex("(?:$PERSON_WORDS)", RegexOption.IGNORE_CASE)

    /**
     * Eine Zahl am Anfang, auch als Bereich („4-6“, „4 bis 6“) – aber keine Größe: nicht „26 cm“, „26er“, „9-inch“,
     * „30 x 40“, „250 g“, „1,5 kg“ oder „65 %“.
     */
    private val LEADING_NUMBER = Regex(
        "^(\\d{1,3})(?:\\s*(?:-|–|to|bis|à|a|ou|o)\\s*\\d{1,3})?" +
            "(?![\\d.,/])(?!\\s*-?\\s*(?:er\\b|cm\\b|mm\\b|inch|in\\b|zoll|\"|″|”|x\\b|×|x\\s*\\d|g\\b|gr\\b|kg\\b|ml\\b|l\\b|oz\\b|lb|%|°))" +
            "(?=\\s|$|\\()",
        RegexOption.IGNORE_CASE,
    )

    /** „ein“, „eine“, „one“, „a“, „un“, „una“ … am Anfang zählen als Anzahl. */
    private val NUMBER_WORDS = mapOf(
        "ein" to 1, "eine" to 1, "einen" to 1, "einem" to 1, "einer" to 1, "one" to 1, "a" to 1, "an" to 1,
        "un" to 1, "una" to 1, "uno" to 1, "une" to 1, "zwei" to 2, "two" to 2, "due" to 2, "deux" to 2, "dos" to 2,
        "drei" to 3, "three" to 3, "tre" to 3, "trois" to 3, "tres" to 3, "vier" to 4, "four" to 4, "quattro" to 4,
        "quatre" to 4, "cuatro" to 4,
    )
    private val LEADING_WORD = Regex("^(${NUMBER_WORDS.keys.joinToString("|")})\\s+(?=\\S)", RegexOption.IGNORE_CASE)

    /** Artikel vor der Einheit: „für die Springform“ → „Springform“. */
    private val ARTICLE = Regex("^(?:der|die|das|den|the|il|lo|la|le|el|los|las)\\s+", RegexOption.IGNORE_CASE)

    /** Ø, „26 cm“, „9-inch“, „9\"“, „26er“, „30 x 40“, „je 250 g“, „à 250 g“, „250 g each“, „da 250 g“. */
    private val SIZE = Regex(
        "(?:[Ø⌀]|\\d\\s*-?\\s*(?:cm|mm|inch|in\\b|zoll|\"|″|”)|\\d{2}er\\b|\\d\\s*[x×]\\s*\\d|" +
            "(?:^|[\\s(])(?:je|à|a|da|de|each|ciascuno|chacun|cada)\\s+\\d|\\d\\s*(?:g|kg|oz|lb)\\s+each)",
        RegexOption.IGNORE_CASE,
    )

    /** Angaben ohne Zahl, die keine Rezeptmenge sind. */
    private val NOT_A_YIELD = Regex(
        "(?:einige|etwas|nach belieben|some|a few|several|alcuni|quelques|algunos|varies|variabel)",
        RegexOption.IGNORE_CASE,
    )
}
