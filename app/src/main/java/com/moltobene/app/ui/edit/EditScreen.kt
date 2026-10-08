package com.moltobene.app.ui.edit

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.moltobene.app.R
import com.moltobene.app.data.TextLinks
import com.moltobene.app.data.ocr.TextLanguage
import com.moltobene.app.data.share.IncomingText
import com.moltobene.app.data.web.WebAddress
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.components.OptionButton
import com.moltobene.app.ui.components.PageViewer
import com.moltobene.app.ui.components.RecipePhotoLarge
import com.moltobene.app.ui.components.ScreenTitle
import com.moltobene.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Dateitypen für „Aus Datei übernehmen“: Rezeptdateien (schema.org), gespeicherte Rezeptseiten und Text.
 * Manche Speicherorte kennen den Typ einer .json-Datei nicht; ob ein Rezept darin steht, zeigt erst der Inhalt.
 */
private val RECIPE_FILE_TYPES = arrayOf(
    "application/json", "application/ld+json", "text/html", "text/plain", "application/octet-stream",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: EditViewModel,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onSaved: (id: String, wasNew: Boolean, message: String) -> Unit,
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val savedMessage = stringResource(R.string.recipe_saved)
    val onRecipeSaved: (String, Boolean) -> Unit = { id, wasNew -> onSaved(id, wasNew, savedMessage) }
    val cameraMissing = stringResource(R.string.camera_unavailable)
    val fileChooserMissing = stringResource(R.string.file_chooser_unavailable)
    val scope = rememberCoroutineScope()

    val requestClose: () -> Unit = {
        if (viewModel.isDirty || viewModel.recognitionInterrupted) {
            confirmDiscard = true
        } else {
            onClose()
        }
    }
    BackHandler { requestClose() }

    // Geht die App in den Hintergrund (Anruf, anderer Bildschirm), wird ein neues Rezept als Entwurf gesichert.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStop() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onStart() }

    viewModel.message?.let { messageRes ->
        val text = if (messageRes == R.string.share_too_many) stringResource(messageRes, MAX_PAGES) else stringResource(messageRes)
        LaunchedEffect(messageRes, text) {
            // Die Meldungen hier sagen auch, was man tun kann – deshalb länger sichtbar.
            snackbarHostState.showSnackbar(text, duration = SnackbarDuration.Long)
            viewModel.message = null
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCameraResult(success)
    }

    // Texterkennung: Seiten auswählen oder fotografieren; sie sammeln sich in der Seitenübersicht.
    var showRecognitionDialog by rememberSaveable { mutableStateOf(viewModel.takeStartPrompt()) }
    // „Aus Text übernehmen“: Text aus der Zwischenablage einfügen oder selbst eintragen.
    var showTextDialog by rememberSaveable { mutableStateOf(viewModel.takeTextPrompt()) }
    // „Aus Link übernehmen“: Link einfügen; die Seite wird dann geladen (#55).
    var showLinkDialog by rememberSaveable { mutableStateOf(viewModel.takeLinkPrompt()) }
    // „Aus Datei übernehmen“ (#54): Rezeptdatei oder gespeicherte Rezeptseite in der Dateiauswahl von Android wählen.
    val openRecipeFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFile(uri)
    }
    LaunchedEffect(Unit) {
        if (viewModel.takeFilePrompt()) {
            try {
                openRecipeFile.launch(RECIPE_FILE_TYPES)
            } catch (e: ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(fileChooserMissing) }
            }
        }
    }
    val pickPages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PAGES),
    ) { uris -> viewModel.recognizePhotos(uris) }
    val takePage = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onRecognitionCameraResult(success)
    }
    // Fotoserie ohne Unterbrechung: Nach jedem Foto öffnet sich die Kamera gleich wieder für die nächste Seite.
    if (viewModel.openCameraAgain) {
        LaunchedEffect(Unit) {
            viewModel.cameraReopened()
            try {
                takePage.launch(viewModel.cameraUri())
            } catch (e: ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(cameraMissing) }
            }
        }
    }
    val busy = viewModel.isProcessingPhoto || viewModel.isRecognizing || viewModel.isImporting

    // Vor der Texterkennung: Seiten sammeln (Fotoserie) und am Ende alle zusammen lesen.
    viewModel.pageOverview?.let { overview ->
        PagesScreen(
            overview = overview,
            thumbnails = viewModel.pageThumbnails,
            failed = viewModel.failedThumbnails,
            adding = viewModel.isProcessingPhoto,
            snackbarHostState = snackbarHostState,
            language = viewModel.languageChoice,
            onLanguageChange = viewModel::chooseLanguage,
            onTakePage = {
                try {
                    takePage.launch(viewModel.cameraUri())
                } catch (e: ActivityNotFoundException) {
                    scope.launch { snackbarHostState.showSnackbar(cameraMissing) }
                }
            },
            onChoosePages = {
                pickPages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onChooseArea = viewModel::editArea,
            onRemovePage = viewModel::removePage,
            onRecognize = viewModel::recognizePages,
            onDiscard = viewModel::cancelPages,
        )
        return
    }

    // Bei Bedarf: Bereich einer Seite wählen, der gelesen wird; danach zurück zur Seitenübersicht.
    viewModel.areaSelection?.let { selection ->
        AreaSelectionScreen(
            selection = selection,
            preview = viewModel.areaPreview,
            failed = viewModel.areaPreviewFailed,
            snackbarHostState = snackbarHostState,
            onFrameChange = viewModel::changeFrameArea,
            onSelectFrame = viewModel::selectFrame,
            onKindChange = viewModel::changeFrameKind,
            onAddFrame = viewModel::addFrame,
            onRemoveFrame = viewModel::removeFrame,
            onWholePage = viewModel::resetArea,
            onDone = viewModel::closeAreaSelection,
        )
        return
    }

    // „Seiten ansehen“: gelesene Seiten und Originalseiten als Vollbild.
    viewModel.viewer.page?.let { page ->
        PageViewer(
            page = page,
            pageCount = viewModel.pageCount,
            bitmap = viewModel.viewer.bitmap,
            failed = viewModel.viewer.failed,
            onPageChange = viewModel::showPage,
            onClose = viewModel.viewer::close,
        ) {
            if (viewModel.canChangeViewerPage) {
                TextButton(onClick = viewModel::useViewerPageAsPhoto, enabled = !busy) {
                    Text(stringResource(R.string.page_use_as_photo))
                }
                TextButton(onClick = viewModel::removeViewerPage) { Text(stringResource(R.string.page_remove)) }
            }
        }
        return
    }

    // Das Rezeptfoto ganz und zum Vergrößern.
    viewModel.photoViewer.page?.let {
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
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppTitle(style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    if (viewModel.isSaving) {
                        // Das Speichern der Originalseiten kann auf günstigen Handys einige Sekunden dauern.
                        CircularProgressIndicator(modifier = Modifier.padding(horizontal = Spacing.m).size(24.dp))
                    } else {
                        TextButton(
                            onClick = { viewModel.requestSave(onRecipeSaved) },
                            enabled = !viewModel.isLoading && !busy,
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (viewModel.isLoading) {
            CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                ScreenTitle(stringResource(if (viewModel.isNew) R.string.add_recipe else R.string.edit_recipe))
                PhotoSection(
                    viewModel = viewModel,
                    onChoose = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onTake = {
                        try {
                            takePhoto.launch(viewModel.cameraUri())
                        } catch (e: ActivityNotFoundException) {
                            scope.launch { snackbarHostState.showSnackbar(cameraMissing) }
                        }
                    },
                )

                RecognitionSection(
                    viewModel = viewModel,
                    enabled = !busy,
                    onStart = { showRecognitionDialog = true },
                    onImportText = { showTextDialog = true },
                    onImportLink = { showLinkDialog = true },
                )

                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    isError = viewModel.titleError,
                    supportingText = if (viewModel.titleError) {
                        { Text(stringResource(R.string.field_title_missing)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    OutlinedTextField(
                        value = viewModel.servings,
                        onValueChange = { value ->
                            if (value.length <= 3 && value.all { it.isDigit() }) viewModel.servings = value
                        },
                        label = { Text(stringResource(R.string.servings)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = viewModel.servingsUnit,
                        onValueChange = { viewModel.servingsUnit = it },
                        label = { Text(stringResource(R.string.field_servings_unit)) },
                        placeholder = { Text(stringResource(R.string.field_servings_unit_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                }

                OutlinedTextField(
                    value = viewModel.ingredients,
                    onValueChange = { viewModel.ingredients = it },
                    label = { Text(stringResource(R.string.ingredients)) },
                    supportingText = { Text(stringResource(R.string.field_ingredients_hint)) },
                    minLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = viewModel.steps,
                    onValueChange = { viewModel.steps = it },
                    label = { Text(stringResource(R.string.instructions)) },
                    supportingText = { Text(stringResource(R.string.field_instructions_hint)) },
                    minLines = 6,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                SourceSection(viewModel)

                RecipeLanguageField(language = viewModel.language, onChange = viewModel::changeLanguage)

                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = { viewModel.notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.size(Spacing.xl))
            }
        }
    }

    if (showRecognitionDialog) {
        RecognitionDialog(
            title = stringResource(R.string.import_from_photo),
            hasRecipePhoto = viewModel.hasPhoto,
            onRecipePhoto = {
                showRecognitionDialog = false
                viewModel.recognizeRecipePhoto()
            },
            onChoose = {
                showRecognitionDialog = false
                pickPages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onTake = {
                showRecognitionDialog = false
                try {
                    takePage.launch(viewModel.cameraUri())
                } catch (e: ActivityNotFoundException) {
                    scope.launch { snackbarHostState.showSnackbar(cameraMissing) }
                }
            },
            onDismiss = { showRecognitionDialog = false },
        )
    }

    if (showTextDialog) {
        TextImportDialog(
            onImport = { text ->
                showTextDialog = false
                viewModel.importText(text)
            },
            onDismiss = { showTextDialog = false },
        )
    }

    if (showLinkDialog) {
        LinkImportDialog(
            initial = viewModel.linkForImport,
            onImport = { link ->
                showLinkDialog = false
                viewModel.importLink(link)
            },
            onDismiss = { showLinkDialog = false },
        )
    }

    // Die einzige Frage nach der Texterkennung – beim Speichern (#38).
    if (viewModel.askKeepPages) {
        AlertDialog(
            onDismissRequest = viewModel::cancelKeepPages,
            title = { Text(stringResource(R.string.keep_pages_title)) },
            text = { Text(stringResource(R.string.keep_pages_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.save(keepPages = true, onSaved = onRecipeSaved) }) {
                    Text(stringResource(R.string.keep_pages))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.save(keepPages = false, onSaved = onRecipeSaved) }) {
                    Text(stringResource(R.string.discard))
                }
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.discard(onClose)
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) }
            },
        )
    }
}

/**
 * Übernehmen: „Aus Link übernehmen“ (Laden der Seite mit Abbrechen), „Aus Foto übernehmen“ (Texterkennung mit
 * Fortschritt und Abbrechen) und „Aus Text übernehmen“, der Hinweis zum Prüfen, die gelesenen Seiten und der
 * übernommene Text zum Nachsehen, Kopieren und Entfernen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecognitionSection(
    viewModel: EditViewModel,
    enabled: Boolean,
    onStart: () -> Unit,
    onImportText: () -> Unit,
    onImportLink: () -> Unit,
) {
    var showText by rememberSaveable { mutableStateOf(false) }
    var confirmRemoveText by rememberSaveable { mutableStateOf(false) }
    if (confirmRemoveText) {
        AlertDialog(
            onDismissRequest = { confirmRemoveText = false },
            title = { Text(stringResource(R.string.recognized_text_remove_title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemoveText = false
                    showText = false
                    viewModel.clearRecognizedText()
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveText = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        when (val state = viewModel.recognition) {
            is RecognitionState.Running -> Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        // Ganze Sätze, die der Screenreader von selbst vorliest, sobald eine neue Seite drankommt (#43).
                        Text(
                            text = if (state.pageCount > 1) {
                                stringResource(R.string.ocr_running_pages, state.page, state.pageCount)
                            } else {
                                stringResource(R.string.ocr_running)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        TextButton(onClick = viewModel::cancelRecognition) { Text(stringResource(R.string.cancel)) }
                    }
                    if (state.percent > 0) {
                        LinearProgressIndicator(progress = { state.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            RecognitionState.Idle -> when {
                viewModel.isImporting -> Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            Text(
                                text = stringResource(viewModel.importing ?: R.string.link_loading),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                            TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.cancel)) }
                        }
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                viewModel.recognitionInterrupted -> InterruptedRecognition(
                    enabled = enabled,
                    onDiscard = viewModel::discardRecognition,
                    onRetry = viewModel::retryRecognition,
                )
                else -> {
                    OptionButton(R.drawable.ic_link, R.string.import_from_link, onImportLink, enabled = enabled)
                    OptionButton(R.drawable.ic_document_scanner, R.string.import_from_photo, onStart, enabled = enabled)
                    OptionButton(R.drawable.ic_content_paste, R.string.import_from_text, onImportText, enabled = enabled)
                }
            }
        }

        // Bleibt bis zum Speichern sichtbar; der Screenreader liest den Hinweis vor, sobald er erscheint.
        val checkHint = viewModel.checkHint
        if (checkHint != null && !viewModel.isRecognizing) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(checkHint),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(Spacing.m)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        // Ein Link zum Rezept aus der Videobeschreibung wird erst auf Wunsch geladen; die Seite steht vorher da.
        val videoSite = viewModel.videoRecipeSite
        if (videoSite != null && !viewModel.isImporting) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(start = Spacing.m, top = Spacing.m, end = Spacing.s, bottom = Spacing.xs)) {
                    Text(
                        text = stringResource(R.string.video_recipe_link, videoSite),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = Spacing.s),
                    )
                    TextButton(
                        onClick = viewModel::importVideoRecipeLink,
                        enabled = enabled,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.import_from_link))
                    }
                }
            }
        }

        val hasText = viewModel.recognizedText.isNotBlank()
        if (viewModel.pageCount > 0 || hasText) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (viewModel.pageCount > 0) {
                    TextButton(onClick = viewModel::showPages, enabled = enabled) { Text(stringResource(R.string.pages_show)) }
                }
                if (hasText) {
                    TextButton(onClick = { showText = !showText }) {
                        Text(stringResource(if (showText) R.string.recognized_text_hide else R.string.recognized_text_show))
                    }
                }
            }
        }

        if (hasText) {
            if (showText) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(
                            text = stringResource(R.string.recognized_text),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(R.string.recognized_text_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SelectionContainer {
                            Text(viewModel.recognizedText, style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = { confirmRemoveText = true }, modifier = Modifier.align(Alignment.End)) {
                            Text(stringResource(R.string.recognized_text_remove))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sprache des Rezepts (#62): meist erkennt die App sie selbst; bei kurzen Rezepten hilft die Wahl. Die Sprachen
 * stehen mit ihrem eigenen Namen in der Liste („English“, „Italiano“), damit jeder seine Sprache findet.
 */
@Composable
private fun RecipeLanguageField(language: String?, onChange: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = language?.let { nativeLanguageName(it) } ?: stringResource(R.string.ocr_language_auto)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.field_language, current), modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                Text(
                    text = stringResource(R.string.field_language_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(horizontal = Spacing.m, vertical = Spacing.s)
                        .semantics { heading() },
                )
                val codes = listOf<String?>(null) + TextLanguage.SUPPORTED + listOfNotNull(language?.takeIf { it !in TextLanguage.SUPPORTED })
                codes.forEach { code ->
                    val selected = code == language
                    DropdownMenuItem(
                        text = { Text(code?.let { nativeLanguageName(it) } ?: stringResource(R.string.ocr_language_auto)) },
                        leadingIcon = { if (selected) Icon(Icons.Filled.Check, contentDescription = null) },
                        onClick = {
                            open = false
                            onChange(code)
                        },
                        modifier = Modifier.semantics { this.selected = selected },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.field_language_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.m),
        )
    }
}

/**
 * Quelle mit Seite (#40). Nach einer Texterkennung stehen darunter antippbare Vorschläge: zuletzt
 * genutzte Bücher und die erkannte Seitenzahl – ohne zusätzliches Fenster.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceSection(viewModel: EditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedTextField(
                value = viewModel.source,
                onValueChange = { viewModel.source = it },
                label = { Text(stringResource(R.string.source)) },
                supportingText = { Text(stringResource(R.string.field_source_hint)) },
                modifier = Modifier.weight(2f),
            )
            if (!viewModel.sourceIsLink) {
                OutlinedTextField(
                    value = viewModel.sourcePage,
                    onValueChange = { value -> if (value.length <= 9) viewModel.sourcePage = value },
                    label = { Text(stringResource(R.string.field_source_page)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val books = viewModel.sourceSuggestions
        val page = viewModel.pageSuggestion
        if (books.isNotEmpty() || page != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(
                    text = stringResource(R.string.source_suggestions),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                books.forEach { book ->
                    SuggestionChip(
                        onClick = { viewModel.takeSourceSuggestion(book) },
                        label = { Text(book, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
                page?.let {
                    SuggestionChip(
                        onClick = viewModel::takePageSuggestion,
                        label = { Text(stringResource(R.string.source_page, it)) },
                    )
                }
            }
        }
    }
}

/** Android hat die App während der Erkennung beendet: Die Seiten sind noch da und lassen sich erneut lesen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InterruptedRecognition(
    enabled: Boolean,
    onDiscard: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                text = stringResource(R.string.ocr_interrupted),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onDiscard, enabled = enabled) { Text(stringResource(R.string.discard)) }
                TextButton(onClick = onRetry, enabled = enabled) { Text(stringResource(R.string.ocr_retry)) }
            }
        }
    }
}

/** Auswahl, woher der Text kommt: Rezeptfoto, ausgewählte Fotos (mehrere Seiten) oder Kamera. */
@Composable
private fun RecognitionDialog(
    title: String,
    hasRecipePhoto: Boolean,
    onRecipePhoto: () -> Unit,
    onChoose: () -> Unit,
    onTake: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, modifier = Modifier.semantics { heading() }) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Text(stringResource(R.string.ocr_dialog_text, MAX_PAGES))
                Spacer(Modifier.size(Spacing.xs))
                if (hasRecipePhoto) {
                    OptionButton(R.drawable.ic_document_scanner, R.string.ocr_from_recipe_photo, onRecipePhoto)
                }
                OptionButton(R.drawable.ic_image, R.string.ocr_choose_photos, onChoose)
                OptionButton(R.drawable.ic_photo_camera, R.string.photo_take, onTake)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * „Aus Text übernehmen“: Rezepttext z. B. aus WhatsApp, einer E-Mail oder einer Notiz einfügen. Die Zwischenablage
 * wird nur gelesen, wenn „Aus der Zwischenablage einfügen“ angetippt wird.
 */
@Composable
private fun TextImportDialog(onImport: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var clipboardEmpty by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_from_text), modifier = Modifier.semantics { heading() }) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Text(stringResource(R.string.import_text_hint))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(IncomingText.MAX_CHARS) },
                    label = { Text(stringResource(R.string.import_text_field)) },
                    minLines = 5,
                    maxLines = 12,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OptionButton(R.drawable.ic_content_paste, R.string.paste_from_clipboard, onClick = {
                    scope.launch {
                        val pasted = clipboard.getClipEntry()?.clipData
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
                        clipboardEmpty = pasted.isEmpty()
                        if (pasted.isNotEmpty()) {
                            text = listOf(text.trim(), pasted).filter { it.isNotEmpty() }.joinToString("\n\n")
                                .take(IncomingText.MAX_CHARS)
                        }
                    }
                })
                if (clipboardEmpty) {
                    Text(
                        text = stringResource(R.string.clipboard_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(text) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.import_text_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/**
 * „Aus Link übernehmen“ (#55): Link zu einer Rezeptseite einfügen, z. B. aus dem Browser. Die Zwischenablage wird nur
 * gelesen, wenn „Aus der Zwischenablage einfügen“ angetippt wird; steht dort mehr Text, zählt der erste Link darin.
 */
@Composable
private fun LinkImportDialog(initial: String, onImport: (String) -> Unit, onDismiss: () -> Unit) {
    var link by rememberSaveable { mutableStateOf(initial) }
    var clipboardEmpty by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_from_link), modifier = Modifier.semantics { heading() }) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Text(stringResource(R.string.import_link_hint))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it.take(WebAddress.MAX_LENGTH) },
                    label = { Text(stringResource(R.string.import_link_field)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OptionButton(R.drawable.ic_content_paste, R.string.paste_from_clipboard, onClick = {
                    scope.launch {
                        val pasted = clipboard.getClipEntry()?.clipData
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
                        clipboardEmpty = pasted.isEmpty()
                        if (pasted.isNotEmpty()) link = (TextLinks.first(pasted) ?: pasted).take(WebAddress.MAX_LENGTH)
                    }
                })
                if (clipboardEmpty) {
                    Text(
                        text = stringResource(R.string.clipboard_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onImport(link) }, enabled = link.isNotBlank()) {
                Text(stringResource(R.string.import_text_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun PhotoSection(
    viewModel: EditViewModel,
    onChoose: () -> Unit,
    onTake: () -> Unit,
) {
    val photo = viewModel.photoFile
    val idle = !viewModel.isProcessingPhoto && !viewModel.isRecognizing
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        when {
            viewModel.isProcessingPhoto -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(Spacing.s))
                    Text(
                        text = stringResource(R.string.photo_processing),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            photo != null -> RecipePhotoLarge(
                file = photo,
                onOpen = { viewModel.photoViewer.open(0) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedButton(onClick = onChoose, enabled = idle, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_image), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(R.string.photo_choose))
            }
            OutlinedButton(onClick = onTake, enabled = idle, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(R.string.photo_take))
            }
        }
        if (photo != null && idle) {
            TextButton(onClick = viewModel::removePhoto) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(R.string.photo_remove))
            }
        }
    }
}
