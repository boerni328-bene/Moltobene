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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

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
            ExtendedFloatingActionButton(
                onClick = onAddRecipe,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_recipe)) },
            )
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
                        EmptyCollection(onAddRecipe = onAddRecipe)
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
        // Platz unten, damit der Knopf „Rezept hinzufügen“ den letzten Eintrag nicht verdeckt.
        contentPadding = PaddingValues(bottom = 96.dp),
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
private fun EmptyCollection(onAddRecipe: () -> Unit) {
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
    }
}
