package com.moltobene.app.ui.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.ui.navigation.RecipeRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface RecipeUiState {
    data object Loading : RecipeUiState
    data object NotFound : RecipeUiState
    data object Error : RecipeUiState
    data class Content(val recipe: Recipe, val photo: File?) : RecipeUiState
}

class RecipeViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val photoStore: PhotoStore,
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

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(recipeId)
            onDeleted()
        }
    }
}
