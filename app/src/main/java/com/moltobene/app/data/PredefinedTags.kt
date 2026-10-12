package com.moltobene.app.data

import java.text.Collator

/**
 * Mitgelieferte Schlagwörter (#57, „Gruppen sind Schlagwörter“): feste englische Kennung, angezeigt wird der Text aus
 * strings.xml in der Sprache der Oberfläche. Gespeichert wird [Tag.name] = [Tag.predefinedKey] = Kennung; eigene
 * Schlagwörter haben ihren eingegebenen Namen und keine Kennung. „pasta“ (Pastagericht) bleibt getrennt von der Teigart.
 */
object PredefinedTags {
    val KEYS: List<String> = listOf(
        "main_course", "side_dish", "appetizer", "soup", "salad", "sauce", "pasta", "casserole",
        "dessert", "bread", "cake", "pizza", "basics",
    )

    fun tag(key: String): Tag = Tag(name = key, predefinedKey = key)

    fun isKnown(key: String?): Boolean = key != null && key in KEYS

    /**
     * Ein eigenes Schlagwort wird zum mitgelieferten, wenn es dasselbe meint: wie die Kennung („Sauce“ → `sauce`,
     * auch „main course“) oder wie ein angezeigter Name ([labels]: Kennung → Text, in der Sprache der Oberfläche).
     */
    fun resolve(text: String, labels: Map<String, String> = emptyMap()): Tag {
        val name = text.trim().replace(Regex("""\s+"""), " ")
        val normalized = name.lowercase().replace(' ', '_')
        KEYS.firstOrNull { it == normalized }?.let { return tag(it) }
        labels.entries.firstOrNull { it.value.equals(name, ignoreCase = true) }?.let { return tag(it.key) }
        return Tag(name = name)
    }
}

/** Schlagwörter als Text für das Bearbeiten (eines je Zeile): „p:Kennung“ mitgeliefert, „c:Name“ eigen. */
object TagCodec {
    const val MAX_TAGS = 20
    const val MAX_NAME = 30

    fun encode(tags: List<Tag>): String = tags.joinToString("\n") { tag ->
        if (PredefinedTags.isKnown(tag.predefinedKey)) "p:${tag.predefinedKey}" else "c:${tag.name}"
    }

    fun decode(text: String): List<Tag> = text.lines().mapNotNull { line ->
        when {
            line.startsWith("p:") -> line.removePrefix("p:").takeIf(PredefinedTags::isKnown)?.let(PredefinedTags::tag)
            line.startsWith("c:") -> line.removePrefix("c:").trim().takeIf { it.isNotEmpty() }?.let { Tag(name = it) }
            else -> null
        }
    }.distinctBy { it.name.lowercase() }.take(MAX_TAGS)
}

/** Ein Schlagwort mit der Zahl der Rezepte, die es haben. */
data class TagCount(val tag: Tag, val count: Int)

/** Schlagwort-Filter der Sammlung (#57): alle gewählten Schlagwörter zugleich. */
object TagFilter {

    /** Schlagwörter in Gebrauch mit Anzahl: mitgelieferte in fester Reihenfolge, danach eigene A–Z. */
    fun counts(tagsByRecipe: Map<String, List<Tag>>): List<TagCount> {
        val counts = LinkedHashMap<String, Pair<Tag, Int>>()
        for (tags in tagsByRecipe.values) {
            for (tag in tags.distinctBy { it.name }) {
                val current = counts[tag.name]
                counts[tag.name] = (current?.first ?: tag) to ((current?.second ?: 0) + 1)
            }
        }
        val order = PredefinedTags.KEYS
        // Umlaute und Akzente werden einsortiert wie in der Sprache des Handys („Äpfel“ bei A).
        val collator = Collator.getInstance().apply { strength = Collator.SECONDARY }
        return counts.values
            .map { (tag, count) -> TagCount(tag, count) }
            .sortedWith(
                compareBy<TagCount>({ if (PredefinedTags.isKnown(it.tag.predefinedKey)) 0 else 1 }, { order.indexOf(it.tag.predefinedKey) })
                    .thenBy(collator) { it.tag.name },
            )
    }

    /** Ohne Auswahl passt jedes Rezept; sonst muss es alle gewählten Schlagwörter haben. */
    fun matches(recipeTags: List<Tag>, selected: Set<String>): Boolean =
        selected.isEmpty() || recipeTags.mapTo(HashSet()) { it.name }.containsAll(selected)
}
