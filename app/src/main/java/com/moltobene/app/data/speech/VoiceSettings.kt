package com.moltobene.app.data.speech

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Eine Stimme der Sprachausgabe, so wie das Handy sie meldet (#85). Die Sprachausgabe von Android sagt weder Geschlecht
 * noch Charakter einer Stimme; bekannt sind nur Name, Sprache, Region, Güte und ob Internet nötig ist.
 */
data class VoiceInfo(
    val name: String,
    val language: String,
    val country: String,
    val quality: Int,
    val networkRequired: Boolean,
    val installed: Boolean = true,
)

/** Das Ergebnis der Stimmenwahl: [voice], und ob statt der gewünschten Stimme eine andere genommen wurde. */
data class VoiceChoice(val voice: VoiceInfo, val fellBack: Boolean)

/**
 * Stimmenwahl für das Vorlesen (#79, #85): nur Stimmen der richtigen Sprache, die installiert sind und ohne Internet
 * funktionieren – nie eine Stimme in falscher Sprache. Reines Kotlin, ohne Handy prüfbar.
 */
object VoiceSelection {

    /** Alle Stimmen für [language], die offline und installiert sind; die passendste zuerst. */
    fun usable(all: Collection<VoiceInfo>, language: String, deviceCountry: String = ""): List<VoiceInfo> = all
        .filter { it.language.equals(language, ignoreCase = true) && !it.networkRequired && it.installed }
        .sortedWith(
            compareByDescending<VoiceInfo> { it.country.equals(deviceCountry, ignoreCase = true) && deviceCountry.isNotEmpty() }
                .thenByDescending { it.quality }
                .thenBy { it.country }
                .thenBy { it.name },
        )

    /**
     * Die Stimme zum Vorlesen: die gewünschte ([preferredName]), wenn es sie noch gibt; sonst die beste passende, und
     * [VoiceChoice.fellBack] sagt dann, dass die App darauf hinweisen soll. Gibt es keine, null.
     */
    fun choose(all: Collection<VoiceInfo>, language: String, preferredName: String?, deviceCountry: String = ""): VoiceChoice? {
        val list = usable(all, language, deviceCountry)
        if (list.isEmpty()) return null
        val wanted = preferredName?.takeIf { it.isNotBlank() }?.let { name -> list.firstOrNull { it.name == name } }
        if (wanted != null) return VoiceChoice(wanted, fellBack = false)
        return VoiceChoice(list.first(), fellBack = !preferredName.isNullOrBlank())
    }
}

/** Voreinstellungen für Tempo und Tonhöhe beim Vorlesen (#85). Kennungen sind fest und werden nicht übersetzt. */
enum class SpeechPreset(val key: String, val rate: Int, val pitch: Int) {
    RELAXED("relaxed", rate = 85, pitch = 90),
    NORMAL("normal", rate = 100, pitch = 100),
    LIVELY("lively", rate = 115, pitch = 110),
}

/**
 * Einstellungen des Vorlesens (#85): die gewählte Stimme je Sprache (leer = beste passende), Tempo und Tonhöhe in Prozent
 * von „normal“. Nur auf dem Handy gemerkt, nicht in der Sicherung.
 */
@Serializable
data class SpeechSettings(
    val voices: Map<String, String> = emptyMap(),
    val rate: Int = 100,
    val pitch: Int = 100,
    /** Kopfhörertasten beim Kochen (#79); an, bis jemand sie abschaltet. */
    val headsetButtons: Boolean = true,
) {
    fun voiceFor(language: String): String? = voices[language.lowercase()]

    fun withVoice(language: String, name: String?): SpeechSettings {
        val key = language.lowercase()
        return copy(voices = if (name.isNullOrBlank()) voices - key else voices + (key to name))
    }

    fun withRate(value: Int) = copy(rate = value.coerceIn(RATE_MIN, RATE_MAX))

    fun withPitch(value: Int) = copy(pitch = value.coerceIn(PITCH_MIN, PITCH_MAX))

    fun withHeadsetButtons(on: Boolean) = copy(headsetButtons = on)

    fun withPreset(preset: SpeechPreset) = copy(rate = preset.rate, pitch = preset.pitch)

    /** Die Voreinstellung, die zu Tempo und Tonhöhe passt; null bei eigenen Werten. */
    fun preset(): SpeechPreset? = SpeechPreset.entries.firstOrNull { it.rate == rate && it.pitch == pitch }

    /** Tempo ein Stück weiter ([direction] +1) oder zurück (−1), innerhalb der Grenzen. */
    fun steppedRate(direction: Int) = withRate(rate + direction.coerceIn(-1, 1) * STEP)

    fun steppedPitch(direction: Int) = withPitch(pitch + direction.coerceIn(-1, 1) * STEP)

    companion object {
        const val RATE_MIN = 60
        const val RATE_MAX = 150
        const val PITCH_MIN = 70
        const val PITCH_MAX = 130
        const val STEP = 5

        /** Mehr als zwei Sprachen und längere Namen kommen nicht vor; alles darüber wird verworfen. */
        private const val MAX_NAME_LENGTH = 120
        private val LANGUAGES = setOf("de", "en")
        private val json = Json { ignoreUnknownKeys = true }

        fun encode(settings: SpeechSettings): String = json.encodeToString(serializer(), settings)

        /** Liest gemerkte Einstellungen; Unlesbares ergibt die Standardwerte, einzelne Werte werden auf die Grenzen gesetzt. */
        fun decode(text: String?): SpeechSettings {
            if (text.isNullOrBlank()) return SpeechSettings()
            val parsed = runCatching { json.decodeFromString(serializer(), text) }.getOrElse { return SpeechSettings() }
            return SpeechSettings(
                voices = parsed.voices
                    .filter { (language, name) -> language in LANGUAGES && name.isNotBlank() && name.length <= MAX_NAME_LENGTH }
                    .filterNot { (_, name) -> name.any { it.isISOControl() } },
                rate = parsed.rate.coerceIn(RATE_MIN, RATE_MAX),
                pitch = parsed.pitch.coerceIn(PITCH_MIN, PITCH_MAX),
                headsetButtons = parsed.headsetButtons,
            )
        }
    }
}
