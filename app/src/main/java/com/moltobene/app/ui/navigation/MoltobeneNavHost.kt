package com.moltobene.app.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.moltobene.app.MoltobeneApplication
import com.moltobene.app.data.ScreenOn
import com.moltobene.app.data.StartScreen
import com.moltobene.app.ui.collection.CollectionScreen
import com.moltobene.app.ui.basics.BasicsViewModel
import com.moltobene.app.ui.basics.BasicsScreen
import com.moltobene.app.ui.knowhow.KnowHowScreen
import com.moltobene.app.ui.knowhow.KnowHowViewModel
import com.moltobene.app.ui.collection.CollectionViewModel
import com.moltobene.app.R
import com.moltobene.app.ui.dough.DoughScreen
import com.moltobene.app.ui.dough.DoughViewModel
import com.moltobene.app.ui.edit.EditScreen
import com.moltobene.app.ui.edit.EditViewModel
import com.moltobene.app.ui.edit.SharedContent
import com.moltobene.app.ui.home.HomeScreen
import com.moltobene.app.ui.home.HomeViewModel
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

    val startScreen by container.preferences.startScreen.collectAsStateWithLifecycle()

    // Die Startseite ist die Wurzel: Zurück aus der Sammlung führt hierher, Zurück von hier beendet die App (#81).
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            // „Beim Öffnen: Rezepte“: gleich weiter zur Sammlung, nur einmal und nicht bei geteilten Inhalten. Bis dahin
            // bleibt die Seite leer, damit die Startseite nicht kurz aufblitzt. Zurück führt dann zur Startseite.
            var forwardToCollection by rememberSaveable {
                mutableStateOf(startScreen == StartScreen.COLLECTION && shared == null)
            }
            if (forwardToCollection) {
                LaunchedEffect(Unit) {
                    forwardToCollection = false
                    navController.navigate(CollectionRoute)
                }
            } else {
                HomeScreen(
                    viewModel = viewModel { HomeViewModel(container.repository, container.photoStore, container.preferences) },
                    snackbarHostState = snackbarHostState,
                    onOpenCollection = { navController.navigate(CollectionRoute) },
                    onOpenDough = { navController.navigate(DoughRoute) },
                    onOpenBasics = { navController.navigate(BasicsRoute) },
                    onOpenRecipe = { id -> navController.navigate(RecipeRoute(id)) },
                    onAddRecipe = { navController.navigate(EditRoute()) },
                    onAddFromPhoto = { navController.navigate(EditRoute(fromPhoto = true)) },
                    onAddFromText = { navController.navigate(EditRoute(fromText = true)) },
                    onAddFromLink = { navController.navigate(EditRoute(fromLink = true)) },
                    onAddFromFile = { navController.navigate(EditRoute(fromFile = true)) },
                    onAddFromAudio = { navController.navigate(EditRoute(fromAudio = true)) },
                    onAddFromSpeech = { navController.navigate(EditRoute(fromSpeech = true)) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }
        }
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
                onAddFromAudio = { navController.navigate(EditRoute(fromAudio = true)) },
                onAddFromSpeech = { navController.navigate(EditRoute(fromSpeech = true)) },
                onOpenDough = { navController.navigate(DoughRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                // Zurück zur Startseite; ist sie nicht im Stapel (Start im Kochbuch), wird sie geöffnet.
                onOpenHome = {
                    if (!navController.popBackStack<HomeRoute>(inclusive = false)) {
                        navController.navigate(HomeRoute) { popUpTo<CollectionRoute> { inclusive = true } }
                    }
                },
            )
        }
        composable<RecipeRoute> { entry ->
            // „Zuletzt geöffnet“ auf der Startseite (#81): nur auf diesem Handy gemerkt.
            val openedId = entry.toRoute<RecipeRoute>().id
            LaunchedEffect(openedId) { runCatching { container.preferences.addRecentRecipe(openedId) } }
            val screenOn by container.preferences.screenOn.collectAsStateWithLifecycle()
            val speechSettings by container.preferences.speechSettings.collectAsStateWithLifecycle()
            // „Große Schrift beim Kochen“ (#61): gemerkt in den kleinen Merkwerten.
            var cookingLarge by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) { runCatching { container.preferences.cookingLargeText() }.getOrNull()?.let { cookingLarge = it } }
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
                screenOn = screenOn ?: ScreenOn.STAYS_ON,
                speech = container.speech,
                voice = container.voice,
                headsetButtons = speechSettings.headsetButtons,
                headsetMapping = speechSettings.headsetMapping(),
                cookingLarge = cookingLarge,
                onCookingLargeChange = { large ->
                    cookingLarge = large
                    scope.launch { runCatching { container.preferences.setCookingLargeText(large) } }
                },
                snackbarHostState = snackbarHostState,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(EditRoute(id)) },
                onDeleted = { message ->
                    navController.popBackStack()
                    showMessage(message)
                },
                onOpenSettings = { navController.navigate(SettingsRoute) },
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
                        container.transcriber,
                        container.dictation,
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
        composable<BasicsRoute> {
            BasicsScreen(
                viewModel = viewModel { BasicsViewModel(container.basicRecipes, container.repository) },
                snackbarHostState = snackbarHostState,
                onBack = { navController.popBackStack() },
                onOpenRecipe = { id -> navController.navigate(RecipeRoute(id)) },
            )
        }
        composable<KnowHowRoute> {
            KnowHowScreen(
                viewModel = viewModel { KnowHowViewModel(container.knowHow) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<DoughRoute> {
            val context = LocalContext.current
            val resources = LocalResources.current
            DoughScreen(
                viewModel = viewModel { DoughViewModel(context, container.preferences, container.repository) },
                snackbarHostState = snackbarHostState,
                onBack = { navController.popBackStack() },
                onOpenKnowHow = { navController.navigate(KnowHowRoute) },
                onSaved = { id ->
                    navController.navigate(EditRoute(id)) { popUpTo<DoughRoute> { inclusive = true } }
                    showMessage(resources.getString(R.string.dough_saved))
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                viewModel = viewModel { SettingsViewModel(container.backupManager, container.preferences, container.languagePack, container.voicePack, container.storageUsage) { container.speech } },
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
