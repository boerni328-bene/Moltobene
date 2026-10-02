package com.moltobene.app.data

import android.content.Context
import com.moltobene.app.data.db.MoltobeneDatabase
import com.moltobene.app.data.photos.PhotoStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Abfragen der Sammlung mit einer echten Datenbank. */
@RunWith(RobolectricTestRunner::class)
class RecipeRepositoryTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val database = MoltobeneDatabase.create(context)
    private val repository = RecipeRepository(database.recipeDao(), PhotoStore(context))

    @After
    fun close() = database.close()

    private fun recipe(id: Int, source: RecipeSource?, updatedAt: Long) =
        Recipe(id = "aaaaaaaa-0000-0000-0000-00000000000$id", title = "Rezept $id", source = source, createdAt = 0, updatedAt = updatedAt)

    @Test
    fun zuletztGenutzteBuecherAlsVorschlag() = runBlocking {
        repository.save(recipe(1, RecipeSource(SourceType.BOOK, name = "Omas Kochbuch", page = "12"), updatedAt = 100))
        repository.save(recipe(2, RecipeSource(SourceType.OTHER, name = "Backen mit Lea"), updatedAt = 300))
        repository.save(recipe(3, RecipeSource(SourceType.BOOK, name = "Omas Kochbuch", page = "47"), updatedAt = 400))
        // Links und Rezepte ohne Quelle sind keine Vorschläge.
        repository.save(recipe(4, RecipeSource(SourceType.WEB, url = "https://example.org/rezept"), updatedAt = 500))
        repository.save(recipe(5, null, updatedAt = 600))
        repository.save(recipe(6, RecipeSource(SourceType.OTHER, name = "Zettel von Tante Ute"), updatedAt = 200))

        // Jedes Buch nur einmal, das zuletzt genutzte zuerst, höchstens drei.
        assertEquals(listOf("Omas Kochbuch", "Backen mit Lea", "Zettel von Tante Ute"), repository.recentSourceNames())
        assertEquals(listOf("Omas Kochbuch"), repository.recentSourceNames(limit = 1))
    }
}
