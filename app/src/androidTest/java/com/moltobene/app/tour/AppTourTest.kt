package com.moltobene.app.tour

import android.content.Intent
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.moltobene.app.MainActivity
import com.moltobene.app.MoltobeneApplication
import com.moltobene.app.R
import com.moltobene.app.data.CollectionSort
import com.moltobene.app.data.CookingTextSize
import com.moltobene.app.data.voice.VoiceCommand
import com.moltobene.app.data.Appearance
import com.moltobene.app.data.Palette
import com.moltobene.app.data.ScreenOn
import com.moltobene.app.data.StartScreen
import com.moltobene.app.data.basics.BasicRecipes
import com.moltobene.app.data.knowhow.KnowHow
import com.moltobene.app.data.ThemeMode
import com.moltobene.app.data.share.RecipeJsonLd
import com.moltobene.app.ui.whatsnew.WhatsNew
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        openAddMenu()
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
        openAddMenu()
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

        // Sortieren (#57): Menü zeigen, „Zuletzt hinzugefügt“ wählen, der Merkwert folgt, dann wieder nach Titel.
        composeRule.onNodeWithContentDescription(text(R.string.sort_menu)).performClick()
        waitForText(text(R.string.sort_recent))
        screenshot("08b-sortieren-menue")
        composeRule.onNodeWithText(text(R.string.sort_recent)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { runBlocking { container().preferences.collectionSort() } == CollectionSort.RECENT }
        screenshot("08c-sortiert-zuletzt")
        // Bei großer Schrift sind nur wenige Karten sichtbar; die Liste lädt träge, daher zum Rezept scrollen.
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(SampleRecipes.POTATO_SALAD))
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(SampleRecipes.TOMATO_SAUCE))
        composeRule.onNodeWithContentDescription(text(R.string.sort_menu)).performClick()
        waitForText(text(R.string.sort_title))
        composeRule.onNodeWithText(text(R.string.sort_title)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { runBlocking { container().preferences.collectionSort() } == CollectionSort.TITLE }

        composeRule.onNode(hasSetTextAction()).performTextInput("Tomate")
        waitUntilGone(SampleRecipes.POTATO_SALAD)
        waitForText(SampleRecipes.TOMATO_SAUCE)
        screenshot("09-suche")

        composeRule.onNodeWithContentDescription(text(R.string.search_clear)).performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("Zimtschnecken")
        waitForText(text(R.string.search_no_results))
        screenshot("10-suche-ohne-treffer")
    }

    /** Ruhiger Hinweis zum Sichern (#51): ab 10 ungesicherten Rezepten, führt zu den Einstellungen, lässt sich ausblenden. */
    @Test
    fun sicherungsHinweis() {
        runBlocking { SampleRecipes.addPlain(container(), count = 12) }
        val hint = activity.resources.getQuantityString(R.plurals.backup_hint, 12, 12)
        waitForText(hint)
        screenshot("08a-sicherungshinweis")

        composeRule.onNodeWithText(text(R.string.backup_hint_action)).performClick()
        waitForText(text(R.string.backup_never))
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
        waitForText(hint)

        composeRule.onNodeWithContentDescription(text(R.string.backup_hint_hide)).performClick()
        waitUntilGone(hint)
    }

    @Test
    fun rezeptAnsehenUndLoeschen() {
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        screenshot("11-rezept-mit-foto")

        // Antippen zeigt das Foto ganz, als Vollbild zum Vergrößern.
        composeRule.onNodeWithContentDescription(text(R.string.photo)).performSemanticsAction(SemanticsActions.OnClick)
        waitForDescription(text(R.string.zoom_in))
        waitForDescription(text(R.string.photo))
        screenshot("11a-foto-vollbild")
        composeRule.onNodeWithContentDescription(text(R.string.close)).performClick()
        waitForText("800 g reife Tomaten")

        openRecipeTab(R.string.source)
        composeRule.onNodeWithText("Omas Kochbuch", substring = true).performScrollTo()
        screenshot("12-rezept-ende")

        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.delete_recipe)).performClick()
        waitForText(text(R.string.delete_confirm_title))
        screenshot("13-rezept-loeschen")

        composeRule.onNodeWithText(text(R.string.delete)).performClick()
        // Zurück in der Sammlung (Titel „MOLTOBENE“); nur dort steht oben das Symbol für die Einstellungen.
        waitForDescription(text(R.string.settings_title))
        waitUntilGone(SampleRecipes.TOMATO_SAUCE)
        val titles = runBlocking { container().repository.getAll().map { it.title } }
        assertFalse(SampleRecipes.TOMATO_SAUCE in titles)
        assertTrue(SampleRecipes.POTATO_SALAD in titles)
    }

    /**
     * Startseite (#81): Der Rundgang beginnt mit „Beim Öffnen: Rezepte“, Zurück führt zur Startseite mit den Kacheln
     * „Rezepte“ (mit Anzahl) und „Teigrechner“; die Einstellung „Beim Öffnen der App“ lässt sich ändern.
     */
    @Test
    fun startseite() {
        addSampleRecipes()
        waitForText(SampleRecipes.POTATO_SALAD)
        val count = runBlocking { container().repository.getAll().size }
        pressBack()

        val recipes = activity.resources.getQuantityString(R.plurals.home_recipes_count, count, count)
        waitForText(recipes)
        composeRule.onNodeWithText(text(R.string.home_recipes_title)).assertIsDisplayed()
        // Bei großer Schrift reicht der Bildschirm nicht für alle Kacheln; die Seite scrollt.
        composeRule.onNodeWithText(text(R.string.dough_title)).performScrollTo().assertIsDisplayed()
        // „Was koche ich heute?“ (#81): ein Vorschlag aus der Sammlung, „Anderes Rezept“ wechselt, „Kochen“ öffnet.
        waitForText(text(R.string.home_today_title))
        screenshot("00-startseite")
        composeRule.onNodeWithText(text(R.string.home_today_title)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.home_today_another)).performScrollTo().performClick()
        // Tippen auf die Kachel öffnet das vorgeschlagene Rezept.
        // Bei großer Schrift kann der Hauptknopf einen Teil der Kachel verdecken; deshalb direkt die Aktion auslösen.
        composeRule.onNodeWithText(text(R.string.home_today_title)).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        // Erst weiter, wenn das Rezept offen ist; sonst würde Zurück die App von der Startseite aus beenden.
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.home_today_title)).fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) { container().preferences.recentRecipes.value.orEmpty().isNotEmpty() }
        pressBack()
        waitForText(recipes)
        // „Zuletzt geöffnet“ zeigt das eben geöffnete Rezept.
        composeRule.onNodeWithText(text(R.string.home_recent_title)).performScrollTo().assertIsDisplayed()
        screenshot("00b-startseite-zuletzt")

        // „Rezepte“ öffnet die Sammlung, Zurück führt wieder zur Startseite.
        // Die Startseite steht noch bei „Zuletzt geöffnet“; bei großer Schrift muss die Kachel erst ins Bild.
        composeRule.onNodeWithText(text(R.string.home_recipes_title)).performScrollTo().performClick()
        waitForText(SampleRecipes.POTATO_SALAD)
        pressBack()
        waitForText(recipes)

        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        waitForText(text(R.string.settings_section_start))
        openSettingsGroup(R.string.settings_section_start)
        composeRule.onNodeWithText(text(R.string.start_home)).performScrollTo().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { container().preferences.startScreen.value == StartScreen.HOME }
        composeRule.onNodeWithText(text(R.string.start_home)).assertIsSelected()
        screenshot("00a-einstellung-start")
    }

    /**
     * Vorlesen einstellen (#85): Stimme je Sprache wählen, Tempo und Tonhöhe mit Voreinstellungen und Plus, Hörprobe mit
     * den gezeigten Einstellungen. Die Stimmen sind nachgestellt (zwei je Sprache).
     */
    @Test
    fun vorlesenEinstellungen() {
        val speech = SampleSpeech()
        container().speech = speech
        addSampleRecipes()
        waitForText(SampleRecipes.POTATO_SALAD)
        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        waitForText(text(R.string.settings_section_speech))
        openSettingsGroup(R.string.settings_section_speech)

        val ui = activity.resources.configuration.locales[0]
        fun cap(value: String) = value.replaceFirstChar { it.titlecase(ui) }
        val germanName = cap(java.util.Locale.forLanguageTag("de").getDisplayLanguage(ui))
        val austria = cap(java.util.Locale.Builder().setLanguage("de").setRegion("AT").build().getDisplayName(ui))

        // Stimme für Deutsch: aus den nachgestellten Stimmen die österreichische wählen.
        composeRule.onNodeWithText(text(R.string.speech_voice_for, germanName)).performScrollTo().performClick()
        waitForText(austria)
        screenshot("16a-vorlesen-stimme")
        composeRule.onNodeWithText(austria).performScrollTo().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { container().preferences.speechSettings.value.voiceFor("de") == "de-at-x-zweite-local" }
        composeRule.onNode(hasText(text(R.string.close)) and hasClickAction()).performClick()
        waitUntilGone(austria)

        // Tempo und Tonhöhe: „Lebhaft“, dann Tempo um einen Schritt erhöhen.
        composeRule.onNodeWithText(text(R.string.speech_preset_lively)).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(TIMEOUT_MILLIS) { container().preferences.speechSettings.value.rate == 115 }
        composeRule.onNodeWithContentDescription(text(R.string.speech_more, text(R.string.speech_tempo))).performScrollTo().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { container().preferences.speechSettings.value.rate == 120 }
        assertEquals(110, container().preferences.speechSettings.value.pitch)
        screenshot("16b-vorlesen-tempo")

        // Hörprobe: spricht mit den gezeigten Einstellungen.
        composeRule.onNodeWithText(text(R.string.speech_sample)).performScrollTo().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { speech.spoken.isNotEmpty() }
        assertEquals(120, speech.lastSettings?.rate)
        assertEquals("de-at-x-zweite-local", speech.lastSettings?.voiceFor("de"))

        // Zurück zu „Normal“.
        composeRule.onNodeWithText(text(R.string.speech_preset_normal)).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(TIMEOUT_MILLIS) { container().preferences.speechSettings.value.rate == 100 }
    }

    /** Kochen (#56): Zutat abhaken, aktueller Schritt, Portionen umrechnen – das gespeicherte Rezept bleibt. */
    @Test
    fun notizUndGrosseSchriftBeimKochen() {
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")

        // Schriftgröße (#61) im Menü ⋮: „groß“ wählen, größere Schrift, die Wahl bleibt gemerkt.
        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        waitForDescription(text(R.string.cooking_size_large))
        screenshot("12s-menue-schriftgroesse")
        composeRule.onNodeWithContentDescription(text(R.string.cooking_size_large)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { runBlocking { container().preferences.cookingTextSize() } == CookingTextSize.LARGE }
        waitForText("800 g reife Tomaten")
        screenshot("12t-grosse-schrift-kochen")

        // Notiz hinzufügen direkt in der Rezeptansicht (#61).
        // Das Beispielrezept hat schon eine Notiz, deshalb heißt der Knopf „Notiz bearbeiten“; der Text ist vorausgefüllt.
        openRecipeTab(R.string.notes)
        composeRule.onNodeWithText(text(R.string.note_edit)).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(hasSetTextAction()).performTextReplacement("Nächstes Mal weniger Salz")
        screenshot("12u-notiz-hinzufuegen")
        composeRule.onNodeWithText(text(R.string.save)).performClick()
        waitForText("Nächstes Mal weniger Salz")
        composeRule.onNodeWithText(text(R.string.note_edit)).performScrollTo()
        screenshot("12v-notiz-gespeichert")
        val stored = runBlocking { container().repository.getAll().first { it.title == SampleRecipes.TOMATO_SAUCE } }
        assertEquals("Nächstes Mal weniger Salz", stored.notes)

        // Wieder ausschalten, damit andere Tests in normaler Schrift laufen.
        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        waitForDescription(text(R.string.cooking_size_medium))
        composeRule.onNodeWithContentDescription(text(R.string.cooking_size_medium)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { runBlocking { container().preferences.cookingTextSize() } == CookingTextSize.MEDIUM }
    }

    /** Langes Berühren eines Rezepts im Kochbuch öffnet dasselbe Menü wie ⋮ in der Rezeptansicht. */
    @Test
    fun menuZumRezeptInDerListe() {
        addSampleRecipes()
        waitForText(SampleRecipes.POTATO_SALAD)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(SampleRecipes.TOMATO_SAUCE))
        composeRule.onNodeWithText(SampleRecipes.TOMATO_SAUCE).performSemanticsAction(SemanticsActions.OnLongClick)
        waitForText(text(R.string.share_recipe))
        waitForText(text(R.string.delete_recipe))
        screenshot("12w-menue-in-der-liste")
    }

    @Test
    fun kochen() {
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")

        composeRule.onNodeWithText("800 g reife Tomaten").performScrollTo().performClick()
        composeRule.onNodeWithText("800 g reife Tomaten").assertIsOn()
        openRecipeTab(R.string.instructions)
        composeRule.onNodeWithText("Tomaten würfeln", substring = true).performScrollTo().performClick()
        openRecipeTab(R.string.ingredients)
        screenshot("12a-kochen-abgehakt")

        // Von 4 auf 5 Portionen: Nur die Anzeige ändert sich, das Häkchen bleibt. Stück werden auf ½ gerundet und
        // bekommen „≈“, der Screenreader liest „etwa …, umgerechnet“ (#65).
        // Nach dem Scrollen liegt das Ziel oben unter der Leiste; der Test löst die Aktion deshalb ohne Berührungspunkt aus.
        composeRule.onNodeWithContentDescription(text(R.string.servings_more)).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        val tomatoes = text(R.string.ingredient_adjusted, "1000 g reife Tomaten")
        waitForText(tomatoes)
        composeRule.onNodeWithText(tomatoes).assertIsOn()
        composeRule.onNodeWithText(text(R.string.ingredient_adjusted, text(R.string.amount_about, "1½") + " Zwiebel"))
            .assertIsOff()
        screenshot("12b-portionen-umgerechnet")
        val stored = runBlocking { container().repository.getAll().first { it.title == SampleRecipes.TOMATO_SAUCE } }
        assertEquals("800 g reife Tomaten", stored.ingredients.first().text)

        composeRule.onNodeWithText(text(R.string.servings_reset)).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        waitForText("800 g reife Tomaten")
        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        composeRule.onNodeWithText(text(R.string.cooking_reset)).performClick()
        composeRule.onNodeWithText("800 g reife Tomaten").assertIsOff()
    }

    /**
     * Kochleiste und Vorlesen (#79): „Weiter“ markiert Schritt 1, „Vorlesen“ liest ihn (mit nachgestellter Stimme),
     * „Anhalten“ beendet das Vorlesen, nach dem letzten Schritt kommt „Letzter Schritt“, „Zutaten“ liest die
     * Zutaten. Ohne Stimme erscheint eine ehrliche Meldung mit dem Weg in die Einstellungen.
     */
    @Test
    fun kochleiste() {
        val speech = SampleSpeech()
        container().speech = speech
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        screenshot("12m-kochleiste")

        composeRule.onNodeWithText(text(R.string.cook_bar_next)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.cook_bar_read_description)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { speech.spoken.isNotEmpty() }
        val first = speech.spoken.first()
        assertEquals("Schritt 1.", first.first())
        assertTrue(first.any { it.contains("Zwiebel und Knoblauch") })
        waitForDescription(text(R.string.cook_bar_stop_description))
        screenshot("12n-kochleiste-vorlesen")
        composeRule.onNodeWithContentDescription(text(R.string.cook_bar_stop_description)).performClick()
        waitForDescription(text(R.string.cook_bar_read_description))

        // Schritt 2 und 3, danach keine Schleife.
        composeRule.onNodeWithText(text(R.string.cook_bar_next)).performClick()
        composeRule.onNodeWithText(text(R.string.cook_bar_next)).performClick()
        composeRule.onNodeWithText(text(R.string.cook_bar_next)).performClick()
        waitForText(text(R.string.cook_last_step))
        screenshot("12o-kochleiste-letzter-schritt")
        composeRule.onNodeWithText(text(R.string.cook_bar_previous)).performClick()
    }

    /**
     * Kopfhörertasten (#79): Nach dem ersten Vorlesen meldet die App Android eine aktive Mediensitzung mit Wiedergabe,
     * Pause, Weiter und Zurück; beim Anhalten steht sie auf „pausiert“. Echte Tastendrücke lassen sich auf dem Emulator
     * nicht prüfen: Android leitet sie nur an Apps weiter, die wirklich Ton ausgeben, die nachgestellte Stimme tut das nicht.
     */
    @Test
    fun kopfhoerertasten() {
        val speech = SampleSpeech()
        container().speech = speech
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        // Vor dem ersten Vorlesen gehört den Kopfhörertasten noch keine Sitzung der App (Musik-Apps behalten sie).
        composeRule.waitUntil(TIMEOUT_MILLIS) { mediaSession().isEmpty() }
        composeRule.onNodeWithText(text(R.string.cook_bar_next)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.cook_bar_read_description)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { speech.spoken.isNotEmpty() }
        waitForDescription(text(R.string.cook_bar_stop_description))

        // Aktiv, spielt, Aktionen: Stopp, Pause, Wiedergabe, Zurück, Weiter und Wiedergabe/Pause (567).
        try {
            composeRule.waitUntil(TIMEOUT_MILLIS) { mediaSession().let { "active=true" in it && "state=PLAYING(3)" in it } }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Mediensitzung: " + mediaSession().replace("\n", " | "), e)
        }
        val playing = mediaSession()
        assertTrue(playing, "actions=567" in playing)
        screenshot("12p-kopfhoerertasten")

        composeRule.onNodeWithContentDescription(text(R.string.cook_bar_stop_description)).performClick()
        waitForDescription(text(R.string.cook_bar_read_description))
        composeRule.waitUntil(TIMEOUT_MILLIS) { "state=PAUSED(2)" in mediaSession() }
    }

    /** Die Zeilen der Mediensitzung „MoltobeneKochen“ aus `dumpsys media_session`; leer, wenn es sie nicht gibt. */
    private fun mediaSession(): String {
        val lines = shell("dumpsys media_session").lines()
        // „Media button session is …“ nennt nur die Sitzung für die Tasten; gemeint ist der Eintrag der Sitzung selbst.
        val start = lines.indexOfFirst { it.contains("MoltobeneKochen") && !it.contains("Media button session") }
        if (start < 0) return ""
        // Bis zur nächsten Sitzung („… (userId=0)“); dazwischen stehen Eigentümer, Zustand und Aktionen.
        val rest = lines.drop(start + 1)
        val end = rest.indexOfFirst { it.contains("(userId=") }.let { if (it < 0) rest.size else it }
        return (listOf(lines[start]) + rest.take(end)).joinToString("\n")
    }

    /** Führt einen Shell-Befehl auf dem Emulator aus und liest die Ausgabe bis zum Ende. */
    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().decodeToString() }
    }

    /**
     * Spracheingabe (#80): Ohne Erkennungspaket weist der Mikrofon-Knopf auf die Einstellungen hin. Mit Paket (nachgestellt)
     * und erteilter Berechtigung hört das Mikrofon zu; „Weiter“ liest den ersten Schritt vor, beim Vorlesen gilt nur „Stopp“,
     * „Mikrofon aus“ schaltet wieder ab.
     */
    @Test
    fun spracheingabe() {
        val speech = SampleSpeech()
        container().speech = speech
        addSampleRecipes()

        // Ohne Erkennungspaket: Hinweis mit dem Weg in die Einstellungen.
        val voice = SampleVoice(available = false)
        container().voice = voice
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        composeRule.onNodeWithContentDescription(text(R.string.cook_bar_mic_on_description)).performClick()
        waitForText(text(R.string.voice_needs_pack))
        screenshot("12q-spracheingabe-ohne-paket")
        composeRule.onNodeWithText(text(R.string.voice_open_settings)).performClick()
        waitForText(text(R.string.settings_section_voice_input))
        openSettingsGroup(R.string.settings_section_voice_input)
        screenshot("12r-spracheingabe-einstellungen")
        leaveSettings()
        waitForText("800 g reife Tomaten")

        // Mit Erkennungspaket (nachgestellt) und erteilter Berechtigung.
        voice.available = true
        TestDevice.shell("pm grant ${activity.packageName} android.permission.RECORD_AUDIO")
        composeRule.onNodeWithContentDescription(text(R.string.cook_bar_mic_on_description)).performClick()
        waitForText(text(R.string.voice_listening))
        waitForDescription(text(R.string.cook_bar_mic_off_description))
        screenshot("12s-spracheingabe-hoert-zu")

        voice.say(VoiceCommand.NEXT)
        composeRule.waitUntil(TIMEOUT_MILLIS) { speech.spoken.isNotEmpty() }
        assertEquals("Schritt 1.", speech.spoken.first().first())
        waitForDescription(text(R.string.cook_bar_stop_description))

        // Beim Vorlesen hört das Mikrofon das Vorlesen mit: „Weiter“ wird ignoriert, „Stopp“ gilt.
        val before = speech.spoken.size
        voice.say(VoiceCommand.NEXT)
        Thread.sleep(500)
        assertEquals(before, speech.spoken.size)
        voice.say(VoiceCommand.STOP)
        waitForDescription(text(R.string.cook_bar_read_description))

        // „Weiter“ nach dem Vorlesen liest den nächsten Schritt.
        voice.say(VoiceCommand.NEXT)
        composeRule.waitUntil(TIMEOUT_MILLIS) { speech.spoken.size > before }
        assertEquals("Schritt 2.", speech.spoken.last().first())
        voice.say(VoiceCommand.STOP)
        waitForDescription(text(R.string.cook_bar_read_description))

        // „Mikrofon aus“ schaltet das Zuhören ab.
        voice.say(VoiceCommand.MICROPHONE_OFF)
        waitForDescription(text(R.string.cook_bar_mic_on_description))
        composeRule.onAllNodesWithText(text(R.string.voice_listening)).assertCountEquals(0)
    }

    /** Ohne Stimme (#79): ehrliche Meldung statt falscher Stimme, mit dem Weg in die Einstellungen. */
    @Test
    fun vorlesenOhneStimme() {
        container().speech = SampleSpeech(noVoice = true)
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        composeRule.onNodeWithText(text(R.string.cook_bar_next)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.cook_bar_read_description)).performClick()
        waitForText(text(R.string.settings_title))
        screenshot("12p-vorlesen-ohne-stimme")
    }

    /**
     * Umrechnen nach einer Zutat (#66): Beim „Lemon drizzle cake“ nur 300 g statt 200 g Mehl – alle Mengen rechnen mit,
     * die gewählte Zutat zeigt genau die eingetippte Menge, das gespeicherte Rezept bleibt.
     */
    @Test
    fun umrechnenNachZutat() {
        addSampleRecipes()
        openRecipe("Lemon drizzle cake")
        waitForText("200 g soft butter")
        composeRule.onNodeWithText(text(R.string.adjust_action)).performScrollTo().performClick()
        waitForText(text(R.string.adjust_by_ingredient))
        screenshot("12g-umrechnen-fenster")
        composeRule.onNodeWithText(text(R.string.adjust_by_ingredient)).performClick()
        waitForText("200 g self-raising flour")
        // Die Zeile steht auch im Rezept hinter dem Fenster; das Fenster ist der letzte Treffer.
        composeRule.onAllNodesWithText("200 g self-raising flour").onLast().performClick()
        waitForField(R.string.adjust_amount_label)
        // 0 wird abgelehnt.
        field(R.string.adjust_amount_label).performTextClearance()
        field(R.string.adjust_amount_label).performTextInput("0")
        waitForText(text(R.string.adjust_amount_invalid))
        screenshot("12h-umrechnen-menge-ungueltig")
        field(R.string.adjust_amount_label).performTextClearance()
        field(R.string.adjust_amount_label).performTextInput("300")
        composeRule.onNodeWithText(text(R.string.adjust_confirm)).performClick()

        waitForText("300 g self-raising flour", substring = true)
        composeRule.onAllNodesWithText("300 g self-raising flour", substring = true).onFirst().performScrollTo()
        screenshot("12i-umgerechnet-nach-zutat")
        val stored = runBlocking { container().repository.getAll().first { it.title == "Lemon drizzle cake" } }
        assertEquals("200 g self-raising flour", stored.ingredients[3].text)

        composeRule.onNodeWithText(text(R.string.servings_reset)).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        waitForText("200 g self-raising flour")
    }

    /**
     * Umrechnen nach Backform (#67): „Springform (Ø 26 cm)“ aus der Rezeptmenge wird vorausgefüllt, die eigene Form hat
     * 28 cm – die Mengen wachsen mit der Fläche (etwa das 1,16-Fache), die Anzahl der Formen bleibt.
     */
    @Test
    fun umrechnenNachBackform() {
        addSampleRecipes()
        runBlocking {
            container().repository.save(
                com.moltobene.app.data.Recipe(
                    id = com.moltobene.app.data.RecipeIds.newId(),
                    title = "Apfelkuchen",
                    language = "de",
                    servings = 1,
                    servingsUnit = "Springform (Ø 26 cm)",
                    ingredients = listOf("200 g Mehl", "3 Eier", "150 g Zucker").map { com.moltobene.app.data.Ingredient(it) },
                    steps = listOf("Alles verrühren und backen."),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        openRecipe("Apfelkuchen")
        waitForText("200 g Mehl")
        composeRule.onNodeWithText(text(R.string.adjust_action)).performScrollTo().performClick()
        waitForText(text(R.string.adjust_by_pan))
        composeRule.onNodeWithText(text(R.string.adjust_by_pan)).performClick()
        waitForText(text(R.string.adjust_pan_own))
        // Die Form im Rezept ist vorausgefüllt; die eigene Form kommt dazu.
        composeRule.onAllNodes(hasSetTextAction()).onLast().performTextInput("28")
        screenshot("12j-umrechnen-backform")
        // Das Ergebnis hängt von genauen Texten ab; die Pseudosprache setzt eingefügte Werte in »…«. Das Fenster ist geprüft.
        assumeTrue(variant != DisplayVariant.PSEUDO)
        composeRule.onNodeWithText(text(R.string.adjust_confirm)).performClick()

        waitForText(text(R.string.adjust_pan_hint, "Ø 28 cm", "Ø 26 cm"), substring = true)
        waitForText("230 g Mehl", substring = true)
        composeRule.onAllNodesWithText("230 g Mehl", substring = true).onFirst().performScrollTo()
        screenshot("12k-umgerechnet-nach-backform")
        val stored = runBlocking { container().repository.getAll().first { it.title == "Apfelkuchen" } }
        assertEquals("200 g Mehl", stored.ingredients.first().text)

        composeRule.onNodeWithText(text(R.string.servings_reset)).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        waitForText("200 g Mehl")
    }

    /** Teigrechner Pasta und Kuchen (#70): Eierteig, Wasserteig, Rührteig und Quark-Öl-Teig, Entwurf mit Backform. */
    @Test
    fun teigrechnerPastaUndKuchen() {
        openAddMenu()
        waitForText(text(R.string.dough_title))
        composeRule.onNodeWithText(text(R.string.dough_title)).performScrollTo().performClick()
        waitForText(text(R.string.dough_save))

        clickVisible(text(R.string.dough_type_pasta))
        waitForText(text(R.string.dough_pasta_egg))
        screenshot("14f-teigrechner-pasta")
        // Bei normaler Schrift ist die Teigart ein Auswahlfeld, bei großer Schrift ein Chip.
        if (variant != DisplayVariant.LARGE_TEXT) {
            composeRule.onNodeWithContentDescription(text(R.string.dough_section_dough) + ": " + text(R.string.dough_pasta_egg)).performClick()
        }
        clickVisible(text(R.string.dough_pasta_water))
        waitForText(text(R.string.dough_semolina))
        screenshot("14g-teigrechner-wasserteig")

        clickVisible(text(R.string.dough_type_cake))
        waitForText(text(R.string.dough_cake_batter))
        screenshot("14h-teigrechner-kuchen")
        // Die sechs Kuchenarten stehen in einem Auswahlfeld (#83): öffnen und die Art wählen.
        clickVisible(text(R.string.dough_cake_batter))
        waitForText(text(R.string.dough_cake_quark_oil))
        clickVisible(text(R.string.dough_cake_quark_oil))
        waitForText(text(R.string.dough_label_quark))
        composeRule.onNodeWithText(text(R.string.dough_save)).performClick()
        composeRule.waitUntil(10_000) {
            runBlocking { container().repository.getAll().any { it.isDraft && it.title == text(R.string.dough_cake_quark_oil) } }
        }
        val draft = runBlocking { container().repository.getAll().first { it.isDraft } }
        assertEquals(1, draft.servings)
        assertNotNull(com.moltobene.app.data.PanScaling.parse(draft.servingsUnit))
        waitForText(text(R.string.dough_saved))
        screenshot("14i-teigrechner-kuchen-entwurf")
    }

    /** Sammlung: Favorit markieren, Filter „Favoriten“, Umschalten auf die Liste. */
    @Test
    fun favoritenUndAnsicht() {
        addSampleRecipes()
        if (composeRule.onAllNodesWithContentDescription(text(R.string.view_as_grid)).fetchSemanticsNodes().isNotEmpty()) {
            clickVisible(text(R.string.view_as_grid))
        }
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText(SampleRecipes.TOMATO_SAUCE)
        composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        waitForText(text(R.string.favorite_add))
        composeRule.onNodeWithText(text(R.string.favorite_add)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            runBlocking { container().repository.getAll().any { it.favorite } }
        }
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
        waitForText(text(R.string.filter_favorites))
        screenshot("14d-sammlung-favorit")

        clickVisible(text(R.string.filter_favorites))
        waitForText(SampleRecipes.TOMATO_SAUCE)
        composeRule.onNodeWithText(SampleRecipes.POTATO_SALAD).assertDoesNotExist()
        clickVisible(text(R.string.filter_all))
        // Die Ansicht bleibt gemerkt; hier immer auf die Liste und zurück.
        if (composeRule.onAllNodesWithContentDescription(text(R.string.view_as_list)).fetchSemanticsNodes().isNotEmpty()) {
            clickVisible(text(R.string.view_as_list))
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithContentDescription(text(R.string.view_as_grid)).fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("14e-sammlung-liste")
        // Zurück zum Raster, damit die übrigen Bildschirmfotos die Standardansicht zeigen.
        clickVisible(text(R.string.view_as_grid))
    }

    /** Teigrechner (#69): von der Sammlung öffnen, Biga wählen, als Entwurf speichern. */
    @Test
    fun teigrechner() {
        // Der Teigrechner steht im Menü des Hauptknopfs „Hinzufügen“.
        openAddMenu()
        waitForText(text(R.string.dough_title))
        composeRule.onNodeWithText(text(R.string.dough_title)).performScrollTo().performClick()
        waitForText(text(R.string.dough_save))
        waitForText("4 " + text(R.string.dough_unit_balls, "250"), substring = true)
        screenshot("14a-teigrechner")

        // Voreinstellungen (#83, Etappe 3): weiterschalten, eigene speichern, löschen, zurück zu den Standardwerten.
        // Die gemerkten Werte des vorigen Durchgangs bleiben stehen; „Zurücksetzen“ führt zu den Standardwerten.
        doughPresetAction(R.string.dough_reset)
        waitForText(text(R.string.dough_preset_default))
        composeRule.onNodeWithText(text(R.string.dough_preset_position, 1, 3)).assertExists()
        // Eigene Änderungen: vor dem Ersetzen durch eine Voreinstellung wird nachgefragt.
        // Wasseranteil ändern: Der Wert weicht vom Standard ab (Eingabe per Tippen, Regler und Zahl gehören zusammen, #86).
        field(R.string.dough_water).performScrollTo().performTextClearance()
        field(R.string.dough_water).performTextInput("66")
        composeRule.onNodeWithContentDescription(text(R.string.dough_preset_next)).performScrollTo().performClick()
        waitForText(text(R.string.dough_preset_replace_title))
        screenshot("14o-teigrechner-voreinstellung-ersetzen")
        composeRule.onNode(hasText(text(R.string.cancel)) and hasClickAction()).performClick()
        waitUntilGone(text(R.string.dough_preset_replace_title))
        composeRule.onNodeWithText(text(R.string.dough_preset_custom)).assertExists()
        composeRule.onNodeWithContentDescription(text(R.string.dough_preset_next)).performScrollTo().performClick()
        waitForText(text(R.string.dough_preset_replace_title))
        composeRule.onNode(hasText(text(R.string.dough_preset_replace_confirm)) and hasClickAction()).performClick()
        // Von „Eigene Werte“ geht „Weiter“ auf den ersten Eintrag; danach passen die Werte, es kommt keine Nachfrage mehr.
        waitForText(text(R.string.dough_preset_position, 1, 3))
        composeRule.onNodeWithContentDescription(text(R.string.dough_preset_next)).performScrollTo().performClick()
        waitForText(text(R.string.dough_preset_neapolitan))
        composeRule.onNodeWithText(text(R.string.dough_preset_position, 2, 3)).assertExists()
        screenshot("14l-teigrechner-voreinstellung")
        doughPresetAction(R.string.dough_preset_save)
        waitForText(text(R.string.dough_preset_save_title))
        field(R.string.dough_preset_name).performTextInput("Mein Teig")
        screenshot("14m-teigrechner-voreinstellung-speichern")
        composeRule.onNode(hasText(text(R.string.save)) and hasClickAction()).performClick()
        waitForText("Mein Teig")
        composeRule.onNodeWithText(text(R.string.dough_preset_position, 4, 4)).assertExists()
        doughPresetAction(R.string.dough_preset_delete)
        waitForText(text(R.string.dough_preset_delete_title, "Mein Teig"))
        composeRule.onNode(hasText(text(R.string.delete)) and hasClickAction()).performClick()
        waitUntilGone("Mein Teig")
        composeRule.onNodeWithContentDescription(text(R.string.dough_preset_previous)).performScrollTo().performClick()
        waitForText(text(R.string.dough_preset_default))

        // Teigwissen (#84, Etappe 1): „Was bewirkt das?“ an der Karte „Teig“ (bei normaler Schrift im Menü) öffnet kurze Hinweise.
        if (variant == DisplayVariant.LARGE_TEXT) {
            composeRule.onNodeWithContentDescription(text(R.string.dough_help_open, text(R.string.dough_section_dough)))
                .performScrollTo().performClick()
        } else {
            doughPresetAction(R.string.dough_help_title)
        }
        waitForText(text(R.string.dough_help_water_effect))
        screenshot("14n-teigrechner-hinweise")
        composeRule.onNode(hasText(text(R.string.dough_help_close)) and hasClickAction()).performClick()
        waitUntilGone(text(R.string.dough_help_water_effect))

        // Eine Bedienung pro Wert (#86): Regler mit antippbarer Zahl. Wasseranteil tippen, der Standard wird genannt,
        // „Auf Standard“ stellt ihn wieder her.
        field(R.string.dough_water).performScrollTo().performTextClearance()
        field(R.string.dough_water).performTextInput("66")
        waitForText(text(R.string.dough_default_value, "65 %"))
        screenshot("14d-teigrechner-standard")
        composeRule.onNodeWithText(text(R.string.dough_default_reset_short)).performScrollTo().performClick()
        waitUntilGone(text(R.string.dough_default_value, "65 %"))

        // Bei großer Schrift hat jeder Wert einen Regler; sonst stehen Minus und Plus am Feld (Entwurf vom 22.10.2026).
        if (variant == DisplayVariant.LARGE_TEXT) {
            composeRule.waitUntil(TIMEOUT_MILLIS) {
                composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).fetchSemanticsNodes().isNotEmpty()
            }
        }
        screenshot("14j-teigrechner-regler")

        if (variant == DisplayVariant.LARGE_TEXT) {
            // Bei großer Schrift sind Hefeart und Vorteig Auswahlfelder statt Chips.
            composeRule.onAllNodesWithText(text(R.string.dough_yeast_fresh))[0].performScrollTo().performClick()
            waitForText(text(R.string.dough_yeast_instant))
            screenshot("14k-teigrechner-auswahlfeld")
            clickVisible(text(R.string.dough_yeast_instant))
        }
        // Das Auswahlfeld des Vorteigs liest sich als „Vorteig: Ohne“; die Liste öffnet die ganze Fläche.
        val prefix = text(R.string.dough_section_preferment) + ":"
        composeRule.onNode(
            SemanticsMatcher("Beschreibung beginnt mit $prefix") {
                it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { d -> d.startsWith(prefix) } == true
            },
        ).performScrollTo().performClick()
        composeRule.onAllNodesWithText(text(R.string.dough_pre_biga))[0].performClick()
        // Mit Vorteig erscheinen seine Felder; bei großer Schrift steht die Erklärung dabei.
        // (Der Feldname enthält „%“; deshalb ohne Formatierung lesen.)
        waitForText(activity.resources.getText(R.string.dough_preferment_flour).toString())

        screenshot("14b-teigrechner-biga")

        composeRule.onNodeWithText(text(R.string.dough_save)).performClick()
        composeRule.waitUntil(10_000) {
            runBlocking { container().repository.getAll().any { it.isDraft && it.ingredients.any { line -> line.isHeading } } }
        }
        val draft = runBlocking { container().repository.getAll().first { it.isDraft } }
        assertEquals(com.moltobene.app.data.SourceType.OWN, draft.source?.type)
        // Pizza und Brot bekommen einen Zeitplan (#71); die Biga reift 1 bis 3 Tage.
        assertTrue(draft.timeline.isNotEmpty())
        assertEquals(draft.timeline.size, draft.steps.size)
        waitForText(text(R.string.dough_saved))
        screenshot("14c-teigrechner-entwurf")
    }

    /** Zeitplan eines Teigs (#71): Teig starten, Phase abschließen, laufender Teig auf der Startseite. */
    @Test
    fun teigZeitplan() {
        addSampleRecipes()
        val title = "Pizzateig mit Biga"
        runBlocking {
            container().repository.save(
                com.moltobene.app.data.Recipe(
                    id = com.moltobene.app.data.RecipeIds.newId(),
                    title = title,
                    language = "de",
                    ingredients = listOf(com.moltobene.app.data.Ingredient("500 g Mehl")),
                    steps = listOf("Biga reifen lassen (1 bis 3 Tage, im Kühlschrank)", "Backen (8 bis 15 Minuten, im Backofen)"),
                    timeline = listOf(
                        com.moltobene.app.data.TimelinePhase("Biga reifen lassen", 24 * 60, 72 * 60, com.moltobene.app.data.PhasePlace.FRIDGE),
                        com.moltobene.app.data.TimelinePhase("Teig kneten", 10, 30, com.moltobene.app.data.PhasePlace.NONE),
                        com.moltobene.app.data.TimelinePhase("Backen", 8, 15, com.moltobene.app.data.PhasePlace.OVEN),
                    ),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
        openRecipe(title)
        waitForText(text(R.string.schedule_start))
        composeRule.onNodeWithText(text(R.string.schedule_start)).performScrollTo().assertIsDisplayed()
        screenshot("14p-zeitplan")
        composeRule.onNodeWithText(text(R.string.schedule_start)).performScrollTo().performClick()
        waitForText(text(R.string.schedule_complete))
        composeRule.onNodeWithText(text(R.string.schedule_complete)).performScrollTo().assertIsDisplayed()
        screenshot("14q-zeitplan-lauf")
        composeRule.onNodeWithText(text(R.string.schedule_complete)).performScrollTo().performClick()
        waitForText(text(R.string.schedule_undo))
        screenshot("14r-zeitplan-phase-erledigt")
        val run = runBlocking { container().repository.observeRuns().first().single() }
        assertEquals(1, run.doneAt.size)

        // Zurück zur Startseite: Der laufende Teig steht ganz oben.
        pressBack()
        pressBack()
        waitForText(text(R.string.home_running_title))
        composeRule.onAllNodesWithText(title).onFirst().performScrollTo().assertIsDisplayed()
        screenshot("14s-startseite-laufender-teig")
    }

    /** Rezept übersetzen (#60): Umschalter „DE | EN“, Übersetzung mit Hinweis; Abhaken gilt auch im Original. */
    @Test
    fun uebersetzen() {
        container().translationEngine = SampleTranslation.engine()
        addSampleRecipes()
        openRecipe(SampleTranslation.TITLE)
        waitForText(SampleTranslation.FIRST_INGREDIENT)
        screenshot("12c-rezept-englisch")

        // Der Umschalter steht oben in der Leiste und bleibt beim Blättern sichtbar.
        composeRule.onNodeWithText("DE").performClick()
        waitForText(SampleTranslation.FIRST_INGREDIENT_DE)
        screenshot("12d-rezept-uebersetzt")

        composeRule.onNodeWithText(SampleTranslation.FIRST_INGREDIENT_DE).performScrollTo().performClick()
        composeRule.onNodeWithText(SampleTranslation.FIRST_INGREDIENT_DE).assertIsOn()
        val stored = runBlocking { container().repository.getAll().first { it.title == SampleTranslation.TITLE } }
        assertEquals(SampleTranslation.FIRST_INGREDIENT, stored.ingredients.first().text)

        // Zurück zum Original: Das Häkchen bleibt, die Übersetzung ist gespeichert.
        composeRule.onNodeWithText(text(R.string.translation_code_original, "EN")).performClick()
        waitForText(SampleTranslation.FIRST_INGREDIENT)
        composeRule.onNodeWithText(SampleTranslation.FIRST_INGREDIENT).assertIsOn()
    }

    /** Ohne Sprachpaket: Hinweis mit dem Weg zu den Einstellungen (#60). */
    @Test
    fun uebersetzenOhneSprachpaket() {
        container().translationEngine = SampleTranslation.engine(available = false)
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        composeRule.onNodeWithText("EN").performClick()
        waitForText(text(R.string.translation_open_settings))
        screenshot("12e-sprachpaket-fehlt")

        composeRule.onNodeWithText(text(R.string.translation_open_settings)).performClick()
        waitForText(text(R.string.settings_section_translation))
        openSettingsGroup(R.string.settings_section_translation)
        waitForText(text(R.string.language_pack_title))
        composeRule.onNodeWithText(text(R.string.language_pack_title)).performScrollTo()
        screenshot("12f-einstellungen-sprachpaket")
    }

    /** Sprache des Rezepts beim Bearbeiten wählen (#62); danach passt der Umschalter „DE | EN“. */
    @Test
    fun spracheWaehlen() {
        addSampleRecipes()
        openRecipe(SampleRecipes.POTATO_SALAD)
        waitForText("1 kg festkochende Kartoffeln")
        composeRule.onNodeWithContentDescription(text(R.string.edit_recipe)).performClick()
        waitForField(R.string.field_title)

        composeRule.onNodeWithText(text(R.string.field_language, "Deutsch")).performScrollTo().performClick()
        waitForText("English")
        screenshot("13a-sprache-waehlen")
        composeRule.onNodeWithText("English").performClick()
        composeRule.onNodeWithText(text(R.string.field_language, "English")).assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.save)).performClick()
        waitForText(text(R.string.translation_code_original, "EN"))
        val stored = runBlocking { container().repository.getAll().first { it.title == SampleRecipes.POTATO_SALAD } }
        assertEquals("en", stored.language)
    }

    /**
     * Schlagwörter (#57): im Bearbeiten ein mitgeliefertes wählen und ein eigenes hinzufügen, in der Rezeptansicht
     * sehen, in der Sammlung danach filtern (bei Schrift 200 % über die Liste „Schlagwort: Alle“).
     */
    @Test
    fun schlagwoerter() {
        addSampleRecipes()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        composeRule.onNodeWithContentDescription(text(R.string.edit_recipe)).performClick()
        waitForField(R.string.field_title)

        composeRule.onNodeWithText(text(R.string.tag_sauce)).performScrollTo().performClick()
        input(R.string.tags_custom, "Omas Klassiker")
        composeRule.onNode(hasText(text(R.string.tags_add)) and hasClickAction()).performScrollTo().performClick()
        composeRule.onNodeWithText("Omas Klassiker").performScrollTo().assertIsDisplayed()
        screenshot("15a-schlagwoerter-bearbeiten")
        composeRule.onNodeWithText(text(R.string.save)).performClick()

        waitForText("Omas Klassiker")
        composeRule.onNodeWithText(text(R.string.tag_sauce)).assertIsDisplayed()
        val stored = runBlocking { container().repository.getAll().first { it.title == SampleRecipes.TOMATO_SAUCE } }
        assertEquals(listOf("sauce", "Omas Klassiker"), stored.tags.map { it.name })
        assertEquals(listOf("sauce", null), stored.tags.map { it.predefinedKey })
        screenshot("15b-schlagwoerter-rezept")

        pressBack()
        val sauces = text(R.string.tag_filter_chip, text(R.string.tag_sauce), 1)
        val large = variant == DisplayVariant.LARGE_TEXT
        // Die Sammlung kann bei großer Schrift nicht alles zugleich zeigen; Rezepte werden darum gezielt angesteuert.
        waitForText(if (large) text(R.string.tag_filter_all) else sauces)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(SampleRecipes.POTATO_SALAD))
        if (large) {
            composeRule.onNodeWithText(text(R.string.tag_filter_all)).performScrollTo().performClick()
            waitForText(sauces)
            composeRule.onNodeWithText(sauces).performClick()
            composeRule.onNodeWithText(text(R.string.close)).performClick()
        } else {
            composeRule.onNodeWithText(sauces).performScrollTo().performClick()
        }
        waitUntilGone(SampleRecipes.POTATO_SALAD)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(SampleRecipes.TOMATO_SAUCE))
        composeRule.onNodeWithText(SampleRecipes.TOMATO_SAUCE).assertIsDisplayed()
        screenshot("15c-filter-schlagwort")
    }

    /**
     * Grundrezepte (#82): von der Startseite aus ein Grundrezept ansehen, als normales Rezept übernehmen (Quelle
     * „Moltobene“, mit Schlagwörtern) und beim zweiten Mal den Hinweis „Schon in der Sammlung“ sehen.
     */
    @Test
    fun grundrezepte() {
        addSampleRecipes()
        waitForText(SampleRecipes.POTATO_SALAD)
        pressBack()
        waitForText(text(R.string.home_recipes_title))
        composeRule.onNodeWithText(text(R.string.basics_title)).performScrollTo().performClick()
        waitForText(text(R.string.basics_intro))

        val first = runBlocking { container().basicRecipes.load() }.first()
        val title = BasicRecipes.text(first, variant.contentLanguage).title
        composeRule.onNodeWithText(title).performScrollTo().performClick()
        waitForText(text(R.string.instructions))
        screenshot("16a-grundrezepte")

        adoptFirstBasic()
        waitForText(text(R.string.basics_added, title))
        screenshot("16b-grundrezept-uebernommen")
        val stored = runBlocking { container().repository.getAll().filter { it.title == title } }
        assertEquals(1, stored.size)
        assertEquals("Moltobene", stored.single().source?.name)
        assertEquals(variant.contentLanguage, stored.single().language)
        assertTrue(stored.single().tags.any { it.name == "basics" })

        adoptFirstBasic()
        waitForText(text(R.string.basics_duplicate_title))
        screenshot("16c-grundrezept-duplikat")
        composeRule.onNodeWithText(text(R.string.basics_adopt_anyway)).performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            runBlocking { container().repository.getAll().count { it.title == title } } == 2
        }
    }

    /**
     * Teigwissen (#84): aus dem Teigrechner über das Info-Symbol die Artikel öffnen und den ersten (Mehltypen) aufklappen; alle Länder
     * stehen als Überschriften da.
     */
    @Test
    fun teigwissen() {
        // Der Rundgang startet in der Sammlung; die Startseite liegt darunter.
        waitForDescription(text(R.string.add_short))
        pressBack()
        waitForText(text(R.string.home_recipes_title))
        // Teigwissen liegt im Teigrechner: ein Info-Symbol in der obersten Zeile.
        composeRule.onNodeWithText(text(R.string.dough_title)).performScrollTo().performClick()
        waitForText(text(R.string.dough_save))
        composeRule.onNodeWithContentDescription(text(R.string.knowhow_title)).performClick()
        waitForText(text(R.string.knowhow_intro))
        screenshot("17a-teigwissen")

        val first = runBlocking { container().knowHow.load() }.first()
        val article = KnowHow.text(first, variant.contentLanguage)
        composeRule.onNodeWithText(article.title).performScrollTo().performClick()
        waitForText(article.sections.last().heading)
        for (section in article.sections.filter { it.rows.isNotEmpty() }) {
            composeRule.onNodeWithText(section.heading).assertExists()
        }
        screenshot("17b-teigwissen-artikel")
    }

    private fun adoptFirstBasic() {
        // Direkt auslösen statt antippen: Der Hinweis nach dem Übernehmen liegt kurz über dem Knopf.
        composeRule.onAllNodesWithText(text(R.string.basics_adopt)).onFirst().performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
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
        openAddMenu()
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

        // Nur bei großer Schrift scrollt die Seite; sonst steht der Knopf unten und braucht kein Scrollen.
        runCatching { composeRule.onNodeWithText(text(R.string.area_done)).performScrollTo() }
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

    /**
     * „Hinzufügen“ zeigt alle Wege, auch „Aus Audio übernehmen“ (Beta). Die Dateiauswahl von Android und die Erkennung
     * selbst brauchen das Erkennungspaket und echte Audiodateien und gehören nicht in den Rundgang.
     */
    @Test
    fun hinzufuegenMitAudio() {
        openAddMenu()
        waitForText(text(R.string.import_from_audio))
        composeRule.onNodeWithText(text(R.string.import_from_file)).assertExists()
        screenshot("22a-hinzufuegen-wege")
    }

    @Test
    fun ausTextUebernehmen() {
        openAddMenu()
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

    /**
     * Übernehmen ohne Verbindung (#90): Die Meldung bleibt als Karte stehen, der Link ist gespeichert, „Erneut
     * versuchen“ lädt ihn, sobald es wieder geht.
     */
    @Test
    fun ausLinkOhneVerbindung() {
        val online = SamplePage.loader(activity)
        var connected = false
        container().pageLoader = object : com.moltobene.app.data.web.PageLoader {
            override suspend fun loadPage(url: String) =
                if (connected) online.loadPage(url) else throw com.moltobene.app.data.web.WebException(com.moltobene.app.data.web.WebException.Problem.NO_CONNECTION)

            override suspend fun loadImage(url: String, target: java.io.File) = online.loadImage(url, target)
        }
        openAddMenu()
        waitForText(text(R.string.import_from_link))
        composeRule.onNodeWithText(text(R.string.import_from_link)).performClick()
        waitForField(R.string.import_link_field)
        field(R.string.import_link_field).performTextInput(SamplePage.URL)
        composeRule.onNodeWithText(text(R.string.import_text_action)).performClick()
        waitForText(text(R.string.link_error_offline))
        screenshot("25a-aus-link-ohne-verbindung")

        connected = true
        composeRule.onNodeWithText(text(R.string.import_retry)).performClick()
        waitForText(text(R.string.link_done))
        waitUntilGone(text(R.string.link_error_offline))
        screenshot("25b-aus-link-erneut-versucht")
    }

    @Test
    fun ausLinkUebernehmen() {
        // Nachgestellte Rezeptseite statt echtem Internet (#55).
        container().pageLoader = SamplePage.loader(activity)
        openAddMenu()
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

        // Schlagwörter (#57): Kategorie und Stichwörter der Seite nur als Vorschläge; „Reis“ wird angetippt.
        composeRule.onNodeWithText(text(R.string.tags_suggestions)).performScrollTo()
        screenshot("26a-schlagwort-vorschlaege")
        composeRule.onNodeWithText("Reis").performSemanticsAction(SemanticsActions.OnClick)
        waitForDescription(text(R.string.tags_remove, "Reis"))
        composeRule.onNodeWithText("Zitrone").performScrollTo().assertIsDisplayed()

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
        // Nur das angetippte Schlagwort ist gesetzt, die Kategorie „Hauptgerichte“ wurde nicht automatisch übernommen.
        assertEquals(listOf("Reis"), recipe.tags.map { it.name })
        assertEquals("de", recipe.language)
        assertEquals(1, recipe.photoIds.size)
    }

    /**
     * „Aus Link übernehmen“ mit einem YouTube-Video: Die Zutaten kommen aus der Videobeschreibung, ihre Links stehen
     * zur Auswahl (der zum ganzen Rezept zuerst, Instagram nicht) und werden erst auf Wunsch geladen. Das Rezept der
     * gewählten Seite ersetzt dann die Zutaten aus der Beschreibung; die Seite wird zur Quelle, das Video steht unter
     * „Video“ – ohne den Zusatz „si=…“.
     */
    @Test
    fun ausVideoUebernehmen() {
        container().pageLoader = SamplePage.loader(activity)
        openAddMenu()
        waitForText(text(R.string.import_from_link))
        composeRule.onNodeWithText(text(R.string.import_from_link)).performClick()
        waitForField(R.string.import_link_field)
        field(R.string.import_link_field).performTextInput(SamplePage.VIDEO_URL)
        composeRule.onNodeWithText(text(R.string.import_text_action)).performClick()

        waitForText(text(R.string.video_done))
        // Das Vorschaubild des Videos wird zum Foto, kurz nach dem Rezept.
        waitForText(text(R.string.photo_remove))
        composeRule.onNode(hasText(SamplePage.VIDEO_INGREDIENT, substring = true) and hasSetTextAction()).assertExists()
        // Die Auswahl ganz zeigen: Text und alle Links darunter, der Link zum Rezept zuerst.
        val offer = activity.resources.getQuantityString(R.plurals.video_recipe_links, 2, 2)
        val links = composeRule.onAllNodes(hasClickAction() and hasAnySibling(hasText(offer)))
        links.assertCountEquals(2)
        links[0].assertTextEquals(SamplePage.VIDEO_RECIPE_LINK)
        links[1].assertTextEquals(SamplePage.VIDEO_BLOG_LINK)
        links[1].performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(offer).performScrollTo()
        // Bis eine Rezeptseite gewählt ist, ist das Video Quelle und steht schon unter „Video“.
        composeRule.onAllNodes(hasText(SamplePage.VIDEO_WATCH_URL) and hasSetTextAction()).assertCountEquals(2)
        screenshot("27a-aus-video-uebernommen")

        links[0].performClick()
        waitForText(text(R.string.link_done))
        waitUntilGone(offer)

        composeRule.onNodeWithText(text(R.string.save)).performClick()
        waitForText("320 g Risottoreis")
        val recipe = runBlocking { container().repository.getAll().single() }
        assertEquals(SamplePage.TITLE, recipe.title)
        // Die Zutaten der Rezeptseite ersetzen die aus der Beschreibung, statt sie zu verdoppeln.
        assertEquals(5, recipe.ingredients.size)
        assertEquals(3, recipe.steps.size)
        assertEquals(SamplePage.CLEAN_URL, recipe.source?.url)
        // Die Vorschläge der Seite wurden nicht angetippt, also ist auch nichts gesetzt.
        assertEquals(emptyList<String>(), recipe.tags.map { it.name })
        assertEquals(SamplePage.VIDEO_WATCH_URL, recipe.videoUrl)
        assertEquals(1, recipe.photoIds.size)

        // In der Rezeptansicht stehen Quelle und Video untereinander.
        openRecipeTab(R.string.source)
        composeRule.onNodeWithText(SamplePage.VIDEO_WATCH_URL).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(SamplePage.CLEAN_URL).assertExists()
        screenshot("27b-quelle-und-video")
    }

    /**
     * „Öffnen mit…“ für eine Rezeptdatei (#54), wie aus WhatsApp oder dem Dateimanager: Android öffnet Moltobene
     * mit der Datei, die App liest sie ohne Internet und übernimmt auch das eingebettete Foto.
     */
    @Test
    fun ausDateiUebernehmen() {
        val file = SamplePage.recipeFile(activity)
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(file, RecipeJsonLd.MIME_TYPE)
                    .setClass(activity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
            waitForText(text(R.string.file_done))
            waitForText(text(R.string.photo_remove))
            composeRule.onNode(hasSetTextAction() and hasText(SamplePage.FILE_TITLE)).performScrollTo().assertIsDisplayed()
            screenshot("28-aus-datei-uebernommen")

            composeRule.onNodeWithText(text(R.string.save)).performClick()
            waitForText(SamplePage.FILE_INGREDIENT)
            val recipe = runBlocking { container().repository.getAll().single() }
            assertEquals(SamplePage.FILE_TITLE, recipe.title)
            assertEquals(3, recipe.servings)
            assertEquals(3, recipe.ingredients.size)
            assertEquals(3, recipe.steps.size)
            assertEquals(15, recipe.prepMinutes)
            assertEquals(40, recipe.totalMinutes)
            // Buch und Seite aus „Familienkochbuch, S. 12“ getrennt.
            assertEquals("Familienkochbuch", recipe.source?.name)
            assertEquals("12", recipe.source?.page)
            // Schlagwörter aus der Rezeptdatei (#57): „Hauptgerichte“ wird wieder zum mitgelieferten Schlagwort.
            assertEquals(listOf("main_course", "Vegetarisch"), recipe.tags.map { it.name })
            assertEquals(1, recipe.photoIds.size)
        } finally {
            SamplePage.deleteRecipeFile(activity, file)
            closeOtherWindows()
        }
    }

    @Test
    fun einstellungen() {
        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        // Die Liste zeigt oben „Schnell“, dann die Gruppen mit ihrem Stand; „Kochbuch“ führt zum Sichern.
        waitForText(text(R.string.settings_quick))
        screenshot("29-einstellungen")
        openSettingsGroup(R.string.collection_title)
        waitForText(text(R.string.backup_title))
        screenshot("29-einstellungen-kochbuch")
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()

        openSettingsGroup(R.string.settings_section_about)
        screenshot("29d-einstellungen-info")
        composeRule.onNodeWithText(text(R.string.whats_new_title, WhatsNew.VERSION_NAME)).performScrollTo().performClick()
        waitForText(text(R.string.close))
        screenshot("30-neu-in-version")
        composeRule.onNodeWithText(text(R.string.close)).performClick()

        composeRule.onNodeWithText(text(R.string.privacy_title)).performScrollTo().performClick()
        waitForText(text(R.string.close))
        screenshot("31-datenschutz")
        composeRule.onNodeWithText(text(R.string.close)).performClick()

        composeRule.onNodeWithText(text(R.string.licenses_title)).performScrollTo().performClick()
        waitForText(text(R.string.licenses_intro))
        screenshot("32-lizenzen")
    }

    /** „Speicher“ in den Einstellungen: Posten je Bereich und die Summe; Pakete lassen sich dort löschen. */
    @Test
    fun speicherAnzeige() {
        addSampleRecipes()
        waitForText(SampleRecipes.POTATO_SALAD)
        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        waitForText(text(R.string.settings_section_storage))
        openSettingsGroup(R.string.settings_section_storage)
        waitForText(text(R.string.storage_total))
        composeRule.onNodeWithText(text(R.string.storage_total)).performScrollTo()
        screenshot("29e-speicher")
    }

    /**
     * „Darstellung“ in den Einstellungen: Farbwelt und Hell/Dunkel wählen, die ganze App wechselt sofort. Gewählt wird
     * Hell bzw. Dunkel wie in der Darstellung des Rundgangs, damit das Handy dabei bleibt, wie es ist.
     */
    @Test
    fun darstellungWaehlen() {
        addSampleRecipes()
        waitForText(SampleRecipes.POTATO_SALAD)
        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        waitForText(text(R.string.settings_section_appearance))
        openSettingsGroup(R.string.settings_section_appearance)

        composeRule.onNodeWithText(text(R.string.palette_terracotta)).performScrollTo().performClick()
        val mode = if (variant.dark) ThemeMode.DARK else ThemeMode.LIGHT
        composeRule.onNodeWithText(text(if (variant.dark) R.string.theme_dark else R.string.theme_light)).performScrollTo().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { container().preferences.appearance.value == Appearance(mode, Palette.TERRACOTTA) }
        composeRule.onNodeWithText(text(R.string.palette_terracotta)).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.appearance_mode)).performScrollTo()
        screenshot("29a-darstellung")

        leaveSettings()
        waitForText(SampleRecipes.POTATO_SALAD)
        screenshot("29b-sammlung-terrakotta")
    }

    /**
     * „Kochen“ in den Einstellungen (#68): „Bildschirm in der Rezeptansicht“ wählen. Mit Frist bleibt der Bildschirm
     * in der Rezeptansicht an, mit „Wie das Gerät“ nicht. Die Frist selbst prüft `ScreenOnTest` mit nachgestellter Uhr.
     */
    @Test
    fun bildschirmInDerRezeptansicht() {
        addSampleRecipes()
        waitForText(SampleRecipes.POTATO_SALAD)
        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        waitForText(text(R.string.settings_section_cooking))
        openSettingsGroup(R.string.settings_section_cooking)

        val fifteen = activity.resources.getQuantityString(R.plurals.screen_on_off_after, 15, 15)
        composeRule.onNodeWithText(fifteen).performScrollTo().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { container().preferences.screenOn.value == ScreenOn.OFF_AFTER_15 }
        composeRule.onNodeWithText(fifteen).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.settings_section_cooking)).performScrollTo()
        screenshot("29c-bildschirm-in-der-rezeptansicht")

        leaveSettings()
        openRecipe(SampleRecipes.TOMATO_SAUCE)
        waitForText("800 g reife Tomaten")
        composeRule.waitUntil(TIMEOUT_MILLIS) { screenKeptOn() }

        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.settings_title)).performClick()
        openSettingsGroup(R.string.settings_section_cooking)
        // „Wie das Gerät“ steht auch unter „Hell oder dunkel“; der Abschnitt „Kochen“ kommt zuerst.
        composeRule.onAllNodesWithText(text(R.string.screen_on_device)).onFirst().performScrollTo().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { container().preferences.screenOn.value == ScreenOn.DEVICE }
        leaveSettings()
        // Die Sammlung steht noch beim Rezept von eben (bei großer Schrift ist der Anfang der Liste nicht mehr geladen).
        composeRule.waitUntil(TIMEOUT_MILLIS) { composeRule.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(SampleRecipes.TOMATO_SAUCE))
        composeRule.onNodeWithText(SampleRecipes.TOMATO_SAUCE).performSemanticsAction(SemanticsActions.OnClick)
        waitForText("800 g reife Tomaten")
        composeRule.waitForIdle()
        assertFalse(screenKeptOn())
    }

    // --- Hilfsfunktionen ---

    /** Wechselt in der Rezeptansicht zum Reiter „Zutaten“, „Zubereitung“ oder „Notizen“ (Entwurf vom 22.10.2026). */
    private fun openRecipeTab(@StringRes label: Int) {
        composeRule.onNode(isSelectable() and hasText(text(label))).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
    }

    /** Verlässt die Einstellungen: Ist eine Gruppe offen, führt Zurück erst zur Liste, dann aus den Einstellungen. */
    private fun leaveSettings() {
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithText(text(R.string.settings_quick)).fetchSemanticsNodes().isEmpty()) {
            composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithContentDescription(text(R.string.back)).performClick()
    }

    /** Öffnet eine Gruppe der Einstellungen (nur aufrufen, wenn die Liste zu sehen ist); sie ersetzt dann die Liste. */
    private fun openSettingsGroup(@StringRes title: Int) {
        composeRule.onNodeWithText(text(title)).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    private val activity get() = composeRule.activity

    /** Text aus strings.xml in der Sprache der jeweiligen Darstellung. */
    private fun text(@StringRes id: Int, vararg args: Any): String = activity.getString(id, *args)

    /** Eintrag der Voreinstellungszeile im Teigrechner: bei normaler Schrift im Menü ⋮, bei großer Schrift ein Knopf in der Karte. */
    private fun doughPresetAction(@StringRes label: Int) {
        if (variant == DisplayVariant.LARGE_TEXT) {
            composeRule.onNodeWithText(text(label)).performScrollTo().performClick()
        } else {
            composeRule.onNodeWithContentDescription(text(R.string.more_options)).performClick()
            composeRule.onNodeWithText(text(label)).performClick()
        }
    }

    private fun field(@StringRes label: Int) = composeRule.onNode(hasSetTextAction() and (hasText(text(label)) or hasContentDescription(text(label))))

    private fun input(@StringRes label: Int, value: String) {
        field(label).performScrollTo().performTextInput(value)
    }

    private fun pressBack() = InstrumentationRegistry.getInstrumentation().runOnMainSync {
        activity.onBackPressedDispatcher.onBackPressed()
    }

    private fun container() = (activity.application as MoltobeneApplication).container

    /** Hält gerade eine Ansicht der App den Bildschirm an („Bildschirm anlassen“)? */
    private fun screenKeptOn(): Boolean {
        var on = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync { on = activity.window.decorView.keepsScreenOn() }
        return on
    }

    private fun View.keepsScreenOn(): Boolean =
        keepScreenOn || (this is ViewGroup && (0 until childCount).any { getChildAt(it).keepsScreenOn() })

    /** Schließt Fenster der App, die ein Schritt zusätzlich geöffnet hat (z. B. über „Öffnen mit…“). */
    private fun closeOtherWindows() = InstrumentationRegistry.getInstrumentation().runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            .filter { it !== activity }
            .forEach { it.finish() }
    }

    private fun addSampleRecipes() = runBlocking { SampleRecipes.addTo(container(), activity) }

    private fun openRecipe(title: String) {
        waitForText(SampleRecipes.POTATO_SALAD)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(title))
        // Direkt auslösen statt antippen: Ganz unten liegen die Knöpfe „Rezept übernehmen“ und
        // „Rezept hinzufügen“ über der Liste, ein Tipp in die Mitte des Eintrags kann sie treffen.
        composeRule.onNodeWithText(title).performSemanticsAction(SemanticsActions.OnClick)
    }

    /** Öffnet das Menü des Hauptknopfs „Hinzufügen“ in der Sammlung. */
    private fun openAddMenu() {
        waitForDescription(text(R.string.add_short))
        composeRule.onNodeWithContentDescription(text(R.string.add_short)).performClick()
    }

    /**
     * Tippt auf das erste sichtbare Element mit diesem Text oder dieser Beschreibung. Manche Aktionen gibt es doppelt,
     * z. B. „Rezept hinzufügen“ in der leeren Sammlung und als Symbol oben in der Leiste.
     */
    private fun clickVisible(value: String) {
        val nodes = composeRule.onAllNodes(hasText(value) or hasContentDescription(value))
        val count = nodes.fetchSemanticsNodes().size
        val index = (0 until count).firstOrNull { runCatching { nodes[it].assertIsDisplayed() }.isSuccess }
            ?: throw AssertionError("„$value“ ist auf dem Bildschirm nicht sichtbar.")
        nodes[index].performClick()
    }

    private fun waitForText(value: String, substring: Boolean = false) = composeRule.waitUntil(TIMEOUT_MILLIS) {
        composeRule.onAllNodesWithText(value, substring = substring).fetchSemanticsNodes().isNotEmpty()
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
        fun variants(): List<DisplayVariant> = DisplayVariant.entries.filter { !it.isTablet }
    }
}
