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
import com.etix.features.ocr.model.OCRResult
import com.etix.features.ocr.ui.OCRPermissionBottomSheet

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

        // ✅ Lancer OCR via BottomSheet (PAS de NavController)
        binding.btnScanTicket.setOnClickListener {
            OCRPermissionBottomSheet().show(
                parentFragmentManager,
                "OCRPermission"
            )
        }

        // ✅ Réception du résultat OCR
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
     * OCR V3 – CLEAN & SAFE
     * - Aucun NavController
     * - Aucun badge
     * - Aucun drawable
     * - Remplissage minimal
     */
    private fun applyOCRResult(result: OCRResult) {

        // 🏪 Magasin
        result.merchant
            ?.takeIf { it.isNotBlank() }
            ?.let { binding.inputStore.setText(it) }

        // 💰 Montant
        result.amount
            ?.let { binding.inputAmount.setText(it.toString()) }

        // ⛔ Le reste viendra plus tard (V4+)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
