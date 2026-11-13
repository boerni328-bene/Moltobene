package com.moltobene.app.data.dough

import com.moltobene.app.data.PhasePlace
import com.moltobene.app.data.TimelinePhase
import kotlin.math.roundToInt

/** Die Arten von Phasen im Zeitplan eines Teigs (#71); der Text kommt aus strings.xml ([DoughTexts.phase]). */
enum class PhaseKind { PREFERMENT_MIX, PREFERMENT_RIPEN, KNEAD, BULK, SHAPE, PROOF, WARM_UP, BAKE }

/**
 * Zeitplan für Pizza und Brot (#71). Die Gehzeiten kommen seit 0.74.0 aus dem Teigrechner ([DoughProcess], Wunsch vom
 * 05.11.2026) und stehen als eine Dauer im Zeitplan; die Arbeitsschritte (Ansetzen, Kneten, Formen, Backen) behalten
 * ihre Spannen (Richtwerte nach `docs/recherche/2026-10-17-zeitplan.md`). Der Nutzer schließt jede Phase selbst ab,
 * wenn der Teig so weit ist.
 */
object DoughTimeline {

    /** Eine Phase mit Dauer in Minuten (frühestens, spätestens) und Ort. */
    data class Planned(val kind: PhaseKind, val min: Int, val max: Int, val place: PhasePlace)

    /**
     * Zeitplan zur Eingabe; [process] sind die wirksamen Gehzeiten ([DoughProcessRules.effective]). Ohne Angabe gelten
     * die Standardwerte zu Teigart und Vorteig.
     */
    fun build(
        input: LeavenedInput,
        texts: DoughTexts,
        process: DoughProcess = DoughProcessRules.defaults(input.type, input.preferment),
        kneading: Kneading = Kneading.HAND,
    ): List<TimelinePhase> = plan(input.type, input.preferment, process, kneading).map {
        TimelinePhase(texts.phase(it.kind, input.type, input.preferment), it.min, it.max, it.place)
    }

    /** Die Phasen in ihrer Reihenfolge; Pizza aus dem Kühlschrank wärmt vor dem Backen auf, Brot backt direkt. */
    fun plan(type: DoughType, preferment: Preferment, process: DoughProcess, kneading: Kneading): List<Planned> {
        fun minutes(hours: Double): Int = (hours * 60).roundToInt().coerceAtLeast(1)
        val pizza = type == DoughType.PIZZA
        val out = mutableListOf<Planned>()
        if (preferment != Preferment.NONE) {
            out += Planned(PhaseKind.PREFERMENT_MIX, 5, 15, PhasePlace.NONE)
            val ripen = minutes(process.ripen)
            out += Planned(PhaseKind.PREFERMENT_RIPEN, ripen, ripen, if (preferment == Preferment.BIGA) PhasePlace.FRIDGE else PhasePlace.ROOM)
        }
        val (kneadMin, kneadMax) = DoughProcessRules.kneadMinutes(kneading)
        out += Planned(PhaseKind.KNEAD, kneadMin, kneadMax, PhasePlace.NONE)
        val bulk = minutes(process.bulk)
        out += Planned(PhaseKind.BULK, bulk, bulk, PhasePlace.ROOM)
        out += Planned(PhaseKind.SHAPE, 10, 20, PhasePlace.NONE)
        val cold = process.proofPlace == RestPlace.FRIDGE
        val proof = minutes(process.proof)
        out += Planned(PhaseKind.PROOF, proof, proof, if (cold) PhasePlace.FRIDGE else PhasePlace.ROOM)
        if (pizza && cold) {
            val warm = minutes(process.warm)
            out += Planned(PhaseKind.WARM_UP, warm, warm, PhasePlace.ROOM)
        }
        out += if (pizza) Planned(PhaseKind.BAKE, 8, 15, PhasePlace.OVEN) else Planned(PhaseKind.BAKE, 30, 50, PhasePlace.OVEN)
        return out
    }
}
