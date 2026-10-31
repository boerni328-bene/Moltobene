package com.moltobene.app.data.dough

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Voreinstellungen des Teigrechners (#83, Etappe 3): fertige Sätze von Werten für Pizza und Brot, zum Durchschalten.
 * Eine Voreinstellung legt nur fest, wie der Teig wird (Wasser, Salz, Öl, Hefe, Vorteig); Anzahl und Gewicht je Stück
 * bleiben, wie sie sind. Reines Kotlin ohne Android, per Unit-Test prüfbar.
 *
 * Mitgelieferte Voreinstellungen haben feste englische Kennungen, der Name kommt aus strings.xml. Die Werte sind
 * gerundete Richtwerte, eigene Festlegung nach Abgleich mit mehreren Quellen (`docs/recherche/2026-10-12-voreinstellungen.md`).
 */
object DoughPresets {

    /** Eine mitgelieferte Voreinstellung; [key] ist fest und wird nie übersetzt. */
    data class Shipped(val key: String, val input: LeavenedInput)

    /** Wie viele eigene Voreinstellungen je Teigart, und wie lang ein Name höchstens ist. */
    const val MAX_OWN = 20
    const val MAX_NAME_LENGTH = 40

    /** Alle mitgelieferten Voreinstellungen einer Teigart; Pasta und Kuchen haben (noch) keine. */
    fun shipped(type: DoughType): List<Shipped> = when (type) {
        DoughType.PIZZA -> listOf(
            Shipped("default", DoughLimits.defaults(type)),
            Shipped("neapolitan", base(type, water = 60.0, salt = 2.8, oil = 0.0, yeast = 0.2)),
            Shipped("new_york", base(type, water = 62.0, salt = 2.0, oil = 2.0, yeast = 0.5)),
            Shipped("sheet_pan", base(type, water = 70.0, salt = 2.0, oil = 5.0, yeast = 0.9, yeastKind = YeastKind.INSTANT_DRY)),
            Shipped("detroit", base(type, water = 72.0, salt = 2.0, oil = 2.0, yeast = 0.8, yeastKind = YeastKind.INSTANT_DRY)),
            Shipped("focaccia", base(type, water = 75.0, salt = 2.2, oil = 4.0, yeast = 1.0)),
            Shipped("pinsa", base(type, water = 80.0, salt = 2.2, oil = 2.0, yeast = 0.2)),
            Shipped("grandma", base(type, water = 65.0, salt = 2.0, oil = 3.0, yeast = 0.7, yeastKind = YeastKind.INSTANT_DRY)),
            Shipped("chicago", base(type, water = 52.0, salt = 1.8, oil = 8.0, yeast = 0.7, yeastKind = YeastKind.INSTANT_DRY)),
        )
        DoughType.BREAD -> listOf(
            Shipped("default", DoughLimits.defaults(type)),
            Shipped("white_bread", base(type, water = 65.0, salt = 2.0, oil = 0.0, yeast = 1.0)),
            Shipped(
                "poolish_bread",
                base(
                    type, water = 70.0, salt = 2.0, oil = 0.0, yeast = 0.3,
                    preferment = Preferment.POOLISH, prefermentFlour = 30.0, prefermentWater = 100.0,
                ),
            ),
            Shipped("ciabatta", base(type, water = 78.0, salt = 2.0, oil = 2.0, yeast = 0.5)),
            Shipped("baguette", base(type, water = 68.0, salt = 2.0, oil = 0.0, yeast = 0.5)),
            Shipped("semmel", base(type, water = 58.0, salt = 2.0, oil = 2.0, yeast = 2.0)),
            Shipped("buerli", base(type, water = 62.0, salt = 2.0, oil = 2.0, yeast = 1.5)),
            Shipped("salt_stick", base(type, water = 52.0, salt = 2.0, oil = 4.0, yeast = 2.0)),
            Shipped("lye", base(type, water = 54.0, salt = 2.0, oil = 4.0, yeast = 1.5)),
            Shipped("naan", base(type, water = 50.0, salt = 1.8, oil = 5.0, yeast = 1.2, yeastKind = YeastKind.INSTANT_DRY)),
            Shipped("toast", base(type, water = 58.0, salt = 2.0, oil = 5.0, yeast = 2.0)),
            Shipped("bagel", base(type, water = 55.0, salt = 2.0, oil = 0.0, yeast = 1.0)),
        )
        DoughType.PASTA, DoughType.CAKE -> emptyList()
    }

    private fun base(
        type: DoughType,
        water: Double,
        salt: Double,
        oil: Double,
        yeast: Double,
        yeastKind: YeastKind = YeastKind.FRESH,
        preferment: Preferment = Preferment.NONE,
        prefermentFlour: Double = 30.0,
        prefermentWater: Double = 100.0,
    ): LeavenedInput = DoughLimits.defaults(type).copy(
        water = water, salt = salt, oil = oil, yeastKind = yeastKind, yeast = yeast, preferment = preferment,
        prefermentFlour = prefermentFlour, prefermentWater = prefermentWater,
    )

    /** Die Werte der Voreinstellung [preset] auf die Eingaben [current] anwenden; Anzahl und Stückgewicht bleiben. */
    fun apply(current: LeavenedInput, preset: LeavenedInput): LeavenedInput = current.copy(
        water = preset.water,
        salt = preset.salt,
        oil = preset.oil,
        yeastKind = preset.yeastKind,
        yeast = preset.yeast,
        preferment = preset.preferment,
        prefermentFlour = preset.prefermentFlour,
        prefermentWater = preset.prefermentWater,
    )

    /**
     * Gilt [current] noch als diese Voreinstellung? Verglichen werden nur die Werte, die eine Voreinstellung festlegt;
     * beim Vorteig nur, solange es einen gibt. Kleine Abweichungen durch Umrechnen der Hefeart zählen nicht.
     */
    fun matches(current: LeavenedInput, preset: LeavenedInput): Boolean {
        if (current.preferment != preset.preferment || current.yeastKind != preset.yeastKind) return false
        fun same(a: Double, b: Double) = a.isFinite() && kotlin.math.abs(a - b) < 0.0051
        val withPreferment = current.preferment == Preferment.NONE ||
            (same(current.prefermentFlour, preset.prefermentFlour) && same(current.prefermentWater, preset.prefermentWater))
        return same(current.water, preset.water) && same(current.salt, preset.salt) && same(current.oil, preset.oil) &&
            same(current.yeast, preset.yeast) && withPreferment
    }

    /**
     * Die Stelle in [presets], die zu [current] passt, sonst -1. Gibt es mehrere gleiche, zählt die erste: so steht
     * eine mitgelieferte vor einer eigenen Kopie.
     */
    fun indexOf(current: LeavenedInput, presets: List<LeavenedInput>): Int = presets.indexOfFirst { matches(current, it) }

    /**
     * Die nächste (+1) oder vorherige (−1) Stelle, reihum. Passt nichts ([index] −1), geht „weiter“ auf die erste und
     * „zurück“ auf die letzte.
     */
    fun step(index: Int, size: Int, direction: Int): Int {
        if (size <= 0) return -1
        return when {
            index < 0 -> if (direction >= 0) 0 else size - 1
            else -> ((index + direction) % size + size) % size
        }
    }

    // --- Eigene Voreinstellungen ---------------------------------------------------------------------------------

    /** Eine eigene Voreinstellung: der Name des Nutzers und die Werte im Format der gemerkten Werte ([DoughMemory]). */
    @Serializable
    data class Own(val name: String, val values: String)

    /** Warum eine eigene Voreinstellung nicht gespeichert wurde. */
    enum class SaveResult { SAVED, EMPTY_NAME, TOO_MANY }

    /** Ein Name ohne Zeilenumbrüche und überflüssige Leerzeichen, auf [MAX_NAME_LENGTH] gekürzt; leer bleibt leer. */
    fun cleanName(name: String): String =
        name.filterNot { it.isISOControl() }.trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH).trim()

    /**
     * Fügt [input] unter [name] zu [list] hinzu; gibt es den Namen schon (ohne auf Groß- und Kleinschreibung zu
     * achten), werden dessen Werte ersetzt und der bisherige Name bleibt. Gibt die neue Liste und das Ergebnis zurück.
     */
    fun save(list: List<Own>, name: String, input: LeavenedInput): Pair<List<Own>, SaveResult> {
        val clean = cleanName(name)
        if (clean.isEmpty()) return list to SaveResult.EMPTY_NAME
        val entry = Own(clean, DoughMemory.encode(input))
        val existing = list.indexOfFirst { it.name.equals(clean, ignoreCase = true) }
        if (existing >= 0) return list.toMutableList().also { it[existing] = entry.copy(name = list[existing].name) } to SaveResult.SAVED
        if (list.size >= MAX_OWN) return list to SaveResult.TOO_MANY
        return (list + entry) to SaveResult.SAVED
    }

    fun delete(list: List<Own>, name: String): List<Own> = list.filterNot { it.name == name }

    /** Die Werte einer eigenen Voreinstellung; jeder Wert wird dabei gegen [DoughLimits] geprüft. */
    fun values(type: DoughType, own: Own): LeavenedInput = DoughMemory.decode(type, own.values)

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(list: List<Own>): String = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Own.serializer()), list)

    /**
     * Liest die gemerkten eigenen Voreinstellungen. Unlesbares ergibt eine leere Liste, Einträge mit leerem oder zu
     * langem Namen und alles über [MAX_OWN] hinaus werden verworfen – nie eine Ausnahme.
     */
    fun decode(text: String?): List<Own> {
        if (text.isNullOrBlank()) return emptyList()
        val parsed = runCatching {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Own.serializer()), text)
        }.getOrElse { return emptyList() }
        return parsed
            .filter { it.name.isNotBlank() && it.name == cleanName(it.name) && it.values.length <= 400 }
            .distinctBy { it.name.lowercase() }
            .take(MAX_OWN)
    }

    /** Unter diesem Schlüssel werden die eigenen Voreinstellungen einer Teigart gemerkt. */
    fun key(type: DoughType): String = "presets_${type.key}"
}
