package com.moltobene.app.ui.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.R
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.Appearance
import com.moltobene.app.data.Palette
import com.moltobene.app.data.ScreenOn
import com.moltobene.app.data.StorageFull
import com.moltobene.app.data.ThemeMode
import com.moltobene.app.data.backup.BackupManager
import com.moltobene.app.data.backup.BackupReader
import com.moltobene.app.data.translate.LanguagePackManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Idle : SettingsUiState

    /** Es läuft gerade etwas; [label] beschreibt was (Text-Ressource). */
    data class Working(val label: Int) : SettingsUiState

    data class ConfirmRestore(val preview: BackupManager.RestorePreview) : SettingsUiState
}

sealed interface SettingsEvent {
    data class BackupDone(val count: Int) : SettingsEvent
    data class BackupFailed(val problem: BackupManager.BackupException.Problem) : SettingsEvent
    data class RestoreDone(val restored: Int) : SettingsEvent
    data class RestoreProblem(val problem: BackupReader.Problem) : SettingsEvent
    data object RestoreFailed : SettingsEvent
    data object LanguagePackDeleted : SettingsEvent
}

class SettingsViewModel(
    private val backupManager: BackupManager,
    private val preferences: AppPreferences,
    private val languagePack: LanguagePackManager,
) : ViewModel() {

    /**
     * Sprachpaket für „Rezept übersetzen“ (#60); der Download läuft weiter, wenn die Einstellungen geschlossen werden.
     * Der Stand ändert sich im Hintergrund; an die Anzeige geht er nur vom Hauptthread aus.
     */
    val languagePackState: StateFlow<LanguagePackManager.State> =
        languagePack.state.stateIn(viewModelScope, SharingStarted.Eagerly, languagePack.state.value)

    fun downloadLanguagePack() = languagePack.download()

    fun cancelLanguagePack() = languagePack.cancel()

    fun deleteLanguagePack() {
        viewModelScope.launch {
            languagePack.delete()
            event = SettingsEvent.LanguagePackDeleted
        }
    }

    /** Darstellung: Hell/Dunkel und Farbwelt; die ganze App wechselt sofort. */
    val appearance: StateFlow<Appearance?> = preferences.appearance

    fun setThemeMode(mode: ThemeMode) = updateAppearance { it.copy(mode = mode) }

    fun setPalette(palette: Palette) = updateAppearance { it.copy(palette = palette) }

    private fun updateAppearance(change: (Appearance) -> Appearance) {
        viewModelScope.launch {
            runCatching { preferences.setAppearance(change(appearance.value ?: Appearance())) }
        }
    }

    /** „Bildschirm in der Rezeptansicht“ (#68); gilt sofort. */
    val screenOn: StateFlow<ScreenOn?> = preferences.screenOn

    fun setScreenOn(screenOn: ScreenOn) {
        viewModelScope.launch { runCatching { preferences.setScreenOn(screenOn) } }
    }

    /** Letzte geprüfte Sicherung für „Zuletzt gesichert: …“ (#51); null, solange sie lädt oder es keine gibt. */
    var lastBackup by mutableStateOf<AppPreferences.LastBackup?>(null)
        private set

    init {
        viewModelScope.launch { lastBackup = runCatching { preferences.lastBackup() }.getOrNull() }
        viewModelScope.launch { languagePack.refresh() }
    }

    var state by mutableStateOf<SettingsUiState>(SettingsUiState.Idle)
        private set

    /** Ergebnis für die Meldungsleiste; wird nach dem Anzeigen zurückgesetzt. */
    var event by mutableStateOf<SettingsEvent?>(null)

    fun backup(uri: Uri) {
        state = SettingsUiState.Working(R.string.backup_running)
        viewModelScope.launch {
            event = try {
                val count = backupManager.export(uri)
                // Erst die geprüfte Sicherung zählt als „zuletzt gesichert“.
                val backup = AppPreferences.LastBackup(System.currentTimeMillis(), count)
                runCatching { preferences.setLastBackup(backup) }
                lastBackup = backup
                SettingsEvent.BackupDone(count)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupManager.BackupException) {
                SettingsEvent.BackupFailed(e.problem)
            } catch (e: Exception) {
                SettingsEvent.BackupFailed(BackupManager.BackupException.Problem.FAILED)
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
                rememberRestoredBackup(preview)
                SettingsEvent.RestoreDone(result.added + result.replaced)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (StorageFull.isCause(e)) SettingsEvent.RestoreProblem(BackupReader.Problem.NO_SPACE) else SettingsEvent.RestoreFailed
            }
            state = SettingsUiState.Idle
        }
    }

    /**
     * Nach dem Wiederherstellen, z. B. auf einem neuen Handy: Die Rezepte stecken in der Sicherung, aus der sie kommen.
     * Ist sie neuer als die zuletzt gemerkte, gilt sie als „zuletzt gesichert“ – sonst hieße es fälschlich,
     * die ganze Sammlung sei noch nicht gesichert.
     */
    private suspend fun rememberRestoredBackup(preview: BackupManager.RestorePreview) {
        val current = runCatching { preferences.lastBackup() }.getOrNull()
        if (current == null || current.at < preview.backupCreatedAt) {
            val backup = AppPreferences.LastBackup(preview.backupCreatedAt, preview.total)
            runCatching { preferences.setLastBackup(backup) }
            lastBackup = backup
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
