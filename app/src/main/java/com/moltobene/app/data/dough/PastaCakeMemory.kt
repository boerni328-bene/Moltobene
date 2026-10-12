package com.moltobene.app.data.dough

import com.moltobene.app.data.Pan
import com.moltobene.app.data.PanScaling

private fun parseValues(text: String?): Map<String, String> = text.orEmpty().split(';').mapNotNull { part ->
    val index = part.indexOf('=')
    if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
}.toMap()

/**
 * Zuletzt benutzte Werte für Pasta (#70), je Art getrennt; geprüft wird jeder Wert gegen [PastaLimits]. Unlesbares,
 * Fremdes oder Außerhalb ergibt den Startwert dieses Felds, nie eine Ausnahme.
 */
object PastaMemory {

    fun key(kind: PastaKind) = "pasta.${kind.key}"

    fun encode(input: PastaInput): String = listOf(
        "portions=${input.portions}",
        "flour=${input.flourPerPortion}",
        "semolina=${input.semolina}",
        "egg=${input.egg}",
        "yolk=${input.yolk}",
        "water=${input.water}",
        "salt=${input.salt}",
        "oil=${input.oil}",
    ).joinToString(";")

    fun decode(kind: PastaKind, text: String?): PastaInput {
        val defaults = PastaLimits.defaults(kind)
        val values = parseValues(text)
        fun number(key: String, field: DoughField, default: Double): Double {
            val value = values[key]?.toDoubleOrNull() ?: return default
            return if (value in PastaLimits.range(kind, field)) value else default
        }
        val portions = values["portions"]?.toIntOrNull()
            ?.takeIf { it.toDouble() in PastaLimits.range(kind, DoughField.COUNT) } ?: defaults.portions
        val decoded = PastaInput(
            kind = kind,
            portions = portions,
            flourPerPortion = number("flour", DoughField.FLOUR_PER_PORTION, defaults.flourPerPortion),
            semolina = number("semolina", DoughField.SEMOLINA, defaults.semolina),
            egg = number("egg", DoughField.WHOLE_EGG, defaults.egg),
            yolk = number("yolk", DoughField.YOLK, defaults.yolk),
            water = number("water", DoughField.WATER, defaults.water),
            salt = number("salt", DoughField.SALT, defaults.salt),
            oil = number("oil", DoughField.OIL, defaults.oil),
        )
        // Zusammen gültig? Sonst sind die Startwerte sicherer als ein Teig ohne Flüssigkeit.
        return if (PastaCalculator.validate(decoded).isEmpty()) decoded else defaults
    }
}

/** Zuletzt benutzte Werte für Kuchen (#70), je Art getrennt; wie [PastaMemory] mit [CakeLimits]. */
object CakeMemory {

    fun key(kind: CakeKind) = "cake.${kind.key}"

    fun encode(input: CakeInput): String {
        val size = when (val size = input.size) {
            is CakeSize.PanSize -> "size=pan;round=${if (size.pan.round) 1 else 0};a=${size.pan.a};b=${size.pan.b}"
            is CakeSize.FlourGrams -> "size=flour;grams=${size.grams}"
        }
        return listOf(
            size,
            "fat=${input.fat}",
            "sugar=${input.sugar}",
            "egg=${input.egg}",
            "milk=${input.milk}",
            "quark=${input.quark}",
            "yeastKind=${input.yeastKind.key}",
            "yeast=${input.yeast}",
            "baking=${input.bakingPowder}",
            "salt=${input.salt}",
        ).joinToString(";")
    }

    fun decode(kind: CakeKind, text: String?): CakeInput {
        val defaults = CakeLimits.defaults(kind)
        val values = parseValues(text)
        val used = CakeLimits.fields(kind)
        fun number(key: String, field: CakeField, default: Double): Double {
            if (field !in used) return default
            val value = values[key]?.toDoubleOrNull() ?: return default
            return if (value in CakeLimits.range(kind, field)) value else default
        }
        val flourRange = CakeLimits.flourRange(kind)
        val size = when (values["size"]) {
            "flour" -> values["grams"]?.toDoubleOrNull()?.takeIf { it in flourRange }?.let { CakeSize.FlourGrams(it) }
            "pan" -> {
                val a = values["a"]?.toDoubleOrNull()
                val b = values["b"]?.toDoubleOrNull()
                if (a == null || b == null) {
                    null
                } else {
                    val pan = if (values["round"] == "1") Pan.round(a) else Pan.rectangle(a, b)
                    pan.takeIf { PanScaling.isValid(it) }?.let { CakeSize.PanSize(it) }
                }
            }
            else -> null
        } ?: defaults.size
        val decoded = CakeInput(
            kind = kind,
            size = size,
            fat = number("fat", CakeField.FAT, defaults.fat),
            sugar = number("sugar", CakeField.SUGAR, defaults.sugar),
            egg = number("egg", CakeField.EGG, defaults.egg),
            milk = number("milk", CakeField.MILK, defaults.milk),
            quark = number("quark", CakeField.QUARK, defaults.quark),
            yeastKind = YeastKind.fromKey(values["yeastKind"]) ?: defaults.yeastKind,
            yeast = number("yeast", CakeField.YEAST, defaults.yeast),
            bakingPowder = number("baking", CakeField.BAKING_POWDER, defaults.bakingPowder),
            salt = number("salt", CakeField.SALT, defaults.salt),
        )
        // Die Form kann zusammen mit der Art zu viel oder zu wenig Mehl ergeben; dann gilt die Startgröße.
        return if (CakeCalculator.validate(decoded).isEmpty()) decoded else decoded.copy(size = defaults.size)
    }
}
