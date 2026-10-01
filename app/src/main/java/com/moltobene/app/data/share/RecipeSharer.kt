package com.moltobene.app.data.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.moltobene.app.data.Recipe
import com.moltobene.app.data.photos.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Was an das Android-Teilen-Menü übergeben wird. */
data class PreparedShare(
    val mimeType: String,
    val subject: String,
    val text: String?,
    val fileUri: Uri?,
)

/**
 * Bereitet ein Rezept zum Teilen vor. Dateien entstehen in cache/share/ (nur dieser Ordner ist für
 * andere Apps freigegeben) und werden nach einem Tag wieder gelöscht. Fotos werden dabei neu
 * gespeichert, damit garantiert keine Zusatzdaten wie der Aufnahmeort mitgehen.
 */
class RecipeSharer(private val context: Context, private val photoStore: PhotoStore) {

    private val shareRoot: File get() = File(context.cacheDir, SHARE_DIR)

    /** Text mit Foto (falls vorhanden), gut lesbar z. B. in WhatsApp oder per E-Mail. */
    suspend fun prepareText(recipe: Recipe, labels: ShareLabels): PreparedShare = withContext(Dispatchers.IO) {
        val folder = newFolder()
        val subject = recipe.title.trim().ifEmpty { labels.untitled }
        val photoUri = recipe.photoIds.firstOrNull()
            ?.let { photoStore.encodeForSharing(it) }
            ?.let { jpeg ->
                val file = File(folder, "${ShareFiles.baseName(recipe.title, labels.untitled)}.jpg")
                file.writeBytes(jpeg)
                uriFor(file)
            }
        PreparedShare(
            mimeType = if (photoUri != null) "image/jpeg" else "text/plain",
            subject = subject,
            text = RecipeShareText.format(recipe, labels),
            fileUri = photoUri,
        )
    }

    /** Rezeptdatei im Standard schema.org/Recipe, mit eingebettetem Foto. */
    suspend fun prepareFile(recipe: Recipe, untitled: String): PreparedShare = withContext(Dispatchers.IO) {
        val folder = newFolder()
        val jpeg = recipe.photoIds.firstOrNull()?.let { photoStore.encodeForSharing(it) }
        val file = File(folder, "${ShareFiles.baseName(recipe.title, untitled)}.${RecipeJsonLd.FILE_EXTENSION}")
        file.writeText(RecipeJsonLd.build(recipe, untitled, jpeg))
        PreparedShare(
            mimeType = RecipeJsonLd.MIME_TYPE,
            subject = recipe.title.trim().ifEmpty { untitled },
            text = null,
            fileUri = uriFor(file),
        )
    }

    /** Jedes Teilen bekommt einen eigenen Ordner, damit eine noch laufende Übergabe nicht gestört wird. */
    private fun newFolder(): File {
        val cutoff = System.currentTimeMillis() - KEEP_MILLIS
        shareRoot.listFiles().orEmpty().filter { it.lastModified() < cutoff }.forEach { it.deleteRecursively() }
        return File(shareRoot, UUID.randomUUID().toString()).apply { mkdirs() }
    }

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private companion object {
        const val SHARE_DIR = "share"
        const val KEEP_MILLIS = 24L * 60 * 60 * 1000
    }
}
