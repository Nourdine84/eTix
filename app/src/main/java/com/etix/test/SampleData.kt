package com.etix.test

import com.etix.model.Ticket
import java.util.*

fun ticket(
    id: Long = 0L,
    store: String = "Shop",
    amount: Double = 10.0,
    category: String = "Autre",
    description: String? = null,
    dateMillis: Long = Calendar.getInstance().timeInMillis
) = Ticket(
    id = id,
    store = store,
    amount = amount,
    category = category,
    description = description,
    dateMillis = dateMillis
)