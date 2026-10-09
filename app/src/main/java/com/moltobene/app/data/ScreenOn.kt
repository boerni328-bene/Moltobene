package com.moltobene.app.data

/**
 * „Bildschirm in der Rezeptansicht“ (#68): wie lange der Bildschirm beim Kochen anbleibt. Gespeichert wird mit fester
 * englischer Kennung in [AppPreferences], nicht in der Sicherung; unbekannte Werte werden zum Standard „Bleibt an“.
 *
 * Umgesetzt nur über „Bildschirm anlassen“ der Ansicht – keine Berechtigung, kein Wecker, kein Hintergrunddienst.
 * Endet die Frist, gilt wieder die Einstellung des Handys.
 */
enum class ScreenOn(val key: String, val minutes: Int?) {
    /** Bleibt an, solange das Rezept offen ist (Standard, wie bisher). */
    STAYS_ON("stays_on", null),
    OFF_AFTER_15("off_after_15_minutes", 15),
    OFF_AFTER_30("off_after_30_minutes", 30),

    /** Die App hält den Bildschirm nicht an. */
    DEVICE("device", null);

    companion object {
        fun fromKey(key: String?): ScreenOn = entries.firstOrNull { it.key == key } ?: STAYS_ON
    }
}

/**
 * Die Frist nach dem letzten Tippen: Jede Berührung startet sie neu. [clock] liefert Millisekunden einer gleichmäßig
 * laufenden Uhr (auf dem Handy die Zeit seit dem Einschalten, in Tests eine nachgestellte Uhr).
 */
class ScreenOnTimer(private val setting: ScreenOn, private val clock: () -> Long) {
    private var lastTouch = clock()

    fun touch() {
        lastTouch = clock()
    }

    /** Wie lange der Bildschirm noch anbleiben soll: null ohne Ende, 0 nicht (mehr). */
    fun remainingMillis(): Long? {
        val minutes = setting.minutes ?: return if (setting == ScreenOn.DEVICE) 0 else null
        return (minutes * MILLIS_PER_MINUTE - (clock() - lastTouch)).coerceAtLeast(0)
    }

    fun keepsScreenOn(): Boolean = remainingMillis().let { it == null || it > 0 }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
