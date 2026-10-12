package com.moltobene.app.data.basics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Liest die mitgelieferten Grundrezepte (#82) aus `assets/`; nie auf dem Hauptthread. */
class BasicRecipeStore(context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(): List<BasicRecipes.BasicRecipe> = withContext(Dispatchers.IO) {
        BasicRecipes.parse(appContext.assets.open(BasicRecipes.ASSET).bufferedReader().use { it.readText() })
    }
}
