package com.moltobene.app.ui.schedule

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.moltobene.app.R
import com.moltobene.app.data.DoughRun
import com.moltobene.app.data.TimelinePhase
import com.moltobene.app.data.dough.DoughBakePlan
import com.moltobene.app.ui.components.NoticeCard
import com.moltobene.app.ui.components.NoticeKind
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.dough.AndroidDurationText
import com.moltobene.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Ein Start, der höchstens so weit zurückliegt, gilt noch als „jetzt“ (die Anzeige erneuert sich nur jede Minute). */
private const val GRACE_MS = 2 * 60_000L

/**
 * Uhrzeiten vor dem Start (0.77.0, Entwurf 7A): oben der Backtermin (antippen ändert Tag und Uhrzeit), darunter Start,
 * Gesamtdauer und jede Phase mit Beginn; Nachtschritte und schon vergangene Beginne stehen als Text dabei. Ohne
 * gewählten Backtermin gilt der früheste (auf die halbe Stunde aufgerundet), und „Teig starten“ beginnt wie bisher.
 * Mit gewähltem Termin gibt es „Planen“: Der Teig steht dann als geplanter Teig auf der Startseite.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BakePlanPanel(timeline: List<TimelinePhase>, now: Long, actions: ScheduleActions) {
    val context = LocalContext.current
    val chosen = actions.bakeAt
    val bake = chosen ?: DoughBakePlan.earliestBake(timeline, now)
    val slots = DoughBakePlan.slots(timeline, bake, now)
    val start = slots.first().start
    val flex = DoughBakePlan.flexIndex(timeline)
    val late = chosen != null && start < now - GRACE_MS
    var picking by rememberSaveable { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(stringResource(R.string.schedule_bake_label), style = MaterialTheme.typography.labelLarge)
            val bakeText = formatMoment(context, bake)
            val change = stringResource(R.string.schedule_bake_change, bakeText)
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.semantics { contentDescription = change }) {
                Text(bakeText)
            }
            Column(modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(stringResource(R.string.schedule_plan_start, formatMoment(context, start)), style = MaterialTheme.typography.titleMedium)
                val total = timeline.sumOf { it.maxMinutes }
                Text(
                    stringResource(R.string.schedule_plan_total, AndroidDurationText.format(context, total, total)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            timeline.forEachIndexed { index, phase -> SlotRow(context, phase, slots[index]) }
            if (late) {
                val started = DoughBakePlan.startNow(timeline, DoughRun(id = "", recipeId = "", startedAt = now), bake, now)
                val text = if (started.bakeAt == bake && flex != null) {
                    stringResource(R.string.schedule_late_kept, formatMoment(context, start), timeline[flex].text)
                } else {
                    stringResource(R.string.schedule_late_moved, formatMoment(context, start), formatMoment(context, started.bakeAt ?: bake))
                }
                NoticeCard(text = text, kind = NoticeKind.INFO)
            } else if (flex != null) {
                Text(
                    stringResource(R.string.schedule_flex, timeline[flex].text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(R.string.schedule_hint), style = MaterialTheme.typography.bodyMedium)
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (chosen != null && !late) {
            Button(onClick = actions.onPlan) { Text(stringResource(R.string.schedule_plan)) }
            OutlinedButton(onClick = actions.onStart) { Text(stringResource(R.string.schedule_start)) }
        } else {
            Button(onClick = actions.onStart) { Text(stringResource(R.string.schedule_start)) }
        }
    }
    if (picking) {
        BakeAtPicker(
            initial = bake,
            onPicked = {
                picking = false
                actions.onBakeAt(it)
            },
            onDismiss = { picking = false },
        )
    }
}

/** Ein geplanter Teig (noch nicht gestartet): Start und Backtermin, die Phasen mit Uhrzeit, „Jetzt starten“. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlannedRunView(timeline: List<TimelinePhase>, run: DoughRun, now: Long, onStartNow: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bake = run.bakeAt ?: DoughBakePlan.bakeWhenStarted(timeline, run.startedAt, 0)
    val slots = DoughBakePlan.slots(timeline, bake, now)
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(
                    stringResource(R.string.schedule_planned_title, formatMoment(context, run.startedAt)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.schedule_bake_planned, formatMoment(context, bake)), style = MaterialTheme.typography.bodyLarge)
            }
            if (run.startedAt < now - GRACE_MS) {
                NoticeCard(
                    text = stringResource(R.string.schedule_planned_missed, formatMoment(context, run.startedAt)),
                    kind = NoticeKind.INFO,
                )
            }
            timeline.forEachIndexed { index, phase -> SlotRow(context, phase, slots[index]) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Button(onClick = onStartNow) { Text(stringResource(R.string.schedule_start_now)) }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.schedule_remove)) }
            }
        }
    }
}

/** Phase mit Dauer, Ort und Beginn, z. B. „Beginn Fr., 20. Nov., 23:00 · nachts“. */
@Composable
private fun SlotRow(context: Context, phase: TimelinePhase, slot: DoughBakePlan.Slot) {
    val place = AndroidDurationText.place(context, phase.place)
    val duration = AndroidDurationText.format(context, phase.minMinutes, phase.maxMinutes)
    val detail = if (place.isEmpty()) duration else "$duration, $place"
    var line = stringResource(R.string.schedule_slot_start, formatMoment(context, slot.start))
    if (slot.past) line = stringResource(R.string.schedule_phase_line, line, stringResource(R.string.schedule_slot_past))
    if (slot.night) line = stringResource(R.string.schedule_phase_line, line, stringResource(R.string.schedule_slot_night))
    val spoken = stringResource(R.string.schedule_phase_semantics, phase.text, detail, line)
    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
        Text(phase.text, style = MaterialTheme.typography.bodyLarge)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            line,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (slot.night || slot.past) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/**
 * Backtermin wählen: erst der Tag, dann die Uhrzeit (Bausteine von Material 3, keine neue Bibliothek). Vergangene Tage
 * lassen sich nicht wählen. Bei großer Schrift die Uhrzeit als Eingabe statt als Zifferblatt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BakeAtPicker(initial: Long, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val initialLocal = remember(initial) { Instant.ofEpochMilli(initial).atZone(zone) }
    // Der Tag als Mitternacht in UTC, wie die Tagesauswahl von Material 3 ihn liefert.
    var day by rememberSaveable { mutableStateOf<Long?>(null) }
    val chosenDay = day
    if (chosenDay == null) {
        val today = remember { LocalDate.now(zone).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialLocal.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= today
                override fun isSelectableYear(year: Int): Boolean = year >= LocalDate.now(zone).year
            },
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { day = state.selectedDateMillis }, enabled = state.selectedDateMillis != null) {
                    Text(stringResource(R.string.schedule_date_next))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    } else {
        val state = rememberTimePickerState(
            initialHour = initialLocal.hour,
            initialMinute = initialLocal.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        val largeText = isLargeText()
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.schedule_time_title)) },
            text = { if (largeText) TimeInput(state = state) else TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val date = Instant.ofEpochMilli(chosenDay).atZone(ZoneOffset.UTC).toLocalDate()
                    onPicked(date.atTime(state.hour, state.minute).atZone(zone).toInstant().toEpochMilli())
                }) { Text(stringResource(R.string.schedule_time_set)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
