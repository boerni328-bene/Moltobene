package com.moltobene.app.data.backup

import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import com.moltobene.app.data.Tag
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Aufbau der Sicherungsdatei (ZIP):
 *  - manifest.json  – Kennung, Formatversion, App-Version, Datum, Anzahl
 *  - recipes.json   – alle Rezepte
 *  - photos/<id>.jpg und photos/<id>_thumb.jpg – Fotos der Gerichte und Originalseiten
 *
 * Seit 0.7.0 nennt ein Rezept seine Originalseiten in „pages“. Ältere Sicherungen haben das Feld nicht
 * und bleiben lesbar; ältere App-Versionen übergehen es und übernehmen die Rezepte ohne Originalseiten.
 *
 * Regel: Alte Sicherungen müssen immer lesbar bleiben. Neue Felder nur mit Standardwert ergänzen;
 * bei einer inkompatiblen Änderung [CURRENT_FORMAT_VERSION] erhöhen und das alte Format weiter lesen.
 */
object BackupFormat {
    const val FORMAT_ID = "moltobene-backup"
    const val CURRENT_FORMAT_VERSION = 1
    const val MANIFEST_ENTRY = "manifest.json"
    const val RECIPES_ENTRY = "recipes.json"
    const val PHOTO_FOLDER = "photos/"

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun photoEntry(photoId: String) = "$PHOTO_FOLDER$photoId.jpg"

    fun thumbEntry(photoId: String) = "$PHOTO_FOLDER${photoId}_thumb.jpg"
}

@Serializable
data class BackupManifest(
    val format: String = BackupFormat.FORMAT_ID,
    val formatVersion: Int = BackupFormat.CURRENT_FORMAT_VERSION,
    val appVersion: String = "",
    val createdAt: Long = 0,
    val recipeCount: Int = 0,
)

@Serializable
data class BackupRecipes(val recipes: List<BackupRecipe> = emptyList())

@Serializable
data class BackupRecipe(
    val id: String,
    val title: String,
    val language: String? = null,
    val servings: Int? = null,
    val servingsUnit: String? = null,
    val source: BackupSource? = null,
    val notes: String = "",
    val favorite: Boolean = false,
    val draft: Boolean = false,
    val prepMinutes: Int? = null,
    val totalMinutes: Int? = null,
    val originalText: String? = null,
    val ingredients: List<BackupIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
    val photos: List<String> = emptyList(),
    val pages: List<String> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class BackupSource(
    val type: String = SourceType.OTHER.key,
    val name: String? = null,
    val url: String? = null,
    val page: String? = null,
)

@Serializable
data class BackupIngredient(
    val text: String,
    val heading: Boolean = false,
    val quantity: Double? = null,
    val unit: String? = null,
)

/** [key] ist die feste Kennung mitgelieferter Schlagwörter, [name] der lesbare Text. */
@Serializable
data class BackupTag(val name: String, val key: String? = null)

fun Recipe.toBackup() = BackupRecipe(
    id = id,
    title = title,
    language = language,
    servings = servings,
    servingsUnit = servingsUnit,
    source = source?.let { BackupSource(type = it.type.key, name = it.name, url = it.url, page = it.page) },
    notes = notes,
    favorite = favorite,
    draft = isDraft,
    prepMinutes = prepMinutes,
    totalMinutes = totalMinutes,
    originalText = originalText,
    ingredients = ingredients.map { BackupIngredient(text = it.text, heading = it.isHeading, quantity = it.quantity, unit = it.unit) },
    steps = steps,
    tags = tags.map { BackupTag(name = it.name, key = it.predefinedKey) },
    photos = photoIds,
    pages = pageIds,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun BackupRecipe.toRecipe() = Recipe(
    id = id,
    title = title,
    language = language,
    servings = servings,
    servingsUnit = servingsUnit,
    source = source?.let { RecipeSource(type = SourceType.fromKey(it.type), name = it.name, url = it.url, page = it.page) },
    notes = notes,
    favorite = favorite,
    isDraft = draft,
    prepMinutes = prepMinutes,
    totalMinutes = totalMinutes,
    originalText = originalText,
    ingredients = ingredients.map { Ingredient(text = it.text, isHeading = it.heading, quantity = it.quantity, unit = it.unit) },
    steps = steps,
    tags = tags.map { Tag(name = it.name, predefinedKey = it.key) },
    photoIds = photos,
    pageIds = pages,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
