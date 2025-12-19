package com.etix.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.etix.databinding.FragmentHomeBinding
import com.etix.data.getDatabase  // Note: utilise getDatabase() au lieu de AppDatabase
import com.etix.data.TicketRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ✅ Utilise getDatabase() (fonction top-level) au lieu de AppDatabase
    private val ticketRepository by lazy {
        TicketRepository(getDatabase(requireContext()).ticketDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGoAdd.setOnClickListener {
            // TODO: navigation vers AddTicketFragment
        }

        binding.btnHistoryContainer.setOnClickListener {
            // TODO: navigation vers TicketHistoryFragment
        }
    }

    override fun onResume() {
        super.onResume()
        updateStats()
    }

    private fun updateStats() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Début du jour
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        // Début du mois
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ Utilise les bonnes fonctions du Repository
                val totalMonth = ticketRepository.sumBetweenDates(startOfMonth, now)
                val totalDay = ticketRepository.sumBetweenDates(startOfDay, now)
                val count = ticketRepository.countBetweenDates(startOfMonth, now)

                // Calcul de la moyenne manuellement (total / count)
                val average = if (count > 0) totalMonth / count else 0.0

                val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)
                val monthFormatted = formatter.format(totalMonth)
                val dayFormatted = formatter.format(totalDay)
                val averageFormatted = formatter.format(average)

                launch(Dispatchers.Main) {
                    binding.kpiTotal.text = monthFormatted
                    binding.kpiAverage.text = averageFormatted
                    binding.kpiTicketCount.text = count.toString()
                    binding.tvTotalMonth.text = dayFormatted
                }
            } catch (e: Exception) {
                e.printStackTrace()
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