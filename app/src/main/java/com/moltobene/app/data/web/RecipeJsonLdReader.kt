package com.moltobene.app.data.web

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URL

/**
 * Liest ein Rezept im Standard schema.org/Recipe (JSON-LD) – so, wie Koch-Portale es unsichtbar in ihre Seiten
 * einbetten und wie Moltobene es beim Teilen als Rezeptdatei schreibt (#54, #55). Verträgt die üblichen
 * Spielarten: „@graph“, „@type“ als Liste, Zubereitung als Text, Liste, HowToStep oder HowToSection, Portionen
 * und Fotos in verschiedenen Formen, HTML und Sonderzeichen im Text. Fremde Inhalte gelten als unsicher:
 * Größe, Tiefe und Anzahl der Einträge sind begrenzt. Reines Kotlin, per Unit-Test prüfbar.
 */
object RecipeJsonLdReader {

    private val json = Json { isLenient = true }

    /** Das erste Rezept in [text]; null, wenn keins darin steht. [baseUrl] macht relative Links absolut. */
    fun read(text: String, baseUrl: String? = null): WebRecipe? {
        if (text.length > MAX_CHARS) return null
        val element = parse(text) ?: return null
        val recipe = findRecipe(element, 0) ?: return null
        return toRecipe(recipe, baseUrl)
    }

    /** Auch nicht ganz korrektes JSON wird gelesen: mit HTML-Kommentaren, Zeilenumbrüchen in Texten oder Komma am Ende. */
    internal fun parse(text: String): JsonElement? {
        val cleaned = text.trim()
            .removePrefix(BYTE_ORDER_MARK)
            // Kommentare ohne Daten fallen ganz weg; um die Daten gelegte Kommentarzeichen nur die Zeichen selbst.
            .replace(TEXT_COMMENT, "")
            .replace("<!--", "").replace("-->", "")
            .replace("<![CDATA[", "").replace("]]>", "")
            .replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')
            .trim()
            .trimEnd(';')
        return runCatching { json.parseToJsonElement(cleaned) }.getOrNull()
            ?: runCatching { json.parseToJsonElement(cleaned.replace(TRAILING_COMMA, "$1")) }.getOrNull()
    }

    /** Sucht das Rezept: direkt, in einer Liste, im „@graph“ oder als Hauptinhalt einer Seite. */
    private fun findRecipe(element: JsonElement, depth: Int): JsonObject? {
        if (depth > MAX_DEPTH) return null
        return when (element) {
            is JsonObject -> if (isRecipe(element)) element else element.values.firstNotNullOfOrNull { findRecipe(it, depth + 1) }
            is JsonArray -> element.take(MAX_ITEMS).firstNotNullOfOrNull { findRecipe(it, depth + 1) }
            else -> null
        }
    }

    private fun isRecipe(item: JsonObject): Boolean = types(item).any { it.equals("Recipe", ignoreCase = true) }

    /** „@type“ ohne Vorsatz: „Recipe“, „schema:Recipe“ und „https://schema.org/Recipe“ zählen gleich. */
    private fun types(item: JsonObject): List<String> = when (val type = item["@type"]) {
        is JsonPrimitive -> listOf(type.content)
        is JsonArray -> type.mapNotNull { (it as? JsonPrimitive)?.content }
        else -> emptyList()
    }.map { it.substringAfterLast('/').substringAfterLast(':').substringAfterLast('#') }

    private fun toRecipe(item: JsonObject, baseUrl: String?): WebRecipe {
        val yieldText = yieldText(item["recipeYield"] ?: item["yield"])
        val servings = RecipeYield.parse(yieldText)
        val prep = RecipeDuration.minutes(text(item["prepTime"]))
        val cook = RecipeDuration.minutes(text(item["cookTime"]))
        val total = RecipeDuration.minutes(text(item["totalTime"]))
            ?: if (prep != null || cook != null) (prep ?: 0) + (cook ?: 0) else null
        return WebRecipe(
            title = HtmlText.inline(text(item["name"]) ?: text(item["headline"])).ifEmpty { null },
            description = HtmlText.inline(text(item["description"])).ifEmpty { null },
            yieldText = yieldText,
            servings = servings?.count,
            servingsUnit = servings?.unit,
            ingredients = ingredients(item["recipeIngredient"] ?: item["ingredients"]),
            steps = steps(item["recipeInstructions"], 0).take(MAX_STEPS),
            imageUrls = images(item["image"], baseUrl, 0),
            prepMinutes = prep,
            totalMinutes = total,
            language = language(text(item["inLanguage"])),
            url = listOfNotNull(text(item["url"]), idOf(item["mainEntityOfPage"]))
                .firstOrNull { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) },
            sourceName = HtmlText.inline(text(item["isBasedOn"]))
                .takeIf { it.isNotEmpty() && !it.startsWith("http", ignoreCase = true) },
        )
    }

    /** Text eines Feldes: Text, Zahl, das erste Element einer Liste oder „name“/„text“/„@value“ eines Objekts. */
    private fun text(element: JsonElement?): String? = when (element) {
        null, JsonNull -> null
        is JsonPrimitive -> element.content.takeIf { it.isNotBlank() }
        is JsonArray -> element.firstNotNullOfOrNull { text(it) }
        is JsonObject -> text(element["@value"]) ?: text(element["name"]) ?: text(element["text"])
    }

    private fun idOf(element: JsonElement?): String? = when (element) {
        is JsonObject -> text(element["@id"]) ?: text(element["url"])
        else -> text(element)
    }

    /** Portionen: bei einer Liste wie ["4", "4 Portionen"] die aussagekräftigste Angabe (mit Wort). */
    private fun yieldText(element: JsonElement?): String? {
        val values = when (element) {
            is JsonArray -> element.mapNotNull { text(it) }
            else -> listOfNotNull(text(element))
        }.map { HtmlText.inline(it) }.filter { it.isNotEmpty() }
        return values.firstOrNull { value -> value.any { it.isLetter() } && value.any { it.isDigit() } } ?: values.firstOrNull()
    }

    private fun ingredients(element: JsonElement?): List<String> {
        val raw = when (element) {
            is JsonArray -> element.take(MAX_ITEMS).flatMap { item ->
                when (item) {
                    is JsonPrimitive -> listOf(item.content)
                    is JsonObject -> listOfNotNull(text(item["text"]) ?: text(item["name"]))
                    else -> emptyList()
                }
            }
            is JsonPrimitive -> HtmlText.lines(element.content)
            else -> emptyList()
        }
        return raw.map { HtmlText.inline(it) }.filter { it.isNotEmpty() }.take(MAX_INGREDIENTS)
    }

    /**
     * Schritte der Zubereitung. Abschnitte (HowToSection, z. B. „Teig“) werden dem ersten ihrer Schritte
     * vorangestellt, weil die Zubereitung keine Zwischenüberschriften kennt: „Teig: Mehl und Butter verkneten.“
     */
    private fun steps(element: JsonElement?, depth: Int): List<String> {
        if (element == null || depth > MAX_DEPTH) return emptyList()
        return when (element) {
            is JsonPrimitive -> HtmlText.lines(element.content).map(::withoutNumber).filter { it.isNotEmpty() }
            is JsonArray -> element.take(MAX_ITEMS).flatMap { steps(it, depth + 1) }
            is JsonObject -> {
                val children = element["itemListElement"] ?: element["steps"]
                if (children != null) {
                    val inner = steps(children, depth + 1)
                    val name = HtmlText.inline(text(element["name"]))
                    if (name.isEmpty() || inner.isEmpty() || inner.first().startsWith(name)) {
                        inner
                    } else {
                        listOf("${name.trimEnd(':')}: ${inner.first()}") + inner.drop(1)
                    }
                } else {
                    val stepText = text(element["text"]) ?: text(element["name"]) ?: text(element["description"])
                    HtmlText.lines(stepText).map(::withoutNumber).filter { it.isNotEmpty() }
                }
            }
            else -> emptyList()
        }
    }

    /** Fotos: Link, Liste von Links oder ImageObject; das breiteste zuerst, falls Breiten angegeben sind. */
    private fun images(element: JsonElement?, baseUrl: String?, depth: Int): List<String> {
        if (element == null || depth > 2) return emptyList()
        val found = mutableListOf<Pair<String, Int>>()
        fun add(value: JsonElement?) {
            when (value) {
                is JsonPrimitive -> absolute(value.content, baseUrl)?.let { found += it to 0 }
                is JsonObject -> {
                    val url = text(value["url"]) ?: text(value["contentUrl"]) ?: text(value["@id"])
                    val width = text(value["width"])?.filter { it.isDigit() }?.take(6)?.toIntOrNull() ?: 0
                    absolute(url, baseUrl)?.let { found += it to width }
                }
                is JsonArray -> value.take(MAX_ITEMS).forEach { add(it) }
                else -> Unit
            }
        }
        add(element)
        return found.sortedByDescending { it.second }.map { it.first }.distinct().take(MAX_IMAGES)
    }

    /** Nur Links ins Internet (http/https) oder eingebettete Fotos (data:) zählen; relative werden absolut. */
    internal fun absolute(value: String?, baseUrl: String?): String? {
        val link = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (link.startsWith("data:image/", ignoreCase = true)) return link
        val resolved = runCatching { if (!baseUrl.isNullOrBlank()) URL(URL(baseUrl), link).toString() else URL(link).toString() }
            .getOrNull() ?: return null
        return resolved.takeIf { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
    }

    /** Sprache als Kürzel aus zwei Buchstaben, z. B. „de-DE“ → „de“. */
    internal fun language(value: String?): String? =
        value?.trim()?.lowercase()?.substringBefore('-')?.substringBefore('_')?.takeIf { LANGUAGE.matches(it) }

    /** Nummern am Anfang eines Schritts („1.“, „Schritt 2:“) fallen weg; die App nummeriert selbst. */
    internal fun withoutNumber(step: String): String = step.replace(STEP_NUMBER, "").trim()

    /** Unsichtbares Zeichen, mit dem manche Dateien beginnen (als Zahl geschrieben, damit es im Quelltext sichtbar bleibt). */
    internal val BYTE_ORDER_MARK = 0xFEFF.toChar().toString()

    private const val MAX_CHARS = 2_000_000
    private const val MAX_DEPTH = 8
    private const val MAX_ITEMS = 300
    private const val MAX_INGREDIENTS = 200
    private const val MAX_STEPS = 100
    private const val MAX_IMAGES = 5
    private val TRAILING_COMMA = Regex(",\\s*([}\\]])")
    private val TEXT_COMMENT = Regex("<!--[^{\\[]*?-->")
    private val LANGUAGE = Regex("[a-z]{2}")
    private val STEP_NUMBER = Regex(
        "^(?:(?:schritt|step|passo|passaggio|étape|etape|paso)\\s*)?\\d{1,2}\\s*[.):](?!\\d)\\s*",
        RegexOption.IGNORE_CASE,
    )
}
