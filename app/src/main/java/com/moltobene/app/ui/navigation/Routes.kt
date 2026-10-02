package com.moltobene.app.ui.navigation

import kotlinx.serialization.Serializable

// Ziele der Navigation. Werden über ihren Namen gefunden – siehe Schutzregel in proguard-rules.pro.

@Serializable
object CollectionRoute

@Serializable
data class RecipeRoute(val id: String)

/**
 * [id] leer = neues Rezept hinzufügen; [fromPhoto] = gleich mit der Texterkennung beginnen;
 * [fromText] = gleich mit „Aus Text übernehmen“ beginnen; [fromShare] = Bilder oder Text aus „Teilen mit…“ übernehmen.
 */
@Serializable
data class EditRoute(
    val id: String? = null,
    val fromPhoto: Boolean = false,
    val fromText: Boolean = false,
    val fromShare: Boolean = false,
)

@Serializable
object SettingsRoute

@Serializable
object LicensesRoute
