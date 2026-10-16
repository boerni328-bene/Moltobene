package com.moltobene.app.data

import com.moltobene.app.data.dough.DoughMemory
import com.moltobene.app.data.speech.SpeechSettings
import com.moltobene.app.data.dough.DoughType
import com.moltobene.app.data.dough.LeavenedInput
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Kleine Merkwerte der App (keine Rezeptdaten). Zugriffe laufen nie auf dem Hauptthread. */
class AppPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy { appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE) }

    private val appearanceState = MutableStateFlow<Appearance?>(null)

    /** Darstellung aus den Einstellungen; null, bis [loadAppearance] sie gelesen hat. */
    val appearance: StateFlow<Appearance?> = appearanceState.asStateFlow()

    private val startScreenState = MutableStateFlow<StartScreen?>(null)

    /** „Beim Öffnen der App“ (#81); null, bis [loadAppearance] sie gelesen hat. */
    val startScreen: StateFlow<StartScreen?> = startScreenState.asStateFlow()

    private val recentState = MutableStateFlow<List<String>?>(null)

    /** Zuletzt geöffnete Rezepte (#81), das neueste zuerst; null, bis [loadRecentRecipes] sie gelesen hat. */
    val recentRecipes: StateFlow<List<String>?> = recentState.asStateFlow()

    private val screenOnState = MutableStateFlow<ScreenOn?>(null)

    /** „Bildschirm in der Rezeptansicht“ (#68); null, bis [loadAppearance] sie gelesen hat (dann gilt „Bleibt an“). */
    val screenOn: StateFlow<ScreenOn?> = screenOnState.asStateFlow()

    private val speechSettingsState = MutableStateFlow(SpeechSettings())

    /** Stimme, Tempo und Tonhöhe beim Vorlesen (#85); bis [loadAppearance] sie gelesen hat, gelten die Standardwerte. */
    val speechSettings: StateFlow<SpeechSettings> = speechSettingsState.asStateFlow()

    /**
     * Liest die Darstellung und die Bildschirm-Einstellung; gleich beim Start der App, damit der erste Bildschirm schon
     * in den richtigen Farben kommt.
     */
    suspend fun loadAppearance() = withContext(Dispatchers.IO) {
        // Vor der Darstellung, denn die Oberfläche erscheint, sobald die Darstellung da ist, und braucht den Start.
        val start = runCatching { StartScreen.fromKey(prefs.getString(KEY_START_SCREEN, null)) }.getOrDefault(StartScreen.HOME)
        startScreenState.compareAndSet(null, start)
        val stored = runCatching {
            Appearance(
                mode = ThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, null)),
                palette = Palette.fromKey(prefs.getString(KEY_PALETTE, null)),
            )
        }.getOrDefault(Appearance())
        appearanceState.compareAndSet(null, stored)
        val screen = runCatching { ScreenOn.fromKey(prefs.getString(KEY_SCREEN_ON, null)) }.getOrDefault(ScreenOn.STAYS_ON)
        screenOnState.compareAndSet(null, screen)
        val speech = runCatching { SpeechSettings.decode(prefs.getString(KEY_SPEECH_SETTINGS, null)) }.getOrDefault(SpeechSettings())
        if (!speechSettingsTouched) speechSettingsState.value = speech
    }

    @Volatile
    private var speechSettingsTouched = false

    /** Gilt sofort beim nächsten Vorlesen, gespeichert wird danach (#85). */
    suspend fun setSpeechSettings(settings: SpeechSettings) {
        speechSettingsTouched = true
        speechSettingsState.value = settings
        withContext(Dispatchers.IO) { prefs.edit { putString(KEY_SPEECH_SETTINGS, SpeechSettings.encode(settings)) } }
    }

    suspend fun loadRecentRecipes() = withContext(Dispatchers.IO) {
        val stored = runCatching { RecentRecipes.decode(prefs.getString(KEY_RECENT_RECIPES, null)) }.getOrDefault(emptyList())
        recentState.compareAndSet(null, stored)
    }

    /** Merkt ein geöffnetes Rezept für „Zuletzt geöffnet“. */
    suspend fun addRecentRecipe(id: String) {
        loadRecentRecipes()
        val updated = RecentRecipes.add(recentState.value.orEmpty(), id)
        recentState.value = updated
        withContext(Dispatchers.IO) { prefs.edit { putString(KEY_RECENT_RECIPES, RecentRecipes.encode(updated)) } }
    }

    /** Gilt beim nächsten Öffnen der App; gespeichert wird danach. */
    suspend fun setStartScreen(startScreen: StartScreen) {
        startScreenState.value = startScreen
        withContext(Dispatchers.IO) { prefs.edit { putString(KEY_START_SCREEN, startScreen.key) } }
    }

    /** Gilt sofort, gespeichert wird danach. */
    suspend fun setScreenOn(screenOn: ScreenOn) {
        screenOnState.value = screenOn
        withContext(Dispatchers.IO) { prefs.edit { putString(KEY_SCREEN_ON, screenOn.key) } }
    }

    /** Die App wechselt sofort, gespeichert wird danach. */
    suspend fun setAppearance(appearance: Appearance) {
        appearanceState.value = appearance
        withContext(Dispatchers.IO) {
            prefs.edit {
                putString(KEY_THEME_MODE, appearance.mode.key)
                putString(KEY_PALETTE, appearance.palette.key)
            }
        }
    }

    /** versionCode, für den „Neu in Version …“ zuletzt erledigt war; null beim ersten Start. */
    suspend fun lastSeenVersionCode(): Int? = withContext(Dispatchers.IO) {
        if (prefs.contains(KEY_LAST_SEEN_VERSION)) prefs.getInt(KEY_LAST_SEEN_VERSION, 0) else null
    }

    suspend fun setLastSeenVersionCode(versionCode: Int) = withContext(Dispatchers.IO) {
        prefs.edit { putInt(KEY_LAST_SEEN_VERSION, versionCode) }
    }

    /** Zuletzt erkannte Sprache der Texterkennung (z. B. „de“); damit beginnt die nächste Erkennung. */
    suspend fun recognitionLanguage(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_RECOGNITION_LANGUAGE, null)
    }

    suspend fun setRecognitionLanguage(language: String) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_RECOGNITION_LANGUAGE, language) }
    }

    /** Letzte geprüfte Sicherung (#51): Zeitpunkt und Zahl der Rezepte. */
    data class LastBackup(val at: Long, val recipes: Int)

    /** Die letzte geprüfte Sicherung; null, wenn auf diesem Handy noch nie gesichert wurde. */
    suspend fun lastBackup(): LastBackup? = withContext(Dispatchers.IO) {
        if (!prefs.contains(KEY_LAST_BACKUP_AT)) return@withContext null
        LastBackup(prefs.getLong(KEY_LAST_BACKUP_AT, 0), prefs.getInt(KEY_LAST_BACKUP_RECIPES, 0))
    }

    /** Merkt sich eine Sicherung; der Hinweis in der Sammlung beginnt danach wieder bei null. */
    suspend fun setLastBackup(backup: LastBackup) = withContext(Dispatchers.IO) {
        prefs.edit {
            putLong(KEY_LAST_BACKUP_AT, backup.at)
            putInt(KEY_LAST_BACKUP_RECIPES, backup.recipes)
            putInt(KEY_BACKUP_HINT_HIDDEN_AT, 0)
        }
    }

    /**
     * Wie viele Rezepte ungesichert waren, als der Hinweis in der Sammlung ausgeblendet wurde (0 = nicht ausgeblendet).
     * Er erscheint erst wieder, wenn noch einmal so viele dazugekommen sind, wie für den Hinweis nötig sind.
     */
    suspend fun backupHintHiddenAt(): Int = withContext(Dispatchers.IO) { prefs.getInt(KEY_BACKUP_HINT_HIDDEN_AT, 0) }

    suspend fun setBackupHintHiddenAt(unsaved: Int) = withContext(Dispatchers.IO) {
        prefs.edit { putInt(KEY_BACKUP_HINT_HIDDEN_AT, unsaved) }
    }

    /** Sammlung als Raster (Standard) oder Liste. */
    suspend fun collectionGrid(): Boolean = withContext(Dispatchers.IO) { prefs.getBoolean(KEY_COLLECTION_GRID, true) }

    suspend fun setCollectionGrid(grid: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit { putBoolean(KEY_COLLECTION_GRID, grid) }
    }

    /** Reihenfolge der Sammlung (#57): nach Titel (Standard) oder zuletzt hinzugefügt. */
    suspend fun collectionSort(): CollectionSort = withContext(Dispatchers.IO) {
        CollectionSort.fromName(prefs.getString(KEY_COLLECTION_SORT, null))
    }

    suspend fun setCollectionSort(sort: CollectionSort) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_COLLECTION_SORT, sort.name) }
    }

    /** Zuletzt benutzte Werte des Teigrechners je Teigart (#69); geprüft wird beim Lesen in [DoughMemory]. */
    suspend fun doughValues(type: DoughType): LeavenedInput = withContext(Dispatchers.IO) {
        DoughMemory.decode(type, prefs.getString(KEY_DOUGH_PREFIX + type.key, null))
    }
    /** Gemerkte Werte einer Teigart des Teigrechners als Text (Pasta und Kuchen je Art, #70); null, wenn nichts da ist. */
    suspend fun doughMemory(key: String): String? = withContext(Dispatchers.IO) { prefs.getString(KEY_DOUGH_PREFIX + key, null) }

    suspend fun setDoughMemory(key: String, text: String) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_DOUGH_PREFIX + key, text) }
    }


    suspend fun setDoughValues(input: LeavenedInput) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_DOUGH_PREFIX + input.type.key, DoughMemory.encode(input)) }
    }

    /** true, wenn die App frisch installiert und noch nie aktualisiert wurde. */
    suspend fun isFreshInstall(): Boolean = withContext(Dispatchers.IO) {
        val manager = appContext.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            manager.getPackageInfo(appContext.packageName, 0)
        }
        info.firstInstallTime == info.lastUpdateTime
    }

    private companion object {
        const val FILE_NAME = "moltobene"
        const val KEY_LAST_SEEN_VERSION = "last_seen_version_code"
        const val KEY_RECOGNITION_LANGUAGE = "recognition_language"
        const val KEY_LAST_BACKUP_AT = "last_backup_at"
        const val KEY_LAST_BACKUP_RECIPES = "last_backup_recipes"
        const val KEY_BACKUP_HINT_HIDDEN_AT = "backup_hint_hidden_at"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_PALETTE = "palette"
        const val KEY_SCREEN_ON = "screen_on"
        const val KEY_SPEECH_SETTINGS = "speech_settings"
        const val KEY_START_SCREEN = "start_screen"
        const val KEY_RECENT_RECIPES = "recent_recipes"
        const val KEY_COLLECTION_GRID = "collection_grid"
        const val KEY_COLLECTION_SORT = "collection_sort"
        const val KEY_DOUGH_PREFIX = "dough_values_"
    }
}
