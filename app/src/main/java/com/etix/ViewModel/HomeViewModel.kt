package com.etix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.max

data class HomeUiState(
    val todayTotal: Double = 0.0,
    val todayCount: Int = 0,
    val monthTotal: Double = 0.0,
    val monthCount: Int = 0,
    val monthAvg: Double = 0.0
)

class HomeViewModel(
    private val repository: TicketRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui

    init {
        observeKpis()
    }

    private fun observeKpis() {
        val (startDay, endDay) = dayRange()
        val (startMonth, endMonth) = monthRange()

        viewModelScope.launch {
            combine(
                repository.sumBetweenDatesFlow(startDay, endDay),
                repository.countBetweenDatesFlow(startDay, endDay),
                repository.sumBetweenDatesFlow(startMonth, endMonth),
                repository.countBetweenDatesFlow(startMonth, endMonth)
            ) { todaySum: Double,
                todayCount: Int,
                monthSum: Double,
                monthCount: Int ->

                val avg = if (monthCount > 0) monthSum / monthCount else 0.0

                HomeUiState(
                    todayTotal = max(0.0, todaySum),
                    todayCount = todayCount,
                    monthTotal = max(0.0, monthSum),
                    monthCount = monthCount,
                    monthAvg = max(0.0, avg)
                )
            }.collect { state ->
                _ui.value = state
            }
        }
    }

    private fun dayRange(): Pair<Long, Long> {
        val s = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val e = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return s to e
    }

    private fun monthRange(): Pair<Long, Long> {
        val s = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val e = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return s to e
    }
}
