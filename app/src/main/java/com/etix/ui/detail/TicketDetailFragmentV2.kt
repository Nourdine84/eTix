package com.etix.ui.detail

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentTicketDetailV2Binding
import kotlinx.coroutines.launch

class TicketDetailFragmentV2 : Fragment() {

    private var _binding: FragmentTicketDetailV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TicketRepository
    private var ticketId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticketId = requireArguments().getLong("ticketId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketDetailV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = TicketRepository(
            AppDatabase.getInstance(requireContext()).ticketDao()
        )

        lifecycleScope.launch {
            repository.getByIdFlow(ticketId).collect { ticket ->
                ticket ?: return@collect

                binding.tvStore.text = ticket.store
                binding.tvAmount.text = "${ticket.amount} €"
                binding.tvCategory.text = ticket.category
                binding.tvDescription.text = ticket.description ?: "-"
            }
        }

        binding.btnEdit.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireParentFragment().id,
                    TicketEditFragmentV2.newInstance(ticketId)
                )
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
