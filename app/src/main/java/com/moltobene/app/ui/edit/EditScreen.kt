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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.moltobene.app.R
import com.moltobene.app.ui.components.CenteredMessage
import com.moltobene.app.ui.components.RecipePhoto
import com.moltobene.app.ui.theme.Spacing
import kotlinx.coroutines.launch

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
    val cameraMissing = stringResource(R.string.camera_unavailable)
    val scope = rememberCoroutineScope()

    val requestClose: () -> Unit = {
        if (viewModel.isDirty) {
            confirmDiscard = true
        } else {
            onClose()
        }
    }
    BackHandler { requestClose() }

    // Geht die App in den Hintergrund (Anruf, anderer Bildschirm), wird ein neues Rezept als Entwurf gesichert.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStop() }

    viewModel.message?.let { messageRes ->
        val text = stringResource(messageRes)
        LaunchedEffect(messageRes, text) {
            snackbarHostState.showSnackbar(text)
            viewModel.message = null
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCameraResult(success)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (viewModel.isNew) R.string.add_recipe else R.string.edit_recipe))
                },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save { id, wasNew -> onSaved(id, wasNew, savedMessage) } },
                        enabled = !viewModel.isSaving && !viewModel.isLoading && !viewModel.isProcessingPhoto,
                    ) {
                        Text(stringResource(R.string.save))
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

                OutlinedTextField(
                    value = viewModel.source,
                    onValueChange = { viewModel.source = it },
                    label = { Text(stringResource(R.string.source)) },
                    supportingText = { Text(stringResource(R.string.field_source_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )

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

@Composable
private fun PhotoSection(
    viewModel: EditViewModel,
    onChoose: () -> Unit,
    onTake: () -> Unit,
) {
    val photo = viewModel.photoFile
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
            photo != null -> RecipePhoto(
                file = photo,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.large),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            OutlinedButton(onClick = onChoose, enabled = !viewModel.isProcessingPhoto, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_image), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(R.string.photo_choose))
            }
            OutlinedButton(onClick = onTake, enabled = !viewModel.isProcessingPhoto, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(R.string.photo_take))
            }
        }
        if (photo != null && !viewModel.isProcessingPhoto) {
            TextButton(onClick = viewModel::removePhoto) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.s))
                Text(stringResource(R.string.photo_remove))
            }
        }
    }
}
