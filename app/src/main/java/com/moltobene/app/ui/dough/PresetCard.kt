package com.moltobene.app.ui.dough

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.dough.DoughPresets
import com.moltobene.app.data.dough.DoughType
import com.moltobene.app.data.dough.LeavenedInput
import com.moltobene.app.ui.theme.Spacing

/** Ein Eintrag der Liste: Name, Werte und – bei eigenen – der Name zum Löschen. */
internal class PresetEntry(val name: String, val input: LeavenedInput, val ownName: String?)

/** Anzeigename einer mitgelieferten Voreinstellung aus den Ressourcen. */
@Composable
internal fun shippedPresetName(key: String): String = stringResource(
    when (key) {
        "neapolitan" -> R.string.dough_preset_neapolitan
        "sheet_pan" -> R.string.dough_preset_sheet_pan
        "white_bread" -> R.string.dough_preset_white_bread
        "poolish_bread" -> R.string.dough_preset_poolish_bread
        else -> R.string.dough_preset_default
    },
)

/** Alle Einträge einer Teigart: erst die mitgelieferten, dann die eigenen. */
@Composable
internal fun presetEntries(type: DoughType, own: List<DoughPresets.Own>): List<PresetEntry> {
    val shipped = DoughPresets.shipped(type).map { PresetEntry(shippedPresetName(it.key), it.input, null) }
    return shipped + own.map { PresetEntry(it.name, DoughPresets.values(type, it), it.name) }
}

/**
 * Voreinstellungen (#83, Etappe 3): Pfeile zum Durchschalten, Antippen des Namens öffnet die Liste, darunter
 * „Als Voreinstellung speichern“ und bei einer eigenen „Löschen“. [index] −1 heißt: die Werte sind keine der Einträge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PresetCard(
    entries: List<PresetEntry>,
    index: Int,
    canSave: Boolean,
    onSelect: (Int) -> Unit,
    onSave: (String) -> DoughPresets.SaveResult,
    onDelete: (String) -> Unit,
    compact: Boolean = false,
    extraActions: List<Pair<String, () -> Unit>> = emptyList(),
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    // Sind die Werte keine der Voreinstellungen (eigene Änderungen), wird vor dem Ersetzen nachgefragt.
    var pending by rememberSaveable { mutableStateOf(-1) }
    fun choose(i: Int) {
        when {
            i < 0 -> Unit
            index < 0 -> pending = i
            else -> onSelect(i)
        }
    }
    val name = entries.getOrNull(index)?.name ?: stringResource(R.string.dough_preset_custom)
    val position = if (index >= 0) stringResource(R.string.dough_preset_position, index + 1, entries.size) else null
    val description = listOfNotNull(stringResource(R.string.dough_preset_choose, name), position).joinToString(", ")

    val navigation: @Composable (@Composable () -> Unit) -> Unit = { trailing ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { choose(DoughPresets.step(index, entries.size, -1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.dough_preset_previous))
            }
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics(mergeDescendants = true) {
                            role = Role.DropdownList
                            contentDescription = description
                        }
                        .clickable { menuOpen = true },
                ) {
                    Text(
                        name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (position != null) {
                        Text(
                            position, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    entries.forEachIndexed { i, entry ->
                        DropdownMenuItem(
                            text = { Text(if (i == index) "✓ ${entry.name}" else entry.name) },
                            onClick = {
                                menuOpen = false
                                choose(i)
                            },
                        )
                    }
                }
            }
            IconButton(onClick = { choose(DoughPresets.step(index, entries.size, +1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.dough_preset_next))
            }
            trailing()
        }
    }
    val isOwn = entries.getOrNull(index)?.ownName != null
    if (compact) {
        // Eine Zeile ohne Karte (ohne Scrollen, Entwurf vom 30.10.2026): Speichern, Löschen und weitere Einträge im Menü.
        var moreOpen by rememberSaveable { mutableStateOf(false) }
        Box(modifier = Modifier.padding(horizontal = Spacing.s)) {
            navigation {
                Box {
                    IconButton(onClick = { moreOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dough_preset_save)) },
                            enabled = canSave,
                            onClick = {
                                moreOpen = false
                                saving = true
                            },
                        )
                        if (isOwn) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dough_preset_delete)) },
                                onClick = {
                                    moreOpen = false
                                    deleting = true
                                },
                            )
                        }
                        for ((label, action) in extraActions) {
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                moreOpen = false
                                action()
                            })
                        }
                    }
                }
            }
        }
    } else {
        Card("") {
            navigation {}
            // Bei großer Schrift passen die Knöpfe nicht nebeneinander; sie rutschen in die nächste Zeile.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                TextButton(onClick = { saving = true }, enabled = canSave) { Text(stringResource(R.string.dough_preset_save)) }
                if (isOwn) {
                    TextButton(onClick = { deleting = true }) { Text(stringResource(R.string.dough_preset_delete)) }
                }
            }
        }
    }

    if (pending >= 0) {
        AlertDialog(
            onDismissRequest = { pending = -1 },
            title = { Text(stringResource(R.string.dough_preset_replace_title)) },
            text = { Text(stringResource(R.string.dough_preset_replace_text)) },
            confirmButton = {
                TextButton(onClick = {
                    val chosen = pending
                    pending = -1
                    onSelect(chosen)
                }) { Text(stringResource(R.string.dough_preset_replace_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pending = -1 }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (saving) {
        SavePresetDialog(
            existing = entries.mapNotNull { it.ownName },
            onSave = { typed ->
                val result = onSave(typed)
                if (result == DoughPresets.SaveResult.SAVED) saving = false
                result
            },
            onDismiss = { saving = false },
        )
    }
    val ownName = entries.getOrNull(index)?.ownName
    if (deleting && ownName != null) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.dough_preset_delete_title, ownName)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    onDelete(ownName)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SavePresetDialog(
    existing: List<String>,
    onSave: (String) -> DoughPresets.SaveResult,
    onDismiss: () -> Unit,
) {
    var typed by rememberSaveable { mutableStateOf("") }
    var failed by rememberSaveable { mutableStateOf(false) }
    val clean = DoughPresets.cleanName(typed)
    val replaces = existing.any { it.equals(clean, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dough_preset_save_title)) },
        text = {
            OutlinedTextField(
                value = typed,
                onValueChange = {
                    typed = it.take(DoughPresets.MAX_NAME_LENGTH * 2)
                    failed = false
                },
                label = { Text(stringResource(R.string.dough_preset_name)) },
                singleLine = true,
                isError = failed,
                supportingText = {
                    when {
                        failed -> Text(stringResource(R.string.dough_preset_too_many, DoughPresets.MAX_OWN))
                        replaces -> Text(stringResource(R.string.dough_preset_replace_hint))
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            )
        },
        confirmButton = {
            TextButton(
                enabled = clean.isNotEmpty(),
                onClick = { if (onSave(typed) == DoughPresets.SaveResult.TOO_MANY) failed = true },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
