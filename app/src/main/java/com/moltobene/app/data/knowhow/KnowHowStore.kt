package com.moltobene.app.data.knowhow

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Liest die mitgelieferten Artikel des Teigwissens (#84) aus `assets/`; nie auf dem Hauptthread. */
class KnowHowStore(context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(): List<KnowHow.Article> = withContext(Dispatchers.IO) {
        KnowHow.parse(appContext.assets.open(KnowHow.ASSET).bufferedReader().use { it.readText() })
    }
}
