package com.moltobene.app.ui.components

import android.content.res.Resources
import androidx.annotation.StringRes
import com.moltobene.app.R
import com.moltobene.app.data.PredefinedTags
import com.moltobene.app.data.Tag

/** Angezeigte Namen der mitgelieferten Schlagwörter (#57) aus strings.xml; eigene zeigen ihren Namen. */
object TagLabels {

    @StringRes
    fun resId(key: String): Int? = when (key) {
        "main_course" -> R.string.tag_main_course
        "side_dish" -> R.string.tag_side_dish
        "appetizer" -> R.string.tag_appetizer
        "soup" -> R.string.tag_soup
        "salad" -> R.string.tag_salad
        "sauce" -> R.string.tag_sauce
        "pasta" -> R.string.tag_pasta
        "casserole" -> R.string.tag_casserole
        "dessert" -> R.string.tag_dessert
        "bread" -> R.string.tag_bread
        "cake" -> R.string.tag_cake
        "pizza" -> R.string.tag_pizza
        "basics" -> R.string.tag_basics
        else -> null
    }

    fun text(resources: Resources, tag: Tag): String =
        tag.predefinedKey?.let(::resId)?.let(resources::getString) ?: tag.name

    /** Kennung → Text für alle mitgelieferten Schlagwörter, in der Sprache der Oberfläche. */
    fun all(resources: Resources): Map<String, String> =
        PredefinedTags.KEYS.mapNotNull { key -> resId(key)?.let { key to resources.getString(it) } }.toMap()
}
