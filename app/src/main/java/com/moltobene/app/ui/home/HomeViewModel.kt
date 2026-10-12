package com.moltobene.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.data.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/**
 * Startseite (#81): Die Kacheln stehen sofort, nur die Anzahl der Rezepte wird nach dem ersten Bild gelesen – eine
 * einzige Zählung, ohne Rezepte oder Fotos zu laden.
 */
class HomeViewModel(repository: RecipeRepository) : ViewModel() {

    /** Anzahl der Rezepte; null, solange sie noch nicht gelesen ist (dann steht keine Zahl da). */
    val recipeCount: StateFlow<Int?> = repository.observeCount()
        .catch { }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
