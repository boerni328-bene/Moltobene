package com.moltobene.app

import android.app.Application
import android.content.Context
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.backup.BackupManager
import com.moltobene.app.data.db.MoltobeneDatabase
import com.moltobene.app.data.ocr.TextRecognizer
import com.moltobene.app.data.photos.PhotoStore
import com.moltobene.app.data.share.RecipeSharer

class MoltobeneApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
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
}
