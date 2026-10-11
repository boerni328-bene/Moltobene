package com.moltobene.app.ui.recipe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.moltobene.app.data.Pan
import com.moltobene.app.data.PanScaling
import com.moltobene.app.ui.theme.Spacing
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Eine Zutat, nach der umgerechnet werden kann (#66).
 * @param line die Zeile im Original; nach ihr wird gerechnet
 * @param shown die Zeile, wie sie angezeigt wird (übersetzt oder Original)
 * @param amount die Menge am Anfang der Zeile im Original
 * @param unit Einheit hinter dem Eingabefeld, z. B. „g“; null bei „3 Eier“
 */
internal class AdjustChoice(val index: Int, val line: String, val shown: String, val amount: Double, val unit: String?)

private enum class AdjustStep { WAYS, SERVINGS, INGREDIENTS, AMOUNT, PAN }

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
    recipePan: Pair<Pan, Boolean>?,
    onServings: (Int) -> Unit,
    onIngredient: (AdjustChoice, Double) -> Unit,
    onPan: (from: Pan, to: Pan, inch: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    // „Nach Backform“ gibt es immer; die anderen Wege nur, wenn das Rezept sie hergibt.
    val start = AdjustStep.WAYS
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
                    if (servings != null) {
                        ChoiceRow(stringResource(R.string.adjust_by_servings)) {
                            input = servings.toString()
                            step = AdjustStep.SERVINGS
                        }
                        HorizontalDivider()
                    }
                    if (choices.isNotEmpty()) {
                        ChoiceRow(stringResource(R.string.adjust_by_ingredient)) { step = AdjustStep.INGREDIENTS }
                        HorizontalDivider()
                    }
                    ChoiceRow(stringResource(R.string.adjust_by_pan)) { step = AdjustStep.PAN }
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

        AdjustStep.PAN -> PanStep(recipePan = recipePan, onPan = onPan, onDismiss = onDismiss)

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

/**
 * „Nach Backform“ (#67): Form im Rezept (aus der Einheit der Portionen vorausgefüllt, sonst eintippen) und eigene Form,
 * jeweils rund mit Durchmesser oder eckig mit Länge × Breite, in cm oder Zoll. Ist die Angabe im Rezept nicht
 * eindeutig, bleibt das Feld leer: Die App rät nie still.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PanStep(recipePan: Pair<Pan, Boolean>?, onPan: (Pan, Pan, Boolean) -> Unit, onDismiss: () -> Unit) {
    var round by rememberSaveable { mutableStateOf(recipePan?.first?.round ?: true) }
    var inch by rememberSaveable { mutableStateOf(recipePan?.second ?: false) }
    var fromA by rememberSaveable { mutableStateOf(recipePan?.first?.let { panText(it.a, inch) }.orEmpty()) }
    var fromB by rememberSaveable { mutableStateOf(recipePan?.first?.takeIf { !it.round }?.let { panText(it.b, inch) }.orEmpty()) }
    var toA by rememberSaveable { mutableStateOf("") }
    var toB by rememberSaveable { mutableStateOf("") }

    fun pan(a: String, b: String): Pan? {
        val first = AmountScaling.parseInput(a)?.let { PanScaling.toCm(it, inch) } ?: return null
        val second = if (round) first else AmountScaling.parseInput(b)?.let { PanScaling.toCm(it, inch) } ?: return null
        return (if (round) Pan.round(first) else Pan.rectangle(first, second)).takeIf { PanScaling.isValid(it) }
    }
    val from = pan(fromA, fromB)
    val to = pan(toA, toB)
    val factor = if (from != null && to != null) PanScaling.factor(from, to) else null
    val unit = stringResource(if (inch) R.string.adjust_pan_unit_inch else R.string.adjust_pan_unit_cm)
    val filled = fromA.isNotBlank() && toA.isNotBlank() && (round || (fromB.isNotBlank() && toB.isNotBlank()))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.adjust_by_pan)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    FilterChip(selected = round, onClick = { round = true }, label = { Text(stringResource(R.string.adjust_pan_round)) })
                    FilterChip(selected = !round, onClick = { round = false }, label = { Text(stringResource(R.string.adjust_pan_rectangle)) })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    FilterChip(selected = !inch, onClick = { inch = false }, label = { Text(stringResource(R.string.adjust_pan_unit_cm)) })
                    FilterChip(selected = inch, onClick = { inch = true }, label = { Text(stringResource(R.string.adjust_pan_unit_inch)) })
                }
                PanFields(stringResource(R.string.adjust_pan_in_recipe), round, unit, fromA, fromB, { fromA = it }, { fromB = it })
                PanFields(stringResource(R.string.adjust_pan_own), round, unit, toA, toB, { toA = it }, { toB = it })
                if (filled && factor == null) {
                    Text(
                        text = stringResource(R.string.adjust_pan_invalid, PanScaling.MIN_CM.roundToInt(), PanScaling.MAX_CM.roundToInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (from != null && to != null) onPan(from, to, inch) }, enabled = factor != null) {
                Text(stringResource(R.string.adjust_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Ein Durchmesser oder Länge und Breite, mit Überschrift. */
@Composable
private fun PanFields(
    title: String,
    round: Boolean,
    unit: String,
    a: String,
    b: String,
    onA: (String) -> Unit,
    onB: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = a,
            onValueChange = { onA(it.take(INPUT_LENGTH)) },
            label = { Text(stringResource(if (round) R.string.adjust_pan_diameter else R.string.adjust_pan_length)) },
            suffix = { Text(unit) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!round) {
            OutlinedTextField(
                value = b,
                onValueChange = { onB(it.take(INPUT_LENGTH)) },
                label = { Text(stringResource(R.string.adjust_pan_width)) },
                suffix = { Text(unit) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Ein Maß zum Ändern in der gewählten Einheit, z. B. „26“ oder „9“. */
private fun panText(cm: Double, inch: Boolean): String = inputText(if (inch) cm / PanScaling.CM_PER_INCH else cm, ',')
