package com.moltobene.app.data

/**
 * Links in übernommenem Text („Aus Text übernehmen“). Browser teilen meist nur den Link, oft mit dem Titel
 * der Seite davor; WhatsApp und E-Mails enthalten dagegen den ganzen Rezepttext, manchmal mit einem Link
 * als Quelle. Nur Links mit http/https zählen. Reines Kotlin, per Unit-Test prüfbar.
 */
object TextLinks {

    /** Ein geteilter Link mit dem Text drumherum, z. B. dem Titel der Seite. */
    data class SharedLink(val url: String, val title: String?)

    private val LINK = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)

    /** Satzzeichen am Ende gehören meist zum Satz, nicht zum Link („… unter https://example.org.“). */
    private const val TRAILING = ".,;:!?)]}»“”\"'"

    /** Alle Links in der Reihenfolge, in der sie im Text stehen. */
    fun all(text: String): List<String> =
        LINK.findAll(text).map { it.value.trimEnd { char -> char in TRAILING } }
            .filter { RecipeText.isWebLink(it) }
            .distinct()
            .toList()

    fun first(text: String): String? = all(text).firstOrNull()

    /**
     * Ist der Text im Grunde nur ein Link – höchstens mit einem kurzen Titel davor oder danach? Dann ist es
     * ein geteilter Link und kein Rezepttext.
     */
    fun sharedLink(text: String): SharedLink? {
        val links = all(text)
        if (links.size != 1) return null
        val rest = LINK.replace(text, " ").lines().map { it.trim() }.filter { it.isNotEmpty() }
        val words = rest.sumOf { line -> line.split(Regex("\\s+")).size }
        if (rest.size > MAX_TITLE_LINES || words > MAX_TITLE_WORDS) return null
        return SharedLink(links.single(), rest.joinToString(" ").trim().trim('-', '–', '|', ':').trim().ifEmpty { null })
    }

    private const val MAX_TITLE_LINES = 2
    private const val MAX_TITLE_WORDS = 15
}
