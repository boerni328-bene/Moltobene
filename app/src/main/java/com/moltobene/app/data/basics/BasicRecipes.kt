package com.moltobene.app.data.basics

import com.moltobene.app.data.PredefinedTags
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.RecipeTitle
import com.moltobene.app.data.RecipeText
import com.moltobene.app.data.SourceType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Mitgelieferte Grundrezepte (#82): eine JSON-Datei in `assets/basics/` mit Version und Kennungen, je Rezept ein eigens
 * geschriebener deutscher und englischer Text (nicht maschinell übersetzt). Sie liegen nicht in der Datenbank oder in
 * der Sicherung, bis jemand ein Grundrezept übernimmt; dann wird daraus ein ganz normales Rezept mit der Quelle
 * „Moltobene“. Inhalt: nur Tatsachen und Verhältnisse in eigenem Wortlaut (Quellenliste unter `docs/recherche/`).
 */
object BasicRecipes {
    const val SOURCE_NAME = "Moltobene"
    const val ASSET = "basics/basics.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private class File(val version: Int, val recipes: List<BasicRecipe>)

    /** Das Grundrezept in allen Sprachen. [texts] ist nach Sprache („de“, „en“) geordnet. */
    @Serializable
    data class BasicRecipe(
        val id: String,
        val tags: List<String> = emptyList(),
        val servings: Int,
        val servingsUnit: String? = null,
        val prepMinutes: Int? = null,
        val totalMinutes: Int? = null,
        val texts: Map<String, BasicText>,
    )

    /**
     * Zutaten wie im Eingabefeld, eine je Zeile; eine Zeile mit Doppelpunkt am Ende ist eine Zwischenüberschrift.
     * [summary] ist ein Satz für die Auswahl.
     */
    @Serializable
    data class BasicText(
        val title: String,
        val summary: String,
        val ingredients: List<String>,
        val steps: List<String>,
        val notes: String = "",
    )

    fun parse(text: String): List<BasicRecipe> = json.decodeFromString<File>(text).recipes

    /** Deutsch für deutsche Oberfläche, sonst Englisch (Rückfallsprache). */
    fun languageFor(deviceLanguage: String): String = if (deviceLanguage.lowercase().startsWith("de")) "de" else "en"

    fun text(basic: BasicRecipe, language: String): BasicText =
        basic.texts[languageFor(language)] ?: basic.texts.getValue("en")

    /** Das normale Rezept, das aus dem Grundrezept wird: frei bearbeitbar, umrechenbar, übersetzbar. */
    fun toRecipe(basic: BasicRecipe, language: String, id: String, now: Long): Recipe {
        val code = languageFor(language)
        val text = text(basic, code)
        return Recipe(
            id = id,
            title = text.title,
            language = code,
            servings = basic.servings,
            servingsUnit = basic.servingsUnit,
            source = RecipeSource(type = SourceType.OTHER, name = SOURCE_NAME),
            notes = text.notes,
            prepMinutes = basic.prepMinutes,
            totalMinutes = basic.totalMinutes,
            ingredients = RecipeText.parseIngredients(text.ingredients.joinToString("\n")),
            steps = text.steps.map { it.trim() }.filter { it.isNotEmpty() },
            tags = basic.tags.filter(PredefinedTags::isKnown).map(PredefinedTags::tag),
            createdAt = now,
            updatedAt = now,
        )
    }

    /**
     * Das Rezept der Sammlung, das aus diesem Grundrezept stammt, oder null. Gesucht wird unter den Rezepten mit der
     * Quelle „Moltobene“ nach dem Titel in einer der Sprachen; ein umbenanntes Rezept gilt nicht als Duplikat.
     */
    fun existingRecipe(basic: BasicRecipe, fromSource: Collection<RecipeTitle>): RecipeTitle? {
        val own = basic.texts.values.map { it.title.trim().lowercase() }.toSet()
        return fromSource.firstOrNull { it.title.trim().lowercase() in own }
    }
}
