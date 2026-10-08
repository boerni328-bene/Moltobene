package com.moltobene.app.data

import com.moltobene.app.data.db.IngredientEntity
import com.moltobene.app.data.db.RecipeDao
import com.moltobene.app.data.db.RecipeEntity
import com.moltobene.app.data.db.RecipePhotoEntity
import com.moltobene.app.data.db.RecipeRows
import com.moltobene.app.data.db.RecipeSearchEntity
import com.moltobene.app.data.db.RecipeSummaryRow
import com.moltobene.app.data.db.RecipeTagEntity
import com.moltobene.app.data.db.RecipeWithDetails
import com.moltobene.app.data.db.StepEntity
import com.moltobene.app.data.photos.PhotoStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Zentrale Stelle für Speichern und Laden von Rezepten. */
class RecipeRepository(
    private val dao: RecipeDao,
    private val photoStore: PhotoStore,
) {

    fun observeSummaries(query: String): Flow<List<RecipeSummary>> {
        val source = if (query.isBlank()) {
            dao.observeSummaries()
        } else {
            val match = SearchQuery.toFtsMatch(query) ?: return flowOf(emptyList())
            dao.observeSearch(match)
        }
        return source.map { rows -> rows.map { it.toSummary() } }
    }

    fun observeRecipe(id: String): Flow<Recipe?> = dao.observeDetails(id).map { it?.toRecipe() }

    suspend fun getRecipe(id: String): Recipe? = dao.getDetails(id)?.toRecipe()

    suspend fun getAll(): List<Recipe> = dao.getAllDetails().map { it.toRecipe() }

    suspend fun updatedAtById(): Map<String, Long> = dao.getAllUpdatedAt().associate { it.id to it.updatedAt }

    /** Zahl der Rezepte, die seit [since] hinzugefügt oder geändert wurden; aktualisiert sich von selbst. */
    fun observeChangedSince(since: Long): Flow<Int> = dao.observeChangedSince(since)

    suspend fun save(recipe: Recipe) = dao.save(recipe.toRows())

    /** Speichert alle Rezepte in einem Schritt – schlägt einer fehl, bleibt die Sammlung unverändert. */
    suspend fun saveAll(recipes: List<Recipe>) = dao.saveAll(recipes.map { it.toRows() })

    /** Löscht das Rezept und seine Fotodateien. */
    suspend fun delete(id: String) {
        val photoIds = dao.getDetails(id)?.photos?.map { it.photoId }.orEmpty()
        dao.delete(id)
        photoIds.forEach { photoStore.delete(it) }
    }

    /** Zuletzt genutzte Bücher und andere Quellen ohne Link – als Vorschlag beim Übernehmen (#40). */
    suspend fun recentSourceNames(limit: Int = 3): List<String> = dao.getRecentSourceNames(limit)

    /** Entfernt Fotodateien, die zu keinem Rezept mehr gehören (z. B. nach einem Abbruch). */
    suspend fun cleanUpUnusedPhotos() {
        photoStore.deleteUnused(referenced = dao.getAllPhotoIds().toSet())
    }
}

private fun RecipeSummaryRow.toSummary() = RecipeSummary(id = id, title = title, isDraft = isDraft, photoId = photoId)

private fun RecipeWithDetails.toRecipe() = Recipe(
    id = recipe.id,
    title = recipe.title,
    language = recipe.language,
    servings = recipe.servings,
    servingsUnit = recipe.servingsUnit,
    source = if (recipe.sourceType == null && recipe.sourceName == null && recipe.sourceUrl == null) {
        null
    } else {
        RecipeSource(
            type = SourceType.fromKey(recipe.sourceType),
            name = recipe.sourceName,
            url = recipe.sourceUrl,
            page = recipe.sourcePage,
        )
    },
    videoUrl = recipe.videoUrl,
    notes = recipe.notes,
    favorite = recipe.favorite,
    isDraft = recipe.isDraft,
    prepMinutes = recipe.prepMinutes,
    totalMinutes = recipe.totalMinutes,
    originalText = recipe.originalText,
    ingredients = ingredients.sortedBy { it.position }
        .map { Ingredient(text = it.text, isHeading = it.isHeading, quantity = it.quantity, unit = it.unit) },
    steps = steps.sortedBy { it.position }.map { it.text },
    tags = tags.map { Tag(name = it.name, predefinedKey = it.predefinedKey) },
    photoIds = photos.filter { it.kind != RecipePhotoEntity.KIND_PAGE }.sortedBy { it.position }.map { it.photoId },
    pageIds = photos.filter { it.kind == RecipePhotoEntity.KIND_PAGE }.sortedBy { it.position }.map { it.photoId },
    createdAt = recipe.createdAt,
    updatedAt = recipe.updatedAt,
)

private fun Recipe.toRows(): RecipeRows = RecipeRows(
    recipe = RecipeEntity(
        id = id,
        title = title,
        language = language,
        servings = servings,
        servingsUnit = servingsUnit,
        sourceType = source?.type?.key,
        sourceName = source?.name,
        sourceUrl = source?.url,
        sourcePage = source?.page,
        videoUrl = videoUrl,
        notes = notes,
        favorite = favorite,
        isDraft = isDraft,
        prepMinutes = prepMinutes,
        totalMinutes = totalMinutes,
        originalText = originalText,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ),
    ingredients = ingredients.mapIndexed { index, it ->
        IngredientEntity(
            recipeId = id,
            position = index,
            text = it.text,
            isHeading = it.isHeading,
            quantity = it.quantity,
            unit = it.unit,
        )
    },
    steps = steps.mapIndexed { index, text -> StepEntity(recipeId = id, position = index, text = text) },
    photos = photoIds.distinct().mapIndexed { index, photoId ->
        RecipePhotoEntity(photoId = photoId, recipeId = id, position = index, kind = RecipePhotoEntity.KIND_PHOTO)
    } + pageIds.distinct().filter { it !in photoIds }.mapIndexed { index, photoId ->
        RecipePhotoEntity(photoId = photoId, recipeId = id, position = index, kind = RecipePhotoEntity.KIND_PAGE)
    },
    tags = tags.distinctBy { it.name }.map { RecipeTagEntity(recipeId = id, name = it.name, predefinedKey = it.predefinedKey) },
    search = RecipeSearchEntity(
        recipeId = id,
        title = title,
        ingredients = ingredients.joinToString("\n") { it.text },
        notes = notes,
    ),
)
