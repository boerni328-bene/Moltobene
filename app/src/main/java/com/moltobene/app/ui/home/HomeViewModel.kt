package com.moltobene.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.dough.DoughRunPlanner
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.ui.collection.RecipeListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Startseite (#81): Die Kacheln stehen sofort, danach kommen nach dem ersten Bild die Anzahl der Rezepte (eine einzige
 * Zählung), „Was koche ich heute?“ (ein Rezept) und „Zuletzt geöffnet“ (höchstens fünf Rezepte) – nie die ganze
 * Sammlung und nie große Fotos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: RecipeRepository,
    private val photoStore: PhotoStore,
    private val preferences: AppPreferences,
) : ViewModel() {

    /** Anzahl der Rezepte; null, solange sie noch nicht gelesen ist (dann steht keine Zahl da). */
    val recipeCount: StateFlow<Int?> = repository.observeCount()
        .catch { }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val suggestionState = MutableStateFlow<RecipeListItem?>(null)

    /** Das vorgeschlagene Rezept; null, solange keines da ist oder die Sammlung leer ist. */
    val suggestion: StateFlow<RecipeListItem?> = suggestionState.asStateFlow()

    /** Die zuletzt geöffneten Rezepte, das neueste zuerst; gelöschte fehlen. */
    val recent: StateFlow<List<RecipeListItem>> = combine(
        preferences.recentRecipes.filterNotNull(),
        repository.observeCount().catch { emit(0) },
    ) { ids, _ -> ids }
        .mapLatest { ids -> runCatching { repository.getSummaries(ids).map(::toItem) }.getOrDefault(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Laufende Teige (#71) für die Startseite: das Rezept, die aktuelle Phase und wann es frühestens und spätestens weitergeht. */
    val running: StateFlow<List<RunningDough>> = repository.observeRuns()
        .mapLatest { runs ->
            runs.mapNotNull { run ->
                val recipe = repository.getRecipe(run.recipeId)?.takeIf { it.timeline.isNotEmpty() } ?: return@mapNotNull null
                val state = DoughRunPlanner.state(recipe.timeline, run)
                val time = state.phases.getOrNull(state.current)
                RunningDough(
                    recipeId = recipe.id,
                    title = recipe.title,
                    phase = recipe.timeline.getOrNull(state.current)?.text,
                    earliest = time?.endEarliest ?: 0L,
                    latest = time?.endLatest ?: 0L,
                )
            }
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { runCatching { preferences.loadRecentRecipes() } }
        // Ändert sich die Zahl der Rezepte (erstes Lesen, Hinzufügen, Löschen), gibt es einen neuen Vorschlag.
        viewModelScope.launch {
            repository.observeCount().catch { }.distinctUntilChanged().collect { count ->
                suggestionState.value = if (count > 0) pick(null) else null
            }
        }
    }

    /** „Anderes Rezept“. */
    fun suggestAnother() {
        viewModelScope.launch { suggestionState.value = pick(suggestionState.value?.id) ?: suggestionState.value }
    }

    private suspend fun pick(exclude: String?): RecipeListItem? =
        runCatching { repository.suggestRecipe(exclude)?.let(::toItem) }.getOrNull()

    private fun toItem(summary: com.moltobene.app.data.RecipeSummary) = RecipeListItem(
        id = summary.id,
        title = summary.title,
        isDraft = summary.isDraft,
        favorite = summary.favorite,
        thumbnail = summary.photoId?.let { photoStore.thumbFile(it) },
    )
}

/** Ein laufender Teig auf der Startseite; [phase] ist null, wenn alle Phasen abgeschlossen sind. */
data class RunningDough(val recipeId: String, val title: String, val phase: String?, val earliest: Long, val latest: Long)
