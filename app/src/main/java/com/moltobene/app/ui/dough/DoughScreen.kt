package com.moltobene.app.ui.dough

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import com.moltobene.app.ui.components.AppSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.dough.DoughDraft
import com.moltobene.app.data.dough.DoughCalculator
import com.moltobene.app.data.dough.DoughField
import com.moltobene.app.data.dough.DoughIssue
import com.moltobene.app.data.dough.DoughLimits
import com.moltobene.app.data.dough.DoughNumbers
import com.moltobene.app.data.dough.DoughPresets
import com.moltobene.app.data.dough.DoughResult
import com.moltobene.app.data.dough.DoughStepper
import com.moltobene.app.data.dough.DoughType
import com.moltobene.app.data.dough.LeavenedInput
import com.moltobene.app.data.dough.LineRole
import com.moltobene.app.data.dough.Preferment
import com.moltobene.app.data.dough.YeastKind
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.AppTitleWithScreen
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.theme.Spacing
import com.moltobene.app.ui.components.RepeatIconButton
import com.moltobene.app.ui.components.readingWidth
import java.util.Locale
import kotlinx.coroutines.delay

/** Zahl aus einem Eingabefeld: Komma oder Punkt; leer oder Text ergibt NaN (wird als „keine Zahl“ gemeldet). */
internal fun parseNumber(text: String): Double =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: Double.NaN

internal fun show(value: Double): String =
    DoughNumbers.separator(DoughNumbers.plain(value, 2), Locale.getDefault().language)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoughScreen(
    viewModel: DoughViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenKnowHow: () -> Unit,
    onSaved: (String) -> Unit,
) {
    var typeKey by rememberSaveable { mutableStateOf(DoughType.PIZZA.key) }
    val type = DoughType.fromKey(typeKey) ?: DoughType.PIZZA
    Scaffold(
        topBar = {
            TopAppBar(
                // Name der App und klein der Bildschirm (Entwurf vom 22.10.2026): spart die Überschrift im Formular.
                title = { AppTitleWithScreen(stringResource(R.string.dough_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                // Teigwissen (#84): ein einfaches Info-Symbol in der obersten Zeile.
                actions = {
                    IconButton(onClick = onOpenKnowHow) {
                        Icon(painterResource(R.drawable.ic_info), contentDescription = stringResource(R.string.knowhow_title))
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { padding ->
        CompositionLocalProvider(LocalDoughSliders provides true) {
            Column(modifier = Modifier.padding(padding).fillMaxSize().readingWidth()) {
                val header: @Composable () -> Unit = {
                    Header(type) { typeKey = it.key }
                }
                when (type) {
                    DoughType.PIZZA, DoughType.BREAD -> LeavenedContent(type, viewModel, onSaved, header)
                    DoughType.PASTA -> PastaContent(viewModel, onSaved, header)
                    DoughType.CAKE -> CakeContent(viewModel, onSaved, header)
                }
            }
        }
    }
}

@Composable
private fun LeavenedContent(
    type: DoughType,
    viewModel: DoughViewModel,
    onSaved: (String) -> Unit,
    header: @Composable () -> Unit,
) {
    val loaded by produceState<LeavenedInput?>(null, type) {
        value = null
        value = viewModel.load(type)
    }
    val initial = loaded
    if (initial == null) {
        Column { header() }
        CircularProgressIndicator(Modifier.padding(Spacing.m))
    } else {
        DoughForm(initial, viewModel, onSaved, header)
    }
}

private fun typeLabel(type: DoughType): Int = when (type) {
    DoughType.PIZZA -> R.string.dough_type_pizza
    DoughType.BREAD -> R.string.dough_type_bread
    DoughType.PASTA -> R.string.dough_type_pasta
    DoughType.CAKE -> R.string.dough_type_cake
}

/** Überschrift und Wahl der Teigart: ein Schalter über die volle Breite, bei großer Schrift Chips untereinander. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun Header(selected: DoughType, onSelect: (DoughType) -> Unit) {
    val types = DoughType.entries
    if (isLargeText()) {
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            for (type in types) {
                FilterChip(selected = type == selected, onClick = { onSelect(type) }, label = { Text(stringResource(typeLabel(type))) })
            }
        }
    } else {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m)) {
            types.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = type == selected,
                    onClick = { onSelect(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, types.size),
                    icon = {},
                ) {
                    Text(stringResource(typeLabel(type)), maxLines = 1)
                }
            }
        }
    }
}
/** Eine Karte mit Überschrift; gliedert das Formular ruhig und kompakt. */
@Composable
internal fun Card(title: String, onHelp: (() -> Unit)? = null, highlighted: Boolean = false, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = if (highlighted) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (title.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                // „Was bewirkt das?“ (Teigwissen, #84): ein Fenster mit kurzen Hinweisen zu den Werten der Karte.
                if (onHelp != null) {
                    IconButton(onClick = onHelp) {
                        Icon(painterResource(R.drawable.ic_help), contentDescription = stringResource(R.string.dough_help_open, title))
                    }
                }
            }
            content()
        }
    }
}

/**
 * Felder des Teigrechners (#83): bei normaler Schrift je zwei nebeneinander (kompakter, weniger Scrollen), bei großer
 * Schrift untereinander über die volle Breite. In der Zweierreihe fehlen Plus und Minus; der Regler bleibt.
 */
@Composable
internal fun Fields(vararg fields: @Composable (Modifier) -> Unit, weights: List<Float>? = null, trailing: (@Composable () -> Unit)? = null) {
    if (isLargeText()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            fields.forEach { it(Modifier.fillMaxWidth()) }
            trailing?.invoke()
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        fields.toList().chunked(2).forEach { pair ->
            if (pair.size == 1) {
                if (trailing == null) {
                    pair[0](Modifier.fillMaxWidth())
                } else {
                    // Das letzte einzelne Feld teilt sich die Reihe mit dem Hinweis, statt eine halb leere Reihe zu füllen.
                    CompositionLocalProvider(LocalDoughCompactField provides true) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                            pair[0](Modifier.weight(1f))
                            Box(modifier = Modifier.weight(1f)) { trailing() }
                        }
                    }
                }
            } else {
                CompositionLocalProvider(LocalDoughCompactField provides true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        pair.forEach { it(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** Steht das Feld in einer Zweierreihe? Dann ohne Plus und Minus. */
internal val LocalDoughCompactField = compositionLocalOf { false }

/** Plus, Minus und die Markierung „Standard“ eines Felds (#83): Schrittweite, Standardwert, Grenzen. */
internal class Stepping(
    val step: Double,
    val default: Double? = null,
    val range: DoughLimits.Range? = null,
    val field: DoughField? = null,
)

/** Sind die Regler eingeschaltet? Gilt für alle Felder des Teigrechners (#83, Etappe 2). */
internal val LocalDoughSliders = compositionLocalOf { false }

/** So lange nach der letzten Änderung wird gemerkt, damit Plus/Minus und Tippen nicht jedes Mal schreiben. */
internal const val MEMORY_DELAY_MILLIS = 400L

@Composable
internal fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    issue: DoughIssue?,
    modifier: Modifier = Modifier,
    integer: Boolean = false,
    suffix: String? = null,
    stepping: Stepping? = null,
) {
    val error = issue?.let { issueText(it) }
    val current = parseNumber(value)
    val default = stepping?.default
    val atDefault = default != null && DoughStepper.isDefault(current, default)
    val field: @Composable (Modifier) -> Unit = { m ->
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label, maxLines = 1) },
            suffix = suffix?.let { { Text(it) } },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
            modifier = m,
        )
    }
    if (stepping == null) {
        field(modifier)
        return
    }
    // Bei normaler Schrift ein Feld mit Minus und Plus in einer Zeile (Entwurf vom 22.10.2026); bei großer Schrift
    // bleibt es beim Feld mit Knöpfen und Regler darunter.
    if (!isLargeText()) {
        StepperField(label, value, onChange, error, modifier, integer, suffix, stepping)
        return
    }
    fun text(number: Double) = if (integer) number.toLong().toString() else show(number)
    val bounds = if (LocalDoughSliders.current && stepping.field != null) DoughStepper.sliderBounds(stepping.field, stepping.range) else null
    // Eine Bedienung pro Wert (#86): Regler mit antippbarer Zahl; Plus und Minus nur bei kleinen ganzen Zahlen
    // (Anzahl) und wo es keinen Regler gibt.
    val compact = LocalDoughCompactField.current
    val showSteps = !compact && (integer || bounds == null)
    val buttons: @Composable () -> Unit = {
        val stepText = show(stepping.step) + suffix?.let { " $it" }.orEmpty()
        RepeatIconButton(
            onStep = { onChange(text(DoughStepper.next(current, stepping.step, -1, stepping.range, default))) },
            enabled = DoughStepper.canStep(current, -1, stepping.range),
            contentDescription = stringResource(R.string.dough_step_less, label, stepText),
            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
        }
        RepeatIconButton(
            onStep = { onChange(text(DoughStepper.next(current, stepping.step, +1, stepping.range, default))) },
            enabled = DoughStepper.canStep(current, +1, stepping.range),
            contentDescription = stringResource(R.string.dough_step_more, label, stepText),
            modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
    val slider: @Composable () -> Unit = {
        if (bounds != null && stepping.field != null) {
            ValueSlider(
                label = label,
                current = current,
                bounds = bounds,
                stepping = stepping,
                field = stepping.field,
                valueText = if (current.isFinite()) text(current) + suffix?.let { " $it" }.orEmpty() else "",
                onChange = { onChange(text(it)) },
            )
        }
    }
    // „Standard 250 g“ nur, wenn der Wert abweicht – eine Zeile mit „Zurücksetzen“, kein Text bei Standard.
    val defaultLine: @Composable () -> Unit = {
        if (default != null && !atDefault) {
            val value = stringResource(R.string.dough_default_value, show(default) + suffix?.let { " $it" }.orEmpty())
            val reset = stringResource(R.string.dough_default_reset_short)
            if (compact) {
                // In der Zweierreihe ist wenig Breite: der Text und darunter der Knopf.
                Column(modifier = Modifier.fillMaxWidth().padding(start = Spacing.xs)) {
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onChange(text(default)) }) { Text(reset) }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(start = Spacing.m),
                    )
                    TextButton(onClick = { onChange(text(default)) }) { Text(reset) }
                }
            }
        }
    }
    if (isLargeText()) {
        // Bei großer Schrift hat das Feld die volle Breite, die Knöpfe stehen darunter.
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            field(Modifier.fillMaxWidth())
            if (showSteps) Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) { buttons() }
            slider()
            defaultLine()
        }
    } else {
        Column(modifier = modifier) {
            if (showSteps) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    field(Modifier.weight(1f))
                    Row(modifier = Modifier.padding(top = Spacing.xs)) { buttons() }
                }
            } else {
                field(Modifier.fillMaxWidth())
            }
            slider()
            defaultLine()
        }
    }
}

/** Hängt die Einheit („g“, „%“) hinter die Zahl, ohne dass sie mitgetippt oder gelöscht wird. */
private class SuffixTransformation(private val suffix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText = TransformedText(
        AnnotatedString(text.text + " " + suffix),
        object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset
            override fun transformedToOriginal(offset: Int) = offset.coerceAtMost(text.length)
        },
    )
}

/**
 * Feld mit Minus und Plus in einer Zeile (Entwurf vom 22.10.2026): darüber der Name, in der Mitte die antippbare Zahl
 * mit Einheit. Die Schrittweite, die Grenzen und der Standardwert kommen wie beim Feld mit Regler aus [stepping].
 */
@Composable
private fun StepperField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    modifier: Modifier,
    integer: Boolean,
    suffix: String?,
    stepping: Stepping,
) {
    val current = parseNumber(value)
    val default = stepping.default
    val atDefault = default != null && DoughStepper.isDefault(current, default)
    fun text(number: Double) = if (integer) number.toLong().toString() else show(number)
    val stepText = show(stepping.step) + suffix?.let { " $it" }.orEmpty()
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1)
        Surface(
            shape = MaterialTheme.shapes.small,
            color = colors.surface,
            border = BorderStroke(1.dp, if (error != null) colors.error else colors.outlineVariant),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RepeatIconButton(
                    onStep = { onChange(text(DoughStepper.next(current, stepping.step, -1, stepping.range, default))) },
                    enabled = DoughStepper.canStep(current, -1, stepping.range),
                    contentDescription = stringResource(R.string.dough_step_less, label, stepText),
                ) {
                    Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
                    cursorBrush = SolidColor(colors.primary),
                    visualTransformation = if (suffix != null) SuffixTransformation(suffix) else VisualTransformation.None,
                    modifier = Modifier.weight(1f).semantics { contentDescription = label },
                )
                RepeatIconButton(
                    onStep = { onChange(text(DoughStepper.next(current, stepping.step, +1, stepping.range, default))) },
                    enabled = DoughStepper.canStep(current, +1, stepping.range),
                    contentDescription = stringResource(R.string.dough_step_more, label, stepText),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(start = Spacing.xs))
        }
        // „Standard 250 g“ nur, wenn der Wert abweicht; mit „Zurücksetzen“ in einer Zeile.
        if (default != null && !atDefault) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.dough_default_value, show(default) + suffix?.let { " $it" }.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = Spacing.xs),
                )
                TextButton(onClick = { onChange(text(default)) }) { Text(stringResource(R.string.dough_default_reset_short)) }
            }
        }
    }
}

/**
 * Regler zu einem Feld (#83, Etappe 2): zeigt den üblichen Bereich, rundet auf die Schrittweite und hält die Grenzen
 * aus [DoughLimits] ein. Eine Marke zeigt den Standardwert; die Zahl im Feld bleibt die Wahrheit. Hefe läuft auf einer
 * gestauchten Skala, damit kleine Mengen treffbar sind.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ValueSlider(
    label: String,
    current: Double,
    bounds: DoughLimits.Range,
    stepping: Stepping,
    field: DoughField,
    valueText: String,
    onChange: (Double) -> Unit,
) {
    val curved = DoughStepper.isCurved(field)
    val step = DoughStepper.sliderStepFor(field)
    val position = DoughStepper.positionOf(current, bounds, curved)
    val ticks = ((bounds.max - bounds.min) / step).let { Math.round(it).toInt() }
    // Bei wenigen Stufen (z. B. Anzahl) rastet der Regler ein, sonst läuft er stufenlos und rundet beim Loslassen der Zahl.
    val steps = if (!curved && ticks in 2..20) ticks - 1 else 0
    val markColor = MaterialTheme.colorScheme.tertiary
    val defaultPosition = stepping.default?.let { DoughStepper.positionOf(it, bounds, curved) }
    Slider(
        value = position,
        onValueChange = { onChange(DoughStepper.valueAt(it, bounds, curved, step, stepping.range)) },
        valueRange = 0f..1f,
        steps = steps,
        // Schlanker Regler: dünne Leiste, kleiner Daumen, keine Rastpunkte; die Höhe bleibt zum Treffen bei 36 dp.
        thumb = {
            Box(
                Modifier
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
            )
        },
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(position.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .drawBehind {
                // Kleine Marke für den Standardwert auf der Leiste (der Daumen ist 18 dp breit).
                if (defaultPosition != null) {
                    val inset = 9.dp.toPx()
                    val x = inset + (size.width - 2 * inset) * defaultPosition
                    drawLine(markColor, Offset(x, size.height / 2 - 6.dp.toPx()), Offset(x, size.height / 2 + 6.dp.toPx()), strokeWidth = 2.dp.toPx())
                }
            }
            .semantics {
                contentDescription = label
                stateDescription = valueText
            },
    )
}

/**
 * Auswahl aus festen Möglichkeiten (#83, Etappe 2): bei wenigen Möglichkeiten Chips, bei vielen Möglichkeiten oder großer
 * Schrift ein Auswahlfeld. Die Erklärung steht in beiden Fällen darunter.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChoiceField(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    val dropdown = options.size >= DROPDOWN_ALWAYS_FROM || (isLargeText() && options.size >= DROPDOWN_LARGE_FROM)
    if (!dropdown) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            for (option in options) {
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(optionLabel(option)) })
            }
        }
        return
    }
    var open by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = optionLabel(selected)
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, maxLines = 1) },
            trailingIcon = { Text("▾") },
            modifier = Modifier.fillMaxWidth(),
        )
        // Die ganze Fläche öffnet die Liste; das Feld und die Auswahl lesen sich als ein Element.
        Box(
            modifier = Modifier
                .matchParentSize()
                .semantics(mergeDescendants = true) {
                    role = Role.DropdownList
                    contentDescription = "$label: $selectedLabel"
                }
                .clickable(onClickLabel = label) { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (option in options) {
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = {
                    open = false
                    onSelect(option)
                })
            }
        }
    }
}

/**
 * Auswahlfeld in der Größe der Felder mit Minus und Plus (Entwurf vom 30.10.2026): der Name darüber, darunter die
 * Auswahl mit Pfeil; die Liste öffnet beim Antippen. Für das Formular ohne Scrollen bei normaler Schrift.
 */
@Composable
internal fun <T> CompactChoice(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val selectedLabel = optionLabel(selected)
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1)
        Box {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        role = Role.DropdownList
                        contentDescription = "$label: $selectedLabel"
                    }
                    .clickable(onClickLabel = label) { open = true },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 48.dp).padding(start = Spacing.s),
                ) {
                    Text(selectedLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("▾", modifier = Modifier.padding(horizontal = Spacing.s))
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                for (option in options) {
                    DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = {
                        open = false
                        onSelect(option)
                    })
                }
            }
        }
    }
}

/** Ab so vielen Möglichkeiten gibt es immer ein Auswahlfeld, bei großer Schrift schon ab drei. */
private const val DROPDOWN_ALWAYS_FROM = 6
private const val DROPDOWN_LARGE_FROM = 3

@Composable
internal fun issueText(issue: DoughIssue): String? = when (issue) {
    is DoughIssue.NotANumber -> stringResource(R.string.dough_error_number)
    is DoughIssue.OutOfRange -> stringResource(R.string.dough_error_range, show(issue.min), show(issue.max))
    is DoughIssue.PrefermentWaterTooHigh ->
        stringResource(R.string.dough_error_preferment, show(issue.neededPercent), show(issue.totalPercent))
    is DoughIssue.TooLittleLiquid -> stringResource(R.string.dough_error_liquid, show(issue.neededPercent))
    DoughIssue.PanInvalid -> stringResource(R.string.dough_error_pan)
    DoughIssue.Inconsistent -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoughForm(
    initial: LeavenedInput,
    viewModel: DoughViewModel,
    onSaved: (String) -> Unit,
    header: @Composable () -> Unit,
) {
    val type = initial.type
    // Neu beginnen mit den gemerkten Werten (nach „Zurücksetzen“: mit den Standardwerten).
    var resetCount by rememberSaveable(type) { mutableStateOf(0) }
    val start = if (resetCount == 0) initial else DoughLimits.defaults(type)
    var count by rememberSaveable(type, resetCount) { mutableStateOf(start.count.toString()) }
    var piece by rememberSaveable(type, resetCount) { mutableStateOf(show(start.pieceGrams)) }
    var water by rememberSaveable(type, resetCount) { mutableStateOf(show(start.water)) }
    var salt by rememberSaveable(type, resetCount) { mutableStateOf(show(start.salt)) }
    var oil by rememberSaveable(type, resetCount) { mutableStateOf(show(start.oil)) }
    var yeastKindKey by rememberSaveable(type, resetCount) { mutableStateOf(start.yeastKind.key) }
    var yeast by rememberSaveable(type, resetCount) { mutableStateOf(show(start.yeast)) }
    var prefermentKey by rememberSaveable(type, resetCount) { mutableStateOf(start.preferment.key) }
    var prefermentFlour by rememberSaveable(type, resetCount) { mutableStateOf(show(start.prefermentFlour)) }
    var prefermentWater by rememberSaveable(type, resetCount) { mutableStateOf(show(start.prefermentWater)) }

    val yeastKind = YeastKind.fromKey(yeastKindKey) ?: YeastKind.FRESH
    val preferment = Preferment.fromKey(prefermentKey) ?: Preferment.NONE
    val defaults = DoughLimits.defaults(type)
    fun stepping(field: DoughField, default: Double?) =
        Stepping(DoughStepper.stepFor(field), default, DoughLimits.range(type, field, preferment), field)
    val input = LeavenedInput(
        type = type,
        count = count.trim().toIntOrNull() ?: 0,
        pieceGrams = parseNumber(piece),
        water = parseNumber(water),
        salt = parseNumber(salt),
        oil = parseNumber(oil),
        yeastKind = yeastKind,
        yeast = parseNumber(yeast),
        preferment = preferment,
        prefermentFlour = parseNumber(prefermentFlour),
        prefermentWater = parseNumber(prefermentWater),
    )
    val result = DoughCalculator.calculate(input)
    val issues = (result as? DoughResult.Invalid)?.issues.orEmpty()
    fun issueFor(field: DoughField): DoughIssue? = issues.firstOrNull {
        (it is DoughIssue.NotANumber && it.field == field) || (it is DoughIssue.OutOfRange && it.field == field)
    }

    // Gültige Werte merken, damit sie beim nächsten Öffnen wieder da sind.
    LaunchedEffect(input, result is DoughResult.Success) {
        if (result is DoughResult.Success) {
            delay(MEMORY_DELAY_MILLIS)
            viewModel.remember(input)
        }
    }

    // Voreinstellungen (#83, Etappe 3): mitgelieferte und eigene; die zuletzt gewählte eigene bleibt vorn, solange die Werte passen.
    val entries = presetEntries(type, viewModel.own(type))
    val entryInputs = entries.map { it.input }
    var chosen by rememberSaveable(type) { mutableStateOf(-1) }
    val presetIndex = chosen.takeIf { it in entryInputs.indices && DoughPresets.matches(input, entryInputs[it]) }
        ?: DoughPresets.indexOf(input, entryInputs)
    fun applyPreset(i: Int) {
        val preset = entries.getOrNull(i)?.input ?: return
        chosen = i
        water = show(preset.water)
        salt = show(preset.salt)
        oil = show(preset.oil)
        yeastKindKey = preset.yeastKind.key
        yeast = show(preset.yeast)
        prefermentKey = preset.preferment.key
        prefermentFlour = show(preset.prefermentFlour)
        prefermentWater = show(preset.prefermentWater)
    }

    // Hinweise „Was bewirkt das?“ (Teigwissen, #84): welche Karte gerade erklärt wird.
    var help by rememberSaveable { mutableStateOf<String?>(null) }
    DoughHelpDialog(helpTopics(help)) { help = null }

    val bread = type == DoughType.BREAD
    // Die Felder stehen einmal hier, damit das Formular ohne Scrollen und das mit Karten dieselben benutzen.
    val countField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(if (bread) R.string.dough_count_loaf else R.string.dough_count_ball), count,
            { count = it }, issueFor(DoughField.COUNT), m, integer = true,
            stepping = stepping(DoughField.COUNT, defaults.count.toDouble()),
        )
    }
    val pieceField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_piece), piece, { piece = it },
            issueFor(DoughField.PIECE_GRAMS), m, suffix = "g",
            stepping = stepping(DoughField.PIECE_GRAMS, defaults.pieceGrams),
        )
    }
    val waterField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_water), water, { water = it }, issueFor(DoughField.WATER), m, suffix = "%",
            stepping = stepping(DoughField.WATER, defaults.water),
        )
    }
    val saltField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_salt), salt, { salt = it }, issueFor(DoughField.SALT), m, suffix = "%",
            stepping = stepping(DoughField.SALT, defaults.salt),
        )
    }
    val oilField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_oil), oil, { oil = it }, issueFor(DoughField.OIL), m, suffix = "%",
            stepping = stepping(DoughField.OIL, defaults.oil),
        )
    }
    val yeastField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_yeast_amount), yeast, { yeast = it },
            issueFor(DoughField.YEAST), m, suffix = "%",
            stepping = stepping(DoughField.YEAST, YeastKind.FRESH.convert(defaults.yeast, yeastKind)),
        )
    }
    val prefermentFlourField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_preferment_flour), prefermentFlour, { prefermentFlour = it },
            issueFor(DoughField.PREFERMENT_FLOUR), m, suffix = "%",
            stepping = stepping(DoughField.PREFERMENT_FLOUR, defaults.prefermentFlour),
        )
    }
    val prefermentWaterField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_preferment_water), prefermentWater, { prefermentWater = it },
            issueFor(DoughField.PREFERMENT_WATER), m, suffix = "%",
            stepping = stepping(DoughField.PREFERMENT_WATER, DoughLimits.defaultPrefermentWater(preferment)),
        )
    }
    fun selectYeastKind(kind: YeastKind) {
        // Die Menge bleibt gleichwertig, wenn die Hefeart wechselt.
        val current = parseNumber(yeast)
        if (current.isFinite()) {
            yeast = DoughNumbers.separator(
                DoughNumbers.plain(yeastKind.convert(current, kind), 3),
                Locale.getDefault().language,
            )
        }
        yeastKindKey = kind.key
    }
    fun selectPreferment(option: Preferment) {
        prefermentKey = option.key
        // Biga und Poolish haben feste Wasseranteile, die hier vorausgefüllt werden.
        if (option != Preferment.NONE) prefermentWater = show(DoughLimits.defaultPrefermentWater(option))
    }
    // Bei normaler Schrift steht das Ergebnis oben fest und rechnet beim Ändern der Werte mit (Entwurf vom 22.10.2026);
    // bei großer Schrift bleibt es bei der Leiste unten, damit zum Scrollen genug Platz bleibt.
    val pinned = !isLargeText()
    val success = result as? DoughResult.Success
    val resultDraft = success?.let { viewModel.draft(it, input) }
    val resultTotals = success?.let { viewModel.totals(it, input) }
    val prefermentItems = success?.let { viewModel.prefermentItems(it, input) }.orEmpty()
    val prefermentBar = if (prefermentItems.isEmpty()) null else stringResource(
        R.string.dough_bar_preferment,
        stringResource(prefermentLabel(preferment)),
        prefermentItems.joinToString(", ") { "${it.amount} ${it.name}" },
    )
    Column(modifier = Modifier.fillMaxSize()) {
        if (pinned) {
            header()
            ResultTop(
                summary = if (success != null && resultDraft != null) summaryLine(success, resultDraft) else null,
                totals = resultTotals,
                preferment = prefermentBar,
            )
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            if (!pinned) header()

            PresetCard(
                entries = entries,
                index = presetIndex,
                canSave = result is DoughResult.Success,
                onSelect = ::applyPreset,
                onSave = { name ->
                    val saved = viewModel.saveOwnPreset(type, name, input)
                    if (saved == DoughPresets.SaveResult.SAVED) {
                        val position = viewModel.own(type).indexOfFirst { it.name.equals(DoughPresets.cleanName(name), ignoreCase = true) }
                        chosen = DoughPresets.shipped(type).size + position
                    }
                    saved
                },
                onDelete = { name ->
                    viewModel.deleteOwnPreset(type, name)
                    chosen = -1
                },
                compact = pinned,
                extraActions = listOf(
                    stringResource(R.string.dough_reset) to { resetCount += 1 },
                    stringResource(R.string.dough_help_title) to { help = "all" },
                ),
            )

            if (pinned) {
                // Alles in einem Bild, ohne Scrollen (Entwurf vom 30.10.2026): Felder paarweise, Auswahlen im Format der Felder.
                val yeastKindChoice: @Composable (Modifier) -> Unit = { m ->
                    CompactChoice(
                        label = stringResource(R.string.dough_section_yeast),
                        options = YeastKind.entries,
                        selected = yeastKind,
                        optionLabel = { stringResource(AndroidDoughTexts.yeastName(it)) },
                        onSelect = ::selectYeastKind,
                        modifier = m,
                    )
                }
                val prefermentChoice: @Composable (Modifier) -> Unit = { m ->
                    CompactChoice(
                        label = stringResource(R.string.dough_section_preferment),
                        options = Preferment.entries,
                        selected = preferment,
                        optionLabel = { stringResource(prefermentLabel(it)) },
                        onSelect = ::selectPreferment,
                        modifier = m,
                    )
                }
                val compactFields = buildList<@Composable (Modifier) -> Unit> {
                    add(countField)
                    add(pieceField)
                    add(waterField)
                    add(saltField)
                    add(oilField)
                    add(prefermentChoice)
                    if (preferment != Preferment.NONE) {
                        add(prefermentFlourField)
                        add(prefermentWaterField)
                    }
                    add(yeastKindChoice)
                    add(yeastField)
                }
                Column(modifier = Modifier.padding(horizontal = Spacing.m)) {
                    Fields(*compactFields.toTypedArray())
                }
                Text(
                    stringResource(R.string.dough_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.m),
                )
                return@Column
            }

            Card(stringResource(R.string.dough_section_dough), onHelp = { help = "dough" }) {
                Fields(countField, pieceField)
                Fields(
                    waterField,
                    saltField,
                    oilField,
                    // Der Hinweis steht neben dem Öl, statt eine eigene Zeile zu belegen.
                    trailing = {
                        Text(
                            stringResource(R.string.dough_percent_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Card(stringResource(R.string.dough_section_yeast), onHelp = { help = "yeast" }) {
                ChoiceField(
                    label = stringResource(R.string.dough_section_yeast),
                    options = YeastKind.entries,
                    selected = yeastKind,
                    optionLabel = { stringResource(AndroidDoughTexts.yeastName(it)) },
                    onSelect = ::selectYeastKind,
                )
                TextButton(onClick = { help = yeastKind.key }) {
                    Text(stringResource(R.string.dough_help_term_open, stringResource(AndroidDoughTexts.yeastName(yeastKind))))
                }
                yeastField(Modifier.fillMaxWidth())
            }

            // Der Vorteig ist hervorgehoben (Rahmen in der Hauptfarbe): Auswahl, Anteile und die Mengen des Vorteigs als Kacheln,
            // dazu ein Fenster, das Biga, Poolish und Sauerteig erklärt (Entwurf vom 22.10.2026).
            Card(stringResource(R.string.dough_section_preferment), onHelp = { help = "preferment" }, highlighted = true) {
                ChoiceField(
                    label = stringResource(R.string.dough_section_preferment),
                    options = Preferment.entries,
                    selected = preferment,
                    optionLabel = { stringResource(prefermentLabel(it)) },
                    onSelect = ::selectPreferment,
                )
                if (preferment != Preferment.NONE) {
                    TextButton(onClick = { help = preferment.key }) {
                        Text(stringResource(R.string.dough_help_term_open, stringResource(prefermentLabel(preferment))))
                    }
                }
                Text(
                    stringResource(prefermentExplanation(preferment)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (preferment != Preferment.NONE) {
                    val tiles = (result as? DoughResult.Success)?.let { viewModel.prefermentItems(it, input) }.orEmpty()
                    if (tiles.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            for (item in tiles) TotalTile(item)
                        }
                    }
                    Fields(prefermentFlourField, prefermentWaterField)
                }
            }

            // Mit Vorteig steht hier die Aufteilung; die Summen stehen immer unten.
            if (result is DoughResult.Success && preferment != Preferment.NONE) {
                val draft = viewModel.draft(result, input)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
                ) {
                    Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            stringResource(R.string.dough_section_result),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        for (line in draft.ingredients) {
                            Text(
                                line.text,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (line.isHeading) FontWeight.Bold else null,
                                modifier = Modifier.padding(top = if (line.isHeading) Spacing.xs else 0.dp),
                            )
                        }
                    }
                }
            }

            if (result is DoughResult.Success) {
                val yeastGrams = result.sections.flatMap { it.lines }.filter { it.role == LineRole.YEAST }.sumOf { it.grams }
                if (yeastGrams > 0) {
                    val language = LocalConfiguration.current.locales[0].language
                    val resources = LocalResources.current
                    val others = YeastKind.entries.filter { it != yeastKind }.joinToString(", ") { other ->
                        val grams = DoughNumbers.grams(yeastKind.convert(yeastGrams, other), LineRole.YEAST, language)
                        resources.getString(R.string.dough_yeast_item, grams, resources.getString(AndroidDoughTexts.yeastName(other)))
                    }
                    Text(
                        stringResource(R.string.dough_yeast_equal, others),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = Spacing.m),
                    )
                }
            }
            if (isLargeText() && result is DoughResult.Success) {
                TotalsCard(viewModel.totals(result, input))
            }
            Text(
                stringResource(R.string.dough_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.m),
            )
            TextButton(onClick = { resetCount += 1 }, modifier = Modifier.padding(horizontal = Spacing.s)) {
                Text(stringResource(R.string.dough_reset))
            }
        }

        if (pinned) {
            SaveBar(enabled = success != null, onSave = { viewModel.saveAsRecipe(input, onSaved) })
        } else {
            ResultBar(
                summary = if (success != null && resultDraft != null) summaryLine(success, resultDraft) else null,
                totals = resultTotals,
                onSave = { viewModel.saveAsRecipe(input, onSaved) },
                preferment = prefermentBar,
            )
        }
    }
}

/**
 * Oben fest (Entwurf vom 22.10.2026): die Summe und die Gesamtmengen als Kacheln. [summary] null = ungültig. Die
 * Kacheln teilen sich die Breite, bei mehr als vier lassen sie sich seitlich verschieben.
 */
@Composable
internal fun ResultTop(summary: String?, totals: List<TotalItem>?, preferment: String? = null) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.xs),
    ) {
        Column(modifier = Modifier.padding(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                if (summary != null) {
                    Text(summary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        stringResource(R.string.dough_invalid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (summary != null && !totals.isNullOrEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    for (item in totals) TotalTile(item)
                }
            }
            // Biga, Poolish, Sauerteig: was für den Vorteig am Abend vorher angesetzt wird.
            if (summary != null && preferment != null) {
                Text(preferment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics(mergeDescendants = true) {})
            }
        }
    }
}

/** „Als Rezept speichern“ unten; die Summen stehen oben ([ResultTop]). */
@Composable
internal fun SaveBar(enabled: Boolean, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Button(
            onClick = onSave,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
        ) {
            Text(stringResource(R.string.dough_save))
        }
    }
}

private fun prefermentLabel(preferment: Preferment): Int = when (preferment) {
    Preferment.NONE -> R.string.dough_pre_none
    Preferment.BIGA -> R.string.dough_pre_biga
    Preferment.POOLISH -> R.string.dough_pre_poolish
    Preferment.SOURDOUGH -> R.string.dough_pre_sourdough
}

private fun prefermentExplanation(preferment: Preferment): Int = when (preferment) {
    Preferment.NONE -> R.string.dough_explain_preferment
    Preferment.BIGA -> R.string.dough_explain_biga
    Preferment.POOLISH -> R.string.dough_explain_poolish
    Preferment.SOURDOUGH -> R.string.dough_explain_sourdough
}


/** „1000 g Teig · 4 Teigkugeln (je 250 g)“. */
@Composable
internal fun summaryLine(result: DoughResult.Success, draft: DoughDraft): String = stringResource(
    R.string.dough_total_line,
    DoughNumbers.separator(DoughNumbers.plain(result.totalGrams, 0), draft.language),
    "${draft.servings} ${draft.servingsUnit}",
)

/**
 * Immer sichtbar: die Überschrift des Ergebnisses, die Gesamtmengen als Kacheln (seitlich zu verschieben) und
 * „Als Rezept speichern“ – auch beim Ändern der Eingaben weiter oben. [summary] null = ungültig. Bei großer Schrift
 * stehen die Gesamtmengen als Karte im Formular ([TotalsCard]), damit die Leiste klein bleibt (#83).
 */
@Composable
internal fun ResultBar(summary: String?, totals: List<TotalItem>?, onSave: () -> Unit, preferment: String? = null) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                if (summary != null) {
                    Text(summary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        stringResource(R.string.dough_invalid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (summary != null && !totals.isNullOrEmpty() && !isLargeText()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    for (item in totals) TotalTile(item)
                }
            }
            // Biga, Poolish, Sauerteig: was für den Vorteig am Abend vorher angesetzt wird.
            if (summary != null && preferment != null) {
                Text(preferment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics(mergeDescendants = true) {})
            }
            Button(onClick = onSave, enabled = summary != null, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dough_save))
            }
        }
    }
}

/** Eine Gesamtmenge als Kachel: die Menge groß, darunter der Name. */
@Composable
private fun TotalTile(item: TotalItem) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs).semantics(mergeDescendants = true) {},
        ) {
            Text(item.amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(item.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Bei großer Schrift: die Gesamtmengen als Liste mit dem Namen links und der Menge rechts (#83). */
@Composable
internal fun TotalsCard(items: List<TotalItem>) {
    if (items.isEmpty()) return
    Card(stringResource(R.string.dough_totals_title)) {
        for (item in items) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            ) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                Text(item.amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
