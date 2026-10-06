package com.moltobene.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moltobene.app.BuildConfig
import com.moltobene.app.R
import com.moltobene.app.data.backup.BackupReader
import com.moltobene.app.ui.theme.Spacing
import com.moltobene.app.ui.whatsnew.WhatsNew
import com.moltobene.app.ui.whatsnew.WhatsNewDialog
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

private val BACKUP_MIME_TYPES = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    // Texte über LocalResources lesen: passt sich an, wenn sich z. B. die App-Sprache ändert.
    val resources = LocalResources.current
    var showWhatsNew by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.backup(uri)
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.checkRestore(uri)
    }

    viewModel.event?.let { event ->
        val text = eventText(event)
        LaunchedEffect(event) {
            snackbarHostState.showSnackbar(text)
            viewModel.event = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.collection_title))
            ListItem(
                headlineContent = { Text(stringResource(R.string.backup_title)) },
                supportingContent = { Text(stringResource(R.string.backup_text)) },
                modifier = Modifier.clickable {
                    createBackup.launch(resources.getString(R.string.backup_file_name, LocalDate.now().toString()))
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.restore_title)) },
                supportingContent = { Text(stringResource(R.string.restore_text)) },
                modifier = Modifier.clickable { openBackup.launch(BACKUP_MIME_TYPES) },
            )
            SectionHeader(stringResource(R.string.settings_section_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = { Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.whats_new_title, WhatsNew.VERSION_NAME)) },
                modifier = Modifier.clickable { showWhatsNew = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.privacy_title)) },
                modifier = Modifier.clickable { showPrivacy = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.licenses_title)) },
                modifier = Modifier.clickable(onClick = onOpenLicenses),
            )
        }
    }

    if (showWhatsNew) WhatsNewDialog(onDismiss = { showWhatsNew = false })
    if (showPrivacy) PrivacyDialog(onDismiss = { showPrivacy = false })

    when (val current = viewModel.state) {
        is SettingsUiState.Working -> WorkingDialog(stringResource(current.label))
        is SettingsUiState.ConfirmRestore -> {
            val preview = current.preview
            val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(preview.backupCreatedAt))
            AlertDialog(
                onDismissRequest = viewModel::cancelRestore,
                title = { Text(stringResource(R.string.restore_confirm_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(stringResource(R.string.restore_confirm_date, date))
                        Text(pluralStringResource(R.plurals.restore_confirm_total, preview.total, preview.total))
                        Text(stringResource(R.string.restore_confirm_counts, preview.newCount, preview.existingCount))
                        if (preview.skipped > 0) {
                            Text(
                                text = pluralStringResource(R.plurals.restore_confirm_skipped, preview.skipped, preview.skipped),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(stringResource(R.string.restore_confirm_rule))
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmRestore) { Text(stringResource(R.string.restore_action)) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelRestore) { Text(stringResource(R.string.cancel)) }
                },
            )
        }
        SettingsUiState.Idle -> Unit
    }
}

@Composable
private fun eventText(event: SettingsEvent): String = when (event) {
    is SettingsEvent.BackupDone -> pluralStringResource(R.plurals.backup_done, event.count, event.count)
    SettingsEvent.BackupFailed -> stringResource(R.string.backup_error)
    is SettingsEvent.RestoreDone -> if (event.restored == 0) {
        stringResource(R.string.restore_nothing_new)
    } else {
        pluralStringResource(R.plurals.restore_done, event.restored, event.restored)
    }
    is SettingsEvent.RestoreProblem -> stringResource(
        when (event.problem) {
            BackupReader.Problem.NOT_A_BACKUP -> R.string.restore_error_not_backup
            BackupReader.Problem.NEWER_VERSION -> R.string.restore_error_newer
            BackupReader.Problem.DAMAGED -> R.string.restore_error_damaged
            BackupReader.Problem.TOO_LARGE -> R.string.restore_error_too_large
        }
    )
    SettingsEvent.RestoreFailed -> stringResource(R.string.restore_error_failed)
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = Spacing.m, end = Spacing.m, top = Spacing.l, bottom = Spacing.s)
            .semantics { heading() },
    )
}

/** Hinweis, solange gesichert oder wiederhergestellt wird – lässt sich nicht wegtippen. */
@Composable
private fun WorkingDialog(text: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        androidx.compose.material3.Surface(shape = MaterialTheme.shapes.extraLarge) {
            Row(
                modifier = Modifier.padding(Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                CircularProgressIndicator()
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Kurze Datenschutzerklärung (#55): was gespeichert wird und wann die App ins Internet geht. */
@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_title), modifier = Modifier.semantics { heading() }) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                stringArrayResource(R.array.privacy_paragraphs).forEach { paragraph ->
                    Text(paragraph, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}
