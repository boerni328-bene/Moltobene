package com.moltobene.app.ui.basics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import com.moltobene.app.ui.components.AppSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
import com.moltobene.app.data.basics.BasicRecipes
import com.moltobene.app.ui.collection.RecipeListItem
import com.moltobene.app.ui.collection.Thumbnail
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.components.ScreenTitle
import com.moltobene.app.ui.theme.Spacing
import com.moltobene.app.ui.components.readingWidth

/**
 * Grundrezepte (#82): mitgelieferte Rezepte zum Übernehmen in die eigene Sammlung. Antippen zeigt Zutaten und
 * Zubereitung; „Übernehmen“ macht daraus ein ganz normales Rezept. Statt eines Fotos steht der Anfangsbuchstabe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicsScreen(
    viewModel: BasicsViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenRecipe: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocalConfiguration.current.locales[0].language
    val resources = LocalResources.current
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    // Ergebnis der Übernahme: Hinweis mit „Öffnen“ oder, bei einem Duplikat, die Rückfrage.
    when (val event = viewModel.event) {
        is BasicsEvent.Added -> LaunchedEffect(event) {
            val result = snackbarHostState.showSnackbar(
                message = resources.getString(R.string.basics_added, event.title),
                actionLabel = resources.getString(R.string.basics_open),
                duration = SnackbarDuration.Short,
            )
            if (viewModel.event === event) viewModel.event = null
            if (result == SnackbarResult.ActionPerformed) onOpenRecipe(event.recipeId)
        }
        BasicsEvent.Failed -> LaunchedEffect(event) {
            snackbarHostState.showSnackbar(resources.getString(R.string.basics_failed))
            viewModel.event = null
        }
        is BasicsEvent.Duplicate -> AlertDialog(
            onDismissRequest = { viewModel.event = null },
            title = { Text(stringResource(R.string.basics_duplicate_title), modifier = Modifier.semantics { heading() }) },
            text = { Text(stringResource(R.string.basics_duplicate_text, event.title)) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        viewModel.event = null
                        onOpenRecipe(event.existingRecipeId)
                    }) { Text(stringResource(R.string.basics_open)) }
                    TextButton(onClick = {
                        val basic = (state as? BasicsUiState.Content)?.recipes?.firstOrNull { it.id == event.basicId }
                        viewModel.event = null
                        if (basic != null) viewModel.adopt(basic, language, force = true)
                    }) { Text(stringResource(R.string.basics_adopt_anyway)) }
                    TextButton(onClick = { viewModel.event = null }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
        null -> Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppTitle(style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = state) {
            BasicsUiState.Loading -> CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
            BasicsUiState.Error -> CenteredMessage(text = stringResource(R.string.basics_error), modifier = Modifier.padding(padding))
            is BasicsUiState.Content -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().readingWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(bottom = Spacing.s)) {
                        ScreenTitle(stringResource(R.string.basics_title))
                        Text(
                            stringResource(R.string.basics_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(current.recipes, key = { it.id }) { basic ->
                    BasicCard(
                        basic = basic,
                        language = language,
                        expanded = expanded == basic.id,
                        onToggle = { expanded = if (expanded == basic.id) null else basic.id },
                        onAdopt = { viewModel.adopt(basic, language) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicCard(
    basic: BasicRecipes.BasicRecipe,
    language: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdopt: () -> Unit,
) {
    val text = BasicRecipes.text(basic, language)
    val stateText = stringResource(if (expanded) R.string.basics_expanded else R.string.basics_collapsed)
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().semantics { stateDescription = stateText },
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                Thumbnail(
                    item = RecipeListItem(id = basic.id, title = text.title, isDraft = false, favorite = false, thumbnail = null),
                    title = text.title,
                    modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (expanded) {
                Text(
                    pluralStringResource(R.plurals.servings_count, basic.servings, basic.servings),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.ingredients), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                for (line in text.ingredients) {
                    val heading = line.endsWith(":")
                    Text(
                        line,
                        style = if (heading) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
                        color = if (heading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(stringResource(R.string.instructions), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                text.steps.forEachIndexed { index, step ->
                    Text("${index + 1}. $step", style = MaterialTheme.typography.bodyLarge)
                }
                if (text.notes.isNotBlank()) {
                    Text(stringResource(R.string.notes), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                    Text(text.notes, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Button(onClick = onAdopt, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.basics_adopt))
            }
        }
    }
}
