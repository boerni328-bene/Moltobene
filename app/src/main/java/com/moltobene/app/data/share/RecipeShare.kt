package com.moltobene.app.data.share

import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.RecipeText
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
 * Notizen werden nie geteilt (Entscheidung des Projektinhabers), die Quelle und der Link zum Video immer.
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
    /** Seitenangabe, z. B. „S. 47“. */
    val page: (String) -> String = { it },
    /** Zeile mit dem Link zum Video, z. B. „Video: https://…“. */
    val video: (String) -> String = { it },
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
            val unit = recipe.servingsUnit?.trim()?.takeIf { it.isNotEmpty() }
            val count = recipe.servings
            // Eine Backform ohne Anzahl (#63) steht für sich, z. B. „Springform Ø 26 cm“.
            if (count != null) append('\n').append(labels.servings(count, unit)) else if (unit != null) append('\n').append(unit)
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

        listOfNotNull(
            sourceText(recipe.source, labels.page)?.let(labels.source),
            videoLink(recipe)?.let(labels.video),
        ).takeIf { it.isNotEmpty() }?.let { blocks += it.joinToString("\n") }

        return blocks.joinToString("\n\n")
    }

    /** Der Link zum Video; fehlt, wenn er leer ist oder derselbe wie die Quelle. */
    fun videoLink(recipe: Recipe): String? =
        recipe.videoUrl?.trim()?.takeIf { it.isNotEmpty() && it != recipe.source?.url?.trim() }

    /** Lesbare Quelle: der Link, sonst Name und Seite (eine reine Zahl mit [page], z. B. „S. 47“). */
    fun sourceText(source: RecipeSource?, page: (String) -> String = { it }): String? {
        if (source == null) return null
        source.url?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return listOfNotNull(source.name, source.page?.let { RecipeText.formatPage(it, page) })
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

    /**
     * [photoJpeg] wird als data-Link eingebettet, damit die Datei ohne Internet vollständig ist.
     * [page] schreibt eine Seitenzahl der Quelle aus, z. B. „S. 47“.
     */
    fun build(recipe: Recipe, untitled: String, photoJpeg: ByteArray?, page: (String) -> String = { it }): String {
        val document: JsonObject = buildJsonObject {
            put("@context", "https://schema.org")
            put("@type", "Recipe")
            put("name", recipe.title.trim().ifEmpty { untitled })
            recipe.language?.takeIf { it.isNotBlank() }?.let { put("inLanguage", it) }
            val unit = recipe.servingsUnit?.trim()?.takeIf { it.isNotEmpty() }
            when (val count = recipe.servings) {
                null -> unit?.let { put("recipeYield", it) }
                else -> put("recipeYield", if (unit == null) "$count" else "$count $unit")
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
                RecipeShareText.sourceText(recipe.source, page)?.let { name ->
                    putJsonObject("isBasedOn") {
                        put("@type", "CreativeWork")
                        put("name", name)
                    }
                }
            }
            // Auch wenn es dieselbe Adresse wie die Quelle ist: So kommt das Feld „Video“ beim Übernehmen wieder an.
            recipe.videoUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { link ->
                putJsonObject("video") {
                    put("@type", "VideoObject")
                    put("name", recipe.title.trim().ifEmpty { untitled })
                    put("url", link)
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
