package com.moltobene.app.data.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
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
class AndroidSpeechOutput(
    context: Context,
    private val settings: () -> SpeechSettings = { SpeechSettings() },
) : SpeechOutput {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    override val state: StateFlow<SpeechState> = mutableState.asStateFlow()

    private var engine: TextToSpeech? = null
    private var ready = false
    private var pending: Triple<String, List<String>, SpeechSettings?>? = null
    private var readyWaiters = mutableListOf<CompletableDeferred<Boolean>>()
    private var token = 0
    private var lastUtterance: String? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun speak(language: String, sentences: List<String>, settings: SpeechSettings?) {
        stopSpeaking()
        if (sentences.isEmpty()) return
        mutableState.value = SpeechState.Speaking()
        if (ready) {
            play(language, sentences, settings)
        } else {
            pending = Triple(language, sentences, settings)
            startEngine()
        }
    }

    override suspend fun voices(language: String): List<VoiceInfo>? {
        if (!ready) {
            val waiter = CompletableDeferred<Boolean>()
            readyWaiters += waiter
            startEngine()
            if (!waiter.await()) return null
        }
        val current = engine ?: return null
        return VoiceSelection.usable(infos(current), language, Locale.getDefault().country)
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
        readyWaiters.forEach { it.complete(false) }
        readyWaiters.clear()
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
            readyWaiters.forEach { it.complete(false) }
            readyWaiters.clear()
            return
        }
        ready = true
        readyWaiters.forEach { it.complete(true) }
        readyWaiters.clear()
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
        pending?.let { (language, sentences, override) ->
            pending = null
            play(language, sentences, override)
        }
    }

    private fun play(language: String, sentences: List<String>, override: SpeechSettings?) {
        val current = engine ?: return
        val chosen = override ?: settings()
        val choice = VoiceSelection.choose(infos(current), language, chosen.voiceFor(language), Locale.getDefault().country)
        val voice = choice?.let { picked -> current.voices.orEmpty().firstOrNull { it.name == picked.voice.name } }
        if (voice == null) {
            mutableState.value = SpeechState.NoVoice(language)
            return
        }
        if (current.setVoice(voice) == TextToSpeech.ERROR) {
            mutableState.value = SpeechState.NoVoice(language)
            return
        }
        current.setSpeechRate(chosen.rate / 100f)
        current.setPitch(chosen.pitch / 100f)
        if (choice.fellBack) mutableState.value = SpeechState.Speaking(usedFallback = true)
        requestFocus()
        token++
        sentences.forEachIndexed { index, sentence ->
            val id = "$token:$index"
            if (index == sentences.lastIndex) lastUtterance = id
            current.speak(sentence, TextToSpeech.QUEUE_ADD, null, id)
        }
    }

    /** Alle Stimmen der Sprachausgabe als [VoiceInfo]; ob sie ohne Internet gehen, melden die Stimmen selbst. */
    private fun infos(current: TextToSpeech): List<VoiceInfo> =
        runCatching { current.voices.orEmpty() }.getOrDefault(emptySet()).map { voice ->
            VoiceInfo(
                name = voice.name,
                language = voice.locale.language,
                country = voice.locale.country,
                quality = voice.quality,
                networkRequired = voice.isNetworkConnectionRequired,
                installed = TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty(),
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
