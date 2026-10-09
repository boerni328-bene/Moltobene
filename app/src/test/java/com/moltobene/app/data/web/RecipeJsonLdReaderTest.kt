package com.moltobene.app.data.web

import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.RecipeYield
import com.moltobene.app.data.SourceType
import com.moltobene.app.data.share.RecipeJsonLd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Rezepte im Standard schema.org/Recipe, in den Spielarten der Koch-Portale (#54, #55). Alle Beispiele sind selbst geschrieben. */
class RecipeJsonLdReaderTest {

    @Test
    fun einfachesRezept() {
        val recipe = read(
            """
            {"@context":"https://schema.org","@type":"Recipe","name":"Linsensuppe","inLanguage":"de-DE",
             "recipeYield":"4 Portionen","prepTime":"PT15M","cookTime":"PT40M",
             "recipeIngredient":["250 g Linsen","1 Zwiebel","1 l Gemüsebrühe"],
             "recipeInstructions":"Zwiebel würfeln und andünsten.\nLinsen und Brühe dazugeben.\nGut 40 Minuten köcheln lassen.",
             "image":"https://example.org/bilder/linsensuppe.jpg"}
            """,
        )
        assertEquals("Linsensuppe", recipe.title)
        assertEquals("de", recipe.language)
        assertEquals(4, recipe.servings)
        assertNull(recipe.servingsUnit)
        assertEquals(listOf("250 g Linsen", "1 Zwiebel", "1 l Gemüsebrühe"), recipe.ingredients)
        assertEquals(3, recipe.steps.size)
        assertEquals(15, recipe.prepMinutes)
        assertEquals(55, recipe.totalMinutes)
        assertEquals(listOf("https://example.org/bilder/linsensuppe.jpg"), recipe.imageUrls)
    }

    @Test
    fun rezeptImGraphMitTypAlsListe() {
        val recipe = read(
            """
            {"@context":"https://schema.org","@graph":[
              {"@type":"WebSite","name":"Ein Kochblog"},
              {"@type":"BreadcrumbList","itemListElement":[]},
              {"@type":["Recipe","NewsArticle"],"name":"Apfel&shy;kuchen &amp; Streusel",
               "recipeIngredient":["<b>200 g</b> Mehl","3 Äpfel"],
               "recipeInstructions":[{"@type":"HowToStep","text":"Teig kneten."},{"@type":"HowToStep","text":"Backen."}]}
            ]}
            """,
        )
        assertEquals("Apfelkuchen & Streusel", recipe.title)
        assertEquals(listOf("200 g Mehl", "3 Äpfel"), recipe.ingredients)
        assertEquals(listOf("Teig kneten.", "Backen."), recipe.steps)
    }

    @Test
    fun abschnitteDerZubereitung() {
        val recipe = read(
            """
            {"@type":"Recipe","name":"Pizza","recipeIngredient":["500 g Mehl"],
             "recipeInstructions":[
               {"@type":"HowToSection","name":"Teig","itemListElement":[
                 {"@type":"HowToStep","text":"Mehl, Hefe und Wasser verkneten."},
                 {"@type":"HowToStep","text":"Eine Stunde gehen lassen."}]},
               {"@type":"HowToSection","name":"Belag","itemListElement":[
                 {"@type":"HowToStep","name":"Belegen","text":"Tomaten und Mozzarella verteilen."}]}]}
            """,
        )
        assertEquals(
            listOf("Teig: Mehl, Hefe und Wasser verkneten.", "Eine Stunde gehen lassen.", "Belag: Tomaten und Mozzarella verteilen."),
            recipe.steps,
        )
    }

    @Test
    fun zubereitungAlsHtmlMitNummern() {
        val recipe = read(
            """
            {"@type":"Recipe","name":"Risotto","recipeIngredient":["320 g Reis"],
             "recipeInstructions":"<ol><li>1. Zwiebel anschwitzen.</li><li>2) Reis dazugeben.</li><li>Schritt 3: 1.5 l Brühe nach und nach angießen.</li></ol>"}
            """,
        )
        assertEquals(listOf("Zwiebel anschwitzen.", "Reis dazugeben.", "1.5 l Brühe nach und nach angießen."), recipe.steps)
    }

    @Test
    fun portionenUndFotosInVerschiedenenFormen() {
        val recipe = read(
            """
            {"@type":"Recipe","name":"Muffins","recipeIngredient":["2 Eier"],"recipeYield":["12","12 Stück"],
             "image":[{"@type":"ImageObject","url":"/bilder/klein.jpg","width":"300"},
                      {"@type":"ImageObject","url":"/bilder/gross.jpg","width":1200}]}
            """,
            baseUrl = "https://example.org/rezepte/muffins",
        )
        assertEquals(12, recipe.servings)
        assertEquals("Stück", recipe.servingsUnit)
        assertEquals("https://example.org/bilder/gross.jpg", recipe.imageUrls.first())
    }

    @Test
    fun nichtGanzKorrektesJson() {
        val recipe = read(
            """
            <!-- Rezept -->
            {"@type": "Recipe", "name": "Brot", "recipeIngredient": ["500 g Mehl", "1 TL Salz",],
             "recipeInstructions": "Alles verkneten
             und backen.",}
            """,
        )
        assertEquals("Brot", recipe.title)
        assertEquals(2, recipe.ingredients.size)
    }

    @Test
    fun ohneRezeptGibtEsNichts() {
        assertNull(RecipeJsonLdReader.read("""{"@type":"Article","name":"Nachrichten"}"""))
        assertNull(RecipeJsonLdReader.read("kein JSON"))
    }

    /** Was Moltobene beim Teilen als Rezeptdatei schreibt, liest es auch wieder (Grundlage für #54). */
    @Test
    fun eigeneRezeptdateiHinUndZurueck() {
        val original = Recipe(
            id = "0b0f6c0a-0000-4000-8000-000000000001",
            title = "Omas Gulasch",
            language = "de",
            servings = 6,
            prepMinutes = 30,
            totalMinutes = 150,
            source = RecipeSource(SourceType.WEB, url = "https://example.org/gulasch"),
            videoUrl = "https://vimeo.com/123456",
            ingredients = listOf(Ingredient("Für das Fleisch", isHeading = true), Ingredient("1 kg Rindfleisch")),
            steps = listOf("Fleisch anbraten.", "Zwei Stunden schmoren."),
            createdAt = 0,
            updatedAt = 0,
        )
        val recipe = read(RecipeJsonLd.build(original, untitled = "Ohne Titel", photoJpeg = byteArrayOf(1, 2, 3)))
        assertEquals("Omas Gulasch", recipe.title)
        assertEquals(6, recipe.servings)
        assertEquals(listOf("Für das Fleisch:", "1 kg Rindfleisch"), recipe.ingredients)
        assertEquals(original.steps, recipe.steps)
        assertEquals(30, recipe.prepMinutes)
        assertEquals(150, recipe.totalMinutes)
        assertEquals("https://example.org/gulasch", recipe.url)
        assertEquals("data:image/jpeg;base64,AQID", recipe.imageUrls.single())
        assertEquals("https://vimeo.com/123456", recipe.videoUrl)
    }

    /** Video auf einer Rezeptseite: YouTube als einfacher Link zum Video; Videodateien und die Seite selbst nicht. */
    @Test
    fun videoZumRezept() {
        val base = "\"@context\":\"https://schema.org\",\"@type\":\"Recipe\",\"name\":\"Risotto\",\"url\":\"https://example.org/risotto\""
        assertEquals(
            "https://www.youtube.com/watch?v=AbCdEfGhIjK",
            read("{$base,\"video\":{\"@type\":\"VideoObject\",\"contentUrl\":\"https://cdn.example.org/v.mp4\",\"embedUrl\":\"https://www.youtube.com/embed/AbCdEfGhIjK?rel=0\"}}").videoUrl,
        )
        assertEquals(
            "https://www.youtube.com/watch?v=AbCdEfGhIjK",
            read("{$base,\"video\":[\"https://youtu.be/AbCdEfGhIjK?si=xyz\"]}").videoUrl,
        )
        assertNull(read("{$base,\"video\":{\"@type\":\"VideoObject\",\"contentUrl\":\"https://cdn.example.org/v.mp4\"}}").videoUrl)
        assertNull(read("{$base,\"video\":{\"@type\":\"VideoObject\",\"url\":\"https://example.org/risotto#video\"}}").videoUrl)
        assertNull(read("{$base,\"video\":{\"@type\":\"VideoObject\",\"url\":\"javascript:alert(1)\"}}").videoUrl)
    }

    @Test
    fun portionen() {
        fun yield(text: String) = RecipeYield.parse(text)
        assertEquals(RecipeYield.Servings(4, null), yield("4 Portionen"))
        assertEquals(RecipeYield.Servings(6, null), yield("Für 6 Personen"))
        assertEquals(RecipeYield.Servings(4, null), yield("Serves 4"))
        assertEquals(RecipeYield.Servings(4, null), yield("4-6 servings"))
        assertEquals(RecipeYield.Servings(12, "Stück"), yield("12 Stück"))
        assertEquals(RecipeYield.Servings(1, "Springform (26 cm)"), yield("1 Springform (26 cm)"))
        assertNull(yield("einige"))
    }

    @Test
    fun dauern() {
        assertEquals(90, RecipeDuration.minutes("PT1H30M"))
        assertEquals(20, RecipeDuration.minutes("P0DT0H20M"))
        assertEquals(90, RecipeDuration.minutes("PT90M"))
        assertEquals(45, RecipeDuration.minutes("45 Min."))
        assertEquals(75, RecipeDuration.minutes("1 Std. 15 Min."))
        assertNull(RecipeDuration.minutes("PT0M"))
        assertNull(RecipeDuration.minutes("bald"))
    }

    private fun read(json: String, baseUrl: String? = null): WebRecipe {
        val recipe = RecipeJsonLdReader.read(json.trimIndent(), baseUrl)
        assertNotNull(recipe)
        return recipe!!
    }
}
