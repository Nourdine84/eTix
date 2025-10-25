// 📁 com.etix.fragments.TicketDetailFragment.kt
package com.etix.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.etix.R
import com.etix.ViewModel.TicketDetailViewModel
import com.etix.ViewModel.Factory.TicketDetailVMFactory
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class TicketDetailFragment : Fragment() {

    private val args: TicketDetailFragmentArgs by navArgs()

    private lateinit var tvStoreName: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvDescription: TextView

    private val viewModel: TicketDetailViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        val repo = TicketRepository(dao)
        TicketDetailVMFactory(args.ticketId, repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ticket_detail, container, false)

        tvStoreName = view.findViewById(R.id.tvStoreName)
        tvAmount = view.findViewById(R.id.tvAmount)
        tvDate = view.findViewById(R.id.tvDate)
        tvCategory = view.findViewById(R.id.tvCategory)
        tvDescription = view.findViewById(R.id.tvDescription)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            viewModel.ticket.collectLatest { ticket ->
                if (ticket != null) {

                    // 🛠️ Correction : `storeName` → `store`
                    tvStoreName.text = "Magasin : ${ticket.store}"

                    // 💶 Formatage du montant
                    val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)
                    val amountFormatted = formatter.format(ticket.amount)
                    tvAmount.text = "Montant : $amountFormatted"

                    tvDate.text = "Date : ${formatDate(ticket.dateMillis)}"
                    tvCategory.text = "Catégorie : ${ticket.category}"
                    tvDescription.text = "Description : ${ticket.description ?: "-"}"
                }
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}