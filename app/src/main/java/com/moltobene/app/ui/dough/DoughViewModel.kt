package com.moltobene.app.ui.dough

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.R
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.TemperatureUnit
import com.moltobene.app.data.WeightUnit
import com.moltobene.app.data.dough.CakeCalculator
import com.moltobene.app.data.dough.CakeInput
import com.moltobene.app.data.dough.CakeKind
import com.moltobene.app.data.dough.CakeMemory
import com.moltobene.app.data.dough.PastaCalculator
import com.moltobene.app.data.dough.PastaInput
import com.moltobene.app.data.dough.PastaKind
import com.moltobene.app.data.dough.PastaMemory
import com.moltobene.app.data.dough.DoughCalculator
import com.moltobene.app.data.dough.DoughDraft
import com.moltobene.app.data.dough.DoughKitchen
import com.moltobene.app.data.dough.DoughNumbers
import com.moltobene.app.data.dough.DoughPresets
import com.moltobene.app.data.dough.DoughProcess
import com.moltobene.app.data.dough.DoughProcessMemory
import com.moltobene.app.data.dough.DoughProcessRules
import com.moltobene.app.data.dough.DoughResult
import com.moltobene.app.data.dough.DoughSummary
import com.moltobene.app.data.dough.DoughTexts
import com.moltobene.app.data.dough.PhaseKind
import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.dough.DoughType
import com.moltobene.app.data.dough.LeavenedInput
import com.moltobene.app.data.dough.LineRole
import com.moltobene.app.data.dough.Preferment
import com.moltobene.app.data.dough.RestPlace
import com.moltobene.app.data.dough.SectionKind
import com.moltobene.app.data.dough.StepDetail
import com.moltobene.app.data.dough.YeastKind
import com.moltobene.app.data.dough.YieldKind
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/** Eine Gesamtmenge mit Namen, z. B. „594 g“ und „Mehl“ (#83); [role] kennzeichnet z. B. das Wasser für seine Temperatur. */
data class TotalItem(val amount: String, val name: String, val role: LineRole? = null)

/** Texte des Entwurfs aus den Ressourcen der Oberfläche (#69). */
class AndroidDoughTexts(private val context: Context) : DoughTexts {

    override fun prefermentName(preferment: Preferment): String = context.getString(
        when (preferment) {
            Preferment.BIGA -> R.string.dough_pre_biga
            Preferment.POOLISH -> R.string.dough_pre_poolish
            Preferment.SOURDOUGH -> R.string.dough_pre_sourdough
            Preferment.NONE -> R.string.dough_pre_none
        },
    )

    override fun title(type: DoughType, preferment: Preferment): String {
        val bread = type == DoughType.BREAD
        return if (preferment == Preferment.NONE) {
            context.getString(if (bread) R.string.dough_recipe_title_bread else R.string.dough_recipe_title_pizza)
        } else {
            context.getString(
                if (bread) R.string.dough_recipe_title_bread_with else R.string.dough_recipe_title_pizza_with,
                prefermentName(preferment),
            )
        }
    }

    override fun heading(kind: SectionKind, preferment: Preferment, amount: String): String = when (kind) {
        SectionKind.PREFERMENT -> context.getString(R.string.dough_heading_preferment, amount, prefermentName(preferment))
        else -> context.getString(R.string.dough_heading_main, amount)
    }

    override fun wholePreferment(preferment: Preferment, amount: String): String =
        context.getString(R.string.dough_whole_preferment, amount, prefermentName(preferment))

    override fun ingredient(role: LineRole, yeastKind: YeastKind): String = context.getString(
        when (role) {
            LineRole.FLOUR -> R.string.dough_ingredient_flour
            LineRole.WATER -> R.string.dough_ingredient_water
            LineRole.SALT -> R.string.dough_ingredient_salt
            LineRole.OIL -> R.string.dough_ingredient_oil
            LineRole.YEAST -> yeastName(yeastKind)
            LineRole.SEMOLINA -> R.string.dough_ingredient_semolina
            LineRole.EGG -> R.string.dough_ingredient_egg
            LineRole.YOLK -> R.string.dough_ingredient_yolk
            LineRole.MILK -> R.string.dough_ingredient_milk
            LineRole.LIQUID -> R.string.dough_ingredient_liquid
            LineRole.SUGAR -> R.string.dough_ingredient_sugar
            LineRole.BUTTER -> R.string.dough_ingredient_butter
            LineRole.QUARK -> R.string.dough_ingredient_quark
            LineRole.BAKING_POWDER -> R.string.dough_ingredient_baking_powder
            LineRole.MALT -> R.string.dough_ingredient_malt
            LineRole.YOGURT -> R.string.dough_ingredient_yogurt
            LineRole.WHEAT -> R.string.dough_ingredient_wheat
            LineRole.RYE -> R.string.dough_ingredient_rye
            LineRole.WHOLE -> R.string.dough_ingredient_whole
            LineRole.SPELT -> R.string.dough_ingredient_spelt
            LineRole.HONEY -> R.string.dough_ingredient_honey
            LineRole.SEEDS -> R.string.dough_ingredient_seeds
        },
    )

    override fun pastaTitle(kind: PastaKind): String = context.getString(
        if (kind == PastaKind.EGG) R.string.dough_pasta_egg else R.string.dough_pasta_water,
    )

    override fun cakeTitle(kind: CakeKind): String = context.getString(cakeKindName(kind))

    override fun servingsUnit(kind: YieldKind, detail: String): String = when (kind) {
        YieldKind.BALL -> context.getString(R.string.dough_unit_balls, detail)
        YieldKind.LOAF -> context.getString(R.string.dough_unit_loaves, detail)
        YieldKind.PORTION -> context.getString(R.string.dough_unit_portions, detail)
        YieldKind.PAN -> context.getString(R.string.dough_unit_pan, detail)
        YieldKind.DOUGH -> context.getString(R.string.dough_unit_dough)
    }

    override fun phase(kind: PhaseKind, type: DoughType, preferment: Preferment): String = when (kind) {
        PhaseKind.PREFERMENT_MIX -> context.getString(R.string.phase_preferment_mix, prefermentName(preferment))
        PhaseKind.PREFERMENT_RIPEN -> context.getString(R.string.phase_preferment_ripen, prefermentName(preferment))
        PhaseKind.KNEAD -> context.getString(R.string.phase_knead)
        PhaseKind.BULK -> context.getString(R.string.phase_bulk)
        PhaseKind.SHAPE -> context.getString(R.string.phase_shape)
        PhaseKind.PROOF -> context.getString(if (type == DoughType.PIZZA) R.string.phase_proof_balls else R.string.phase_proof_loaf)
        PhaseKind.WARM_UP -> context.getString(R.string.phase_warm_up)
        PhaseKind.BAKE -> context.getString(R.string.phase_bake)
    }

    override fun duration(minMinutes: Int, maxMinutes: Int): String = AndroidDurationText.format(context, minMinutes, maxMinutes)

    override fun placeAt(place: PhasePlace, temperature: String): String = when (place) {
        PhasePlace.NONE -> ""
        PhasePlace.ROOM -> context.getString(R.string.place_room_at, temperature)
        PhasePlace.FRIDGE -> context.getString(R.string.place_fridge_at, temperature)
        PhasePlace.OVEN -> context.getString(R.string.place_oven_at, temperature)
    }

    /** „a und b“, „a, b und c“ (englisch mit Komma vor „and“), aus ganzen Texten in strings.xml. */
    override fun join(parts: List<String>): String = when (parts.size) {
        0 -> ""
        1 -> parts[0]
        2 -> context.getString(R.string.list_pair, parts[0], parts[1])
        else -> context.getString(R.string.list_last, parts.dropLast(1).joinToString(", "), parts.last())
    }

    override fun stepDetail(detail: StepDetail): String = when (detail) {
        is StepDetail.Mix -> context.getString(
            when (detail.preferment) {
                Preferment.BIGA -> R.string.step_mix_biga
                Preferment.SOURDOUGH -> R.string.step_mix_sourdough
                else -> R.string.step_mix_poolish
            },
            detail.ingredients,
        )
        is StepDetail.Ripen -> context.getString(
            when (detail.preferment) {
                Preferment.BIGA -> R.string.step_ripen_biga
                Preferment.SOURDOUGH -> R.string.step_ripen_sourdough
                else -> R.string.step_ripen_poolish
            },
        )
        is StepDetail.Knead -> kneadText(detail)
        StepDetail.Bulk -> context.getString(R.string.step_bulk)
        is StepDetail.Shape -> shapeText(detail)
        is StepDetail.Proof -> when {
            detail.type == DoughType.PIZZA && detail.place == RestPlace.FRIDGE -> context.getString(R.string.step_proof_balls_fridge)
            detail.type == DoughType.PIZZA -> context.getString(R.string.step_proof_balls_room, detail.oven)
            detail.place == RestPlace.FRIDGE -> context.getString(R.string.step_proof_loaf_fridge, detail.oven)
            else -> context.getString(R.string.step_proof_loaf_room, detail.oven)
        }
        is StepDetail.Warm -> context.getString(R.string.step_warm, detail.oven)
        is StepDetail.Bake -> context.getString(if (detail.type == DoughType.PIZZA) R.string.step_bake_pizza else R.string.step_bake_bread)
    }

    /** Kneten aus ganzen Sätzen: womit geknetet wird, wann das Salz dazukommt, woran man das Ende erkennt. */
    private fun kneadText(detail: StepDetail.Knead): String {
        val start = when {
            detail.preferment == null -> context.getString(R.string.step_knead_plain, detail.ingredients)
            detail.ingredients.isBlank() -> context.getString(R.string.step_knead_plain, detail.preferment)
            detail.withPreferment == Preferment.BIGA -> context.getString(R.string.step_knead_biga, detail.preferment, detail.ingredients)
            else -> context.getString(R.string.step_knead_preferment, detail.preferment, detail.ingredients)
        }
        val salt = detail.salt?.let { context.getString(R.string.step_knead_salt, it) }
        val end = context.getString(R.string.step_knead_end, detail.doughTemp)
        return listOfNotNull(start, salt, end).joinToString(" ")
    }

    /** Teilen und formen; ein einzelnes Stück wird nur geformt. */
    private fun shapeText(detail: StepDetail.Shape): String {
        val res = context.resources
        return when {
            detail.count <= 1 && detail.type == DoughType.PIZZA -> context.getString(R.string.step_shape_ball)
            detail.count <= 1 -> context.getString(R.string.step_shape_loaf)
            detail.type == DoughType.PIZZA -> res.getQuantityString(R.plurals.step_shape_balls, detail.count, detail.count, detail.piece)
            else -> res.getQuantityString(R.plurals.step_shape_loaves, detail.count, detail.count, detail.piece)
        }
    }

    companion object {
        fun cakeKindName(kind: CakeKind): Int = when (kind) {
            CakeKind.BATTER -> R.string.dough_cake_batter
            CakeKind.SPONGE -> R.string.dough_cake_sponge
            CakeKind.SHORTCRUST -> R.string.dough_cake_shortcrust
            CakeKind.YEAST_SWEET -> R.string.dough_cake_yeast
            CakeKind.CHOUX -> R.string.dough_cake_choux
            CakeKind.QUARK_OIL -> R.string.dough_cake_quark_oil
        }

        fun yeastName(kind: YeastKind): Int = when (kind) {
            YeastKind.FRESH -> R.string.dough_yeast_fresh
            YeastKind.INSTANT_DRY -> R.string.dough_yeast_instant
            YeastKind.ACTIVE_DRY -> R.string.dough_yeast_active
        }
    }
}

/** Teigrechner (#69): lädt und merkt die letzten Werte und speichert das Ergebnis als Entwurf. */
class DoughViewModel(
    context: Context,
    private val preferences: AppPreferences,
    private val repository: RecipeRepository,
) : ViewModel() {

    private val texts = AndroidDoughTexts(context.applicationContext)

    /** „Gewichte“ aus den Einstellungen (Gramm oder Unzen); folgt ihrer Änderung, auch bei offenem Teigrechner. */
    var weightUnit by mutableStateOf(preferences.weightUnit.value ?: WeightUnit.GRAMS)
        private set

    /** „Temperaturen“ aus den Einstellungen (°C oder °F, Wunsch vom 05.11.2026); folgt ihrer Änderung. */
    var temperatureUnit by mutableStateOf(preferences.temperatureUnit.value ?: TemperatureUnit.CELSIUS)
        private set

    /**
     * Raum, Kühlschrank und Knetart (Entwurf 10C): gelten für alle Teigarten und werden sofort gemerkt; geladen beim
     * ersten [load].
     */
    var kitchen by mutableStateOf(DoughKitchen())
        private set
    private var kitchenLoaded = false

    /** Eigene Voreinstellungen je Teigart (#83, Etappe 3); nur auf dem Handy, nicht in der Sicherung. */
    private val ownPresets = mutableStateMapOf<String, List<DoughPresets.Own>>()

    init {
        viewModelScope.launch { preferences.weightUnit.collect { unit -> if (unit != null) weightUnit = unit } }
        viewModelScope.launch { preferences.temperatureUnit.collect { unit -> if (unit != null) temperatureUnit = unit } }
    }

    /** Die gemerkten Werte einer Teigart: Eingaben sowie Gehzeiten und Temperaturen. */
    data class Loaded(val input: LeavenedInput, val process: DoughProcess)

    fun own(type: DoughType): List<DoughPresets.Own> = ownPresets[type.key].orEmpty()

    /** Lädt die gemerkten Werte, Gehzeiten und Temperaturen und die eigenen Voreinstellungen der Teigart. */
    suspend fun load(type: DoughType): Loaded {
        ownPresets[type.key] = runCatching { DoughPresets.decode(preferences.doughMemory(DoughPresets.key(type))) }
            .getOrDefault(emptyList())
        if (!kitchenLoaded) {
            kitchen = runCatching { DoughProcessMemory.decodeKitchen(preferences.doughMemory(DoughProcessMemory.KITCHEN_KEY)) }
                .getOrDefault(DoughKitchen())
            kitchenLoaded = true
        }
        val input = preferences.doughValues(type)
        val process = runCatching {
            DoughProcessMemory.decode(type, input.preferment, preferences.doughMemory(DoughProcessMemory.processKey(type)))
        }.getOrDefault(DoughProcessRules.defaults(type, input.preferment))
        return Loaded(input, process)
    }

    /** Ändert Raum, Kühlschrank oder Knetart und merkt sie sofort (gelten für alle Teigarten). */
    fun updateKitchen(value: DoughKitchen) {
        if (value == kitchen) return
        kitchen = value
        viewModelScope.launch {
            runCatching { preferences.setDoughMemory(DoughProcessMemory.KITCHEN_KEY, DoughProcessMemory.encodeKitchen(value)) }
        }
    }

    /** Merkt Gehzeiten und Temperaturen der Teigart. */
    fun rememberProcess(type: DoughType, process: DoughProcess) {
        viewModelScope.launch {
            runCatching { preferences.setDoughMemory(DoughProcessMemory.processKey(type), DoughProcessMemory.encode(process)) }
        }
    }

    fun saveOwnPreset(type: DoughType, name: String, input: LeavenedInput): DoughPresets.SaveResult {
        val (list, result) = DoughPresets.save(own(type), name, input)
        if (result == DoughPresets.SaveResult.SAVED) persistOwn(type, list)
        return result
    }

    fun deleteOwnPreset(type: DoughType, name: String) = persistOwn(type, DoughPresets.delete(own(type), name))

    private fun persistOwn(type: DoughType, list: List<DoughPresets.Own>) {
        ownPresets[type.key] = list
        viewModelScope.launch { runCatching { preferences.setDoughMemory(DoughPresets.key(type), DoughPresets.encode(list)) } }
    }

    fun remember(input: LeavenedInput) {
        viewModelScope.launch { preferences.setDoughValues(input) }
    }

    /** Der Entwurf zum Ergebnis mit Zeitplan und Zubereitung, für die Anzeige und zum Speichern. */
    fun draft(result: DoughResult.Success, input: LeavenedInput, process: DoughProcess): DoughDraft =
        DoughDraft.from(result, input, texts, language, weightUnit, process, kitchen, temperatureUnit)

    /** Summen über Vorteig und Hauptteig, z. B. „594 g“ – „Mehl“; für die Leiste unten und die Karte bei großer Schrift. */
    fun totals(result: DoughResult.Success, input: LeavenedInput): List<TotalItem> = totalItems(result, input.yeastKind)

    /** Mehl, Wasser und Hefe des Vorteigs (Biga, Poolish, Sauerteig) für die Leiste unten; leer ohne Vorteig. */
    fun prefermentItems(result: DoughResult.Success, input: LeavenedInput): List<TotalItem> {
        val lines = result.sections.firstOrNull { it.kind == SectionKind.PREFERMENT }?.lines.orEmpty()
        return lines.filter { it.grams > 0 }.map {
            TotalItem(DoughNumbers.weight(it.grams, it.role, language, weightUnit), texts.ingredient(it.role, input.yeastKind))
        }
    }

    /**
     * Was zum Kneten zum Vorteig in den Hauptteig kommt (Wunsch vom 04.11.2026): Hefe zuerst, falls sie nicht im Vorteig
     * ist, dann Mehl, Wasser, Salz und der Rest; leer ohne Vorteig.
     */
    fun mainItems(result: DoughResult.Success, input: LeavenedInput): List<TotalItem> =
        DoughSummary.mainLines(result).map {
            TotalItem(DoughNumbers.weight(it.grams, it.role, language, weightUnit), texts.ingredient(it.role, input.yeastKind), it.role)
        }

    private fun totalItems(result: DoughResult.Success, yeastKind: YeastKind): List<TotalItem> {
        val lines = result.sections.flatMap { it.lines }
        return TOTALS_ORDER.mapNotNull { role ->
            val grams = lines.filter { it.role == role }.sumOf { it.grams }
            if (grams <= 0) null else TotalItem(DoughNumbers.weight(grams, role, language, weightUnit), texts.ingredient(role, yeastKind))
        }
    }

    private val language get() = if (Locale.getDefault().language == "de") "de" else "en"

    suspend fun loadPasta(kind: PastaKind): PastaInput = PastaMemory.decode(kind, preferences.doughMemory(PastaMemory.key(kind)))

    fun rememberPasta(input: PastaInput) {
        viewModelScope.launch { preferences.setDoughMemory(PastaMemory.key(input.kind), PastaMemory.encode(input)) }
    }

    fun draftPasta(result: DoughResult.Success, input: PastaInput): DoughDraft = DoughDraft.fromPasta(result, input, texts, language, weightUnit)

    fun savePasta(input: PastaInput, onSaved: (String) -> Unit) {
        val result = PastaCalculator.calculate(input) as? DoughResult.Success ?: return
        save(draftPasta(result, input), onSaved)
    }

    suspend fun loadCake(kind: CakeKind): CakeInput = CakeMemory.decode(kind, preferences.doughMemory(CakeMemory.key(kind)))

    fun rememberCake(input: CakeInput) {
        viewModelScope.launch { preferences.setDoughMemory(CakeMemory.key(input.kind), CakeMemory.encode(input)) }
    }

    fun draftCake(result: DoughResult.Success, input: CakeInput): DoughDraft = DoughDraft.fromCake(result, input, texts, language, weightUnit)

    fun saveCake(input: CakeInput, onSaved: (String) -> Unit) {
        val result = CakeCalculator.calculate(input) as? DoughResult.Success ?: return
        save(draftCake(result, input), onSaved)
    }

    /** Summen für Pasta und Kuchen, wie [totals]. */
    fun totals(result: DoughResult.Success, yeastKind: YeastKind): List<TotalItem> = totalItems(result, yeastKind)

    private fun save(draft: DoughDraft, onSaved: (String) -> Unit) {
        val recipe = draft.toRecipe(UUID.randomUUID().toString(), System.currentTimeMillis())
        viewModelScope.launch {
            repository.save(recipe)
            onSaved(recipe.id)
        }
    }

    /** Speichert das Ergebnis als Entwurf mit Zeitplan und Zubereitung; [onSaved] bekommt die Nummer des Rezepts. */
    fun saveAsRecipe(input: LeavenedInput, process: DoughProcess, onSaved: (String) -> Unit) {
        val result = DoughCalculator.calculate(input) as? DoughResult.Success ?: return
        save(draft(result, input, process), onSaved)
    }
}


/** Reihenfolge der Summen: Mehl zuerst, dann Flüssiges, Würze, Treibmittel. */
private val TOTALS_ORDER = listOf(
    LineRole.SEMOLINA, LineRole.FLOUR, LineRole.WHEAT, LineRole.RYE, LineRole.WHOLE, LineRole.SPELT, LineRole.BUTTER, LineRole.SUGAR, LineRole.HONEY, LineRole.EGG, LineRole.YOLK, LineRole.QUARK, LineRole.SEEDS,
    LineRole.MILK, LineRole.YOGURT, LineRole.LIQUID, LineRole.WATER, LineRole.SALT, LineRole.OIL, LineRole.YEAST, LineRole.MALT,
    LineRole.BAKING_POWDER,
)
