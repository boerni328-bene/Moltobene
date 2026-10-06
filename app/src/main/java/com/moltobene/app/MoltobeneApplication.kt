package com.moltobene.app

import android.app.Application
import android.content.Context
import android.system.ErrnoException
import android.system.Os
import androidx.annotation.VisibleForTesting
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.LegacyCleanup
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.backup.BackupManager
import com.moltobene.app.data.db.MoltobeneDatabase
import com.moltobene.app.data.ocr.PendingRecognition
import com.moltobene.app.data.ocr.TextRecognizer
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.data.share.RecipeFileReader
import com.moltobene.app.data.share.RecipeSharer
import com.moltobene.app.data.web.HttpPageLoader
import com.moltobene.app.data.web.PageLoader
import com.moltobene.app.data.web.WebImporter
import com.moltobene.app.ui.edit.SharedInput

class MoltobeneApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun attachBaseContext(base: Context) {
        // #49: Telemetrie von ONNX Runtime dauerhaft aus – zusätzlich zum im Manifest entfernten Startbaustein,
        // auch falls die Internet-Berechtigung später gewollt zurückkommt. So früh wie möglich, vor dem Laden
        // der Bibliothek; die Bibliothek liest den Wert beim Start und bleibt dann aus.
        try {
            Os.setenv(DISABLE_TELEMETRY, "1", true)
        } catch (e: ErrnoException) {
            // Ohne Startbaustein sendet die Bibliothek ohnehin nichts.
        }
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        // Reste älterer Versionen entfernen, im Hintergrund, damit der Start nicht wartet.
        Thread({ LegacyCleanup.run(filesDir, noBackupFilesDir, cacheDir) }, "aufraeumen").apply {
            priority = Thread.MIN_PRIORITY
        }.start()
    }

    private companion object {
        const val DISABLE_TELEMETRY = "ORT_DISABLE_TELEMETRY"
    }
}

/** Hält die gemeinsam genutzten Bausteine. Alles wird erst bei Bedarf erzeugt (schneller App-Start). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: MoltobeneDatabase by lazy { MoltobeneDatabase.create(appContext) }
    val photoStore: PhotoStore by lazy { PhotoStore(appContext) }
    val repository: RecipeRepository by lazy { RecipeRepository(database.recipeDao(), photoStore) }
    val backupManager: BackupManager by lazy { BackupManager(appContext, repository, photoStore) }
    val recipeSharer: RecipeSharer by lazy { RecipeSharer(appContext, photoStore) }
    val preferences: AppPreferences by lazy { AppPreferences(appContext) }
    val textRecognizer: TextRecognizer by lazy { TextRecognizer(appContext) }
    val pendingRecognition: PendingRecognition by lazy { PendingRecognition(appContext, photoStore) }
    val sharedInput = SharedInput()
    val recipeFileReader: RecipeFileReader by lazy { RecipeFileReader(appContext) }

    /**
     * Einzige Stelle für Internetzugriffe (#55). Der Rundgang auf dem Emulator setzt hier einen Lader mit
     * nachgestellten Seiten ein, damit er nicht vom echten Internet abhängt.
     */
    @VisibleForTesting
    var pageLoader: PageLoader = HttpPageLoader()

    /** Für „Aus Link übernehmen“; nutzt den jeweils aktuellen [pageLoader]. */
    val webImporter: WebImporter get() = WebImporter(pageLoader, appContext.cacheDir)
}
