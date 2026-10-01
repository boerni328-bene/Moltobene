package com.moltobene.app.ui.licenses

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LicensesUiState {
    data object Loading : LicensesUiState
    data object Error : LicensesUiState

    /** Lizenztexte je Datei. */
    data class Content(val texts: Map<String, String>) : LicensesUiState
}

/** Lädt die Lizenztexte aus den mitgelieferten Dateien (nicht auf dem Hauptthread). */
class LicensesViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext

    var state by mutableStateOf<LicensesUiState>(LicensesUiState.Loading)
        private set

    init {
        viewModelScope.launch {
            state = try {
                val texts = withContext(Dispatchers.IO) {
                    LICENSE_GROUPS.associate { group ->
                        group.file to appContext.assets.open("licenses/${group.file}").bufferedReader().use { it.readText() }
                    }
                }
                LicensesUiState.Content(texts)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LicensesUiState.Error
            }
        }
    }
}
