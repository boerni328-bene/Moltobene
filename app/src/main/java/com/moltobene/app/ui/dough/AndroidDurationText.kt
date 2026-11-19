package com.moltobene.app.ui.dough

import android.content.Context
import android.content.res.Resources
import com.moltobene.app.R
import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.dough.DurationUnit

/** Dauer und Ort einer Phase des Zeitplans (#71) als Text aus strings.xml; die Einheit wählt [DurationUnit]. */
object AndroidDurationText {

    /** „1 bis 3 Tage“, bei gleicher Dauer „2 Stunden“. */
    fun format(context: Context, minMinutes: Int, maxMinutes: Int): String = format(context.resources, minMinutes, maxMinutes)

    fun format(res: Resources, minMinutes: Int, maxMinutes: Int): String {
        val unit = DurationUnit.of(minMinutes, maxMinutes)
        val min = unit.value(minMinutes)
        val max = unit.value(maxMinutes)
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

    /** Gesamtdauer in Tagen, Stunden und Minuten, z. B. „1 Tag, 45 Minuten“ (Uhrzeiten, 0.77.0). */
    fun total(context: Context, minutes: Int): String {
        val res = context.resources
        val days = minutes / (24 * 60)
        val hours = minutes % (24 * 60) / 60
        val rest = minutes % 60
        val parts = buildList {
            if (days > 0) add(res.getQuantityString(R.plurals.duration_days, days, days))
            if (hours > 0) add(res.getQuantityString(R.plurals.duration_hours, hours, hours))
            if (rest > 0 || isEmpty()) add(res.getQuantityString(R.plurals.duration_minutes, rest, rest))
        }
        return parts.reduce { a, b -> res.getString(R.string.duration_list, a, b) }
    }

    fun place(context: Context, place: PhasePlace): String = when (place) {
        PhasePlace.NONE -> ""
        PhasePlace.ROOM -> context.getString(R.string.place_room)
        PhasePlace.FRIDGE -> context.getString(R.string.place_fridge)
        PhasePlace.OVEN -> context.getString(R.string.place_oven)
    }
}
