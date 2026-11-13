package com.moltobene.app.data.dough

import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.TemperatureUnit
import com.moltobene.app.data.TimelinePhase
import com.moltobene.app.data.WeightUnit

/** Was ein Schritt der Zubereitung sagt (Wunsch vom 05.11.2026); Mengen und Temperaturen sind fertiger Text. */
sealed interface StepDetail {
    /** Vorteig ansetzen: [ingredients] wie „184 g Mehl, 92 g Wasser und 1,2 g Frischhefe“. */
    data class Mix(val preferment: Preferment, val ingredients: String) : StepDetail

    data class Ripen(val preferment: Preferment) : StepDetail

    /**
     * Kneten: mit Vorteig [preferment] (z. B. „277 g Biga“) und dazu [ingredients]; [salt] kommt nach etwa 5 Minuten,
     * [doughTemp] ist das Ziel nach dem Kneten.
     */
    data class Knead(val withPreferment: Preferment, val preferment: String?, val ingredients: String, val salt: String?, val doughTemp: String) : StepDetail

    data object Bulk : StepDetail

    data class Shape(val type: DoughType, val count: Int, val piece: String) : StepDetail

    /** Teigkugeln bzw. Laib gehen lassen; [oven] zum Vorheizen (bei Pizza aus dem Kühlschrank erst beim Aufwärmen). */
    data class Proof(val type: DoughType, val place: RestPlace, val oven: String) : StepDetail

    data class Warm(val oven: String) : StepDetail

    data class Bake(val type: DoughType) : StepDetail
}

/**
 * Die Schritte der Zubereitung zum Zeitplan (Wunsch vom 05.11.2026): je Phase Name, Dauer, Ort mit Temperatur und eine
 * Anleitung mit Mengen, z. B. „Teig kneten (15 bis 20 Minuten): 613 g Mehl, 368 g Wasser (20 °C) und 1,2 g Frischhefe
 * kneten. …“. Reines Kotlin; die Sätze kommen aus [DoughTexts].
 */
object DoughSteps {

    @Suppress("LongParameterList")
    fun build(
        plan: List<DoughTimeline.Planned>,
        timeline: List<TimelinePhase>,
        result: DoughResult.Success,
        input: LeavenedInput,
        process: DoughProcess,
        kitchen: DoughKitchen,
        waterText: String?,
        texts: DoughTexts,
        language: String,
        unit: WeightUnit,
        temperatureUnit: TemperatureUnit,
    ): List<String> {
        val oven = temperatureUnit.format(process.ovenTemp)
        fun amount(line: DoughLine, temperature: String? = null): String {
            val text = DoughNumbers.weight(line.grams, line.role, language, unit) + " " + texts.ingredient(line.role, input.yeastKind)
            return if (temperature != null && line.role == LineRole.WATER) "$text ($temperature)" else text
        }
        val preferment = input.preferment
        val pre = result.sections.firstOrNull { it.kind == SectionKind.PREFERMENT }?.lines.orEmpty().filter { it.grams > 0 }
        val plain = result.sections.firstOrNull { it.kind == SectionKind.PLAIN }?.lines.orEmpty().filter { it.grams > 0 }
        val kneadLines = if (preferment == Preferment.NONE) plain else DoughSummary.mainLines(result)
        val salt = kneadLines.firstOrNull { it.role == LineRole.SALT }
        return plan.mapIndexed { index, phase ->
            val detail = when (phase.kind) {
                PhaseKind.PREFERMENT_MIX -> {
                    // Poolish und Sauerteig mit Wasser in Raumtemperatur; die Biga kommt ohnehin in den Kühlschrank.
                    val warmWater = if (preferment == Preferment.BIGA) null else temperatureUnit.format(kitchen.room)
                    StepDetail.Mix(preferment, texts.join(pre.map { amount(it, warmWater) }))
                }
                PhaseKind.PREFERMENT_RIPEN -> StepDetail.Ripen(preferment)
                PhaseKind.KNEAD -> StepDetail.Knead(
                    withPreferment = preferment,
                    preferment = if (preferment == Preferment.NONE) null else
                        DoughNumbers.weight(DoughSummary.prefermentGrams(result), 0, language, unit) + " " + texts.prefermentName(preferment),
                    ingredients = texts.join(kneadLines.filter { it.role != LineRole.SALT }.map { amount(it, waterText) }),
                    salt = salt?.let { amount(it) },
                    doughTemp = temperatureUnit.format(process.doughTemp),
                )
                PhaseKind.BULK -> StepDetail.Bulk
                PhaseKind.SHAPE -> StepDetail.Shape(input.type, input.count, DoughNumbers.weight(input.pieceGrams, 1, language, unit))
                PhaseKind.PROOF -> StepDetail.Proof(input.type, process.proofPlace, oven)
                PhaseKind.WARM_UP -> StepDetail.Warm(oven)
                PhaseKind.BAKE -> StepDetail.Bake(input.type)
            }
            val place = when (phase.place) {
                PhasePlace.ROOM -> texts.placeAt(PhasePlace.ROOM, temperatureUnit.format(kitchen.room))
                PhasePlace.FRIDGE -> texts.placeAt(PhasePlace.FRIDGE, temperatureUnit.format(kitchen.fridge))
                PhasePlace.OVEN -> texts.placeAt(PhasePlace.OVEN, oven)
                PhasePlace.NONE -> ""
            }
            val duration = texts.duration(phase.min, phase.max)
            val head = if (place.isEmpty()) duration else "$duration, $place"
            "${timeline[index].text} ($head): ${texts.stepDetail(detail)}"
        }
    }
}
