package com.moltobene.app.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.moltobene.app.MoltobeneApplication
import com.moltobene.app.ui.collection.CollectionScreen
import com.moltobene.app.ui.collection.CollectionViewModel
import com.moltobene.app.ui.edit.EditScreen
import com.moltobene.app.ui.edit.EditViewModel
import com.moltobene.app.ui.recipe.RecipeScreen
import com.moltobene.app.ui.recipe.RecipeViewModel
import com.moltobene.app.ui.settings.SettingsScreen
import com.moltobene.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun MoltobeneNavHost() {
    val navController = rememberNavController()
    val container = (LocalContext.current.applicationContext as MoltobeneApplication).container
    // Gemeinsame Meldungsleiste, damit z. B. „Rezept gespeichert“ auch nach dem Bildschirmwechsel erscheint.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { text -> scope.launch { snackbarHostState.showSnackbar(text) } }

    NavHost(navController = navController, startDestination = CollectionRoute) {
        composable<CollectionRoute> {
            CollectionScreen(
                viewModel = viewModel { CollectionViewModel(container.repository, container.photoStore) },
                snackbarHostState = snackbarHostState,
                onOpenRecipe = { id -> navController.navigate(RecipeRoute(id)) },
                onAddRecipe = { navController.navigate(EditRoute()) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<RecipeRoute> {
            RecipeScreen(
                viewModel = viewModel {
                    RecipeViewModel(createSavedStateHandle(), container.repository, container.photoStore)
                },
                snackbarHostState = snackbarHostState,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(EditRoute(id)) },
                onDeleted = { message ->
                    navController.popBackStack()
                    showMessage(message)
                },
            )
        }
        composable<EditRoute> {
            EditScreen(
                viewModel = viewModel {
                    EditViewModel(createSavedStateHandle(), container.repository, container.photoStore)
                },
                snackbarHostState = snackbarHostState,
                onClose = { navController.popBackStack() },
                onSaved = { id, wasNew, message ->
                    if (wasNew) {
                        navController.navigate(RecipeRoute(id)) {
                            popUpTo<EditRoute> { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                    showMessage(message)
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                viewModel = viewModel { SettingsViewModel(container.backupManager) },
                snackbarHostState = snackbarHostState,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
