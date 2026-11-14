package com.moltobene.app.ui.dough

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.moltobene.app.data.dough.DoughFlours
import com.moltobene.app.data.dough.DoughIssue
import com.moltobene.app.data.dough.DoughKitchen
import com.moltobene.app.data.dough.DoughLimits
import com.moltobene.app.data.dough.DoughNumbers
import com.moltobene.app.data.dough.DoughPresets
import com.moltobene.app.data.dough.DoughProcess
import com.moltobene.app.data.dough.DoughProcessMemory
import com.moltobene.app.data.dough.DoughProcessRules
import com.moltobene.app.data.dough.DoughResult
import com.moltobene.app.data.dough.DoughSummary
import com.moltobene.app.data.dough.DoughStepper
import com.moltobene.app.data.StepperSide
import com.moltobene.app.data.WeightUnit
import com.moltobene.app.data.dough.DoughType
import com.moltobene.app.data.dough.DoughWater
import com.moltobene.app.data.dough.LeavenedInput
import com.moltobene.app.data.dough.LineRole
import com.moltobene.app.data.dough.Preferment
import com.moltobene.app.data.dough.SectionKind
import com.moltobene.app.data.dough.YeastKind
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.AppTitleWithScreen
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.components.scrollFade
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

/**
 * Brücke zwischen der Kopfleiste und dem Formular: Das gerade gezeigte Formular trägt ein, was „Speichern als
 * Voreinstellung“ und „Zurücksetzen“ in der Leiste tun.
 */
@Stable
internal class DoughBarActions {
    var showSave by mutableStateOf(false)
    var canSave by mutableStateOf(false)
    var save: () -> Unit = {}
    var reset: () -> Unit = {}
}

internal val LocalDoughBar = compositionLocalOf { DoughBarActions() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoughScreen(
    viewModel: DoughViewModel,
    stepperSide: StepperSide,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenKnowHow: () -> Unit,
    onOpenSettings: () -> Unit,
    /** Nummer des gespeicherten Rezepts; [withSteps]: mit Zeitplan und Zubereitung (Pizza und Brot). */
    onSaved: (id: String, withSteps: Boolean) -> Unit,
) {
    var typeKey by rememberSaveable { mutableStateOf(DoughType.PIZZA.key) }
    val type = DoughType.fromKey(typeKey) ?: DoughType.PIZZA
    val bar = remember { DoughBarActions() }
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    val leavened = type == DoughType.PIZZA || type == DoughType.BREAD
    if (helpOpen && leavened) {
        DoughHelpDialog(
            topics = helpTopics("all"),
            onDismiss = { helpOpen = false },
            onKnowHow = {
                helpOpen = false
                onOpenKnowHow()
            },
        )
    }
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
                // Ganz oben (Wunsch des Projektinhabers vom 31.10.2026): Speichern als Voreinstellung, Zurücksetzen und
                // rechts das Info-Symbol mit „Was bewirkt das?“; bei großer Schrift stehen die ersten beiden in den Karten.
                actions = {
                    if (!isLargeText()) {
                        if (bar.showSave) {
                            IconButton(onClick = { bar.save() }, enabled = bar.canSave) {
                                Icon(painterResource(R.drawable.ic_save), contentDescription = stringResource(R.string.dough_preset_save))
                            }
                        }
                        IconButton(onClick = { bar.reset() }) {
                            Icon(painterResource(R.drawable.ic_refresh), contentDescription = stringResource(R.string.dough_reset))
                        }
                    }
                    IconButton(onClick = { if (leavened) helpOpen = true else onOpenKnowHow() }) {
                        Icon(
                            painterResource(R.drawable.ic_info),
                            contentDescription = stringResource(if (leavened) R.string.dough_help_title else R.string.knowhow_title),
                        )
                    }
                },
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { padding ->
        CompositionLocalProvider(
            LocalDoughSliders provides true, LocalStepperSide provides stepperSide, LocalDoughBar provides bar,
        ) {
            // Tippen neben ein Feld schließt die Tastatur wieder.
            val focusManager = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().readingWidth()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        }
                    },
            ) {
                val header: @Composable () -> Unit = {
                    Header(type) { typeKey = it.key }
                }
                when (type) {
                    DoughType.PIZZA, DoughType.BREAD -> LeavenedContent(type, viewModel, { onSaved(it, true) }, onOpenSettings, header)
                    DoughType.PASTA -> PastaContent(viewModel, { onSaved(it, false) }, header)
                    DoughType.CAKE -> CakeContent(viewModel, { onSaved(it, false) }, header)
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
    onOpenSettings: () -> Unit,
    header: @Composable () -> Unit,
) {
    val loaded by produceState<DoughViewModel.Loaded?>(null, type) {
        value = null
        value = viewModel.load(type)
    }
    val initial = loaded
    if (initial == null) {
        Column { header() }
        CircularProgressIndicator(Modifier.padding(Spacing.m))
    } else {
        DoughForm(initial.input, initial.process, viewModel, onSaved, onOpenSettings, header)
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
    /** Kein Regler (z. B. beim Gewicht in Unzen, wo die üblichen Bereiche in Gramm angegeben sind). */
    val noSlider: Boolean = false,
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
            // Geänderte Werte stehen fett (Standardwerte gibt es nur gemeinsam über „Zurücksetzen“).
            textStyle = LocalTextStyle.current.copy(fontWeight = if (default != null && !atDefault) FontWeight.Bold else null),
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
    val bounds = if (LocalDoughSliders.current && stepping.field != null && !stepping.noSlider) DoughStepper.sliderBounds(stepping.field, stepping.range) else null
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
    if (isLargeText()) {
        // Bei großer Schrift hat das Feld die volle Breite, die Knöpfe stehen darunter.
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            field(Modifier.fillMaxWidth())
            if (showSteps) Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) { buttons() }
            slider()
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
 * Auf welcher Seite des Werts Minus und Plus nebeneinander stehen (Einstellung „Plus und Minus“, Wunsch vom 30.10.2026,
 * z. B. für Linkshänder).
 */
internal val LocalStepperSide = compositionLocalOf { StepperSide.RIGHT }

/**
 * Feld mit Minus und Plus in einer Zeile (Entwurf vom 22.10.2026): darüber der Name, die antippbare Zahl mit Einheit
 * und beide Knöpfe nebeneinander rechts oder links davon ([LocalStepperSide]). Ein geänderter Wert steht fett; den
 * Standardwert stellt „Zurücksetzen“ für alle Werte gemeinsam wieder her. Die Schrittweite und die Grenzen kommen wie
 * beim Feld mit Regler aus [stepping].
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
    val changed = default != null && !DoughStepper.isDefault(current, default)
    fun text(number: Double) = if (integer) number.toLong().toString() else show(number)
    val stepText = show(stepping.step) + suffix?.let { " $it" }.orEmpty()
    val colors = MaterialTheme.colorScheme
    val buttons: @Composable () -> Unit = {
        RepeatIconButton(
            onStep = { onChange(text(DoughStepper.next(current, stepping.step, -1, stepping.range, default))) },
            enabled = DoughStepper.canStep(current, -1, stepping.range),
            contentDescription = stringResource(R.string.dough_step_less, label, stepText),
        ) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = null)
        }
        RepeatIconButton(
            onStep = { onChange(text(DoughStepper.next(current, stepping.step, +1, stepping.range, default))) },
            enabled = DoughStepper.canStep(current, +1, stepping.range),
            contentDescription = stringResource(R.string.dough_step_more, label, stepText),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1)
        Surface(
            shape = MaterialTheme.shapes.small,
            color = colors.surface,
            border = BorderStroke(1.dp, if (error != null) colors.error else colors.outlineVariant),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val left = LocalStepperSide.current == StepperSide.LEFT
                if (left) buttons()
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = colors.onSurface,
                        textAlign = TextAlign.End,
                        fontWeight = if (changed) FontWeight.Bold else null,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
                    cursorBrush = SolidColor(colors.primary),
                    visualTransformation = if (suffix != null) SuffixTransformation(suffix) else VisualTransformation.None,
                    modifier = Modifier.weight(1f).padding(horizontal = Spacing.xs).semantics { contentDescription = label },
                )
                if (!left) buttons()
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.padding(start = Spacing.xs))
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
    DoughIssue.FlourMixTooHigh -> stringResource(R.string.dough_error_flour_mix)
    DoughIssue.Inconsistent -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DoughForm(
    initial: LeavenedInput,
    initialProcess: DoughProcess,
    viewModel: DoughViewModel,
    onSaved: (String) -> Unit,
    onOpenSettings: () -> Unit,
    header: @Composable () -> Unit,
) {
    val type = initial.type
    // Neu beginnen mit den gemerkten Werten (nach „Zurücksetzen“: mit den Standardwerten).
    var resetCount by rememberSaveable(type) { mutableStateOf(0) }
    val start = if (resetCount == 0) initial else DoughLimits.defaults(type)
    var count by rememberSaveable(type, resetCount) { mutableStateOf(start.count.toString()) }
    // Gewichte in Gramm oder Unzen (Einstellung); wechselt die Einheit, beginnt das Feld neu mit den gemerkten Werten.
    val unit = viewModel.weightUnit
    var piece by rememberSaveable(type, resetCount, unit) { mutableStateOf(show(unit.fromGrams(start.pieceGrams))) }
    var water by rememberSaveable(type, resetCount) { mutableStateOf(show(start.water)) }
    var salt by rememberSaveable(type, resetCount) { mutableStateOf(show(start.salt)) }
    var oil by rememberSaveable(type, resetCount) { mutableStateOf(show(start.oil)) }
    var yeastKindKey by rememberSaveable(type, resetCount) { mutableStateOf(start.yeastKind.key) }
    var yeast by rememberSaveable(type, resetCount) { mutableStateOf(show(start.yeast)) }
    // Zusätzliche Hefe im Hauptteig, nur bei Biga und Poolish (Wunsch vom 04.11.2026).
    var mainYeast by rememberSaveable(type, resetCount) { mutableStateOf(show(start.mainYeast)) }
    var prefermentKey by rememberSaveable(type, resetCount) { mutableStateOf(start.preferment.key) }
    var prefermentFlour by rememberSaveable(type, resetCount) { mutableStateOf(show(start.prefermentFlour)) }
    var prefermentWater by rememberSaveable(type, resetCount) { mutableStateOf(show(start.prefermentWater)) }
    var maltText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.malt)) }
    var yogurtText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.yogurt)) }
    var milkText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.milk)) }
    var sugarText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.sugar)) }
    var ryeText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.rye)) }
    var wholeText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.whole)) }
    var speltText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.spelt)) }
    var honeyText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.honey)) }
    var eggText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.egg)) }
    var seedsText by rememberSaveable(type, resetCount) { mutableStateOf(show(start.seeds)) }
    // Weitere Zutaten erscheinen, sobald sie eine Menge haben oder über „Weitere Zutat“ hinzugefügt wurden.
    var addedExtras by rememberSaveable(type, resetCount) { mutableStateOf("") }
    // Gehzeiten und Temperaturen (Entwurf 10C, Wahl vom 12.11.2026): je Teigart gemerkt, nach „Zurücksetzen“ die
    // Standardwerte; als Text gemerkt, damit sie auch nach dem Drehen des Handys bleiben.
    var processText by rememberSaveable(type, resetCount) {
        mutableStateOf(DoughProcessMemory.encode(if (resetCount == 0) initialProcess else DoughProcessRules.defaults(type, start.preferment)))
    }
    // Welche Zeile im Bereich „Gehzeiten und Temperaturen“ offen ist (immer höchstens eine).
    var openRow by rememberSaveable(type) { mutableStateOf("") }
    // Welcher Bereich offen ist (Entwurf 5 „Aufklappen“ vom 04.11.2026); bleibt beim Zurücksetzen und bei Voreinstellungen.
    var openSection by rememberSaveable(type) { mutableStateOf(SECTION_DOUGH) }
    fun toggleSection(key: String) {
        openSection = if (openSection == key) "" else key
    }

    val yeastKind = YeastKind.fromKey(yeastKindKey) ?: YeastKind.FRESH
    val preferment = Preferment.fromKey(prefermentKey) ?: Preferment.NONE
    val process = DoughProcessMemory.decode(type, preferment, processText)
    fun setProcess(value: DoughProcess) {
        processText = DoughProcessMemory.encode(value)
    }
    // Raum, Kühlschrank und Knetart gelten für alle Teigarten (im ViewModel gemerkt); °C oder °F aus den Einstellungen.
    val kitchen = viewModel.kitchen
    val temperatureUnit = viewModel.temperatureUnit
    // „Zurücksetzen“ stellt alle Werte des Formulars wieder her, auch Raum, Kühlschrank und Knetart.
    fun reset() {
        resetCount += 1
        viewModel.updateKitchen(DoughKitchen())
    }
    val defaults = DoughLimits.defaults(type)
    fun stepping(field: DoughField, default: Double?) =
        Stepping(DoughStepper.stepFor(field), default, DoughLimits.range(type, field, preferment), field)
    val input = LeavenedInput(
        type = type,
        count = count.trim().toIntOrNull() ?: 0,
        pieceGrams = unit.toGrams(parseNumber(piece)),
        water = parseNumber(water),
        salt = parseNumber(salt),
        oil = parseNumber(oil),
        yeastKind = yeastKind,
        yeast = parseNumber(yeast),
        preferment = preferment,
        prefermentFlour = parseNumber(prefermentFlour),
        prefermentWater = parseNumber(prefermentWater),
        malt = parseNumber(maltText),
        yogurt = parseNumber(yogurtText),
        milk = parseNumber(milkText),
        sugar = parseNumber(sugarText),
        rye = parseNumber(ryeText),
        whole = parseNumber(wholeText),
        spelt = parseNumber(speltText),
        honey = parseNumber(honeyText),
        egg = parseNumber(eggText),
        seeds = parseNumber(seedsText),
        mainYeast = parseNumber(mainYeast),
    )
    val result = DoughCalculator.calculate(input)
    val issues = (result as? DoughResult.Invalid)?.issues.orEmpty()
    fun issueFor(field: DoughField): DoughIssue? = issues.firstOrNull {
        (it is DoughIssue.NotANumber && it.field == field) || (it is DoughIssue.OutOfRange && it.field == field) ||
            (it is DoughIssue.FlourMixTooHigh && field in listOf(DoughField.RYE, DoughField.WHOLE, DoughField.SPELT))
    }

    // Gewicht je Stück in der gewählten Einheit: Grenzen und Standardwert umgerechnet, Unzen ohne Regler.
    fun pieceIssue(): DoughIssue? = issueFor(DoughField.PIECE_GRAMS)?.let {
        if (it is DoughIssue.OutOfRange && unit != WeightUnit.GRAMS) {
            DoughIssue.OutOfRange(it.field, unit.fromGrams(it.min), unit.fromGrams(it.max))
        } else {
            it
        }
    }
    fun pieceStepping(): Stepping {
        if (unit == WeightUnit.GRAMS) return stepping(DoughField.PIECE_GRAMS, defaults.pieceGrams)
        val grams = DoughLimits.range(type, DoughField.PIECE_GRAMS)
        return Stepping(
            0.5, parseNumber(show(unit.fromGrams(defaults.pieceGrams))),
            DoughLimits.Range(unit.fromGrams(grams.min), unit.fromGrams(grams.max)), DoughField.PIECE_GRAMS, noSlider = true,
        )
    }

    // Gültige Werte merken, damit sie beim nächsten Öffnen wieder da sind.
    LaunchedEffect(input, result is DoughResult.Success) {
        if (result is DoughResult.Success) {
            delay(MEMORY_DELAY_MILLIS)
            viewModel.remember(input)
        }
    }
    LaunchedEffect(process) {
        delay(MEMORY_DELAY_MILLIS)
        viewModel.rememberProcess(type, process)
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
        // Ein anderer Vorteig bringt seine üblichen Gehzeiten mit; die Temperaturen bleiben.
        if (preset.preferment != preferment) setProcess(DoughProcessRules.forPreferment(process, type, preset.preferment))
        prefermentKey = preset.preferment.key
        prefermentFlour = show(preset.prefermentFlour)
        prefermentWater = show(preset.prefermentWater)
        maltText = show(preset.malt)
        yogurtText = show(preset.yogurt)
        milkText = show(preset.milk)
        sugarText = show(preset.sugar)
        ryeText = show(preset.rye)
        wholeText = show(preset.whole)
        speltText = show(preset.spelt)
        honeyText = show(preset.honey)
        eggText = show(preset.egg)
        seedsText = show(preset.seeds)
        mainYeast = show(preset.mainYeast)
    }

    // Hinweise „Was bewirkt das?“ (Teigwissen, #84): welche Karte gerade erklärt wird.
    var help by rememberSaveable { mutableStateOf<String?>(null) }
    DoughHelpDialog(helpTopics(help), onDismiss = { help = null })
    var savingPreset by rememberSaveable { mutableStateOf(false) }
    // Speichern und Zurücksetzen oben in der Kopfleiste tragen sich hier ein.
    val bar = LocalDoughBar.current
    SideEffect {
        bar.showSave = true
        bar.canSave = result is DoughResult.Success
        bar.save = { savingPreset = true }
        bar.reset = { reset() }
    }
    DisposableEffect(Unit) { onDispose { bar.showSave = false } }

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
            pieceIssue(), m, suffix = unit.label,
            stepping = pieceStepping(),
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
            // Mit Biga oder Poolish gibt es zwei Hefefelder; das erste nennt dann den Vorteig.
            stringResource(if (preferment.carriesYeast) R.string.dough_yeast_in_preferment else R.string.dough_yeast_amount),
            yeast, { yeast = it },
            issueFor(DoughField.YEAST), m, suffix = "%",
            stepping = stepping(DoughField.YEAST, YeastKind.FRESH.convert(defaults.yeast, yeastKind)),
        )
    }
    val mainYeastField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_yeast_main), mainYeast, { mainYeast = it },
            issueFor(DoughField.MAIN_YEAST), m, suffix = "%",
            stepping = stepping(DoughField.MAIN_YEAST, 0.0),
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
    // Mehlsorten und weitere Zutaten (Brot und Pizza): Menge in % vom Mehl; das Feld erscheint mit einer Menge oder nach „Weitere Zutat“.
    val extras = listOf(
        ExtraSpec("rye", DoughField.RYE, R.string.dough_label_rye, ryeText) { ryeText = it },
        ExtraSpec("whole", DoughField.WHOLE, R.string.dough_label_whole, wholeText) { wholeText = it },
        ExtraSpec("spelt", DoughField.SPELT, R.string.dough_label_spelt, speltText) { speltText = it },
        ExtraSpec("malt", DoughField.MALT, R.string.dough_label_malt, maltText) { maltText = it },
        ExtraSpec("seeds", DoughField.SEEDS, R.string.dough_label_seeds, seedsText) { seedsText = it },
        ExtraSpec("honey", DoughField.HONEY, R.string.dough_label_honey, honeyText) { honeyText = it },
        ExtraSpec("sugar", DoughField.SUGAR, R.string.dough_label_sugar, sugarText) { sugarText = it },
        ExtraSpec("yogurt", DoughField.YOGURT, R.string.dough_label_yogurt, yogurtText) { yogurtText = it },
        ExtraSpec("milk", DoughField.MILK, R.string.dough_label_milk, milkText) { milkText = it },
        ExtraSpec("egg", DoughField.EGG, R.string.dough_label_eggs, eggText) { eggText = it },
    )
    val addedKeys = addedExtras.split(',')
    val shownExtras = extras.filter { spec -> spec.key in addedKeys || parseNumber(spec.text).let { !it.isFinite() || it > 0 } }
    val extraField: (ExtraSpec) -> @Composable (Modifier) -> Unit = { spec ->
        { m ->
            NumberField(
                stringResource(spec.label), spec.text, spec.set, issueFor(spec.field), m, suffix = "%",
                stepping = stepping(spec.field, 0.0),
            )
        }
    }
    val extrasNote = parseNumber(yogurtText) > 0 || parseNumber(milkText) > 0
    // Mehlsorten nehmen verschieden viel Wasser auf (Richtwerte): ein Vorschlag, den ein Antippen übernimmt.
    val waterShift = DoughFlours.waterShift(input)
    val flourHint: @Composable () -> Unit = {
        if (waterShift != 0.0) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.xs)) {
                Text(
                    stringResource(R.string.dough_flour_hint, (if (waterShift > 0) "+" else "") + show(waterShift)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    val range = DoughLimits.range(type, DoughField.WATER, preferment)
                    water = show((parseNumber(water) + waterShift).coerceIn(range.min, range.max))
                }) { Text(stringResource(R.string.dough_flour_apply)) }
            }
        }
    }
    // Wählt eine weitere Zutat aus; fehlt keine mehr, entfällt der Knopf.
    val addExtraButton: @Composable () -> Unit = {
        val open = extras.filter { it !in shownExtras }
        if (open.isNotEmpty()) {
            var menu by rememberSaveable { mutableStateOf(false) }
            Box {
                TextButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                    Text(stringResource(R.string.dough_add_extra))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    for (spec in open) {
                        DropdownMenuItem(
                            text = { Text(stringResource(spec.label)) },
                            onClick = {
                                menu = false
                                addedExtras = (addedKeys + spec.key).filter { it.isNotEmpty() }.joinToString(",")
                            },
                        )
                    }
                }
            }
        }
    }
    fun selectYeastKind(kind: YeastKind) {
        // Die Mengen bleiben gleichwertig, wenn die Hefeart wechselt (auch die zusätzliche im Hauptteig).
        fun converted(text: String): String {
            val current = parseNumber(text)
            if (!current.isFinite()) return text
            return DoughNumbers.separator(DoughNumbers.plain(yeastKind.convert(current, kind), 3), Locale.getDefault().language)
        }
        yeast = converted(yeast)
        mainYeast = converted(mainYeast)
        yeastKindKey = kind.key
    }
    fun selectPreferment(option: Preferment) {
        if (option != preferment) setProcess(DoughProcessRules.forPreferment(process, type, option))
        prefermentKey = option.key
        // Biga und Poolish haben feste Wasseranteile, die hier vorausgefüllt werden.
        if (option != Preferment.NONE) prefermentWater = show(DoughLimits.defaultPrefermentWater(option))
    }
    // Bei normaler Schrift steht das Ergebnis oben fest und rechnet beim Ändern der Werte mit (Entwurf vom 22.10.2026);
    // bei großer Schrift bleibt es bei der Leiste unten, damit zum Scrollen genug Platz bleibt.
    val pinned = !isLargeText()
    val success = result as? DoughResult.Success
    val language = LocalConfiguration.current.locales[0].language
    val resources = LocalResources.current
    val resultTotals = success?.let { viewModel.totals(it, input) }
    val prefermentItems = success?.let { viewModel.prefermentItems(it, input) }.orEmpty()
    val prefermentName = stringResource(prefermentLabel(preferment))
    // Mengen vorn (Wunsch vom 05.11.2026): „277 g Biga“.
    val prefermentAmount = success?.let {
        stringResource(R.string.dough_summary_value, DoughNumbers.weight(DoughSummary.prefermentGrams(it), 0, language, unit), prefermentName)
    } ?: prefermentName
    // Gramm einer Zutat in einem Teil des Teigs, z. B. die Hefe im Vorteig; null ohne Ergebnis.
    fun gramsIn(kind: SectionKind, role: LineRole): String? = success?.let { done ->
        val grams = done.sections.filter { it.kind == kind }.flatMap { it.lines }.filter { it.role == role }.sumOf { it.grams }
        DoughNumbers.weight(grams, role, language, unit)
    }
    fun withGrams(text: String, grams: String?): String = if (grams == null) text else resources.getString(R.string.dough_summary_amount, text, grams)

    // Wassertemperatur für den Hauptteig (ohne Vorteig: für den Teig) nach Raum, Knetart und Teig nach dem Kneten
    // (Wunsch vom 05.11.2026); unter 2 °C oder über 40 °C mit Warnung.
    val waterTemperature = success?.let { DoughWater.temperature(it, preferment, kitchen, process.doughTemp) }
    val waterText = waterTemperature?.let { temperatureUnit.format(it.coerceAtLeast(0.0)) }
    val waterWarning = when {
        waterTemperature == null -> null
        waterTemperature < DoughWater.TOO_COLD -> stringResource(R.string.dough_water_too_cold)
        waterTemperature > DoughWater.TOO_HOT -> stringResource(R.string.dough_water_too_hot, temperatureUnit.format(DoughWater.TOO_HOT))
        else -> null
    }
    val effective = DoughProcessRules.effective(process, kitchen, type, preferment)
    val totalText = totalShort(resources, DoughProcessRules.totalMinutes(effective, type, preferment, kitchen.kneading))

    val prefermentBar = if (prefermentItems.isEmpty()) null else stringResource(
        R.string.dough_bar_preferment,
        prefermentAmount,
        prefermentItems.joinToString(", ") { "${it.amount} ${it.name}" },
    )
    // Darunter, was zum Kneten in den Hauptteig kommt (Wunsch vom 04.11.2026), mit Gewicht und Wassertemperatur
    // (Wunsch vom 05.11.2026): „1000 g Hauptteig: 277 g Biga, 429 g Mehl, 276 g Wasser (30 °C), 17 g Salz“.
    val mainItems = success?.let { viewModel.mainItems(it, input) }.orEmpty()
    val mainBar = if (success == null || prefermentItems.isEmpty() || mainItems.isEmpty()) null else stringResource(
        R.string.dough_bar_main,
        DoughNumbers.weight(success.totalGrams, 0, language, unit),
        (
            listOf(prefermentAmount) + mainItems.map { item ->
                val text = "${item.amount} ${item.name}"
                if (item.role == LineRole.WATER && waterText != null) withGrams(text, waterText) else text
            }
            ).joinToString(", "),
    )
    // Ohne Vorteig: „Wasser: 20 °C · Teig nach dem Kneten: 24 °C“.
    val plainBar = if (preferment != Preferment.NONE || waterText == null) null else listOf(
        stringResource(R.string.dough_process_water_plain, waterText),
        stringResource(R.string.dough_process_row, stringResource(R.string.dough_process_dough_temp), temperatureUnit.format(process.doughTemp)),
    ).joinToString(SUMMARY_SEPARATOR)
    // Zusammenfassungen der Bereiche (Entwurf 5) mit der Menge vorn (Wunsch vom 05.11.2026): „1,2 g Frischhefe · 0,2 %
    // vom Mehl“, mit zusätzlicher Hefe im Hauptteig „1,8 g Frischhefe · Vorteig 0,2 % (1,2 g) · Hauptteig 0,1 % (0,6 g)“.
    val withMainYeast = preferment.carriesYeast && parseNumber(mainYeast).let { !it.isFinite() || it > 0 }
    val yeastName = stringResource(AndroidDoughTexts.yeastName(yeastKind))
    val yeastAmount = success?.let {
        stringResource(R.string.dough_summary_value, DoughNumbers.weight(DoughSummary.yeastGrams(it), LineRole.YEAST, language, unit), yeastName)
    } ?: yeastName
    val yeastParts = if (withMainYeast) {
        listOf(
            yeastAmount,
            withGrams(summaryValue(R.string.dough_section_preferment, yeast), gramsIn(SectionKind.PREFERMENT, LineRole.YEAST)),
            withGrams(summaryValue(R.string.dough_main_dough, mainYeast), gramsIn(SectionKind.MAIN, LineRole.YEAST)),
        )
    } else {
        listOf(yeastAmount, stringResource(R.string.dough_summary_of_flour, "${yeast.trim()} %"))
    }
    val yeastSummary = yeastParts.joinToString(SUMMARY_SEPARATOR)
    // Vorteig: „277 g Biga · Mehl 30 % (184 g) · Wasser 50 % (92 g)“.
    val prefermentSummary = if (preferment == Preferment.NONE) {
        prefermentName
    } else {
        listOf(
            prefermentAmount,
            withGrams(summaryValue(R.string.dough_ingredient_flour, prefermentFlour), gramsIn(SectionKind.PREFERMENT, LineRole.FLOUR)),
            withGrams(summaryValue(R.string.dough_ingredient_water, prefermentWater), gramsIn(SectionKind.PREFERMENT, LineRole.WATER)),
        ).joinToString(SUMMARY_SEPARATOR)
    }
    // Gehzeiten und Temperaturen: „Raum 22 °C · Wasser 30 °C · gesamt 29 Std. 55 Min.“.
    val processSummary = if (waterText == null) {
        stringResource(R.string.dough_process_summary_no_water, temperatureUnit.format(kitchen.room), totalText)
    } else {
        stringResource(R.string.dough_process_summary, temperatureUnit.format(kitchen.room), waterText, totalText)
    }
    val processRowList = processRows(
        resources, language, type, preferment, prefermentName, process, kitchen, temperatureUnit, waterText,
        onProcess = { setProcess(it) },
        onKitchen = { viewModel.updateKitchen(it) },
    )
    val processContent: @Composable () -> Unit = {
        ProcessList(
            rows = processRowList,
            openKey = openRow,
            onToggle = { key -> openRow = if (openRow == key) "" else key },
            warning = waterWarning,
            onUnits = onOpenSettings,
        )
    }
    val doughSummary = buildList {
        add(stringResource(R.string.dough_summary_pieces, count.trim(), "${piece.trim()} ${unit.label}"))
        add(summaryValue(R.string.dough_ingredient_water, water))
        add(summaryValue(R.string.dough_ingredient_salt, salt))
        for (spec in shownExtras) add(summaryValue(spec.label, spec.text))
        add(summaryValue(R.string.dough_ingredient_oil, oil))
    }.joinToString(SUMMARY_SEPARATOR)
    Column(modifier = Modifier.fillMaxSize()) {
        if (pinned) {
            header()
            ResultTop(
                total = success?.let { DoughNumbers.weight(it.totalGrams, 0, language, unit) },
                totals = resultTotals,
                lines = listOfNotNull(prefermentBar, mainBar, plainBar),
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
                saving = savingPreset,
                onSavingChange = { savingPreset = it },
                canSave = result is DoughResult.Success,
                hasOwnChanges = !DoughPresets.matches(input, defaults),
                onSelect = { applyPreset(it) },
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
            )

            if (pinned) {
                // Entwurf 5 „Aufklappen“ (Wahl des Projektinhabers vom 04.11.2026): Hefe, Vorteig und Teig als Bereiche mit
                // Zusammenfassung, immer einer offen. So stehen bei normaler Schrift alle Werte auf einem Bildschirm; Hefe
                // und Vorteig bleiben oben (Wunsch vom 31.10. und 03.11.2026).
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    DoughSection(
                        title = stringResource(R.string.dough_section_yeast),
                        summary = yeastSummary,
                        expanded = openSection == SECTION_YEAST,
                        onToggle = { toggleSection(SECTION_YEAST) },
                    ) {
                        SubtleChoice(
                            label = "",
                            options = YeastKind.entries,
                            selected = yeastKind,
                            optionLabel = { stringResource(AndroidDoughTexts.yeastName(it)) },
                            onSelect = { selectYeastKind(it) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                        )
                        yeastField(Modifier.fillMaxWidth())
                        if (preferment.carriesYeast) mainYeastField(Modifier.fillMaxWidth())
                        if (success != null) YeastEquivalent(success, yeastKind)
                    }
                    DoughSection(
                        title = stringResource(R.string.dough_section_preferment),
                        summary = prefermentSummary,
                        expanded = openSection == SECTION_PREFERMENT,
                        onToggle = { toggleSection(SECTION_PREFERMENT) },
                    ) {
                        SubtleChoice(
                            label = "",
                            options = Preferment.entries,
                            selected = preferment,
                            optionLabel = { stringResource(prefermentLabel(it)) },
                            onSelect = { selectPreferment(it) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            stringResource(prefermentExplanation(preferment)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (preferment != Preferment.NONE) Fields(prefermentFlourField, prefermentWaterField)
                    }
                    DoughSection(
                        title = stringResource(R.string.dough_section_dough),
                        summary = doughSummary,
                        expanded = openSection == SECTION_DOUGH,
                        onToggle = { toggleSection(SECTION_DOUGH) },
                    ) {
                        val doughFields = buildList<@Composable (Modifier) -> Unit> {
                            add(countField)
                            add(pieceField)
                            add(waterField)
                            add(saltField)
                            addAll(shownExtras.map(extraField))
                            add(oilField)
                        }
                        // „Weitere Zutat“ steht neben dem letzten Feld, wenn dort Platz ist, sonst darunter.
                        val odd = doughFields.size % 2 == 1
                        Fields(*doughFields.toTypedArray(), trailing = if (odd) addExtraButton else null)
                        if (!odd) addExtraButton()
                        if (extrasNote) {
                            Text(
                                stringResource(R.string.dough_extras_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        flourHint()
                        Text(
                            stringResource(R.string.dough_percent_line),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Gehzeiten und Temperaturen (Entwurf 10C, Wahl vom 12.11.2026): eine ruhige Liste mit den Werten.
                    DoughSection(
                        title = stringResource(R.string.dough_section_process),
                        summary = processSummary,
                        expanded = openSection == SECTION_PROCESS,
                        onToggle = { toggleSection(SECTION_PROCESS) },
                    ) {
                        processContent()
                    }
                }
                Text(
                    stringResource(R.string.dough_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.m),
                )
                return@Column
            }

            Card(stringResource(R.string.dough_section_yeast), onHelp = { help = "yeast" }) {
                ChoiceField(
                    label = stringResource(R.string.dough_section_yeast),
                    options = YeastKind.entries,
                    selected = yeastKind,
                    optionLabel = { stringResource(AndroidDoughTexts.yeastName(it)) },
                    onSelect = { selectYeastKind(it) },
                )
                TextButton(onClick = { help = yeastKind.key }) {
                    Text(stringResource(R.string.dough_help_term_open, stringResource(AndroidDoughTexts.yeastName(yeastKind))))
                }
                yeastField(Modifier.fillMaxWidth())
                if (preferment.carriesYeast) mainYeastField(Modifier.fillMaxWidth())
            }

            Card(stringResource(R.string.dough_section_dough), onHelp = { help = "dough" }) {
                Fields(countField, pieceField)
                Fields(
                    waterField,
                    saltField,
                    // Der Hinweis steht neben dem Salz, statt eine eigene Zeile zu belegen.
                    trailing = {
                        Text(
                            stringResource(R.string.dough_percent_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            Card(stringResource(R.string.dough_section_extras)) {
                for (spec in shownExtras) extraField(spec)(Modifier.fillMaxWidth())
                oilField(Modifier.fillMaxWidth())
                if (extrasNote) {
                    Text(
                        stringResource(R.string.dough_extras_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                flourHint()
                addExtraButton()
            }

            // Der Vorteig ist hervorgehoben (Rahmen in der Hauptfarbe): Auswahl, Anteile und die Mengen des Vorteigs als Kacheln,
            // dazu ein Fenster, das Biga, Poolish und Sauerteig erklärt (Entwurf vom 22.10.2026).
            Card(stringResource(R.string.dough_section_preferment), onHelp = { help = "preferment" }, highlighted = true) {
                ChoiceField(
                    label = stringResource(R.string.dough_section_preferment),
                    options = Preferment.entries,
                    selected = preferment,
                    optionLabel = { stringResource(prefermentLabel(it)) },
                    onSelect = { selectPreferment(it) },
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
                            modifier = rememberScrollState().let { Modifier.scrollFade(it).horizontalScroll(it) },
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            for (item in tiles) TotalTile(item)
                        }
                    }
                    Fields(prefermentFlourField, prefermentWaterField)
                }
            }

            Card(stringResource(R.string.dough_section_process)) {
                Text(processSummary, style = MaterialTheme.typography.bodyMedium)
                processContent()
            }

            // Mit Vorteig steht hier die Aufteilung; die Summen stehen immer unten.
            if (result is DoughResult.Success && preferment != Preferment.NONE) {
                val draft = viewModel.draft(result, input, process)
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
                YeastEquivalent(result, yeastKind, Modifier.padding(horizontal = Spacing.m))
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
            TextButton(onClick = { reset() }, modifier = Modifier.padding(horizontal = Spacing.s)) {
                Text(stringResource(R.string.dough_reset))
            }
        }

        if (pinned) {
            SaveBar(enabled = success != null, onSave = { viewModel.saveAsRecipe(input, process, onSaved) })
        } else {
            // Große Schrift: Die Aufteilung des Vorteigs steht schon als Karte im Formular; in der Leiste nähme sie
            // dem Formular den Platz (bei Schrift 200 % blieben nur wenige Zeilen).
            ResultBar(
                summary = if (success != null) leavenedSummary(success, input, unit) else null,
                totals = resultTotals,
                onSave = { viewModel.saveAsRecipe(input, process, onSaved) },
            )
        }
    }
}

/**
 * Oben fest (Entwurf 5 vom 04.11.2026): als erste Kachel der Gesamtteig, dann die Gesamtmengen; darunter Zeilen wie
 * „Biga: …“ (was am Abend vorher angesetzt wird) und „Hauptteig: …“ (was zum Kneten dazukommt). [total] null =
 * ungültig. Passen die Kacheln nicht nebeneinander, lassen sie sich seitlich verschieben.
 */
@Composable
internal fun ResultTop(total: String?, totals: List<TotalItem>?, lines: List<String> = emptyList()) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.xs),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (total == null) {
                Text(
                    stringResource(R.string.dough_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = Spacing.xs).semantics { liveRegion = LiveRegionMode.Polite },
                )
            } else {
                Row(
                    modifier = rememberScrollState().let { Modifier.scrollFade(it).horizontalScroll(it) },
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    TotalTile(
                        TotalItem(total, stringResource(R.string.dough_tile_total)),
                        emphasized = true,
                        compact = true,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    for (item in totals.orEmpty()) TotalTile(item, compact = true)
                }
                for (line in lines) {
                    Text(line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics(mergeDescendants = true) {})
                }
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
internal fun summaryLine(result: DoughResult.Success, draft: DoughDraft, unit: WeightUnit): String = stringResource(
    R.string.dough_total_line,
    DoughNumbers.weight(result.totalGrams, 0, draft.language, unit),
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
                    modifier = rememberScrollState().let { Modifier.scrollFade(it).horizontalScroll(it) },
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

/**
 * Eine Gesamtmenge als Kachel: die Menge groß, darunter der Name. [emphasized]: der Gesamtteig, in der Hauptfarbe.
 * [compact]: etwas kleinere Schrift, für die Ergebniskarte oben (Entwurf 5).
 */
@Composable
private fun TotalTile(item: TotalItem, emphasized: Boolean = false, compact: Boolean = false, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (emphasized) colors.primary else colors.surface,
        contentColor = if (emphasized) colors.onPrimary else colors.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs).semantics(mergeDescendants = true) {},
        ) {
            Text(
                item.amount,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                item.name,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = if (emphasized) colors.onPrimary else colors.onSurfaceVariant,
            )
        }
    }
}

/** „Gleichwertige Hefemenge: 0,4 g Instanthefe, 0,5 g Aktivhefe“; nichts, wenn der Teig keine Hefe hat. */
@Composable
private fun YeastEquivalent(result: DoughResult.Success, yeastKind: YeastKind, modifier: Modifier = Modifier) {
    val yeastGrams = DoughSummary.yeastGrams(result)
    if (yeastGrams <= 0) return
    val language = LocalConfiguration.current.locales[0].language
    val resources = LocalResources.current
    val others = YeastKind.entries.filter { it != yeastKind }.joinToString(", ") { other ->
        val grams = DoughNumbers.grams(yeastKind.convert(yeastGrams, other), LineRole.YEAST, language)
        resources.getString(R.string.dough_yeast_item, grams, resources.getString(AndroidDoughTexts.yeastName(other)))
    }
    Text(stringResource(R.string.dough_yeast_equal, others), style = MaterialTheme.typography.bodySmall, modifier = modifier)
}

/** Ein Wert in der Zusammenfassung eines Bereichs, z. B. „Wasser 60 %“; [value] wie eingegeben. */
@Composable
internal fun summaryValue(@StringRes name: Int, value: String, unit: String = "%"): String =
    stringResource(R.string.dough_summary_value, stringResource(name), "${value.trim()} $unit")

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

/** Pizza und Brot: „Gesamtteig: 1000 g (4 × 250 g)“. */
@Composable
internal fun leavenedSummary(result: DoughResult.Success, input: LeavenedInput, unit: WeightUnit): String {
    val language = LocalConfiguration.current.locales[0].language
    return stringResource(
        R.string.dough_total_pieces,
        DoughNumbers.weight(result.totalGrams, 0, language, unit),
        input.count,
        DoughNumbers.weight(input.pieceGrams, 1, language, unit),
    )
}

/** Grenzen einer Meldung in der gewählten Gewichtseinheit (die Grenzen selbst gelten in Gramm). */
internal fun DoughIssue?.inUnit(unit: WeightUnit): DoughIssue? =
    if (this is DoughIssue.OutOfRange && unit != WeightUnit.GRAMS) DoughIssue.OutOfRange(field, unit.fromGrams(min), unit.fromGrams(max)) else this

/** Eine Mehlsorte oder weitere Zutat im Teigrechner: Kennung, Feld, Beschriftung, Eingabetext und wie er sich ändert. */
private class ExtraSpec(
    val key: String,
    val field: DoughField,
    @androidx.annotation.StringRes val label: Int,
    val text: String,
    val set: (String) -> Unit,
)

/**
 * Auswahl als Schaltflächen wie die Teigart oben, nur dezenter (kleinere Schrift, ruhige Farben): Hefeart und Vorteig.
 * Ohne [label] (leer) steht nur die Reihe, z. B. in einem Bereich, dessen Name schon darüber steht.
 */
@Composable
internal fun <T> SubtleChoice(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        activeBorderColor = MaterialTheme.colorScheme.outline,
                        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    contentPadding = PaddingValues(horizontal = Spacing.xs),
                ) {
                    Text(
                        optionLabel(option),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (option == selected) FontWeight.Bold else null,
                        maxLines = maxLines,
                    )
                }
            }
        }
    }
}
