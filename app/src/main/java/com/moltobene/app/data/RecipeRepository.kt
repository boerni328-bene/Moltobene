package com.moltobene.app.data

import com.moltobene.app.data.db.DoughRunEntity
import com.moltobene.app.data.db.IngredientEntity
import com.moltobene.app.data.db.PhaseEntity
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
import kotlin.random.Random

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

    /** Kurzfassungen der genannten Rezepte in der Reihenfolge von [ids]; gelöschte fehlen. */
    suspend fun getSummaries(ids: List<String>): List<RecipeSummary> {
        if (ids.isEmpty()) return emptyList()
        val byId = dao.getSummaries(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toSummary() }
    }

    /**
     * „Was koche ich heute?“ (#81): ein zufälliges Rezept, kein Entwurf, zwei von drei Mal ein Favorit, wenn es welche
     * gibt. [exclude] ist das zuletzt gezeigte Rezept; gibt es kein anderes, kommt es noch einmal.
     */
    suspend fun suggestRecipe(exclude: String? = null, favoriteChance: Int = 2, random: Int = Random.nextInt(3)): RecipeSummary? {
        val skip = exclude.orEmpty()
        val preferFavorite = random < favoriteChance
        val row = (if (preferFavorite) dao.getRandomSummary(skip, true) else null)
            ?: dao.getRandomSummary(skip, false)
            ?: dao.getRandomSummary("", false)
        return row?.toSummary()
    }

    /** Schlagwörter je Rezept (Kennung des Rezepts → Schlagwörter) für den Filter der Sammlung (#57). */
    fun observeTagsByRecipe(): Flow<Map<String, List<Tag>>> = dao.observeTagLinks().map { links ->
        links.groupBy({ it.recipeId }, { Tag(name = it.name, predefinedKey = it.predefinedKey) })
    }

    /** Titel der Rezepte, deren Quelle den Namen [sourceName] hat. */
    suspend fun titlesBySource(sourceName: String): List<RecipeTitle> =
        dao.getTitlesBySourceName(sourceName).map { RecipeTitle(it.id, it.title) }

    fun observeRecipe(id: String): Flow<Recipe?> = dao.observeDetails(id).map { it?.toRecipe() }

    suspend fun getRecipe(id: String): Recipe? = dao.getDetails(id)?.toRecipe()

    suspend fun getAll(): List<Recipe> = dao.getAllDetails().map { it.toRecipe() }

    suspend fun updatedAtById(): Map<String, Long> = dao.getAllUpdatedAt().associate { it.id to it.updatedAt }

    /** Zahl der Rezepte, die seit [since] hinzugefügt oder geändert wurden; aktualisiert sich von selbst. */
    fun observeChangedSince(since: Long): Flow<Int> = dao.observeChangedSince(since)

    /** Wie viele Rezepte es gibt – für die Startseite. */
    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun save(recipe: Recipe) = dao.save(recipe.toRows())

    /** Speichert alle Rezepte in einem Schritt – schlägt einer fehl, bleibt die Sammlung unverändert. */
    suspend fun saveAll(recipes: List<Recipe>) = dao.saveAll(recipes.map { it.toRows() })

    /** Laufende Teige (#71), die zuletzt gestarteten zuerst. */
    fun observeRuns(): Flow<List<DoughRun>> = dao.observeRuns().map { rows -> rows.map { it.toRun() } }

    suspend fun saveRun(run: DoughRun) = dao.upsertRun(run.toRow())

    suspend fun deleteRun(id: String) = dao.deleteRun(id)

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

private fun RecipeSummaryRow.toSummary() = RecipeSummary(id = id, title = title, isDraft = isDraft, favorite = favorite, photoId = photoId, createdAt = createdAt, totalMinutes = totalMinutes, servings = servings)

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
    timeline = phases.sortedBy { it.position }.take(TimelinePhase.MAX_PHASES)
        .map { TimelinePhase.lenient(it.text, it.minMinutes, it.maxMinutes, PhasePlace.fromKey(it.place)) },
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
    phases = timeline.mapIndexed { index, it ->
        PhaseEntity(
            recipeId = id,
            position = index,
            text = it.text,
            minMinutes = it.minMinutes,
            maxMinutes = it.maxMinutes,
            place = it.place.key,
        )
    },
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

private fun DoughRunEntity.toRun() = DoughRun(
    id = id,
    recipeId = recipeId,
    startedAt = startedAt,
    doneAt = doneAt.split(',').mapNotNull { it.trim().toLongOrNull() }.take(TimelinePhase.MAX_PHASES),
)

private fun DoughRun.toRow() = DoughRunEntity(id = id, recipeId = recipeId, startedAt = startedAt, doneAt = doneAt.joinToString(","))
