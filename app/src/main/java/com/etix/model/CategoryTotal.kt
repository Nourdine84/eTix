package com.etix.model

data class CategoryTotal(
    val name: String,    // ✅ correspond maintenant à l'alias "name" dans la requête
    val total: Double
)
