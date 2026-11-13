package com.moltobene.app.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.TextLinks
import com.moltobene.app.data.share.IncomingText
import com.moltobene.app.data.web.WebAddress
import com.moltobene.app.ui.components.OptionButton
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Übernehmen: „Aus Link übernehmen“ (Laden der Seite mit Abbrechen), „Aus Foto übernehmen“ (Texterkennung mit
 * Fortschritt und Abbrechen) und „Aus Text übernehmen“, der Hinweis zum Prüfen, die gelesenen Seiten und der
 * übernommene Text zum Nachsehen, Kopieren und Entfernen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecognitionSection(
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
                    ImportChips(enabled, onImportLink, onStart, onImportText)
                }
            }
        }


        // Links aus der Videobeschreibung stehen zur Auswahl; geladen wird erst der gewählte.
        val videoLinks = viewModel.videoRecipeLinks
        if (videoLinks.isNotEmpty() && !viewModel.isImporting) {
            VideoLinks(links = videoLinks, enabled = enabled, onChoose = viewModel::importVideoRecipeLink)
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

/** Android hat die App während der Erkennung beendet: Die Seiten sind noch da und lassen sich erneut lesen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InterruptedRecognition(
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
internal fun RecognitionDialog(
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
internal fun TextImportDialog(onImport: (String) -> Unit, onDismiss: () -> Unit) {
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
                // Bei großer Schrift steht das Eingabefeld vor dem langen Hinweis, damit es gleich zu sehen ist (#53).
                val hint = @Composable { Text(stringResource(R.string.import_text_hint)) }
                val large = isLargeText()
                if (!large) hint()
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(IncomingText.MAX_CHARS) },
                    label = { Text(stringResource(R.string.import_text_field)) },
                    minLines = if (large) 3 else 5,
                    maxLines = 12,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (large) hint()
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
internal fun LinkImportDialog(initial: String, onImport: (String) -> Unit, onDismiss: () -> Unit) {
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

/** Die Wege „Aus Link / Foto / Text übernehmen“ als kleine Chips statt dreier großer Schaltflächen. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ImportChips(enabled: Boolean, onLink: () -> Unit, onPhoto: () -> Unit, onText: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        for ((icon, label, action) in listOf(
            Triple(R.drawable.ic_link, R.string.import_from_link, onLink),
            Triple(R.drawable.ic_document_scanner, R.string.import_from_photo, onPhoto),
            Triple(R.drawable.ic_content_paste, R.string.import_from_text, onText),
        )) {
            androidx.compose.material3.AssistChip(
                onClick = action,
                enabled = enabled,
                label = { Text(stringResource(label)) },
                leadingIcon = {
                    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}
