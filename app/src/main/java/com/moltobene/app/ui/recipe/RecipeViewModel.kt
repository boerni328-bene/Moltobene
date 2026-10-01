package com.moltobene.app.ui.recipe

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.ocr.CropArea
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.data.share.PreparedShare
import com.moltobene.app.data.share.RecipeSharer
import com.moltobene.app.data.share.ShareLabels
import com.moltobene.app.ui.components.PageViewerModel
import com.moltobene.app.ui.navigation.RecipeRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

sealed interface RecipeUiState {
    data object Loading : RecipeUiState
    data object NotFound : RecipeUiState
    data object Error : RecipeUiState
    data class Content(val recipe: Recipe, val photo: File?) : RecipeUiState
}

sealed interface ShareEvent {
    data class Ready(val share: PreparedShare) : ShareEvent
    data object Failed : ShareEvent
}

class RecipeViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val photoStore: PhotoStore,
    private val sharer: RecipeSharer,
) : ViewModel() {

    val recipeId: String = savedStateHandle.toRoute<RecipeRoute>().id

    val state: StateFlow<RecipeUiState> = repository.observeRecipe(recipeId)
        .map { recipe ->
            if (recipe == null) {
                RecipeUiState.NotFound
            } else {
                RecipeUiState.Content(recipe, recipe.photoIds.firstOrNull()?.let { photoStore.photoFile(it) })
            }
        }
        .catch { emit(RecipeUiState.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeUiState.Loading)

    /** „Originalseiten ansehen“ (#38). */
    val viewer = PageViewerModel(viewModelScope) { index ->
        val pageId = (state.value as? RecipeUiState.Content)?.recipe?.pageIds?.getOrNull(index)
            ?: throw IOException("Seite fehlt")
        photoStore.loadPage(Uri.fromFile(photoStore.photoFile(pageId)), CropArea.WHOLE_PAGE)
    }

    /** true, solange das Teilen vorbereitet wird. */
    var preparingShare by mutableStateOf(false)
        private set

    /** Ergebnis fürs Teilen; die Oberfläche öffnet damit das Teilen-Menü und setzt es zurück. */
    var shareEvent by mutableStateOf<ShareEvent?>(null)

    /** Teilt das Rezept als Text mit Foto oder ([asFile]) als Rezeptdatei. */
    fun share(asFile: Boolean, labels: ShareLabels) {
        val recipe = (state.value as? RecipeUiState.Content)?.recipe ?: return
        if (preparingShare) return
        preparingShare = true
        viewModelScope.launch {
            shareEvent = try {
                val prepared = if (asFile) {
                    sharer.prepareFile(recipe, labels.untitled)
                } else {
                    sharer.prepareText(recipe, labels)
                }
                ShareEvent.Ready(prepared)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ShareEvent.Failed
            }
            preparingShare = false
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(recipeId)
            onDeleted()
        }
    }
}
