package com.etix.ui.add

import android.os.Build
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
import com.etix.databinding.FragmentAddTicketV2Binding
import com.etix.features.ocr.OCRKeys
import com.etix.features.ocr.model.OCRResult
import com.etix.model.Ticket
import com.etix.ui.ticket.TicketFormController
import kotlinx.coroutines.launch

/**
 * Ajouter un ticket — référence iOS AddTicketView : magasin, montant, date, catégorie, description.
 * Validation iOS : magasin non vide + montant > 0 (virgule acceptée). Après enregistrement : formulaire
 * réinitialisé, on reste sur l'écran (décision produit iOS, ROADMAP « retour Home différé »).
 */
class AddTicketFragmentV2 : Fragment() {

    private var _binding: FragmentAddTicketV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TicketRepository
    private lateinit var form: TicketFormController
    private var usedCategories: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTicketV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        form = TicketFormController(this, binding.form) { usedCategories }
        savedInstanceState?.let {
            form.restore(it.getLong(KEY_DATE, System.currentTimeMillis()), it.getString(KEY_CATEGORY).orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.distinctCategoriesFlow().collect { usedCategories = it }
            }
        }

        binding.form.inputAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { form.setAmountInvalid(false) }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 📥 OCR (flux non branché ; conservé pour le lot OCR)
        parentFragmentManager.setFragmentResultListener(OCRKeys.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            @Suppress("DEPRECATION")
            val result = if (Build.VERSION.SDK_INT >= 33) bundle.getParcelable(OCRKeys.RESULT_BUNDLE, OCRResult::class.java)
                         else bundle.getParcelable(OCRKeys.RESULT_BUNDLE)
            result?.merchant?.let { binding.form.inputStore.setText(it) }
            result?.amount?.let { binding.form.inputAmount.setText(it.toString().replace('.', ',')) }
        }

        binding.btnSaveTicket.setOnClickListener { save() }
    }

    private fun save() {
        val v = form.read()
        if (v.store.isEmpty() || v.amount == null) {
            form.setAmountInvalid(v.amount == null)
            Toast.makeText(requireContext(), "Impossible d'enregistrer. Vérifie le magasin et le montant.", Toast.LENGTH_LONG).show()
            return
        }
        val ticket = Ticket(store = v.store, amount = v.amount, dateMillis = v.dateMillis,
            category = v.category, description = v.description)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.insert(ticket)
            Toast.makeText(requireContext(), "Ticket enregistré", Toast.LENGTH_SHORT).show()
            form.reset()
        }
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

    companion object {
        private const val KEY_DATE = "add_date"
        private const val KEY_CATEGORY = "add_category"
    }
}
