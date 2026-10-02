package com.moltobene.app.tour

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.filters.SdkSuppress
import com.moltobene.app.MainActivity
import com.moltobene.app.MoltobeneApplication
import com.moltobene.app.R
import com.moltobene.app.ui.whatsnew.WhatsNew
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Rundgang durch die App auf einem echten Android (Emulator): bedient die wichtigsten Wege wie ein Mensch,
 * prüft das Ergebnis und macht von jedem Bildschirm ein Foto – in jeder [DisplayVariant].
 * Die Nummern im Namen der Fotos geben die Reihenfolge beim Ansehen vor.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@RunWith(Parameterized::class)
class AppTourTest(private val variant: DisplayVariant) {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    // Erst einstellen und aufräumen, dann die App starten.
    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(DeviceSetupRule(variant)).around(composeRule)

    @Test
    fun leereSammlung() {
        waitForText(text(R.string.collection_empty_title))
        composeRule.onNodeWithText(text(R.string.collection_empty_text)).assertIsDisplayed()
        screenshot("01-sammlung-leer")
    }

    @Test
    fun rezeptHinzufuegen() {
        clickVisible(text(R.string.add_recipe))
        waitForField(R.string.field_title)
        screenshot("02-rezept-hinzufuegen")

        input(R.string.field_title, "Gemüsesuppe")
        input(R.string.servings, "4")
        input(R.string.ingredients, "2 Karotten\n1 Stange Lauch\n3 Kartoffeln\n1 l Gemüsebrühe")
        input(R.string.instructions, "Gemüse putzen und klein schneiden.\nIn der Brühe 20 Minuten köcheln lassen.")
        input(R.string.source, "Notizbuch")
        screenshot("03-rezept-hinzufuegen-ausgefuellt")

        composeRule.onNodeWithText(text(R.string.save)).performClick()
        waitForText("1 l Gemüsebrühe")
        composeRule.onNodeWithText("Gemüsesuppe").assertIsDisplayed()
        screenshot("04-rezept-gespeichert")

        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
        waitForText("Gemüsesuppe")
        screenshot("05-sammlung-ein-rezept")
    }

    @Test
    fun titelFehltUndVerwerfen() {
        clickVisible(text(R.string.add_recipe))
        waitForField(R.string.field_title)
        input(R.string.ingredients, "500 g Mehl")

        composeRule.onNodeWithText(text(R.string.save)).performClick()
        waitForText(text(R.string.field_title_missing))
        field(R.string.field_title).performScrollTo()
        screenshot("06-titel-fehlt")

        composeRule.onNodeWithContentDescription(text(R.string.close)).performClick()
        waitForText(text(R.string.discard_title))
        screenshot("07-aenderungen-verwerfen")

        composeRule.onNodeWithText(text(R.string.discard)).performClick()
        waitForText(text(R.string.collection_empty_title))
    }

    @Test
    fun sammlungDurchsuchen() {
        addSampleRecipes()
        waitForText(SampleRecipes.POTATO_SALAD)
        screenshot("08-sammlung")

        composeRule.onNode(hasSetTextAction()).performTextInput("Tomate")
        waitUntilGone(SampleRecipes.POTATO_SALAD)
        waitForText(SampleRecipes.TOMATO_SAUCE)
        screenshot("09-suche")

        composeRule.onNodeWithContentDescription(text(R.string.search_clear)).performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("Zimtschnecken")
        waitForText(text(R.string.search_no_results))
        screenshot("10-suche-ohne-treffer")
    }

    @Test
    fun rezeptAnsehenUndLoeschen() {
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        screenshot("11-rezept-mit-foto")

        composeRule.onNodeWithText("Omas Kochbuch", substring = true).performScrollTo()
        screenshot("12-rezept-ende")

        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.delete_recipe)).performClick()
        waitForText(text(R.string.delete_confirm_title))
        screenshot("13-rezept-loeschen")

        composeRule.onNodeWithText(text(R.string.delete)).performClick()
        waitForText(text(R.string.collection_title))
        waitUntilGone(SampleRecipes.TOMATO_SAUCE)
        val titles = runBlocking { container().repository.getAll().map { it.title } }
        assertFalse(SampleRecipes.TOMATO_SAUCE in titles)
        assertTrue(SampleRecipes.POTATO_SALAD in titles)
    }

    @Test
    fun entwurfOhneFoto() {
        addSampleRecipes()
        openRecipe(SampleRecipes.LENTIL_SOUP)
        waitForText("250 g Tellerlinsen")
        composeRule.onNodeWithText(text(R.string.draft_label)).assertIsDisplayed()
        screenshot("14-entwurf-ohne-foto")
    }

    @Test
    fun ausFotoUebernehmen() {
        clickVisible(text(R.string.import_from_photo))
        waitForText(text(R.string.ocr_choose_photos))
        screenshot("15-aus-foto-uebernehmen")

        composeRule.onNodeWithText(text(R.string.cancel)).performClick()
        waitUntilGone(text(R.string.ocr_choose_photos))
        waitForField(R.string.field_title)
        screenshot("16-aus-foto-abgebrochen")
    }

    @Test
    fun einstellungen() {
        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        waitForText(text(R.string.backup_title))
        screenshot("17-einstellungen")

        composeRule.onNodeWithText(text(R.string.whats_new_title, WhatsNew.VERSION_NAME)).performScrollTo().performClick()
        waitForText(text(R.string.close))
        screenshot("18-neu-in-version")

        composeRule.onNodeWithText(text(R.string.close)).performClick()
        composeRule.onNodeWithText(text(R.string.licenses_title)).performScrollTo().performClick()
        waitForText(text(R.string.licenses_intro))
        screenshot("19-lizenzen")
    }

    // --- Hilfsfunktionen ---

    private val activity get() = composeRule.activity

    /** Text aus strings.xml in der Sprache der jeweiligen Darstellung. */
    private fun text(@StringRes id: Int, vararg args: Any): String = activity.getString(id, *args)

    private fun field(@StringRes label: Int) = composeRule.onNode(hasSetTextAction() and hasText(text(label)))

    private fun input(@StringRes label: Int, value: String) {
        field(label).performScrollTo().performTextInput(value)
    }

    private fun container() = (activity.application as MoltobeneApplication).container

    private fun addSampleRecipes() = runBlocking { SampleRecipes.addTo(container(), activity) }

    private fun openRecipe(title: String) {
        waitForText(SampleRecipes.POTATO_SALAD)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(title))
        // Direkt auslösen statt antippen: Ganz unten liegen die Knöpfe „Aus Foto übernehmen“ und
        // „Rezept hinzufügen“ über der Liste, ein Tipp in die Mitte des Eintrags kann sie treffen.
        composeRule.onNodeWithText(title).performSemanticsAction(SemanticsActions.OnClick)
    }

    /**
     * Tippt auf das erste sichtbare Element mit diesem Text. Manche Aktionen gibt es doppelt,
     * z. B. „Rezept hinzufügen“ in der leeren Sammlung und als Knopf unten rechts.
     */
    private fun clickVisible(value: String) {
        val nodes = composeRule.onAllNodesWithText(value)
        val count = nodes.fetchSemanticsNodes().size
        val index = (0 until count).firstOrNull { runCatching { nodes[it].assertIsDisplayed() }.isSuccess }
            ?: throw AssertionError("„$value“ ist auf dem Bildschirm nicht sichtbar.")
        nodes[index].performClick()
    }

    private fun waitForText(value: String) = composeRule.waitUntil(TIMEOUT_MILLIS) {
        composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitForField(@StringRes label: Int) = composeRule.waitUntil(TIMEOUT_MILLIS) {
        composeRule.onAllNodes(hasSetTextAction() and hasText(text(label))).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitUntilGone(value: String) = composeRule.waitUntil(TIMEOUT_MILLIS) {
        composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isEmpty()
    }

    private fun screenshot(name: String) {
        composeRule.waitForIdle()
        // Die Bildschirmtastatur würde sonst einen großen Teil des Bildschirms verdecken.
        composeRule.runOnUiThread {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
        composeRule.waitForIdle()
        // Zeit zum Zeichnen, z. B. für Vorschaubilder, die im Hintergrund geladen werden.
        Thread.sleep(SETTLE_MILLIS)
        TestDevice.screenshot(variant, name)
    }

    companion object {
        private const val TIMEOUT_MILLIS = 10_000L
        private const val SETTLE_MILLIS = 700L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants(): List<DisplayVariant> = DisplayVariant.entries
    }
}
