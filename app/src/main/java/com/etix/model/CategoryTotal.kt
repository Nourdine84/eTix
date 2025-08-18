package com.etix.model

import androidx.room.ColumnInfo

data class CategoryTotal(
    val category: String,
    @ColumnInfo(name = "totalAmount") val totalAmount: Double
)
