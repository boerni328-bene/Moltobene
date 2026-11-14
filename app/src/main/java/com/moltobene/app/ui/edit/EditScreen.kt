package com.moltobene.app.ui.edit

import com.moltobene.app.data.voice.DictationState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import com.moltobene.app.ui.components.AppSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.moltobene.app.R
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.components.NoticeCard
import com.moltobene.app.ui.components.NoticeKind
import com.moltobene.app.ui.components.TagLabels
import com.moltobene.app.ui.components.PageViewer
import com.moltobene.app.ui.components.ScreenTitle
import com.moltobene.app.ui.theme.Spacing
import com.moltobene.app.ui.components.readingWidth
import kotlinx.coroutines.launch

/**
 * Dateitypen für „Aus Datei übernehmen“: Rezeptdateien (schema.org), gespeicherte Rezeptseiten und Text.
 * Manche Speicherorte kennen den Typ einer .json-Datei nicht; ob ein Rezept darin steht, zeigt erst der Inhalt.
 */
internal val RECIPE_FILE_TYPES = arrayOf(
    "application/json", "application/ld+json", "text/html", "text/plain", "application/octet-stream",
)

/** Dateitypen für „Aus Audio übernehmen“: Audiodateien aller Art, auch Sprachnachrichten (ogg/opus). */
internal val AUDIO_FILE_TYPES = arrayOf("audio/*", "application/ogg")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: EditViewModel,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onSaved: (id: String, wasNew: Boolean, message: String) -> Unit,
) {
    // Damit Schlagwörter aus Dateien in jeder Sprache der App erkannt werden (z. B. „Hauptgerichte“ und „Main courses“).
    val appContext = LocalContext.current
    val tagLabelSets = remember(appContext) { TagLabels.allLanguages(appContext) }
    SideEffect { viewModel.tagLabelSets = tagLabelSets }
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
    // „Aus Audio übernehmen“ (Beta): Audiodatei in der Dateiauswahl von Android wählen; sie wird nur gelesen.
    val openAudioFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importAudio(uri)
    }
    LaunchedEffect(Unit) {
        if (viewModel.takeAudioPrompt()) {
            try {
                openAudioFile.launch(AUDIO_FILE_TYPES)
            } catch (e: ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(fileChooserMissing) }
            }
        }
    }
    // „Aufnehmen“ (Rezept einsprechen, Beta): Das Mikrofon wird erst beim Antippen gebraucht und dann abgefragt.
    val dictationState by viewModel.dictationState.collectAsStateWithLifecycle()
    val micDenied = stringResource(R.string.voice_permission_denied)
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startDictation() else scope.launch { snackbarHostState.showSnackbar(micDenied) }
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (viewModel.takeSpeechPrompt()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                viewModel.startDictation()
            } else {
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    (dictationState as? DictationState.Failed)?.let { failed ->
        LaunchedEffect(failed) { viewModel.onDictationFailed(failed.problem) }
    }
    // Im Hintergrund hört das Mikrofon nicht weiter zu. Beim Drehen oder Falten (#92) läuft die Aufnahme weiter,
    // sonst ginge der bisher gesprochene Text verloren; Zustand und Erkennung liegen im ViewModel.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val changingConfiguration = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
                .filterIsInstance<Activity>().firstOrNull()?.isChangingConfigurations == true
            if (event == Lifecycle.Event.ON_STOP && !changingConfiguration) viewModel.cancelDictation()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (dictationState !is DictationState.Off && dictationState !is DictationState.Failed) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDictation,
            title = { Text(stringResource(R.string.add_record), modifier = Modifier.semantics { heading() }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(stringResource(R.string.dictation_hint), style = MaterialTheme.typography.bodyMedium)
                    val shown = dictationState
                    Text(
                        text = when (shown) {
                            DictationState.Starting -> stringResource(R.string.dictation_starting)
                            DictationState.Finishing -> stringResource(R.string.dictation_finishing)
                            else -> stringResource(R.string.dictation_listening)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    (shown as? DictationState.Listening)?.text?.takeIf { it.isNotBlank() }?.let { heard ->
                        Text(heard, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::finishDictation, enabled = dictationState is DictationState.Listening) {
                    Text(stringResource(R.string.dictation_done))
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDictation) { Text(stringResource(R.string.cancel)) } },
        )
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
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { padding ->
        if (viewModel.isLoading) {
            CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .readingWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                ScreenTitle(stringResource(if (viewModel.isNew) R.string.add_recipe else R.string.edit_recipe))
                // Hinweise zum Übernehmen stehen ganz oben, damit sie nicht unter dem Foto verschwinden (#90).
                val checkHint = viewModel.checkHint
                if (checkHint != null && !viewModel.isRecognizing) NoticeCard(stringResource(checkHint), NoticeKind.INFO)
                viewModel.problem?.let { problem ->
                    NoticeCard(stringResource(problem), NoticeKind.PROBLEM) {
                        if (viewModel.canRetryImport) {
                            TextButton(onClick = viewModel::retryImport) { Text(stringResource(R.string.import_retry)) }
                        }
                        TextButton(onClick = viewModel::dismissProblem) { Text(stringResource(R.string.notice_dismiss)) }
                    }
                }
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

                TagsSection(viewModel)

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
