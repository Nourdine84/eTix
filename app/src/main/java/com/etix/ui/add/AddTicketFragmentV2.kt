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
import com.etix.features.ocr.domain.OCRResult

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

        binding.btnScanTicket.setOnClickListener {
            findNavController().navigate(
                R.id.action_addTicket_to_ocrPermission
            )
        }

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

    private fun applyOCRResult(result: OCRResult) {
        binding.inputStore.setText(result.merchant ?: "")
        binding.inputAmount.setText(result.amount?.toString() ?: "")
        // ⛔ category & description volontairement ignorés
        // 👉 seront branchés lors de l’injection du nouveau design
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
