package com.moltobene.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Datenbank der App. Bei jeder Änderung am Aufbau: [version] erhöhen, eine Migration schreiben
 * und testen – gespeicherte Rezepte dürfen bei einem Update nie verloren gehen.
 */
@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        RecipePhotoEntity::class,
        RecipeTagEntity::class,
        RecipeSearchEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MoltobeneDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao

    companion object {
        fun create(context: Context): MoltobeneDatabase =
            Room.databaseBuilder(context, MoltobeneDatabase::class.java, "moltobene.db").build()
    }
}
