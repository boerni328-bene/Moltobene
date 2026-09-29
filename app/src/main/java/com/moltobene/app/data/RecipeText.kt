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

    /** Quelle aus dem Eingabefeld: Links (nur http/https) werden als Link erkannt, alles andere als Name. */
    fun parseSource(text: String): RecipeSource? {
        val value = text.trim()
        if (value.isEmpty()) return null
        return if (isWebLink(value)) {
            RecipeSource(type = SourceType.WEB, url = value)
        } else {
            RecipeSource(type = SourceType.OTHER, name = value)
        }
    }

    fun formatSource(source: RecipeSource?): String =
        source?.url ?: source?.name ?: ""

    fun isWebLink(value: String): Boolean {
        val lower = value.lowercase()
        return (lower.startsWith("https://") || lower.startsWith("http://")) && !value.any { it.isWhitespace() }
    }

    private fun nonEmptyLines(text: String): List<String> =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }
}
