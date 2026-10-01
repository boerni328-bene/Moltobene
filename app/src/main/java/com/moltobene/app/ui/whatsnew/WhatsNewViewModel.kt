package com.moltobene.app.ui.whatsnew

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.BuildConfig
import com.moltobene.app.data.AppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Entscheidet beim Start, ob „Neu in Version …“ erscheint, und merkt sich, wenn es erledigt ist. */
class WhatsNewViewModel(private val preferences: AppPreferences) : ViewModel() {

    var visible by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val show = try {
                WhatsNew.shouldShow(preferences.lastSeenVersionCode(), preferences.isFreshInstall())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (show) visible = true else markSeen()
        }
    }

    fun dismiss() {
        visible = false
        viewModelScope.launch { markSeen() }
    }

    private suspend fun markSeen() {
        try {
            preferences.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Nicht schlimm: Dann erscheint die Übersicht beim nächsten Start noch einmal.
        }
    }
}
