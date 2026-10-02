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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
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
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    // „Rezept übernehmen“: Auswahl der Erfassungswege, damit unten rechts nur zwei Knöpfe stehen.
    var chooseImport by rememberSaveable { mutableStateOf(false) }
    if (chooseImport) {
        ImportChooser(
            onFromPhoto = {
                chooseImport = false
                onAddFromPhoto()
            },
            onFromText = {
                chooseImport = false
                onAddFromText()
            },
            onDismiss = { chooseImport = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.collection_title), modifier = Modifier.semantics { heading() })
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                ExtendedFloatingActionButton(
                    onClick = { chooseImport = true },
                    icon = { Icon(painterResource(R.drawable.ic_move_to_inbox), contentDescription = null) },
                    text = { Text(stringResource(R.string.import_recipe)) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                ExtendedFloatingActionButton(
                    onClick = onAddRecipe,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_recipe)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                CollectionUiState.Loading -> CenteredMessage(text = "") { CircularProgressIndicator() }
                CollectionUiState.Error -> CenteredMessage(text = stringResource(R.string.collection_error))
                is CollectionUiState.Content -> {
                    val collectionIsEmpty = current.items.isEmpty() && query.isBlank()
                    if (collectionIsEmpty) {
                        EmptyCollection(onAddRecipe = onAddRecipe, onImport = { chooseImport = true })
                    } else {
                        SearchField(query = query, onQueryChange = viewModel::onQueryChange)
                        if (current.items.isEmpty()) {
                            CenteredMessage(text = stringResource(R.string.search_no_results))
                        } else {
                            RecipeList(items = current.items, onOpenRecipe = onOpenRecipe)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
    )
}

@Composable
private fun RecipeList(items: List<RecipeListItem>, onOpenRecipe: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Platz unten, damit die Knöpfe „Rezept übernehmen“ und „Rezept hinzufügen“ den letzten Eintrag nicht verdecken.
        contentPadding = PaddingValues(bottom = 176.dp),
    ) {
        items(items, key = { it.id }) { item ->
            RecipeRow(item = item, onClick = { onOpenRecipe(item.id) })
            HorizontalDivider(modifier = Modifier.padding(start = Spacing.m + 64.dp + Spacing.m))
        }
    }
}

@Composable
private fun RecipeRow(item: RecipeListItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 80.dp)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        RecipePhoto(
            file = item.thumbnail,
            placeholderIconSize = 28.dp,
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.medium),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = item.title.ifBlank { stringResource(R.string.untitled_recipe) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.isDraft) DraftLabel()
        }
    }
}

@Composable
private fun EmptyCollection(onAddRecipe: () -> Unit, onImport: () -> Unit) {
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
        Spacer(Modifier.size(Spacing.l))
        Button(onClick = onAddRecipe) {
            Text(stringResource(R.string.add_recipe))
        }
        Spacer(Modifier.size(Spacing.s))
        OutlinedButton(onClick = onImport) {
            Text(stringResource(R.string.import_recipe))
        }
    }
}

/** Woher das Rezept kommt. „Aus Link übernehmen“ kommt hinzu, sobald die App Internetseiten lesen kann. */
@Composable
private fun ImportChooser(onFromPhoto: () -> Unit, onFromText: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_recipe), modifier = Modifier.semantics { heading() }) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                OptionButton(R.drawable.ic_document_scanner, R.string.import_from_photo, onFromPhoto)
                OptionButton(R.drawable.ic_content_paste, R.string.import_from_text, onFromText)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
