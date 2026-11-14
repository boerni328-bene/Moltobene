package com.moltobene.app.ui.recipe

import android.content.ActivityNotFoundException
import android.content.Intent
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.Adjustment
import com.moltobene.app.data.AmountScaling
import com.moltobene.app.data.Pan
import com.moltobene.app.data.PanScaling
import com.moltobene.app.data.Tag
import com.moltobene.app.data.CookingTextSize
import com.moltobene.app.ui.components.TagLabels
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.components.IngredientLine
import com.moltobene.app.ui.theme.Spacing
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Portionen mit − und +; umgerechnet wird nur in der Anzeige (#56). Nach einer Zutat umgerechnet (#66) können es
 * krumme Portionen sein: Dann steht „etwa“ davor, und − und + gehen von der gerundeten Zahl aus.
 */
@Composable
internal fun ServingsControl(shown: Int, approximate: Boolean, unit: String?, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(shown - 1) }, enabled = shown > 1) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.servings_fewer))
        }
        val text = servingsControlText(shown, unit)
        Text(
            text = if (approximate) stringResource(R.string.amount_about, text) else text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Der Screenreader sagt die neue Zahl an.
            modifier = Modifier.weight(1f, fill = false).semantics { liveRegion = LiveRegionMode.Polite },
        )
        IconButton(onClick = { onChange(shown + 1) }, enabled = shown < Adjustment.MAX_SERVINGS) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.servings_more))
        }
    }
}

/**
 * „Umrechnen …“ (#66) und, wenn umgerechnet ist, der Hinweis mit „Zurücksetzen“. Bei großer Schrift dürfen die beiden
 * Schaltflächen untereinander stehen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdjustActions(adjusted: Boolean, hint: String?, bakingNote: Boolean, onAdjust: () -> Unit, onReset: () -> Unit) {
    if (adjusted && hint != null) {
        Text(
            text = hint + " " + stringResource(if (bakingNote) R.string.adjust_pan_baking else R.string.adjust_hint_rules),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (adjusted) TextButton(onClick = onReset) { Text(stringResource(R.string.servings_reset)) }
    }
}

/** Zwischen − und +: „4 Portionen“ oder mit eigener Einheit „Springform (Ø 26 cm): 2“ statt „2 Springform“ (#63). */
@Composable
internal fun servingsControlText(servings: Int, unit: String?): String =
    if (unit == null) {
        pluralStringResource(R.plurals.servings_count, servings, servings)
    } else {
        stringResource(R.string.servings_unit_count, unit, servings)
    }

/** Als Satzteil, z. B. im Hinweis „Im Rezept: 1 Springform (Ø 26 cm)“. */
@Composable
internal fun servingsText(servings: Int, unit: String?): String =
    if (unit == null) {
        pluralStringResource(R.plurals.servings_count, servings, servings)
    } else {
        stringResource(R.string.servings_with_unit, servings, unit)
    }

/**
 * Eine Zutat zum Abhaken: Die ganze Zeile ist antippbar, abgehakt heißt Häkchen und durchgestrichen.
 * Umgerechnete Mengen sind fett, damit sie nicht nur an der Farbe zu erkennen sind.
 */
@Composable
internal fun IngredientRow(
    parts: List<AmountScaling.Part>,
    locale: LocaleList?,
    amountWidth: Dp?,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val state = stringResource(if (checked) R.string.ingredient_checked else R.string.ingredient_unchecked)
    val resources = LocalResources.current
    // Der Screenreader liest „etwa 335 g Mehl, umgerechnet“ (#65); die Zutat behält dabei die Sprache des Rezepts.
    val spoken = remember(parts, locale, resources) {
        if (parts.none { it.scaled }) return@remember null
        val line = AmountScaling.spoken(parts) { resources.getString(R.string.amount_about, it) }
        val full = resources.getString(R.string.ingredient_adjusted, line)
        buildAnnotatedString {
            append(full)
            val start = full.indexOf(line)
            if (start >= 0) addStyle(SpanStyle(localeList = locale), start, start + line.length)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { stateDescription = state },
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Box(modifier = if (spoken != null) Modifier.clearAndSetSemantics { text = spoken } else Modifier) {
            IngredientLine(
                parts = parts,
                amountWidth = amountWidth,
                locale = locale,
                style = cookingTextStyle().copy(
                    textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                ),
                color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Ein Schritt; antippen markiert ihn als aktuellen Schritt – mit Symbol und Hintergrund, nicht nur über die Farbe. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StepRow(number: Int, text: AnnotatedString, current: Boolean, barInset: Dp, onClick: () -> Unit) {
    // „Weiter“ und „Zurück“ (#79) scrollen den aktuellen Schritt in den sichtbaren Bereich.
    val requester = remember { BringIntoViewRequester() }
    // Liegt die Leiste über dem Inhalt, bleibt Platz in Höhe der Leiste frei, damit der Schritt nicht darunter landet.
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val insetPx = with(LocalDensity.current) { barInset.toPx() }
    LaunchedEffect(current) {
        if (current) requester.bringIntoView(Rect(0f, -insetPx, rowSize.width.toFloat(), rowSize.height.toFloat()))
    }
    val currentLabel = stringResource(R.string.step_current)
    // Ein kräftiger Balken links zeigt den aktuellen Schritt auch ohne Farbe: zusätzlich zu Symbol, Hintergrund und Text.
    val barColor = MaterialTheme.colorScheme.primary
    val accent = if (current) Modifier.drawBehind { drawRect(barColor, size = Size(4.dp.toPx(), size.height)) } else Modifier
    val markLabel = stringResource(R.string.step_mark_current)
    val highlight = if (current) {
        Modifier.background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
    } else {
        Modifier
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
            .onSizeChanged { rowSize = it }
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(highlight)
            .then(accent)
            .clickable(onClickLabel = markLabel, onClick = onClick)
            .padding(Spacing.s)
            .semantics(mergeDescendants = true) { if (current) stateDescription = currentLabel },
    ) {
        val color = if (current) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        if (current) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = "$number.",
            style = cookingTextStyle(current),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 24.dp),
        )
        Text(text, style = cookingTextStyle(current), color = color)
    }
}

/** Ab dieser Abweichung von einer ganzen Zahl steht „etwa“ vor den Portionen. */
internal const val SERVINGS_TOLERANCE = 0.01

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = when (LocalCookingSize.current) {
            CookingTextSize.SMALL -> MaterialTheme.typography.headlineSmall
            CookingTextSize.MEDIUM -> MaterialTheme.typography.headlineMedium
            CookingTextSize.LARGE -> MaterialTheme.typography.headlineLarge
        },
        modifier = Modifier
            .padding(top = Spacing.m)
            .semantics { heading() },
    )
}

/** Öffnet nur Links mit http/https im Browser. */
@Composable
internal fun SourceLink(url: String, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val errorText = stringResource(R.string.source_link_error)
    TextButton(
        onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(errorText) }
            }
        },
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(url) }
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Zeiten aus einer Übernahme (#55), im Format des Handys, z. B. „Gesamtzeit: 1 Std., 30 Min.“. */
@Composable
internal fun RecipeTimes(prepMinutes: Int?, totalMinutes: Int?) {
    val locale = LocalConfiguration.current.locales[0]
    listOfNotNull(
        prepMinutes?.takeIf { it > 0 }?.let { R.string.recipe_time_prep to it },
        totalMinutes?.takeIf { it > 0 }?.let { R.string.recipe_time_total to it },
    ).forEach { (label, minutes) ->
        Text(
            text = stringResource(label, formatDuration(minutes, locale)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Dauer in Stunden und Minuten mit den Abkürzungen der Sprache, z. B. „1 Std., 30 Min.“ oder “1 hr, 30 min”. */
internal fun formatDuration(minutes: Int, locale: Locale): String {
    val hours = minutes / 60
    val rest = minutes % 60
    val parts = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        if (rest > 0 || hours == 0) add(Measure(rest, MeasureUnit.MINUTE))
    }
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT).formatMeasures(*parts.toTypedArray())
}

/** Eine Backform als Text, z. B. „Ø 28 cm“ oder „30 × 40 cm“, bei Eingabe in Zoll mit „(9 Zoll)“ (#67). */
@Composable
internal fun panLabel(pan: Pan, inch: Boolean): String {
    fun cm(value: Double) = if (abs(value - value.roundToInt()) < 0.05) value.roundToInt().toString() else "%.1f".format(java.util.Locale.ROOT, value)
    fun inches(value: Double) = (value / PanScaling.CM_PER_INCH).let { if (abs(it - it.roundToInt()) < 0.05) it.roundToInt().toString() else "%.1f".format(java.util.Locale.ROOT, it) }
    val base = if (pan.round) {
        stringResource(R.string.pan_round, cm(pan.a))
    } else {
        stringResource(R.string.pan_rectangle, cm(pan.a), cm(pan.b))
    }
    if (!inch) return base
    val size = if (pan.round) inches(pan.a) else stringResource(R.string.pan_size_pair, inches(pan.a), inches(pan.b))
    return stringResource(R.string.pan_with_inch, base, stringResource(R.string.pan_inch, size))
}

/** „Umrechnen …“ neben den Portionen in derselben Zeile; bei großer Schrift darunter. */
@Composable
internal fun AdjustRow(onAdjust: () -> Unit, content: @Composable () -> Unit) {
    val button = @Composable { TextButton(onClick = onAdjust) { Text(stringResource(R.string.adjust_action)) } }
    if (isLargeText()) {
        Column {
            content()
            button()
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) { content() }
            button()
        }
    }
}

/** Die Schlagwörter des Rezepts (#57) als ruhige Marken unter den Zeiten; mit Text, nicht nur mit Farbe. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecipeTags(tags: List<Tag>) {
    val resources = LocalResources.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        for (tag in tags) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    text = TagLabels.text(resources, tag),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
                )
            }
        }
    }
}

/** Schriftgröße in der Rezeptansicht (#61): klein, mittel oder groß, unabhängig von der Schriftgröße des Geräts. */
internal val LocalCookingSize = compositionLocalOf { CookingTextSize.MEDIUM }

/**
 * Schrift für Zutaten, Schritte und Notizen nach der gewählten Größe. Nur der aktuelle Schritt (Einzelschritt beim
 * Kochen) ist eine Stufe größer.
 */
@Composable
internal fun cookingTextStyle(current: Boolean = false): TextStyle = with(MaterialTheme.typography) {
    when (LocalCookingSize.current) {
        CookingTextSize.SMALL -> if (current) titleMedium else bodyMedium
        CookingTextSize.MEDIUM -> if (current) titleLarge else bodyLarge
        CookingTextSize.LARGE -> if (current) headlineMedium else titleLarge
    }
}

/**
 * Notizen zum Rezept (#61) mit „Notiz hinzufügen“ bzw. „Notiz bearbeiten“ direkt in der Rezeptansicht. Die angefangene
 * Notiz übersteht das Drehen des Handys und das Beenden der App durch Android (`rememberSaveable`).
 */
@Composable
internal fun NotesSection(notes: String, onSave: (String) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    if (notes.isNotBlank()) {
        SectionTitle(stringResource(R.string.notes))
        Text(notes, style = cookingTextStyle())
    }
    OutlinedButton(
        onClick = {
            draft = notes
            editing = true
        },
        modifier = Modifier.padding(top = if (notes.isBlank()) Spacing.m else 0.dp),
    ) {
        Text(stringResource(if (notes.isBlank()) R.string.note_add else R.string.note_edit))
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.notes)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(stringResource(R.string.notes)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(draft.trim())
                    editing = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
