package com.moltobene.app.ui.recipe

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
import com.moltobene.app.data.Adjustment
import com.moltobene.app.data.DoughRun
import com.moltobene.app.data.AmountScaling
import com.moltobene.app.data.PanScaling
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeText
import com.moltobene.app.data.ScreenOn
import com.moltobene.app.data.ScreenOnTimer
import com.moltobene.app.data.ocr.TextLanguage
import com.moltobene.app.data.share.PreparedShare
import com.moltobene.app.data.share.RecipeShareText
import com.moltobene.app.data.share.ShareLabels
import com.moltobene.app.data.speech.HeadsetAction
import com.moltobene.app.data.speech.HeadsetMapping
import com.moltobene.app.data.CookingTextSize
import com.moltobene.app.data.speech.SpeechOutput
import com.moltobene.app.data.speech.SpeechState
import com.moltobene.app.data.speech.SpokenText
import com.moltobene.app.data.speech.headphonesConnected
import com.moltobene.app.data.translate.LanguagePack
import com.moltobene.app.data.translate.RecipeTranslations
import com.moltobene.app.data.voice.VoiceCommand
import com.moltobene.app.data.voice.VoiceInput
import com.moltobene.app.data.voice.VoiceProblem
import com.moltobene.app.data.voice.VoiceState
import com.moltobene.app.ui.components.AppSnackbarHost
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.components.DraftLabel
import com.moltobene.app.ui.components.PageViewer
import com.moltobene.app.ui.components.RecipePhoto
import com.moltobene.app.ui.components.RecipePhotoLarge
import com.moltobene.app.ui.components.TagLabels
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.components.DashedDivider
import com.moltobene.app.ui.components.isShortHeight
import com.moltobene.app.ui.components.rememberAmountWidth
import com.moltobene.app.ui.components.isWideLayout
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.moltobene.app.ui.schedule.ScheduleActions
import com.moltobene.app.ui.schedule.ScheduleCard
import com.moltobene.app.ui.schedule.rememberNow
import com.moltobene.app.ui.theme.Spacing
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel,
    screenOn: ScreenOn,
    speech: SpeechOutput,
    voice: VoiceInput,
    headsetButtons: Boolean = true,
    headsetMapping: HeadsetMapping = HeadsetMapping(),
    cookingSize: CookingTextSize = CookingTextSize.MEDIUM,
    onCookingSizeChange: (CookingTextSize) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val translation by viewModel.translation.collectAsStateWithLifecycle()
    val checked by viewModel.checkedIngredients.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val adjustment by viewModel.adjustment.collectAsStateWithLifecycle()
    val runs by viewModel.runs.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    // Das Blättern liegt hier, damit die Leiste über dem Foto erst beim Blättern deckend wird (Rang 9).
    val scrollState = rememberScrollState()
    val cookingBarHolder = remember { CookingBarHolder() }
    val deletedMessage = stringResource(R.string.recipe_deleted)
    val context = LocalContext.current
    val resources = LocalResources.current

    // Jede Berührung startet die Frist für den Bildschirm neu (#68).
    val onTouch = keepScreenOn(screenOn)

    // Vorlesen (#79): hält an, wenn die Ansicht den Vordergrund verlässt (auch Bildschirm aus), und wird beim Verlassen frei.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { speech.stop() }
    DisposableEffect(speech) { onDispose { speech.release() } }
    val speechState by speech.state.collectAsStateWithLifecycle()
    (speechState as? SpeechState.NoVoice)?.let { problem ->
        val message = stringResource(R.string.speech_no_voice, languageName(problem.language))
        val action = stringResource(R.string.settings_title)
        LaunchedEffect(problem) {
            val result = snackbarHostState.showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) openSpeechSettings(context)
            speech.stop()
        }
    }
    if (speechState is SpeechState.Unavailable) {
        val message = stringResource(R.string.speech_unavailable)
        LaunchedEffect(speechState) {
            snackbarHostState.showSnackbar(message)
            speech.stop()
        }
    }

    // Zurück aus den Einstellungen: Ist das Sprachpaket inzwischen da, wird gleich übersetzt.
    LifecycleResumeEffect(Unit) {
        viewModel.retryTranslation()
        onPauseOrDispose {}
    }

    viewModel.shareEvent?.let { event ->
        val chooserTitle = stringResource(R.string.share_recipe)
        val errorText = stringResource(R.string.share_error)
        LaunchedEffect(event) {
            val opened = event is ShareEvent.Ready && openShareMenu(context, event.share, chooserTitle)
            if (!opened) snackbarHostState.showSnackbar(errorText)
            viewModel.shareEvent = null
        }
    }

    // „Originalseiten ansehen“ als Vollbild.
    val pageCount = (state as? RecipeUiState.Content)?.recipe?.pageIds?.size ?: 0
    viewModel.viewer.page?.let { page ->
        if (pageCount > 0) {
            Box(modifier = Modifier.observeTouches(onTouch)) {
                PageViewer(
                    page = page.coerceAtMost(pageCount - 1),
                    pageCount = pageCount,
                    bitmap = viewModel.viewer.bitmap,
                    failed = viewModel.viewer.failed,
                    onPageChange = { if (it in 0 until pageCount) viewModel.viewer.open(it) },
                    onClose = viewModel.viewer::close,
                )
            }
            return
        }
    }
    viewModel.photoViewer.page?.let {
        Box(modifier = Modifier.observeTouches(onTouch)) {
            PageViewer(
                page = 0,
                pageCount = 1,
                bitmap = viewModel.photoViewer.bitmap,
                failed = viewModel.photoViewer.failed,
                onPageChange = {},
                onClose = viewModel.photoViewer::close,
                title = stringResource(R.string.photo),
                errorText = stringResource(R.string.photo_load_error),
            )
        }
        return
    }

    // Mit Foto und ohne Übersetzungsbalken liegt das Foto unter der Leiste, der Inhalt nutzt dann die Höhe der Leiste mit.
    val barOverPhoto = !deleting && !isWideLayout() && (state as? RecipeUiState.Content)?.let {
        it.photo != null && !translationBarShown(it.recipe, translation)
    } == true
    Scaffold(
        // Jede Berührung, auch beim Blättern, startet die Frist für den Bildschirm neu (#68); sie wird nur beobachtet.
        modifier = Modifier.observeTouches(onTouch),
        topBar = {
            // Foto bis unter die Leiste (Rang 9): Solange oben nichts gescrollt ist, ist die Leiste durchsichtig, mit einem
            // einfachen Verlauf in der Farbe des Hintergrunds für die Lesbarkeit; beim Blättern wird sie deckend.
            val overPhoto = barOverPhoto && scrollState.value == 0
            Box {
                if (overPhoto) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.surface.copy(alpha = BAR_GRADIENT_ALPHA), Color.Transparent),
                                ),
                            ),
                    )
                }
                TopAppBar(
                    colors = if (overPhoto) {
                        TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    } else {
                        TopAppBarDefaults.topAppBarColors()
                    },
                    // Name der App und Umschalter „DE | EN“ bleiben beim Blättern oben stehen (Wunsch vom 08.10.2026).
                    title = {
                        val top = cookingBarHolder.data
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            AppTitle(style = MaterialTheme.typography.titleMedium)
                            // Handy quer: Die Kochleiste ist hier oben; „Schritt 2 von 5“ steht neben dem Namen der App.
                            top?.statusText()?.let { status ->
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        val content = state as? RecipeUiState.Content
                        if (content != null && !deleting) {
                            cookingBarHolder.data?.let { CookingBarTop(it) }
                            val original = remember(content.recipe) { RecipeTranslations.languageOf(content.recipe) }
                            if (original != null && LanguagePack.targets(original).isNotEmpty()) {
                                LanguageSwitch(
                                    original = original,
                                    selected = translation.selectedLanguage(original),
                                    onSelect = { viewModel.showLanguage(it, original) },
                                )
                            }
                            IconButton(onClick = { onEdit(viewModel.recipeId) }) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_recipe))
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    RecipeMenuItems(
                                        favorite = content.recipe.favorite,
                                        shareEnabled = !viewModel.preparingShare,
                                        textSize = cookingSize,
                                        onTextSize = onCookingSizeChange,
                                        onFavorite = viewModel::toggleFavorite,
                                        onShare = { viewModel.share(asFile = false, labels = shareLabels(resources)) },
                                        onShareFile = { viewModel.share(asFile = true, labels = shareLabels(resources)) },
                                        onDelete = { confirmDelete = true },
                                        onDismiss = { menuOpen = false },
                                        onReset = viewModel::resetProgress,
                                        resetEnabled = checked.isNotEmpty() || currentStep != null,
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { padding ->
        val current = state
        when {
            deleting -> CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
            current is RecipeUiState.Content -> CompositionLocalProvider(LocalCookingSize provides cookingSize) { RecipeContent(
                recipe = current.recipe,
                photo = current.photo,
                cooking = CookingState(
                    checked = checked.toSet(),
                    currentStep = currentStep,
                    adjustment = adjustment,
                    onToggleIngredient = viewModel::toggleIngredient,
                    onToggleStep = viewModel::toggleCurrentStep,
                    onAdjust = viewModel::setAdjustment,
                    onPreviousStep = viewModel::previousStep,
                    onNextStep = viewModel::nextStep,
                ),
                schedule = ScheduleActions(
                    onStart = viewModel::startRun,
                    onComplete = viewModel::completePhase,
                    onUndo = viewModel::undoPhase,
                    onRemove = viewModel::removeRun,
                ),
                runs = runs,
                translation = TranslationActions(
                    state = translation,
                    onShowLanguage = viewModel::showLanguage,
                    onRetry = viewModel::retryTranslation,
                    onOpenSettings = onOpenSettings,
                ),
                speech = speech,
                voice = voice,
                headsetButtons = headsetButtons,
                headsetMapping = headsetMapping,
                snackbarHostState = snackbarHostState,
                onShowPages = { viewModel.viewer.open(0) },
                onShowPhoto = { viewModel.photoViewer.open(0) },
                scrollState = scrollState,
                onSaveNotes = viewModel::saveNotes,
                barInset = if (barOverPhoto) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + BAR_HEIGHT else 0.dp,
                topBarHolder = cookingBarHolder,
                modifier = if (barOverPhoto) Modifier.padding(bottom = padding.calculateBottomPadding()) else Modifier.padding(padding),
            ) }
            current is RecipeUiState.Loading ->
                CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
            current is RecipeUiState.NotFound ->
                CenteredMessage(text = stringResource(R.string.recipe_not_found), modifier = Modifier.padding(padding))
            else -> CenteredMessage(text = stringResource(R.string.recipe_load_error), modifier = Modifier.padding(padding))
        }
    }

    val content = state as? RecipeUiState.Content
    if (confirmDelete && content != null) {
        val title = content.recipe.title.ifBlank { stringResource(R.string.untitled_recipe) }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text, title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    deleting = true
                    viewModel.delete { onDeleted(deletedMessage) }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Texte für den geteilten Rezepttext, in der Sprache der Oberfläche. */
/** Öffnet die Einstellungen der Sprachausgabe des Handys, sonst die allgemeinen Einstellungen. */
internal fun openSpeechSettings(context: Context) {
    val intents = listOf(Intent("com.android.settings.TTS_SETTINGS"), Intent(android.provider.Settings.ACTION_SETTINGS))
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (e: ActivityNotFoundException) {
            // Nächster Versuch.
        } catch (e: SecurityException) {
            // Nächster Versuch.
        }
    }
}

internal fun shareLabels(resources: Resources) = ShareLabels(
    untitled = resources.getString(R.string.untitled_recipe),
    ingredients = resources.getString(R.string.ingredients),
    instructions = resources.getString(R.string.instructions),
    servings = { count, unit ->
        if (unit == null) {
            resources.getQuantityString(R.plurals.servings_count, count, count)
        } else {
            resources.getString(R.string.servings_with_unit, count, unit)
        }
    },
    source = { resources.getString(R.string.share_source, it) },
    page = { resources.getString(R.string.source_page, it) },
    video = { resources.getString(R.string.share_video, it) },
    tag = { TagLabels.text(resources, it) },
)

/** Öffnet das Android-Teilen-Menü. Andere Apps dürfen nur die übergebene Datei lesen. */
internal fun openShareMenu(context: Context, share: PreparedShare, chooserTitle: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = share.mimeType
        putExtra(Intent.EXTRA_SUBJECT, share.subject)
        share.text?.let { putExtra(Intent.EXTRA_TEXT, it) }
        share.fileUri?.let { uri ->
            putExtra(Intent.EXTRA_STREAM, uri)
            // Text und Datei gemeinsam, wie Android es selbst macht: Manche Apps (z. B. WhatsApp)
            // lesen nur diesen Teil und übernehmen den Text sonst nicht.
            clipData = ClipData(share.subject, arrayOf(share.mimeType), ClipData.Item(share.text, null, null, uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    return try {
        context.startActivity(Intent.createChooser(send, chooserTitle))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** Beobachtet jede Berührung, auch beim Blättern, ohne sie abzufangen. */
internal fun Modifier.observeTouches(onTouch: () -> Unit): Modifier = pointerInput(onTouch) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            onTouch()
        }
    }
}

/**
 * Beim Kochen bleibt der Bildschirm an – so lange, wie unter „Bildschirm in der Rezeptansicht“ gewählt (#68). Nur über
 * „Bildschirm anlassen“ der Ansicht, ohne Berechtigung und ohne Hintergrunddienst. Gibt zurück, was bei jeder Berührung
 * aufgerufen wird: Sie startet die Frist neu und schaltet nach dem Ausgehen wieder an.
 */
@Composable
internal fun keepScreenOn(setting: ScreenOn): () -> Unit {
    val view = LocalView.current
    val timer = remember(setting) { ScreenOnTimer(setting) { SystemClock.uptimeMillis() } }
    val touches = remember { Channel<Unit>(Channel.CONFLATED) }
    LaunchedEffect(view, timer) {
        while (true) {
            val remaining = timer.remainingMillis()
            view.keepScreenOn = remaining == null || remaining > 0
            when {
                remaining == null -> awaitCancellation()
                remaining > 0 -> withTimeoutOrNull(remaining) { touches.receive() }
                else -> touches.receive()
            }
        }
    }
    DisposableEffect(view) {
        onDispose { view.keepScreenOn = false }
    }
    return remember(timer) {
        val onTouch: () -> Unit = {
            timer.touch()
            touches.trySend(Unit)
        }
        onTouch
    }
}

/** Was beim Kochen dazukommt (#56): Häkchen, aktueller Schritt und Umrechnen (#66). */
internal class CookingState(
    val checked: Set<String>,
    val currentStep: String?,
    val adjustment: Adjustment?,
    val onToggleIngredient: (Int, String) -> Unit,
    val onToggleStep: (Int, String) -> Unit,
    val onAdjust: (Adjustment?) -> Unit,
    val onPreviousStep: () -> StepMove,
    val onNextStep: () -> StepMove,
)

@Composable
internal fun RecipeContent(
    recipe: Recipe,
    photo: File?,
    cooking: CookingState,
    translation: TranslationActions,
    schedule: ScheduleActions,
    runs: List<DoughRun>,
    speech: SpeechOutput,
    voice: VoiceInput,
    headsetButtons: Boolean,
    headsetMapping: HeadsetMapping,
    snackbarHostState: SnackbarHostState,
    onShowPages: () -> Unit,
    onShowPhoto: () -> Unit,
    onSaveNotes: (String) -> Unit,
    scrollState: ScrollState,
    barInset: Dp,
    topBarHolder: CookingBarHolder?,
    modifier: Modifier = Modifier,
) {
    // Das Foto läuft oben bündig unter die Leiste; unten sind die Ecken weich.
    val photoShape = MaterialTheme.shapes.extraLarge.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
    // Übersetzung (#60): Sie ersetzt nur die Anzeige; Abhaken und Portionen beziehen sich weiter aufs Original.
    val originalLanguage = remember(recipe) { RecipeTranslations.languageOf(recipe) }
    val translated = (translation.state as? TranslationState.Shown)?.translation
    val shownLanguage = translated?.language ?: originalLanguage
    val textLocale = remember(shownLanguage) { shownLanguage?.let { LocaleList(it) } }
    val title = translated?.title ?: recipe.title
    val originalServings = recipe.servings?.takeIf { it > 0 }
    val servingsUnit = (translated?.servingsUnit ?: recipe.servingsUnit)?.takeIf { it.isNotBlank() }

    // Umrechnen (#66): Gerechnet wird mit dem Original, auch wenn die Übersetzung gezeigt wird (#60).
    val deviceLanguage = LocalConfiguration.current.locales[0].language
    val originalTexts = remember(recipe.ingredients) { recipe.ingredients.map { it.text } }
    val recipeLanguage = remember(recipe.language, originalTexts, deviceLanguage) {
        recipe.language ?: TextLanguage.detect(originalTexts.joinToString("\n")) ?: deviceLanguage
    }
    val factor = remember(cooking.adjustment, originalServings, originalTexts, recipeLanguage) {
        Adjustment.factor(cooking.adjustment, originalServings, originalTexts, recipeLanguage)
    }
    // Mengen werden so geschrieben wie im Rezept bzw. in der Übersetzung; ohne Angabe gilt die erkannte Sprache.
    val language = translated?.language ?: recipeLanguage
    val texts = translated?.ingredients?.takeIf { it.size == recipe.ingredients.size } ?: originalTexts
    // Die Zutat, nach der umgerechnet wird, zeigt genau die eingetippte Menge.
    val exactIndex = (cooking.adjustment as? Adjustment.ByIngredient)?.index?.takeIf { factor != 1.0 }
    val lines = remember(texts, factor, language, exactIndex) {
        texts.mapIndexed { index, text -> AmountScaling.scale(text, factor, language, exactLeading = index == exactIndex) }
    }
    // Zur Wahl für „Nach Zutat“ stehen nur Zeilen mit einer lesbaren Menge am Anfang.
    val choices = remember(recipe.ingredients, texts, recipeLanguage, language) {
        recipe.ingredients.mapIndexedNotNull { index, ingredient ->
            if (ingredient.isHeading) return@mapIndexedNotNull null
            val amount = AmountScaling.leadingAmount(ingredient.text, recipeLanguage) ?: return@mapIndexedNotNull null
            AdjustChoice(
                index = index,
                line = ingredient.text,
                shown = texts[index],
                amount = amount.value,
                unit = AmountScaling.leadingAmount(texts[index], language)?.unit ?: amount.unit,
            )
        }
    }

    // Vorlesen und Kochleiste (#79): gelesen wird, was angezeigt wird, in der Sprache des angezeigten Textes.
    val stepTexts = translated?.steps?.takeIf { it.size == recipe.steps.size } ?: recipe.steps
    val currentIndex = recipe.steps.indices.firstOrNull { RecipeViewModel.progressKey(it, recipe.steps[it]) == cooking.currentStep }
    val speechLanguage = (translated?.language ?: shownLanguage ?: recipeLanguage).take(2)
    val speechState by speech.state.collectAsStateWithLifecycle()
    val speaking = speechState is SpeechState.Speaking
    // Kurzmeldungen stehen in der Kochleiste selbst, damit sie die Knöpfe nicht verdecken wie eine Snackbar.
    var barMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(barMessage) {
        if (barMessage != null) {
            delay(BAR_MESSAGE_MILLIS)
            barMessage = null
        }
    }
    val fallbackMessage = stringResource(R.string.speech_voice_fallback)
    LaunchedEffect(speechState) {
        if ((speechState as? SpeechState.Speaking)?.usedFallback == true) barMessage = fallbackMessage
    }
    val lastStepMessage = stringResource(R.string.cook_last_step)
    fun readStep(index: Int) {
        val sentences = SpokenText.sentences(stepTexts[index], speechLanguage)
        speech.speak(speechLanguage, listOf(SpokenText.stepLabel(speechLanguage, index + 1)) + sentences)
    }
    fun move(result: StepMove) {
        when (result) {
            is StepMove.To -> if (speaking) readStep(result.index)
            StepMove.AtEnd -> barMessage = lastStepMessage
            StepMove.None -> Unit
        }
    }
    // Ohne Markierung beginnt das Vorlesen beim ersten Schritt.
    fun startReading() {
        val index = currentIndex ?: (cooking.onNextStep() as? StepMove.To)?.index
        if (index != null) readStep(index)
    }

    // Kopfhörertasten (#79): erst aktiv, wenn in dieser Ansicht einmal vorgelesen wurde, damit Musik-Apps ihre Tasten
    // behalten. Weiter und Zurück lesen den Schritt gleich vor, denn wer die Tasten benutzt, schaut nicht aufs Handy.
    var headsetArmed by rememberSaveable { mutableStateOf(false) }
    if (speaking) headsetArmed = true
    fun perform(action: HeadsetAction) {
        when (action) {
            HeadsetAction.TOGGLE -> if (speaking) speech.stop() else startReading()
            HeadsetAction.NEXT -> when (val result = cooking.onNextStep()) {
                is StepMove.To -> readStep(result.index)
                StepMove.AtEnd -> barMessage = lastStepMessage
                StepMove.None -> Unit
            }
            HeadsetAction.PREVIOUS -> when (val result = cooking.onPreviousStep()) {
                is StepMove.To -> readStep(result.index)
                StepMove.AtEnd -> barMessage = lastStepMessage
                StepMove.None -> Unit
            }
            HeadsetAction.NONE -> Unit
        }
    }
    // Welche Geste hinter „Wiedergabe/Pause“, „Weiter“ und „Zurück“ steckt, entscheidet der Kopfhörer; die Belegung steht
    // in den Einstellungen. Wiedergabe und Pause sind für Android zwei Befehle, für die Belegung eine Taste.
    HeadsetEffect(
        enabled = headsetButtons && headsetArmed,
        speaking = speaking,
        onPlay = { perform(headsetMapping.playPause.let { if (it == HeadsetAction.TOGGLE && speaking) HeadsetAction.NONE else it }) },
        onPause = { perform(headsetMapping.playPause.let { if (it == HeadsetAction.TOGGLE && !speaking) HeadsetAction.NONE else it }) },
        onNext = { perform(headsetMapping.next) },
        onPrevious = { perform(headsetMapping.previous) },
    )

    // Spracheingabe (#80): Der Mikrofon-Knopf in der Kochleiste schaltet das Zuhören an und aus. Das Mikrofon hört dauerhaft
    // zu, bis es ausgeschaltet wird (Knopf oder Sprachbefehl), die Ansicht verlassen wird oder der Bildschirm ausgeht.
    // Beim Vorlesen über den Lautsprecher hört es das Vorlesen mit; deshalb gilt dann nur „Stopp“. Mit Kopfhörern gelten alle Befehle.
    val voiceState by voice.state.collectAsStateWithLifecycle()
    val listening = voiceState is VoiceState.Listening
    val micBusy = voiceState is VoiceState.Starting || listening
    val context = LocalContext.current
    fun onVoiceCommand(command: VoiceCommand) {
        // Über den Lautsprecher gilt beim Vorlesen nur „Stopp“; mit Kopfhörern hört das Mikrofon die Stimme nicht mit.
        if (speaking && command != VoiceCommand.STOP && !headphonesConnected(context)) return
        when (command) {
            VoiceCommand.NEXT -> perform(HeadsetAction.NEXT)
            VoiceCommand.PREVIOUS -> perform(HeadsetAction.PREVIOUS)
            VoiceCommand.REPEAT -> currentIndex?.let { readStep(it) } ?: startReading()
            VoiceCommand.READ -> startReading()
            VoiceCommand.STOP -> speech.stop()
            VoiceCommand.MICROPHONE_OFF -> voice.stop()
        }
    }
    val latestVoiceCommand = rememberUpdatedState({ command: VoiceCommand -> onVoiceCommand(command) })
    val voiceScope = rememberCoroutineScope()
    val microphoneDenied = stringResource(R.string.voice_permission_denied)
    val voiceNeedsPack = stringResource(R.string.voice_needs_pack)
    val voiceOpenSettings = stringResource(R.string.voice_open_settings)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voice.start { latestVoiceCommand.value(it) } else barMessage = microphoneDenied
    }
    fun toggleMicrophone() {
        when {
            micBusy -> voice.stop()
            !voice.isAvailable() -> voiceScope.launch {
                val result = snackbarHostState.showSnackbar(voiceNeedsPack, actionLabel = voiceOpenSettings)
                if (result == SnackbarResult.ActionPerformed) translation.onOpenSettings()
            }
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ->
                voice.start { latestVoiceCommand.value(it) }
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) voice.stop() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            voice.stop()
        }
    }
    val voiceErrorMicrophone = stringResource(R.string.voice_error_microphone)
    val voiceErrorMemory = stringResource(R.string.voice_error_memory)
    val voiceErrorOther = stringResource(R.string.voice_error_other)
    LaunchedEffect(voiceState) {
        val failed = voiceState as? VoiceState.Failed ?: return@LaunchedEffect
        barMessage = when (failed.problem) {
            VoiceProblem.NO_PACK -> voiceNeedsPack
            VoiceProblem.MICROPHONE -> voiceErrorMicrophone
            VoiceProblem.LOW_MEMORY -> voiceErrorMemory
            VoiceProblem.OTHER -> voiceErrorOther
        }
    }
    val voiceHint = when (voiceState) {
        VoiceState.Starting -> stringResource(R.string.voice_starting)
        VoiceState.Listening -> stringResource(R.string.voice_listening)
        else -> null
    }

    // Tablet (#91): Ab etwa 720 dp Breite stehen Zutaten und Zubereitung nebeneinander, jede Spalte blättert für sich.
    // Pfeiltasten (Tablet mit Tastatur, Chromebook): Auf/Ab blättern, Links/Rechts wechseln den Reiter.
    val keyFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { keyFocus.requestFocus() } }
    val wide = isWideLayout()
    var adjusting by rememberSaveable { mutableStateOf(false) }
    val videoLink = RecipeShareText.videoLink(recipe)
    val shownTabs = buildList {
        if (recipe.ingredients.isNotEmpty() && !wide) add(TAB_INGREDIENTS)
        if (recipe.steps.isNotEmpty()) add(TAB_STEPS)
        add(TAB_NOTES)
        if (recipe.source != null || videoLink != null || recipe.pageIds.isNotEmpty()) add(TAB_SOURCE)
    }
    var tabKey by rememberSaveable { mutableStateOf(shownTabs.first()) }
    val tab = if (tabKey in shownTabs) tabKey else shownTabs.first()
    var lastStep by rememberSaveable { mutableStateOf(cooking.currentStep) }
    LaunchedEffect(cooking.currentStep) {
        if (cooking.currentStep != lastStep) {
            lastStep = cooking.currentStep
            if (cooking.currentStep != null && TAB_STEPS in shownTabs) tabKey = TAB_STEPS
        }
    }
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    // Pfeiltasten: Auf/Ab blättert (auf dem Tablet die rechte Spalte), Links/Rechts wechselt den Reiter.
    val rightScroll = rememberScrollState()
    val keyScroll = if (wide) rightScroll else scrollState
    val keyScrollStep = with(LocalDensity.current) { 120.dp.toPx() }
    val swipe = Modifier.pointerInput(shownTabs, tab) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                val index = shownTabs.indexOf(tab)
                if (total <= -swipeThreshold && index < shownTabs.lastIndex) {
                    tabKey = shownTabs[index + 1]
                } else if (total >= swipeThreshold && index > 0) {
                    tabKey = shownTabs[index - 1]
                }
            },
            onDragCancel = { total = 0f },
            onHorizontalDrag = { _, delta -> total += delta },
        )
    }

    val tabRow: @Composable () -> Unit = {
        ScrollableTabRow(selectedTabIndex = shownTabs.indexOf(tab), edgePadding = 0.dp, containerColor = Color.Transparent) {
            shownTabs.forEach { key ->
                Tab(
                    selected = key == tab,
                    onClick = { tabKey = key },
                    text = {
                        Text(
                            stringResource(
                                when (key) {
                                    TAB_INGREDIENTS -> R.string.ingredients
                                    TAB_STEPS -> R.string.instructions
                                    TAB_SOURCE -> R.string.source
                                    else -> R.string.notes
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
    val headerContent: @Composable () -> Unit = {
        if (recipe.isDraft) DraftLabel()
        Text(
            text = localized(title.ifBlank { stringResource(R.string.untitled_recipe) }, textLocale),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (translated != null && recipe.title.isNotBlank() && recipe.title != title) {
            Text(
                text = stringResource(R.string.translation_original_title, recipe.title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs)) {
        AdjustRow(onAdjust = { adjusting = true }) {
        if (originalServings != null) {
            // Nach Backform bleibt es bei der Anzahl der Formen; nur die Mengen ändern sich (#67).
            val exact = if (cooking.adjustment is Adjustment.ByPan) originalServings.toDouble() else originalServings * factor
            val shown = exact.roundToInt().coerceIn(1, Adjustment.MAX_SERVINGS)
            ServingsControl(
                shown = shown,
                approximate = abs(exact - shown) >= SERVINGS_TOLERANCE,
                unit = servingsUnit,
                onChange = { cooking.onAdjust(if (it == originalServings) null else Adjustment.Servings(it)) },
            )
        } else if (servingsUnit != null) {
            // Eine Backform ohne Anzahl (#63), z. B. „Springform Ø 26 cm“.
            Text(
                text = localized(servingsUnit, textLocale),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
        run {
            AdjustActions(
                adjusted = factor != 1.0,
                hint = when (val current = cooking.adjustment) {
                    is Adjustment.ByIngredient -> stringResource(
                        R.string.adjust_ingredient_hint,
                        lines.getOrNull(current.index)?.joinToString("") { it.text }.orEmpty(),
                        texts.getOrNull(current.index).orEmpty(),
                    )
                    is Adjustment.ByPan -> stringResource(
                        R.string.adjust_pan_hint,
                        panLabel(current.to, current.inch),
                        panLabel(current.from, current.inch),
                    )
                    else -> originalServings?.let { stringResource(R.string.servings_scaled_hint, servingsText(it, servingsUnit)) }
                },
                bakingNote = cooking.adjustment is Adjustment.ByPan,
                onAdjust = { adjusting = true },
                onReset = { cooking.onAdjust(null) },
            )
        }
            }
        }
        if (adjusting) {
            AdjustDialog(
                servings = originalServings?.let { (it * factor).roundToInt().coerceIn(1, Adjustment.MAX_SERVINGS) },
                servingsUnit = servingsUnit,
                choices = choices,
                decimalSeparator = if (recipeLanguage == "en") '.' else ',',
                recipePan = remember(recipe.servingsUnit) { PanScaling.parse(recipe.servingsUnit) },
                onServings = {
                    adjusting = false
                    cooking.onAdjust(if (it == originalServings) null else Adjustment.Servings(it))
                },
                onIngredient = { choice, amount ->
                    adjusting = false
                    cooking.onAdjust(Adjustment.byIngredient(choice.index, choice.line, amount))
                },
                onPan = { from, to, inch ->
                    adjusting = false
                    cooking.onAdjust(Adjustment.ByPan(from, to, inch))
                },
                onDismiss = { adjusting = false },
            )
        }
        RecipeTimes(recipe.prepMinutes, recipe.totalMinutes)
        if (recipe.tags.isNotEmpty()) RecipeTags(recipe.tags)

        if (recipe.timeline.isNotEmpty()) {
            ScheduleCard(timeline = recipe.timeline, runs = runs, now = rememberNow(), actions = schedule)
        }
    }
    val ingredientsContent: @Composable () -> Unit = {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs)) {
        val amountWidth = rememberAmountWidth(
            lines = recipe.ingredients.mapIndexed { index, ingredient -> if (ingredient.isHeading) null else lines[index] },
            style = cookingTextStyle(),
        )
        recipe.ingredients.forEachIndexed { index, ingredient ->
            if (ingredient.isHeading) {
                Text(
                    text = localized(texts[index], textLocale),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                )
            } else {
                if (index > 0 && !recipe.ingredients[index - 1].isHeading) DashedDivider()
                IngredientRow(
                    parts = lines[index],
                    locale = textLocale,
                    amountWidth = amountWidth,
                    checked = RecipeViewModel.progressKey(index, ingredient.text) in cooking.checked,
                    onToggle = { cooking.onToggleIngredient(index, ingredient.text) },
                )
            }
        }
    }
            }
    }
    val stepsContent: @Composable () -> Unit = {
        val steps = translated?.steps?.takeIf { it.size == recipe.steps.size } ?: recipe.steps
        recipe.steps.forEachIndexed { index, step ->
            StepRow(
                number = index + 1,
                text = localized(steps[index], textLocale),
                current = cooking.currentStep == RecipeViewModel.progressKey(index, step),
                barInset = barInset,
                onClick = { cooking.onToggleStep(index, step) },
            )
        }
    }
    val otherContent: @Composable () -> Unit = {
        if (tab == TAB_NOTES) NotesSection(notes = recipe.notes, onSave = onSaveNotes)

        if (tab == TAB_SOURCE) recipe.source?.let { source ->
            SectionTitle(stringResource(R.string.source))
            val url = source.url
            if (url != null && RecipeText.isWebLink(url)) {
                SourceLink(url = url, snackbarHostState = snackbarHostState)
            } else {
                val resources = LocalResources.current
                val text = listOfNotNull(
                    source.name,
                    source.page?.let { page -> RecipeText.formatPage(page) { resources.getString(R.string.source_page, it) } },
                ).joinToString(", ")
                if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Der Link zum Video, wenn er nicht schon als Quelle dasteht (z. B. bei einem Rezept nur aus dem Video).
        if (tab == TAB_SOURCE) videoLink?.let { video ->
            SectionTitle(stringResource(R.string.video))
            if (RecipeText.isWebLink(video)) {
                SourceLink(url = video, snackbarHostState = snackbarHostState)
            } else {
                Text(video, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Originalseiten (#38), z. B. die Kochbuchseite oder ein handgeschriebenes Rezept zum Nachlesen.
        if (tab == TAB_SOURCE && recipe.pageIds.isNotEmpty()) {
            OutlinedButton(onClick = onShowPages, modifier = Modifier.padding(top = Spacing.m)) {
                Icon(painterResource(R.drawable.ic_document_scanner), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(pluralStringResource(R.plurals.original_pages_show, recipe.pageIds.size, recipe.pageIds.size))
            }
        }
    }
    val tabContent: @Composable () -> Unit = {
        if (tab == TAB_INGREDIENTS) ingredientsContent()
        if (tab == TAB_STEPS) stepsContent()
        otherContent()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(keyFocus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val index = shownTabs.indexOf(tab)
                when (event.key) {
                    Key.DirectionRight -> {
                        if (index < shownTabs.lastIndex) tabKey = shownTabs[index + 1]
                        true
                    }
                    Key.DirectionLeft -> {
                        if (index > 0) tabKey = shownTabs[index - 1]
                        true
                    }
                    Key.DirectionDown -> {
                        voiceScope.launch { keyScroll.animateScrollBy(keyScrollStep) }
                        true
                    }
                    Key.DirectionUp -> {
                        voiceScope.launch { keyScroll.animateScrollBy(-keyScrollStep) }
                        true
                    }
                    else -> false
                }
            },
    ) {
        // Der Übersetzungsbalken (Fortschritt, Hinweis, Fehler) steht fest unter der Leiste und wandert beim Blättern nicht
        // mit (Wunsch vom 14.10.2026).
        if (translationBarShown(recipe, translation.state) && originalLanguage != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s)) {
                    TranslationStatus(original = originalLanguage, actions = translation)
                }
            }
        }
        if (wide) {
            // Tablet (#91): links Titel, Menge und Zutaten, rechts Foto und Zubereitung; jede Spalte blättert für sich.
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(scrollState)) {
                    Column(
                        modifier = Modifier.padding(Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        headerContent()
                        if (recipe.ingredients.isNotEmpty()) {
                            SectionTitle(stringResource(R.string.ingredients))
                            ingredientsContent()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rightScroll)) {
                    if (photo != null) {
                        RecipePhotoLarge(
                            file = photo,
                            onOpen = onShowPhoto,
                            modifier = Modifier.padding(horizontal = Spacing.m).fillMaxWidth().clip(MaterialTheme.shapes.large),
                            // Handy quer: flacher zuschneiden, damit der erste Schritt ohne Scrollen sichtbar bleibt.
                            minRatio = if (isShortHeight()) COOKING_PHOTO_SHORT_MIN_RATIO else COOKING_PHOTO_MIN_RATIO,
                        )
                    }
                    Column(
                        modifier = Modifier.padding(Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) {
                        tabRow()
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).then(swipe),
                            verticalArrangement = Arrangement.spacedBy(Spacing.s),
                        ) { tabContent() }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState),
            ) {
                if (photo != null) {
                    // Beim Kochen soll Platz für Zutaten und Schritte bleiben: höchstens etwa 5:3 hoch, höhere Fotos werden zugeschnitten.
                    RecipePhotoLarge(file = photo, onOpen = onShowPhoto, modifier = Modifier.fillMaxWidth().clip(photoShape), minRatio = COOKING_PHOTO_MIN_RATIO)
                } else {
                    RecipePhoto(file = null, placeholderIconSize = 40.dp, modifier = Modifier.fillMaxWidth().height(120.dp).clip(photoShape))
                }
                Column(
                    modifier = Modifier.padding(Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    headerContent()
                    tabRow()
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).then(swipe),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    ) { tabContent() }
                }
            }
        }
        val hasBar = recipe.steps.isNotEmpty() || recipe.ingredients.any { !it.isHeading }
        val data = CookingBarData(
            hasSteps = recipe.steps.isNotEmpty(),
            speaking = speaking,
            stepNumber = currentIndex?.plus(1),
            stepCount = recipe.steps.size,
            message = barMessage,
            voiceHint = voiceHint,
            microphoneOn = micBusy,
            onMicrophone = { toggleMicrophone() },
            onPrevious = { move(cooking.onPreviousStep()) },
            onRead = {
                if (speaking) speech.stop() else startReading()
            },
            onNext = { move(cooking.onNextStep()) },
        )
        // Handy quer (niedriges Fenster): Die Kochleiste steht oben in der Leiste neben dem Namen der App und lässt
        // Zutaten und Zubereitung mehr Platz. Bei großer Schrift bleibt sie unten, dort ist mehr Raum für die Texte.
        val inTopBar = hasBar && topBarHolder != null && isShortHeight() && !isLargeText()
        SideEffect { topBarHolder?.data = if (inTopBar) data else null }
        DisposableEffect(topBarHolder) { onDispose { topBarHolder?.data = null } }
        if (hasBar && !inTopBar) CookingBar(data)
    }
}

/** Kleinstes Seitenverhältnis (Breite zu Höhe) des Fotos in der Rezeptansicht: nie höher als 5:3. */
internal const val COOKING_PHOTO_MIN_RATIO = 2f
internal const val COOKING_PHOTO_SHORT_MIN_RATIO = 3.2f
internal const val TAB_INGREDIENTS = 0
internal const val TAB_STEPS = 1
internal const val TAB_NOTES = 2
internal const val TAB_SOURCE = 3
