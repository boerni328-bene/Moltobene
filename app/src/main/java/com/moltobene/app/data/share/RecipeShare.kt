package com.moltobene.app.data.share

import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.Base64

/*
 * Teilen einzelner Rezepte: als lesbarer Text und als Rezeptdatei im Standard schema.org/Recipe.
 * Notizen werden nie geteilt (Entscheidung des Projektinhabers), die Quelle immer.
 * Reines Kotlin, per Unit-Test prüfbar.
 */

/** Texte aus strings.xml für den lesbaren Text, in der Sprache der Oberfläche. */
class ShareLabels(
    val untitled: String,
    val ingredients: String,
    val instructions: String,
    /** Portionenangabe, z. B. „4 Portionen“ oder „12 Stück“; [unit] ist null ohne eigene Angabe. */
    val servings: (count: Int, unit: String?) -> String,
    /** Zeile mit der Quelle, z. B. „Quelle: Omas Kochbuch“. */
    val source: (String) -> String,
)

object RecipeShareText {

    /**
     * Lesbarer Text, z. B. für WhatsApp oder E-Mail. Zwischenüberschriften der Zutaten enden wie im
     * Eingabefeld mit Doppelpunkt, damit sich der Text später wieder übernehmen lässt.
     */
    fun format(recipe: Recipe, labels: ShareLabels): String {
        val blocks = mutableListOf<String>()

        blocks += buildString {
            append(recipe.title.trim().ifEmpty { labels.untitled })
            recipe.servings?.let { count ->
                append('\n').append(labels.servings(count, recipe.servingsUnit?.trim()?.takeIf { it.isNotEmpty() }))
            }
        }

        if (recipe.ingredients.isNotEmpty()) {
            blocks += buildString {
                append(labels.ingredients)
                recipe.ingredients.forEachIndexed { index, ingredient ->
                    if (ingredient.isHeading) {
                        if (index > 0) append('\n')
                        append('\n').append(ingredient.text).append(':')
                    } else {
                        append("\n• ").append(ingredient.text)
                    }
                }
            }
        }

        if (recipe.steps.isNotEmpty()) {
            blocks += buildString {
                append(labels.instructions)
                recipe.steps.forEachIndexed { index, step -> append('\n').append(index + 1).append(". ").append(step) }
            }
        }

        sourceText(recipe.source)?.let { blocks += labels.source(it) }

        return blocks.joinToString("\n\n")
    }

    /** Lesbare Quelle: der Link, sonst Name und Seite. */
    fun sourceText(source: RecipeSource?): String? {
        if (source == null) return null
        source.url?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return listOfNotNull(source.name, source.page)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
            .ifEmpty { null }
    }
}

/** Rezeptdatei im offenen Standard schema.org/Recipe (JSON-LD), lesbar auch für andere Rezept-Apps. */
object RecipeJsonLd {

    const val MIME_TYPE = "application/json"
    const val FILE_EXTENSION = "json"

    private val json = Json { prettyPrint = true }

    /** [photoJpeg] wird als data-Link eingebettet, damit die Datei ohne Internet vollständig ist. */
    fun build(recipe: Recipe, untitled: String, photoJpeg: ByteArray?): String {
        val document: JsonObject = buildJsonObject {
            put("@context", "https://schema.org")
            put("@type", "Recipe")
            put("name", recipe.title.trim().ifEmpty { untitled })
            recipe.language?.takeIf { it.isNotBlank() }?.let { put("inLanguage", it) }
            recipe.servings?.let { count ->
                val unit = recipe.servingsUnit?.trim()?.takeIf { it.isNotEmpty() }
                put("recipeYield", if (unit == null) "$count" else "$count $unit")
            }
            recipe.prepMinutes?.takeIf { it > 0 }?.let { put("prepTime", isoDuration(it)) }
            recipe.totalMinutes?.takeIf { it > 0 }?.let { put("totalTime", isoDuration(it)) }
            if (recipe.ingredients.isNotEmpty()) {
                putJsonArray("recipeIngredient") {
                    recipe.ingredients.forEach { add(if (it.isHeading) "${it.text}:" else it.text) }
                }
            }
            if (recipe.steps.isNotEmpty()) {
                putJsonArray("recipeInstructions") {
                    recipe.steps.forEach { step ->
                        addJsonObject {
                            put("@type", "HowToStep")
                            put("text", step)
                        }
                    }
                }
            }
            if (recipe.tags.isNotEmpty()) put("keywords", recipe.tags.joinToString(", ") { it.name })
            val url = recipe.source?.url?.trim()?.takeIf { it.isNotEmpty() }
            if (url != null) {
                put("url", url)
            } else {
                RecipeShareText.sourceText(recipe.source)?.let { name ->
                    putJsonObject("isBasedOn") {
                        put("@type", "CreativeWork")
                        put("name", name)
                    }
                }
            }
            photoJpeg?.let { put("image", "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(it)) }
        }
        return json.encodeToString(JsonObject.serializer(), document)
    }

    /** Dauer im Format ISO 8601, z. B. 90 Minuten → „PT1H30M“. */
    fun isoDuration(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return buildString {
            append("PT")
            if (hours > 0) append(hours).append('H')
            if (rest > 0 || hours == 0) append(rest).append('M')
        }
    }
}

object ShareFiles {
    private const val MAX_NAME_LENGTH = 60
    private const val FORBIDDEN = "\\/:*?\"<>|"

    /** Dateiname aus dem Titel, ohne Zeichen, die in Dateinamen nicht erlaubt sind. */
    fun baseName(title: String, fallback: String): String {
        val cleaned = title
            .map { if (it in FORBIDDEN || it.isISOControl()) ' ' else it }
            .joinToString("")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
            .take(MAX_NAME_LENGTH)
            .trim()
        return cleaned.ifEmpty { fallback }
    }
}
