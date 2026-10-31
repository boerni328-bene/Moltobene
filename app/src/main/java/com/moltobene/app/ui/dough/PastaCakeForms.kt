package com.moltobene.app.ui.dough

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.moltobene.app.R
import com.moltobene.app.data.Pan
import com.moltobene.app.data.PanScaling
import com.moltobene.app.data.dough.CakeCalculator
import com.moltobene.app.data.dough.CakeField
import com.moltobene.app.data.dough.CakeInput
import com.moltobene.app.data.dough.CakeKind
import com.moltobene.app.data.dough.CakeLimits
import com.moltobene.app.data.dough.CakeSize
import com.moltobene.app.data.dough.DoughConstants
import com.moltobene.app.data.dough.DoughField
import com.moltobene.app.data.dough.DoughIssue
import com.moltobene.app.data.dough.DoughNumbers
import com.moltobene.app.data.dough.DoughResult
import com.moltobene.app.data.dough.DoughStepper
import com.moltobene.app.data.dough.PastaCalculator
import com.moltobene.app.data.dough.PastaInput
import com.moltobene.app.data.dough.PastaKind
import com.moltobene.app.data.dough.PastaLimits
import com.moltobene.app.data.dough.LineRole
import com.moltobene.app.data.dough.YeastKind
import com.moltobene.app.ui.theme.Spacing
import java.util.Locale
import kotlinx.coroutines.delay

private const val SUFFIX_PERCENT = "%"

@Composable
internal fun PastaContent(viewModel: DoughViewModel, onSaved: (String) -> Unit, header: @Composable () -> Unit) {
    var kindKey by rememberSaveable { mutableStateOf(PastaKind.EGG.key) }
    val kind = PastaKind.fromKey(kindKey) ?: PastaKind.EGG
    val loaded by produceState<PastaInput?>(null, kind) {
        value = null
        value = viewModel.loadPasta(kind)
    }
    val initial = loaded
    if (initial == null) {
        Column { header() }
        CircularProgressIndicator(Modifier.padding(Spacing.m))
    } else {
        PastaForm(initial, viewModel, onSaved, header) { kindKey = it.key }
    }
}

/** Zeigt unter dem Feld, was nicht stimmt. */
private fun List<DoughIssue>.forField(field: DoughField): DoughIssue? = firstOrNull {
    (it is DoughIssue.NotANumber && it.field == field) || (it is DoughIssue.OutOfRange && it.field == field)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PastaForm(
    initial: PastaInput,
    viewModel: DoughViewModel,
    onSaved: (String) -> Unit,
    header: @Composable () -> Unit,
    onKind: (PastaKind) -> Unit,
) {
    val kind = initial.kind
    var resetCount by rememberSaveable(kind) { mutableStateOf(0) }
    val start = if (resetCount == 0) initial else PastaLimits.defaults(kind)
    var portions by rememberSaveable(kind, resetCount) { mutableStateOf(start.portions.toString()) }
    var flour by rememberSaveable(kind, resetCount) { mutableStateOf(show(start.flourPerPortion)) }
    var semolina by rememberSaveable(kind, resetCount) { mutableStateOf(show(start.semolina)) }
    var egg by rememberSaveable(kind, resetCount) { mutableStateOf(show(start.egg)) }
    var yolk by rememberSaveable(kind, resetCount) { mutableStateOf(show(start.yolk)) }
    var water by rememberSaveable(kind, resetCount) { mutableStateOf(show(start.water)) }
    var salt by rememberSaveable(kind, resetCount) { mutableStateOf(show(start.salt)) }
    var oil by rememberSaveable(kind, resetCount) { mutableStateOf(show(start.oil)) }

    val defaults = PastaLimits.defaults(kind)
    fun stepping(field: DoughField, default: Double) =
        Stepping(DoughStepper.stepFor(field), default, PastaLimits.range(kind, field), field)
    val input = PastaInput(
        kind = kind,
        portions = portions.trim().toIntOrNull() ?: 0,
        flourPerPortion = parseNumber(flour),
        semolina = parseNumber(semolina),
        egg = if (kind == PastaKind.EGG) parseNumber(egg) else 0.0,
        yolk = if (kind == PastaKind.EGG) parseNumber(yolk) else 0.0,
        water = parseNumber(water),
        salt = parseNumber(salt),
        oil = parseNumber(oil),
    )
    val result = PastaCalculator.calculate(input)
    val issues = (result as? DoughResult.Invalid)?.issues.orEmpty()
    LaunchedEffect(input, result is DoughResult.Success) {
        if (result is DoughResult.Success) {
            delay(MEMORY_DELAY_MILLIS)
            viewModel.rememberPasta(input)
        }
    }

    val success = result as? DoughResult.Success
    val resultTotals = success?.let { viewModel.totals(it, YeastKind.FRESH) }
    val draft = success?.let { viewModel.draftPasta(it, input) }
    val portionsField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_portions), portions, { portions = it }, issues.forField(DoughField.COUNT), m,
            integer = true, stepping = stepping(DoughField.COUNT, defaults.portions.toDouble()),
        )
    }
    val flourField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_flour_per_portion), flour, { flour = it },
            issues.forField(DoughField.FLOUR_PER_PORTION), m, suffix = "g",
            stepping = stepping(DoughField.FLOUR_PER_PORTION, defaults.flourPerPortion),
        )
    }
    val semolinaField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_semolina), semolina, { semolina = it },
            issues.forField(DoughField.SEMOLINA), m, suffix = SUFFIX_PERCENT,
            stepping = stepping(DoughField.SEMOLINA, defaults.semolina),
        )
    }
    val waterField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_water), water, { water = it }, issues.forField(DoughField.WATER), m, suffix = SUFFIX_PERCENT,
            stepping = stepping(DoughField.WATER, defaults.water),
        )
    }
    val eggField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_egg_whole), egg, { egg = it }, issues.forField(DoughField.WHOLE_EGG), m, suffix = SUFFIX_PERCENT,
            stepping = stepping(DoughField.WHOLE_EGG, defaults.egg),
        )
    }
    val yolkField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_yolk), yolk, { yolk = it }, issues.forField(DoughField.YOLK), m, suffix = SUFFIX_PERCENT,
            stepping = stepping(DoughField.YOLK, defaults.yolk),
        )
    }
    val saltField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_salt), salt, { salt = it }, issues.forField(DoughField.SALT), m, suffix = SUFFIX_PERCENT,
            stepping = stepping(DoughField.SALT, defaults.salt),
        )
    }
    val oilField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_oil), oil, { oil = it }, issues.forField(DoughField.OIL), m, suffix = SUFFIX_PERCENT,
            stepping = stepping(DoughField.OIL, defaults.oil),
        )
    }
    // Hinweise zu Ei und Flüssigkeit: bei Eierteig unter den Feldern.
    val eggNotes: @Composable () -> Unit = {
        issues.filterIsInstance<DoughIssue.TooLittleLiquid>().firstOrNull()?.let {
            Text(
                stringResource(R.string.dough_error_liquid, show(it.neededPercent)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (success != null) {
            val lines = success.sections.flatMap { it.lines }
            EggHints(
                lines.filter { it.role == LineRole.EGG }.sumOf { it.grams },
                lines.filter { it.role == LineRole.YOLK }.sumOf { it.grams },
            )
        }
    }

    FormFrame(
        header = header,
        summary = if (success != null && draft != null) summaryLine(success, draft) else null,
        totals = resultTotals,
        onSave = { viewModel.savePasta(input, onSaved) },
        onReset = { resetCount += 1 },
        extra = resultTotals,
        compact = {
            // Alles in einem Bild (Entwurf vom 30.10.2026): Teigart als Auswahlfeld, die Felder paarweise.
            val kindChoice: @Composable (Modifier) -> Unit = { m ->
                CompactChoice(
                    label = stringResource(R.string.dough_section_dough),
                    options = PastaKind.entries,
                    selected = kind,
                    optionLabel = { stringResource(if (it == PastaKind.EGG) R.string.dough_pasta_egg else R.string.dough_pasta_water) },
                    onSelect = onKind,
                    modifier = m,
                )
            }
            val compactFields = buildList<@Composable (Modifier) -> Unit> {
                add(kindChoice)
                add(portionsField)
                add(flourField)
                add(semolinaField)
                add(waterField)
                if (kind == PastaKind.EGG) {
                    add(eggField)
                    add(yolkField)
                }
                add(saltField)
                add(oilField)
            }
            Column(modifier = Modifier.padding(horizontal = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Fields(*compactFields.toTypedArray())
                if (kind == PastaKind.EGG) eggNotes()
            }
        },
    ) {
        Card(stringResource(R.string.dough_section_dough)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                for (option in PastaKind.entries) {
                    FilterChip(
                        selected = option == kind,
                        onClick = { onKind(option) },
                        label = { Text(stringResource(if (option == PastaKind.EGG) R.string.dough_pasta_egg else R.string.dough_pasta_water)) },
                    )
                }
            }
            Fields(portionsField, flourField)
        }
        Card(stringResource(R.string.dough_section_ingredients)) {
            Text(
                stringResource(R.string.dough_percent_note_all),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Fields(semolinaField, waterField)
            if (kind == PastaKind.EGG) {
                Fields(eggField, yolkField)
                eggNotes()
            }
            Fields(saltField, oilField)
        }
    }
}

/** „etwa 4 Eier, etwa 3 Eigelb“ aus den Gramm; Richtwerte. */
@Composable
internal fun EggHints(eggGrams: Double, yolkGrams: Double) {
    val eggs = DoughConstants.eggsAbout(eggGrams, DoughConstants.EGG_GRAMS)
    val yolks = DoughConstants.eggsAbout(yolkGrams, DoughConstants.YOLK_GRAMS)
    if (eggs > 0) {
        Text(
            pluralStringResource(R.plurals.dough_eggs_about, eggs, eggs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (yolks > 0) {
        Text(
            pluralStringResource(R.plurals.dough_yolks_about, yolks, yolks),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Rahmen eines Formulars. Bei normaler Schrift (Entwurf vom 30.10.2026): oben die Teigart, das Ergebnis, dann [compact]
 * ohne Karten und Scrollen, unten „Als Rezept speichern“. Bei großer Schrift: scrollend Teigart, Karten ([content]),
 * Hinweis und Zurücksetzen, unten die feste Leiste mit dem Ergebnis.
 */
@Composable
private fun FormFrame(
    header: @Composable () -> Unit,
    summary: String?,
    totals: List<TotalItem>?,
    onSave: () -> Unit,
    onReset: () -> Unit,
    extra: List<TotalItem>?,
    compact: @Composable () -> Unit,
    note: Int? = null,
    content: @Composable () -> Unit,
) {
    val noteText: @Composable () -> Unit = {
        if (note != null) {
            Text(
                stringResource(note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.m),
            )
        }
    }
    if (!com.moltobene.app.ui.components.isLargeText()) {
        // Zurücksetzen liegt oben in der Kopfleiste; Speichern als Voreinstellung gibt es hier nicht.
        val bar = LocalDoughBar.current
        SideEffect { bar.reset = onReset }
        Column(modifier = Modifier.fillMaxSize()) {
            header()
            ResultTop(summary = summary, totals = totals)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                compact()
                noteText()
                Text(
                    stringResource(R.string.dough_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.m),
                )
            }
            SaveBar(enabled = summary != null, onSave = onSave)
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            header()
            content()
            if (extra != null) {
                TotalsCard(extra)
            }
            noteText()
            Text(
                stringResource(R.string.dough_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.m),
            )
            TextButton(onClick = onReset, modifier = Modifier.padding(horizontal = Spacing.s)) {
                Text(stringResource(R.string.dough_reset))
            }
        }
        ResultBar(summary = summary, totals = totals, onSave = onSave)
    }
}
// ------------------------------------------------------------------ Kuchen

@Composable
internal fun CakeContent(viewModel: DoughViewModel, onSaved: (String) -> Unit, header: @Composable () -> Unit) {
    var kindKey by rememberSaveable { mutableStateOf(CakeKind.BATTER.key) }
    val kind = CakeKind.fromKey(kindKey) ?: CakeKind.BATTER
    val loaded by produceState<CakeInput?>(null, kind) {
        value = null
        value = viewModel.loadCake(kind)
    }
    val initial = loaded
    if (initial == null) {
        Column { header() }
        CircularProgressIndicator(Modifier.padding(Spacing.m))
    } else {
        CakeForm(initial, viewModel, onSaved, header) { kindKey = it.key }
    }
}

private fun CakeField.toDoughField() = when (this) {
    CakeField.FAT -> DoughField.FAT
    CakeField.SUGAR -> DoughField.SUGAR
    CakeField.EGG -> DoughField.EGG
    CakeField.MILK -> DoughField.MILK
    CakeField.QUARK -> DoughField.QUARK
    CakeField.YEAST -> DoughField.YEAST
    CakeField.BAKING_POWDER -> DoughField.BAKING_POWDER
    CakeField.SALT -> DoughField.SALT
}

private fun cakeFieldLabel(kind: CakeKind, field: CakeField): Int = when (field) {
    CakeField.FAT -> if (kind == CakeKind.QUARK_OIL) R.string.dough_oil else R.string.dough_label_butter
    CakeField.SUGAR -> R.string.dough_label_sugar
    CakeField.EGG -> R.string.dough_label_eggs
    CakeField.MILK -> if (kind == CakeKind.CHOUX) R.string.dough_label_liquid else R.string.dough_label_milk
    CakeField.QUARK -> R.string.dough_label_quark
    CakeField.YEAST -> R.string.dough_yeast_amount
    CakeField.BAKING_POWDER -> R.string.dough_label_baking_powder
    CakeField.SALT -> R.string.dough_salt
}

private fun fmt(value: Double, decimals: Int = 1): String =
    DoughNumbers.separator(DoughNumbers.plain(value, decimals), Locale.getDefault().language)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CakeForm(
    initial: CakeInput,
    viewModel: DoughViewModel,
    onSaved: (String) -> Unit,
    header: @Composable () -> Unit,
    onKind: (CakeKind) -> Unit,
) {
    val kind = initial.kind
    var resetCount by rememberSaveable(kind) { mutableStateOf(0) }
    val start = if (resetCount == 0) initial else CakeLimits.defaults(kind)
    val startPan = (start.size as? CakeSize.PanSize)?.pan ?: DoughConstants.REFERENCE_PAN
    var byPan by rememberSaveable(kind, resetCount) { mutableStateOf(start.size is CakeSize.PanSize) }
    var round by rememberSaveable(kind, resetCount) { mutableStateOf(startPan.round) }
    var inch by rememberSaveable(kind, resetCount) { mutableStateOf(false) }
    var panA by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(startPan.a)) }
    var panB by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(startPan.b)) }
    var flour by rememberSaveable(kind, resetCount) {
        mutableStateOf(fmt((start.size as? CakeSize.FlourGrams)?.grams ?: CakeLimits.referenceFlour(kind), 0))
    }
    val fields = CakeLimits.fields(kind)
    val defaults = CakeLimits.defaults(kind)
    var fat by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(start.fat, 2)) }
    var sugar by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(start.sugar, 2)) }
    var egg by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(start.egg, 2)) }
    var milk by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(start.milk, 2)) }
    var quark by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(start.quark, 2)) }
    var yeast by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(start.yeast, 2)) }
    var baking by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(start.bakingPowder, 2)) }
    var salt by rememberSaveable(kind, resetCount) { mutableStateOf(fmt(start.salt, 2)) }
    var yeastKindKey by rememberSaveable(kind, resetCount) { mutableStateOf(start.yeastKind.key) }
    val yeastKind = YeastKind.fromKey(yeastKindKey) ?: YeastKind.FRESH

    fun cakeStepping(field: CakeField, default: Double) =
        Stepping(DoughStepper.stepFor(field.toDoughField()), default, CakeLimits.range(kind, field), field.toDoughField())
    val size = if (byPan) {
        fun cm(text: String) = PanScaling.toCm(parseNumber(text), inch)
        CakeSize.PanSize(if (round) Pan.round(cm(panA)) else Pan.rectangle(cm(panA), cm(panB)))
    } else {
        CakeSize.FlourGrams(parseNumber(flour))
    }
    val input = CakeInput(
        kind = kind, size = size,
        fat = parseNumber(fat), sugar = parseNumber(sugar), egg = parseNumber(egg), milk = parseNumber(milk),
        quark = parseNumber(quark), yeastKind = yeastKind, yeast = parseNumber(yeast),
        bakingPowder = parseNumber(baking), salt = parseNumber(salt),
    )
    val result = CakeCalculator.calculate(input)
    val issues = (result as? DoughResult.Invalid)?.issues.orEmpty()
    LaunchedEffect(input, result is DoughResult.Success) {
        if (result is DoughResult.Success) {
            delay(MEMORY_DELAY_MILLIS)
            viewModel.rememberCake(input)
        }
    }

    val success = result as? DoughResult.Success
    val resultTotals = success?.let { viewModel.totals(it, yeastKind) }
    val draft = success?.let { viewModel.draftCake(it, input) }
    val unit = if (inch) "″" else "cm"
    val panIssue = issues.firstOrNull { it === DoughIssue.PanInvalid }
    val sizeIssue = issues.forField(DoughField.FLOUR_GRAMS)
    fun selectUnit(toInch: Boolean) {
        if (toInch == inch) return
        val factor = if (toInch) 1 / PanScaling.CM_PER_INCH else PanScaling.CM_PER_INCH
        panA = parseNumber(panA).let { if (it.isFinite()) fmt(it * factor) else panA }
        panB = parseNumber(panB).let { if (it.isFinite()) fmt(it * factor) else panB }
        inch = toInch
    }
    fun selectYeastKind(option: YeastKind) {
        // Die Menge bleibt gleichwertig, wenn die Hefeart wechselt.
        val current = parseNumber(yeast)
        if (current.isFinite()) yeast = fmt(yeastKind.convert(current, option), 3)
        yeastKindKey = option.key
    }
    val panStepping = Stepping(DoughStepper.stepFor(DoughField.PAN_SIZE))
    val diameterField: @Composable (Modifier) -> Unit = { m ->
        NumberField(stringResource(R.string.dough_pan_diameter), panA, { panA = it }, panIssue ?: sizeIssue, m, suffix = unit, stepping = panStepping)
    }
    val lengthField: @Composable (Modifier) -> Unit = { m ->
        NumberField(stringResource(R.string.dough_pan_length), panA, { panA = it }, panIssue ?: sizeIssue, m, suffix = unit, stepping = panStepping)
    }
    val widthField: @Composable (Modifier) -> Unit = { m ->
        NumberField(stringResource(R.string.dough_pan_width), panB, { panB = it }, panIssue, m, suffix = unit, stepping = panStepping)
    }
    val flourField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_label_flour), flour, { flour = it },
            issues.forField(DoughField.FLOUR_GRAMS), m, suffix = "g",
            stepping = Stepping(DoughStepper.stepFor(DoughField.FLOUR_GRAMS), CakeLimits.referenceFlour(kind), null, DoughField.FLOUR_GRAMS),
        )
    }
    val yeastAmountField: @Composable (Modifier) -> Unit = { m ->
        NumberField(
            stringResource(R.string.dough_yeast_amount), yeast, { yeast = it },
            issues.forField(DoughField.YEAST), m, suffix = SUFFIX_PERCENT,
            stepping = cakeStepping(CakeField.YEAST, YeastKind.FRESH.convert(defaults.yeast, yeastKind)),
        )
    }
    val ingredientFields = fields.filter { it != CakeField.YEAST }.map { field ->
        val content: @Composable (Modifier) -> Unit = { m ->
            val (value, change) = when (field) {
                CakeField.FAT -> fat to { v: String -> fat = v }
                CakeField.SUGAR -> sugar to { v: String -> sugar = v }
                CakeField.EGG -> egg to { v: String -> egg = v }
                CakeField.MILK -> milk to { v: String -> milk = v }
                CakeField.QUARK -> quark to { v: String -> quark = v }
                CakeField.BAKING_POWDER -> baking to { v: String -> baking = v }
                CakeField.SALT -> salt to { v: String -> salt = v }
                CakeField.YEAST -> yeast to { v: String -> yeast = v }
            }
            NumberField(
                stringResource(cakeFieldLabel(kind, field)), value, change,
                issues.forField(field.toDoughField()), m, suffix = SUFFIX_PERCENT,
                stepping = cakeStepping(field, CakeLimits.value(defaults, field)),
            )
        }
        content
    }
    val eggHints: @Composable () -> Unit = {
        if (success != null && CakeField.EGG in fields) {
            EggHints(success.sections.flatMap { it.lines }.filter { it.role == LineRole.EGG }.sumOf { it.grams }, 0.0)
        }
    }

    FormFrame(
        header = header,
        summary = if (success != null && draft != null) summaryLine(success, draft) else null,
        totals = resultTotals,
        onSave = { viewModel.saveCake(input, onSaved) },
        onReset = { resetCount += 1 },
        extra = resultTotals,
        note = R.string.dough_cake_baking_note,
        compact = {
            // Alles in einem Bild (Entwurf vom 30.10.2026): Größe (runde, eckige Backform oder Mehl) als ein Auswahlfeld.
            val kindChoice: @Composable (Modifier) -> Unit = { m ->
                CompactChoice(
                    label = stringResource(R.string.dough_cake_kind_label),
                    options = CakeKind.entries,
                    selected = kind,
                    optionLabel = { stringResource(AndroidDoughTexts.cakeKindName(it)) },
                    onSelect = onKind,
                    modifier = m,
                )
            }
            val sizeChoice: @Composable (Modifier) -> Unit = { m ->
                CompactChoice(
                    label = stringResource(R.string.dough_section_size),
                    options = listOf(CAKE_SIZE_ROUND, CAKE_SIZE_RECT, CAKE_SIZE_FLOUR),
                    selected = if (!byPan) CAKE_SIZE_FLOUR else if (round) CAKE_SIZE_ROUND else CAKE_SIZE_RECT,
                    optionLabel = {
                        stringResource(
                            when (it) {
                                CAKE_SIZE_ROUND -> R.string.dough_size_pan_round
                                CAKE_SIZE_RECT -> R.string.dough_size_pan_rect
                                else -> R.string.dough_size_flour
                            },
                        )
                    },
                    onSelect = {
                        byPan = it != CAKE_SIZE_FLOUR
                        if (byPan) round = it == CAKE_SIZE_ROUND
                    },
                    modifier = m,
                )
            }
            val unitChoice: @Composable (Modifier) -> Unit = { m ->
                CompactChoice(
                    label = stringResource(R.string.dough_label_unit),
                    options = listOf(false, true),
                    selected = inch,
                    optionLabel = { stringResource(if (it) R.string.dough_unit_inch else R.string.dough_unit_cm) },
                    onSelect = ::selectUnit,
                    modifier = m,
                )
            }
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
            val compactFields = buildList<@Composable (Modifier) -> Unit> {
                add(kindChoice)
                add(sizeChoice)
                if (!byPan) {
                    add(flourField)
                } else if (round) {
                    add(diameterField)
                    add(unitChoice)
                } else {
                    add(lengthField)
                    add(widthField)
                    add(unitChoice)
                }
                if (CakeField.YEAST in fields) {
                    add(yeastKindChoice)
                    add(yeastAmountField)
                }
                addAll(ingredientFields)
            }
            Column(modifier = Modifier.padding(horizontal = Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Fields(*compactFields.toTypedArray())
                if (byPan && success != null) {
                    Text(
                        stringResource(R.string.dough_cake_flour_from_pan, fmt(success.flourGrams, 0)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                eggHints()
            }
        },
    ) {
        Card(stringResource(R.string.dough_section_dough)) {
            ChoiceField(
                label = stringResource(R.string.dough_cake_kind_label),
                options = CakeKind.entries,
                selected = kind,
                optionLabel = { stringResource(AndroidDoughTexts.cakeKindName(it)) },
                onSelect = onKind,
            )
        }

        Card(stringResource(R.string.dough_section_size)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                FilterChip(selected = byPan, onClick = { byPan = true }, label = { Text(stringResource(R.string.dough_size_pan)) })
                FilterChip(selected = !byPan, onClick = { byPan = false }, label = { Text(stringResource(R.string.dough_size_flour)) })
            }
            if (byPan) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    FilterChip(selected = round, onClick = { round = true }, label = { Text(stringResource(R.string.adjust_pan_round)) })
                    FilterChip(selected = !round, onClick = { round = false }, label = { Text(stringResource(R.string.adjust_pan_rectangle)) })
                    FilterChip(selected = !inch, onClick = { selectUnit(false) }, label = { Text(stringResource(R.string.dough_unit_cm)) })
                    FilterChip(selected = inch, onClick = { selectUnit(true) }, label = { Text(stringResource(R.string.dough_unit_inch)) })
                }
                if (round) {
                    diameterField(Modifier.fillMaxWidth())
                } else {
                    Fields(lengthField, widthField)
                }
                success?.let {
                    Text(
                        stringResource(R.string.dough_cake_flour_from_pan, fmt(it.flourGrams, 0)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                flourField(Modifier.fillMaxWidth())
            }
        }

        if (CakeField.YEAST in fields) {
            Card(stringResource(R.string.dough_section_yeast)) {
                ChoiceField(
                    label = stringResource(R.string.dough_section_yeast),
                    options = YeastKind.entries,
                    selected = yeastKind,
                    optionLabel = { stringResource(AndroidDoughTexts.yeastName(it)) },
                    onSelect = ::selectYeastKind,
                )
                yeastAmountField(Modifier.fillMaxWidth())
            }
        }

        Card(stringResource(R.string.dough_section_ingredients)) {
            Text(
                stringResource(R.string.dough_percent_note_all),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ingredientFields.chunked(2).forEach { row -> Fields(*row.toTypedArray()) }
            eggHints()
        }
    }
}

private const val CAKE_SIZE_ROUND = 0
private const val CAKE_SIZE_RECT = 1
private const val CAKE_SIZE_FLOUR = 2
