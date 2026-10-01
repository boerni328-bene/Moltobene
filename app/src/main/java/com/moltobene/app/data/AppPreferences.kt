package com.moltobene.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Kleine Merkwerte der App (keine Rezeptdaten). Zugriffe laufen nie auf dem Hauptthread. */
class AppPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy { appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE) }

    /** versionCode, für den „Neu in Version …“ zuletzt erledigt war; null beim ersten Start. */
    suspend fun lastSeenVersionCode(): Int? = withContext(Dispatchers.IO) {
        if (prefs.contains(KEY_LAST_SEEN_VERSION)) prefs.getInt(KEY_LAST_SEEN_VERSION, 0) else null
    }

    suspend fun setLastSeenVersionCode(versionCode: Int) = withContext(Dispatchers.IO) {
        prefs.edit { putInt(KEY_LAST_SEEN_VERSION, versionCode) }
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
    }
}
