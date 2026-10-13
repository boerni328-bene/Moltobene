package com.moltobene.app.data.speech

import android.content.Context
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
    }

    /** Sitzung beenden; die Tasten gehören wieder der Musik-App. */
    fun deactivate() {
        val current = session ?: return
        session = null
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
        const val ACTIONS = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
    }
}
