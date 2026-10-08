package com.moltobene.app.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import com.moltobene.app.data.ThemeMode

/**
 * Ab Android 12 merkt sich Android die Wahl „Hell“ oder „Dunkel“ für Moltobene. So passen auch das Startbild und
 * die Fensterfarbe schon beim Start. Die Farben selbst wählt die App immer selbst ([ThemeMode.isDark]), auch auf
 * älteren Handys. Nur aufrufen, wenn in den Einstellungen gewählt wird – nicht beim Start, sonst würde das
 * Hell/Dunkel des Rundgangs auf dem Emulator überschrieben.
 */
fun applyNightMode(context: Context, mode: ThemeMode) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val manager = context.getSystemService(UiModeManager::class.java) ?: return
    manager.setApplicationNightMode(
        when (mode) {
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        },
    )
}
