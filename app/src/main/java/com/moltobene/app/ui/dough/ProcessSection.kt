package com.moltobene.app.ui.dough

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.StepperSide
import com.moltobene.app.data.TemperatureUnit
import com.moltobene.app.data.dough.DoughKitchen
import com.moltobene.app.data.dough.DoughNumbers
import com.moltobene.app.data.dough.DoughProcess
import com.moltobene.app.data.dough.DoughProcessRules
import com.moltobene.app.data.dough.DoughType
import com.moltobene.app.data.dough.Kneading
import com.moltobene.app.data.dough.Preferment
import com.moltobene.app.data.dough.RestPlace
import com.moltobene.app.data.dough.RestTime
import com.moltobene.app.data.dough.TemperatureField
import com.moltobene.app.ui.components.RepeatIconButton
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

/** Eine Auswahl in einer aufgeklappten Zeile, z. B. „Von Hand | Mit Maschine“. */
internal class ProcessChoice(val options: List<String>, val selected: Int, val onSelect: (Int) -> Unit)

/**
 * Ein Wert mit Minus und Plus in einer aufgeklappten Zeile; [label] steht klein im Rahmen, [spoken] liest der
 * Screenreader statt der Abkürzung (z. B. „1 Stunde“ statt „1 Std.“).
 */
internal class ProcessStepper(
    val label: String,
    val value: String,
    val spoken: String,
    val changed: Boolean,
    val canLess: Boolean,
    val canMore: Boolean,
    val onLess: () -> Unit,
    val onMore: () -> Unit,
    val lessDescription: String,
    val moreDescription: String,
)

/**
 * Eine Zeile der ruhigen Liste (Entwurf 10C, Wahl des Projektinhabers vom 12.11.2026): zugeklappt Name, Wert und klein
 * der Ort oder die Knetart; aufgeklappt die Auswahl und die Felder mit Minus und Plus. [changed]: selbst geändert, fett.
 */
internal class ProcessRow(
    val key: String,
    val label: String,
    val value: String,
    val spoken: String,
    val changed: Boolean,
    val suffix: String?,
    val steppers: List<ProcessStepper>,
    val choice: ProcessChoice? = null,
    val note: String? = null,
)

/**
 * Bereich „Gehzeiten und Temperaturen“ (Wunsch vom 05./06.11.2026): zuerst nur die Werte Zeile für Zeile; Antippen
 * öffnet in dieser Zeile Minus und Plus (und wo nötig die Auswahl), nochmals Antippen schließt. Darunter der Hinweis,
 * dass sich die Gehzeiten nach dem Raum richten, der Weg zu °C oder °F in den Einstellungen und Warnungen zum Wasser.
 */
@Composable
internal fun ProcessList(
    rows: List<ProcessRow>,
    openKey: String,
    onToggle: (String) -> Unit,
    warning: String?,
    onUnits: () -> Unit,
) {
    Column {
        for (row in rows) ProcessRowView(row, open = row.key == openKey, onToggle = { onToggle(row.key) })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val unitsDescription = stringResource(R.string.dough_process_units_description)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.xs)) {
            Text(
                stringResource(R.string.dough_process_adapts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUnits, modifier = Modifier.semantics { contentDescription = unitsDescription }) {
                Text(stringResource(R.string.dough_process_units))
            }
        }
        if (warning != null) {
            Text(
                warning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun ProcessRowView(row: ProcessRow, open: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state = stringResource(if (open) R.string.basics_expanded else R.string.basics_collapsed)
    val spoken = listOfNotNull(row.label, row.spoken.takeIf { !open }, row.suffix.takeIf { !open }).joinToString(", ")
    val large = isLargeText()
    Column {
        HorizontalDivider(color = colors.outlineVariant)
        val value: @Composable () -> Unit = {
            if (!open) {
                Column(horizontalAlignment = if (large) Alignment.Start else Alignment.End) {
                    Text(
                        row.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (row.changed) FontWeight.Bold else null,
                    )
                    if (row.suffix != null) {
                        Text(row.suffix, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics {
                    contentDescription = spoken
                    stateDescription = state
                }
                .padding(vertical = Spacing.xs),
        ) {
            if (large) {
                // Bei großer Schrift steht der Wert unter dem Namen, damit nichts abgeschnitten wird.
                Column(modifier = Modifier.weight(1f)) {
                    Text(row.label, style = MaterialTheme.typography.bodyLarge)
                    value()
                }
            } else {
                Text(row.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                value()
            }
            Icon(
                if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
        if (open) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.padding(bottom = Spacing.s),
            ) {
                row.choice?.let { choice ->
                    SubtleChoice(
                        label = "",
                        options = choice.options.indices.toList(),
                        selected = choice.selected,
                        optionLabel = { choice.options[it] },
                        onSelect = choice.onSelect,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                    )
                }
                // Zwei Felder stehen nebeneinander (Raum und Kühlschrank), bei großer Schrift untereinander.
                if (large || row.steppers.size == 1) {
                    for (stepper in row.steppers) StepperBox(stepper, Modifier.fillMaxWidth())
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        for (stepper in row.steppers) StepperBox(stepper, Modifier.weight(1f))
                    }
                }
                if (row.note != null) {
                    Text(row.note, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Rahmen mit kleinem Namen und Wert, daneben Minus und Plus, rechts oder links nach der Einstellung „Plus und Minus“
 * (wie die übrigen Felder des Teigrechners).
 */
@Composable
private fun StepperBox(stepper: ProcessStepper, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val left = LocalStepperSide.current == StepperSide.LEFT
    val described = stringResource(R.string.dough_process_row, stepper.label, stepper.spoken)
    val buttons: @Composable () -> Unit = {
        RepeatIconButton(onStep = stepper.onLess, enabled = stepper.canLess, contentDescription = stepper.lessDescription) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
        }
        RepeatIconButton(onStep = stepper.onMore, enabled = stepper.canMore, contentDescription = stepper.moreDescription) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (left) buttons()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.s, vertical = Spacing.xs)
                    .semantics(mergeDescendants = true) {
                        contentDescription = described
                        liveRegion = LiveRegionMode.Polite
                    },
                horizontalAlignment = if (left) Alignment.End else Alignment.Start,
            ) {
                Text(stepper.label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text(
                    stepper.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (stepper.changed) FontWeight.Bold else null,
                )
            }
            if (!left) buttons()
        }
    }
}

/**
 * Gehzeit kurz wie im Entwurf 10C: ganze Stunden „24 Std.“, unter 2 Stunden in Minuten „45 Min.“, sonst mit einer
 * Stelle „2,5 Std.“; das Dezimalzeichen folgt der Sprache.
 */
internal fun hoursShort(res: Resources, hours: Double, language: String): String {
    val minutes = (hours * 60).roundToInt()
    return when {
        minutes % 60 == 0 -> res.getString(R.string.dough_hours_short, (minutes / 60).toString())
        minutes < 120 -> res.getString(R.string.dough_minutes_short, minutes.toString())
        else -> res.getString(R.string.dough_hours_short, DoughNumbers.separator(DoughNumbers.plain(hours, 1), language))
    }
}

/** Gehzeit zum Vorlesen, z. B. „1 Stunde“, „45 Minuten“, „2 Tage“. */
internal fun hoursSpoken(res: Resources, hours: Double): String {
    val minutes = (hours * 60).roundToInt().coerceAtLeast(1)
    return AndroidDurationText.format(res, minutes, minutes)
}

/** Gesamtzeit kurz, z. B. „45 Min.“, „4 Std.“ oder „29 Std. 55 Min.“. */
internal fun totalShort(res: Resources, minutes: Int): String = when {
    minutes < 60 -> res.getString(R.string.dough_minutes_short, minutes.toString())
    minutes % 60 == 0 -> res.getString(R.string.dough_hours_short, (minutes / 60).toString())
    else -> res.getString(R.string.dough_hours_minutes_short, (minutes / 60).toString(), (minutes % 60).toString())
}

/** Selbst geänderte Temperatur: weicht vom Standardwert ab (mit Spielraum für umgerechnete °F-Werte). */
private const val CHANGED_TOLERANCE = 0.01

/** Grenzen der Temperaturfelder mit Spielraum. */
private const val LIMIT_TOLERANCE = 1e-9

/** Kennungen der Zeilen; welche offen ist, wird gemerkt. */
internal const val ROW_KITCHEN = "kitchen"
internal const val ROW_RIPEN = "ripen"
internal const val ROW_KNEAD = "knead"
internal const val ROW_BULK = "bulk"
internal const val ROW_PROOF = "proof"
internal const val ROW_WARM = "warm"
internal const val ROW_OVEN = "oven"

/**
 * Die Zeilen des Bereichs in der Reihenfolge des Ablaufs (Entwurf 10C): Raum und Kühlschrank, Vorteig reifen lassen,
 * Teig nach dem Kneten (mit Knetart und Wasser), Teig gehen lassen, Teigkugeln bzw. Laib gehen lassen (mit Ort),
 * Teigkugeln aufwärmen (Pizza aus dem Kühlschrank) und Backofen. Angezeigt werden die wirksamen Gehzeiten.
 */
@Suppress("LongParameterList", "LongMethod")
internal fun processRows(
    res: Resources,
    language: String,
    type: DoughType,
    preferment: Preferment,
    prefermentName: String,
    process: DoughProcess,
    kitchen: DoughKitchen,
    unit: TemperatureUnit,
    waterText: String?,
    onProcess: (DoughProcess) -> Unit,
    onKitchen: (DoughKitchen) -> Unit,
): List<ProcessRow> {
    val effective = DoughProcessRules.effective(process, kitchen, type, preferment)
    val defaults = DoughProcessRules.defaults(type, preferment)
    val risingTime = res.getString(R.string.dough_process_rising_time)
    val roomShort = res.getString(R.string.dough_place_room_short)
    val fridgeShort = res.getString(R.string.dough_place_fridge_short)

    /** [label] nennt der Screenreader bei Minus und Plus; [boxLabel] steht klein im Rahmen. */
    fun temperature(
        label: String,
        field: TemperatureField,
        value: Double,
        default: Double,
        boxLabel: String = label,
        set: (Double) -> Unit,
    ): ProcessStepper {
        val step = unit.formatStep(field.step(unit))
        val shown = unit.format(value)
        return ProcessStepper(
            label = boxLabel,
            value = shown,
            spoken = shown,
            changed = abs(value - default) > CHANGED_TOLERANCE,
            canLess = value > field.min + LIMIT_TOLERANCE,
            canMore = value < field.max - LIMIT_TOLERANCE,
            onLess = { set(field.next(value, -1, unit)) },
            onMore = { set(field.next(value, +1, unit)) },
            lessDescription = res.getString(R.string.dough_step_less, label, step),
            moreDescription = res.getString(R.string.dough_step_more, label, step),
        )
    }

    fun timeStepper(rowLabel: String, time: RestTime): ProcessStepper {
        val hours = effective.hours(time)
        return ProcessStepper(
            label = risingTime,
            value = hoursShort(res, hours, language),
            spoken = hoursSpoken(res, hours),
            changed = time in process.manual,
            canLess = DoughProcessRules.canStep(process, kitchen, type, preferment, time, -1),
            canMore = DoughProcessRules.canStep(process, kitchen, type, preferment, time, +1),
            onLess = { onProcess(DoughProcessRules.step(process, kitchen, type, preferment, time, -1)) },
            onMore = { onProcess(DoughProcessRules.step(process, kitchen, type, preferment, time, +1)) },
            lessDescription = res.getString(R.string.dough_process_row, rowLabel, res.getString(R.string.dough_process_shorter, risingTime)),
            moreDescription = res.getString(R.string.dough_process_row, rowLabel, res.getString(R.string.dough_process_longer, risingTime)),
        )
    }

    fun timeRow(key: String, label: String, time: RestTime, suffix: String, choice: ProcessChoice? = null): ProcessRow {
        val stepper = timeStepper(label, time)
        return ProcessRow(key, label, stepper.value, stepper.spoken, stepper.changed, suffix, listOf(stepper), choice)
    }

    val rows = mutableListOf<ProcessRow>()
    val roomLabel = res.getString(R.string.dough_process_room)
    val fridgeLabel = res.getString(R.string.dough_process_fridge)
    val room = temperature(roomLabel, TemperatureField.ROOM, kitchen.room, DoughKitchen.DEFAULT_ROOM) { onKitchen(kitchen.copy(room = it)) }
    val fridge = temperature(fridgeLabel, TemperatureField.FRIDGE, kitchen.fridge, DoughKitchen.DEFAULT_FRIDGE) {
        onKitchen(kitchen.copy(fridge = it))
    }
    rows += ProcessRow(
        key = ROW_KITCHEN,
        label = res.getString(R.string.dough_process_kitchen),
        value = room.value + SUMMARY_SEPARATOR + fridge.value,
        spoken = listOf(
            res.getString(R.string.dough_process_row, roomLabel, room.spoken),
            res.getString(R.string.dough_process_row, fridgeLabel, fridge.spoken),
        ).joinToString(", "),
        changed = room.changed || fridge.changed,
        suffix = null,
        steppers = listOf(room, fridge),
        note = res.getString(R.string.dough_process_kitchen_note),
    )
    if (preferment != Preferment.NONE) {
        val place = if (preferment == Preferment.BIGA) fridgeShort else roomShort
        rows += timeRow(ROW_RIPEN, res.getString(R.string.phase_preferment_ripen, prefermentName), RestTime.RIPEN, place)
    }
    val kneading = res.getString(if (kitchen.kneading == Kneading.MACHINE) R.string.dough_knead_machine_lower else R.string.dough_knead_hand_lower)
    val doughTemp = temperature(
        res.getString(R.string.dough_process_dough_temperature), TemperatureField.DOUGH, process.doughTemp, defaults.doughTemp,
    ) { onProcess(process.copy(doughTemp = it)) }
    rows += ProcessRow(
        key = ROW_KNEAD,
        label = res.getString(R.string.dough_process_dough_temp),
        value = doughTemp.value,
        spoken = doughTemp.spoken,
        changed = doughTemp.changed,
        suffix = if (waterText == null) kneading else res.getString(R.string.dough_process_knead_suffix, kneading, waterText),
        steppers = listOf(doughTemp),
        choice = ProcessChoice(
            options = listOf(res.getString(R.string.dough_knead_hand), res.getString(R.string.dough_knead_machine)),
            selected = if (kitchen.kneading == Kneading.MACHINE) 1 else 0,
            onSelect = { onKitchen(kitchen.copy(kneading = if (it == 1) Kneading.MACHINE else Kneading.HAND)) },
        ),
        note = waterText?.let {
            res.getString(if (preferment == Preferment.NONE) R.string.dough_process_water_plain else R.string.dough_process_water, it)
        },
    )
    rows += timeRow(ROW_BULK, res.getString(R.string.phase_bulk), RestTime.BULK, roomShort)
    val cold = process.proofPlace == RestPlace.FRIDGE
    val pizza = type == DoughType.PIZZA
    rows += timeRow(
        ROW_PROOF,
        res.getString(if (pizza) R.string.phase_proof_balls else R.string.phase_proof_loaf),
        RestTime.PROOF,
        if (cold) fridgeShort else roomShort,
        ProcessChoice(
            options = listOf(res.getString(R.string.dough_place_room_choice), res.getString(R.string.dough_place_fridge_choice)),
            selected = if (cold) 1 else 0,
            onSelect = { onProcess(DoughProcessRules.withPlace(process, type, preferment, if (it == 1) RestPlace.FRIDGE else RestPlace.ROOM)) },
        ),
    )
    if (DoughProcessRules.applies(RestTime.WARM, type, preferment, process.proofPlace)) {
        rows += timeRow(ROW_WARM, res.getString(R.string.phase_warm_up), RestTime.WARM, roomShort)
    }
    val ovenLabel = res.getString(R.string.dough_process_oven)
    val oven = temperature(
        ovenLabel, TemperatureField.OVEN, process.ovenTemp, defaults.ovenTemp, res.getString(R.string.dough_process_temperature),
    ) { onProcess(process.copy(ovenTemp = it)) }
    rows += ProcessRow(ROW_OVEN, ovenLabel, oven.value, oven.spoken, oven.changed, null, listOf(oven))
    return rows
}
