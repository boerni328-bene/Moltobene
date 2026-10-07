package com.moltobene.app.data.web

import com.moltobene.app.data.ocr.RecipeTextParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.ByteArrayInputStream

/**
 * Liest eine Internetseite für „Aus Link übernehmen“ (#55), in dieser Reihenfolge:
 * 1. eingebettete Rezeptdaten im Standard schema.org/Recipe (JSON-LD) – so machen es fast alle Koch-Portale,
 * 2. ältere Auszeichnung derselben Daten direkt im HTML (Microdata),
 * 3. sonst der sichtbare Text der Seite, den der Aufrufer wie „Aus Text übernehmen“ einordnen kann.
 * Skripte werden nie ausgeführt. Ohne Android-Abhängigkeiten, per Unit-Test prüfbar.
 */
object RecipePage {

    /**
     * @param recipe das Rezept, falls die Seite es in einem Standardformat enthält
     * @param title Titel der Seite (ohne Namen des Portals, soweit erkennbar)
     * @param text sichtbarer Text des Hauptteils, mit Zeilen
     * @param language Sprache der Seite als Kürzel, falls angegeben
     * @param imageUrl Vorschaufoto der Seite (z. B. für soziale Netzwerke)
     */
    data class Content(
        val recipe: WebRecipe?,
        val title: String?,
        val text: String,
        val language: String?,
        val imageUrl: String?,
    )

    fun read(page: WebPage): Content {
        val start = page.body.take(64).toByteArray().toString(Charsets.UTF_8)
            .removePrefix(RecipeJsonLdReader.BYTE_ORDER_MARK)
            .trimStart()
        // Ein Link direkt auf eine Rezeptdatei (JSON-LD) statt auf eine Seite.
        if (start.startsWith("{") || start.startsWith("[")) {
            val json = page.body.toString(page.charset?.let { charset(it) } ?: Charsets.UTF_8)
            val recipe = RecipeJsonLdReader.read(json, page.url)
            return Content(recipe, recipe?.title, "", recipe?.language, null)
        }
        val document = Jsoup.parse(ByteArrayInputStream(page.body), page.charset, page.url)
        return read(document)
    }

    fun read(document: Document): Content {
        val language = RecipeJsonLdReader.language(document.selectFirst("html")?.attr("lang"))
        val imageUrl = meta(document, "og:image")?.let { RecipeJsonLdReader.absolute(it, document.location()) }
        val title = pageTitle(document)

        val embedded = jsonLd(document)
        val recipe = (embedded?.takeIf { it.hasContent } ?: microdata(document) ?: embedded)?.let { found ->
            found.copy(
                title = found.title ?: title,
                imageUrls = found.imageUrls.ifEmpty { listOfNotNull(imageUrl) },
                language = found.language ?: language,
            )
        }
        val text = if (recipe?.hasContent == true) "" else mainText(document)
        return Content(recipe, recipe?.title ?: title, text, language, imageUrl)
    }

    /**
     * Das erste Rezept in den eingebetteten Daten; eine Seite hat oft mehrere solche Blöcke. Manche Seiten ergänzen
     * Zutaten und Zubereitung erst im Browser per Skript – dann zählen wenigstens Titel, Portionen und Zeiten.
     */
    private fun jsonLd(document: Document): WebRecipe? {
        val recipes = document.select("script[type]")
            .asSequence()
            .filter { it.attr("type").contains("ld+json", ignoreCase = true) }
            .take(MAX_SCRIPTS)
            .mapNotNull { RecipeJsonLdReader.read(it.data(), document.location()) }
            .toList()
        return recipes.firstOrNull { it.hasContent } ?: recipes.firstOrNull()
    }

    /** Ältere Auszeichnung mit „itemprop“-Angaben direkt im HTML. */
    private fun microdata(document: Document): WebRecipe? {
        val root = document.select("[itemtype]").firstOrNull { element ->
            element.attr("itemtype").split(WHITESPACE).any { RECIPE_TYPE.matches(it) }
        } ?: return null
        fun hasProp(element: Element, name: String) =
            element.attr("itemprop").split(WHITESPACE).any { it.equals(name, ignoreCase = true) }
        // Nur Angaben des Rezepts selbst, nicht z. B. der Name des Autors in einem eigenen Abschnitt.
        fun props(name: String): List<Element> = root.select("[itemprop]").filter { element ->
            hasProp(element, name) && element.parents().firstOrNull { it.hasAttr("itemscope") } == root
        }
        fun value(element: Element): String = when {
            element.hasAttr("content") -> element.attr("content")
            element.hasAttr("datetime") -> element.attr("datetime")
            element.normalName() == "meta" -> element.attr("content")
            else -> element.text()
        }
        fun first(name: String): String? = props(name).firstOrNull()?.let(::value)?.let { HtmlText.inline(it) }?.ifEmpty { null }

        val ingredients = (props("recipeIngredient") + props("ingredients"))
            .map { HtmlText.inline(value(it)) }
            .filter { it.isNotEmpty() }
            .take(MAX_LINES)
        val steps = props("recipeInstructions").flatMap { element ->
            val inner = element.select("[itemprop]").filter { hasProp(it, "text") }
            val lines = if (inner.isNotEmpty()) inner.map { it.text() } else HtmlText.blockText(element).lines()
            lines.map { RecipeJsonLdReader.withoutNumber(it.trim()) }
        }.filter { it.isNotEmpty() }.take(MAX_LINES)
        val images = props("image").mapNotNull { element ->
            val link = when {
                element.hasAttr("content") -> element.attr("abs:content").ifEmpty { element.attr("content") }
                element.hasAttr("src") -> element.attr("abs:src")
                element.hasAttr("href") -> element.attr("abs:href")
                else -> element.text()
            }
            RecipeJsonLdReader.absolute(link, document.location())
        }.distinct()
        val yieldText = first("recipeYield")
        val servings = RecipeYield.parse(yieldText)
        val prep = RecipeDuration.minutes(first("prepTime"))
        val cook = RecipeDuration.minutes(first("cookTime"))
        return WebRecipe(
            title = first("name"),
            description = first("description"),
            yieldText = yieldText,
            servings = servings?.count,
            servingsUnit = servings?.unit,
            ingredients = ingredients,
            steps = steps,
            imageUrls = images,
            prepMinutes = prep,
            totalMinutes = RecipeDuration.minutes(first("totalTime"))
                ?: if (prep != null || cook != null) (prep ?: 0) + (cook ?: 0) else null,
            language = RecipeJsonLdReader.language(first("inLanguage")),
        ).takeIf { it.hasContent }
    }

    /** Titel aus den Angaben für soziale Netzwerke, sonst aus dem Titel des Fensters oder der Hauptüberschrift. */
    private fun pageTitle(document: Document): String? {
        val candidates = listOfNotNull(
            meta(document, "og:title"),
            document.title(),
            document.selectFirst("h1")?.text(),
        ).map { HtmlText.inline(it) }.filter { it.isNotEmpty() }
        val title = candidates.firstOrNull() ?: return null
        // „Spaghetti Carbonara | Kochportal“ → „Spaghetti Carbonara“, wenn der Rest wie der Name des Portals aussieht.
        val site = meta(document, "og:site_name")?.let { HtmlText.inline(it) }
        val parts = title.split(TITLE_SEPARATOR)
        return when {
            parts.size > 1 && site != null && parts.last().contains(site, ignoreCase = true) -> parts.dropLast(1).joinToString(" – ")
            else -> title
        }.trim().ifEmpty { null }
    }

    /**
     * Sichtbarer Text des Hauptteils: ohne Menüs, Kopf- und Fußzeilen, Formulare und Skripte. Gewählt wird der
     * engste Bereich, in dem das Rezept mit seiner Überschrift „Zutaten“ steht – „article“, sonst „main“, sonst die
     * ganze Seite. Manche Baukästen für Internetseiten (z. B. Webflow) setzen „article“ nur um einzelne Textblöcke
     * wie die Zutatenliste; deren Überschrift und die Zubereitung stehen dann außerhalb.
     */
    private fun mainText(document: Document): String {
        val copy = document.clone()
        copy.select(
            "script, style, noscript, template, svg, iframe, nav, header, footer, aside, form, button, select, " +
                "[hidden], [aria-hidden=true], [role=navigation], [role=banner], [role=contentinfo]",
        ).remove()
        val areas = (copy.select("article").take(MAX_ARTICLES) + listOfNotNull(copy.selectFirst("main, [role=main]"), copy.body()))
            .distinct()
        val texts = areas.asSequence().map { HtmlText.blockText(it).take(MAX_TEXT) }
        return withoutTrailingSections(texts.firstOrNull { looksLikeRecipe(it) } ?: texts.first())
    }

    /**
     * Kommentare und weitere Rezepte unter dem Rezept gehören nicht dazu: Der Text endet an ihrer Überschrift,
     * wenn sie nach den Überschriften für Zutaten und Zubereitung steht.
     */
    internal fun withoutTrailingSections(text: String): String {
        val lines = text.lines()
        val ingredients = lines.indexOfFirst { RecipeTextParser.isIngredientHeading(it) }
        if (ingredients < 0) return text
        val steps = lines.indexOfFirst { RecipeTextParser.isStepsHeading(it) }
        val end = (maxOf(ingredients, steps) + 1 until lines.size).firstOrNull { TRAILING_SECTION.matches(lines[it].trim()) }
            ?: return text
        return lines.subList(0, end).joinToString("\n").trim()
    }

    private fun meta(document: Document, property: String): String? =
        document.select("meta[content]")
            .firstOrNull { it.attr("property").equals(property, ignoreCase = true) || it.attr("name").equals(property, ignoreCase = true) }
            ?.attr("content")?.trim()?.ifEmpty { null }

    private const val MAX_SCRIPTS = 30
    private const val MAX_ARTICLES = 10
    private const val MAX_LINES = 200
    /** So viel nimmt auch „Aus Text übernehmen“ an. */
    private const val MAX_TEXT = 50_000
    /** „Kommentare“, „Comments & Ratings“, „Weitere Rezepte“, „You may also like“ … in den Sprachen der App und der Texterkennung. */
    private val TRAILING_SECTION = Regex(
        "^(comments?|kommentare?|commenti|commentaires?|comentarios?|leave a (reply|comment)|schreibe? einen kommentar|" +
            "(other|more|related|similar) recipes|you (may|might) also like|(weitere|ähnliche|andere|mehr) rezepte|" +
            "das könnte (dir|ihnen|euch) auch gefallen|altre ricette|ricette (correlate|simili)|autres recettes|" +
            "recettes similaires|otras recetas|recetas (relacionadas|similares))\\b.{0,30}$",
        RegexOption.IGNORE_CASE,
    )
    private val TITLE_SEPARATOR = Regex("\\s+[|–—-]\\s+")
    private val WHITESPACE = Regex("\\s+")
    private val RECIPE_TYPE = Regex("(?i)(https?://)?(www\\.)?schema\\.org/Recipe")
}
