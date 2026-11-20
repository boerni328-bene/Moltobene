package com.moltobene.app.ui.schedule

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.TimelinePhase
import com.moltobene.app.ui.components.RepeatIconButton
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.dough.parseNumber
import com.moltobene.app.ui.dough.show
import com.moltobene.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/*
 * Zeitplan als Zeitleiste (Entwurf G3, gewählt am 20.11.2026): Der Tag steht als Trenner über den Schritten, jeder
 * Schritt ist eine Zeile mit Uhrzeit, Name und Dauer, ein Punkt links zeigt den Zustand (voll = erledigt, Ring =
 * aktuell, leer = folgt). Der aktuelle Schritt ist aufgeklappt.
 */

/** Zustand eines Schritts für den Punkt auf der Zeitleiste; steht immer auch als Text für den Screenreader. */
internal enum class StepMark { DONE, CURRENT, UPCOMING }

/** Abstand des Punkts von oben: auf Höhe der ersten Textzeile. */
private val DOT_TOP = 14.dp
private val RAIL = 18.dp

/** Tag wie auf dem Handy eingestellt, z. B. „Sonntag, 22. Nov.“. */
internal fun dayText(context: Context, millis: Long): String = DateUtils.formatDateTime(
    context,
    millis,
    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR,
)

/** Nur die Uhrzeit wie auf dem Handy eingestellt (12 oder 24 Stunden). */
internal fun timeText(context: Context, millis: Long): String = DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME)

private fun dayOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

/** Die Schritte untereinander; wo der Tag wechselt, steht er als Trenner darüber. [starts] ist der Beginn jedes Schritts. */
@Composable
internal fun StepTimeline(starts: List<Long>, step: @Composable (index: Int, last: Boolean) -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        starts.forEachIndexed { index, start ->
            if (index == 0 || dayOf(start) != dayOf(starts[index - 1])) DaySeparator(dayText(context, start), first = index == 0)
            step(index, index == starts.lastIndex)
        }
    }
}

/** Der Tag fett in der Hauptfarbe, daneben eine Linie über die ganze Breite (Wunsch vom 20.11.2026). */
@Composable
private fun DaySeparator(text: String, first: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier.fillMaxWidth().padding(top = if (first) Spacing.xs else Spacing.m, bottom = Spacing.xs),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
    }
}

/**
 * Ein Schritt: Punkt auf der Zeitleiste, Uhrzeit, Name und rechts Dauer oder „Fertig 12:20“. [onClick] klappt auf bzw.
 * ändert die Uhrzeit; [below] steht aufgeklappt darunter. Bei großer Schrift stehen Uhrzeit, Name und Dauer untereinander.
 */
@Composable
internal fun StepLine(
    time: String,
    title: String,
    trailing: String,
    mark: StepMark,
    last: Boolean,
    spoken: String,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    highlighted: Boolean = false,
    below: (@Composable () -> Unit)? = null,
    /** Über die ganze Breite unter dem Schritt (Knöpfe des aktuellen Schritts, Entwurf G4). */
    footer: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val dot = colors.primary
    val ring = colors.primaryContainer
    val rail = colors.outlineVariant
    val hollow = if (highlighted) colors.secondaryContainer else colors.surfaceContainer
    val faded = mark == StepMark.DONE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (highlighted) Modifier.background(colors.secondaryContainer, MaterialTheme.shapes.small) else Modifier)
            .padding(horizontal = Spacing.xs),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier.width(RAIL).fillMaxHeight().drawBehind {
                    val x = size.width / 2
                    val top = DOT_TOP.toPx()
                    val radius = 5.dp.toPx()
                    drawLine(rail, Offset(x, 0f), Offset(x, if (last) top else size.height), strokeWidth = 2.dp.toPx())
                    if (mark == StepMark.CURRENT) drawCircle(ring, radius + 4.dp.toPx(), Offset(x, top))
                    drawCircle(if (mark == StepMark.UPCOMING) hollow else dot, radius, Offset(x, top))
                    drawCircle(dot, radius, Offset(x, top), style = Stroke(2.dp.toPx()))
                },
            )
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.s, top = Spacing.xs, bottom = Spacing.xs)) {
                val header = Modifier
                    .fillMaxWidth()
                    .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier)
                    .clearAndSetSemantics { contentDescription = spoken }
                val timeColor = if (faded) colors.onSurfaceVariant else if (mark == StepMark.CURRENT) colors.primary else colors.onSurface
                val titleColor = if (faded) colors.onSurfaceVariant else colors.onSurface
                val titleWeight = if (mark == StepMark.CURRENT) FontWeight.Bold else FontWeight.Normal
                if (isLargeText()) {
                    Column(modifier = header) {
                        Text(time, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = timeColor)
                        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = titleWeight, color = titleColor)
                        Text(trailing, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                } else {
                    Row(modifier = header, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(time, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = timeColor)
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = titleWeight,
                            color = titleColor,
                            modifier = Modifier.weight(1f),
                        )
                        Text(trailing, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                below?.invoke()
            }
        }
        if (footer != null) Box(modifier = Modifier.padding(bottom = Spacing.s)) { footer() }
    }
}

/**
 * Dauer eines Schritts ändern (0.78.0): Zahl eintippen oder mit − und +. Lange Schritte in Stunden (Schritt 1 Stunde),
 * kurze in Minuten (Schritt 5 Minuten). Übernommen wird beim Tippen auf − und +, mit „Fertig“ auf der Tastatur oder
 * beim Verlassen des Felds. [hours] richtet sich nach dem Rezept, damit die Einheit beim Ändern nicht springt.
 */
@Composable
internal fun DurationEditor(minutes: Int, hours: Boolean, name: String, onChange: (Int) -> Unit) {
    val unit = stringResource(if (hours) R.string.schedule_unit_hours else R.string.schedule_unit_minutes)
    fun shown(m: Int): String = if (hours) show((m / 6.0).roundToInt() / 10.0) else m.toString()
    var text by rememberSaveable(minutes) { mutableStateOf(shown(minutes)) }
    fun typed(): Int? = parseNumber(text).takeIf { it.isFinite() && it > 0 }?.let { if (hours) (it * 60).roundToInt() else it.roundToInt() }
    fun commit() {
        val value = typed() ?: return
        if (value != minutes) onChange(value.coerceIn(1, TimelinePhase.MAX_MINUTES))
    }
    val step = if (hours) 60 else 5
    fun stepped(direction: Int): Int {
        val grid = (minutes + step / 2) / step * step
        return (grid + direction * step).coerceIn(step, TimelinePhase.MAX_MINUTES)
    }
    val stepText = stringResource(if (hours) R.string.schedule_step_hour else R.string.schedule_step_minutes)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.schedule_duration), maxLines = 1) },
            suffix = { Text(unit) },
            singleLine = true,
            isError = typed() == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) commit() },
        )
        RepeatIconButton(
            onStep = { onChange(stepped(-1)) },
            enabled = minutes > step,
            contentDescription = stringResource(R.string.dough_step_less, name, stepText),
            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
        }
        RepeatIconButton(
            onStep = { onChange(stepped(+1)) },
            enabled = minutes < TimelinePhase.MAX_MINUTES,
            contentDescription = stringResource(R.string.dough_step_more, name, stepText),
            modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
}

/** Uhrzeit korrigieren, wann ein Schritt fertig war (0.78.0); der Tag bleibt. Bei großer Schrift als Eingabe. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DoneTimeDialog(initial: Long, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val start = remember(initial) { Instant.ofEpochMilli(initial).atZone(zone) }
    val state = rememberTimePickerState(start.hour, start.minute, DateFormat.is24HourFormat(context))
    val largeText = isLargeText()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_done_time_title)) },
        text = { if (largeText) TimeInput(state = state) else TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onPicked(start.withHour(state.hour).withMinute(state.minute).withSecond(0).toInstant().toEpochMilli())
            }) { Text(stringResource(R.string.schedule_time_set)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
