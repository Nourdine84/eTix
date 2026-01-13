package com.etix.features.ocr.domain

/**
 * Debug OCR activable/désactivable facilement.
 * Pas de dépendance Android (Logcat), donc safe pour unit tests.
 */
object OCRDebug {

    var enabled: Boolean = false

    fun d(tag: String, msg: String) {
        if (enabled) println("[$tag] $msg")
    }
}
