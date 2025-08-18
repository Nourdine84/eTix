package com.etix.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val store: String,
    val date: String,
    val amount: Double,
    val category: String,
    val description: String
)
