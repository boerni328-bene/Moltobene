package com.moltobene.app.data.dough

/**
 * Zuletzt benutzte Werte des Teigrechners je Teigart (#69): ein Satz Zahlen und Kennungen als Text, ohne Verlauf, ohne
 * Namen. Beim Laden wird jeder Wert gegen [DoughLimits] geprüft; Unlesbares, Fremdes oder Außerhalb ergibt den
 * Standardwert dieses Felds – nie eine Ausnahme.
 */
object DoughMemory {

    fun encode(input: LeavenedInput): String = listOf(
        "count=${input.count}",
        "piece=${input.pieceGrams}",
        "water=${input.water}",
        "salt=${input.salt}",
        "oil=${input.oil}",
        "yeastKind=${input.yeastKind.key}",
        "yeast=${input.yeast}",
        "preferment=${input.preferment.key}",
        "prefermentFlour=${input.prefermentFlour}",
        "prefermentWater=${input.prefermentWater}",
        "malt=${input.malt}",
        "yogurt=${input.yogurt}",
        "milk=${input.milk}",
        "sugar=${input.sugar}",
        "rye=${input.rye}",
        "whole=${input.whole}",
        "spelt=${input.spelt}",
        "honey=${input.honey}",
        "egg=${input.egg}",
        "seeds=${input.seeds}",
    ).joinToString(";")

    /** Die gemerkten Werte für [type]; ohne oder mit kaputtem [text] die Standardwerte. */
    fun decode(type: DoughType, text: String?): LeavenedInput {
        val defaults = DoughLimits.defaults(type).copy(type = type)
        val values = text.orEmpty().split(';').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
        }.toMap()
        val preferment = Preferment.fromKey(values["preferment"]) ?: defaults.preferment

        fun number(key: String, field: DoughField, default: Double, withPreferment: Preferment = preferment): Double {
            val value = values[key]?.toDoubleOrNull() ?: return default
            return if (value in DoughLimits.range(type, field, withPreferment)) value else default
        }

        val count = values["count"]?.toIntOrNull()
            ?.takeIf { it.toDouble() in DoughLimits.range(type, DoughField.COUNT) } ?: defaults.count
        return LeavenedInput(
            type = type,
            count = count,
            pieceGrams = number("piece", DoughField.PIECE_GRAMS, defaults.pieceGrams),
            water = number("water", DoughField.WATER, defaults.water),
            salt = number("salt", DoughField.SALT, defaults.salt),
            oil = number("oil", DoughField.OIL, defaults.oil),
            yeastKind = YeastKind.fromKey(values["yeastKind"]) ?: defaults.yeastKind,
            yeast = number("yeast", DoughField.YEAST, defaults.yeast),
            preferment = preferment,
            prefermentFlour = number("prefermentFlour", DoughField.PREFERMENT_FLOUR, defaults.prefermentFlour),
            prefermentWater = number("prefermentWater", DoughField.PREFERMENT_WATER, defaults.prefermentWater),
            malt = number("malt", DoughField.MALT, 0.0),
            yogurt = number("yogurt", DoughField.YOGURT, 0.0),
            milk = number("milk", DoughField.MILK, 0.0),
            sugar = number("sugar", DoughField.SUGAR, 0.0),
            rye = number("rye", DoughField.RYE, 0.0),
            whole = number("whole", DoughField.WHOLE, 0.0),
            spelt = number("spelt", DoughField.SPELT, 0.0),
            honey = number("honey", DoughField.HONEY, 0.0),
            egg = number("egg", DoughField.EGG, 0.0),
            seeds = number("seeds", DoughField.SEEDS, 0.0),
        )
    }
}
