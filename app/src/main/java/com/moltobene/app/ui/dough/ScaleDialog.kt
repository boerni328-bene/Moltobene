package com.moltobene.app.ui.dough

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.moltobene.app.R
import com.moltobene.app.data.WeightUnit
import com.moltobene.app.data.dough.DoughScale
import com.moltobene.app.ui.components.RepeatIconButton
import com.moltobene.app.ui.theme.Spacing
import kotlin.math.roundToLong

/**
 * Fenster zum Ändern einer Menge oben im Teigrechner (Wunsch vom 18.11.2026): der Wert mit − und + oder per Eingabe;
 * „Umrechnen“ rechnet den ganzen Teig im gleichen Verhältnis um ([DoughScale]). [grams] ist die Menge jetzt.
 */
@Composable
internal fun ScaleDialog(name: String, grams: Double, unit: WeightUnit, onScale: (Double) -> Unit, onDismiss: () -> Unit) {
    val start = unit.fromGrams(grams).let { if (it >= 10) it.roundToLong().toDouble() else (it * 10).roundToLong() / 10.0 }
    var text by rememberSaveable { mutableStateOf(show(start)) }
    val value = parseNumber(text)
    val valid = value.isFinite() && value > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.dough_scale_amount), maxLines = 1) },
                        suffix = { Text(unit.label) },
                        singleLine = true,
                        isError = !valid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    RepeatIconButton(
                        onStep = { text = show(DoughScale.next(parseNumber(text), -1)) },
                        enabled = valid && value > DoughScale.step(value) / 2,
                        contentDescription = stringResource(R.string.dough_step_less, name, show(DoughScale.step(value)) + " " + unit.label),
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    ) {
                        Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
                    }
                    RepeatIconButton(
                        onStep = { text = show(DoughScale.next(parseNumber(text), +1)) },
                        enabled = true,
                        contentDescription = stringResource(R.string.dough_step_more, name, show(DoughScale.step(value)) + " " + unit.label),
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                }
                Text(
                    stringResource(R.string.dough_scale_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onScale(unit.toGrams(value)) }, enabled = valid) {
                Text(stringResource(R.string.dough_scale_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
