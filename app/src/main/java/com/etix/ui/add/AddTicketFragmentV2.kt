package com.etix.ui.add

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.etix.R
import com.etix.databinding.FragmentAddTicketV2Binding
import com.etix.features.ocr.OCRKeys
import com.etix.features.ocr.model.OCRResult

class AddTicketFragmentV2 : Fragment() {

    private var _binding: FragmentAddTicketV2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTicketV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // 📸 Lancer OCR
        binding.btnScanTicket.setOnClickListener {
            findNavController().navigate(
                R.id.action_addTicket_to_ocrPermission
            )
        }

        // 📥 Réception résultat OCR
        parentFragmentManager.setFragmentResultListener(
            OCRKeys.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->

            val result: OCRResult? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bundle.getParcelable(
                        OCRKeys.RESULT_BUNDLE,
                        OCRResult::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    bundle.getParcelable(OCRKeys.RESULT_BUNDLE)
                }

            result ?: return@setFragmentResultListener
            applyOCRResult(result)
        }
    }

    /**
     * OCR V2 – CLEAN
     * - Remplit uniquement les champs existants
     * - Aucun badge
     * - Aucune couleur
     * - Aucun drawable
     */
    private fun applyOCRResult(result: OCRResult) {

        // 🏪 Magasin
        result.merchant
            ?.takeIf { it.isNotBlank() }
            ?.let { binding.inputStore.setText(it) }

        // 💰 Montant
        result.amount
            ?.let { binding.inputAmount.setText(it.toString()) }

        // ⛔ Date / Catégorie / Description
        // 👉 volontairement NON branchés
        // 👉 OCR V2 = extraction simple
        // 👉 UI V3 arrivera ensuite
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
