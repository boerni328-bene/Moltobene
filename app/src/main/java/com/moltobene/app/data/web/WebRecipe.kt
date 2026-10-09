package com.moltobene.app.data.web

/**
 * Ein Rezept, wie es auf einer Internetseite oder in einer Rezeptdatei steht (schema.org/Recipe), schon
 * bereinigt: Text ohne HTML, Zutaten und Schritte je Zeile, Links absolut. Grundlage für einen Entwurf.
 */
data class WebRecipe(
    val title: String? = null,
    val description: String? = null,
    /** Angabe zu den Portionen, wie sie dasteht, z. B. „4 Portionen“ oder „1 Kuchen (26 cm)“. */
    val yieldText: String? = null,
    val servings: Int? = null,
    /** Einheit der Portionen, z. B. „Stück“; null bei Portionen oder Personen. */
    val servingsUnit: String? = null,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    /** Fotos des Gerichts, das beste zuerst. */
    val imageUrls: List<String> = emptyList(),
    val prepMinutes: Int? = null,
    val totalMinutes: Int? = null,
    /** Sprache als Kürzel wie „de“, nur wenn sie angegeben ist. */
    val language: String? = null,
    val url: String? = null,
    /** Quelle ohne Link, z. B. „Omas Kochbuch, S. 47“ (schema.org isBasedOn), wie Moltobene sie in Rezeptdateien schreibt. */
    val sourceName: String? = null,
    /** Link zu einem Video des Rezepts (schema.org video), z. B. auf YouTube. */
    val videoUrl: String? = null,
) {
    /** Steht genug da, um daraus einen Entwurf zu machen? */
    val hasContent: Boolean get() = ingredients.isNotEmpty() || steps.isNotEmpty()

    /**
     * Lesbarer Text für „Übernommener Text“: so bleibt auch erhalten, was kein eigenes Feld hat, z. B. die
     * Beschreibung. Ohne Überschriften, damit er in jeder Sprache der Oberfläche passt.
     */
    fun toText(source: String?): String = listOfNotNull(
        title,
        description,
        yieldText,
        ingredients.joinToString("\n").ifEmpty { null },
        steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n").ifEmpty { null },
        source,
    ).joinToString("\n\n")
}

/** Dauern aus Rezeptangaben: ISO 8601 („PT1H30M“, „P0DT0H20M“) oder einfache Angaben wie „30 Min.“. */
object RecipeDuration {

    fun minutes(text: String?): Int? {
        val value = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        ISO.matchEntire(value.uppercase())?.let { match ->
            val (days, hours, minutes, seconds) = match.destructured
            val total = (days.toDoubleOrNull() ?: 0.0) * 1440 + (hours.toDoubleOrNull() ?: 0.0) * 60 +
                (minutes.toDoubleOrNull() ?: 0.0) + (seconds.toDoubleOrNull() ?: 0.0) / 60
            return total.toInt().takeIf { it in 1..MAX_MINUTES }
        }
        var total = 0
        PLAIN_HOURS.find(value)?.let { total += (it.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0).times(60).toInt() }
        PLAIN_MINUTES.find(value)?.let { total += it.groupValues[1].toIntOrNull() ?: 0 }
        return total.takeIf { it in 1..MAX_MINUTES }
    }

    private const val MAX_MINUTES = 7 * 24 * 60
    private val ISO = Regex("P(?:(\\d+(?:\\.\\d+)?)D)?(?:T(?:(\\d+(?:\\.\\d+)?)H)?(?:(\\d+(?:\\.\\d+)?)M)?(?:(\\d+(?:\\.\\d+)?)S)?)?")
    private val PLAIN_HOURS = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:std|stunde|stunden|h|hr|hrs|hour|hours|ore|ora|heure|heures|hora|horas)\\b", RegexOption.IGNORE_CASE)
    private val PLAIN_MINUTES = Regex("(\\d+)\\s*(?:min|mins|minute|minuten|minutes|minuti|minutos)\\b", RegexOption.IGNORE_CASE)
}
