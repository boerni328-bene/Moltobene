package com.moltobene.app.data.dough

/**
 * Gemerkte Gehzeiten und Temperaturen (Wunsch vom 05.11.2026): je Teigart ein [DoughProcess], dazu einmal die Küche
 * ([DoughKitchen]). Gleiches Format wie [DoughMemory] („Name=Wert;…“). Unlesbares, Fremdes oder Außerhalb ergibt den
 * Standardwert dieses Felds – nie eine Ausnahme. Gespeichert wird nur auf dem Handy, nicht in der Sicherung.
 */
object DoughProcessMemory {

    /** Schlüssel in den Merkwerten des Teigrechners. */
    const val KITCHEN_KEY = "kitchen"

    fun processKey(type: DoughType): String = "process_" + type.key

    fun encode(process: DoughProcess): String = listOf(
        "doughTemp=${process.doughTemp}",
        "ovenTemp=${process.ovenTemp}",
        "ripen=${process.ripen}",
        "bulk=${process.bulk}",
        "proof=${process.proof}",
        "warm=${process.warm}",
        "proofPlace=${process.proofPlace.key}",
        "manual=${process.manual.joinToString(",") { it.key }}",
    ).joinToString(";")

    /** Die gemerkten Werte für [type] mit [preferment]; ohne oder mit kaputtem [text] die Standardwerte. */
    fun decode(type: DoughType, preferment: Preferment, text: String?): DoughProcess {
        val values = parse(text)
        val defaults = DoughProcessRules.defaults(type, preferment)
        val place = RestPlace.fromKey(values["proofPlace"]) ?: defaults.proofPlace

        fun temperature(key: String, field: TemperatureField, default: Double): Double =
            values[key]?.toDoubleOrNull()?.takeIf { it in field } ?: default

        fun hours(time: RestTime): Double {
            val list = DoughProcessRules.steps(time, type, preferment, place)
            val default = DoughProcessRules.defaultHours(time, type, preferment, place)
            val value = values[time.key]?.toDoubleOrNull() ?: return default
            return list.firstOrNull { kotlin.math.abs(it - value) < 1e-6 } ?: default
        }

        val manual = values["manual"].orEmpty().split(',').mapNotNull { RestTime.fromKey(it.trim()) }.toSet()
        return DoughProcess(
            doughTemp = temperature("doughTemp", TemperatureField.DOUGH, defaults.doughTemp),
            ovenTemp = temperature("ovenTemp", TemperatureField.OVEN, defaults.ovenTemp),
            ripen = if (preferment == Preferment.NONE) 0.0 else hours(RestTime.RIPEN),
            bulk = hours(RestTime.BULK),
            proof = hours(RestTime.PROOF),
            warm = hours(RestTime.WARM),
            proofPlace = place,
            manual = manual,
        )
    }

    fun encodeKitchen(kitchen: DoughKitchen): String =
        listOf("room=${kitchen.room}", "fridge=${kitchen.fridge}", "kneading=${kitchen.kneading.key}").joinToString(";")

    fun decodeKitchen(text: String?): DoughKitchen {
        val values = parse(text)
        return DoughKitchen(
            room = values["room"]?.toDoubleOrNull()?.takeIf { it in TemperatureField.ROOM } ?: DoughKitchen.DEFAULT_ROOM,
            fridge = values["fridge"]?.toDoubleOrNull()?.takeIf { it in TemperatureField.FRIDGE } ?: DoughKitchen.DEFAULT_FRIDGE,
            kneading = Kneading.fromKey(values["kneading"]) ?: Kneading.HAND,
        )
    }

    private fun parse(text: String?): Map<String, String> = text.orEmpty().split(';').mapNotNull { part ->
        val index = part.indexOf('=')
        if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
    }.toMap()
}
