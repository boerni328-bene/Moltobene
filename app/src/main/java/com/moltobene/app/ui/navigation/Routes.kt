package com.moltobene.app.ui.navigation

import kotlinx.serialization.Serializable

// Ziele der Navigation. Werden über ihren Namen gefunden – siehe Schutzregel in proguard-rules.pro.

@Serializable
object CollectionRoute

@Serializable
data class RecipeRoute(val id: String)

/** [id] leer = neues Rezept hinzufügen; [fromPhoto] = gleich mit der Texterkennung beginnen. */
@Serializable
data class EditRoute(val id: String? = null, val fromPhoto: Boolean = false)

@Serializable
object SettingsRoute
