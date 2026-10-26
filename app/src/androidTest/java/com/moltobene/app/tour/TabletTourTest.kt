package com.moltobene.app.tour

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.filters.SdkSuppress
import com.moltobene.app.MainActivity
import com.moltobene.app.MoltobeneApplication
import com.moltobene.app.R
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Rundgang auf einem Tablet (#91): Der Emulator wird auf Tablet-Größe gestellt (quer und hoch). Geprüft wird vor allem, dass
 * in der Rezeptansicht Zutaten und Zubereitung gleichzeitig zu sehen sind, und dass die Pfeiltaste einen Schritt weiterschaltet.
 * Die Bildschirmfotos liegen in den Ordnern „tablet-quer“ und „tablet-hoch“.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@RunWith(Parameterized::class)
class TabletTourTest(private val variant: DisplayVariant) {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceSetupRule(variant)).around(composeRule)

    @Test
    fun kochbuchUndRezept() {
        runBlocking { SampleRecipes.addTo(container(), composeRule.activity) }
        waitForText(SampleRecipes.POTATO_SALAD)
        screenshot("t01-kochbuch")

        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(SampleRecipes.TOMATO_SAUCE))
        composeRule.onNodeWithText(SampleRecipes.TOMATO_SAUCE).performSemanticsAction(SemanticsActions.OnClick)
        waitForText("800 g reife Tomaten")
        // Zutaten und Zubereitung stehen gleichzeitig auf dem Bildschirm, ohne zwischen Reitern zu wechseln.
        waitForText("Tomaten würfeln", substring = true)
        composeRule.onNodeWithText("800 g reife Tomaten").assertIsDisplayed()
        // Auf dem niedrigen Handy quer liegt der Schritt unter dem Foto, dort muss gescrollt werden.
        if (variant != DisplayVariant.PHONE_LANDSCAPE) {
            composeRule.onNodeWithText("Tomaten würfeln", substring = true).assertIsDisplayed()
        }
        screenshot("t02-rezept-zweispaltig")

        // Pfeiltaste rechts wechselt den Reiter (Zubereitung → Notizen), die Schritte verschwinden aus der Spalte.
        TestDevice.shell("input keyevent KEYCODE_DPAD_RIGHT")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Tomaten würfeln", substring = true).fetchSemanticsNodes().isEmpty()
        }
        screenshot("t03-reiter-pfeiltaste")
        TestDevice.shell("input keyevent KEYCODE_DPAD_LEFT")
        waitForText("Tomaten würfeln", substring = true)
    }

    /** Drehen oder Falten baut die Ansicht neu auf (#92): Schritt und Reiter in der Kochansicht bleiben erhalten. */
    @Test
    fun drehenInDerKochansicht() {
        runBlocking { SampleRecipes.addTo(container(), composeRule.activity) }
        waitForText(SampleRecipes.POTATO_SALAD)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(SampleRecipes.TOMATO_SAUCE))
        composeRule.onNodeWithText(SampleRecipes.TOMATO_SAUCE).performSemanticsAction(SemanticsActions.OnClick)
        waitForText("800 g reife Tomaten")
        composeRule.onNodeWithContentDescription(text(R.string.cook_bar_next_description)).performClick()
        waitForText("Schritt 1 von", substring = true)

        composeRule.activityRule.scenario.recreate()

        waitForText("Schritt 1 von", substring = true)
        composeRule.onNodeWithText("800 g reife Tomaten").assertIsDisplayed()
        screenshot("t03a-nach-dem-drehen")
    }

    @Test
    fun hinzufuegenUndEinstellungen() {
        waitForText(text(R.string.collection_empty_title))
        screenshot("t04-kochbuch-leer")

        composeRule.onNodeWithContentDescription(text(R.string.add_short)).performClick()
        composeRule.onNodeWithText(text(R.string.add_recipe)).performClick()
        waitForText(text(R.string.field_title))
        screenshot("t05-hinzufuegen")
        composeRule.onNodeWithContentDescription(text(R.string.close)).performClick()
        waitForText(text(R.string.collection_empty_title))

        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        waitForText(text(R.string.settings_quick))
        screenshot("t06-einstellungen")
    }

    private fun hasText(value: String) = androidx.compose.ui.test.hasText(value)

    private fun container() = (composeRule.activity.application as MoltobeneApplication).container

    private fun text(@StringRes id: Int): String = composeRule.activity.getString(id)

    private fun waitForText(value: String, substring: Boolean = false) = composeRule.waitUntil(TIMEOUT_MILLIS) {
        composeRule.onAllNodesWithText(value, substring = substring).fetchSemanticsNodes().isNotEmpty()
    }

    private fun screenshot(name: String) {
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            WindowCompat.getInsetsController(composeRule.activity.window, composeRule.activity.window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
        composeRule.waitForIdle()
        Thread.sleep(SETTLE_MILLIS)
        TestDevice.screenshot(variant, name)
    }

    companion object {
        private const val TIMEOUT_MILLIS = 10_000L
        private const val SETTLE_MILLIS = 600L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants(): List<DisplayVariant> = DisplayVariant.entries.filter { it.isTablet }
    }
}
