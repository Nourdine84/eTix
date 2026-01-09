package com.etix.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ✅ Repository basé sur AppDatabase (V2 propre)
    private val ticketRepository by lazy {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        TicketRepository(dao)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGoAdd.setOnClickListener {
            // TODO navigation AddTicketFragment
        }

        binding.btnHistoryContainer.setOnClickListener {
            // TODO navigation TicketHistoryFragment
        }
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }

    private fun updateStats() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 🔹 Début du jour
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        // 🔹 Début du mois
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val totalMonth = ticketRepository.sumBetweenDates(startOfMonth, now)
                val totalDay = ticketRepository.sumBetweenDates(startOfDay, now)
                val count = ticketRepository.countBetweenDates(startOfMonth, now)

                val average = if (count > 0) totalMonth / count else 0.0

                val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)

                launch(Dispatchers.Main) {
                    binding.kpiTotal.text = formatter.format(totalMonth)
                    binding.kpiAverage.text = formatter.format(average)
                    binding.kpiTicketCount.text = count.toString()
                    binding.tvTotalMonth.text = formatter.format(totalDay)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    binding.kpiTotal.text = "0,00 €"
                    binding.kpiAverage.text = "0,00 €"
                    binding.kpiTicketCount.text = "0"
                    binding.tvTotalMonth.text = "0,00 €"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
