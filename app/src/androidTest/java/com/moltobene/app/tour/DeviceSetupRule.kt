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
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import kotlin.math.abs

/**
 * Bereitet jeden Test vor, bevor die App startet: Sprache, Hell/Dunkel und Schriftgröße einstellen
 * und die Sammlung leeren. Danach wird die Schriftgröße wieder zurückgesetzt.
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

    override fun after() = setFontScale(1f)

    /** Schriftgröße des ganzen Handys (wie in den Android-Einstellungen), 2 = 200 %. */
    private fun setFontScale(scale: Float) {
        TestDevice.shell("settings put system font_scale $scale")
        // Die neue Einstellung kommt mit kurzer Verzögerung in der App an.
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (abs(context.resources.configuration.fontScale - scale) > 0.01f && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(100)
        }
    }

    private suspend fun resetApp() {
        val container = (context.applicationContext as MoltobeneApplication).container
        container.repository.getAll().forEach { container.repository.delete(it.id) }
        // „Neu in Version …“ gilt als gesehen, damit es nicht unerwartet über dem Rundgang erscheint.
        container.preferences.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
    }
}
