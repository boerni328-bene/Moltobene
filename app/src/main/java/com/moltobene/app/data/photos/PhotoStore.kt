package com.moltobene.app.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Einzige Stelle, durch die jedes Foto läuft (Issue #31):
 * verkleinert auf ein Detailbild und ein Vorschaubild, richtig gedreht,
 * ohne Zusatzdaten wie den Aufnahmeort (das Neu-Speichern übernimmt keine EXIF-Daten).
 * Fotos liegen im privaten App-Speicher; es sind keine Berechtigungen nötig.
 */
class PhotoStore(private val context: Context) {

    private val photoDir: File get() = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
    private val cameraDir: File get() = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }
    private val cameraFile: File get() = File(cameraDir, "capture.jpg")

    fun photoFile(photoId: String): File = File(photoDir, "$photoId.jpg")

    fun thumbFile(photoId: String): File = File(photoDir, "${photoId}_thumb.jpg")

    fun exists(photoId: String): Boolean = photoFile(photoId).isFile

    /** Adresse, unter der die Kamera-App das Foto ablegt (über FileProvider, ohne Kamera-Berechtigung). */
    fun cameraUri(): Uri {
        cameraFile.delete()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile)
    }

    /** Übernimmt das zuletzt mit der Kamera aufgenommene Foto. */
    suspend fun importCameraPhoto(): String {
        try {
            return importFromUri(Uri.fromFile(cameraFile))
        } finally {
            withContext(Dispatchers.IO) { cameraFile.delete() }
        }
    }

    /** Liest ein Foto ein, verkleinert es und speichert Detail- und Vorschaubild. Liefert die neue Foto-Kennung. */
    suspend fun importFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // Erst nur die Größe lesen (liefert kein Bild, nur die Maße in „bounds“).
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: throw IOException("Foto nicht lesbar")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Kein Foto")

        val orientation = resolver.openInputStream(uri)?.use { stream ->
            runCatching {
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Foto nicht lesbar")

        val rotated = applyOrientation(decoded, orientation)
        val full = scaleDown(rotated, MAX_EDGE)
        val thumb = scaleDown(full, THUMB_EDGE)

        val photoId = UUID.randomUUID().toString()
        try {
            writeJpeg(full, photoFile(photoId))
            writeJpeg(thumb, thumbFile(photoId))
        } catch (e: IOException) {
            delete(photoId)
            throw e
        } finally {
            listOf(decoded, rotated, full, thumb).distinct().forEach { it.recycle() }
        }
        photoId
    }

    /** Kopiert ein Foto aus einer Sicherung (bereits verkleinert) in den Fotospeicher. */
    suspend fun adoptFromBackup(photoId: String, full: File, thumb: File?) = withContext(Dispatchers.IO) {
        copyAtomically(full, photoFile(photoId))
        if (thumb != null && thumb.isFile) {
            copyAtomically(thumb, thumbFile(photoId))
        } else {
            copyAtomically(full, thumbFile(photoId))
        }
    }

    suspend fun delete(photoId: String) = withContext(Dispatchers.IO) {
        photoFile(photoId).delete()
        thumbFile(photoId).delete()
    }

    /**
     * Löscht Fotodateien, die zu keinem gespeicherten Rezept gehören und älter als ein Tag sind
     * (jüngere können noch zu einem Rezept gehören, das gerade bearbeitet wird).
     */
    suspend fun deleteUnused(referenced: Set<String>) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - UNUSED_GRACE_MILLIS
        photoDir.listFiles().orEmpty().forEach { file ->
            val photoId = file.name.removeSuffix(".jpg").removeSuffix("_thumb")
            if (photoId !in referenced && file.lastModified() < cutoff) file.delete()
        }
    }

    private fun writeJpeg(bitmap: Bitmap, target: File) {
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.outputStream().use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) throw IOException("Foto nicht speicherbar")
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("Foto nicht speicherbar")
        }
    }

    private fun copyAtomically(source: File, target: File) {
        val temp = File(target.parentFile, "${target.name}.tmp")
        source.inputStream().use { input -> temp.outputStream().use { input.copyTo(it) } }
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("Foto nicht speicherbar")
        }
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val factor = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * factor).roundToInt().coerceAtLeast(1),
            (bitmap.height * factor).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val PHOTO_DIR = "photos"
        const val CAMERA_DIR = "camera"
        const val MAX_EDGE = 1600
        const val THUMB_EDGE = 360
        const val JPEG_QUALITY = 80
        const val UNUSED_GRACE_MILLIS = 24L * 60 * 60 * 1000
    }
}
