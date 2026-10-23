package com.moltobene.app.ui.recipe

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.net.Uri
import android.os.SystemClock
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
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
import com.moltobene.app.data.Pan
import com.moltobene.app.data.PanScaling
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeText
import com.moltobene.app.data.ScreenOn
import com.moltobene.app.data.ScreenOnTimer
import com.moltobene.app.data.Tag
import com.moltobene.app.data.ocr.TextLanguage
import com.moltobene.app.data.share.PreparedShare
import com.moltobene.app.data.share.RecipeShareText
import com.moltobene.app.data.share.ShareLabels
import com.moltobene.app.data.speech.HeadsetAction
import com.moltobene.app.data.speech.HeadsetMapping
import com.moltobene.app.data.speech.SpeechOutput
import com.moltobene.app.data.speech.SpeechState
import com.moltobene.app.data.speech.SpokenText
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
import java.util.Locale
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
    cookingLarge: Boolean = false,
    onCookingLargeChange: (Boolean) -> Unit = {},
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
                    title = { AppTitle(style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        val content = state as? RecipeUiState.Content
                        if (content != null && !deleting) {
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
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (content.recipe.favorite) R.string.favorite_remove else R.string.favorite_add,
                                                ),
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                                        onClick = {
                                            menuOpen = false
                                            viewModel.toggleFavorite()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.share_recipe)) },
                                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                        enabled = !viewModel.preparingShare,
                                        onClick = {
                                            menuOpen = false
                                            viewModel.share(asFile = false, labels = shareLabels(resources))
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.share_as_file)) },
                                        enabled = !viewModel.preparingShare,
                                        onClick = {
                                            menuOpen = false
                                            viewModel.share(asFile = true, labels = shareLabels(resources))
                                        },
                                    )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.cooking_large_text)) },
                                    leadingIcon = { if (cookingLarge) Icon(Icons.Filled.Check, contentDescription = null) },
                                    modifier = Modifier.semantics { selected = cookingLarge },
                                    onClick = {
                                        menuOpen = false
                                        onCookingLargeChange(!cookingLarge)
                                    },
                                )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.cooking_reset)) },
                                        enabled = checked.isNotEmpty() || currentStep != null,
                                        onClick = {
                                            menuOpen = false
                                            viewModel.resetProgress()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_recipe)) },
                                        onClick = {
                                            menuOpen = false
                                            confirmDelete = true
                                        },
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
            current is RecipeUiState.Content -> CompositionLocalProvider(LocalCookingLarge provides cookingLarge) { RecipeContent(
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

private fun shareLabels(resources: Resources) = ShareLabels(
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
private fun openShareMenu(context: Context, share: PreparedShare, chooserTitle: String): Boolean {
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
private fun Modifier.observeTouches(onTouch: () -> Unit): Modifier = pointerInput(onTouch) {
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
private fun keepScreenOn(setting: ScreenOn): () -> Unit {
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
private class CookingState(
    val checked: Set<String>,
    val currentStep: String?,
    val adjustment: Adjustment?,
    val onToggleIngredient: (Int, String) -> Unit,
    val onToggleStep: (Int, String) -> Unit,
    val onAdjust: (Adjustment?) -> Unit,
    val onPreviousStep: () -> StepMove,
    val onNextStep: () -> StepMove,
)

/** „Rezept übersetzen“ (#60): Zustand und Aktionen für den Umschalter „DE | EN“. */
private class TranslationActions(
    val state: TranslationState,
    val onShowLanguage: (language: String?, original: String?) -> Unit,
    val onRetry: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
private fun RecipeContent(
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
    // Beim Vorlesen hört es das Vorlesen mit; deshalb gilt dann nur „Stopp“.
    val voiceState by voice.state.collectAsStateWithLifecycle()
    val listening = voiceState is VoiceState.Listening
    val micBusy = voiceState is VoiceState.Starting || listening
    fun onVoiceCommand(command: VoiceCommand) {
        if (speaking && command != VoiceCommand.STOP) return
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
    val context = LocalContext.current
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
    // Pfeiltasten (Tablet mit Tastatur, Chromebook) gehen zum nächsten und vorigen Schritt.
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
                when (event.key) {
                    Key.DirectionRight -> move(cooking.onNextStep()).let { true }
                    Key.DirectionLeft -> move(cooking.onPreviousStep()).let { true }
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
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                    if (photo != null) {
                        RecipePhotoLarge(
                            file = photo,
                            onOpen = onShowPhoto,
                            modifier = Modifier.padding(horizontal = Spacing.m).fillMaxWidth().clip(MaterialTheme.shapes.large),
                            minRatio = COOKING_PHOTO_MIN_RATIO,
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
        if (recipe.steps.isNotEmpty() || recipe.ingredients.any { !it.isHeading }) {
            CookingBar(
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
        }
    }
}

/**
 * Portionen mit − und +; umgerechnet wird nur in der Anzeige (#56). Nach einer Zutat umgerechnet (#66) können es
 * krumme Portionen sein: Dann steht „etwa“ davor, und − und + gehen von der gerundeten Zahl aus.
 */
@Composable
private fun ServingsControl(shown: Int, approximate: Boolean, unit: String?, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(shown - 1) }, enabled = shown > 1) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.servings_fewer))
        }
        val text = servingsControlText(shown, unit)
        Text(
            text = if (approximate) stringResource(R.string.amount_about, text) else text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Der Screenreader sagt die neue Zahl an.
            modifier = Modifier.weight(1f, fill = false).semantics { liveRegion = LiveRegionMode.Polite },
        )
        IconButton(onClick = { onChange(shown + 1) }, enabled = shown < Adjustment.MAX_SERVINGS) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.servings_more))
        }
    }
}

/**
 * „Umrechnen …“ (#66) und, wenn umgerechnet ist, der Hinweis mit „Zurücksetzen“. Bei großer Schrift dürfen die beiden
 * Schaltflächen untereinander stehen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdjustActions(adjusted: Boolean, hint: String?, bakingNote: Boolean, onAdjust: () -> Unit, onReset: () -> Unit) {
    if (adjusted && hint != null) {
        Text(
            text = hint + " " + stringResource(if (bakingNote) R.string.adjust_pan_baking else R.string.adjust_hint_rules),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (adjusted) TextButton(onClick = onReset) { Text(stringResource(R.string.servings_reset)) }
    }
}

/** Zwischen − und +: „4 Portionen“ oder mit eigener Einheit „Springform (Ø 26 cm): 2“ statt „2 Springform“ (#63). */
@Composable
private fun servingsControlText(servings: Int, unit: String?): String =
    if (unit == null) {
        pluralStringResource(R.plurals.servings_count, servings, servings)
    } else {
        stringResource(R.string.servings_unit_count, unit, servings)
    }

/** Als Satzteil, z. B. im Hinweis „Im Rezept: 1 Springform (Ø 26 cm)“. */
@Composable
private fun servingsText(servings: Int, unit: String?): String =
    if (unit == null) {
        pluralStringResource(R.plurals.servings_count, servings, servings)
    } else {
        stringResource(R.string.servings_with_unit, servings, unit)
    }

/** Dünne gestrichelte Trennlinie zwischen zwei Zutaten. */
@Composable
private fun DashedDivider() {
    val color = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color,
                    androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                    androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                )
            },
    )
}

/**
 * Eine Zutat zum Abhaken: Die ganze Zeile ist antippbar, abgehakt heißt Häkchen und durchgestrichen.
 * Umgerechnete Mengen sind fett, damit sie nicht nur an der Farbe zu erkennen sind.
 */
@Composable
private fun IngredientRow(parts: List<AmountScaling.Part>, locale: LocaleList?, checked: Boolean, onToggle: () -> Unit) {
    val state = stringResource(if (checked) R.string.ingredient_checked else R.string.ingredient_unchecked)
    val resources = LocalResources.current
    // Der Screenreader liest „etwa 335 g Mehl, umgerechnet“ (#65); die Zutat behält dabei die Sprache des Rezepts.
    val spoken = remember(parts, locale, resources) {
        if (parts.none { it.scaled }) return@remember null
        val line = AmountScaling.spoken(parts) { resources.getString(R.string.amount_about, it) }
        val full = resources.getString(R.string.ingredient_adjusted, line)
        buildAnnotatedString {
            append(full)
            val start = full.indexOf(line)
            if (start >= 0) addStyle(SpanStyle(localeList = locale), start, start + line.length)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { stateDescription = state },
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Box(modifier = if (spoken != null) Modifier.clearAndSetSemantics { text = spoken } else Modifier) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(localeList = locale)) {
                        parts.forEach { part ->
                            if (part.scaled) {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part.text) }
                            } else {
                                append(part.text)
                            }
                        }
                    }
                },
                style = cookingTextStyle().copy(
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                ),
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Ein Schritt; antippen markiert ihn als aktuellen Schritt – mit Symbol und Hintergrund, nicht nur über die Farbe. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepRow(number: Int, text: AnnotatedString, current: Boolean, barInset: Dp, onClick: () -> Unit) {
    // „Weiter“ und „Zurück“ (#79) scrollen den aktuellen Schritt in den sichtbaren Bereich.
    val requester = remember { BringIntoViewRequester() }
    // Liegt die Leiste über dem Inhalt, bleibt Platz in Höhe der Leiste frei, damit der Schritt nicht darunter landet.
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val insetPx = with(LocalDensity.current) { barInset.toPx() }
    LaunchedEffect(current) {
        if (current) requester.bringIntoView(Rect(0f, -insetPx, rowSize.width.toFloat(), rowSize.height.toFloat()))
    }
    val currentLabel = stringResource(R.string.step_current)
    // Ein kräftiger Balken links zeigt den aktuellen Schritt auch ohne Farbe: zusätzlich zu Symbol, Hintergrund und Text.
    val barColor = MaterialTheme.colorScheme.primary
    val accent = if (current) Modifier.drawBehind { drawRect(barColor, size = Size(4.dp.toPx(), size.height)) } else Modifier
    val markLabel = stringResource(R.string.step_mark_current)
    val highlight = if (current) {
        Modifier.background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
    } else {
        Modifier
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
            .onSizeChanged { rowSize = it }
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(highlight)
            .then(accent)
            .clickable(onClickLabel = markLabel, onClick = onClick)
            .padding(Spacing.s)
            .semantics(mergeDescendants = true) { if (current) stateDescription = currentLabel },
    ) {
        val color = if (current) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        if (current) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = "$number.",
            style = cookingTextStyle(current),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 24.dp),
        )
        Text(text, style = cookingTextStyle(current), color = color)
    }
}

/**
 * Kochleiste (#79, #89): „Zurück“ und „Weiter“ nebeneinander – „Weiter“ ist der häufigste Knopf, deshalb betont –, davon
 * getrennt ganz rechts das Mikrofon (Spracheingabe, #80) und „Vorlesen“ als Lautsprecher-Symbol (beim Vorlesen als „Anhalten“-Symbol, Wunsch vom 14.10.2026). Darüber steht, welcher Schritt gerade dran ist
 * („Schritt 2 von 5“, als Text, nicht nur Farbe). Mindestens 64 dp hoch. Bei großer Schrift stehen „Zurück“ und „Weiter“
 * oben, „Vorlesen“ darunter rechts, damit kein Wort mitten im Wort umbricht. Das Vorlesen startet nur auf Antippen.
 */
@Composable
private fun CookingBar(
    hasSteps: Boolean,
    speaking: Boolean,
    stepNumber: Int?,
    stepCount: Int,
    message: String?,
    voiceHint: String?,
    microphoneOn: Boolean,
    onMicrophone: () -> Unit,
    onPrevious: () -> Unit,
    onRead: () -> Unit,
    onNext: () -> Unit,
) {
    val previous = stringResource(R.string.cook_bar_previous)
    val previousDescription = stringResource(R.string.cook_bar_previous_description)
    val readIcon = if (speaking) R.drawable.ic_stop else R.drawable.ic_volume_up
    val readDescription = stringResource(if (speaking) R.string.cook_bar_stop_description else R.string.cook_bar_read_description)
    val next = stringResource(R.string.cook_bar_next)
    val nextDescription = stringResource(R.string.cook_bar_next_description)
    val microphoneIcon = if (microphoneOn) R.drawable.ic_mic_off else R.drawable.ic_mic
    val microphoneDescription = stringResource(if (microphoneOn) R.string.cook_bar_mic_off_description else R.string.cook_bar_mic_on_description)
    val compact = isLargeText()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            val status = message ?: stepNumber?.let { stringResource(R.string.cook_step_of, it, stepCount) }
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    textAlign = TextAlign.Center,
                )
            }
            if (voiceHint != null) {
                Text(
                    text = voiceHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    textAlign = TextAlign.Center,
                )
            }
            if (compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    BarButton(previous, previousDescription, hasSteps, onPrevious)
                    BarButton(next, nextDescription, hasSteps, onNext, primary = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Spacer(Modifier.weight(1f))
                    BarIconButton(microphoneIcon, microphoneDescription, true, onMicrophone, active = microphoneOn)
                    BarIconButton(readIcon, readDescription, hasSteps, onRead)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    BarButton(previous, previousDescription, hasSteps, onPrevious)
                    BarButton(next, nextDescription, hasSteps, onNext, primary = true)
                    // Lücke, damit „Vorlesen“ getrennt von den beiden Schritt-Knöpfen ganz rechts steht.
                    Spacer(Modifier.width(Spacing.m))
                    BarIconButton(microphoneIcon, microphoneDescription, true, onMicrophone, active = microphoneOn)
                    BarIconButton(readIcon, readDescription, hasSteps, onRead)
                }
            }
        }
    }
}

@Composable
private fun RowScope.BarButton(text: String, description: String, enabled: Boolean, onClick: () -> Unit, primary: Boolean = false) {
    val modifier = Modifier
        .weight(1f)
        .heightIn(min = 64.dp)
        .semantics { contentDescription = description }
    val padding = PaddingValues(horizontal = Spacing.xs, vertical = Spacing.xs)
    if (primary) {
        Button(onClick = onClick, enabled = enabled, modifier = modifier, contentPadding = padding) {
            Text(text, textAlign = TextAlign.Center)
        }
    } else {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier, contentPadding = padding) {
            Text(text, textAlign = TextAlign.Center)
        }
    }
}

/** Knopf nur mit Symbol (z. B. „Vorlesen“ als Lautsprecher); die Beschreibung für den Screenreader steht in strings.xml. */
@Composable
private fun BarIconButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit, active: Boolean = false) {
    val modifier = Modifier.width(64.dp).heightIn(min = 64.dp).semantics { contentDescription = description }
    val content: @Composable () -> Unit = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp)) }
    if (active) {
        // Eingeschaltet (z. B. das Mikrofon hört zu): betont und mit anderem Symbol, nicht nur durch die Farbe.
        Button(onClick = onClick, enabled = enabled, modifier = modifier, contentPadding = PaddingValues(Spacing.xs)) { content() }
    } else {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier, contentPadding = PaddingValues(Spacing.xs)) { content() }
    }
}

/** So lange steht eine Kurzmeldung („Letzter Schritt“) in der Kochleiste. */
private const val BAR_MESSAGE_MILLIS = 3_000L

/** Ab dieser Abweichung von einer ganzen Zahl steht „etwa“ vor den Portionen. */
private const val SERVINGS_TOLERANCE = 0.01

/** Text in der Sprache des Rezepts: Der Screenreader liest ihn mit der passenden Aussprache vor (#60). */
private fun localized(text: String, locale: LocaleList?): AnnotatedString =
    if (locale == null) AnnotatedString(text) else buildAnnotatedString { withStyle(SpanStyle(localeList = locale)) { append(text) } }

/** Name einer Sprache in der Sprache der Oberfläche, z. B. „Englisch“. */
@Composable
private fun languageName(code: String): String {
    val ui = LocalConfiguration.current.locales[0]
    return Locale.forLanguageTag(code).getDisplayLanguage(ui).replaceFirstChar { it.titlecase(ui) }
}

/** Die gezeigte bzw. gewünschte Sprache: das Original, eine Übersetzung oder eine, die gerade entsteht. */
private fun TranslationState.selectedLanguage(original: String): String = when (this) {
    TranslationState.Original -> original
    is TranslationState.Working -> language
    is TranslationState.Shown -> translation.language
    is TranslationState.NeedsPack -> language
    is TranslationState.Failed -> language
}

/**
 * Umschalter „DE | EN“ oben in der Leiste (#60): Abkürzungen statt Flaggen, die Originalsprache mit Sternchen markiert,
 * z. B. „DE*“ (Wunsch vom 08.10.2026). Die gewählte Sprache ist ausgefüllt und fett, nicht nur farbig; der Screenreader liest
 * den Namen der Sprache. Schmal gehalten, damit daneben „MOLTOBENE“, Bearbeiten und ⋮ Platz haben.
 */
@Composable
private fun LanguageSwitch(original: String, selected: String, onSelect: (String) -> Unit) {
    val languages = (listOf(original) + LanguagePack.targets(original)).sorted()
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.selectableGroup(),
    ) {
        languages.forEach { code ->
            val isSelected = code == selected
            val name = languageName(code)
            val description = if (code == original) stringResource(R.string.translation_language_original, name) else name
            val shape = MaterialTheme.shapes.small
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(code) })
                    .semantics { contentDescription = description }
                    .widthIn(min = 40.dp)
                    .then(
                        if (isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer, shape)
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                        },
                    )
                    .padding(horizontal = Spacing.s, vertical = Spacing.xs),
            ) {
                Text(
                    text = if (code == original) {
                        stringResource(R.string.translation_code_original, code.uppercase(Locale.ROOT))
                    } else {
                        code.uppercase(Locale.ROOT)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Fest unter der Leiste: Fortschritt, Hinweis auf die maschinelle Übersetzung oder was zu tun ist (#60). */
@Composable
private fun TranslationStatus(original: String, actions: TranslationActions) {
    val state = actions.state
    when (state) {
        TranslationState.Original -> Unit
        is TranslationState.Working -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                Text(
                    text = stringResource(R.string.translation_running, state.done, state.total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { actions.onShowLanguage(null, original) }) { Text(stringResource(R.string.cancel)) }
            }
            LinearProgressIndicator(
                progress = { if (state.total > 0) state.done.toFloat() / state.total else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is TranslationState.Shown -> Text(
            text = stringResource(R.string.translation_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        is TranslationState.NeedsPack -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            val context = LocalContext.current
            Text(
                text = stringResource(R.string.translation_needs_pack, Formatter.formatShortFileSize(context, LanguagePack.DOWNLOAD_BYTES)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            OutlinedButton(onClick = actions.onOpenSettings) { Text(stringResource(R.string.translation_open_settings)) }
        }
        is TranslationState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(if (state.lowMemory) R.string.translation_low_memory else R.string.translation_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            OutlinedButton(onClick = actions.onRetry) { Text(stringResource(R.string.translation_retry)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = if (LocalCookingLarge.current) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .padding(top = Spacing.m)
            .semantics { heading() },
    )
}

/** Öffnet nur Links mit http/https im Browser. */
@Composable
private fun SourceLink(url: String, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val errorText = stringResource(R.string.source_link_error)
    TextButton(
        onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(errorText) }
            }
        },
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(url) }
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Zeiten aus einer Übernahme (#55), im Format des Handys, z. B. „Gesamtzeit: 1 Std., 30 Min.“. */
@Composable
private fun RecipeTimes(prepMinutes: Int?, totalMinutes: Int?) {
    val locale = LocalConfiguration.current.locales[0]
    listOfNotNull(
        prepMinutes?.takeIf { it > 0 }?.let { R.string.recipe_time_prep to it },
        totalMinutes?.takeIf { it > 0 }?.let { R.string.recipe_time_total to it },
    ).forEach { (label, minutes) ->
        Text(
            text = stringResource(label, formatDuration(minutes, locale)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Dauer in Stunden und Minuten mit den Abkürzungen der Sprache, z. B. „1 Std., 30 Min.“ oder “1 hr, 30 min”. */
internal fun formatDuration(minutes: Int, locale: Locale): String {
    val hours = minutes / 60
    val rest = minutes % 60
    val parts = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        if (rest > 0 || hours == 0) add(Measure(rest, MeasureUnit.MINUTE))
    }
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT).formatMeasures(*parts.toTypedArray())
}

/** Eine Backform als Text, z. B. „Ø 28 cm“ oder „30 × 40 cm“, bei Eingabe in Zoll mit „(9 Zoll)“ (#67). */
@Composable
private fun panLabel(pan: Pan, inch: Boolean): String {
    fun cm(value: Double) = if (abs(value - value.roundToInt()) < 0.05) value.roundToInt().toString() else "%.1f".format(java.util.Locale.ROOT, value)
    fun inches(value: Double) = (value / PanScaling.CM_PER_INCH).let { if (abs(it - it.roundToInt()) < 0.05) it.roundToInt().toString() else "%.1f".format(java.util.Locale.ROOT, it) }
    val base = if (pan.round) {
        stringResource(R.string.pan_round, cm(pan.a))
    } else {
        stringResource(R.string.pan_rectangle, cm(pan.a), cm(pan.b))
    }
    if (!inch) return base
    val size = if (pan.round) inches(pan.a) else stringResource(R.string.pan_size_pair, inches(pan.a), inches(pan.b))
    return stringResource(R.string.pan_with_inch, base, stringResource(R.string.pan_inch, size))
}

/** „Umrechnen …“ neben den Portionen in derselben Zeile; bei großer Schrift darunter. */
@Composable
private fun AdjustRow(onAdjust: () -> Unit, content: @Composable () -> Unit) {
    val button = @Composable { TextButton(onClick = onAdjust) { Text(stringResource(R.string.adjust_action)) } }
    if (isLargeText()) {
        Column {
            content()
            button()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) { content() }
            button()
        }
    }
}

/** Die Schlagwörter des Rezepts (#57) als ruhige Marken unter den Zeiten; mit Text, nicht nur mit Farbe. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeTags(tags: List<Tag>) {
    val resources = LocalResources.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (tag in tags) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    text = TagLabels.text(resources, tag),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
                )
            }
        }
    }
}

/** Kleinstes Seitenverhältnis (Breite zu Höhe) des Fotos in der Rezeptansicht: nie höher als 5:3. */
private const val COOKING_PHOTO_MIN_RATIO = 2f
private const val TAB_INGREDIENTS = 0
private const val TAB_STEPS = 1
private const val TAB_NOTES = 2
private const val TAB_SOURCE = 3

/** Ob unter der Leiste der Übersetzungsbalken steht (Rezept mit möglicher Übersetzung, die gerade nicht das Original zeigt). */
private fun translationBarShown(recipe: Recipe, state: TranslationState): Boolean {
    val original = RecipeTranslations.languageOf(recipe)
    return original != null && LanguagePack.targets(original).isNotEmpty() && state != TranslationState.Original
}

/** Deckkraft des Verlaufs oben auf dem Foto, damit Titel und Symbole der durchsichtigen Leiste lesbar bleiben. */
private const val BAR_GRADIENT_ALPHA = 0.9f
private val BAR_HEIGHT = 64.dp

/** „Große Schrift beim Kochen“ (#61): Zutaten, Schritte und Notizen größer, unabhängig von der Schriftgröße des Handys. */
private val LocalCookingLarge = compositionLocalOf { false }

/**
 * Schrift für Zutaten, Schritte und Notizen: normal in der Rezeptansicht, nur der aktuelle Schritt (Einzelschritt beim
 * Kochen) ist groß, noch größer mit „Große Schrift beim Kochen“.
 */
@Composable
private fun cookingTextStyle(current: Boolean = false): TextStyle = when {
    !current -> MaterialTheme.typography.bodyLarge
    LocalCookingLarge.current -> MaterialTheme.typography.headlineMedium
    else -> MaterialTheme.typography.titleLarge
}

/**
 * Notizen zum Rezept (#61) mit „Notiz hinzufügen“ bzw. „Notiz bearbeiten“ direkt in der Rezeptansicht. Die angefangene
 * Notiz übersteht das Drehen des Handys und das Beenden der App durch Android (`rememberSaveable`).
 */
@Composable
private fun NotesSection(notes: String, onSave: (String) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    if (notes.isNotBlank()) {
        SectionTitle(stringResource(R.string.notes))
        Text(notes, style = cookingTextStyle())
    }
    OutlinedButton(
        onClick = {
            draft = notes
            editing = true
        },
        modifier = Modifier.padding(top = if (notes.isBlank()) Spacing.m else 0.dp),
    ) {
        Text(stringResource(if (notes.isBlank()) R.string.note_add else R.string.note_edit))
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.notes)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(stringResource(R.string.notes)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(draft.trim())
                    editing = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
