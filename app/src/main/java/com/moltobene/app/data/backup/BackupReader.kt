package com.moltobene.app.data.backup

import com.moltobene.app.data.RecipeIds
import com.moltobene.app.data.StorageFull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/**
 * Liest und prüft eine Sicherungsdatei, bevor irgendetwas in die Sammlung übernommen wird.
 * Sicherheitsregeln (Issue #29): Dateinamen werden nie aus der Datei übernommen, sondern aus
 * der geprüften Foto-Kennung gebildet (kein Schreiben außerhalb des Zielordners); Größen und
 * Anzahl der Einträge sind begrenzt.
 */
class BackupReader(private val limits: Limits = Limits()) {

    data class Limits(
        val maxEntries: Int = 20_000,
        val maxJsonBytes: Long = 50L * 1024 * 1024,
        val maxPhotoBytes: Long = 25L * 1024 * 1024,
        val maxTotalBytes: Long = 4L * 1024 * 1024 * 1024,
    )

    /** [NO_SPACE]: Beim Auspacken war auf dem Handy kein Platz mehr (#51) – die Datei selbst ist in Ordnung. */
    enum class Problem { NOT_A_BACKUP, NEWER_VERSION, DAMAGED, TOO_LARGE, NO_SPACE }

    sealed interface Result {
        /** [photoDir] enthält die ausgepackten Fotos als <id>.jpg und <id>_thumb.jpg. */
        data class Ok(
            val manifest: BackupManifest,
            val recipes: List<BackupRecipe>,
            val skippedRecipes: Int,
            val photoDir: File,
        ) : Result

        data class Failed(val problem: Problem) : Result
    }

    /** Packt die Sicherung nach [extractDir] aus (der Ordner wird vorher geleert) und prüft sie. */
    fun read(input: InputStream, extractDir: File): Result {
        extractDir.deleteRecursively()
        extractDir.mkdirs()
        var manifestText: String? = null
        var recipesText: String? = null
        var entries = 0
        var total = 0L

        try {
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries++
                    if (entries > limits.maxEntries) return Result.Failed(Problem.TOO_LARGE)
                    val name = entry.name
                    val written: Long = when {
                        entry.isDirectory -> 0L
                        name == BackupFormat.MANIFEST_ENTRY -> {
                            val bytes = readLimited(zip, limits.maxJsonBytes) ?: return Result.Failed(Problem.TOO_LARGE)
                            manifestText = bytes.toString(Charsets.UTF_8)
                            bytes.size.toLong()
                        }
                        name == BackupFormat.RECIPES_ENTRY -> {
                            val bytes = readLimited(zip, limits.maxJsonBytes) ?: return Result.Failed(Problem.TOO_LARGE)
                            recipesText = bytes.toString(Charsets.UTF_8)
                            bytes.size.toLong()
                        }
                        else -> {
                            val target = photoTarget(name, extractDir)
                            if (target == null) {
                                0L // unbekannte Einträge (z. B. aus neueren Versionen) werden übersprungen
                            } else {
                                target.outputStream().use { copyLimited(zip, it, limits.maxPhotoBytes) }
                                    ?: return Result.Failed(Problem.TOO_LARGE)
                            }
                        }
                    }
                    total += written
                    if (total > limits.maxTotalBytes) return Result.Failed(Problem.TOO_LARGE)
                }
            }
        } catch (e: ZipException) {
            return Result.Failed(Problem.NOT_A_BACKUP)
        } catch (e: IOException) {
            return Result.Failed(if (StorageFull.isCause(e)) Problem.NO_SPACE else Problem.DAMAGED)
        }

        val manifest = manifestText?.let {
            runCatching { BackupFormat.json.decodeFromString(BackupManifest.serializer(), it) }.getOrNull()
        } ?: return Result.Failed(Problem.NOT_A_BACKUP)
        if (manifest.format != BackupFormat.FORMAT_ID) return Result.Failed(Problem.NOT_A_BACKUP)
        if (manifest.formatVersion > BackupFormat.CURRENT_FORMAT_VERSION) return Result.Failed(Problem.NEWER_VERSION)

        val recipeElements = recipesText?.let {
            runCatching { BackupFormat.json.parseToJsonElement(it).jsonObject["recipes"]?.jsonArray }.getOrNull()
        } ?: return Result.Failed(Problem.DAMAGED)

        val (recipes, skipped) = decodeRecipes(recipeElements)
        return Result.Ok(manifest = manifest, recipes = recipes, skippedRecipes = skipped, photoDir = extractDir)
    }

    /** Jedes Rezept einzeln lesen: Ein beschädigtes Rezept wird übersprungen, statt alles scheitern zu lassen. */
    private fun decodeRecipes(elements: JsonArray): Pair<List<BackupRecipe>, Int> {
        var skipped = 0
        val recipes = elements.mapNotNull { element ->
            val recipe = runCatching { BackupFormat.json.decodeFromJsonElement(BackupRecipe.serializer(), element) }.getOrNull()
            if (recipe == null || !RecipeIds.isValid(recipe.id)) {
                skipped++
                null
            } else {
                recipe.copy(
                    photos = recipe.photos.filter { RecipeIds.isValid(it) },
                    pages = recipe.pages.filter { RecipeIds.isValid(it) },
                )
            }
        }
        val unique = recipes.distinctBy { it.id }
        return unique to (skipped + recipes.size - unique.size)
    }

    private fun readLimited(input: InputStream, max: Long): ByteArray? {
        val buffer = java.io.ByteArrayOutputStream()
        return if (copyLimited(input, buffer, max) == null) null else buffer.toByteArray()
    }

    /** Kopiert höchstens [max] Bytes; liefert null, wenn es mehr wären. */
    private fun copyLimited(input: InputStream, out: OutputStream, max: Long): Long? {
        val buffer = ByteArray(64 * 1024)
        var count = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            count += read
            if (count > max) return null
            out.write(buffer, 0, read)
        }
        return count
    }

    companion object {
        private val PHOTO_ENTRY = Regex("photos/([0-9a-fA-F-]{36})(_thumb)?\\.jpg")

        /** Zieldatei für einen Foto-Eintrag – oder null, wenn der Name nicht dem erwarteten Muster entspricht. */
        fun photoTarget(entryName: String, dir: File): File? {
            val match = PHOTO_ENTRY.matchEntire(entryName) ?: return null
            val photoId = match.groupValues[1]
            if (!RecipeIds.isValid(photoId)) return null
            val suffix = if (match.groupValues[2].isNotEmpty()) "_thumb" else ""
            return File(dir, "$photoId$suffix.jpg")
        }
    }
}
