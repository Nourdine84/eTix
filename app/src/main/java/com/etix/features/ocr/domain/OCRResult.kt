package com.etix.features.ocr.domain

data class OCRResult(
    val storeName: String? = null,
    val amount: Double? = null,
    val dateMillis: Long? = null,
    val category: String? = null
)
