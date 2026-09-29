package com.moltobene.app.ui.recipe

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moltobene.app.R
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.RecipeText
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.components.DraftLabel
import com.moltobene.app.ui.components.RecipePhoto
import com.moltobene.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.recipe_deleted)

    KeepScreenOn()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state is RecipeUiState.Content && !deleting) {
                        IconButton(onClick = { onEdit(viewModel.recipeId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_recipe))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_recipe))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val current = state
        when {
            deleting -> CenteredMessage(text = "", modifier = Modifier.padding(padding)) { CircularProgressIndicator() }
            current is RecipeUiState.Content -> RecipeContent(
                recipe = current.recipe,
                photo = current.photo,
                snackbarHostState = snackbarHostState,
                modifier = Modifier.padding(padding),
            )
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

/** Beim Kochen soll der Bildschirm nicht ausgehen. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun RecipeContent(
    recipe: Recipe,
    photo: File?,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (photo != null) {
            RecipePhoto(file = photo, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f))
        } else {
            RecipePhoto(file = null, placeholderIconSize = 40.dp, modifier = Modifier.fillMaxWidth().height(120.dp))
        }

        Column(
            modifier = Modifier.padding(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            if (recipe.isDraft) DraftLabel()
            Text(
                text = recipe.title.ifBlank { stringResource(R.string.untitled_recipe) },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            recipe.servings?.let { servings ->
                val unit = recipe.servingsUnit?.takeIf { it.isNotBlank() }
                Text(
                    text = if (unit == null) {
                        pluralStringResource(R.plurals.servings_count, servings, servings)
                    } else {
                        stringResource(R.string.servings_with_unit, servings, unit)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (recipe.ingredients.isNotEmpty()) {
                SectionTitle(stringResource(R.string.ingredients))
                recipe.ingredients.forEach { ingredient ->
                    if (ingredient.isHeading) {
                        Text(
                            text = ingredient.text,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                            Text(ingredient.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (recipe.steps.isNotEmpty()) {
                SectionTitle(stringResource(R.string.instructions))
                recipe.steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.widthIn(min = 24.dp),
                        )
                        Text(step, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            if (recipe.notes.isNotBlank()) {
                SectionTitle(stringResource(R.string.notes))
                Text(recipe.notes, style = MaterialTheme.typography.bodyLarge)
            }

            recipe.source?.let { source ->
                SectionTitle(stringResource(R.string.source))
                val url = source.url
                if (url != null && RecipeText.isWebLink(url)) {
                    SourceLink(url = url, snackbarHostState = snackbarHostState)
                } else {
                    val text = listOfNotNull(source.name, source.page).joinToString(", ")
                    if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
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
