package com.moltobene.app.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.ocr.PendingRecognition
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.data.translate.RecipeTranslations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    val thumbnail: File?,
)

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    data object Error : CollectionUiState
    data class Content(val items: List<RecipeListItem>, val query: String) : CollectionUiState
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

    val state: StateFlow<CollectionUiState> = _query
        .flatMapLatest { query ->
            repository.observeSummaries(query).map { summaries ->
                CollectionUiState.Content(
                    items = summaries.map { summary ->
                        RecipeListItem(
                            id = summary.id,
                            title = summary.title,
                            isDraft = summary.isDraft,
                            thumbnail = summary.photoId?.let { photoStore.thumbFile(it) },
                        )
                    },
                    query = query,
                ) as CollectionUiState
            }
        }
        .catch { emit(CollectionUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState.Loading)

    init {
        viewModelScope.launch {
            runCatching { repository.cleanUpUnusedPhotos() }
            runCatching { pendingRecognition.deleteLeftovers() }
            runCatching { translations.deleteUnused(repository.updatedAtById().keys) }
        }
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
