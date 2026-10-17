package com.moltobene.app.ui.dough

import android.content.Context
import com.moltobene.app.R
import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.dough.DurationUnit

/** Dauer und Ort einer Phase des Zeitplans (#71) als Text aus strings.xml; die Einheit wählt [DurationUnit]. */
object AndroidDurationText {

    /** „1 bis 3 Tage“, bei gleicher Dauer „2 Stunden“. */
    fun format(context: Context, minMinutes: Int, maxMinutes: Int): String {
        val unit = DurationUnit.of(minMinutes, maxMinutes)
        val min = unit.value(minMinutes)
        val max = unit.value(maxMinutes)
        val res = context.resources
        return if (min == max) {
            val id = when (unit) {
                DurationUnit.MINUTES -> R.plurals.duration_minutes
                DurationUnit.HOURS -> R.plurals.duration_hours
                DurationUnit.DAYS -> R.plurals.duration_days
            }
            res.getQuantityString(id, max, max)
        } else {
            val id = when (unit) {
                DurationUnit.MINUTES -> R.plurals.duration_range_minutes
                DurationUnit.HOURS -> R.plurals.duration_range_hours
                DurationUnit.DAYS -> R.plurals.duration_range_days
            }
            res.getQuantityString(id, max, min, max)
        }
    }

    fun place(context: Context, place: PhasePlace): String = when (place) {
        PhasePlace.NONE -> ""
        PhasePlace.ROOM -> context.getString(R.string.place_room)
        PhasePlace.FRIDGE -> context.getString(R.string.place_fridge)
        PhasePlace.OVEN -> context.getString(R.string.place_oven)
    }
}
