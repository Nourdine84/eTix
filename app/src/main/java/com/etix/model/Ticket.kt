package com.etix.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 🔐 ENTITÉ CENTRALE DE L’APP eTix
 * ⚠️ Ce fichier est CANONIQUE
 * ⚠️ Ne pas renommer les champs sans impacter DAO / VM / UI
 */
@Entity(tableName = "tickets")
data class Ticket(

    // ✅ ID unique – utilisé partout (Safe Args, Detail, Edit)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // 🏬 Nom du magasin
    @ColumnInfo(name = "store")
    val store: String,

    // 💰 Montant du ticket
    val amount: Double,

    // 🏷️ Catégorie (texte pour V2)
    val category: String,

    // 📝 Description optionnelle
    val description: String? = null,

    // 📅 Date en millis (UNIFIÉE sur toute l’app)
    val dateMillis: Long
)
