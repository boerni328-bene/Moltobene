package com.moltobene.app.data.dough

import com.moltobene.app.data.PhasePlace

/** Feste deutsche Texte für die Tests des Teigrechners (statt strings.xml). */
internal object GermanTexts : DoughTexts {
    override fun title(type: DoughType, preferment: Preferment) =
        if (preferment == Preferment.NONE) "Pizzateig" else "Pizzateig mit ${preferment.key}"

    override fun pastaTitle(kind: PastaKind) = "Nudelteig ${kind.key}"

    override fun cakeTitle(kind: CakeKind) = "Kuchenteig ${kind.key}"

    override fun heading(kind: SectionKind, preferment: Preferment) =
        if (kind == SectionKind.PREFERMENT) "Für den Vorteig (${preferment.key})" else "Für den Hauptteig"

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

    override fun phase(kind: PhaseKind, preferment: Preferment) = "${kind.name}:${preferment.key}"

    override fun duration(minMinutes: Int, maxMinutes: Int) = "$minMinutes-$maxMinutes min"

    override fun place(place: PhasePlace) = if (place == PhasePlace.NONE) "" else place.key
}
