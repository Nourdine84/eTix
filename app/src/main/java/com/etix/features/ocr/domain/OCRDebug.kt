package com.etix.features.ocr.domain

import android.util.Log

object OCRDebug {

    private const val TAG = "OCR_DEBUG"
    var enabled = false

    fun log(message: String) {
        if (enabled) {
            Log.d(TAG, message)
        }
    }
}
