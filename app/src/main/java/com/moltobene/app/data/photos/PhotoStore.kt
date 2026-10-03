package com.moltobene.app.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.moltobene.app.data.ocr.CropArea
import com.moltobene.app.data.ocr.GrayImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
 * Zum Teilen werden Fotos ebenfalls neu gespeichert ([encodeForSharing]).
 * Für die Texterkennung liefert [loadForRecognition] ein passend großes Graustufenbild;
 * gelesene Seiten werden mit [importPage] zu Originalseiten.
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

    /** Gibt das zuletzt aufgenommene Kamerafoto unverändert an [target] weiter (als Seite für die Texterkennung). */
    suspend fun moveCameraCapture(target: File) = withContext(Dispatchers.IO) {
        if (!cameraFile.isFile) throw IOException("Kein Kamerafoto")
        target.parentFile?.mkdirs()
        if (!cameraFile.renameTo(target)) {
            try {
                cameraFile.copyTo(target, overwrite = true)
            } finally {
                cameraFile.delete()
            }
        }
    }

    /**
     * Foto für die Texterkennung: richtig gedreht, in Graustufen (ein Byte je Bildpunkt) und so groß,
     * dass Tesseract die Schrift gut lesen kann – kleine Fotos werden vergrößert, große verkleinert.
     */
    suspend fun loadForRecognition(uri: Uri): GrayImage = withContext(Dispatchers.IO) {
        // Halber Speicherbedarf; Farben braucht die Texterkennung nicht.
        val rotated = decodeOriented(uri, OCR_MIN_EDGE, Bitmap.Config.RGB_565)
        val edge = max(rotated.width, rotated.height)
        val target = when {
            edge < OCR_TARGET_EDGE -> minOf(OCR_TARGET_EDGE, edge * 2)
            edge > OCR_MAX_EDGE -> OCR_MAX_EDGE
            else -> edge
        }
        val scaled = if (target == edge) {
            rotated
        } else {
            Bitmap.createScaledBitmap(
                rotated,
                (rotated.width.toLong() * target / edge).toInt().coerceAtLeast(1),
                (rotated.height.toLong() * target / edge).toInt().coerceAtLeast(1),
                true,
            )
        }
        try {
            toGray(scaled)
        } finally {
            listOf(rotated, scaled).distinct().forEach { it.recycle() }
        }
    }

    /**
     * Foto für „Bereich auswählen“: genauso gedreht wie für die Texterkennung, damit der Rahmen
     * dieselbe Stelle trifft, aber nur so groß, wie der Bildschirm es braucht ([edge], kleiner z. B. für die
     * Vorschaubilder der Seitenübersicht).
     */
    suspend fun loadPreview(uri: Uri, edge: Int = PREVIEW_EDGE): Bitmap = withContext(Dispatchers.IO) {
        val rotated = decodeOriented(uri, edge, Bitmap.Config.ARGB_8888)
        scaleDown(rotated, edge).also { if (it !== rotated) rotated.recycle() }
    }

    /**
     * Seite zum Ansehen bzw. als Originalseite (#38): genauso gedreht wie für die Texterkennung,
     * auf den gewählten Bereich zugeschnitten und höchstens [PAGE_EDGE] groß – genug, um auch
     * kleine Schrift vergrößert zu lesen.
     */
    suspend fun loadPage(uri: Uri, area: CropArea): Bitmap = withContext(Dispatchers.IO) {
        // RGB_565 braucht halb so viel Speicher; für Seiten reicht das.
        val rotated = decodeOriented(uri, OCR_MIN_EDGE, Bitmap.Config.RGB_565)
        val cropped = if (area.isWholePage) {
            rotated
        } else {
            val box = area.toBox(rotated.width, rotated.height)
            Bitmap.createBitmap(rotated, box.left, box.top, box.width, box.height)
        }
        val scaled = scaleDown(cropped, PAGE_EDGE)
        listOf(rotated, cropped).distinct().filter { it !== scaled }.forEach { it.recycle() }
        scaled
    }

    /** Behält eine gelesene Seite als Originalseite: neu gespeichert, also ohne Zusatzdaten wie den Aufnahmeort. */
    suspend fun importPage(uri: Uri, area: CropArea): String {
        val page = loadPage(uri, area)
        return withContext(Dispatchers.IO) {
            val thumb = scaleDown(page, THUMB_EDGE)
            val photoId = UUID.randomUUID().toString()
            try {
                writeJpeg(page, photoFile(photoId))
                writeJpeg(thumb, thumbFile(photoId))
            } catch (e: IOException) {
                delete(photoId)
                throw e
            } finally {
                listOf(page, thumb).distinct().forEach { it.recycle() }
            }
            photoId
        }
    }

    /** Liest ein Foto mindestens [minEdge] groß (sofern vorhanden) und dreht es laut Exif-Angabe richtig. */
    private fun decodeOriented(uri: Uri, minEdge: Int, config: Bitmap.Config): Bitmap {
        val resolver = context.contentResolver
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
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, minEdge)
            inPreferredConfig = config
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Foto nicht lesbar")
        val rotated = applyOrientation(decoded, orientation)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }

    private fun toGray(bitmap: Bitmap): GrayImage {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = ByteArray(width * height)
        val row = IntArray(width)
        for (y in 0 until height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            val offset = y * width
            for (x in 0 until width) {
                val color = row[x]
                val gray = (((color shr 16) and 0xFF) * 299 + ((color shr 8) and 0xFF) * 587 + (color and 0xFF) * 114) / 1000
                pixels[offset + x] = gray.toByte()
            }
        }
        return GrayImage(width, height, pixels)
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

    /**
     * Foto zum Teilen: neu als JPEG gespeichert, damit garantiert keine Zusatzdaten wie der
     * Aufnahmeort mitgehen. null, wenn das Foto fehlt oder nicht lesbar ist.
     */
    suspend fun encodeForSharing(photoId: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = photoFile(photoId)
        if (!file.isFile) return@withContext null
        val bitmap = BitmapFactory.decodeFile(file.path) ?: return@withContext null
        try {
            val out = ByteArrayOutputStream()
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, SHARE_JPEG_QUALITY, out)) out.toByteArray() else null
        } finally {
            bitmap.recycle()
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
        // Etwas höher, weil das Foto beim Teilen ein zweites Mal gespeichert wird.
        const val SHARE_JPEG_QUALITY = 90
        const val UNUSED_GRACE_MILLIS = 24L * 60 * 60 * 1000

        // Texterkennung: Bei etwa 2400 px liest Tesseract Kochbuchseiten am zuverlässigsten.
        const val OCR_MIN_EDGE = 1800
        const val OCR_TARGET_EDGE = 2400
        const val OCR_MAX_EDGE = 3000

        /** Größe für „Bereich auswählen“. */
        const val PREVIEW_EDGE = 1600

        /** Größe einer Originalseite. */
        const val PAGE_EDGE = 2000
    }
}
