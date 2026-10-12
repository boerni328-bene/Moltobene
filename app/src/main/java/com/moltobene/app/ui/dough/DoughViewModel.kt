package com.moltobene.app.ui.dough

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.R
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.RecipeRepository
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
import com.moltobene.app.data.dough.DoughNumbers
import com.moltobene.app.data.dough.DoughResult
import com.moltobene.app.data.dough.DoughTexts
import com.moltobene.app.data.dough.DoughType
import com.moltobene.app.data.dough.LeavenedInput
import com.moltobene.app.data.dough.LineRole
import com.moltobene.app.data.dough.Preferment
import com.moltobene.app.data.dough.SectionKind
import com.moltobene.app.data.dough.YeastKind
import com.moltobene.app.data.dough.YieldKind
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/** Eine Gesamtmenge mit Namen, z. B. „594 g“ und „Mehl“ (#83). */
data class TotalItem(val amount: String, val name: String)

/** Texte des Entwurfs aus den Ressourcen der Oberfläche (#69). */
class AndroidDoughTexts(private val context: Context) : DoughTexts {

    private fun prefermentName(preferment: Preferment) = context.getString(
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

    override fun heading(kind: SectionKind, preferment: Preferment): String = when (kind) {
        SectionKind.PREFERMENT -> context.getString(R.string.dough_heading_preferment, prefermentName(preferment))
        else -> context.getString(R.string.dough_heading_main)
    }

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

    /** Regler statt oder neben den Zahlen (#83, Etappe 2); gilt für alle Teigarten, bleibt auf dem Handy gemerkt. */
    var sliders by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { sliders = runCatching { preferences.doughMemory(SLIDERS_KEY) == "1" }.getOrDefault(false) }
    }

    fun setSliders(on: Boolean) {
        sliders = on
        viewModelScope.launch { runCatching { preferences.setDoughMemory(SLIDERS_KEY, if (on) "1" else "0") } }
    }

    suspend fun load(type: DoughType): LeavenedInput = preferences.doughValues(type)

    fun remember(input: LeavenedInput) {
        viewModelScope.launch { preferences.setDoughValues(input) }
    }

    /** Der Entwurf zum Ergebnis, für die Anzeige und zum Speichern. */
    fun draft(result: DoughResult.Success, input: LeavenedInput): DoughDraft =
        DoughDraft.from(result, input, texts, if (Locale.getDefault().language == "de") "de" else "en")

    /** Summen über Vorteig und Hauptteig, z. B. „594 g“ – „Mehl“; für die Leiste unten und die Karte bei großer Schrift. */
    fun totals(result: DoughResult.Success, input: LeavenedInput): List<TotalItem> = totalItems(result, input.yeastKind)

    private fun totalItems(result: DoughResult.Success, yeastKind: YeastKind): List<TotalItem> {
        val lines = result.sections.flatMap { it.lines }
        return TOTALS_ORDER.mapNotNull { role ->
            val grams = lines.filter { it.role == role }.sumOf { it.grams }
            if (grams <= 0) null else TotalItem("${DoughNumbers.grams(grams, role, language)} g", texts.ingredient(role, yeastKind))
        }
    }

    private val language get() = if (Locale.getDefault().language == "de") "de" else "en"

    suspend fun loadPasta(kind: PastaKind): PastaInput = PastaMemory.decode(kind, preferences.doughMemory(PastaMemory.key(kind)))

    fun rememberPasta(input: PastaInput) {
        viewModelScope.launch { preferences.setDoughMemory(PastaMemory.key(input.kind), PastaMemory.encode(input)) }
    }

    fun draftPasta(result: DoughResult.Success, input: PastaInput): DoughDraft = DoughDraft.fromPasta(result, input, texts, language)

    fun savePasta(input: PastaInput, onSaved: (String) -> Unit) {
        val result = PastaCalculator.calculate(input) as? DoughResult.Success ?: return
        save(draftPasta(result, input), onSaved)
    }

    suspend fun loadCake(kind: CakeKind): CakeInput = CakeMemory.decode(kind, preferences.doughMemory(CakeMemory.key(kind)))

    fun rememberCake(input: CakeInput) {
        viewModelScope.launch { preferences.setDoughMemory(CakeMemory.key(input.kind), CakeMemory.encode(input)) }
    }

    fun draftCake(result: DoughResult.Success, input: CakeInput): DoughDraft = DoughDraft.fromCake(result, input, texts, language)

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

    /** Speichert das Ergebnis als Entwurf; [onSaved] bekommt die Nummer des Rezepts. */
    fun saveAsRecipe(input: LeavenedInput, onSaved: (String) -> Unit) {
        val result = DoughCalculator.calculate(input) as? DoughResult.Success ?: return
        val recipe = draft(result, input).toRecipe(UUID.randomUUID().toString(), System.currentTimeMillis())
        viewModelScope.launch {
            repository.save(recipe)
            onSaved(recipe.id)
        }
    }
}

private const val SLIDERS_KEY = "sliders"

/** Reihenfolge der Summen: Mehl zuerst, dann Flüssiges, Würze, Treibmittel. */
private val TOTALS_ORDER = listOf(
    LineRole.SEMOLINA, LineRole.FLOUR, LineRole.BUTTER, LineRole.SUGAR, LineRole.EGG, LineRole.YOLK, LineRole.QUARK,
    LineRole.MILK, LineRole.LIQUID, LineRole.WATER, LineRole.SALT, LineRole.OIL, LineRole.YEAST, LineRole.BAKING_POWDER,
)
