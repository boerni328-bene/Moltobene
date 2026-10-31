package com.moltobene.app.ui.dough

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
        "new_york" -> R.string.dough_preset_new_york
        "detroit" -> R.string.dough_preset_detroit
        "focaccia" -> R.string.dough_preset_focaccia
        "farmhouse" -> R.string.dough_preset_farmhouse
        "dark_rye" -> R.string.dough_preset_dark_rye
        "whole_wheat" -> R.string.dough_preset_whole_wheat
        "spelt" -> R.string.dough_preset_spelt
        "pinsa" -> R.string.dough_preset_pinsa
        "grandma" -> R.string.dough_preset_grandma
        "chicago" -> R.string.dough_preset_chicago
        "ciabatta" -> R.string.dough_preset_ciabatta
        "baguette" -> R.string.dough_preset_baguette
        "semmel" -> R.string.dough_preset_semmel
        "buerli" -> R.string.dough_preset_buerli
        "salt_stick" -> R.string.dough_preset_salt_stick
        "lye" -> R.string.dough_preset_lye
        "naan" -> R.string.dough_preset_naan
        "toast" -> R.string.dough_preset_toast
        "bagel" -> R.string.dough_preset_bagel
        "white_bread" -> R.string.dough_preset_white_bread
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
 * Voreinstellungen (#83, Etappe 3): alle Einträge nebeneinander, durch senkrechte Striche getrennt und seitlich zu
 * verschieben; der gewählte steht fett in größerer Schrift und wird ins Bild geholt. Bei einer eigenen Voreinstellung
 * gibt es ein Symbol zum Löschen; Speichern und Zurücksetzen liegen oben in der Leiste. Bei großer Schrift bleiben die
 * Knöpfe in der Karte. [index] −1 heißt: die Werte sind keine der Einträge, dann steht „Eigene Werte“ vorn.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PresetCard(
    entries: List<PresetEntry>,
    index: Int,
    onSelect: (Int) -> Unit,
    saving: Boolean,
    onSavingChange: (Boolean) -> Unit,
    canSave: Boolean,
    onSave: (String) -> DoughPresets.SaveResult,
    onDelete: (String) -> Unit,
    compact: Boolean = false,
) {
    var deleting by rememberSaveable { mutableStateOf(false) }
    // Sind die Werte keine der Voreinstellungen (eigene Änderungen), wird vor dem Ersetzen nachgefragt.
    var pending by rememberSaveable { mutableStateOf(-1) }
    fun choose(i: Int) {
        when {
            i < 0 || i == index -> Unit
            index < 0 -> pending = i
            else -> onSelect(i)
        }
    }
    val isOwn = entries.getOrNull(index)?.ownName != null
    val picker: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
        ) {
            if (index < 0) {
                PresetItem(stringResource(R.string.dough_preset_custom), selected = true, onClick = {})
            }
            entries.forEachIndexed { i, entry ->
                if (i > 0 || index < 0) VerticalDivider(modifier = Modifier.height(24.dp))
                PresetItem(entry.name, selected = i == index, onClick = { choose(i) })
            }
        }
    }
    if (compact) {
        // Eine Zeile ohne Karte (ohne Scrollen, Entwurf vom 30.10.2026): die Einträge und bei eigenen das Löschen.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.s),
        ) {
            picker(Modifier.weight(1f))
            if (isOwn) {
                IconButton(onClick = { deleting = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.dough_preset_delete))
                }
            }
        }
    } else {
        Card("") {
            picker(Modifier.fillMaxWidth())
            // Bei großer Schrift passen die Knöpfe nicht nebeneinander; sie rutschen in die nächste Zeile.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                TextButton(onClick = { onSavingChange(true) }, enabled = canSave) { Text(stringResource(R.string.dough_preset_save)) }
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
                if (result == DoughPresets.SaveResult.SAVED) onSavingChange(false)
                result
            },
            onDismiss = { onSavingChange(false) },
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


/** Ein Eintrag der Reihe: der gewählte fett und größer, die anderen ruhiger; der gewählte rückt ins Bild. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetItem(name: String, selected: Boolean, onClick: () -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) { if (selected) requester.bringIntoView() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .bringIntoViewRequester(requester)
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.s),
    ) {
        Text(
            name,
            style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else null,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
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

/**
 * Auswahl einer Teigart als Reihe wie bei den Voreinstellungen: nebeneinander, durch senkrechte Striche getrennt, seitlich
 * zu verschieben, die gewählte fett und größer (Pasta und Kuchen).
 */
@Composable
internal fun <T> KindRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
    ) {
        options.forEachIndexed { i, option ->
            if (i > 0) VerticalDivider(modifier = Modifier.height(24.dp))
            PresetItem(label(option), selected = option == selected, onClick = { onSelect(option) })
        }
    }
}
