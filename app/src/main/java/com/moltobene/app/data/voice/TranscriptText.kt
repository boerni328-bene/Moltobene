package com.moltobene.app.data.voice

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Der erkannte Text aus der Antwort der Erkennung, z. B. `{"text": "zwei eier"}`. Reines Kotlin, per Unit-Test prüfbar. */
object TranscriptText {

    /** Der Text; leer, wenn nichts erkannt wurde oder die Antwort nicht lesbar ist. */
    fun fromJson(json: String?): String = runCatching {
        Json.parseToJsonElement(json.orEmpty()).jsonObject["text"]?.jsonPrimitive?.contentOrNull
    }.getOrNull().orEmpty().trim()

    /** Das Teilergebnis beim Sprechen, z. B. `{"partial": "zwei ei"}`; leer, wenn nichts erkannt wurde. */
    fun partialFromJson(json: String?): String = runCatching {
        Json.parseToJsonElement(json.orEmpty()).jsonObject["partial"]?.jsonPrimitive?.contentOrNull
    }.getOrNull().orEmpty().trim()
}
