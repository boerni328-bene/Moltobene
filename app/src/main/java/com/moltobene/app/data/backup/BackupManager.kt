package com.moltobene.app.data.backup

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.moltobene.app.BuildConfig
import com.moltobene.app.data.RecipeRepository
import com.moltobene.app.data.StorageFull
import com.moltobene.app.data.photos.PhotoStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Sichern und Wiederherstellen der ganzen Sammlung über eine Datei (Issue #29). */
class BackupManager(
    private val context: Context,
    private val repository: RecipeRepository,
    private val photoStore: PhotoStore,
) {

    /** Vorschau vor dem Wiederherstellen – es ist noch nichts verändert. */
    class RestorePreview internal constructor(
        val backupCreatedAt: Long,
        val total: Int,
        val newCount: Int,
        val existingCount: Int,
        val skipped: Int,
        internal val recipes: List<BackupRecipe>,
        internal val photoDir: File,
    )

    sealed interface PrepareResult {
        data class Ready(val preview: RestorePreview) : PrepareResult
        data class Failed(val problem: BackupReader.Problem) : PrepareResult
    }

    data class RestoreResult(val added: Int, val replaced: Int, val unchanged: Int)

    private val restoreDir: File get() = File(context.cacheDir, "restore")

    /** Sichern ist fehlgeschlagen; [problem] sagt warum, damit die Meldung den nächsten Schritt nennen kann. */
    class BackupException(val problem: Problem, cause: Throwable? = null) : IOException(problem.name, cause) {
        enum class Problem {
            /** Am Speicherort ist kein Platz mehr. */
            NO_SPACE,

            /** Die Datei ließ sich nach dem Schreiben nicht vollständig lesen. */
            INCOMPLETE,

            /** Sonstiger Fehler beim Schreiben. */
            FAILED,
        }
    }

    /**
     * Schreibt alle Rezepte mit Fotos in die gewählte Datei und prüft sie danach (#51): Erst wenn sie sich vollständig
     * lesen lässt, gilt die Sammlung als gesichert. Sonst wird die unvollständige Datei entfernt, damit keine
     * Sicherung mit gültigem Namen liegen bleibt, die sich später nicht wiederherstellen lässt.
     * @return Anzahl der Rezepte
     * @throws BackupException
     */
    suspend fun export(uri: Uri): Int = withContext(Dispatchers.IO) {
        val recipes = repository.getAll()
        val photos = recipes.flatMap { it.photoIds + it.pageIds }.distinct()
            .associateWith { photoStore.photoFile(it) to photoStore.thumbFile(it) }
        val manifest = BackupManifest(
            appVersion = BuildConfig.VERSION_NAME,
            createdAt = System.currentTimeMillis(),
            recipeCount = recipes.size,
        )
        try {
            val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Datei nicht beschreibbar")
            val entries = out.use { BackupWriter.write(it, manifest, recipes.map { recipe -> recipe.toBackup() }, photos) }
            try {
                check(uri, entries, recipes.size)
            } catch (e: IOException) {
                if (StorageFull.isCause(e)) throw e
                throw BackupException(BackupException.Problem.INCOMPLETE, e)
            }
        } catch (e: CancellationException) {
            deleteQuietly(uri)
            throw e
        } catch (e: Exception) {
            deleteQuietly(uri)
            throw when {
                e is BackupException -> e
                StorageFull.isCause(e) -> BackupException(BackupException.Problem.NO_SPACE, e)
                else -> BackupException(BackupException.Problem.FAILED, e)
            }
        }
        recipes.size
    }

    /**
     * Liest die fertige Datei: das Inhaltsverzeichnis am Ende und die Rezeptliste am Anfang, ohne die Fotos noch
     * einmal auszupacken. Lässt sich am Speicherort nicht springen (z. B. bei manchen Cloud-Speichern), wird die
     * Datei einmal ganz gelesen.
     */
    private fun check(uri: Uri, entries: Int, recipes: Int) {
        val resolver = context.contentResolver
        val names = try {
            resolver.openFileDescriptor(uri, "r")?.let { descriptor ->
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
                    val channel = stream.channel
                    if (channel.size() > 0) BackupCheck.entryNames(channel) else null
                }
            }
        } catch (e: BackupCheck.IncompleteException) {
            throw e
        } catch (e: IOException) {
            null
        } catch (e: UnsupportedOperationException) {
            null
        }
        val input = { resolver.openInputStream(uri) ?: throw IOException("Datei nicht lesbar") }
        if (names != null) {
            val count = input().use { BackupCheck.recipeCount(it) }
            BackupCheck.verify(names.size, count, entries, recipes)
        } else {
            val (found, count) = input().use { BackupCheck.streamingCheck(it) }
            BackupCheck.verify(found, count, entries, recipes)
        }
    }

    private fun deleteQuietly(uri: Uri) {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
    }

    /** Liest und prüft die Sicherung vollständig und erstellt eine Vorschau. */
    suspend fun prepareRestore(uri: Uri): PrepareResult = withContext(Dispatchers.IO) {
        val input = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: IOException) {
            null
        } ?: return@withContext PrepareResult.Failed(BackupReader.Problem.DAMAGED)

        when (val result = input.use { BackupReader().read(it, restoreDir) }) {
            is BackupReader.Result.Failed -> {
                restoreDir.deleteRecursively()
                PrepareResult.Failed(result.problem)
            }
            is BackupReader.Result.Ok -> {
                val existing = repository.updatedAtById()
                val existingCount = result.recipes.count { it.id in existing }
                PrepareResult.Ready(
                    RestorePreview(
                        backupCreatedAt = result.manifest.createdAt,
                        total = result.recipes.size,
                        newCount = result.recipes.size - existingCount,
                        existingCount = existingCount,
                        skipped = result.skippedRecipes,
                        recipes = result.recipes,
                        photoDir = result.photoDir,
                    )
                )
            }
        }
    }

    /**
     * Übernimmt die Rezepte: neue werden hinzugefügt, vorhandene nur ersetzt, wenn die Sicherung neuer ist.
     * Es wird nichts gelöscht. Schlägt etwas fehl, bleibt die Sammlung unverändert (alles oder nichts).
     */
    suspend fun applyRestore(preview: RestorePreview): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val existing = repository.updatedAtById()
            val toWrite = preview.recipes.filter { backup ->
                val current = existing[backup.id]
                current == null || backup.updatedAt > current
            }
            val replacedIds = toWrite.map { it.id }.filter { it in existing }.toSet()
            val oldPhotoIds = replacedIds.flatMap { id ->
                repository.getRecipe(id)?.let { it.photoIds + it.pageIds }.orEmpty()
            }

            val adopted = mutableListOf<String>()
            /** Übernimmt die Fotos, die vorhanden sind; fehlende werden übergangen. */
            suspend fun adoptAvailable(photoIds: List<String>): List<String> {
                val available = photoIds.filter { photoId ->
                    photoStore.exists(photoId) || File(preview.photoDir, "$photoId.jpg").isFile
                }
                available.forEach { photoId ->
                    if (!photoStore.exists(photoId)) {
                        photoStore.adoptFromBackup(
                            photoId = photoId,
                            full = File(preview.photoDir, "$photoId.jpg"),
                            thumb = File(preview.photoDir, "${photoId}_thumb.jpg"),
                        )
                        adopted += photoId
                    }
                }
                return available
            }
            try {
                val recipes = toWrite.map { backup ->
                    backup.copy(photos = adoptAvailable(backup.photos), pages = adoptAvailable(backup.pages)).toRecipe()
                }
                repository.saveAll(recipes)

                val newPhotoIds = recipes.flatMap { it.photoIds + it.pageIds }.toSet()
                oldPhotoIds.filter { it !in newPhotoIds }.forEach { photoStore.delete(it) }
            } catch (e: Exception) {
                adopted.forEach { photoStore.delete(it) }
                throw e
            }

            RestoreResult(
                added = toWrite.size - replacedIds.size,
                replaced = replacedIds.size,
                unchanged = preview.recipes.size - toWrite.size,
            )
        } finally {
            preview.photoDir.deleteRecursively()
        }
    }

    fun discardRestore(preview: RestorePreview) {
        preview.photoDir.deleteRecursively()
    }
}
