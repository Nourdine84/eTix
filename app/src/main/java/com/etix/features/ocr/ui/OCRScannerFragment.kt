package com.etix.features.ocr.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.etix.R

class OCRScannerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ocr_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 🔥 V2.01 : OCR mock / texte simulé
        view.findViewById<View>(R.id.btnScan).setOnClickListener {
            findNavController().navigate(
                R.id.action_ocrScanner_to_ocrPreview,
                Bundle().apply {
                    putString(
                        "raw_text",
                        "CARREFOUR\nTotal 23,45€\nMerci"
                    )
                }
            )
        }
    }
}
