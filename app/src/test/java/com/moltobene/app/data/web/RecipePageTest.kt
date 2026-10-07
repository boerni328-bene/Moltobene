package com.moltobene.app.data.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lesen ganzer Internetseiten für „Aus Link übernehmen“ (#55). Alle Seiten sind selbst geschrieben. */
class RecipePageTest {

    @Test
    fun rezeptAusEingebettetenDaten() {
        val content = read(
            """
            <html lang="it"><head>
              <title>Pasta al pomodoro | Cucina Esempio</title>
              <meta property="og:site_name" content="Cucina Esempio">
              <meta property="og:image" content="https://example.org/og.jpg">
              <script type="application/ld+json">{"@type":"Organization","name":"Cucina Esempio"}</script>
              <script type="application/ld+json">
                {"@context":"https://schema.org","@type":"Recipe","name":"Pasta al pomodoro",
                 "recipeYield":"2 porzioni","totalTime":"PT25M",
                 "recipeIngredient":["200 g di spaghetti","400 g di pomodori pelati","Basilico"],
                 "recipeInstructions":[{"@type":"HowToStep","text":"Cuocere la pasta."},{"@type":"HowToStep","text":"Condire con il sugo."}]}
              </script>
            </head><body><nav>Menu</nav><article><h1>Pasta al pomodoro</h1><p>Testo lungo …</p></article></body></html>
            """,
        )
        val recipe = content.recipe
        assertNotNull(recipe)
        assertEquals("Pasta al pomodoro", recipe!!.title)
        assertEquals(2, recipe.servings)
        assertEquals(3, recipe.ingredients.size)
        assertEquals(2, recipe.steps.size)
        assertEquals(25, recipe.totalMinutes)
        // Ohne eigenes Foto im Rezept gilt das Vorschaufoto der Seite, ohne Sprachangabe die Sprache der Seite.
        assertEquals(listOf("https://example.org/og.jpg"), recipe.imageUrls)
        assertEquals("it", recipe.language)
        assertEquals("", content.text)
    }

    @Test
    fun rezeptAusMicrodata() {
        val content = read(
            """
            <html><body>
            <div itemscope itemtype="http://schema.org/Recipe">
              <h1 itemprop="name">Kartoffelsalat</h1>
              <div itemprop="author" itemscope itemtype="http://schema.org/Person"><span itemprop="name">Erika</span></div>
              <meta itemprop="totalTime" content="PT1H">
              <span itemprop="recipeYield">4 Personen</span>
              <img itemprop="image" src="/bilder/salat.jpg">
              <ul>
                <li itemprop="recipeIngredient">1 kg Kartoffeln</li>
                <li itemprop="recipeIngredient">1 Zwiebel</li>
              </ul>
              <div itemprop="recipeInstructions"><p>Kartoffeln kochen.</p><p>Mit Zwiebel und Brühe mischen.</p></div>
            </div>
            </body></html>
            """,
        )
        val recipe = content.recipe
        assertNotNull(recipe)
        assertEquals("Kartoffelsalat", recipe!!.title)
        assertEquals(4, recipe.servings)
        assertEquals(listOf("1 kg Kartoffeln", "1 Zwiebel"), recipe.ingredients)
        assertEquals(listOf("Kartoffeln kochen.", "Mit Zwiebel und Brühe mischen."), recipe.steps)
        assertEquals(60, recipe.totalMinutes)
        assertEquals("https://example.org/bilder/salat.jpg", recipe.imageUrls.single())
    }

    @Test
    fun ohneStandardformatBleibtDerText() {
        val content = read(
            """
            <html lang="de"><head><title>Omas Pfannkuchen – Mein Küchenblog</title>
              <meta property="og:site_name" content="Mein Küchenblog"></head>
            <body>
              <header>Startseite · Rezepte · Über mich</header>
              <article>
                <h1>Omas Pfannkuchen</h1>
                <p>Eine kleine Geschichte vorweg.</p>
                <h2>Zutaten</h2>
                <ul><li>250 g Mehl</li><li>3 Eier</li><li>500 ml Milch</li></ul>
                <h2>Zubereitung</h2>
                <p>Alles verrühren.</p><p>In der Pfanne ausbacken.</p>
              </article>
              <footer>Impressum</footer>
              <script>var tracking = true;</script>
            </body></html>
            """,
        )
        assertNull(content.recipe)
        assertEquals("Omas Pfannkuchen", content.title)
        assertEquals("de", content.language)
        assertTrue(content.text.contains("250 g Mehl"))
        assertTrue(content.text.contains("In der Pfanne ausbacken."))
        assertFalse(content.text.contains("Impressum"))
        assertFalse(content.text.contains("tracking"))
        assertTrue(looksLikeRecipe(content.text))
    }

    /**
     * Aufbau wie bei Seiten aus dem Baukasten Webflow (z. B. joshuaweissman.com): „article“ steht nur um die
     * Zutatenliste, die Überschriften und die Zubereitung stehen daneben. Die eingebetteten Rezeptdaten enthalten
     * Zutaten und Schritte erst, wenn ein Skript im Browser sie ergänzt; Titel, Portionen und Zeiten stehen schon darin.
     */
    @Test
    fun articleNurUmDieZutatenliste() {
        val content = read(
            """
            <html lang="en"><head><title>Braised Beans | Chef Example</title>
              <script type="application/ld+json">{"@context":"https://schema.org","@type":"Recipe","name":"Braised Beans",
                "image":"https://example.org/beans.jpg","prepTime":"30 mins","totalTime":"4 hrs","recipeYield":"6"}</script>
            </head><body>
              <nav>Recipes · Shop</nav>
              <main class="main-wrapper">
                <section id="recipe">
                  <div class="ingredients-card">
                    <h2>Ingredients</h2>
                    <article class="w-richtext">
                      <p><strong>Beans:</strong></p>
                      <ul role="list"><li>500 g white beans</li><li>1 onion, halved</li></ul>
                      <p><strong>Topping:</strong></p>
                      <ul role="list"><li>2 Tbsp breadcrumbs</li></ul>
                    </article>
                  </div>
                  <div class="directions-wrapper">
                    <h2>Directions</h2>
                    <div class="w-richtext"><ol><li>Soak the beans overnight.</li><li>Simmer with the onion.</li></ol></div>
                    <p><strong>Notes:</strong></p><p>Leftovers keep for three days.</p>
                  </div>
                </section>
                <section><h2>Comments &amp; Ratings</h2><div class="w-form-done">Thank you! Your submission has been received!</div></section>
                <section><h2>Other recipes</h2><p>Easy Chicken Nuggets</p></section>
              </main>
              <footer>Imprint</footer>
            </body></html>
            """,
        )
        val details = content.recipe
        assertNotNull(details)
        assertFalse(details!!.hasContent)
        assertEquals(6, details.servings)
        assertEquals(30, details.prepMinutes)
        assertEquals(240, details.totalMinutes)
        assertEquals("Braised Beans", content.title)
        assertTrue(looksLikeRecipe(content.text))
        assertTrue(content.text.contains("500 g white beans"))
        assertTrue(content.text.contains("Simmer with the onion."))
        assertTrue(content.text.contains("Leftovers keep for three days."))
        assertFalse(content.text.contains("Thank you"))
        assertFalse(content.text.contains("Easy Chicken Nuggets"))
        assertFalse(content.text.contains("Imprint"))
    }

    /** Steht das ganze Rezept im „article“, bleiben Kommentare und weitere Rezepte darunter draußen. */
    @Test
    fun articleMitGanzemRezeptGehtVor() {
        val content = read(
            """
            <html lang="de"><body><main>
              <article>
                <h1>Linsensuppe</h1>
                <h2>Zutaten</h2><ul><li>200 g Linsen</li></ul>
                <h2>Zubereitung</h2><p>Linsen weich kochen.</p>
              </article>
              <section><h2>Kommentare</h2><p>Sehr lecker!</p></section>
            </main></body></html>
            """,
        )
        assertTrue(content.text.contains("Linsen weich kochen."))
        assertFalse(content.text.contains("Sehr lecker!"))
    }

    @Test
    fun kommentareErstNachDemRezeptAbschneiden() {
        val text = listOf(
            "Kommentare (12)", "Zutaten", "200 g Linsen", "Zubereitung", "Linsen weich kochen.",
            "Weitere Rezepte", "Erbsensuppe",
        ).joinToString("\n")
        val result = RecipePage.withoutTrailingSections(text)
        assertTrue(result.startsWith("Kommentare (12)"))
        assertTrue(result.endsWith("Linsen weich kochen."))
        // Ohne Überschrift „Zutaten“ bleibt alles, wie es ist.
        assertEquals("Kommentare\nSehr gut", RecipePage.withoutTrailingSections("Kommentare\nSehr gut"))
    }

    @Test
    fun seiteOhneRezept() {
        val content = read("<html><head><title>Nachrichten</title></head><body><p>Heute regnet es.</p></body></html>")
        assertNull(content.recipe)
        assertFalse(looksLikeRecipe(content.text))
    }

    @Test
    fun linkDirektAufEineRezeptdatei() {
        val json = """{"@type":"Recipe","name":"Tee","recipeIngredient":["1 Beutel Tee"],"recipeInstructions":"Aufgießen."}"""
        val content = RecipePage.read(WebPage("https://example.org/tee.json", json.toByteArray(), null))
        assertEquals("Tee", content.recipe?.title)
    }

    @Test
    fun zeichensatzDerSeite() {
        val html = "<html><head><meta charset=\"iso-8859-1\"><title>Käse</title></head><body></body></html>"
        val content = RecipePage.read(WebPage("https://example.org/", html.toByteArray(Charsets.ISO_8859_1), null))
        assertEquals("Käse", content.title)
    }

    private fun read(html: String): RecipePage.Content =
        RecipePage.read(WebPage("https://example.org/rezept", html.trimIndent().toByteArray(), "UTF-8"))
}
