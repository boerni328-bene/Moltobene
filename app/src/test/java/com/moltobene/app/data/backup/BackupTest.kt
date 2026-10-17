package com.moltobene.app.data.backup

import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import com.moltobene.app.data.Tag
import com.moltobene.app.data.TimelinePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val photoId = "11111111-2222-3333-4444-555555555555"
    private val pageId = "66666666-7777-8888-9999-000000000000"

    private val recipe = Recipe(
        id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
        title = "Apfelkuchen",
        language = "de",
        servings = 12,
        servingsUnit = "Stück",
        source = RecipeSource(SourceType.BOOK, name = "Omas Kochbuch", page = "42"),
        videoUrl = "https://www.youtube.com/watch?v=AbCdEfGhIjK",
        notes = "Mit Sahne servieren.",
        favorite = true,
        ingredients = listOf(Ingredient("Für den Teig", isHeading = true), Ingredient("200 g Mehl", quantity = 200.0, unit = "g")),
        steps = listOf("Ofen vorheizen.", "Backen."),
        tags = listOf(Tag("Kuchen", predefinedKey = "cake"), Tag("sonntags")),
        photoIds = listOf(photoId),
        pageIds = listOf(pageId),
        createdAt = 1_000,
        updatedAt = 2_000,
    )

    @Test
    fun sicherungUebersteht_HinUndRueckweg() {
        val full = temp.newFile("full.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val thumb = temp.newFile("thumb.jpg").apply { writeBytes(byteArrayOf(4, 5)) }
        val page = temp.newFile("page.jpg").apply { writeBytes(byteArrayOf(6, 7)) }
        val pageThumb = temp.newFile("page_thumb.jpg").apply { writeBytes(byteArrayOf(8)) }
        val photos = mapOf(photoId to (full to thumb), pageId to (page to pageThumb))
        val bytes = ByteArrayOutputStream().also { out ->
            BackupWriter.write(out, BackupManifest(appVersion = "0.7.0", recipeCount = 1), listOf(recipe.toBackup()), photos)
        }.toByteArray()

        val dir = temp.newFolder("restore")
        val result = BackupReader().read(ByteArrayInputStream(bytes), dir) as BackupReader.Result.Ok

        // Foto des Gerichts und Originalseite bleiben getrennt erhalten.
        assertEquals(listOf(recipe), result.recipes.map { it.toRecipe() })
        assertEquals(0, result.skippedRecipes)
        assertTrue(File(dir, "$photoId.jpg").readBytes().contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(File(dir, "${photoId}_thumb.jpg").readBytes().contentEquals(byteArrayOf(4, 5)))
        assertTrue(File(dir, "$pageId.jpg").readBytes().contentEquals(byteArrayOf(6, 7)))
    }

    @Test
    fun ungueltigeKennungenVonOriginalseitenWerdenUebergangen() {
        val bytes = zip(
            BackupFormat.MANIFEST_ENTRY to BackupFormat.json.encodeToString(BackupManifest.serializer(), BackupManifest()).toByteArray(),
            BackupFormat.RECIPES_ENTRY to """
                {"recipes":[{"id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","title":"Gut","pages":["$pageId","../boese"]}]}
            """.trimIndent().toByteArray(),
        )
        val result = BackupReader().read(ByteArrayInputStream(bytes), temp.newFolder("seiten")) as BackupReader.Result.Ok
        assertEquals(listOf(pageId), result.recipes.single().toRecipe().pageIds)
    }

    @Test
    fun zeitplanUebersteht_HinUndRueckwegUndAlteSicherungenHabenKeinen() {
        val teig = recipe.copy(
            timeline = listOf(
                TimelinePhase("Biga reifen lassen", 24 * 60, 72 * 60, PhasePlace.FRIDGE),
                TimelinePhase("Backen", 8, 15, PhasePlace.OVEN),
            ),
        )
        assertEquals(teig, teig.toBackup().toRecipe())
        // Die Beispiel-Sicherung stammt aus der Zeit vor dem Zeitplan.
        assertTrue(recipe.toBackup().timeline.isEmpty())
        val unmoeglich = teig.toBackup().copy(timeline = listOf(BackupPhase("Kaputt", minMinutes = -5, maxMinutes = 999_999_999, place = "x")))
        val phase = unmoeglich.toRecipe().timeline.single()
        assertEquals(0, phase.minMinutes)
        assertEquals(TimelinePhase.MAX_MINUTES, phase.maxMinutes)
        assertEquals(PhasePlace.NONE, phase.place)
    }

    @Test
    fun beispielSicherungFormat1BleibtLesbar() {
        val bytes = zip(
            BackupFormat.MANIFEST_ENTRY to resource("backup/v1/manifest.json"),
            BackupFormat.RECIPES_ENTRY to resource("backup/v1/recipes.json"),
        )
        val result = BackupReader().read(ByteArrayInputStream(bytes), temp.newFolder("v1")) as BackupReader.Result.Ok

        assertEquals(1, result.manifest.formatVersion)
        assertEquals(2, result.recipes.size)
        val kuchen = result.recipes.first { it.title == "Apfelkuchen" }.toRecipe()
        assertEquals(listOf("Für den Teig", "200 g Mehl"), kuchen.ingredients.map { it.text })
        assertTrue(kuchen.ingredients.first().isHeading)
        assertEquals("cake", kuchen.tags.first().predefinedKey)
        assertEquals(SourceType.BOOK, kuchen.source?.type)
        // Format 1 kennt noch keine Originalseiten und keine Videos.
        assertTrue(kuchen.pageIds.isEmpty())
        assertEquals(null, kuchen.videoUrl)
        // Unbekannte Felder aus neueren Versionen werden ignoriert, fehlende bekommen Standardwerte.
        val suppe = result.recipes.first { it.title == "Suppe" }.toRecipe()
        assertEquals("", suppe.notes)
        assertTrue(suppe.isDraft)
    }

    @Test
    fun gefaehrlicheDateinamenWerdenNieGeschrieben() {
        val bytes = zip(
            BackupFormat.MANIFEST_ENTRY to BackupFormat.json.encodeToString(BackupManifest.serializer(), BackupManifest()).toByteArray(),
            BackupFormat.RECIPES_ENTRY to """{"recipes":[]}""".toByteArray(),
            "../boese.jpg" to byteArrayOf(9),
            "photos/../../boese.jpg" to byteArrayOf(9),
            "/absolut.jpg" to byteArrayOf(9),
        )
        val root = temp.newFolder("root")
        val dir = File(root, "restore")
        val result = BackupReader().read(ByteArrayInputStream(bytes), dir)

        assertTrue(result is BackupReader.Result.Ok)
        assertFalse(File(root, "boese.jpg").exists())
        assertFalse(File(temp.root, "boese.jpg").exists())
        assertEquals(0, dir.listFiles()?.size ?: 0)
    }

    @Test
    fun neuereFormatversionWirdErkannt() {
        val manifest = BackupManifest(formatVersion = BackupFormat.CURRENT_FORMAT_VERSION + 1)
        val bytes = zip(
            BackupFormat.MANIFEST_ENTRY to BackupFormat.json.encodeToString(BackupManifest.serializer(), manifest).toByteArray(),
            BackupFormat.RECIPES_ENTRY to """{"recipes":[]}""".toByteArray(),
        )
        val result = BackupReader().read(ByteArrayInputStream(bytes), temp.newFolder("neu"))
        assertEquals(BackupReader.Result.Failed(BackupReader.Problem.NEWER_VERSION), result)
    }

    @Test
    fun fremdeDateiIstKeineSicherung() {
        val result = BackupReader().read(ByteArrayInputStream("kein zip".toByteArray()), temp.newFolder("fremd"))
        assertEquals(BackupReader.Result.Failed(BackupReader.Problem.NOT_A_BACKUP), result)
    }

    @Test
    fun beschaedigtesRezeptWirdUebersprungen() {
        val bytes = zip(
            BackupFormat.MANIFEST_ENTRY to BackupFormat.json.encodeToString(BackupManifest.serializer(), BackupManifest()).toByteArray(),
            BackupFormat.RECIPES_ENTRY to """
                {"recipes":[
                  {"id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","title":"Gut"},
                  {"id":"keine-uuid","title":"Falsche Kennung"},
                  {"title":"Ohne Kennung"}
                ]}
            """.trimIndent().toByteArray(),
        )
        val result = BackupReader().read(ByteArrayInputStream(bytes), temp.newFolder("kaputt")) as BackupReader.Result.Ok
        assertEquals(listOf("Gut"), result.recipes.map { it.title })
        assertEquals(2, result.skippedRecipes)
    }

    @Test
    fun zuGrosseSicherungWirdAbgelehnt() {
        val bytes = zip(
            BackupFormat.MANIFEST_ENTRY to BackupFormat.json.encodeToString(BackupManifest.serializer(), BackupManifest()).toByteArray(),
            "photos/$photoId.jpg" to ByteArray(2_000),
        )
        val reader = BackupReader(BackupReader.Limits(maxPhotoBytes = 1_000))
        val result = reader.read(ByteArrayInputStream(bytes), temp.newFolder("gross"))
        assertEquals(BackupReader.Result.Failed(BackupReader.Problem.TOO_LARGE), result)
    }


    @Test
    fun voreinstellungenWerdenGesichertUndBereinigtGelesen() {
        val own = com.moltobene.app.data.dough.DoughPresets.Own("Meine Pizza", "x")
        val out = ByteArrayOutputStream()
        BackupWriter.write(
            out, BackupManifest(appVersion = "0.52.0"), emptyList(), emptyMap(),
            BackupPresets(mapOf("pizza" to listOf(own), "unbekannt" to listOf(own))),
        )
        val result = BackupReader().read(ByteArrayInputStream(out.toByteArray()), temp.newFolder("vor")) as BackupReader.Result.Ok
        assertEquals(listOf(own), result.presets["pizza"])
        assertFalse(result.presets.containsKey("unbekannt"))
    }

    @Test
    fun sicherungOhneVoreinstellungenBleibtLesbar() {
        val bytes = zip(
            BackupFormat.MANIFEST_ENTRY to resource("backup/v1/manifest.json"),
            BackupFormat.RECIPES_ENTRY to resource("backup/v1/recipes.json"),
        )
        val result = BackupReader().read(ByteArrayInputStream(bytes), temp.newFolder("ohne")) as BackupReader.Result.Ok
        assertTrue(result.presets.isEmpty())
    }
    private fun resource(path: String): ByteArray =
        requireNotNull(javaClass.classLoader?.getResourceAsStream(path)) { "Testdatei fehlt: $path" }.use { it.readBytes() }

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                entries.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content)
                    zip.closeEntry()
                }
            }
        }.toByteArray()
}
