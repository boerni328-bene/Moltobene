package com.moltobene.app.ui.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.R
import com.moltobene.app.data.backup.BackupManager
import com.moltobene.app.data.backup.BackupReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Idle : SettingsUiState

    /** Es läuft gerade etwas; [label] beschreibt was (Text-Ressource). */
    data class Working(val label: Int) : SettingsUiState

    data class ConfirmRestore(val preview: BackupManager.RestorePreview) : SettingsUiState
}

sealed interface SettingsEvent {
    data class BackupDone(val count: Int) : SettingsEvent
    data object BackupFailed : SettingsEvent
    data class RestoreDone(val restored: Int) : SettingsEvent
    data class RestoreProblem(val problem: BackupReader.Problem) : SettingsEvent
    data object RestoreFailed : SettingsEvent
}

class SettingsViewModel(private val backupManager: BackupManager) : ViewModel() {

    var state by mutableStateOf<SettingsUiState>(SettingsUiState.Idle)
        private set

    /** Ergebnis für die Meldungsleiste; wird nach dem Anzeigen zurückgesetzt. */
    var event by mutableStateOf<SettingsEvent?>(null)

    fun backup(uri: Uri) {
        state = SettingsUiState.Working(R.string.backup_running)
        viewModelScope.launch {
            event = try {
                SettingsEvent.BackupDone(backupManager.export(uri))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SettingsEvent.BackupFailed
            }
            state = SettingsUiState.Idle
        }
    }

    fun checkRestore(uri: Uri) {
        state = SettingsUiState.Working(R.string.restore_checking)
        viewModelScope.launch {
            state = try {
                when (val result = backupManager.prepareRestore(uri)) {
                    is BackupManager.PrepareResult.Ready -> SettingsUiState.ConfirmRestore(result.preview)
                    is BackupManager.PrepareResult.Failed -> {
                        event = SettingsEvent.RestoreProblem(result.problem)
                        SettingsUiState.Idle
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                event = SettingsEvent.RestoreProblem(BackupReader.Problem.DAMAGED)
                SettingsUiState.Idle
            }
        }
    }

    fun confirmRestore() {
        val preview = (state as? SettingsUiState.ConfirmRestore)?.preview ?: return
        state = SettingsUiState.Working(R.string.restore_running)
        viewModelScope.launch {
            event = try {
                val result = backupManager.applyRestore(preview)
                SettingsEvent.RestoreDone(result.added + result.replaced)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SettingsEvent.RestoreFailed
            }
            state = SettingsUiState.Idle
        }
    }

    fun cancelRestore() {
        (state as? SettingsUiState.ConfirmRestore)?.let { backupManager.discardRestore(it.preview) }
        state = SettingsUiState.Idle
    }

    override fun onCleared() {
        (state as? SettingsUiState.ConfirmRestore)?.let { backupManager.discardRestore(it.preview) }
    }
}
