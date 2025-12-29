package com.etix.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.etix.R
import com.etix.features.ocr.OCRFlags

class AddTicketFragmentV2 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 🔒 V2 : layout vide
        // 🔓 V2.01 : layout avec bouton OCR
        return inflater.inflate(
            if (OCRFlags.ENABLE_OCR)
                R.layout.fragment_add_ticket_v2   // futur
            else
                R.layout.empty_layout,
            container,
            false
        )
    }
}
