package com.moltobene.app.ui.schedule

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.moltobene.app.R
import com.moltobene.app.data.DoughRun
import com.moltobene.app.data.TimelinePhase
import com.moltobene.app.data.dough.DoughBakePlan
import com.moltobene.app.data.dough.DoughRunPlanner
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.dough.AndroidDurationText
import com.moltobene.app.ui.theme.Spacing

/** Aktionen der Karte „Zeitplan“ (#71); [bakeAt] ist der gewählte Backtermin (0.77.0), null heißt: so früh wie möglich. */
class ScheduleActions(
    val bakeAt: Long?,
    val onBakeAt: (Long) -> Unit,
    val onStart: () -> Unit,
    val onPlan: () -> Unit,
    val onStartPlanned: (DoughRun) -> Unit,
    val onComplete: (DoughRun) -> Unit,
    val onUndo: (DoughRun) -> Unit,
    val onRemove: (DoughRun) -> Unit,
    /** Dauer einer Phase nur für diesen Teig ändern (0.78.0): Teig, Nummer der Phase, Minuten. */
    val onDuration: (DoughRun, Int, Int) -> Unit,
    /** Korrigieren, wann eine erledigte Phase fertig war (0.78.0). */
    val onDoneAt: (DoughRun, Int, Long) -> Unit,
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
    var removing by remember { mutableStateOf<DoughRun?>(null) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(R.string.schedule_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = Spacing.m).semantics { heading() },
        )
        if (runs.isEmpty()) BakePlanPanel(timeline, now, actions)
        runs.forEach { run ->
            if (run.planned) {
                PlannedRunView(timeline, run, now, onStartNow = { actions.onStartPlanned(run) }, onRemove = { removing = run })
            } else {
                RunView(
                    timeline = timeline,
                    run = run,
                    now = now,
                    showStart = runs.size > 1,
                    actions = actions,
                    onRemove = { removing = run },
                )
            }
        }
        if (runs.isNotEmpty()) {
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
                }) { Text(stringResource(R.string.schedule_remove_action)) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.schedule_keep)) } },
        )
    }
}

/**
 * Ein laufender Teig als Zeitleiste (Entwurf G4, gewählt am 20.11.2026): der Tag als Trenner, jeder Schritt eine Zeile
 * mit Beginn, Name und Dauer. Der aktuelle Schritt ist aufgeklappt: Ort, wann es weitergeht, Dauer zum Ändern und die
 * Knöpfe „Fertig“, „Zurück“, „Abbrechen“ in einer Reihe. Ein künftiger Schritt klappt beim Antippen seine Dauer auf,
 * ein erledigter ändert die Uhrzeit, wann er fertig war. Änderungen gelten nur für diesen Teig, nie für das Rezept.
 */
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
    val phases = DoughBakePlan.adjusted(timeline, run.stretchMinutes, run.durations)
    var open by rememberSaveable(run.id) { mutableIntStateOf(-1) }
    var correcting by rememberSaveable(run.id) { mutableIntStateOf(-1) }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (showStart) {
                Text(stringResource(R.string.schedule_started, formatMoment(context, run.startedAt)), style = MaterialTheme.typography.labelLarge)
            }
            run.bakeAt?.let { bake ->
                Text(stringResource(R.string.schedule_bake_planned, formatMoment(context, bake)), style = MaterialTheme.typography.bodyLarge)
            }
            if (state.finished) {
                Text(
                    stringResource(R.string.schedule_finished),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            StepTimeline(starts = state.phases.map { it.startEarliest }) { index, last ->
                val time = state.phases[index]
                val phase = phases[index]
                val current = index == state.current
                val mark = when {
                    time.done -> StepMark.DONE
                    current -> StepMark.CURRENT
                    else -> StepMark.UPCOMING
                }
                val label = stringResource(
                    when (mark) {
                        StepMark.DONE -> R.string.schedule_state_done
                        StepMark.CURRENT -> R.string.schedule_state_current
                        StepMark.UPCOMING -> R.string.schedule_state_upcoming
                    },
                )
                // Feste Dauern als „1 Tag, 6 Minuten“ statt „1446 Minuten“ (nach Anpassung beim Start oder eigener Dauer).
                val duration = if (phase.minMinutes == phase.maxMinutes) {
                    AndroidDurationText.total(context, phase.maxMinutes)
                } else {
                    AndroidDurationText.format(context, phase.minMinutes, phase.maxMinutes)
                }
                val trailing = if (time.done) stringResource(R.string.schedule_done_short, timeText(context, time.endEarliest)) else duration
                val spoken = stringResource(
                    R.string.schedule_phase_semantics,
                    stringResource(R.string.schedule_phase_line, label, formatMoment(context, time.startEarliest)),
                    phase.text,
                    trailing,
                )
                val expanded = current || (open == index && !time.done)
                val durationName = stringResource(R.string.schedule_duration_of, phase.text)
                val details: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.padding(top = Spacing.xs)) {
                        if (current) CurrentDetails(phase, time, state, now)
                        DurationEditor(
                            minutes = phase.maxMinutes,
                            hours = timeline[index].maxMinutes >= 120,
                            name = durationName,
                            onChange = { actions.onDuration(run, index, it) },
                        )
                    }
                }
                val buttons: @Composable () -> Unit = { RunButtons(run, actions, onRemove, withComplete = true) }
                StepLine(
                    time = timeText(context, time.startEarliest),
                    title = phase.text,
                    trailing = trailing,
                    mark = mark,
                    last = last,
                    spoken = spoken,
                    onClick = when {
                        time.done -> ({ correcting = index })
                        current -> null
                        else -> ({ open = if (open == index) -1 else index })
                    },
                    onClickLabel = stringResource(if (time.done) R.string.schedule_change_time else R.string.schedule_step_open),
                    highlighted = current,
                    below = if (expanded) details else null,
                    footer = if (current) buttons else null,
                )
            }
            if (state.finished) RunButtons(run, actions, onRemove, withComplete = false)
        }
    }
    if (correcting in run.doneAt.indices) {
        val index = correcting
        DoneTimeDialog(
            initial = run.doneAt[index],
            onPicked = {
                correcting = -1
                actions.onDoneAt(run, index, it)
            },
            onDismiss = { correcting = -1 },
        )
    }
}

/** Im aktuellen Schritt: Ort und wann es weitergeht. Der Screenreader liest Änderungen vor. */
@Composable
private fun CurrentDetails(phase: TimelinePhase, time: DoughRunPlanner.PhaseTime, state: DoughRunPlanner.State, now: Long) {
    val context = LocalContext.current
    val place = AndroidDurationText.place(context, phase.place)
    val next = when {
        DoughRunPlanner.overdue(state, now) -> stringResource(R.string.schedule_overdue, formatMoment(context, time.endLatest))
        now >= time.endEarliest -> stringResource(R.string.schedule_ready, formatMoment(context, time.endLatest))
        time.endEarliest == time.endLatest -> stringResource(R.string.schedule_next_at, formatMoment(context, time.endEarliest))
        else -> stringResource(R.string.schedule_next_range, formatMoment(context, time.endEarliest), formatMoment(context, time.endLatest))
    }
    Column(modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        if (place.isNotEmpty()) Text(place, style = MaterialTheme.typography.bodyMedium)
        Text(next, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * „Fertig“, „Zurück“, „Abbrechen“ (Wunsch vom 20.11.2026) in einer Reihe, gleich breit; „Fertig“ gefüllt, die anderen mit Rahmen (heben sich so vom hervorgehobenen Schritt ab). Bei großer
 * Schrift brechen sie um, damit kein Text abgeschnitten wird.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunButtons(run: DoughRun, actions: ScheduleActions, onRemove: () -> Unit, withComplete: Boolean) {
    val padding = PaddingValues(horizontal = Spacing.s)
    if (isLargeText()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.fillMaxWidth()) {
            RunButtonSet(run, actions, onRemove, withComplete, padding, Modifier)
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.fillMaxWidth()) {
            RunButtonSet(run, actions, onRemove, withComplete, padding, Modifier.weight(1f))
        }
    }
}

/**
 * Kräftiger Rahmen für „Zurück“ und „Abbrechen“ (Wunsch vom 21.11.2026): Der Standardrahmen war im Dunkelmodus auf dem
 * hervorgehobenen Schritt kaum zu sehen.
 */
@Composable
internal fun strongBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

@Composable
private fun RunButtonSet(
    run: DoughRun,
    actions: ScheduleActions,
    onRemove: () -> Unit,
    withComplete: Boolean,
    padding: PaddingValues,
    modifier: Modifier,
) {
    if (withComplete) {
        Button(onClick = { actions.onComplete(run) }, contentPadding = padding, modifier = modifier) {
            Text(stringResource(R.string.schedule_complete))
        }
    }
    if (run.doneAt.isNotEmpty()) {
        OutlinedButton(onClick = { actions.onUndo(run) }, contentPadding = padding, border = strongBorder(), modifier = modifier) {
            Text(stringResource(R.string.schedule_undo))
        }
    }
    OutlinedButton(onClick = onRemove, contentPadding = padding, border = strongBorder(), modifier = modifier) {
        Text(stringResource(R.string.schedule_remove))
    }
}
