package com.etix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
        observeTickets()
    }

    private fun observeTickets() {
        viewModelScope.launch {
            repository.getAllFlow().collectLatest { list ->
                _ui.value = computeKpis(list)
            }
        }
    }

    private fun computeKpis(list: List<Ticket>): HomeUiState {
        val (startDay, endDay) = dayRange()
        val (startMonth, endMonth) = monthRange()

        var todayTotal = 0.0
        var todayCount = 0
        var monthTotal = 0.0
        var monthCount = 0

        for (t in list) {
            if (t.dateMillis in startDay..endDay) {
                todayTotal += t.amount
                todayCount++
            }
            if (t.dateMillis in startMonth..endMonth) {
                monthTotal += t.amount
                monthCount++
            }
        }
        val monthAvg = if (monthCount > 0) monthTotal / monthCount else 0.0

        return HomeUiState(
            todayTotal = max(0.0, todayTotal),
            todayCount = todayCount,
            monthTotal = max(0.0, monthTotal),
            monthCount = monthCount,
            monthAvg = max(0.0, monthAvg)
        )
    }

    private fun dayRange(): Pair<Long, Long> {
        val s = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val e = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return s to e
    }

    private fun monthRange(): Pair<Long, Long> {
        val s = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val e = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return s to e
    }
}