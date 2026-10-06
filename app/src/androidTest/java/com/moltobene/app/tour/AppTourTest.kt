package com.moltobene.app.tour

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
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

        // Antippen zeigt das Foto ganz, als Vollbild zum Vergrößern.
        composeRule.onNodeWithContentDescription(text(R.string.photo)).performClick()
        waitForDescription(text(R.string.zoom_in))
        waitForDescription(text(R.string.photo))
        screenshot("11a-foto-vollbild")
        composeRule.onNodeWithContentDescription(text(R.string.close)).performClick()
        waitForText("800 g reife Tomaten")

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
        clickVisible(text(R.string.import_recipe))
        waitForText(text(R.string.import_from_text))
        screenshot("15-rezept-uebernehmen")

        composeRule.onNodeWithText(text(R.string.import_from_photo)).performClick()
        waitForText(text(R.string.ocr_choose_photos))
        screenshot("16-aus-foto-uebernehmen")

        composeRule.onNodeWithText(text(R.string.cancel)).performClick()
        waitUntilGone(text(R.string.ocr_choose_photos))
        waitForField(R.string.field_title)
        screenshot("17-aus-foto-abgebrochen")
    }

    /**
     * Seitenübersicht der Fotoserie, hier mit dem Rezeptfoto als Seite (Kamera und Fotoauswahl sind fremde Apps):
     * Bereich einer Seite wählen, zurück zur Übersicht, Abbrechen mit Rückfrage.
     */
    @Test
    fun seitenuebersicht() {
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        composeRule.onNodeWithContentDescription(text(R.string.edit_recipe)).performClick()
        waitForField(R.string.field_title)

        composeRule.onNodeWithText(text(R.string.import_from_photo)).performScrollTo().performClick()
        waitForText(text(R.string.ocr_from_recipe_photo))
        // Mit 200 % Schrift liegt die Auswahl unter dem Erklärtext; im Fenster wird dafür geblättert.
        composeRule.onNodeWithText(text(R.string.ocr_from_recipe_photo)).performScrollTo().performClick()
        waitForText(text(R.string.area_recognize))
        composeRule.onNodeWithText(text(R.string.page_label, 1)).performScrollTo().assertIsDisplayed()
        screenshot("18-seitenuebersicht")

        composeRule.onNodeWithContentDescription(text(R.string.page_choose_area, 1)).performScrollTo().performClick()
        waitForText(text(R.string.area_title))
        screenshot("19-bereich-waehlen")

        composeRule.onNodeWithText(text(R.string.area_done)).performClick()
        waitForText(text(R.string.area_recognize))
        composeRule.onNodeWithContentDescription(text(R.string.cancel)).performClick()
        waitForText(text(R.string.pages_discard_title))
        screenshot("20-seiten-verwerfen")

        composeRule.onNodeWithText(text(R.string.discard)).performClick()
        waitUntilGone(text(R.string.area_recognize))
        waitForField(R.string.field_title)
    }

    /**
     * Texterkennung auf dem Handy: Das Rezeptfoto ist eine gezeichnete Kochbuchseite; nach „Text erkennen“
     * stehen Zutaten und Zubereitung in ihren Feldern. Prüft auch, dass die Modelle unter Android laufen.
     */
    @Test
    fun texterkennung() {
        // Die Erkennung hängt nicht von der Darstellung ab; einmal genügt, denn sie dauert auf dem Emulator etwas.
        assumeTrue(variant == DisplayVariant.LIGHT)
        runBlocking { SampleRecipes.addRecipePage(container(), activity) }
        waitForText(SampleRecipes.PAGE_RECIPE)
        composeRule.onNodeWithText(SampleRecipes.PAGE_RECIPE).performSemanticsAction(SemanticsActions.OnClick)
        waitForDescription(text(R.string.edit_recipe))
        composeRule.onNodeWithContentDescription(text(R.string.edit_recipe)).performClick()
        waitForField(R.string.field_title)

        composeRule.onNodeWithText(text(R.string.import_from_photo)).performScrollTo().performClick()
        waitForText(text(R.string.ocr_from_recipe_photo))
        composeRule.onNodeWithText(text(R.string.ocr_from_recipe_photo)).performScrollTo().performClick()
        waitForText(text(R.string.area_recognize))
        composeRule.onNodeWithText(text(R.string.area_recognize)).performClick()

        composeRule.waitUntil(OCR_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.ocr_done)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction() and hasText("500 g Mehl", substring = true)).assertExists()
        composeRule.onNode(hasSetTextAction() and hasText("1 Würfel Hefe", substring = true)).assertExists()
        composeRule.onNode(hasSetTextAction() and hasText("lauwarmen Milch auflösen", substring = true)).assertExists()
        field(R.string.ingredients).performScrollTo()
        screenshot("21-text-erkannt")
    }

    @Test
    fun ausTextUebernehmen() {
        clickVisible(text(R.string.import_recipe))
        waitForText(text(R.string.import_from_text))
        composeRule.onNodeWithText(text(R.string.import_from_text)).performClick()
        waitForField(R.string.import_text_field)
        // Vor der Eingabe: Die Tastatur im Fenster würde sonst „Aus der Zwischenablage einfügen“ verdecken.
        screenshot("22-aus-text-uebernehmen")
        // Rezepte bleiben in ihrer Originalsprache – deshalb auch in der englischen Darstellung deutsch.
        field(R.string.import_text_field).performTextInput(SHARED_TEXT)

        composeRule.onNodeWithText(text(R.string.import_text_action)).performClick()
        waitForText(text(R.string.text_done))
        composeRule.onNode(hasSetTextAction() and hasText("Spaghetti aglio e olio")).performScrollTo().assertIsDisplayed()
        screenshot("23-aus-text-uebernommen")

        field(R.string.ingredients).performScrollTo()
        screenshot("24-aus-text-zutaten")

        composeRule.onNodeWithText(text(R.string.save)).performClick()
        waitForText("200 g Spaghetti")
        val recipe = runBlocking { container().repository.getAll().single() }
        assertEquals("Spaghetti aglio e olio", recipe.title)
        assertEquals(2, recipe.servings)
        assertEquals(4, recipe.ingredients.size)
        assertEquals(3, recipe.steps.size)
    }

    @Test
    fun ausLinkUebernehmen() {
        // Nachgestellte Rezeptseite statt echtem Internet (#55).
        container().pageLoader = SamplePage.loader(activity)
        clickVisible(text(R.string.import_recipe))
        waitForText(text(R.string.import_from_link))
        composeRule.onNodeWithText(text(R.string.import_from_link)).performClick()
        waitForField(R.string.import_link_field)
        screenshot("25-aus-link-uebernehmen")
        field(R.string.import_link_field).performTextInput(SamplePage.URL)

        composeRule.onNodeWithText(text(R.string.import_text_action)).performClick()
        waitForText(text(R.string.link_done))
        // Das Foto der Seite kommt kurz nach dem Rezept.
        waitForText(text(R.string.photo_remove))
        composeRule.onNode(hasSetTextAction() and hasText(SamplePage.TITLE)).performScrollTo().assertIsDisplayed()
        screenshot("26-aus-link-uebernommen")

        composeRule.onNodeWithText(text(R.string.save)).performClick()
        waitForText("320 g Risottoreis")
        screenshot("27-aus-link-rezept")
        val recipe = runBlocking { container().repository.getAll().single() }
        assertEquals(SamplePage.TITLE, recipe.title)
        assertEquals(4, recipe.servings)
        assertEquals(5, recipe.ingredients.size)
        assertEquals(3, recipe.steps.size)
        assertEquals(10, recipe.prepMinutes)
        assertEquals(35, recipe.totalMinutes)
        // Ohne Zählzusatz (utm_…) gespeichert.
        assertEquals(SamplePage.CLEAN_URL, recipe.source?.url)
        assertEquals("de", recipe.language)
        assertEquals(1, recipe.photoIds.size)
    }

    @Test
    fun einstellungen() {
        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        waitForText(text(R.string.backup_title))
        screenshot("28-einstellungen")

        composeRule.onNodeWithText(text(R.string.whats_new_title, WhatsNew.VERSION_NAME)).performScrollTo().performClick()
        waitForText(text(R.string.close))
        screenshot("29-neu-in-version")
        composeRule.onNodeWithText(text(R.string.close)).performClick()

        composeRule.onNodeWithText(text(R.string.privacy_title)).performScrollTo().performClick()
        waitForText(text(R.string.close))
        screenshot("30-datenschutz")
        composeRule.onNodeWithText(text(R.string.close)).performClick()

        composeRule.onNodeWithText(text(R.string.licenses_title)).performScrollTo().performClick()
        waitForText(text(R.string.licenses_intro))
        screenshot("31-lizenzen")
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
        // Direkt auslösen statt antippen: Ganz unten liegen die Knöpfe „Rezept übernehmen“ und
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

    private fun waitForDescription(value: String) = composeRule.waitUntil(TIMEOUT_MILLIS) {
        composeRule.onAllNodesWithContentDescription(value).fetchSemanticsNodes().isNotEmpty()
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

        /** Die Texterkennung einer Seite dauert auf dem Emulator deutlich länger als auf einem Handy. */
        private const val OCR_TIMEOUT_MILLIS = 180_000L
        private const val SETTLE_MILLIS = 700L

        /** Selbst geschriebenes Rezept, wie es aus einer Nachricht eingefügt wird. */
        private val SHARED_TEXT = """
            Spaghetti aglio e olio
            Für 2 Portionen

            Zutaten
            200 g Spaghetti
            3 Knoblauchzehen
            4 EL Olivenöl
            1 Peperoncino

            Zubereitung
            Spaghetti in Salzwasser bissfest kochen.
            Knoblauch in Scheiben schneiden und im Öl goldgelb braten.
            Nudeln abgießen und im Öl schwenken.
        """.trimIndent()

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants(): List<DisplayVariant> = DisplayVariant.entries
    }
}
