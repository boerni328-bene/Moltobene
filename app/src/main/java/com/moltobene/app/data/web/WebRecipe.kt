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

/** Portionen aus einer Angabe wie „4 Portionen“, „Für 6 Personen“, „12 Stück“ oder „1 Springform (26 cm)“. */
object RecipeYield {

    data class Servings(val count: Int, val unit: String?)

    fun parse(text: String?): Servings? {
        val value = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = NUMBER.find(value) ?: return null
        val count = match.value.toIntOrNull()?.takeIf { it in 1..MAX } ?: return null
        // Was nach der Zahl (und einem Bereich wie „4-6“) steht, ist die Einheit – außer es sind Portionen.
        val rest = value.substring(match.range.last + 1)
            .replace(RANGE_REST, "")
            .trim()
            .trim(',', '.', ':', ';')
            .trim()
        val unit = rest.takeIf { it.isNotEmpty() && !SERVINGS_WORDS.containsMatchIn(it) && rest.length <= 40 }
        return Servings(count, unit)
    }

    private const val MAX = 999
    private val NUMBER = Regex("\\d{1,3}")
    private val RANGE_REST = Regex("^\\s*[-–]\\s*\\d{1,3}")

    /** Wörter für Portionen und Personen in den Sprachen der Texterkennung – dann gibt es keine eigene Einheit. */
    private val SERVINGS_WORDS = Regex(
        "^(portion|portionen|person|personen|pers\\.?|serving|servings|serves|people|persons|portions|" +
            "porzione|porzioni|persona|persone|personne|personnes|couverts|ración|raciones|porción|porciones|personas)\\b",
        RegexOption.IGNORE_CASE,
    )
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
