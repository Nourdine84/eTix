package com.etix.ui.add

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.etix.databinding.FragmentAddTicketV2Binding
import com.etix.features.ocr.OCRKeys
import com.etix.features.ocr.domain.OCRTicketDraftMapper
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
     * OCR V3 – CLEAN & STABLE
     * - Transformation OCR → TicketDraft (domain)
     * - Pré-remplissage SAFE des champs
     * - Aucun badge
     * - Aucune couleur
     * - Aucune navigation
     */
    private fun applyOCRResult(result: OCRResult) {

        val draft = OCRTicketDraftMapper.toDraft(
            rawText = result.rawText ?: return
        )

        // 🏪 Magasin
        draft.storeName?.let { storeName ->
            binding.inputStore.setText(storeName.toString())
        }

        // 💰 Montant
        draft.amount?.let { amount ->
            binding.inputAmount.setText(amount.toString())
        }

        // ⛔ Date / Catégorie / Description
        // 👉 volontairement NON branchés (V3 scope validé)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
