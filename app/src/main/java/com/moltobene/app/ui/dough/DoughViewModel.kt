package com.moltobene.app.ui.dough

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moltobene.app.R
import com.moltobene.app.data.AppPreferences
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.dough.DoughCalculator
import com.moltobene.app.data.dough.DoughDraft
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
        },
    )

    override fun servingsUnit(kind: YieldKind, pieceGrams: String): String = context.getString(
        if (kind == YieldKind.LOAF) R.string.dough_unit_loaves else R.string.dough_unit_balls,
        pieceGrams,
    )

    companion object {
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

    suspend fun load(type: DoughType): LeavenedInput = preferences.doughValues(type)

    fun remember(input: LeavenedInput) {
        viewModelScope.launch { preferences.setDoughValues(input) }
    }

    /** Der Entwurf zum Ergebnis, für die Anzeige und zum Speichern. */
    fun draft(result: DoughResult.Success, input: LeavenedInput): DoughDraft =
        DoughDraft.from(result, input, texts, if (Locale.getDefault().language == "de") "de" else "en")

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
