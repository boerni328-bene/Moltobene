package com.moltobene.app.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.moltobene.app.ui.components.AppSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.moltobene.app.ui.recipe.ShareEvent
import com.moltobene.app.ui.recipe.openShareMenu
import com.moltobene.app.ui.recipe.shareLabels
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.components.DraftLabel
import com.moltobene.app.ui.components.OptionButton
import com.moltobene.app.ui.components.PhotoPlaceholder
import com.moltobene.app.ui.components.RecipePhoto
import com.moltobene.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenRecipe: (String) -> Unit,
    onAddRecipe: () -> Unit,
    onAddFromPhoto: () -> Unit,
    onAddFromText: () -> Unit,
    onAddFromLink: () -> Unit,
    onAddFromFile: () -> Unit,
    onAddFromAudio: () -> Unit,
    onAddFromSpeech: () -> Unit,
    onOpenDough: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHome: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val grid by viewModel.grid.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val backupHint by viewModel.backupHint.collectAsStateWithLifecycle()
    val textSize by viewModel.textSize.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    // Menü zu einem Rezept in der Liste (langes Berühren): Favorit, Teilen, Schriftgröße, Löschen.
    var deleteTarget by remember { mutableStateOf<RecipeListItem?>(null) }
    val deletedMessage = stringResource(R.string.recipe_deleted)
    viewModel.shareEvent?.let { event ->
        val chooserTitle = stringResource(R.string.share_recipe)
        val errorText = stringResource(R.string.share_error)
        LaunchedEffect(event) {
            val opened = event is ShareEvent.Ready && openShareMenu(context, event.share, chooserTitle)
            if (!opened) snackbarHostState.showSnackbar(errorText)
            viewModel.shareEvent = null
        }
    }
    val actions = RecipeActions(
        textSize = textSize,
        shareEnabled = !viewModel.preparingShare,
        onTextSize = viewModel::onTextSizeChange,
        onFavorite = viewModel::toggleFavorite,
        onShare = { id, asFile -> viewModel.share(id, asFile, shareLabels(resources)) },
        onDelete = { deleteTarget = it },
    )
    deleteTarget?.let { item ->
        val title = item.title.ifBlank { stringResource(R.string.untitled_recipe) }
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text, title)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    viewModel.delete(item.id)
                    scope.launch { snackbarHostState.showSnackbar(deletedMessage) }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    // Beim Zurückkehren neu lesen, z. B. nachdem in den Einstellungen gesichert wurde (#51).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshBackupHint() }

    // „Hinzufügen“: alle Wege an einer Stelle, dazu der Teigrechner.
    var chooseAdd by rememberSaveable { mutableStateOf(false) }
    if (chooseAdd) {
        AddChooser(
            onWrite = {
                chooseAdd = false
                onAddRecipe()
            },
            onFromLink = {
                chooseAdd = false
                onAddFromLink()
            },
            onFromPhoto = {
                chooseAdd = false
                onAddFromPhoto()
            },
            onFromText = {
                chooseAdd = false
                onAddFromText()
            },
            onFromFile = {
                chooseAdd = false
                onAddFromFile()
            },
            onFromAudio = {
                chooseAdd = false
                onAddFromAudio()
            },
            onFromSpeech = {
                chooseAdd = false
                onAddFromSpeech()
            },
            onDough = {
                chooseAdd = false
                onOpenDough()
            },
            onDismiss = { chooseAdd = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Der Name der App als Titel (Wunsch des Projektinhabers vom 08.10.2026).
                title = { AppTitle(style = MaterialTheme.typography.titleLarge, heading = true) },
                // Startsymbol statt Pfeil (Wunsch des Projektinhabers vom 22.10.2026): führt zur Startseite.
                navigationIcon = {
                    IconButton(onClick = onOpenHome) {
                        Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.start_home))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val addLabel = stringResource(R.string.add_short)
            ExtendedFloatingActionButton(
                onClick = { chooseAdd = true },
                modifier = Modifier.semantics { contentDescription = addLabel },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_short)) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                CollectionUiState.Loading -> CenteredMessage(text = "") { CircularProgressIndicator() }
                CollectionUiState.Error -> CenteredMessage(text = stringResource(R.string.collection_error))
                is CollectionUiState.Content -> {
                    val collectionIsEmpty = current.items.isEmpty() && query.isBlank() && current.filter == CollectionFilter.ALL &&
                        current.selectedTags.isEmpty()
                    if (collectionIsEmpty) {
                        EmptyCollection()
                    } else {
                        SearchRow(
                            query = query,
                            onQueryChange = viewModel::onQueryChange,
                            grid = grid,
                            onGridChange = viewModel::onGridChange,
                            sort = sort,
                            onSortChange = viewModel::onSortChange,
                        )
                        FilterRow(
                            filter = current.filter,
                            hasFavorites = current.hasFavorites,
                            hasDrafts = current.hasDrafts,
                            tagCounts = current.tagCounts,
                            selectedTags = current.selectedTags,
                            onFilterChange = viewModel::onFilterChange,
                            onToggleTag = viewModel::onToggleTag,
                        )
                        backupHint?.takeIf { query.isBlank() }?.let { unsaved ->
                            BackupHint(
                                unsaved = unsaved,
                                onBackup = onOpenSettings,
                                onHide = { viewModel.hideBackupHint(unsaved) },
                            )
                        }
                        if (current.items.isEmpty()) {
                            CenteredMessage(text = stringResource(R.string.search_no_results))
                        } else {
                            if (grid) {
                                RecipeGrid(items = current.items, onOpenRecipe = onOpenRecipe, actions = actions)
                            } else {
                                RecipeList(items = current.items, onOpenRecipe = onOpenRecipe, actions = actions)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCollection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.l),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PhotoPlaceholder(
            iconSize = 40.dp,
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.extraLarge),
        )
        Spacer(Modifier.size(Spacing.l))
        Text(
            text = stringResource(R.string.collection_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.size(Spacing.s))
        Text(
            text = stringResource(R.string.collection_empty_text),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
}
    }

/**
 * Ruhiger Hinweis zum Sichern (#51): kein Fenster und keine Benachrichtigung. „Sichern“ führt zu den Einstellungen;
 * ausgeblendet erscheint er erst wieder, wenn weitere Rezepte ungesichert sind.
 */
@Composable
private fun BackupHint(unsaved: Int, onBackup: () -> Unit, onHide: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
    ) {
        val text = pluralStringResource(R.plurals.backup_hint, unsaved, unsaved)
        val actions = @Composable {
            TextButton(onClick = onBackup) { Text(stringResource(R.string.backup_hint_action)) }
            IconButton(onClick = onHide) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.backup_hint_hide))
            }
        }
        // Mit großer Schrift bliebe neben den Schaltflächen kaum Platz: Dann stehen sie unter dem Text.
        if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
            Column(modifier = Modifier.padding(start = Spacing.m, top = Spacing.s)) {
                Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = Spacing.m))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End)) { actions() }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = Spacing.m)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Spacing.s),
                )
                actions()
            }
        }
    }
}

/** Ab dieser Schriftgröße (Einstellung des Handys) stehen Schaltflächen unter statt neben einem Hinweis. */
private const val LARGE_FONT_SCALE = 1.5f
