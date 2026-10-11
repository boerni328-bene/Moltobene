package com.moltobene.app.ui.dough

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
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
import com.moltobene.app.ui.theme.Spacing

/** Zahl aus einem Eingabefeld: Komma oder Punkt; leer oder Text ergibt NaN (wird als „keine Zahl“ gemeldet). */
internal fun parseNumber(text: String): Double =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: Double.NaN

private fun show(value: Double): String =
    DoughNumbers.separator(DoughNumbers.plain(value, 2), java.util.Locale.getDefault().language)

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
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            ScreenTitle(stringResource(R.string.dough_title), modifier = Modifier.padding(ScreenTitlePadding))
            TypeChoice(type) { typeKey = it.key }
            val initial = loaded
            if (initial == null) {
                CircularProgressIndicator(Modifier.padding(Spacing.m))
            } else {
                DoughForm(initial, viewModel, onSaved)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeChoice(selected: DoughType, onSelect: (DoughType) -> Unit) {
    FlowRow(
        modifier = Modifier.padding(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        for (type in listOf(DoughType.PIZZA, DoughType.BREAD)) {
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = {
                    Text(stringResource(if (type == DoughType.BREAD) R.string.dough_type_bread else R.string.dough_type_pizza))
                },
            )
        }
    }
}

@Composable
private fun Section(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs).semantics { heading() },
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    issue: DoughIssue?,
    integer: Boolean = false,
) {
    val error = issue?.let { issueText(it) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
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
private fun DoughForm(initial: LeavenedInput, viewModel: DoughViewModel, onSaved: (String) -> Unit) {
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
    Section(stringResource(R.string.dough_section_dough))
    NumberField(
        stringResource(if (bread) R.string.dough_count_loaf else R.string.dough_count_ball), count,
        { count = it }, issueFor(DoughField.COUNT), integer = true,
    )
    NumberField(
        stringResource(if (bread) R.string.dough_piece_loaf else R.string.dough_piece_ball), piece,
        { piece = it }, issueFor(DoughField.PIECE_GRAMS),
    )
    NumberField(stringResource(R.string.dough_water), water, { water = it }, issueFor(DoughField.WATER))
    NumberField(stringResource(R.string.dough_salt), salt, { salt = it }, issueFor(DoughField.SALT))
    NumberField(stringResource(R.string.dough_oil), oil, { oil = it }, issueFor(DoughField.OIL))

    Section(stringResource(R.string.dough_section_yeast))
    FlowRow(
        modifier = Modifier.padding(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        for (kind in YeastKind.entries) {
            FilterChip(
                selected = kind == yeastKind,
                onClick = {
                    // Die Menge bleibt gleichwertig, wenn die Hefeart wechselt.
                    val current = parseNumber(yeast)
                    if (current.isFinite()) yeast = DoughNumbers.separator(DoughNumbers.plain(yeastKind.convert(current, kind), 3), java.util.Locale.getDefault().language)
                    yeastKindKey = kind.key
                },
                label = { Text(stringResource(AndroidDoughTexts.yeastName(kind))) },
            )
        }
    }
    NumberField(stringResource(R.string.dough_yeast_amount), yeast, { yeast = it }, issueFor(DoughField.YEAST))

    Section(stringResource(R.string.dough_section_preferment))
    Text(
        stringResource(R.string.dough_explain_preferment),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = Spacing.m),
    )
    FlowRow(
        modifier = Modifier.padding(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
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
    prefermentExplanation(preferment)?.let {
        Text(
            stringResource(it),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.m),
        )
    }
    if (preferment != Preferment.NONE) {
        NumberField(
            stringResource(R.string.dough_preferment_flour), prefermentFlour, { prefermentFlour = it },
            issueFor(DoughField.PREFERMENT_FLOUR),
        )
        NumberField(
            stringResource(R.string.dough_preferment_water), prefermentWater, { prefermentWater = it },
            issueFor(DoughField.PREFERMENT_WATER),
        )
    }

    Section(stringResource(R.string.dough_section_result))
    when (result) {
        is DoughResult.Success -> {
            val draft = viewModel.draft(result, input)
            Column(
                modifier = Modifier.padding(horizontal = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    "${draft.servings} ${draft.servingsUnit}",
                    style = MaterialTheme.typography.titleSmall,
                )
                for (line in draft.ingredients) {
                    Text(
                        line.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (line.isHeading) FontWeight.Bold else null,
                        modifier = Modifier.padding(top = if (line.isHeading) Spacing.s else 0.dp),
                    )
                }
                Text(
                    stringResource(R.string.dough_total, DoughNumbers.separator(DoughNumbers.plain(result.totalGrams, 0), draft.language)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.s),
                )
                val yeastGrams = result.sections.flatMap { it.lines }.filter { it.role == LineRole.YEAST }.sumOf { it.grams }
                if (yeastGrams > 0) {
                    val resources = LocalResources.current
                    val others = YeastKind.entries.filter { it != yeastKind }.joinToString(", ") { other ->
                        val grams = DoughNumbers.grams(yeastKind.convert(yeastGrams, other), LineRole.YEAST, draft.language)
                        resources.getString(R.string.dough_yeast_item, grams, resources.getString(AndroidDoughTexts.yeastName(other)))
                    }
                    Text(stringResource(R.string.dough_yeast_equal, others), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is DoughResult.Invalid -> Text(
            stringResource(R.string.dough_invalid),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.m),
        )
    }
    Text(
        stringResource(R.string.dough_hint),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = Spacing.m),
    )
    Button(
        onClick = { viewModel.saveAsRecipe(input, onSaved) },
        enabled = result is DoughResult.Success,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
    ) { Text(stringResource(R.string.dough_save)) }
    OutlinedButton(
        onClick = { resetCount += 1 },
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
    ) { Text(stringResource(R.string.dough_reset)) }
    Column(Modifier.padding(bottom = Spacing.l)) {}
}

private fun prefermentLabel(preferment: Preferment): Int = when (preferment) {
    Preferment.NONE -> R.string.dough_pre_none
    Preferment.BIGA -> R.string.dough_pre_biga
    Preferment.POOLISH -> R.string.dough_pre_poolish
    Preferment.SOURDOUGH -> R.string.dough_pre_sourdough
}

private fun prefermentExplanation(preferment: Preferment): Int? = when (preferment) {
    Preferment.NONE -> null
    Preferment.BIGA -> R.string.dough_explain_biga
    Preferment.POOLISH -> R.string.dough_explain_poolish
    Preferment.SOURDOUGH -> R.string.dough_explain_sourdough
}
