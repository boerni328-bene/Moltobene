package com.moltobene.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.SourceType
import com.moltobene.app.data.photos.PhotoStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Datenbank-Umbauten mit einer echten Datenbank: Eine Sammlung im alten Aufbau wird angelegt,
 * dann öffnet die App sie wie nach einem Update. Es darf nichts verloren gehen.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun version1WirdOhneVerlustUebernommen() = runBlocking {
        createDatabase(version = 1) { db ->
            db.execSQL(
                "INSERT INTO recipes (id, title, language, servings, servingsUnit, sourceType, sourceName, sourcePage, " +
                    "notes, favorite, isDraft, originalText, createdAt, updatedAt) VALUES " +
                    "('$KUCHEN', 'Apfelkuchen', 'de', 12, 'Stück', 'book', 'Omas Kochbuch', '42', 'Mit Sahne.', 1, 0, " +
                    "'Apfelkuchen\nZutaten: 200 g Mehl', 1000, 2000)",
            )
            db.execSQL("INSERT INTO recipes (id, title, notes, favorite, isDraft, createdAt, updatedAt) VALUES ('$SUPPE', 'Suppe', '', 0, 1, 3000, 3000)")
            db.execSQL("INSERT INTO ingredients (recipeId, position, text, isHeading) VALUES ('$KUCHEN', 0, 'Für den Teig', 1)")
            db.execSQL("INSERT INTO ingredients (recipeId, position, text, isHeading, quantity, unit) VALUES ('$KUCHEN', 1, '200 g Mehl', 0, 200.0, 'g')")
            db.execSQL("INSERT INTO steps (recipeId, position, text) VALUES ('$KUCHEN', 0, 'Backen.')")
            db.execSQL("INSERT INTO recipe_photos (photoId, recipeId, position) VALUES ('$FOTO_1', '$KUCHEN', 0)")
            db.execSQL("INSERT INTO recipe_photos (photoId, recipeId, position) VALUES ('$FOTO_2', '$KUCHEN', 1)")
            db.execSQL("INSERT INTO recipe_tags (recipeId, name, predefinedKey) VALUES ('$KUCHEN', 'Kuchen', 'cake')")
            db.execSQL("INSERT INTO recipe_search (recipeId, title, ingredients, notes) VALUES ('$KUCHEN', 'Apfelkuchen', 'Für den Teig\n200 g Mehl', 'Mit Sahne.')")
            db.execSQL("INSERT INTO recipe_search (recipeId, title, ingredients, notes) VALUES ('$SUPPE', 'Suppe', '', '')")
        }

        withRepository { repository ->
            val kuchen = repository.getRecipe(KUCHEN)!!
            assertEquals("Apfelkuchen", kuchen.title)
            assertEquals(12, kuchen.servings)
            assertEquals(SourceType.BOOK, kuchen.source?.type)
            assertEquals("42", kuchen.source?.page)
            assertEquals(listOf("Für den Teig", "200 g Mehl"), kuchen.ingredients.map { it.text })
            assertEquals(200.0, kuchen.ingredients[1].quantity)
            assertEquals(listOf("Backen."), kuchen.steps)
            assertEquals("cake", kuchen.tags.single().predefinedKey)
            assertTrue(kuchen.favorite)
            assertEquals("Apfelkuchen\nZutaten: 200 g Mehl", kuchen.originalText)
            // Vorhandene Fotos bleiben Fotos des Gerichts, in derselben Reihenfolge.
            assertEquals(listOf(FOTO_1, FOTO_2), kuchen.photoIds)
            assertEquals(emptyList<String>(), kuchen.pageIds)
            assertTrue(repository.getRecipe(SUPPE)!!.isDraft)

            // Sammlung und Suche funktionieren wie vorher.
            val summaries = repository.observeSummaries("").first()
            assertEquals(listOf("Apfelkuchen", "Suppe"), summaries.map { it.title })
            assertEquals(FOTO_1, summaries.first().photoId)
            assertEquals(listOf(KUCHEN), repository.observeSummaries("mehl").first().map { it.id })
        }
    }

    @Test
    fun originalseitenBleibenGetrenntVomFotoDesGerichts() = runBlocking {
        withRepository { repository ->
            repository.save(Recipe(id = KUCHEN, title = "Apfelkuchen", photoIds = listOf(FOTO_1), pageIds = listOf(SEITE_1, SEITE_2), createdAt = 1, updatedAt = 1))
            repository.save(Recipe(id = SUPPE, title = "Suppe", pageIds = listOf(SEITE_3), createdAt = 2, updatedAt = 2))

            val kuchen = repository.getRecipe(KUCHEN)!!
            assertEquals(listOf(FOTO_1), kuchen.photoIds)
            assertEquals(listOf(SEITE_1, SEITE_2), kuchen.pageIds)

            // Die Sammlung zeigt nie eine Originalseite als Foto.
            val summaries = repository.observeSummaries("").first().associateBy { it.id }
            assertEquals(FOTO_1, summaries.getValue(KUCHEN).photoId)
            assertNull(summaries.getValue(SUPPE).photoId)

            // Erneutes Speichern ohne eine Seite entfernt nur diese.
            repository.save(kuchen.copy(pageIds = listOf(SEITE_2)))
            assertEquals(listOf(SEITE_2), repository.getRecipe(KUCHEN)!!.pageIds)
        }
    }

    /** Legt die Datenbank genau so an, wie die App in [version] sie angelegt hat (Bauplan aus app/schemas). */
    private fun createDatabase(version: Int, fill: (SQLiteDatabase) -> Unit) {
        val schema = Json.parseToJsonElement(File(SCHEMA_DIR, "$version.json").readText())
            .jsonObject.getValue("database").jsonObject
        val file = context.getDatabasePath(MoltobeneDatabase.NAME).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schema.getValue("entities").jsonArray.forEach { element ->
                val entity = element.jsonObject
                val table = entity.getValue("tableName").jsonPrimitive.content
                db.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                entity["indices"]?.jsonArray?.forEach { index ->
                    db.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.version = version
            fill(db)
        }
    }

    private suspend fun withRepository(block: suspend (RecipeRepository) -> Unit) {
        val database = MoltobeneDatabase.create(context)
        try {
            block(RecipeRepository(database.recipeDao(), PhotoStore(context)))
        } finally {
            database.close()
        }
    }

    private companion object {
        const val SCHEMA_DIR = "schemas/com.moltobene.app.data.db.MoltobeneDatabase"
        const val KUCHEN = "aaaaaaaa-0000-0000-0000-000000000001"
        const val SUPPE = "aaaaaaaa-0000-0000-0000-000000000002"
        const val FOTO_1 = "bbbbbbbb-0000-0000-0000-000000000001"
        const val FOTO_2 = "bbbbbbbb-0000-0000-0000-000000000002"
        const val SEITE_1 = "cccccccc-0000-0000-0000-000000000001"
        const val SEITE_2 = "cccccccc-0000-0000-0000-000000000002"
        const val SEITE_3 = "cccccccc-0000-0000-0000-000000000003"
    }
}
