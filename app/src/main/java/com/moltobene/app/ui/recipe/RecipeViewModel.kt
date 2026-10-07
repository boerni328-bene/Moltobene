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
    private val savedStateHandle: SavedStateHandle,
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

    /**
     * Beim Kochen (#56): abgehakte Zutaten. Gemerkt werden Stelle und Text, damit nach einer Änderung am Rezept
     * keine andere Zutat abgehakt erscheint. Liegt im [SavedStateHandle] und übersteht so das Drehen des Handys
     * und das Beenden der App durch Android.
     */
    val checkedIngredients: StateFlow<List<String>> = savedStateHandle.getStateFlow(KEY_CHECKED, arrayListOf<String>())

    /** Der aktuelle Schritt, gemerkt wie die Zutaten; null, wenn keiner markiert ist. */
    val currentStep: StateFlow<String?> = savedStateHandle.getStateFlow<String?>(KEY_STEP, null)

    /** Angezeigte Portionen; null heißt: wie im Rezept. Das gespeicherte Rezept ändert sich nie. */
    val shownServings: StateFlow<Int?> = savedStateHandle.getStateFlow<Int?>(KEY_SERVINGS, null)

    fun toggleIngredient(index: Int, text: String) {
        val key = progressKey(index, text)
        val checked = ArrayList(checkedIngredients.value)
        if (!checked.remove(key)) checked += key
        savedStateHandle[KEY_CHECKED] = checked
    }

    /** Markiert einen Schritt als aktuellen Schritt; nochmals antippen hebt die Markierung auf. */
    fun toggleCurrentStep(index: Int, text: String) {
        val key = progressKey(index, text)
        savedStateHandle[KEY_STEP] = if (currentStep.value == key) null else key
    }

    fun setServings(servings: Int?) {
        savedStateHandle[KEY_SERVINGS] = servings
    }

    /** „Abhaken zurücksetzen“: keine Zutat abgehakt, kein aktueller Schritt. */
    fun resetProgress() {
        savedStateHandle[KEY_CHECKED] = arrayListOf<String>()
        savedStateHandle[KEY_STEP] = null
    }

    /** „Originalseiten ansehen“ (#38). */
    val viewer = PageViewerModel(viewModelScope) { index ->
        val pageId = (state.value as? RecipeUiState.Content)?.recipe?.pageIds?.getOrNull(index)
            ?: throw IOException("Seite fehlt")
        photoStore.loadPage(Uri.fromFile(photoStore.photoFile(pageId)), CropArea.WHOLE_PAGE)
    }

    /** Das Foto des Rezepts als Vollbild, ganz und zum Vergrößern. */
    val photoViewer = PageViewerModel(viewModelScope) {
        val photo = (state.value as? RecipeUiState.Content)?.photo ?: throw IOException("Foto fehlt")
        photoStore.loadPage(Uri.fromFile(photo), CropArea.WHOLE_PAGE)
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
                    sharer.prepareFile(recipe, labels)
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

    companion object {
        private const val KEY_CHECKED = "cooking_checked"
        private const val KEY_STEP = "cooking_step"
        private const val KEY_SERVINGS = "cooking_servings"

        /** Kennung einer Zeile beim Kochen: Stelle und Text. */
        fun progressKey(index: Int, text: String): String = "$index:${text.hashCode()}"
    }
}
