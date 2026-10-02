package com.moltobene.app.data

/**
 * Umwandlung zwischen dem mehrzeiligen Eingabefeld und den gespeicherten Zeilen.
 * Zutaten: eine Zutat pro Zeile; eine Zeile, die mit „:“ endet, ist eine Zwischenüberschrift.
 * Zubereitung: ein Schritt pro Zeile.
 */
object RecipeText {

    fun parseIngredients(text: String): List<Ingredient> =
        nonEmptyLines(text).map { line ->
            if (line.length > 1 && line.endsWith(":")) {
                Ingredient(text = line.dropLast(1).trimEnd(), isHeading = true)
            } else {
                Ingredient(text = line)
            }
        }

    fun formatIngredients(ingredients: List<Ingredient>): String =
        ingredients.joinToString("\n") { if (it.isHeading) "${it.text}:" else it.text }

    fun parseSteps(text: String): List<String> = nonEmptyLines(text)

    fun formatSteps(steps: List<String>): String = steps.joinToString("\n")

    /**
     * Quelle aus den Eingabefeldern „Quelle“ und „Seite“: Links (nur http/https) werden als Link erkannt,
     * alles andere als Name. Mit einer Seite gilt die Quelle als Buch (#40); bei einem Link zählt die Seite nicht.
     */
    fun parseSource(text: String, page: String? = null): RecipeSource? {
        val value = text.trim()
        val pageValue = page?.trim()?.takeIf { it.isNotEmpty() }
        if (value.isEmpty()) return pageValue?.let { RecipeSource(type = SourceType.BOOK, page = it) }
        return when {
            isWebLink(value) -> RecipeSource(type = SourceType.WEB, url = value)
            pageValue != null -> RecipeSource(type = SourceType.BOOK, name = value, page = pageValue)
            else -> RecipeSource(type = SourceType.OTHER, name = value)
        }
    }

    /** Eine Seite, die nur aus einer Zahl besteht (z. B. „47“), wird mit [label] angezeigt („S. 47“); ältere Angaben wie „S. 42“ bleiben, wie sie sind. */
    fun formatPage(page: String, label: (String) -> String): String =
        page.trim().let { if (it.isNotEmpty() && it.all { char -> char.isDigit() || char == '-' }) label(it) else it }

    fun formatSource(source: RecipeSource?): String =
        source?.url ?: source?.name ?: ""

    fun isWebLink(value: String): Boolean {
        val lower = value.lowercase()
        return (lower.startsWith("https://") || lower.startsWith("http://")) && !value.any { it.isWhitespace() }
    }

    private fun nonEmptyLines(text: String): List<String> =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }
}
