package com.moltobene.app.data.translate

import com.moltobene.app.data.web.FileDownloader
import com.moltobene.app.data.web.WebException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream

/**
 * Sprachpaket für „Rezept übersetzen“ (#60): einmal auf Wunsch herunterladen, prüfen, löschen. Es liegt in
 * `noBackupFilesDir`, kommt also nicht in Sicherungen und nicht in den Umzug auf ein neues Handy – es lässt sich
 * jederzeit neu laden. Der Download läuft weiter, wenn man die Einstellungen verlässt, solange die App offen ist.
 *
 * @param baseDir Verzeichnis für das Paket (`noBackupFilesDir`)
 * @param downloader die einzige Stelle für Internetzugriffe (data/web)
 * @param scope läuft so lange wie die App
 */
class LanguagePackManager(
    private val baseDir: File,
    private val downloader: () -> FileDownloader,
    private val scope: CoroutineScope,
) {
    sealed interface State {
        /** Es wird noch nachgesehen, ob das Paket da ist. */
        data object Checking : State

        data object Missing : State

        /** @param progress 0 bis 1; null, solange die Größe unbekannt ist */
        data class Downloading(val progress: Float?) : State

        /** @param bytes belegter Platz auf dem Handy */
        data class Installed(val bytes: Long) : State

        data class Failed(val problem: Problem) : State
    }

    enum class Problem {
        /** Kein Internet oder die Verbindung ist abgebrochen. */
        NO_CONNECTION,

        /** Nicht genug Platz auf dem Handy. */
        NO_SPACE,

        /** Das Paket kam nicht vollständig oder verändert an. */
        DAMAGED,

        /** Das Paket ist gerade nicht zu bekommen (z. B. Fehler auf dem Server). */
        UNAVAILABLE,
    }

    /** Verzeichnis des installierten Pakets, je Richtung ein Unterverzeichnis. */
    val directory = File(baseDir, DIRECTORY)
    private val partial = File(baseDir, PARTIAL)

    private val mutableState = MutableStateFlow<State>(State.Checking)
    val state: StateFlow<State> = mutableState.asStateFlow()

    private var job: Job? = null

    /** Liest den Stand vom Speicher; Reste eines abgebrochenen Downloads werden entfernt. */
    suspend fun refresh() {
        if (job?.isActive == true) return
        mutableState.value = withContext(Dispatchers.IO) {
            partial.deleteRecursively()
            if (LanguagePack.isComplete(directory)) State.Installed(sizeOf(directory)) else State.Missing
        }
    }

    fun download() {
        if (job?.isActive == true) return
        mutableState.value = State.Downloading(null)
        job = scope.launch(Dispatchers.IO) {
            mutableState.value = try {
                if (baseDir.usableSpace < LanguagePack.INSTALLED_BYTES + SPACE_RESERVE) throw LanguagePackInstaller.InstallException(LanguagePackInstaller.Problem.NO_SPACE)
                downloader().download(LanguagePack.URL, LanguagePack.MAX_DOWNLOAD_BYTES) { input, length ->
                    val counted = CountingInputStream(input) { read ->
                        if (length > 0) mutableState.value = State.Downloading((read.toFloat() / length).coerceIn(0f, 1f))
                    }
                    LanguagePackInstaller.unpack(counted, partial, LanguagePack.MANIFEST_SHA256)
                }
                directory.deleteRecursively()
                if (!partial.renameTo(directory)) throw LanguagePackInstaller.InstallException(LanguagePackInstaller.Problem.NO_SPACE)
                State.Installed(sizeOf(directory))
            } catch (e: CancellationException) {
                partial.deleteRecursively()
                mutableState.value = State.Missing
                throw e
            } catch (e: LanguagePackInstaller.InstallException) {
                State.Failed(if (e.problem == LanguagePackInstaller.Problem.NO_SPACE) Problem.NO_SPACE else Problem.DAMAGED)
            } catch (e: WebException) {
                State.Failed(problemOf(e.problem))
            } catch (e: Exception) {
                State.Failed(Problem.NO_CONNECTION)
            } finally {
                if (mutableState.value !is State.Installed) partial.deleteRecursively()
            }
        }
    }

    /** Bricht den Download ab; die Verbindung wird sofort getrennt. */
    fun cancel() {
        job?.cancel()
    }

    suspend fun delete() {
        job?.cancelAndJoin()
        withContext(Dispatchers.IO) {
            directory.deleteRecursively()
            partial.deleteRecursively()
        }
        mutableState.value = State.Missing
    }

    /** Zählt gelesene Bytes für die Fortschrittsanzeige; meldet höchstens jedes Prozent einmal. */
    private class CountingInputStream(input: InputStream, private val onProgress: (Long) -> Unit) : FilterInputStream(input) {
        private var total = 0L
        private var reported = 0L

        override fun read(): Int = super.read().also { if (it >= 0) count(1) }

        override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { if (it > 0) count(it) }

        private fun count(read: Int) {
            total += read
            if (total - reported >= REPORT_STEP) {
                reported = total
                onProgress(total)
            }
        }
    }

    companion object {
        const val DIRECTORY = "sprachpaket"
        private const val PARTIAL = "sprachpaket.teil"

        /** Etwas Platz soll auch danach noch frei bleiben, z. B. für Fotos. */
        private const val SPACE_RESERVE = 100L * 1024 * 1024
        private const val REPORT_STEP = 512L * 1024

        internal fun sizeOf(directory: File): Long = directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }

        internal fun problemOf(problem: WebException.Problem): Problem = when (problem) {
            WebException.Problem.NO_CONNECTION, WebException.Problem.TIMEOUT -> Problem.NO_CONNECTION
            WebException.Problem.TOO_LARGE -> Problem.DAMAGED
            else -> Problem.UNAVAILABLE
        }
    }
}
