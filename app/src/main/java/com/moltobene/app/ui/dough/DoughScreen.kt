package com.moltobene.app.ui.dough

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.moltobene.app.data.dough.DoughResult
import com.moltobene.app.data.dough.DoughStepper
import com.moltobene.app.data.dough.DoughType
import com.moltobene.app.data.dough.LeavenedInput
import com.moltobene.app.data.dough.LineRole
import com.moltobene.app.data.dough.Preferment
import com.moltobene.app.data.dough.YeastKind
import com.moltobene.app.ui.components.AppTitle
import com.moltobene.app.ui.components.ScreenTitle
import com.moltobene.app.ui.components.ScreenTitlePadding
import com.moltobene.app.ui.components.isLargeText
import com.moltobene.app.ui.theme.Spacing
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
    onSaved: (String) -> Unit,
) {
    var typeKey by rememberSaveable { mutableStateOf(DoughType.PIZZA.key) }
    val type = DoughType.fromKey(typeKey) ?: DoughType.PIZZA
    Scaffold(
        topBar = {
            TopAppBar(
                title = { AppTitle(style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val header: @Composable () -> Unit = { Header(type) { typeKey = it.key } }
            when (type) {
                DoughType.PIZZA, DoughType.BREAD -> LeavenedContent(type, viewModel, onSaved, header)
                DoughType.PASTA -> PastaContent(viewModel, onSaved, header)
                DoughType.CAKE -> CakeContent(viewModel, onSaved, header)
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
    ScreenTitle(stringResource(R.string.dough_title), modifier = Modifier.padding(ScreenTitlePadding))
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
internal fun Card(title: String, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
    ) {
        Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

/** Felder untereinander, jedes über die volle Breite, damit Platz für Plus und Minus bleibt (#83). */
@Composable
internal fun Fields(vararg fields: @Composable (Modifier) -> Unit, weights: List<Float>? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) { fields.forEach { it(Modifier.fillMaxWidth()) } }
}

/** Plus, Minus und die Markierung „Standard“ eines Felds (#83): Schrittweite, Standardwert, Grenzen. */
internal class Stepping(val step: Double, val default: Double? = null, val range: DoughLimits.Range? = null)

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
    val marker = when {
        default == null -> null
        atDefault -> stringResource(R.string.dough_default_marker)
        else -> stringResource(R.string.dough_default_value, show(default) + suffix?.let { " $it" }.orEmpty())
    }
    val hint = error ?: marker
    val field: @Composable (Modifier) -> Unit = { m ->
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label, maxLines = 1) },
            suffix = suffix?.let { { Text(it) } },
            isError = error != null,
            supportingText = hint?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
            modifier = m,
        )
    }
    if (stepping == null) {
        field(modifier)
        return
    }
    fun text(number: Double) = if (integer) number.toLong().toString() else show(number)
    val buttons: @Composable () -> Unit = {
        val stepText = show(stepping.step) + suffix?.let { " $it" }.orEmpty()
        FilledTonalIconButton(
            onClick = { onChange(text(DoughStepper.next(current, stepping.step, -1, stepping.range, default))) },
            enabled = DoughStepper.canStep(current, -1, stepping.range),
        ) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.dough_step_less, label, stepText))
        }
        FilledTonalIconButton(
            onClick = { onChange(text(DoughStepper.next(current, stepping.step, +1, stepping.range, default))) },
            enabled = DoughStepper.canStep(current, +1, stepping.range),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.dough_step_more, label, stepText))
        }
        if (default != null) {
            IconButton(onClick = { onChange(text(default)) }, enabled = !atDefault) {
                Icon(painterResource(R.drawable.ic_undo), contentDescription = stringResource(R.string.dough_default_reset, label))
            }
        }
    }
    if (isLargeText()) {
        // Bei großer Schrift hat das Feld die volle Breite, die Knöpfe stehen darunter.
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            field(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) { buttons() }
        }
    } else {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            field(Modifier.weight(1f))
            Row(modifier = Modifier.padding(top = Spacing.xs)) { buttons() }
        }
    }
}

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
        Stepping(DoughStepper.stepFor(field), default, DoughLimits.range(type, field, preferment))
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

    val bread = type == DoughType.BREAD
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            header()

            Card(stringResource(R.string.dough_section_dough)) {
                Fields(
                    { m ->
                        NumberField(
                            stringResource(if (bread) R.string.dough_count_loaf else R.string.dough_count_ball), count,
                            { count = it }, issueFor(DoughField.COUNT), m, integer = true,
                            stepping = stepping(DoughField.COUNT, defaults.count.toDouble()),
                        )
                    },
                    { m ->
                        NumberField(
                            stringResource(R.string.dough_piece), piece, { piece = it },
                            issueFor(DoughField.PIECE_GRAMS), m, suffix = "g",
                            stepping = stepping(DoughField.PIECE_GRAMS, defaults.pieceGrams),
                        )
                    },
                )
                Text(
                    stringResource(R.string.dough_percent_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Fields(
                    { m ->
                        NumberField(
                            stringResource(R.string.dough_water), water, { water = it }, issueFor(DoughField.WATER), m, suffix = "%",
                            stepping = stepping(DoughField.WATER, defaults.water),
                        )
                    },
                    { m ->
                        NumberField(
                            stringResource(R.string.dough_salt), salt, { salt = it }, issueFor(DoughField.SALT), m, suffix = "%",
                            stepping = stepping(DoughField.SALT, defaults.salt),
                        )
                    },
                    { m ->
                        NumberField(
                            stringResource(R.string.dough_oil), oil, { oil = it }, issueFor(DoughField.OIL), m, suffix = "%",
                            stepping = stepping(DoughField.OIL, defaults.oil),
                        )
                    },
                )
            }

            Card(stringResource(R.string.dough_section_yeast)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    for (kind in YeastKind.entries) {
                        FilterChip(
                            selected = kind == yeastKind,
                            onClick = {
                                // Die Menge bleibt gleichwertig, wenn die Hefeart wechselt.
                                val current = parseNumber(yeast)
                                if (current.isFinite()) {
                                    yeast = DoughNumbers.separator(
                                        DoughNumbers.plain(yeastKind.convert(current, kind), 3),
                                        Locale.getDefault().language,
                                    )
                                }
                                yeastKindKey = kind.key
                            },
                            label = { Text(stringResource(AndroidDoughTexts.yeastName(kind))) },
                        )
                    }
                }
                NumberField(
                    stringResource(R.string.dough_yeast_amount), yeast, { yeast = it },
                    issueFor(DoughField.YEAST), Modifier.fillMaxWidth(), suffix = "%",
                    stepping = stepping(DoughField.YEAST, YeastKind.FRESH.convert(defaults.yeast, yeastKind)),
                )
            }

            Card(stringResource(R.string.dough_section_preferment)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    for (option in Preferment.entries) {
                        FilterChip(
                            selected = option == preferment,
                            onClick = {
                                prefermentKey = option.key
                                // Biga und Poolish haben feste Wasseranteile, die hier vorausgefüllt werden.
                                if (option != Preferment.NONE) prefermentWater = show(DoughLimits.defaultPrefermentWater(option))
                            },
                            label = { Text(stringResource(prefermentLabel(option))) },
                        )
                    }
                }
                Text(
                    stringResource(prefermentExplanation(preferment)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (preferment != Preferment.NONE) {
                    Fields(
                        { m ->
                            NumberField(
                                stringResource(R.string.dough_preferment_flour), prefermentFlour, { prefermentFlour = it },
                                issueFor(DoughField.PREFERMENT_FLOUR), m, suffix = "%",
                                stepping = stepping(DoughField.PREFERMENT_FLOUR, defaults.prefermentFlour),
                            )
                        },
                        { m ->
                            NumberField(
                                stringResource(R.string.dough_preferment_water), prefermentWater, { prefermentWater = it },
                                issueFor(DoughField.PREFERMENT_WATER), m, suffix = "%",
                                stepping = stepping(DoughField.PREFERMENT_WATER, DoughLimits.defaultPrefermentWater(preferment)),
                            )
                        },
                    )
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

        val success = result as? DoughResult.Success
        val draft = success?.let { viewModel.draft(it, input) }
        ResultBar(
            summary = if (success != null && draft != null) summaryLine(success, draft) else null,
            totals = success?.let { viewModel.totals(it, input) },
            onSave = { viewModel.saveAsRecipe(input, onSaved) },
        )
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
internal fun ResultBar(summary: String?, totals: List<TotalItem>?, onSave: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
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
