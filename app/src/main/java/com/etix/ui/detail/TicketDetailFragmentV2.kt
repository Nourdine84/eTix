package com.etix.ui.detail

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentTicketDetailV2Binding
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.launch
import java.util.Locale

class TicketDetailFragmentV2 : Fragment() {

    private var _binding: FragmentTicketDetailV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TicketRepository
    private var ticketId: Long = 0L

    companion object {
        fun newInstance(ticketId: Long) = TicketDetailFragmentV2().apply {
            arguments = Bundle().apply { putLong("ticketId", ticketId) }
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
        _binding = FragmentTicketDetailV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = TicketRepository(
            AppDatabase.getInstance(requireContext()).ticketDao()
        )

        // viewLifecycleOwner : la collecte s'arrête quand la vue est détruite
        // (sinon NPE sur binding quand l'édition met à jour le ticket)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getByIdFlow(ticketId).collect { ticket ->
                ticket ?: return@collect

                binding.tvStore.text = ticket.store
                binding.tvAmount.text = String.format(Locale.FRANCE, "%.2f €", ticket.amount)
                binding.tvCategory.text = ticket.category
                binding.tvDescription.text = ticket.description ?: "-"
            }
        }

        // Avant : requireParentFragment() → IllegalStateException (pas de fragment parent)
        binding.btnEdit.setOnClickListener {
            (activity as? MainActivityV2)?.openTicketEdit(ticketId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
