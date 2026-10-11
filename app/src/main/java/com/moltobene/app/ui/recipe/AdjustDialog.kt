package com.moltobene.app.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.Adjustment
import com.moltobene.app.data.AmountScaling
import com.moltobene.app.ui.theme.Spacing
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Eine Zutat, nach der umgerechnet werden kann (#66).
 * @param line die Zeile im Original; nach ihr wird gerechnet
 * @param shown die Zeile, wie sie angezeigt wird (übersetzt oder Original)
 * @param amount die Menge am Anfang der Zeile im Original
 * @param unit Einheit hinter dem Eingabefeld, z. B. „g“; null bei „3 Eier“
 */
internal class AdjustChoice(val index: Int, val line: String, val shown: String, val amount: Double, val unit: String?)

private enum class AdjustStep { WAYS, SERVINGS, INGREDIENTS, AMOUNT }

/**
 * „Umrechnen …“ (#66): Die Wege stehen untereinander als Liste – „Nach Portionen“ und „Nach Zutat“ –, damit sie auch
 * bei 200 % Schrift passen. Gibt es nur einen Weg, geht es gleich dorthin. Eingaben sind begrenzt: keine 0, nichts
 * Leeres, höchstens 999 Portionen bzw. das Hundertfache einer Zutat.
 * @param servings angezeigte Portionen; null, wenn das Rezept keine hat
 * @param decimalSeparator Dezimalzeichen für die vorausgefüllte Menge (die Eingabe nimmt Komma und Punkt)
 */
@Composable
internal fun AdjustDialog(
    servings: Int?,
    servingsUnit: String?,
    choices: List<AdjustChoice>,
    decimalSeparator: Char,
    onServings: (Int) -> Unit,
    onIngredient: (AdjustChoice, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = when {
        servings != null && choices.isNotEmpty() -> AdjustStep.WAYS
        choices.isNotEmpty() -> AdjustStep.INGREDIENTS
        else -> AdjustStep.SERVINGS
    }
    var step by rememberSaveable { mutableStateOf(start) }
    var chosen by rememberSaveable { mutableStateOf<Int?>(null) }
    var input by rememberSaveable { mutableStateOf(servings?.toString().orEmpty()) }
    val choice = choices.firstOrNull { it.index == chosen }

    when (step) {
        AdjustStep.WAYS -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.adjust_title)) },
            text = {
                Column {
                    ChoiceRow(stringResource(R.string.adjust_by_servings)) {
                        input = servings?.toString().orEmpty()
                        step = AdjustStep.SERVINGS
                    }
                    HorizontalDivider()
                    ChoiceRow(stringResource(R.string.adjust_by_ingredient)) { step = AdjustStep.INGREDIENTS }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        )

        AdjustStep.SERVINGS -> {
            val value = input.trim().toIntOrNull()
            val valid = value != null && value in 1..Adjustment.MAX_SERVINGS
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.adjust_by_servings)) },
                text = {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.take(INPUT_LENGTH) },
                        label = { Text(servingsUnit ?: stringResource(R.string.adjust_servings_label)) },
                        singleLine = true,
                        isError = input.isNotBlank() && !valid,
                        supportingText = if (input.isNotBlank() && !valid) {
                            { Text(stringResource(R.string.adjust_servings_invalid, 1, Adjustment.MAX_SERVINGS)) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { value?.let(onServings) }, enabled = valid) {
                        Text(stringResource(R.string.adjust_confirm))
                    }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }

        AdjustStep.INGREDIENTS -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.adjust_by_ingredient)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.adjust_choose_ingredient),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = Spacing.s),
                    )
                    choices.forEachIndexed { position, item ->
                        if (position > 0) HorizontalDivider()
                        ChoiceRow(item.shown) {
                            chosen = item.index
                            input = inputText(item.amount, decimalSeparator)
                            step = AdjustStep.AMOUNT
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        )

        AdjustStep.AMOUNT -> {
            if (choice == null) {
                // Das Rezept hat sich inzwischen geändert: wieder von vorn.
                LaunchedEffect(Unit) { step = start }
                return
            }
            val amount = AmountScaling.parseInput(input)
            val valid = amount != null && Adjustment.isAllowed(amount, choice.amount)
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.adjust_by_ingredient)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(choice.shown, style = MaterialTheme.typography.bodyLarge)
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.take(INPUT_LENGTH) },
                            label = { Text(stringResource(R.string.adjust_amount_label)) },
                            suffix = if (choice.unit != null) {
                                { Text(choice.unit) }
                            } else {
                                null
                            },
                            singleLine = true,
                            isError = input.isNotBlank() && !valid,
                            supportingText = if (input.isNotBlank() && !valid) {
                                { Text(stringResource(R.string.adjust_amount_invalid)) }
                            } else {
                                null
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { if (amount != null) onIngredient(choice, amount) }, enabled = valid) {
                        Text(stringResource(R.string.adjust_confirm))
                    }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

/** Ein Eintrag zum Antippen: die ganze Zeile, mindestens 48 dp hoch. */
@Composable
private fun ChoiceRow(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.s),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Die Menge zum Ändern, z. B. „500“ oder „1,5“ – ohne Tausenderpunkt, damit sie eindeutig bleibt. */
private fun inputText(value: Double, decimalSeparator: Char): String {
    val whole = value.roundToLong()
    if (abs(value - whole) < 0.0005) return whole.toString()
    return "%.2f".format(Locale.ROOT, value).trimEnd('0').trimEnd('.').replace('.', decimalSeparator)
}

/** Längere Eingaben sind keine Menge. */
private const val INPUT_LENGTH = 12
