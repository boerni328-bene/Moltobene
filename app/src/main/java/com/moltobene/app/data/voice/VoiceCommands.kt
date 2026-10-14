package com.moltobene.app.data.voice

/** Was die Spracheingabe in der Kochansicht auslösen kann (#80). */
enum class VoiceCommand {
    NEXT,
    PREVIOUS,
    REPEAT,
    READ,
    STOP,

    /** Mikrofon ausschalten („Mikro aus“, „Mikrofon aus“, „ausschalten“, „Ende“, „beenden“, „fertig“). */
    MICROPHONE_OFF,
}

/**
 * Die Befehle der Spracheingabe (#80) auf Deutsch und ihre Zuordnung. Die Erkennung kennt nur diese Wörter
 * ([GERMAN_GRAMMAR]); alles andere wird als „[unk]“ verworfen. Reines Kotlin, per Unit-Test prüfbar.
 *
 * Die Wörter stehen hier und nicht in `strings.xml`: Sie hängen an der Sprache des Erkennungspakets, nicht an der
 * Oberfläche (wie die gesprochenen Wörter beim Vorlesen).
 */
object VoiceCommands {
    /** Sprechweise → Befehl, genau so, wie sie in der Grammatik der Erkennung steht. */
    private val GERMAN: Map<String, VoiceCommand> = linkedMapOf(
        "weiter" to VoiceCommand.NEXT,
        "zurück" to VoiceCommand.PREVIOUS,
        "wiederholen" to VoiceCommand.REPEAT,
        "vorlesen" to VoiceCommand.READ,
        "stopp" to VoiceCommand.STOP,
        "mikro aus" to VoiceCommand.MICROPHONE_OFF,
        "mikrofon aus" to VoiceCommand.MICROPHONE_OFF,
        "ausschalten" to VoiceCommand.MICROPHONE_OFF,
        "ende" to VoiceCommand.MICROPHONE_OFF,
        "beenden" to VoiceCommand.MICROPHONE_OFF,
        "fertig" to VoiceCommand.MICROPHONE_OFF,
    )

    /** Der Platzhalter der Erkennung für alles, was kein Befehl ist. */
    private const val UNKNOWN = "[unk]"

    /** Grammatik für die Erkennung als JSON-Liste, z. B. `["weiter", "zurück", …, "[unk]"]`. */
    val GERMAN_GRAMMAR: String = (GERMAN.keys + UNKNOWN).joinToString(prefix = "[", postfix = "]") { "\"$it\"" }

    /**
     * Der erste Befehl im erkannten Text; null, wenn keiner dabei ist. Mehrere Befehle in einem Satz („weiter weiter“)
     * zählen als einer, damit ein Wort, das die Erkennung doppelt hört, nicht zwei Schritte weiterschaltet. Längere
     * Wendungen („mikrofon aus“) haben Vorrang vor Teilwörtern.
     */
    fun parse(text: String?): VoiceCommand? {
        val normalized = text?.trim()?.lowercase()?.replace(Regex("\\s+"), " ").orEmpty()
        if (normalized.isEmpty()) return null
        // Vom Anfang her: das erste Vorkommen, bei gleicher Stelle die längere Wendung.
        return GERMAN.entries
            .mapNotNull { (phrase, command) ->
                val at = Regex("(^| )${Regex.escape(phrase)}( |$)").find(normalized)?.range?.first
                if (at == null) null else Triple(at, phrase.length, command)
            }
            .minWithOrNull(compareBy<Triple<Int, Int, VoiceCommand>> { it.first }.thenByDescending { it.second })
            ?.third
    }
}
