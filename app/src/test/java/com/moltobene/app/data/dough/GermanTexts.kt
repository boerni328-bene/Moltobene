package com.moltobene.app.data.dough

import com.moltobene.app.data.PhasePlace

/** Feste deutsche Texte für die Tests des Teigrechners (statt strings.xml). */
internal object GermanTexts : DoughTexts {
    override fun title(type: DoughType, preferment: Preferment) =
        if (preferment == Preferment.NONE) "Pizzateig" else "Pizzateig mit ${preferment.key}"

    override fun pastaTitle(kind: PastaKind) = "Nudelteig ${kind.key}"

    override fun cakeTitle(kind: CakeKind) = "Kuchenteig ${kind.key}"

    override fun heading(kind: SectionKind, preferment: Preferment, amount: String) =
        if (kind == SectionKind.PREFERMENT) "Für den Vorteig ($amount ${preferment.key})" else "Für den Hauptteig ($amount)"

    override fun wholePreferment(preferment: Preferment, amount: String) = "$amount ${preferment.key} (der ganze Vorteig)"

    override fun prefermentName(preferment: Preferment) = preferment.key

    override fun ingredient(role: LineRole, yeastKind: YeastKind) = when (role) {
        LineRole.FLOUR -> "Mehl"
        LineRole.WATER -> "Wasser"
        LineRole.SALT -> "Salz"
        LineRole.OIL -> "Öl"
        LineRole.YEAST -> "Frischhefe"
        LineRole.SEMOLINA -> "Hartweizengrieß"
        LineRole.EGG -> "Eier"
        LineRole.YOLK -> "Eigelb"
        LineRole.MILK -> "Milch"
        LineRole.LIQUID -> "Wasser oder Milch"
        LineRole.SUGAR -> "Zucker"
        LineRole.BUTTER -> "Butter"
        LineRole.QUARK -> "Quark"
        LineRole.BAKING_POWDER -> "Backpulver"
        LineRole.MALT -> "Backmalz"
        LineRole.YOGURT -> "Joghurt"
        LineRole.WHEAT -> "Weizenmehl"
        LineRole.RYE -> "Roggenmehl"
        LineRole.WHOLE -> "Weizenvollkornmehl"
        LineRole.SPELT -> "Dinkelmehl"
        LineRole.HONEY -> "Honig"
        LineRole.SEEDS -> "Saaten"
    }

    override fun servingsUnit(kind: YieldKind, detail: String) = when (kind) {
        YieldKind.BALL -> "Teigkugeln (je $detail)"
        YieldKind.LOAF -> "Stück (je $detail)"
        YieldKind.PORTION -> "Portionen (je $detail Mehl)"
        YieldKind.PAN -> "Backform ($detail)"
        YieldKind.DOUGH -> "Teig"
    }

    override fun phase(kind: PhaseKind, type: DoughType, preferment: Preferment) = "${kind.name}:${preferment.key}"

    override fun duration(minMinutes: Int, maxMinutes: Int) = "$minMinutes-$maxMinutes min"

    override fun placeAt(place: PhasePlace, temperature: String) = if (place == PhasePlace.NONE) "" else "${place.key} $temperature"

    override fun join(parts: List<String>) =
        if (parts.size <= 1) parts.joinToString("") else parts.dropLast(1).joinToString(", ") + " und " + parts.last()

    override fun stepDetail(detail: StepDetail) = when (detail) {
        is StepDetail.Mix -> "mischen: ${detail.ingredients}"
        is StepDetail.Ripen -> "reifen ${detail.preferment.key}"
        is StepDetail.Knead -> listOfNotNull(detail.preferment, "mit ${detail.ingredients}", detail.salt?.let { "Salz $it" }, "Ziel ${detail.doughTemp}")
            .joinToString(" | ")
        StepDetail.Bulk -> "gehen"
        is StepDetail.Shape -> "${detail.count} × ${detail.piece}"
        is StepDetail.Proof -> "${detail.type.key} ${detail.place.key} ${detail.oven}"
        is StepDetail.Warm -> "aufwärmen ${detail.oven}"
        is StepDetail.Bake -> "backen ${detail.type.key}"
    }
}
