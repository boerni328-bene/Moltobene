package com.moltobene.app.data.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.session.MediaSession
import android.media.session.PlaybackState

/** Was die Kopfhörertasten in der Kochansicht auslösen (#79): Wiedergabe/Pause, Weiter, Zurück. */
interface HeadsetActions {
    /** Wiedergabetaste oder einfacher Druck auf die Kopfhörertaste: Vorlesen starten. */
    fun onPlay()

    /** Pause, Stopp oder noch einmal Druck auf die Kopfhörertaste beim Vorlesen: anhalten. */
    fun onPause()

    fun onNext()

    fun onPrevious()
}

/**
 * Kopfhörer- und Medientasten für das Vorlesen (#79): eine Mediensitzung von Android, **nur solange die Kochansicht im
 * Vordergrund ist** – ohne Dienst, ohne Benachrichtigung, ohne Berechtigung. Android meldet Tastendrücke der
 * Kopfhörer, der Lenkradbedienung oder der Smartwatch an die zuletzt aktive Sitzung; ein Doppeldruck auf die
 * Kopfhörertaste ist „Weiter“, ein dreifacher „Zurück“. Solange die Sitzung aktiv ist, steuern die Tasten diese App und
 * keine Musik-App; beim Verlassen der Ansicht oder wenn der Bildschirm ausgeht, ist sie wieder frei.
 */
class HeadsetButtons(context: Context, private val actions: HeadsetActions) {
    private val appContext = context.applicationContext
    private var session: MediaSession? = null
    private var playing = false
    private var silence: AudioTrack? = null

    /** Sitzung anlegen und aktiv schalten; mehrfaches Aufrufen schadet nicht. */
    fun activate() {
        if (session != null) return
        val created = MediaSession(appContext, TAG)
        created.setCallback(
            object : MediaSession.Callback() {
                override fun onPlay() = actions.onPlay()
                override fun onPause() = actions.onPause()
                override fun onStop() = actions.onPause()
                override fun onSkipToNext() = actions.onNext()
                override fun onSkipToPrevious() = actions.onPrevious()
            },
        )
        session = created
        publishState()
        created.isActive = true
        startSilence()
    }

    /** Sitzung beenden; die Tasten gehören wieder der Musik-App. */
    fun deactivate() {
        val current = session ?: return
        session = null
        stopSilence()
        runCatching {
            current.isActive = false
            current.release()
        }
    }

    /** Meldet Android, ob gerade vorgelesen wird, damit die Wiedergabetaste das Richtige tut. */
    fun update(isPlaying: Boolean) {
        playing = isPlaying
        publishState()
    }

    /**
     * Android leitet Kopfhörertasten nur an Apps weiter, die selbst Ton ausgeben. Das Vorlesen kommt aber aus der
     * Sprachausgabe, nicht aus dieser App; deshalb läuft hier ein stummer Ton (nur Nullen, ohne Lautstärke, ohne
     * Audiofokus), solange die Sitzung aktiv ist.
     */
    private fun startSilence() {
        if (silence != null) return
        runCatching {
            val buffer = ShortArray(SILENCE_FRAMES)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SILENCE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(buffer.size * 2)
                .build()
            track.write(buffer, 0, buffer.size)
            track.setLoopPoints(0, SILENCE_FRAMES, -1)
            track.play()
            silence = track
        }
    }

    private fun stopSilence() {
        val track = silence ?: return
        silence = null
        runCatching {
            track.stop()
            track.release()
        }
    }

    private fun publishState() {
        val current = session ?: return
        val state = PlaybackState.Builder()
            .setActions(ACTIONS)
            .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, 0L, if (playing) 1f else 0f)
            .build()
        current.setPlaybackState(state)
    }

    private companion object {
        const val TAG = "MoltobeneKochen"
        const val SILENCE_RATE = 8000
        const val SILENCE_FRAMES = 8000
        const val ACTIONS = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
    }
}
