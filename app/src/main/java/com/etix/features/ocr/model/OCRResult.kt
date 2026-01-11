package com.etix.features.ocr.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OCRResult(
    val merchant: String? = null,
    val amount: Double? = null,
    val dateMillis: Long? = null,
    val rawText: String
) : Parcelable
