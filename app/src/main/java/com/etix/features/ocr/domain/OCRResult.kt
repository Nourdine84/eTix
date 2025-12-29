package com.etix.features.ocr.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OCRResult(
    val merchant: String?,
    val amount: Double?,
    val dateMillis: Long?,
    val rawText: String
) : Parcelable
