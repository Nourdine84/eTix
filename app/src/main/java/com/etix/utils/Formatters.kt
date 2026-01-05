package com.etix.utils

import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.FRANCE).format(amount)
