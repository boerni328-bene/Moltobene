package com.moltobene.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {

    // Die Liste zeigt nur Fotos des Gerichts, nie eine Originalseite.
    @Query(
        """
        SELECT id, title, isDraft,
            (SELECT photoId FROM recipe_photos WHERE recipe_photos.recipeId = recipes.id AND kind = 'photo'
             ORDER BY position LIMIT 1) AS photoId
        FROM recipes
        ORDER BY title COLLATE NOCASE, createdAt
        """
    )
    abstract fun observeSummaries(): Flow<List<RecipeSummaryRow>>

    @Query(
        """
        SELECT id, title, isDraft,
            (SELECT photoId FROM recipe_photos WHERE recipe_photos.recipeId = recipes.id AND kind = 'photo'
             ORDER BY position LIMIT 1) AS photoId
        FROM recipes
        WHERE id IN (SELECT recipeId FROM recipe_search WHERE recipe_search MATCH :match)
        ORDER BY title COLLATE NOCASE, createdAt
        """
    )
    abstract fun observeSearch(match: String): Flow<List<RecipeSummaryRow>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract fun observeDetails(id: String): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract suspend fun getDetails(id: String): RecipeWithDetails?

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY createdAt")
    abstract suspend fun getAllDetails(): List<RecipeWithDetails>

    @Query("SELECT updatedAt FROM recipes WHERE id = :id")
    abstract suspend fun getUpdatedAt(id: String): Long?

    @Query("SELECT id, updatedAt FROM recipes")
    abstract suspend fun getAllUpdatedAt(): List<RecipeUpdatedAt>

    @Query("SELECT photoId FROM recipe_photos")
    abstract suspend fun getAllPhotoIds(): List<String>

    @Upsert
    abstract suspend fun upsertRecipe(recipe: RecipeEntity)

    @Insert
    abstract suspend fun insertIngredients(rows: List<IngredientEntity>)

    @Insert
    abstract suspend fun insertSteps(rows: List<StepEntity>)

    @Insert
    abstract suspend fun insertPhotos(rows: List<RecipePhotoEntity>)

    @Insert
    abstract suspend fun insertTags(rows: List<RecipeTagEntity>)

    @Insert
    abstract suspend fun insertSearch(row: RecipeSearchEntity)

    @Query("DELETE FROM ingredients WHERE recipeId = :id")
    abstract suspend fun deleteIngredients(id: String)

    @Query("DELETE FROM steps WHERE recipeId = :id")
    abstract suspend fun deleteSteps(id: String)

    @Query("DELETE FROM recipe_photos WHERE recipeId = :id")
    abstract suspend fun deletePhotos(id: String)

    @Query("DELETE FROM recipe_tags WHERE recipeId = :id")
    abstract suspend fun deleteTags(id: String)

    @Query("DELETE FROM recipe_search WHERE recipeId = :id")
    abstract suspend fun deleteSearch(id: String)

    @Query("DELETE FROM recipes WHERE id = :id")
    abstract suspend fun deleteRecipeRow(id: String)

    /** Speichert ein Rezept samt Zutaten, Schritten, Fotos, Schlagwörtern und Suchverzeichnis in einem Schritt. */
    @Transaction
    open suspend fun save(rows: RecipeRows) {
        writeRows(rows)
    }

    /** Speichert viele Rezepte: alles oder nichts (für das Wiederherstellen). */
    @Transaction
    open suspend fun saveAll(all: List<RecipeRows>) {
        all.forEach { writeRows(it) }
    }

    @Transaction
    open suspend fun delete(id: String) {
        deleteSearch(id)
        deleteRecipeRow(id)
    }

    private suspend fun writeRows(rows: RecipeRows) {
        val id = rows.recipe.id
        upsertRecipe(rows.recipe)
        deleteIngredients(id)
        deleteSteps(id)
        deletePhotos(id)
        deleteTags(id)
        deleteSearch(id)
        insertIngredients(rows.ingredients)
        insertSteps(rows.steps)
        insertPhotos(rows.photos)
        insertTags(rows.tags)
        insertSearch(rows.search)
    }
}

data class RecipeUpdatedAt(val id: String, val updatedAt: Long)
