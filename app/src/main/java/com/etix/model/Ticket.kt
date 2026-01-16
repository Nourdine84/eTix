package com.etix.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 🔐 ENTITÉ CENTRALE DE L’APP eTix
 * ⚠️ CANONIQUE — alignée DAO / VM / UI
 */
@Entity(tableName = "tickets")
data class Ticket(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "store")
    val store: String,

    val amount: Double,

    val category: String,

    val description: String? = null,

    val dateMillis: Long
)
