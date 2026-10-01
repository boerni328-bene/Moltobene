package com.moltobene.app.data.ocr

/** Graustufenbild für die Texterkennung: ein Byte je Bildpunkt, Zeile für Zeile. */
class GrayImage(val width: Int, val height: Int, val pixels: ByteArray)
