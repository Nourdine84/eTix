package com.etix.ui.detail

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentTicketEditV2Binding
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
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

        // Chargement unique : une collecte continue écraserait la saisie en cours
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getById(ticketId)?.let { ticket ->
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
        // Avant : "12,50" (virgule FR) → 0.0 enregistré silencieusement
        val amount = binding.inputAmount.text.toString().trim()
            .replace(',', '.').toDoubleOrNull()
        val store = binding.inputStore.text.toString().trim()
        if (amount == null || amount <= 0.0 || store.isEmpty()) {
            Toast.makeText(requireContext(), "Magasin et montant valides requis", Toast.LENGTH_SHORT).show()
            return
        }
        val updated = t.copy(
            store = store,
            amount = amount,
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
                // Le détail d'un ticket supprimé n'a plus de sens : on ferme les deux écrans
                (activity as? MainActivityV2)?.closeTicketFlow()
                    ?: parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
