package com.moltobene.app.data.dough

/** Wahl der Einheit für eine Dauer im Zeitplan (#71): ganze Tage, sonst ganze Stunden, sonst Minuten. */
enum class DurationUnit(val minutes: Int) {
    MINUTES(1), HOURS(60), DAYS(24 * 60);

    /** Wert in dieser Einheit; nur sinnvoll, wenn [of] diese Einheit gewählt hat. */
    fun value(totalMinutes: Int): Int = totalMinutes / minutes

    companion object {
        /** Die größte Einheit, in der alle [minutes] ganze Zahlen ergeben. */
        fun of(vararg minutes: Int): DurationUnit =
            entries.last { unit -> unit == MINUTES || minutes.all { it > 0 && it % unit.minutes == 0 } }
    }
}
