package com.moltobene.app.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.RecipeRepository
import kotlinx.coroutines.flow.Flow
import com.moltobene.app.data.TagFilter
import com.moltobene.app.data.TagCount
import com.moltobene.app.data.Tag
import com.moltobene.app.data.ocr.PendingRecognition
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.data.translate.RecipeTranslations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class RecipeListItem(
    val id: String,
    val title: String,
    val isDraft: Boolean,
    val favorite: Boolean,
    val thumbnail: File?,
)

/** Welche Rezepte die Sammlung zeigt (neben der Suche). */
enum class CollectionFilter { ALL, FAVORITES, DRAFTS }

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    data object Error : CollectionUiState
    data class Content(
        val items: List<RecipeListItem>, val query: String,
        val filter: CollectionFilter,
        val hasFavorites: Boolean,
        val hasDrafts: Boolean,
        /** Schlagwörter in Gebrauch mit Anzahl (#57), unabhängig von Suche und Filter. */
        val tagCounts: List<TagCount> = emptyList(),
        /** Gewählte Schlagwörter (Namen); ein Rezept muss alle haben. */
        val selectedTags: Set<String> = emptySet(),
    ) : CollectionUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModel(
    private val repository: RecipeRepository,
    private val photoStore: PhotoStore,
    private val pendingRecognition: PendingRecognition,
    private val preferences: AppPreferences,
    private val translations: RecipeTranslations,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(CollectionFilter.ALL)
    val filter: StateFlow<CollectionFilter> = _filter.asStateFlow()

    private val _grid = MutableStateFlow(true)

    /** Raster (true) oder Liste; die Wahl bleibt gemerkt. */
    val grid: StateFlow<Boolean> = _grid.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())

    private val tagsByRecipe: Flow<Map<String, List<Tag>>> = repository.observeTagsByRecipe().catch { emit(emptyMap()) }

    val state: StateFlow<CollectionUiState> = combine(_query, _filter, _selectedTags) { query, filter, tags -> Triple(query, filter, tags) }
        .flatMapLatest { (query, filter, chosen) ->
            combine(repository.observeSummaries(query), tagsByRecipe) { summaries, tagsOf ->
                val tagCounts = TagFilter.counts(tagsOf)
                // Ein Schlagwort, das kein Rezept mehr hat, zählt nicht mehr als gewählt.
                val selected = chosen.filterTo(HashSet()) { name -> tagCounts.any { it.tag.name == name } }
                CollectionUiState.Content(
                    items = summaries.filter {
                        when (filter) {
                            CollectionFilter.ALL -> true
                            CollectionFilter.FAVORITES -> it.favorite
                            CollectionFilter.DRAFTS -> it.isDraft
                        } && TagFilter.matches(tagsOf[it.id].orEmpty(), selected)
                    }.map { summary ->
                        RecipeListItem(
                            id = summary.id,
                            title = summary.title,
                            isDraft = summary.isDraft,
                            favorite = summary.favorite,
                            thumbnail = summary.photoId?.let { photoStore.thumbFile(it) },
                        )
                    },
                    query = query,
                    filter = filter,
                    hasFavorites = summaries.any { it.favorite },
                    hasDrafts = summaries.any { it.isDraft },
                    tagCounts = tagCounts,
                    selectedTags = selected,
                ) as CollectionUiState
            }
        }
        .catch { emit(CollectionUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState.Loading)

    init {
        viewModelScope.launch { runCatching { preferences.collectionGrid() }.getOrNull()?.let { _grid.value = it } }
        viewModelScope.launch {
            runCatching { repository.cleanUpUnusedPhotos() }
            runCatching { pendingRecognition.deleteLeftovers() }
            runCatching { translations.deleteUnused(repository.updatedAtById().keys) }
        }
    }

    fun onFilterChange(value: CollectionFilter) {
        _filter.value = value
    }

    /** Schlagwort im Filter wählen oder abwählen (#57). */
    fun onToggleTag(name: String) {
        _selectedTags.value = _selectedTags.value.let { if (name in it) it - name else it + name }
    }

    fun onGridChange(value: Boolean) {
        _grid.value = value
        viewModelScope.launch { runCatching { preferences.setCollectionGrid(value) } }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Stand der letzten Sicherung und des ausgeblendeten Hinweises; null, solange er lädt. */
    private class BackupState(val lastBackupAt: Long, val hiddenAt: Int)

    private val backupState = MutableStateFlow<BackupState?>(null)

    /**
     * Ruhiger Hinweis zum Sichern (#51): Zahl der Rezepte, die seit der letzten Sicherung hinzugefügt oder geändert
     * wurden – ab [BACKUP_HINT_MIN]. Wurde er ausgeblendet, erscheint er erst nach so vielen weiteren wieder.
     * null = kein Hinweis.
     */
    val backupHint: StateFlow<Int?> = backupState
        .filterNotNull()
        .flatMapLatest { saved ->
            repository.observeChangedSince(saved.lastBackupAt).map { unsaved ->
                unsaved.takeIf { it >= saved.hiddenAt + BACKUP_HINT_MIN }
            }
        }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Beim Zurückkehren in die Sammlung neu lesen, z. B. nach dem Sichern in den Einstellungen. */
    fun refreshBackupHint() {
        viewModelScope.launch {
            backupState.value = runCatching {
                BackupState(preferences.lastBackup()?.at ?: 0L, preferences.backupHintHiddenAt())
            }.getOrNull()
        }
    }

    fun hideBackupHint(unsaved: Int) {
        viewModelScope.launch {
            runCatching { preferences.setBackupHintHiddenAt(unsaved) }
            refreshBackupHint()
        }
    }

    companion object {
        /** Ab so vielen ungesicherten Rezepten erscheint der Hinweis. */
        const val BACKUP_HINT_MIN = 10
    }
}
