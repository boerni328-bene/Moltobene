package com.moltobene.app.ui.schedule

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.moltobene.app.R
import com.moltobene.app.data.DoughRun
import com.moltobene.app.data.TimelinePhase
import com.moltobene.app.data.dough.DoughRunPlanner
import com.moltobene.app.ui.dough.AndroidDurationText
import com.moltobene.app.ui.theme.Spacing

/** Aktionen der Karte „Zeitplan“ (#71). */
class ScheduleActions(
    val onStart: () -> Unit,
    val onComplete: (DoughRun) -> Unit,
    val onUndo: (DoughRun) -> Unit,
    val onRemove: (DoughRun) -> Unit,
)

/** Zeitpunkt wie auf dem Handy eingestellt (12 oder 24 Stunden), mit Wochentag und Datum, z. B. „Sa., 17. Okt., 08:30“. */
fun formatMoment(context: Context, millis: Long): String = DateUtils.formatDateTime(
    context,
    millis,
    DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY or
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR,
)

/**
 * Die aktuelle Zeit, einmal pro Minute erneuert, aber nur solange die Ansicht sichtbar ist: kein Sekunden-Countdown,
 * kein Dienst, kein Wecker (#71).
 */
@Composable
fun rememberNow(): Long {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now.longValue = System.currentTimeMillis()
                kotlinx.coroutines.delay(60_000L - now.longValue % 60_000L)
            }
        }
    }
    return now.longValue
}

/**
 * Der Zeitplan eines Teigs im Rezept (#71): ohne Lauf die Phasen mit Dauer und dem Knopf „Teig starten“, mit Lauf
 * jede Phase mit „erledigt“, „aktuell“ oder „folgt“ und den Zeiten, dazu oben der Stand: was jetzt ist und wann es
 * weitergeht. Der Zustand steht immer auch als Text, nie nur über Farbe.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleCard(
    timeline: List<TimelinePhase>,
    runs: List<DoughRun>,
    now: Long,
    actions: ScheduleActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var removing by remember { mutableStateOf<DoughRun?>(null) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(R.string.schedule_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = Spacing.m).semantics { heading() },
        )
        if (runs.isEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    timeline.forEach { phase -> PhaseText(phase, context) }
                    Text(stringResource(R.string.schedule_hint), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        runs.forEach { run ->
            RunView(
                timeline = timeline,
                run = run,
                now = now,
                showStart = runs.size > 1,
                actions = actions,
                onRemove = { removing = run },
            )
        }
        if (runs.isEmpty()) {
            Button(onClick = actions.onStart) { Text(stringResource(R.string.schedule_start)) }
        } else {
            OutlinedButton(onClick = actions.onStart) { Text(stringResource(R.string.schedule_start)) }
        }
    }
    removing?.let { run ->
        AlertDialog(
            onDismissRequest = { removing = null },
            text = { Text(stringResource(R.string.schedule_remove_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    actions.onRemove(run)
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Phase mit Dauer und Ort, z. B. „Biga reifen lassen (1 bis 3 Tage, im Kühlschrank)“. */
@Composable
private fun PhaseText(phase: TimelinePhase, context: Context) {
    val place = AndroidDurationText.place(context, phase.place)
    val duration = AndroidDurationText.format(context, phase.minMinutes, phase.maxMinutes)
    Column {
        Text(phase.text, style = MaterialTheme.typography.bodyLarge)
        Text(
            if (place.isEmpty()) duration else "$duration, $place",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunView(
    timeline: List<TimelinePhase>,
    run: DoughRun,
    now: Long,
    showStart: Boolean,
    actions: ScheduleActions,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val state = DoughRunPlanner.state(timeline, run)
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            if (showStart) {
                Text(
                    stringResource(R.string.schedule_started, formatMoment(context, run.startedAt)),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            RunStatus(timeline, state, now)
            timeline.forEachIndexed { index, phase ->
                val time = state.phases[index]
                val current = index == state.current
                val label = stringResource(
                    when {
                        time.done -> R.string.schedule_state_done
                        current -> R.string.schedule_state_current
                        else -> R.string.schedule_state_upcoming
                    },
                )
                val whenText = when {
                    time.done -> stringResource(R.string.schedule_done_at, formatMoment(context, time.endEarliest))
                    time.endEarliest == time.endLatest -> formatMoment(context, time.endEarliest)
                    else -> stringResource(
                        R.string.schedule_phase_range,
                        formatMoment(context, time.endEarliest),
                        formatMoment(context, time.endLatest),
                    )
                }
                val line = stringResource(R.string.schedule_phase_line, label, whenText)
                val place = AndroidDurationText.place(context, phase.place)
                val duration = AndroidDurationText.format(context, phase.minMinutes, phase.maxMinutes)
                val spoken = stringResource(
                    R.string.schedule_phase_semantics,
                    phase.text,
                    if (place.isEmpty()) duration else "$duration, $place",
                    line,
                )
                Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
                    Text(
                        phase.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        if (place.isEmpty()) duration else "$duration, $place",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(line, style = MaterialTheme.typography.bodyMedium, fontWeight = if (current) FontWeight.Bold else FontWeight.Normal)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (!state.finished) {
                    Button(onClick = { actions.onComplete(run) }) { Text(stringResource(R.string.schedule_complete)) }
                }
                if (run.doneAt.isNotEmpty()) {
                    TextButton(onClick = { actions.onUndo(run) }) { Text(stringResource(R.string.schedule_undo)) }
                }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.schedule_remove)) }
            }
        }
    }
}

/** Der Stand in zwei Sätzen: was jetzt ist und wann es weitergeht. Der Screenreader liest Änderungen vor. */
@Composable
private fun RunStatus(timeline: List<TimelinePhase>, state: DoughRunPlanner.State, now: Long) {
    val context = LocalContext.current
    Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        if (state.finished) {
            Text(stringResource(R.string.schedule_finished), style = MaterialTheme.typography.titleMedium)
            return@Column
        }
        val time = state.phases[state.current]
        Text(
            stringResource(R.string.schedule_now, timeline[state.current].text),
            style = MaterialTheme.typography.titleMedium,
        )
        val next = when {
            DoughRunPlanner.overdue(state, now) -> stringResource(R.string.schedule_overdue, formatMoment(context, time.endLatest))
            now >= time.endEarliest -> stringResource(R.string.schedule_ready, formatMoment(context, time.endLatest))
            time.endEarliest == time.endLatest -> stringResource(R.string.schedule_next_at, formatMoment(context, time.endEarliest))
            else -> stringResource(
                R.string.schedule_next_range,
                formatMoment(context, time.endEarliest),
                formatMoment(context, time.endLatest),
            )
        }
        Text(next, style = MaterialTheme.typography.bodyLarge)
    }
}
