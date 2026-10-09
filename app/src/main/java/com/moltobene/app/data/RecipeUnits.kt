package com.moltobene.app.data

/**
 * Einheiten in Zutatenzeilen (#65), gemeinsam für „Portionen umrechnen“ und die Texterkennung: Deutsch, Englisch,
 * Italienisch, Französisch und Spanisch. Reines Kotlin, per Unit-Test prüfbar.
 */
object RecipeUnits {

    /** Wie eine Menge mit dieser Einheit umgerechnet und gerundet wird. */
    enum class Kind {
        /** Gramm und Milliliter: ab 100 auf 5, ab 10 auf 1, darunter auf 0,5 gerundet. */
        GRAMS,

        /** Stück und Packungen („Würfel“, „Päckchen“, „packet“) und Zutaten ohne Einheit: auf ½, unter 1 auf ¼. */
        PIECES,

        /** Alles andere (kg, l, Löffel, Tassen, oz …): nicht gerundet, wie bisher. */
        OTHER,
    }

    val GRAMS = listOf("g", "gr", "gr.", "Gramm", "gram", "grams", "ml", "mL", "Milliliter")

    /**
     * Packungen und Stückangaben, die nur als Anzahl umgerechnet werden – nie in Gramm, denn ein Würfel Frischhefe
     * hat in Deutschland 42 g, in Italien 25 g.
     */
    val PIECES = listOf(
        "Stk.", "Stk", "Stück", "Würfel", "Päckchen", "Pck.", "Pkg.", "Packung", "Do.", "Dose", "Dosen", "Becher", "Glas",
        "Zehe", "Zehen", "Scheibe", "Scheiben", "Stange", "Stangen", "Blatt", "Blätter", "Zweig", "Zweige", "Bund",
        "piece", "pieces", "packet", "packets", "package", "packages", "envelope", "envelopes", "cube", "cubes",
        "can", "cans", "clove", "cloves", "slice", "slices", "stick", "sticks",
        "bustina", "bustine", "cubetto", "cubetti", "panetto", "spicchio", "spicchi", "pezzo", "pezzi",
        "sachet", "sachets", "gousse", "gousses", "tranche", "tranches",
        "sobre", "sobres", "lata", "latas", "diente", "dientes",
    )

    val OTHER = listOf(
        "kg", "mg", "cl", "dl", "l", "Liter", "liter", "litre", "oz", "lb", "lbs",
        "EL", "TL", "Esslöffel", "Teelöffel", "Msp.", "Pr", "Pr.", "Prise", "Prisen", "Tasse", "Tassen", "Handvoll",
        "cup", "cups", "tbsp", "tbsp.", "tsp", "tsp.", "tablespoon", "tablespoons", "teaspoon", "teaspoons", "pinch",
        "cucchiaio", "cucchiai", "cucchiaino", "cucchiaini", "bicchiere", "pizzico",
        "cuillère", "cuillères", "c.à.s.", "c.à.c.", "càs", "càc", "pincée",
        "cucharada", "cucharadas", "cucharadita", "cucharaditas", "cda", "cda.", "cdas", "cdas.", "cdta", "cdta.", "cdtas", "cdtas.",
        "pizca", "taza", "tazas",
    )

    /** Alle bekannten Einheiten, z. B. für die Korrektur von Lesefehlern der Texterkennung. */
    val ALL: List<String> = (GRAMS + PIECES + OTHER).distinct()

    /** Art der Einheit; null, wenn es keine bekannte Einheit ist (dann zählt die Zutat selbst, z. B. „3 Eier“). */
    fun kindOf(unit: String): Kind? = KINDS[key(unit)]

    /** Groß- und Kleinschreibung und ein Punkt am Ende zählen nicht: „EL“ = „el“, „Stk“ = „Stk.“, „g.“ = „g“. */
    private fun key(unit: String) = unit.trim().trimEnd('.').lowercase()

    private val KINDS: Map<String, Kind> =
        (GRAMS.map { key(it) to Kind.GRAMS } + PIECES.map { key(it) to Kind.PIECES } + OTHER.map { key(it) to Kind.OTHER })
            .filter { it.first.isNotEmpty() }
            .distinctBy { it.first }
            .toMap()
}
