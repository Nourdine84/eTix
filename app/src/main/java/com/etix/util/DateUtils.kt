package com.etix.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun today(): String = fmt.format(System.currentTimeMillis())

    fun firstDayOfCurrentMonth(): String {
        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        return fmt.format(cal.time)
    }

    fun lastDayOfCurrentMonth(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        return fmt.format(cal.time)
    }
}
