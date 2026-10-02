package com.moltobene.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüftexte unter test/resources/ocr/ sind echte Ergebnisse von Tesseract (tessdata_fast) für
 * nachgestellte Handyfotos von Kochbuchseiten (schief, ungleich hell, 1600 px).
 */
class RecipeTextParserTest {

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("ocr/$name")) { "Prüftext $name fehlt" }.readText()

    @Test
    fun deutschesRezeptMitUeberschriften() {
        val recipe = RecipeTextParser.parse(resource("de.txt"))
        assertEquals("Apfelkuchen vom Blech", recipe.title)
        assertEquals(12, recipe.servings)
        assertEquals("Stücke", recipe.servingsUnit)
        assertEquals(
            listOf(
                "250 g weiche Butter", "200 g Zucker", "1 Päckchen Vanillezucker", "4 Eier", "400 g Mehl",
                "1 Päckchen Backpulver", "125 ml Milch", "1,5 kg säuerliche Äpfel", "Für die Streusel:",
                "150 g Mehl", "100 g Zucker", "100 g kalte Butter",
            ),
            recipe.ingredients,
        )
        assertEquals(4, recipe.steps.size)
        assertEquals(
            "Den Backofen auf 180 °C vorheizen. Butter, Zucker und Vanillezucker schaumig rühren, " +
                "dann die Eier nacheinander unterrühren.",
            recipe.steps[0],
        )
        assertEquals("Etwa 40 Minuten backen, bis die Streusel goldbraun sind.", recipe.steps[3])
    }

    @Test
    fun englischesRezeptMitNummeriertenSchritten() {
        val recipe = RecipeTextParser.parse(resource("en.txt"))
        assertEquals("Classic Pancakes", recipe.title)
        assertEquals(4, recipe.servings)
        assertNull(recipe.servingsUnit)
        assertEquals(7, recipe.ingredients.size)
        assertEquals("1 pinch of salt", recipe.ingredients[3])
        assertEquals(
            listOf(
                "Mix the flour, baking powder, sugar and salt in a large bowl.",
                "Whisk the eggs with the milk and butter, then pour into the dry ingredients and stir until smooth.",
                "Cook ladlefuls of batter in a hot pan for about two minutes on each side.",
            ),
            recipe.steps,
        )
    }

    @Test
    fun italienischesRezeptMitZweiSpaltenZutaten() {
        val recipe = RecipeTextParser.parse(resource("it.txt"))
        assertEquals("Risotto ai funghi", recipe.title)
        assertEquals(4, recipe.servings)
        assertTrue(recipe.ingredients.contains("320 g di riso Carnaroli"))
        assertTrue(recipe.ingredients.contains("1/2 bicchiere di vino bianco"))
        assertTrue(recipe.ingredients.contains("1 cipolla"))
        assertTrue(recipe.ingredients.contains("60 g di parmigiano grattugiato"))
        assertEquals(3, recipe.steps.size)
    }

    @Test
    fun franzoesischesUndSpanischesRezept() {
        val fr = RecipeTextParser.parse(resource("fr.txt"))
        assertEquals("Crème brûlée à la vanille", fr.title)
        assertEquals(6, fr.servings)
        assertEquals(6, fr.ingredients.size)
        assertEquals("cassonade pour caraméliser", fr.ingredients.last())
        assertEquals(3, fr.steps.size)

        val es = RecipeTextParser.parse(resource("es.txt"))
        assertEquals("Tortilla de patatas", es.title)
        assertEquals(4, es.servings)
        assertEquals(listOf("6 huevos", "750 g de patatas", "1 cebolla grande", "200 ml de aceite de oliva", "sal"), es.ingredients)
        assertEquals(3, es.steps.size)
    }

    @Test
    fun ohneUeberschriftenNachAussehenDerZeilen() {
        val text = """
            Linsensuppe

            250 g Linsen
            1 Zwiebel
            2 Karotten
            1 l Gemüsebrühe

            Die Zwiebel und die Karotten klein schneiden und in etwas Öl andünsten.
            Linsen und Brühe dazugeben und 30 Minuten köcheln lassen.
        """.trimIndent()
        val recipe = RecipeTextParser.parse(text)
        assertEquals("Linsensuppe", recipe.title)
        assertEquals(listOf("250 g Linsen", "1 Zwiebel", "2 Karotten", "1 l Gemüsebrühe"), recipe.ingredients)
        assertEquals(1, recipe.steps.size)
    }

    @Test
    fun trennstricheRauschenUndSeitenzahlen() {
        val text = """
            ~~~ |||
            Zutaten
            1 Päckchen Vanil-
            lezucker
            Salz- und Pfeffer
            Zubereitung
            Alles gut ver-
            rühren.
            12
        """.trimIndent()
        val recipe = RecipeTextParser.parse(text)
        assertNull(recipe.title)
        assertEquals(listOf("1 Päckchen Vanillezucker", "Salz- und Pfeffer"), recipe.ingredients)
        assertEquals(listOf("Alles gut verrühren."), recipe.steps)
        assertEquals(listOf("Salz- und Pfeffer"), RecipeTextParser.cleanLines("Salz-\nund Pfeffer").filter { it.isNotEmpty() })
    }

    @Test
    fun zubereitungszeitIstKeineUeberschrift() {
        val recipe = RecipeTextParser.parse("Nudelsalat\nZubereitungszeit: 20 Minuten\nPreparation time 20 min\nZutaten\n500 g Nudeln")
        assertEquals("Nudelsalat", recipe.title)
        assertEquals(listOf("500 g Nudeln"), recipe.ingredients)
        assertTrue(recipe.steps.isEmpty())
    }

    @Test
    fun zutatenTabelleAusRezeptApp() {
        // So setzt die Texterkennung eine Tabelle mit Trennlinien zusammen (Zeilen einzeln gelesen),
        // dazu Werbung und Lesefehler, wie sie auf Bildschirmfotos vorkommen.
        val text = """
            Canneloni

            Für die Fülle:
            Öl 4EL
            1 Stk. Zwiebel
            2 Stk. Knoblauchzehen
            500 g Hackfleisch gemischt
            2 Do. Tomaten geschält, klein
            etwas Salz, Pfeffer
            1 Pr Oregano getrocknet
            1 Pr Rosmarin getrocknet
            3 EL Rotwein
            x Anzeige —_
            H=ROoh: mie
            Zubereitung
            Die Zwiebel fein hacken und in Öl andünsten.
        """.trimIndent()
        val recipe = RecipeTextParser.parse(text, "de")
        assertEquals("Canneloni", recipe.title)
        assertEquals(
            listOf(
                "Für die Fülle:", "4 EL Öl", "1 Stk. Zwiebel", "2 Stk. Knoblauchzehen", "500 g Hackfleisch gemischt",
                "2 Do. Tomaten geschält, klein", "etwas Salz, Pfeffer", "1 Pr Oregano getrocknet",
                "1 Pr Rosmarin getrocknet", "3 EL Rotwein",
            ),
            recipe.ingredients,
        )
        assertEquals(listOf("Die Zwiebel fein hacken und in Öl andünsten."), recipe.steps)
    }

    @Test
    fun deutscheZutatUeberZweiZeilen() {
        val text = "Zutaten\n500 g Hackfleisch\ngemischt\n1 Bund Petersilie\nfrische Kräuter\n1 Dose Tomaten,\nin Stücken"
        assertEquals(
            listOf("500 g Hackfleisch gemischt", "1 Bund Petersilie", "frische Kräuter", "1 Dose Tomaten, in Stücken"),
            RecipeTextParser.parse(text, "de").ingredients,
        )
        // Im Italienischen beginnen auch eigene Zutaten klein („sale e pepe“).
        assertEquals(
            listOf("50 g di burro", "sale e pepe"),
            RecipeTextParser.parse("Ingredienti\n50 g di burro\nsale e pepe", "it").ingredients,
        )
    }

    @Test
    fun leererText() {
        val recipe = RecipeTextParser.parse("  \n\n ")
        assertNull(recipe.title)
        assertTrue(recipe.ingredients.isEmpty() && recipe.steps.isEmpty())
    }

    @Test
    fun ueberschriftenOhneRuecksichtAufAkzente() {
        // Mit dem deutschen Sprachpaket gelesen: „Elaboración“ wurde zu „Elaboraciön“.
        val recipe = RecipeTextParser.parse(resource("es_mit_deu_gelesen.txt"), "es")
        assertEquals(listOf("6 huevos", "750 g de patatas", "1 cebolla grande", "200 ml de aceite de oliva", "sal"), recipe.ingredients)
        assertEquals(3, recipe.steps.size)
        assertTrue(recipe.steps.first().startsWith("Pela las patatas"))
        assertEquals(listOf("2 œufs"), RecipeTextParser.parse("Ingrédients\n2 œufs\nPréparation\nBattre les œufs.", "fr").ingredients)
    }

    @Test
    fun seitenzahlenJeSeiteUndZahlAlleinIstKeineZutat() {
        val recipe = RecipeTextParser.parseParts(
            listOf(
                AreaKind.ALL to "Linsensuppe\nZutaten\n250 g Linsen\n1 Zwiebel\n46",
                AreaKind.ALL to "47\n2 Karotten\nZubereitung\nAlles weich kochen.\n48",
            ),
            language = "de",
        )
        assertEquals(listOf("250 g Linsen", "1 Zwiebel", "2 Karotten"), recipe.ingredients)
        assertEquals(listOf("Alles weich kochen."), recipe.steps)
        // Die Seitenzahl der ersten Seite wird Vorschlag für die Quelle (#40).
        assertEquals("46", recipe.pageNumber)
        assertNull(RecipeTextParser.pageNumberOf("Linsensuppe\n250 g Linsen"))
        // Eine einzelne Zahl mitten im Text gilt nicht als Zutat (früher: „4“ als Menge, „7“ als Zutat).
        val withNumber = RecipeTextParser.parse(
            "Linsensuppe\nAlles in einen Topf geben und weich kochen.\n\n47\n\nDann mit Salz abschmecken und servieren.",
            "de",
        )
        assertTrue(withNumber.ingredients.isEmpty())
        assertEquals("Zeile", RecipeTextParser.withoutPageNumbers("12\nZeile\n13"))
    }

    @Test
    fun spracheWirdErkannt() {
        assertEquals("de", TextLanguage.detect(resource("de.txt")))
        assertEquals("en", TextLanguage.detect(resource("en.txt")))
        assertEquals("it", TextLanguage.detect(resource("it.txt")))
        assertEquals("fr", TextLanguage.detect(resource("fr.txt")))
        assertEquals("es", TextLanguage.detect(resource("es.txt")))
        // Auch wenn zuerst mit dem deutschen Sprachpaket gelesen wurde
        assertEquals("fr", TextLanguage.detect(resource("fr_mit_deu_gelesen.txt")))
        assertEquals("es", TextLanguage.detect(resource("es_mit_deu_gelesen.txt")))
        assertEquals("it", TextLanguage.detect(resource("it_mit_deu_gelesen.txt")))
        assertEquals("en", TextLanguage.detect(resource("en_mit_deu_gelesen.txt")))
        assertNull(TextLanguage.detect("Pfannkuchen"))
        assertEquals("spa", TextLanguage.tesseractCode("es"))
        assertEquals("en", TextLanguage.supportedOrDefault("nl"))
    }

    /** „Aus Text übernehmen“: Text aus einer Nachricht, mit Leerzeilen und Satzzeichen wie getippt. */
    @Test
    fun geteilterTextAusEinerNachricht() {
        val text = """
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
        val recipe = RecipeTextParser.parse(text, TextLanguage.detect(text))
        assertEquals("Spaghetti aglio e olio", recipe.title)
        assertEquals(2, recipe.servings)
        assertEquals(listOf("200 g Spaghetti", "3 Knoblauchzehen", "4 EL Olivenöl", "1 Peperoncino"), recipe.ingredients)
        assertEquals(3, recipe.steps.size)
    }
}
