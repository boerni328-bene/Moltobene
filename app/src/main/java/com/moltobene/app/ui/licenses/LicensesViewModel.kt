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

    /** Lizenztexte je Datei, in Abschnitte zerlegt (manche Texte sind sehr lang). */
    data class Content(val texts: Map<String, List<String>>) : LicensesUiState
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
                        group.file to chunks(appContext.assets.open("licenses/${group.file}").bufferedReader().use { it.readText() })
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

/**
 * Zerlegt einen Lizenztext an Leerzeilen in Abschnitte von höchstens etwa [size] Zeichen. Die Hinweise von
 * ONNX Runtime sind über 300 000 Zeichen lang; als ein einziger Text würde ihr Anzeigen günstige Handys
 * sekundenlang aufhalten, in Abschnitten zeichnet die Liste nur, was sichtbar ist.
 */
internal fun chunks(text: String, size: Int = 3000): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    for (paragraph in text.replace("\r\n", "\n").split("\n\n")) {
        if (current.isNotEmpty() && current.length + paragraph.length > size) {
            result += current.toString()
            current.clear()
        }
        if (current.isNotEmpty()) current.append("\n\n")
        current.append(paragraph)
    }
    if (current.isNotBlank()) result += current.toString()
    return result
}
