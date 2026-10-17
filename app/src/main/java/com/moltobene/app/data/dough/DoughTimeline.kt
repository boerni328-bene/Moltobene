package com.moltobene.app.data.dough

import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.TimelinePhase

/** Die Arten von Phasen im Zeitplan eines Teigs (#71); der Text kommt aus strings.xml ([DoughTexts.phase]). */
enum class PhaseKind { PREFERMENT_MIX, PREFERMENT_RIPEN, KNEAD, BULK, SHAPE, PROOF, WARM_UP, BAKE }

/**
 * Zeitplan für Pizza und Brot (#71, Richtwerte nach `docs/recherche/2026-10-17-zeitplan.md`): welche Phasen es gibt,
 * wie lange sie frühestens und spätestens dauern und wo sie stattfinden. Die Dauern sind Spannen, weil Temperatur und
 * Mehl mitreden; der Nutzer schließt jede Phase selbst ab, wenn der Teig so weit ist.
 */
object DoughTimeline {

    private const val H = 60
    private const val D = 24 * 60

    private data class Step(val kind: PhaseKind, val min: Int, val max: Int, val place: PhasePlace)

    /** Zeitplan zur Eingabe; leer für Teige ohne Zeitplan (Pasta, Kuchen). */
    fun build(input: LeavenedInput, texts: DoughTexts): List<TimelinePhase> =
        steps(input.type, input.preferment).map {
            TimelinePhase(texts.phase(it.kind, input.preferment), it.min, it.max, it.place)
        }

    private fun steps(type: DoughType, preferment: Preferment): List<Step> {
        val out = mutableListOf<Step>()
        if (preferment != Preferment.NONE) {
            out += Step(PhaseKind.PREFERMENT_MIX, 5, 15, PhasePlace.NONE)
            out += when (preferment) {
                // Biga: fester Vorteig, 1 bis 3 Tage kühl.
                Preferment.BIGA -> Step(PhaseKind.PREFERMENT_RIPEN, 24 * H, 3 * D, PhasePlace.FRIDGE)
                Preferment.POOLISH -> Step(PhaseKind.PREFERMENT_RIPEN, 8 * H, 16 * H, PhasePlace.ROOM)
                else -> Step(PhaseKind.PREFERMENT_RIPEN, 8 * H, 12 * H, PhasePlace.ROOM)
            }
        }
        out += Step(PhaseKind.KNEAD, 10, 30, PhasePlace.NONE)
        val pizza = type == DoughType.PIZZA
        val long = preferment == Preferment.NONE
        when {
            pizza && long -> {
                out += Step(PhaseKind.BULK, 1 * H, 2 * H, PhasePlace.ROOM)
                out += Step(PhaseKind.SHAPE, 10, 20, PhasePlace.NONE)
                out += Step(PhaseKind.PROOF, 8 * H, 24 * H, PhasePlace.FRIDGE)
                out += Step(PhaseKind.WARM_UP, 1 * H, 2 * H, PhasePlace.ROOM)
            }
            pizza -> {
                out += Step(PhaseKind.BULK, 1 * H, 2 * H, PhasePlace.ROOM)
                out += Step(PhaseKind.SHAPE, 10, 20, PhasePlace.NONE)
                out += Step(PhaseKind.PROOF, 2 * H, 6 * H, PhasePlace.ROOM)
            }
            else -> {
                out += Step(PhaseKind.BULK, 1 * H, 3 * H, PhasePlace.ROOM)
                out += Step(PhaseKind.SHAPE, 10, 20, PhasePlace.NONE)
                out += Step(PhaseKind.PROOF, 45, 90, PhasePlace.ROOM)
            }
        }
        out += if (pizza) Step(PhaseKind.BAKE, 8, 15, PhasePlace.OVEN) else Step(PhaseKind.BAKE, 30, 50, PhasePlace.OVEN)
        return out
    }
}
