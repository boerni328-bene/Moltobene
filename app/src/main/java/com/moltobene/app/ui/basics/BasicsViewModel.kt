package com.moltobene.app.ui.basics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.data.RecipeIds
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.basics.BasicRecipeStore
import com.moltobene.app.data.basics.BasicRecipes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BasicsUiState {
    data object Loading : BasicsUiState
    data object Error : BasicsUiState
    data class Content(val recipes: List<BasicRecipes.BasicRecipe>) : BasicsUiState
}

/** Was nach „Übernehmen“ passiert ist (#82). */
sealed interface BasicsEvent {
    /** Das Grundrezept ist jetzt ein Rezept der Sammlung. */
    data class Added(val basicId: String, val title: String, val recipeId: String) : BasicsEvent

    /** Es ist schon in der Sammlung; übernommen wird erst nach „Trotzdem übernehmen“. */
    data class Duplicate(val basicId: String, val title: String, val existingRecipeId: String) : BasicsEvent

    data object Failed : BasicsEvent
}

class BasicsViewModel(
    private val store: BasicRecipeStore,
    private val repository: RecipeRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow<BasicsUiState>(BasicsUiState.Loading)
    val state: StateFlow<BasicsUiState> = mutableState.asStateFlow()

    /** Ergebnis der letzten Übernahme; die Oberfläche zeigt es an und setzt es zurück. */
    var event by mutableStateOf<BasicsEvent?>(null)

    private var busy = false

    init {
        viewModelScope.launch {
            mutableState.value = runCatching { BasicsUiState.Content(store.load()) }.getOrDefault(BasicsUiState.Error)
        }
    }

    /**
     * Übernimmt ein Grundrezept als normales Rezept in [language] (Sprache der Oberfläche). Ist es schon in der Sammlung,
     * kommt zuerst [BasicsEvent.Duplicate]; mit [force] wird trotzdem übernommen.
     */
    fun adopt(basic: BasicRecipes.BasicRecipe, language: String, force: Boolean = false) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            event = try {
                val title = BasicRecipes.text(basic, language).title
                val existing = runCatching { repository.titlesBySource(BasicRecipes.SOURCE_NAME) }.getOrDefault(emptyList())
                val duplicate = BasicRecipes.existingRecipe(basic, existing)
                if (!force && duplicate != null) {
                    BasicsEvent.Duplicate(basic.id, title, duplicate.id)
                } else {
                    val id = RecipeIds.newId()
                    repository.save(BasicRecipes.toRecipe(basic, language, id, System.currentTimeMillis()))
                    BasicsEvent.Added(basic.id, title, id)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                BasicsEvent.Failed
            }
            busy = false
        }
    }
}
