package com.moltobene.app.perf

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.moltobene.app.MoltobeneApplication
import com.moltobene.app.data.Ingredient
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeIds
import com.moltobene.app.data.RecipeSource
import com.moltobene.app.data.SourceType
import com.moltobene.app.tour.SampleRecipes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Prüflauf (#52): legt eine Testsammlung mit 1.000 Rezepten und Fotos an und misst Laden und Suchen. Die Werte stehen
 * im Protokoll des Telefons unter dem Stichwort „PRUEF“. Die Sammlung bleibt danach auf dem Handy, damit das
 * Prüf-Skript Kaltstart und Blättern an ihr misst (.github/workflows/pruefflauf.yml). Gezeichnete Fotos, selbst
 * geschriebene Texte, nichts Fremdes.
 */
class PerformanceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val container get() = (context.applicationContext as MoltobeneApplication).container

    @Test
    fun sammlungMitTausendRezepten() = runBlocking {
        val repository = container.repository
        repository.getAll().forEach { repository.delete(it.id) }
        // Neu in Version gilt als gesehen, damit es beim Messen nicht über der Sammlung liegt.
        container.preferences.setLastSeenVersionCode(com.moltobene.app.BuildConfig.VERSION_CODE)

        // Ein Foto wird verarbeitet und dann für jedes Rezept kopiert: So ist jedes Rezept mit eigenem Foto und
        // Vorschaubild ausgestattet, ohne dass das Anlegen Minuten dauert.
        val base = File(context.cacheDir, "pruef-foto.jpg")
        SampleRecipes.drawPagePhoto(context, base)
        val baseId = container.photoStore.importFromUri(Uri.fromFile(base))
        base.delete()
        val full = container.photoStore.photoFile(baseId)
        val thumb = container.photoStore.thumbFile(baseId)

        val started = SystemClock.elapsedRealtime()
        val now = System.currentTimeMillis()
        val recipes = (1..COUNT).map { index ->
            val photoId = RecipeIds.newId()
            full.copyTo(container.photoStore.photoFile(photoId), overwrite = true)
            if (thumb.isFile) thumb.copyTo(container.photoStore.thumbFile(photoId), overwrite = true)
            recipe(index, photoId, now - index)
        }
        recipes.chunked(100).forEach { repository.saveAll(it) }
        report("anlegen_ms", SystemClock.elapsedRealtime() - started)
        assertEquals(COUNT, repository.getAll().size)

        // Liste laden, wie die Sammlung beim Start.
        val list = measure { repository.observeSummaries("").first() }
        report("liste_laden_ms", list.first)
        assertEquals(COUNT, list.second.size)

        // Suche: jeder Buchstabe eines Wortes wie beim Tippen.
        val typed = measure { "Linsensuppe".indices.map { repository.observeSummaries("Linsensuppe".take(it + 1)).first().size } }
        report("suche_tippen_ms_gesamt", typed.first)
        report("suche_treffer", typed.second.last().toLong())
        val notFound = measure { repository.observeSummaries("Zzyzx").first() }
        report("suche_ohne_treffer_ms", notFound.first)

        // Ein Rezept mit allem Drum und Dran öffnen.
        val detail = measure { repository.getRecipe(recipes[COUNT / 2].id) }
        report("rezept_oeffnen_ms", detail.first)
    }

    private inline fun <T> measure(block: () -> T): Pair<Long, T> {
        val start = SystemClock.elapsedRealtime()
        val result = block()
        return SystemClock.elapsedRealtime() - start to result
    }

    private fun report(name: String, value: Long) {
        Log.i(TAG, "$name=$value")
    }

    private fun recipe(index: Int, photoId: String, time: Long): Recipe {
        val dish = DISHES[index % DISHES.size]
        return Recipe(
            id = RecipeIds.newId(),
            title = "$dish $index",
            language = "de",
            servings = 2 + index % 6,
            source = RecipeSource(SourceType.WEB, url = "https://example.org/rezepte/$index"),
            ingredients = listOf("200 g Mehl", "3 Eier", "1 Zwiebel", "2 EL Olivenöl", "500 ml Milch", "1 Prise Salz", "Pfeffer")
                .map { Ingredient(it) },
            steps = listOf(
                "Alles vorbereiten und die Zutaten abwiegen.",
                "Die Zwiebel fein würfeln und im Öl glasig dünsten.",
                "Mehl, Eier und Milch glatt rühren und dazugeben.",
                "20 Minuten köcheln lassen und abschmecken.",
            ),
            photoIds = listOf(photoId),
            createdAt = time,
            updatedAt = time,
        )
    }

    private companion object {
        const val TAG = "PRUEF"
        const val COUNT = 1000
        val DISHES = listOf(
            "Linsensuppe", "Tomatensoße", "Pfannkuchen", "Kartoffelsalat", "Risotto", "Gulasch", "Apfelkuchen",
            "Gemüsecurry", "Hefezopf", "Spaghetti", "Ofengemüse", "Kürbissuppe",
        )
    }
}
