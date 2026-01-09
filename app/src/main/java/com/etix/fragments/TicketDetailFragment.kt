package com.etix.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.viewmodel.TicketDetailViewModel
import com.etix.viewmodel.factory.TicketDetailVMFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TicketDetailFragment : Fragment() {

    private val args: TicketDetailFragmentArgs by navArgs()

    private lateinit var txtStore: TextView
    private lateinit var txtAmount: TextView
    private lateinit var txtCategory: TextView
    private lateinit var txtDate: TextView
    private lateinit var txtDescription: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button

    private val viewModel: TicketDetailViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        TicketDetailVMFactory(
            TicketRepository(dao),
            args.ticketId
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_ticket_detail, container, false)

        txtStore = view.findViewById(R.id.tvStore)
        txtAmount = view.findViewById(R.id.tvAmount)
        txtCategory = view.findViewById(R.id.tvCategory)
        txtDate = view.findViewById(R.id.tvDate)
        txtDescription = view.findViewById(R.id.tvDescription)
        btnEdit = view.findViewById(R.id.btnEdit)
        btnDelete = view.findViewById(R.id.btnDelete)

        observeTicket()
        setupActions()

        return view
    }

    private fun observeTicket() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ticket.collectLatest { ticket ->
                if (ticket == null) return@collectLatest

                txtStore.text = ticket.store
                txtAmount.text =
                    String.format(Locale.FRANCE, "%.2f €", ticket.amount)
                txtCategory.text = ticket.category
                txtDate.text = formatDate(ticket.dateMillis)
                txtDescription.text = ticket.description ?: "-"
            }
        }
    }

    private fun setupActions() {

        btnEdit.setOnClickListener {
            val action =
                TicketDetailFragmentDirections.actionDetailToEdit(args.ticketId)
            findNavController().navigate(action)
        }

        btnDelete.setOnClickListener {
            viewModel.delete()
            findNavController().navigateUp()
        }
    }

    private fun formatDate(ms: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        return sdf.format(Date(ms))
    }
}
