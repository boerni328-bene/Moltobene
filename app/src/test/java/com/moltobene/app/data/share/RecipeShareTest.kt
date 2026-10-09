package com.moltobene.app.data.share

import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import com.moltobene.app.data.Tag
import com.moltobene.app.data.web.RecipeJsonLdReader
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class RecipeShareTest {

    private val labels = ShareLabels(
        untitled = "Ohne Titel",
        ingredients = "Zutaten",
        instructions = "Zubereitung",
        servings = { count, unit -> if (unit == null) "$count Portionen" else "$count $unit" },
        source = { "Quelle: $it" },
        video = { "Video: $it" },
    )

    private val recipe = Recipe(
        id = "0b8f4e0e-6a1d-4f3c-9a51-2f7d1c3b9e10",
        title = "Pfannkuchen",
        servings = 4,
        source = RecipeSource(type = SourceType.OTHER, name = "Omas Kochbuch", page = "S. 42"),
        notes = "Geheimer Tipp: nur für mich",
        ingredients = listOf(
            Ingredient("Für den Teig", isHeading = true),
            Ingredient("200 g Mehl"),
            Ingredient("2 Eier"),
            Ingredient("Zum Servieren", isHeading = true),
            Ingredient("Zucker"),
        ),
        steps = listOf("Alles verrühren.", "In der Pfanne ausbacken."),
        tags = listOf(Tag("Süß")),
        createdAt = 1,
        updatedAt = 2,
    )

    @Test
    fun textEnthaeltAllesAusserNotizen() {
        val expected = """
            Pfannkuchen
            4 Portionen

            Zutaten
            Für den Teig:
            • 200 g Mehl
            • 2 Eier

            Zum Servieren:
            • Zucker

            Zubereitung
            1. Alles verrühren.
            2. In der Pfanne ausbacken.

            Quelle: Omas Kochbuch, S. 42
        """.trimIndent()
        assertEquals(expected, RecipeShareText.format(recipe, labels))
        assertFalse(RecipeShareText.format(recipe, labels).contains("Geheimer Tipp"))
    }

    @Test
    fun videoWirdMitGeteilt() {
        val video = "https://www.youtube.com/watch?v=AbCdEfGhIjK"
        val withVideo = recipe.copy(source = RecipeSource(type = SourceType.WEB, url = "https://example.org/rezept"), videoUrl = video)
        assertTrue(RecipeShareText.format(withVideo, labels).endsWith("\n\nQuelle: https://example.org/rezept\nVideo: $video"))
        // Ist das Video selbst die Quelle, steht es im Text nur einmal da.
        val onlyVideo = withVideo.copy(source = RecipeSource(type = SourceType.WEB, url = video))
        assertFalse(RecipeShareText.format(onlyVideo, labels).contains("Video:"))

        // Rezeptdatei: schema.org video – auch wenn es die Quelle ist, damit das Feld beim Übernehmen wieder ankommt.
        val file = Json.parseToJsonElement(RecipeJsonLd.build(withVideo, "Ohne Titel", null)).jsonObject
        assertEquals("VideoObject", file.getValue("video").jsonObject.getValue("@type").jsonPrimitive.content)
        assertEquals(video, file.getValue("video").jsonObject.getValue("url").jsonPrimitive.content)
        assertEquals(video, RecipeJsonLdReader.read(RecipeJsonLd.build(onlyVideo, "Ohne Titel", null))?.videoUrl)
        assertFalse(Json.parseToJsonElement(RecipeJsonLd.build(recipe, "Ohne Titel", null)).jsonObject.containsKey("video"))
    }

    /** Eine Backform ohne Anzahl (#63) wird mitgeteilt und kommt beim Übernehmen wieder an. */
    @Test
    fun backformOhneAnzahlWirdGeteilt() {
        val cake = recipe.copy(servings = null, servingsUnit = "Springform Ø 26 cm")
        assertTrue(RecipeShareText.format(cake, labels).startsWith("Pfannkuchen\nSpringform Ø 26 cm\n\n"))
        val file = RecipeJsonLd.build(cake, "Ohne Titel", null)
        assertEquals("Springform Ø 26 cm", Json.parseToJsonElement(file).jsonObject.getValue("recipeYield").jsonPrimitive.content)
        val back = RecipeJsonLdReader.read(file)
        assertNull(back?.servings)
        assertEquals("Springform Ø 26 cm", back?.servingsUnit)
    }

    @Test
    fun textOhneTitelUndMitPortionsangabe() {
        val text = RecipeShareText.format(
            recipe.copy(title = "  ", servings = 12, servingsUnit = "Stück", ingredients = emptyList(), steps = emptyList(), source = null),
            labels,
        )
        assertEquals("Ohne Titel\n12 Stück", text)
    }

    @Test
    fun linkGehtVorNameBeiDerQuelle() {
        val source = RecipeSource(type = SourceType.WEB, name = "Blog", url = "https://example.org/rezept")
        assertEquals("https://example.org/rezept", RecipeShareText.sourceText(source))
        assertNull(RecipeShareText.sourceText(RecipeSource(type = SourceType.OTHER, name = " ")))
    }

    @Test
    fun seiteDerQuelleMitBezeichnung() {
        val book = RecipeSource(type = SourceType.BOOK, name = "Omas Kochbuch", page = "47")
        assertEquals("Omas Kochbuch, S. 47", RecipeShareText.sourceText(book) { "S. $it" })
        assertTrue(RecipeJsonLd.build(Recipe(id = "x", title = "Kuchen", source = book, createdAt = 0, updatedAt = 0), "Ohne Titel", null) { "p. $it" }.contains("Omas Kochbuch, p. 47"))
    }

    @Test
    fun rezeptdateiFolgtSchemaOrg() {
        val photo = byteArrayOf(1, 2, 3)
        val json = Json.parseToJsonElement(RecipeJsonLd.build(recipe.copy(prepMinutes = 20, totalMinutes = 90), "Ohne Titel", photo)).jsonObject

        assertEquals("https://schema.org", json.getValue("@context").jsonPrimitive.content)
        assertEquals("Recipe", json.getValue("@type").jsonPrimitive.content)
        assertEquals("Pfannkuchen", json.getValue("name").jsonPrimitive.content)
        assertEquals("4", json.getValue("recipeYield").jsonPrimitive.content)
        assertEquals("PT20M", json.getValue("prepTime").jsonPrimitive.content)
        assertEquals("PT1H30M", json.getValue("totalTime").jsonPrimitive.content)
        assertEquals(
            listOf("Für den Teig:", "200 g Mehl", "2 Eier", "Zum Servieren:", "Zucker"),
            json.getValue("recipeIngredient").jsonArray.map { it.jsonPrimitive.content },
        )
        val steps = json.getValue("recipeInstructions").jsonArray.map { it.jsonObject }
        assertEquals("HowToStep", steps[0].getValue("@type").jsonPrimitive.content)
        assertEquals("In der Pfanne ausbacken.", steps[1].getValue("text").jsonPrimitive.content)
        assertEquals("Süß", json.getValue("keywords").jsonPrimitive.content)
        assertEquals("Omas Kochbuch, S. 42", json.getValue("isBasedOn").jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals(
            "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(photo),
            json.getValue("image").jsonPrimitive.content,
        )
        assertFalse("Notizen dürfen nicht geteilt werden", json.toString().contains("Geheimer Tipp"))
        assertFalse(json.containsKey("url"))
    }

    @Test
    fun rezeptdateiMitLinkAlsQuelleUndOhneFoto() {
        val json = Json.parseToJsonElement(
            RecipeJsonLd.build(recipe.copy(source = RecipeSource(type = SourceType.WEB, url = "https://example.org/r")), "Ohne Titel", null),
        ).jsonObject
        assertEquals("https://example.org/r", json.getValue("url").jsonPrimitive.content)
        assertFalse(json.containsKey("isBasedOn"))
        assertFalse(json.containsKey("image"))
    }

    @Test
    fun dauerImFormatIso8601() {
        assertEquals("PT45M", RecipeJsonLd.isoDuration(45))
        assertEquals("PT2H", RecipeJsonLd.isoDuration(120))
        assertEquals("PT1H5M", RecipeJsonLd.isoDuration(65))
    }

    @Test
    fun dateinameOhneVerboteneZeichen() {
        assertEquals("Pasta al forno 1 2", ShareFiles.baseName("Pasta: al forno 1/2", "Ohne Titel"))
        assertEquals("Ohne Titel", ShareFiles.baseName(" ?* ", "Ohne Titel"))
        assertTrue(ShareFiles.baseName("x".repeat(200), "Ohne Titel").length <= 60)
    }
}
