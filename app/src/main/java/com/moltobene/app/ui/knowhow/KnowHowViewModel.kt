package com.moltobene.app.ui.knowhow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.data.knowhow.KnowHow
import com.moltobene.app.data.knowhow.KnowHowStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface KnowHowUiState {
    data object Loading : KnowHowUiState
    data object Error : KnowHowUiState
    data class Content(val articles: List<KnowHow.Article>) : KnowHowUiState
}

class KnowHowViewModel(store: KnowHowStore) : ViewModel() {
    private val mutableState = MutableStateFlow<KnowHowUiState>(KnowHowUiState.Loading)
    val state: StateFlow<KnowHowUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            mutableState.value = runCatching { KnowHowUiState.Content(store.load()) }.getOrDefault(KnowHowUiState.Error)
        }
    }
}
