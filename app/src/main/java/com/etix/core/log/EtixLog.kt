package com.etix.core.log

import android.util.Log
import com.etix.core.log.flags.QAFlags

object EtixLog {

    private const val TAG = "eTix"

    fun d(message: String) {
        if (QAFlags.LOG_ENABLED) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (QAFlags.LOG_ENABLED) {
            Log.e(TAG, message, throwable)
        }
    }

    fun i(message: String) {
        if (QAFlags.LOG_ENABLED) {
            Log.i(TAG, message)
        }
    }
}
