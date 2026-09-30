package com.etix.ui.detail

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentTicketEditV2Binding
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import com.etix.ui.ticket.TicketFormController
import kotlinx.coroutines.launch

/**
 * Modifier un ticket — référence iOS TicketEditView (TicketForm partagé : magasin, montant, date,
 * catégorie, description). « Supprimer » (existant Android, suppression d'UN ticket) conservé.
 */
class TicketEditFragmentV2 : Fragment() {

    private var _binding: FragmentTicketEditV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TicketRepository
    private lateinit var form: TicketFormController
    private var ticketId: Long = 0L
    private var currentTicket: Ticket? = null
    private var usedCategories: List<String> = emptyList()

    companion object {
        private const val KEY_DATE = "edit_date"
        private const val KEY_CATEGORY = "edit_category"

        fun newInstance(ticketId: Long) = TicketEditFragmentV2().apply {
            arguments = Bundle().apply { putLong("ticketId", ticketId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticketId = requireArguments().getLong("ticketId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTicketEditV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        form = TicketFormController(this, binding.form) { usedCategories }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.distinctCategoriesFlow().collect { usedCategories = it }
            }
        }

        // Chargement unique : une collecte continue écraserait la saisie en cours.
        // Après recréation (rotation), les champs texte sont restaurés par Android ; date/catégorie par le Bundle.
        viewLifecycleOwner.lifecycleScope.launch {
            val t = repository.getById(ticketId) ?: return@launch
            currentTicket = t
            if (savedInstanceState == null) {
                form.fill(t)
            } else {
                form.restore(savedInstanceState.getLong(KEY_DATE, t.dateMillis), savedInstanceState.getString(KEY_CATEGORY).orEmpty())
            }
        }

        binding.form.inputAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { form.setAmountInvalid(false) }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { delete() }
    }

    private fun save() {
        val t = currentTicket ?: return
        val v = form.read()
        if (v.store.isEmpty() || v.amount == null) {
            form.setAmountInvalid(v.amount == null)
            Toast.makeText(requireContext(), "Magasin et montant valides requis", Toast.LENGTH_SHORT).show()
            return
        }
        val updated = t.copy(store = v.store, amount = v.amount, dateMillis = v.dateMillis,
            category = v.category, description = v.description)
        lifecycleScope.launch {
            repository.update(updated)
            parentFragmentManager.popBackStack()
        }
    }

    /** Suppression d'UN ticket, après confirmation (iOS : ConfirmDeletePopup) — logique partagée avec le détail. */
    private fun delete() {
        val t = currentTicket ?: return
        TicketDeletion.confirm(this, t, repository)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::form.isInitialized && _binding != null) {
            outState.putLong(KEY_DATE, form.dateMillis)
            outState.putString(KEY_CATEGORY, form.category)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
