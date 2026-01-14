package com.etix.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentAddTicketV2Binding
import com.etix.features.ocr.OCRKeys
import com.etix.features.ocr.model.OCRResult
import com.etix.model.Ticket
import kotlinx.coroutines.launch

class AddTicketFragmentV2 : Fragment() {

    private var _binding: FragmentAddTicketV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TicketRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTicketV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        repository = TicketRepository(dao)

        // 📥 OCR (optionnel)
        parentFragmentManager.setFragmentResultListener(
            OCRKeys.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val result = bundle.getParcelable<OCRResult>(OCRKeys.RESULT_BUNDLE)
            result ?: return@setFragmentResultListener
            applyOCRResult(result)
        }

        // 💾 ENREGISTREMENT
        binding.btnSaveTicket.setOnClickListener {
            saveTicket()
        }
    }

    private fun applyOCRResult(result: OCRResult) {
        result.merchant?.let { binding.inputStore.setText(it) }
        result.amount?.let { binding.inputAmount.setText(it.toString()) }
    }

    private fun saveTicket() {
        val store = binding.inputStore.text.toString().trim()
        val amountText = binding.inputAmount.text.toString().trim()

        if (store.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Champs obligatoires manquants", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(requireContext(), "Montant invalide", Toast.LENGTH_SHORT).show()
            return
        }

        val ticket = Ticket(
            store = store,
            amount = amount,
            dateMillis = System.currentTimeMillis(),
            category = "Autre",
            description = null
        )

        lifecycleScope.launch {
            repository.insert(ticket)

            Toast.makeText(
                requireContext(),
                "Ticket enregistré",
                Toast.LENGTH_SHORT
            ).show()

            clearForm()
        }
    }

    private fun clearForm() {
        binding.inputStore.setText("")
        binding.inputAmount.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
