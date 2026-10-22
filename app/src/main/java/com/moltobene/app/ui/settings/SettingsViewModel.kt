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
import com.moltobene.app.data.StartScreen
import com.moltobene.app.data.StorageFull
import com.moltobene.app.data.StorageUsage
import com.moltobene.app.data.ThemeMode
import com.moltobene.app.data.backup.BackupManager
import com.moltobene.app.data.backup.BackupReader
import com.moltobene.app.data.speech.SpeechOutput
import com.moltobene.app.data.speech.SpeechSettings
import com.moltobene.app.data.speech.SpokenText
import com.moltobene.app.data.speech.VoiceInfo
import com.moltobene.app.data.translate.LanguagePackManager
import com.moltobene.app.data.voice.VoicePackManager
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
    data object VoicePackDeleted : SettingsEvent
    data class CacheCleared(val freedBytes: Long) : SettingsEvent
}

class SettingsViewModel(
    private val backupManager: BackupManager,
    private val preferences: AppPreferences,
    private val languagePack: LanguagePackManager,
    private val voicePack: VoicePackManager,
    private val storage: StorageUsage,
    private val speech: () -> SpeechOutput,
) : ViewModel() {

    /** Belegter Speicher je Posten; null, solange er gemessen wird oder noch nicht gefragt wurde. */
    var storageResult by mutableStateOf<StorageUsage.Result?>(null)
        private set

    /** Misst den Speicher im Hintergrund; wird beim Aufklappen der Gruppe „Speicher“ und nach dem Löschen aufgerufen. */
    fun measureStorage() {
        viewModelScope.launch { storageResult = runCatching { storage.measure() }.getOrNull() }
    }

    /** Leert den Zwischenspeicher und meldet, wie viel frei wurde. */
    fun clearCache() {
        viewModelScope.launch {
            val freed = runCatching { storage.clearCache() }.getOrDefault(0L)
            event = SettingsEvent.CacheCleared(freed)
            measureStorage()
        }
    }

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
            if (storageResult != null) measureStorage()
        }
    }

    /** Erkennungspaket für die Spracheingabe (#80); gleiche Regeln wie beim Sprachpaket. */
    val voicePackState: StateFlow<LanguagePackManager.State> =
        voicePack.state.stateIn(viewModelScope, SharingStarted.Eagerly, voicePack.state.value)

    fun downloadVoicePack() = voicePack.download()

    fun cancelVoicePack() = voicePack.cancel()

    fun deleteVoicePack() {
        viewModelScope.launch {
            voicePack.delete()
            event = SettingsEvent.VoicePackDeleted
            if (storageResult != null) measureStorage()
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

    /** „Beim Öffnen der App“ (#81); gilt beim nächsten Öffnen. */
    val startScreen: StateFlow<StartScreen?> = preferences.startScreen

    fun setStartScreen(startScreen: StartScreen) {
        viewModelScope.launch { runCatching { preferences.setStartScreen(startScreen) } }
    }

    /** „Bildschirm in der Rezeptansicht“ (#68); gilt sofort. */
    val screenOn: StateFlow<ScreenOn?> = preferences.screenOn

    fun setScreenOn(screenOn: ScreenOn) {
        viewModelScope.launch { runCatching { preferences.setScreenOn(screenOn) } }
    }

    /** Stimme, Tempo und Tonhöhe beim Vorlesen (#85); gilt sofort beim nächsten Vorlesen. */
    val speechSettings: StateFlow<SpeechSettings> = preferences.speechSettings

    fun changeSpeech(transform: (SpeechSettings) -> SpeechSettings) {
        viewModelScope.launch { runCatching { preferences.setSpeechSettings(transform(preferences.speechSettings.value)) } }
    }

    /** Die installierten Stimmen ohne Internet für [language]; null, wenn keine Sprachausgabe eingerichtet ist. */
    suspend fun voices(language: String): List<VoiceInfo>? = runCatching { speech().voices(language) }.getOrNull()

    /** Hörprobe: nur auf Antippen, mit den gezeigten (noch nicht gemerkten) Einstellungen. */
    fun playSample(language: String, settings: SpeechSettings) {
        runCatching { speech().speak(language, listOf(SpokenText.sample(language)), settings) }
    }

    fun stopSpeech() {
        runCatching { speech().stop() }
    }

    /** Letzte geprüfte Sicherung für „Zuletzt gesichert: …“ (#51); null, solange sie lädt oder es keine gibt. */
    var lastBackup by mutableStateOf<AppPreferences.LastBackup?>(null)
        private set

    init {
        viewModelScope.launch { lastBackup = runCatching { preferences.lastBackup() }.getOrNull() }
        viewModelScope.launch { languagePack.refresh() }
        viewModelScope.launch { voicePack.refresh() }
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
        // Die Sprachausgabe, die für die Hörprobe gestartet wurde, wird mit den Einstellungen freigegeben (#85).
        runCatching { speech().release() }
    }
}
