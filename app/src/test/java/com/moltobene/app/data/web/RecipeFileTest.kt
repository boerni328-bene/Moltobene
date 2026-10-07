package com.moltobene.app.data.web

import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import com.moltobene.app.data.share.IncomingRecipeFile
import com.moltobene.app.data.share.RecipeJsonLd
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files

/** Rezeptdateien übernehmen (#54): ohne Internet, mit eingebettetem Foto. Alle Beispiele sind selbst geschrieben. */
class RecipeFileTest {

    private val cacheDir: File = Files.createTempDirectory("rezeptdatei").toFile()

    /** Dateien dürfen nie ins Internet: Ein Lader, der beim Aufruf scheitert. */
    private val noInternet = object : PageLoader {
        override suspend fun loadPage(url: String): WebPage = throw AssertionError("Kein Internet für Dateien")
        override suspend fun loadImage(url: String, target: File) = throw AssertionError("Kein Internet für Dateien")
    }
    private val importer = WebImporter(noInternet, cacheDir)

    @Test
    fun eigeneRezeptdateiMitFotoUndBuch() = runBlocking {
        val photo = byteArrayOf(-1, -40, -1, -32, 1, 2, 3, 4)
        val original = Recipe(
            id = "0b0f6c0a-0000-4000-8000-000000000002",
            title = "Gemüse-Curry",
            servings = 3,
            source = RecipeSource(SourceType.BOOK, name = "Familienkochbuch", page = "12"),
            ingredients = listOf(Ingredient("400 g Blumenkohl"), Ingredient("1 Dose Kokosmilch")),
            steps = listOf("Gemüse schneiden.", "Köcheln lassen."),
            createdAt = 0,
            updatedAt = 0,
        )
        val file = RecipeJsonLd.build(original, untitled = "Ohne Titel", photoJpeg = photo) { "S. $it" }

        val result = importer.readFile(file.toByteArray())
        assertTrue(result is WebImporter.Result.Found)
        val recipe = (result as WebImporter.Result.Found).recipe
        assertEquals("Gemüse-Curry", recipe.title)
        assertEquals(3, recipe.servings)
        assertEquals("Familienkochbuch, S. 12", recipe.sourceName)
        assertNull(recipe.url)

        val saved = importer.embeddedPhoto(recipe.imageUrls.single())
        assertArrayEquals(photo, saved.readBytes())
    }

    @Test
    fun gespeicherteRezeptseite() = runBlocking {
        val html = """
            <html><head><script type="application/ld+json">
            {"@type":"Recipe","name":"Brot","recipeIngredient":["500 g Mehl"],"recipeInstructions":"Backen.",
             "image":"bilder/brot.jpg"}
            </script></head><body></body></html>
        """.trimIndent()
        val result = importer.readFile(html.toByteArray())
        val recipe = (result as WebImporter.Result.Found).recipe
        assertEquals("Brot", recipe.title)
        // Ohne zugehörige Seite gibt es keinen relativen Link auf ein Foto.
        assertTrue(recipe.imageUrls.isEmpty())
    }

    @Test
    fun dateiOhneRezept() = runBlocking {
        assertTrue(importer.readFile("""{"name":"Einkaufsliste"}""".toByteArray()) is WebImporter.Result.NoRecipe)
        assertTrue(importer.readFile(byteArrayOf(0, 1, 2, 3)) is WebImporter.Result.NoRecipe)
    }

    @Test
    fun nurEingebetteteFotos() {
        assertArrayEquals(byteArrayOf(1, 2, 3), EmbeddedImage.decode("data:image/jpeg;base64,AQID"))
        assertNull(EmbeddedImage.decode("data:image/svg+xml;base64,AQID"))
        assertNull(EmbeddedImage.decode("data:text/html;base64,AQID"))
        assertNull(EmbeddedImage.decode("https://example.org/foto.jpg"))
        assertNull(EmbeddedImage.decode("data:image/png;base64,"))
    }

    @Test(expected = IncomingRecipeFile.TooLargeException::class)
    fun zuGrosseDatei() {
        IncomingRecipeFile.readAtMost(ByteArrayInputStream(ByteArray(2_000)), max = 1_000)
    }

    /** Gespeicherte Seite, deren Rezeptdaten nur Titel, Portionen und Zeiten enthalten: Der Text wird eingeordnet. */
    @Test
    fun seiteMitRezeptdatenOhneZutaten() = runBlocking {
        val html = """
            <html lang="en"><head><title>Braised Beans | Chef Example</title>
              <script type="application/ld+json">{"@type":"Recipe","name":"Braised Beans","recipeYield":"6","totalTime":"4 hrs"}</script>
            </head><body><main>
              <h2>Ingredients</h2><article><ul><li>500 g white beans</li></ul></article>
              <h2>Directions</h2><ol><li>Simmer the beans.</li></ol>
            </main></body></html>
        """.trimIndent()
        val result = importer.readFile(html.toByteArray())
        assertTrue(result is WebImporter.Result.TextOnly)
        result as WebImporter.Result.TextOnly
        assertEquals("Braised Beans", result.title)
        assertEquals(6, result.details?.servings)
        assertEquals(240, result.details?.totalMinutes)
        assertTrue(result.text.contains("Simmer the beans."))
    }
}
