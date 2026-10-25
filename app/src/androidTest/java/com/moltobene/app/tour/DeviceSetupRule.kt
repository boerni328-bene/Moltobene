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
import com.moltobene.app.data.CollectionSort
import com.moltobene.app.data.ScreenOn
import com.moltobene.app.data.StartScreen
import kotlinx.coroutines.runBlocking
import org.junit.rules.ExternalResource
import kotlin.math.abs

/** Schmaler als das gilt der Bildschirm als Handy (Rückstellung nach einer Tablet-Darstellung). */
private const val PHONE_MAX_WIDTH_DP = 500

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
        setDisplay()
        runBlocking { resetApp() }
    }

    /**
     * Stellt den Bildschirm des Emulators auf Tablet-Größe (#91) oder wieder auf die ursprüngliche Größe zurück. Wie bei der
     * Schriftgröße bleibt die Einstellung nach dem Test bestehen; der nächste Test setzt sie, wie er sie braucht.
     */
    private fun setDisplay() {
        val expected = variant.widthDp
        if (expected != null) {
            TestDevice.shell("wm size ${variant.displaySize}")
            TestDevice.shell("wm density ${variant.density}")
        } else {
            TestDevice.shell("wm size reset")
            TestDevice.shell("wm density reset")
        }
        fun applied() = context.resources.configuration.screenWidthDp.let { width ->
            if (expected != null) abs(width - expected) <= 2 else width < PHONE_MAX_WIDTH_DP
        }
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (!applied() && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(100)
        }
        check(applied()) { "Bildschirmgröße der Darstellung ${variant.folder} nicht übernommen" }
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
        // Der Rundgang beginnt in der Sammlung (Start: Rezepte); die Startseite liegt darunter und hat ihren eigenen Test.
        container.preferences.setStartScreen(StartScreen.COLLECTION)
        container.preferences.setCollectionSort(CollectionSort.DEFAULT)
        container.preferences.setCookingLargeText(false)
    }
}
