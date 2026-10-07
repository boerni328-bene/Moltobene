package com.moltobene.app.data.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rezepte aus Videobeschreibungen. Alle Beschreibungen sind selbst geschrieben, nach dem Muster echter
 * Kochvideos: Rezept zwischen Kapitelmarken, Werbung, Links und Schlagwörtern.
 */
class VideoDescriptionTest {

    private fun video(description: String, title: String = "Schnelle Linsensuppe") = YouTube.Video(
        id = "AbCdEfGhIjK",
        title = title,
        description = description.trimIndent(),
        channel = "Beispielküche",
        imageUrl = "https://i.ytimg.com/vi/AbCdEfGhIjK/maxresdefault.jpg",
    )

    @Test
    fun zutatenUndZubereitungZwischenWerbung() {
        val recipe = VideoDescription.recipe(
            video(
                """
                Heute gibt es eine schnelle Linsensuppe, perfekt für kalte Tage! 🍲

                0:00 Intro
                1:15 Gemüse schneiden
                3:40 Kochen

                Zutaten für 4 Personen:
                🥕 2 Karotten
                250 g rote Linsen
                1 Bund Suppengrün (Karotte, Sellerie und Lauch), fein gewürfelt
                1 l Gemüsebrühe
                Salz und Pfeffer

                Zubereitung:
                Karotten schälen und würfeln.
                Linsen waschen und mit den Karotten in der Brühe 15 Minuten kochen.
                Tomaten dazugeben und abschmecken.

                Meine Töpfe findest du hier: https://www.example.com/shop/toepfe
                ► Instagram: https://instagram.com/beispielkueche

                #linsensuppe #suppe #schnellerezepte
                """,
            ),
        )
        assertNotNull(recipe)
        assertEquals("Schnelle Linsensuppe", recipe!!.title)
        assertEquals(4, recipe.servings)
        // Auch eine lange Zeile bleibt in der Liste der Zutaten.
        assertEquals(
            listOf("2 Karotten", "250 g rote Linsen", "1 Bund Suppengrün (Karotte, Sellerie und Lauch), fein gewürfelt", "1 l Gemüsebrühe", "Salz und Pfeffer"),
            recipe.ingredients,
        )
        assertEquals(
            listOf(
                "Karotten schälen und würfeln.",
                "Linsen waschen und mit den Karotten in der Brühe 15 Minuten kochen.",
                "Tomaten dazugeben und abschmecken.",
            ),
            recipe.steps,
        )
        assertEquals("https://www.youtube.com/watch?v=AbCdEfGhIjK", recipe.url)
        assertEquals(listOf("https://i.ytimg.com/vi/AbCdEfGhIjK/maxresdefault.jpg"), recipe.imageUrls)
        assertEquals("de", recipe.language)
    }

    @Test
    fun zutatenlisteOhneUeberschrift() {
        val recipe = VideoDescription.recipe(
            video(
                """
                Ein Klassiker aus dem Ofen – das Rezept für die Soße gibt es auf unserem Kanal.

                8 Lasagneplatten
                1 Kugel Mozzarella
                400 ml Milch
                40 g Butter
                2 EL Mehl
                Muskatnuss
                Salz & Pfeffer

                #lasagne

                Folge uns auch hier:
                ► https://www.instagram.com/beispielkueche
                ► Website: https://www.example.org/
                """,
                title = "Lasagne",
            ),
        )
        assertNotNull(recipe)
        assertEquals(
            listOf("8 Lasagneplatten", "1 Kugel Mozzarella", "400 ml Milch", "40 g Butter", "2 EL Mehl", "Muskatnuss", "Salz & Pfeffer"),
            recipe!!.ingredients,
        )
        // Ohne Überschrift ist erzählender Text keine Zubereitung.
        assertTrue(recipe.steps.isEmpty())
    }

    @Test
    fun ohneRezept() {
        assertNull(VideoDescription.recipe(video("Heute koche ich einfach drauflos und zeige, was am Ende herauskommt. Viel Spaß beim Zuschauen!\n\n#kochen #essen")))
        assertNull(VideoDescription.recipe(video("")))
        // Nur Werbung und Links.
        assertNull(VideoDescription.recipe(video("Abonniere den Kanal!\nhttps://www.youtube.com/@beispielkueche\n\nMein Messer: https://amzn.to/abc123")))
    }

    @Test
    fun englischMitMethod() {
        val recipe = VideoDescription.recipe(
            video(
                """
                Creamy tomato soup in 20 minutes.

                Serves: 2

                Ingredients
                1 tbsp olive oil
                1 onion, chopped
                400 g tinned tomatoes
                200 ml vegetable stock

                Method
                Fry the onion in the oil until soft.
                Add tomatoes and stock and simmer for 10 minutes.
                Blend until smooth.
                """,
                title = "Tomato soup",
            ),
        )
        assertNotNull(recipe)
        assertEquals(2, recipe!!.servings)
        assertEquals(listOf("1 tbsp olive oil", "1 onion, chopped", "400 g tinned tomatoes", "200 ml vegetable stock"), recipe.ingredients)
        assertEquals(3, recipe.steps.size)
        assertEquals("en", recipe.language)
    }

    @Test
    fun mengenHintenUndProcedimento() {
        val recipe = VideoDescription.recipe(
            video(
                """
                ★ INGREDIENTI:
                Spaghetti 320 g
                Pomodori pelati 400 g
                Basilico q.b.

                PROCEDIMENTO
                Cuocere la pasta in acqua salata.
                Scaldare i pomodori in padella.
                Unire la pasta al sugo e servire.
                """,
                title = "Spaghetti al pomodoro",
            ),
        )
        assertNotNull(recipe)
        assertEquals(3, recipe!!.ingredients.size)
        assertEquals("320 g Spaghetti", recipe.ingredients[0])
        assertEquals("400 g Pomodori pelati", recipe.ingredients[1])
        assertEquals(3, recipe.steps.size)
    }

    @Test
    fun zwischenueberschriftenInMehrerenAbsaetzen() {
        val recipe = VideoDescription.recipe(
            video(
                """
                👉 Zutaten:

                Für die Soße:
                500 g Hackfleisch
                1 Zwiebel

                Für die Béchamel:
                50 g Butter
                500 ml Milch

                Probiert das Rezept unbedingt aus und schreibt mir, wie es euch geschmeckt hat!
                """,
                title = "Lasagne",
            ),
        )
        assertNotNull(recipe)
        // Der erzählende Absatz danach gehört nicht mehr zu den Zutaten.
        assertEquals(
            listOf("Für die Soße:", "500 g Hackfleisch", "1 Zwiebel", "Für die Béchamel:", "50 g Butter", "500 ml Milch"),
            recipe!!.ingredients,
        )
        assertTrue(recipe.steps.isEmpty())
    }

    @Test
    fun linkZumRezept() {
        val description = """
            Das ganze Rezept zum Nachlesen: https://www.example.org/rezepte/linsensuppe?utm_source=youtube
            Mein Kochbuch: https://shop.example.org/
            ► Instagram: https://instagram.com/beispielkueche
            Töpfe: https://amzn.to/abc123
            https://bit.ly/rezept-xyz
        """.trimIndent()
        // Ohne Zählzusatz; Shop-Startseite, soziale Netzwerke und Kurzlinks mit unbekanntem Ziel zählen nicht.
        assertEquals("https://www.example.org/rezepte/linsensuppe", VideoDescription.recipeLink(description))

        // Ohne Rezept-Wort im Link zählt der Hinweis davor.
        assertEquals(
            "https://blog.example.net/2024/10/linsensuppe",
            VideoDescription.recipeLink("Zutaten und Zubereitung findet ihr hier:\nhttps://blog.example.net/2024/10/linsensuppe"),
        )

        // Startseiten, Videos und Seiten ohne Bezug zum Rezept werden nicht angeboten.
        assertNull(
            VideoDescription.recipeLink(
                "Besucht mich: https://www.example.org/\nhttps://www.youtube.com/watch?v=AbCdEfGhIjK\nÜber mich: https://www.example.org/ueber-mich",
            ),
        )
    }

    @Test
    fun ohneBildzeichen() {
        assertEquals("Ofen auf 180 °C vorheizen", VideoDescription.withoutPictographs("🔥 Ofen auf 180 °C ⭐ vorheizen ✅"))
        assertEquals("½ TL Salz", VideoDescription.withoutPictographs("½ TL Salz"))
        assertEquals("Springform ⌀ 26 cm", VideoDescription.withoutPictographs("Springform ⌀ 26 cm"))
        assertEquals("Rezept", VideoDescription.withoutPictographs("► Rezept ★ 👨‍🍳"))
    }

    @Test
    fun kapitelUndSchlagwoerterFallenWeg() {
        val lines = VideoDescription.recipeLines(
            """
            00:00 – Intro
            01:30 – Teig

            Zutaten:
            200 g Mehl
            #backen @beispielkueche
            """.trimIndent(),
        )
        assertEquals(listOf("Zutaten:", "200 g Mehl"), lines)
    }
}
