package com.moltobene.app.tour

import android.app.LocaleManager
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import com.moltobene.app.BuildConfig
import com.moltobene.app.MoltobeneApplication
import com.moltobene.app.data.Appearance
import com.moltobene.app.data.ScreenOn
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import kotlin.math.abs

/**
 * Bereitet jeden Test vor, bevor die App startet: Sprache, Hell/Dunkel und Schriftgröße einstellen
 * und die Sammlung leeren. Die Schriftgröße bleibt danach, wie sie ist: Die Tests einer Darstellung laufen
 * nacheinander, und schnelles Hin- und Herschalten zwischen den Tests kam nicht immer rechtzeitig an.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class DeviceSetupRule(private val variant: DisplayVariant) : ExternalResource() {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    override fun before() {
        // Sprache und Hell/Dunkel nur für Moltobene, wie bei der Android-Einstellung „App-Sprache“.
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(variant.language)
        context.getSystemService(UiModeManager::class.java).setApplicationNightMode(
            if (variant.dark) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO,
        )
        setFontScale(variant.fontScale)
        runBlocking { resetApp() }
    }

    /** Schriftgröße des ganzen Handys (wie in den Android-Einstellungen), 2 = 200 %. */
    private fun setFontScale(scale: Float) {
        fun applied() = abs(context.resources.configuration.fontScale - scale) <= 0.01f
        if (applied()) return
        TestDevice.shell("settings put system font_scale $scale")
        // Die neue Einstellung kommt mit Verzögerung im Handy und in der App an.
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!applied() && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(100)
        }
        check(applied()) { "Schriftgröße $scale nicht übernommen" }
    }

    private suspend fun resetApp() {
        val container = (context.applicationContext as MoltobeneApplication).container
        container.repository.getAll().forEach { container.repository.delete(it.id) }
        // „Neu in Version …“ gilt als gesehen, damit es nicht unerwartet über dem Rundgang erscheint.
        container.preferences.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
        // Der Hinweis zum Sichern (#51) beginnt in jedem Durchgang wieder sichtbar.
        container.preferences.setBackupHintHiddenAt(0)
        // Standard-Darstellung: „Schiefer“ und wie das Handy – Hell/Dunkel kommt dann von der Darstellung des Rundgangs.
        container.preferences.setAppearance(Appearance())
        container.preferences.setScreenOn(ScreenOn.STAYS_ON)
    }
}
