package com.moltobene.app.data.translate

import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/**
 * Gespeicherte Übersetzungen (#60): je Rezept und Sprache eine kleine Datei in `noBackupFilesDir`. Sie sind ein
 * Zwischenspeicher und kein Teil des Rezepts – nicht in der Datenbank, nicht in der Sicherung, nie geteilt – und
 * lassen sich jederzeit neu erzeugen. Dazu merkt sich die App je Rezept, welche Sprache zuletzt gezeigt wurde.
 * Reines Kotlin, per Unit-Test prüfbar.
 */
class TranslationStore(private val directory: File) {

    /** Die gespeicherte Übersetzung, wenn sie zum Original passt ([sourceHash]); sonst null. */
    fun load(recipeId: String, language: String, sourceHash: String): TranslatedRecipe? = runCatching {
        val file = file(recipeId, language)
        if (!file.isFile || file.length() > MAX_FILE_BYTES) return null
        JSON.decodeFromString(TranslatedRecipe.serializer(), file.readText())
    }.getOrNull()?.takeIf { it.sourceHash == sourceHash && it.language == language }

    fun save(recipeId: String, translation: TranslatedRecipe) {
        directory.mkdirs()
        val target = file(recipeId, translation.language)
        val temp = File(directory, target.name + ".neu")
        temp.writeText(JSON.encodeToString(TranslatedRecipe.serializer(), translation))
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Übersetzung nicht gespeichert")
        }
    }

    /** Zuletzt gezeigte Sprache; null heißt: das Original. */
    fun shownLanguage(recipeId: String): String? = runCatching {
        File(directory, "${key(recipeId)}$SHOWN").takeIf { it.isFile }?.readText()?.trim()?.takeIf { LANGUAGE.matches(it) }
    }.getOrNull()

    fun setShownLanguage(recipeId: String, language: String?) {
        val file = File(directory, "${key(recipeId)}$SHOWN")
        if (language == null) {
            file.delete()
        } else {
            require(LANGUAGE.matches(language))
            directory.mkdirs()
            file.writeText(language)
        }
    }

    /** Entfernt alles zu einem Rezept, z. B. wenn es gelöscht wurde. */
    fun delete(recipeId: String) {
        val prefix = key(recipeId) + "."
        directory.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
    }

    /** Entfernt Übersetzungen von Rezepten, die es nicht mehr gibt. */
    fun deleteUnused(recipeIds: Set<String>) {
        val keys = recipeIds.map(::key).toSet()
        directory.listFiles()?.filter { it.name.substringBefore('.') !in keys }?.forEach { it.delete() }
    }

    private fun file(recipeId: String, language: String): File {
        require(LANGUAGE.matches(language))
        return File(directory, "${key(recipeId)}.$language.json")
    }

    private companion object {
        const val SHOWN = ".anzeige"
        const val MAX_FILE_BYTES = 2L * 1024 * 1024
        val LANGUAGE = Regex("[a-z]{2,3}")
        val SAFE_ID = Regex("[A-Za-z0-9-]{1,64}")
        val JSON = Json { ignoreUnknownKeys = true }

        /** Kennungen sind UUIDs; alles andere wird für den Dateinamen umgerechnet. */
        fun key(recipeId: String): String =
            if (SAFE_ID.matches(recipeId)) {
                recipeId
            } else {
                MessageDigest.getInstance("SHA-256").digest(recipeId.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 0xFF) }.take(32)
            }
    }
}
