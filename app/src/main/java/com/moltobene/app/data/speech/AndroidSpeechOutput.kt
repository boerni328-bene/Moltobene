package com.moltobene.app.data.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Vorlesen mit der Sprachausgabe von Android (#79). Alle Aufrufe kommen vom Hauptthread. Es werden nur Stimmen genommen,
 * die ohne Internet funktionieren und zur Sprache des Rezepts passen; gibt es keine, bleibt es bei einer ehrlichen
 * Meldung ([SpeechState.NoVoice]). Während des Vorlesens hat die App den Audiofokus und hält an, wenn ihn jemand
 * anderes braucht (Anruf, Wecker). Die Sprachausgabe wird erst beim ersten Vorlesen gestartet und bei [release] beendet.
 */
class AndroidSpeechOutput(context: Context) : SpeechOutput {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    override val state: StateFlow<SpeechState> = mutableState.asStateFlow()

    private var engine: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, List<String>>? = null
    private var token = 0
    private var lastUtterance: String? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun speak(language: String, sentences: List<String>) {
        stopSpeaking()
        if (sentences.isEmpty()) return
        mutableState.value = SpeechState.Speaking
        if (ready) {
            play(language, sentences)
        } else {
            pending = language to sentences
            startEngine()
        }
    }

    override fun stop() {
        stopSpeaking()
        mutableState.value = SpeechState.Idle
    }

    override fun release() {
        stop()
        pending = null
        engine?.shutdown()
        engine = null
        ready = false
    }

    private fun startEngine() {
        if (engine != null) return
        engine = TextToSpeech(appContext) { status -> main.post { onEngineReady(status) } }
    }

    private fun onEngineReady(status: Int) {
        val current = engine
        if (current == null) return
        if (status != TextToSpeech.SUCCESS) {
            current.shutdown()
            engine = null
            pending = null
            mutableState.value = SpeechState.Unavailable
            return
        }
        ready = true
        current.setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
        )
        current.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                main.post { if (utteranceId != null && utteranceId == lastUtterance) finish() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { if (utteranceId != null && utteranceId.startsWith("$token:")) finish() }
            }
        })
        pending?.let { (language, sentences) ->
            pending = null
            play(language, sentences)
        }
    }

    private fun play(language: String, sentences: List<String>) {
        val current = engine ?: return
        val voice = chooseVoice(current, language)
        if (voice == null) {
            mutableState.value = SpeechState.NoVoice(language)
            return
        }
        if (current.setVoice(voice) == TextToSpeech.ERROR) {
            mutableState.value = SpeechState.NoVoice(language)
            return
        }
        requestFocus()
        token++
        sentences.forEachIndexed { index, sentence ->
            val id = "$token:$index"
            if (index == sentences.lastIndex) lastUtterance = id
            current.speak(sentence, TextToSpeech.QUEUE_ADD, null, id)
        }
    }

    /** Die beste Stimme für [language], die ohne Internet funktioniert und installiert ist; sonst null. */
    private fun chooseVoice(current: TextToSpeech, language: String): Voice? {
        val device = Locale.getDefault()
        return runCatching { current.voices.orEmpty() }.getOrDefault(emptySet())
            .filter { voice ->
                voice.locale.language.equals(language, ignoreCase = true) &&
                    !voice.isNetworkConnectionRequired &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
            }
            .maxWithOrNull(
                compareBy<Voice>({ it.locale.country.equals(device.country, ignoreCase = true) }, { it.quality }),
            )
    }

    private fun stopSpeaking() {
        token++
        lastUtterance = null
        pending = null
        engine?.takeIf { ready }?.stop()
        abandonFocus()
    }

    private fun finish() {
        abandonFocus()
        lastUtterance = null
        mutableState.value = SpeechState.Idle
    }

    private fun requestFocus() {
        val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            )
            .setOnAudioFocusChangeListener { change ->
                // Anruf, Wecker o. Ä.: sofort anhalten und nicht von selbst weiterlesen.
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) main.post { stop() }
            }
            .build()
        focusRequest = request
        manager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        val request = focusRequest ?: return
        focusRequest = null
        (appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.abandonAudioFocusRequest(request)
    }
}
