package com.moltobene.app.data

/**
 * Ein Rezept, so wie es die App verwendet (siehe Issue #28).
 * Aufbau so nah wie möglich an schema.org/Recipe, damit Übernehmen, Teilen und Sichern dieselbe Sprache sprechen.
 */
data class Recipe(
    /** Weltweit eindeutige Kennung (UUID), damit beim Wiederherstellen keine Doppelten entstehen. */
    val id: String,
    val title: String,
    /** Sprache des Rezepts als Kürzel wie „de“ oder „it“ (schema.org inLanguage). */
    val language: String? = null,
    val servings: Int? = null,
    /** Freie Angabe zu den Portionen, z. B. „Stück“ oder „Springform 26 cm“. */
    val servingsUnit: String? = null,
    val source: RecipeSource? = null,
    val notes: String = "",
    val favorite: Boolean = false,
    val isDraft: Boolean = false,
    val prepMinutes: Int? = null,
    val totalMinutes: Int? = null,
    /** Unveränderter Originaltext bzw. Link einer Übernahme (#35). */
    val originalText: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val tags: List<Tag> = emptyList(),
    /** Kennungen der Fotos; das erste ist das Hauptfoto. */
    val photoIds: List<String> = emptyList(),
    /** Originalseiten (#38): Fotos der Vorlage, z. B. der Kochbuchseite – getrennt vom Foto des Gerichts, nie geteilt. */
    val pageIds: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Eine Zutatenzeile. [text] ist immer der Originaltext; [quantity] und [unit] sind
 * zusätzlich erkannte Angaben (werden ab dem Kochmodus gefüllt) und dürfen leer bleiben.
 */
data class Ingredient(
    val text: String,
    val isHeading: Boolean = false,
    val quantity: Double? = null,
    val unit: String? = null,
)

data class RecipeSource(
    val type: SourceType,
    val name: String? = null,
    val url: String? = null,
    val page: String? = null,
)

enum class SourceType(val key: String) {
    WEB("web"), BOOK("book"), PERSON("person"), OWN("own"), OTHER("other");

    companion object {
        fun fromKey(key: String?): SourceType = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** Schlagwort. Mitgelieferte Vorschläge haben eine feste englische Kennung [predefinedKey]. */
data class Tag(val name: String, val predefinedKey: String? = null)

/** Kurzfassung für die Liste der Sammlung. */
data class RecipeSummary(
    val id: String,
    val title: String,
    val isDraft: Boolean,
    val photoId: String?,
)

object RecipeIds {
    private val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    fun newId(): String = java.util.UUID.randomUUID().toString()

    fun isValid(id: String): Boolean = UUID_PATTERN.matches(id)
}
