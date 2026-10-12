package com.moltobene.app.ui.dough

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moltobene.app.R
import com.moltobene.app.data.dough.DoughCalculator
import com.moltobene.app.data.dough.DoughField
import com.moltobene.app.data.dough.DoughIssue
import com.moltobene.app.data.dough.DoughLimits
import com.moltobene.app.data.dough.DoughNumbers
import com.moltobene.app.data.dough.DoughResult
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

/** Zahl aus einem Eingabefeld: Komma oder Punkt; leer oder Text ergibt NaN (wird als „keine Zahl“ gemeldet). */
internal fun parseNumber(text: String): Double =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: Double.NaN

private fun show(value: Double): String =
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
    val loaded by produceState<LeavenedInput?>(null, type) {
        value = null
        value = viewModel.load(type)
    }
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
            val initial = loaded
            if (initial == null) {
                Header(type) { typeKey = it.key }
                CircularProgressIndicator(Modifier.padding(Spacing.m))
            } else {
                DoughForm(initial, viewModel, onSaved) { Header(type) { typeKey = it.key } }
            }
        }
    }
}

/** Überschrift und Wahl der Teigart: ein Schalter über die volle Breite. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(selected: DoughType, onSelect: (DoughType) -> Unit) {
    ScreenTitle(stringResource(R.string.dough_title), modifier = Modifier.padding(ScreenTitlePadding))
    val types = listOf(DoughType.PIZZA, DoughType.BREAD)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m)) {
        types.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index, types.size),
            ) {
                Text(stringResource(if (type == DoughType.BREAD) R.string.dough_type_bread else R.string.dough_type_pizza))
            }
        }
    }
}

/** Eine Karte mit Überschrift; gliedert das Formular ruhig und kompakt. */
@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
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

/** Felder nebeneinander; bei großer Schrift untereinander. */
@Composable
private fun Fields(vararg fields: @Composable (Modifier) -> Unit, weights: List<Float>? = null) {
    if (isLargeText()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) { fields.forEach { it(Modifier.fillMaxWidth()) } }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            fields.forEachIndexed { index, field -> field(Modifier.weight(weights?.getOrNull(index) ?: 1f)) }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    issue: DoughIssue?,
    modifier: Modifier = Modifier,
    integer: Boolean = false,
    suffix: String? = null,
) {
    val error = issue?.let { issueText(it) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, maxLines = 1) },
        suffix = suffix?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun issueText(issue: DoughIssue): String? = when (issue) {
    is DoughIssue.NotANumber -> stringResource(R.string.dough_error_number)
    is DoughIssue.OutOfRange -> stringResource(R.string.dough_error_range, show(issue.min), show(issue.max))
    is DoughIssue.PrefermentWaterTooHigh ->
        stringResource(R.string.dough_error_preferment, show(issue.neededPercent), show(issue.totalPercent))
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
        if (result is DoughResult.Success) viewModel.remember(input)
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
                        )
                    },
                    { m ->
                        NumberField(
                            stringResource(R.string.dough_piece), piece, { piece = it },
                            issueFor(DoughField.PIECE_GRAMS), m, suffix = "g",
                        )
                    },
                )
                Text(
                    stringResource(R.string.dough_percent_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Fields(
                    { m -> NumberField(stringResource(R.string.dough_water), water, { water = it }, issueFor(DoughField.WATER), m, suffix = "%") },
                    { m -> NumberField(stringResource(R.string.dough_salt), salt, { salt = it }, issueFor(DoughField.SALT), m, suffix = "%") },
                    { m -> NumberField(stringResource(R.string.dough_oil), oil, { oil = it }, issueFor(DoughField.OIL), m, suffix = "%") },
                    weights = listOf(1.6f, 1f, 1f),
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
                            )
                        },
                        { m ->
                            NumberField(
                                stringResource(R.string.dough_preferment_water), prefermentWater, { prefermentWater = it },
                                issueFor(DoughField.PREFERMENT_WATER), m, suffix = "%",
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
                Text(
                    viewModel.totals(result, input),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = Spacing.m),
                )
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

        // Immer sichtbar: die Summen und „Als Rezept speichern“ – auch beim Ändern der Eingaben weiter oben.
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                Column(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                    when (result) {
                        is DoughResult.Success -> {
                            val draft = viewModel.draft(result, input)
                            Text(
                                stringResource(
                                    R.string.dough_total_line,
                                    DoughNumbers.separator(DoughNumbers.plain(result.totalGrams, 0), draft.language),
                                    "${draft.servings} ${draft.servingsUnit}",
                                ),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            // Bei großer Schrift stehen die Summen im Formular, damit die Leiste unten klein bleibt.
                            if (!isLargeText()) Text(viewModel.totals(result, input), style = MaterialTheme.typography.bodyMedium)
                        }
                        is DoughResult.Invalid -> Text(
                            stringResource(R.string.dough_invalid),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Button(
                    onClick = { viewModel.saveAsRecipe(input, onSaved) },
                    enabled = result is DoughResult.Success,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.dough_save)) }
            }
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

