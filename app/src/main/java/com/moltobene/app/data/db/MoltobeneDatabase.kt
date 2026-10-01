package com.moltobene.app.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Datenbank der App. Bei jeder Änderung am Aufbau: [version] erhöhen, eine Migration schreiben
 * und testen – gespeicherte Rezepte dürfen bei einem Update nie verloren gehen.
 *
 * Versionen:
 *  - 1: erste Version (0.3.0)
 *  - 2: Fotos haben eine Art – Foto des Gerichts oder Originalseite (#38). Vorhandene Fotos bleiben Fotos des Gerichts.
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
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class MoltobeneDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao

    companion object {
        const val NAME = "moltobene.db"

        fun create(context: Context): MoltobeneDatabase =
            Room.databaseBuilder(context, MoltobeneDatabase::class.java, NAME).build()
    }
}
