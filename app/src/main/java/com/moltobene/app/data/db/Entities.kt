package com.moltobene.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val language: String?,
    val servings: Int?,
    val servingsUnit: String?,
    val sourceType: String?,
    val sourceName: String?,
    val sourceUrl: String?,
    val sourcePage: String?,
    val notes: String,
    val favorite: Boolean,
    val isDraft: Boolean,
    val prepMinutes: Int?,
    val totalMinutes: Int?,
    val originalText: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val position: Int,
    val text: String,
    val isHeading: Boolean,
    val quantity: Double?,
    val unit: String?,
)

@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: String,
    val position: Int,
    val text: String,
)

@Entity(
    tableName = "recipe_photos",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class RecipePhotoEntity(
    @PrimaryKey val photoId: String,
    val recipeId: String,
    val position: Int,
)

@Entity(
    tableName = "recipe_tags",
    primaryKeys = ["recipeId", "name"],
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class RecipeTagEntity(
    val recipeId: String,
    val name: String,
    val predefinedKey: String?,
)

/**
 * Suchverzeichnis (Volltextsuche) über Titel, Zutaten und Notizen.
 * unicode61 findet auch ohne Umlaute/Akzente („kase“ findet „Käse“).
 */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, notIndexed = ["recipeId"])
@Entity(tableName = "recipe_search")
data class RecipeSearchEntity(
    val recipeId: String,
    val title: String,
    val ingredients: String,
    val notes: String,
)

/** Zeile der Sammlungsliste: nur das, was die Liste anzeigt. */
data class RecipeSummaryRow(
    val id: String,
    val title: String,
    val isDraft: Boolean,
    val photoId: String?,
)

data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val steps: List<StepEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val photos: List<RecipePhotoEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val tags: List<RecipeTagEntity>,
)

/** Alles, was zu einem Rezept gespeichert wird – für das Schreiben in einem Schritt. */
data class RecipeRows(
    val recipe: RecipeEntity,
    val ingredients: List<IngredientEntity>,
    val steps: List<StepEntity>,
    val photos: List<RecipePhotoEntity>,
    val tags: List<RecipeTagEntity>,
    val search: RecipeSearchEntity,
)
