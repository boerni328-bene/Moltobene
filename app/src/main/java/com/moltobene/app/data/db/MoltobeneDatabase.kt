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
 *  - 3: Rezepte haben einen Link zu einem Video (0.25.0), getrennt von der Quelle. Vorhandene Rezepte haben keinen.
 *  - 4: Zeitplan der Teige (#71): Phasen je Rezept und laufende Teige als neue Tabellen. Vorhandene Rezepte haben keinen Zeitplan.
 */
@Database(
    entities = [
        RecipeEntity::class,
        IngredientEntity::class,
        StepEntity::class,
        PhaseEntity::class,
        DoughRunEntity::class,
        RecipePhotoEntity::class,
        RecipeTagEntity::class,
        RecipeSearchEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class MoltobeneDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao

    companion object {
        const val NAME = "moltobene.db"

        fun create(context: Context): MoltobeneDatabase =
            Room.databaseBuilder(context, MoltobeneDatabase::class.java, NAME).build()
    }
}
