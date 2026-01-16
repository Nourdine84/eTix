package com.etix.ui.detail

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentTicketEditV2Binding
import com.etix.model.Ticket
import kotlinx.coroutines.launch

class TicketEditFragmentV2 : Fragment() {

    private var _binding: FragmentTicketEditV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TicketRepository
    private var ticketId: Long = 0L
    private var currentTicket: Ticket? = null

    companion object {
        fun newInstance(ticketId: Long) = TicketEditFragmentV2().apply {
            arguments = Bundle().apply {
                putLong("ticketId", ticketId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticketId = requireArguments().getLong("ticketId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketEditV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = TicketRepository(
            AppDatabase.getInstance(requireContext()).ticketDao()
        )

        lifecycleScope.launch {
            repository.getByIdFlow(ticketId).collect { ticket ->
                ticket ?: return@collect
                currentTicket = ticket

                binding.inputStore.setText(ticket.store)
                binding.inputAmount.setText(ticket.amount.toString())
                binding.inputCategory.setText(ticket.category)
                binding.inputDescription.setText(ticket.description ?: "")
            }
        }

        binding.btnSave.setOnClickListener {
            save()
        }

        binding.btnDelete.setOnClickListener {
            delete()
        }
    }

    private fun save() {
        val t = currentTicket ?: return
        val updated = t.copy(
            store = binding.inputStore.text.toString(),
            amount = binding.inputAmount.text.toString().toDoubleOrNull() ?: 0.0,
            category = binding.inputCategory.text.toString(),
            description = binding.inputDescription.text.toString()
        )

        lifecycleScope.launch {
            repository.update(updated)
            parentFragmentManager.popBackStack()
        }
    }

    private fun delete() {
        currentTicket?.let {
            lifecycleScope.launch {
                repository.delete(it)
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
