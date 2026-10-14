package com.moltobene.app.data.voice

import com.moltobene.app.data.translate.LanguagePackInstaller
import com.moltobene.app.data.translate.LanguagePackManager
import com.moltobene.app.data.translate.LanguagePackManager.Problem
import com.moltobene.app.data.translate.LanguagePackManager.State
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
 * Erkennungspaket für die Spracheingabe (#80): einmal auf Wunsch herunterladen, prüfen, löschen – genau wie das
 * Sprachpaket für die Übersetzung ([LanguagePackManager], von dem Zustände und Probleme übernommen sind). Es liegt in
 * `noBackupFilesDir`, kommt also nicht in Sicherungen und nicht in den Umzug auf ein neues Handy.
 *
 * @param baseDir Verzeichnis für das Paket (`noBackupFilesDir`)
 * @param downloader die einzige Stelle für Internetzugriffe (data/web)
 * @param scope läuft so lange wie die App
 */
class VoicePackManager(
    private val baseDir: File,
    private val downloader: () -> FileDownloader,
    private val scope: CoroutineScope,
    private val manifestSha256: String = VoicePack.MANIFEST_SHA256,
    private val url: String = VoicePack.URL,
) {
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
            if (VoicePack.isComplete(directory)) State.Installed(LanguagePackManager.sizeOf(directory)) else State.Missing
        }
    }

    fun download() {
        if (job?.isActive == true) return
        mutableState.value = State.Downloading(null)
        job = scope.launch(Dispatchers.IO) {
            mutableState.value = try {
                if (baseDir.usableSpace < VoicePack.INSTALLED_BYTES + SPACE_RESERVE) throw LanguagePackInstaller.InstallException(LanguagePackInstaller.Problem.NO_SPACE)
                downloader().download(url, VoicePack.MAX_DOWNLOAD_BYTES) { input, length ->
                    val counted = CountingInputStream(input) { read ->
                        if (length > 0) mutableState.value = State.Downloading((read.toFloat() / length).coerceIn(0f, 1f))
                    }
                    LanguagePackInstaller.unpack(
                        counted,
                        partial,
                        manifestSha256,
                        manifestName = VoicePack.MANIFEST,
                        safeName = VoicePack.SAFE_NAME,
                        maxUnpackedBytes = VoicePack.MAX_UNPACKED_BYTES,
                    )
                }
                directory.deleteRecursively()
                if (!partial.renameTo(directory)) throw LanguagePackInstaller.InstallException(LanguagePackInstaller.Problem.NO_SPACE)
                State.Installed(LanguagePackManager.sizeOf(directory))
            } catch (e: CancellationException) {
                partial.deleteRecursively()
                mutableState.value = State.Missing
                throw e
            } catch (e: LanguagePackInstaller.InstallException) {
                State.Failed(if (e.problem == LanguagePackInstaller.Problem.NO_SPACE) Problem.NO_SPACE else Problem.DAMAGED)
            } catch (e: WebException) {
                State.Failed(LanguagePackManager.problemOf(e.problem))
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

    /** Zählt gelesene Bytes für die Fortschrittsanzeige; meldet höchstens alle 512 KB einmal. */
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
        const val DIRECTORY = "erkennungspaket"
        private const val PARTIAL = "erkennungspaket.teil"

        /** Etwas Platz soll auch danach noch frei bleiben, z. B. für Fotos. */
        private const val SPACE_RESERVE = 100L * 1024 * 1024
        private const val REPORT_STEP = 512L * 1024
    }
}
