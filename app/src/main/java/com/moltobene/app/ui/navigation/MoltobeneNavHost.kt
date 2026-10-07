package com.moltobene.app.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.moltobene.app.ui.edit.SharedContent
import com.moltobene.app.ui.licenses.LicensesScreen
import com.moltobene.app.ui.licenses.LicensesViewModel
import com.moltobene.app.ui.recipe.RecipeScreen
import com.moltobene.app.ui.recipe.RecipeViewModel
import com.moltobene.app.ui.settings.SettingsScreen
import com.moltobene.app.ui.settings.SettingsViewModel
import com.moltobene.app.ui.whatsnew.WhatsNewDialog
import com.moltobene.app.ui.whatsnew.WhatsNewViewModel
import kotlinx.coroutines.launch

/** @param shared Bilder oder Text aus „Teilen mit…“; dann öffnet sich gleich das Formular dafür. */
@Composable
fun MoltobeneNavHost(shared: SharedContent? = null) {
    val navController = rememberNavController()
    val container = (LocalContext.current.applicationContext as MoltobeneApplication).container
    // Gemeinsame Meldungsleiste, damit z. B. „Rezept gespeichert“ auch nach dem Bildschirmwechsel erscheint.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { text -> scope.launch { snackbarHostState.showSnackbar(text) } }

    if (shared != null) {
        LaunchedEffect(shared) {
            container.sharedInput.offer(shared)
            navController.navigate(EditRoute(fromShare = true))
        }
    }

    NavHost(navController = navController, startDestination = CollectionRoute) {
        composable<CollectionRoute> {
            CollectionScreen(
                viewModel = viewModel {
                    CollectionViewModel(
                        container.repository,
                        container.photoStore,
                        container.pendingRecognition,
                        container.preferences,
                        container.translations,
                    )
                },
                snackbarHostState = snackbarHostState,
                onOpenRecipe = { id -> navController.navigate(RecipeRoute(id)) },
                onAddRecipe = { navController.navigate(EditRoute()) },
                onAddFromPhoto = { navController.navigate(EditRoute(fromPhoto = true)) },
                onAddFromText = { navController.navigate(EditRoute(fromText = true)) },
                onAddFromLink = { navController.navigate(EditRoute(fromLink = true)) },
                onAddFromFile = { navController.navigate(EditRoute(fromFile = true)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<RecipeRoute> {
            RecipeScreen(
                viewModel = viewModel {
                    RecipeViewModel(
                        createSavedStateHandle(),
                        container.repository,
                        container.photoStore,
                        container.recipeSharer,
                        container.translations,
                    )
                },
                snackbarHostState = snackbarHostState,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(EditRoute(id)) },
                onDeleted = { message ->
                    navController.popBackStack()
                    showMessage(message)
                },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                timerLauncher = container.timerLauncher,
            )
        }
        composable<EditRoute> {
            EditScreen(
                viewModel = viewModel {
                    EditViewModel(
                        createSavedStateHandle(),
                        container.repository,
                        container.photoStore,
                        container.textRecognizer,
                        container.preferences,
                        container.pendingRecognition,
                        container.sharedInput,
                        container.webImporter,
                        container.recipeFileReader,
                    )
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
                viewModel = viewModel { SettingsViewModel(container.backupManager, container.preferences, container.languagePack) },
                snackbarHostState = snackbarHostState,
                onBack = { navController.popBackStack() },
                onOpenLicenses = { navController.navigate(LicensesRoute) },
            )
        }
        composable<LicensesRoute> {
            val context = LocalContext.current
            LicensesScreen(
                viewModel = viewModel { LicensesViewModel(context) },
                onBack = { navController.popBackStack() },
            )
        }
    }

    // „Neu in Version …“ einmal nach einem Update, über dem jeweiligen Bildschirm.
    val whatsNew = viewModel { WhatsNewViewModel(container.preferences) }
    if (whatsNew.visible) WhatsNewDialog(onDismiss = whatsNew::dismiss)
}
