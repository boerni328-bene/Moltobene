package com.moltobene.app.ui.edit

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.ui.components.AppTitleWithScreen
import com.moltobene.app.ui.components.OptionButton
import com.moltobene.app.ui.theme.Spacing

/**
 * Seitenübersicht vor der Texterkennung: Fotos aus Kamera, Fotoauswahl oder „Teilen mit…“ sammeln sich hier
 * als Vorschaubilder. Weitere Seiten lassen sich nacheinander fotografieren oder auswählen, ohne dass
 * zwischendurch gelesen wird; „Text erkennen“ liest am Ende alle Seiten zusammen. Ein Bereich wird nur bei
 * Bedarf gewählt (z. B. bei zwei Spalten), Seiten lassen sich wieder entfernen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagesScreen(
    overview: PageOverview,
    thumbnails: Map<String, Bitmap>,
    failed: Map<String, Boolean>,
    adding: Boolean,
    snackbarHostState: SnackbarHostState,
    language: String?,
    onLanguageChange: (String?) -> Unit,
    onTakePage: () -> Unit,
    onChoosePages: () -> Unit,
    onChooseArea: (Int) -> Unit,
    onRemovePage: (Int) -> Unit,
    onRecognize: () -> Unit,
    onDiscard: () -> Unit,
) {
    // Mehrere Fotos gehen nie aus Versehen verloren: Abbrechen fragt erst nach.
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    BackHandler { confirmDiscard = true }
    val count = overview.pages.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AppTitleWithScreen(
                        screen = stringResource(R.string.import_from_photo),
                        detail = pluralStringResource(R.plurals.pages_count, count, count),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { confirmDiscard = true }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = { LanguageMenuButton(language, onLanguageChange) },
            )
        },
        // Die beiden wichtigsten Schritte bleiben immer sichtbar, auch mit großer Schrift: nächste Seite oder lesen.
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(Spacing.m),
            ) {
                OutlinedButton(
                    onClick = onTakePage,
                    enabled = overview.canAddMore && !adding,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.s))
                    Text(stringResource(R.string.pages_take_more), textAlign = TextAlign.Center)
                }
                Button(onClick = onRecognize, enabled = !adding, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.area_recognize), textAlign = TextAlign.Center)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            itemsIndexed(overview.pages, key = { _, ref -> ref }) { index, ref ->
                PageCard(
                    number = index + 1,
                    thumbnail = thumbnails[ref],
                    failed = failed[ref] == true,
                    areaChosen = overview.areaChosen.getOrElse(index) { false },
                    onChooseArea = { onChooseArea(index) },
                    onRemove = { onRemovePage(index) },
                )
            }
            if (adding) {
                item {
                    val description = stringResource(R.string.pages_adding)
                    PageFrame(modifier = Modifier.semantics { contentDescription = description }) {
                        CircularProgressIndicator()
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    // Unter den Seiten, damit die Fotos auch mit großer Schrift gleich oben zu sehen sind.
                    Text(
                        text = stringResource(R.string.pages_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OptionButton(
                        R.drawable.ic_image,
                        R.string.pages_choose_more,
                        onChoosePages,
                        enabled = overview.canAddMore && !adding,
                    )
                    if (!overview.canAddMore) {
                        Text(
                            text = pluralStringResource(R.plurals.pages_max, MAX_PAGES, MAX_PAGES),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.pages_discard_title)) },
            text = { Text(stringResource(R.string.pages_discard_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscard()
                }) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.pages_keep)) }
            },
        )
    }
}

/** Eine Seite: Vorschaubild, Nummer, „Bereich gewählt“ als Text und die Schaltflächen für Bereich und Entfernen. */
@Composable
private fun PageCard(
    number: Int,
    thumbnail: Bitmap?,
    failed: Boolean,
    areaChosen: Boolean,
    onChooseArea: () -> Unit,
    onRemove: () -> Unit,
) {
    Column {
        PageFrame {
            when {
                thumbnail != null -> {
                    val image = remember(thumbnail) { thumbnail.asImageBitmap() }
                    // Die Nummer darunter beschreibt die Seite; das Bild selbst ist für den Screenreader nur Schmuck.
                    Image(image, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                }
                failed -> Text(
                    text = stringResource(R.string.page_load_error),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(Spacing.s),
                )
                else -> CircularProgressIndicator()
            }
        }
        Text(
            text = stringResource(R.string.page_label, number),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        if (areaChosen) {
            Text(
                text = stringResource(R.string.page_area_chosen),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onChooseArea, enabled = !failed) {
                Icon(painterResource(R.drawable.ic_crop), contentDescription = stringResource(R.string.page_choose_area, number))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.page_remove_number, number))
            }
        }
    }
}

/** Fläche im Seitenformat für Vorschaubild, Ladeanzeige oder Fehler. */
@Composable
private fun PageFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        content()
    }
}
