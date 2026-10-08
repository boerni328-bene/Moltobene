package com.moltobene.app.data

/**
 * Die Zeile „Quelle: …“ am Ende eines Rezepttexts, wie Moltobene sie beim Teilen schreibt (#54) – auf Deutsch
 * oder Englisch, je nach Sprache des Handys, das geteilt hat –, danach vielleicht „Video: …“. Beim Übernehmen aus
 * Text werden sie zu Quelle und Video, statt als letzte Schritte in der Zubereitung zu landen. Reines Kotlin, per Unit-Test prüfbar.
 */
object SourceLine {

    /**
     * @param text der Text ohne die Zeilen mit Quelle und Video
     * @param source die Quelle, z. B. ein Link oder „Omas Kochbuch“
     * @param page die Seite, falls die Quelle mit „S. 47“ bzw. „p. 47“ endete
     * @param video der Link aus der Zeile „Video: …“ (seit 0.25.0 nach der Quelle)
     */
    data class Split(val text: String, val source: String?, val page: String? = null, val video: String? = null)

    fun split(text: String): Split {
        val lines = text.trimEnd().lines().toMutableList()
        var last = lines.indexOfLast { it.isNotBlank() }
        if (last < 0) return Split(text, null)
        val video = VIDEO_LINE.matchEntire(lines[last].trim())?.groupValues?.get(1)?.takeIf { it.length <= MAX_LENGTH }
        if (video != null) {
            lines.removeAt(last)
            last = lines.indexOfLast { it.isNotBlank() }
        }
        val value = lines.getOrNull(last)?.let { LINE.matchEntire(it.trim()) }?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_LENGTH }
            ?: return Split(if (video == null) text else lines.joinToString("\n").trimEnd(), null, video = video)
        val rest = lines.take(last).joinToString("\n").trimEnd()
        val (source, page) = withPage(value)
        return Split(rest, source, page, video)
    }

    /**
     * Trennt eine Seitenangabe ab, wie Moltobene sie beim Teilen anhängt:
     * „Omas Kochbuch, S. 47“ → „Omas Kochbuch“ und „47“. Links bleiben, wie sie sind.
     */
    fun withPage(value: String): Pair<String, String?> {
        val page = PAGE.matchEntire(value.trim())
        // Bei einem Link gibt es keine Seite (#40).
        return if (page != null && !RecipeText.isWebLink(page.groupValues[1].trim())) {
            page.groupValues[1].trim() to page.groupValues[2]
        } else {
            value.trim() to null
        }
    }

    private const val MAX_LENGTH = 300
    private val LINE = Regex("(?:quelle|source)\\s*:\\s*(.+)", RegexOption.IGNORE_CASE)
    private val VIDEO_LINE = Regex("video\\s*:\\s*(https?://\\S+)", RegexOption.IGNORE_CASE)
    private val PAGE = Regex("(.+?),\\s*(?:S\\.|Seite|p\\.|page)\\s*(\\d{1,4}(?:-\\d{1,4})?)", RegexOption.IGNORE_CASE)
}
