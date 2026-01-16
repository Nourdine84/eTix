package com.etix.ui.history

sealed class DateFilter {
    object All : DateFilter()
    object Today : DateFilter()
    object Week : DateFilter()
    object Month : DateFilter()
    data class Custom(val start: Long, val end: Long) : DateFilter()
}
