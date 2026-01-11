package com.etix.features.ocr.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.etix.R
import com.etix.features.ocr.OCRKeys
import com.etix.features.ocr.engine.OCRProcessor

class OCRPreviewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ocr_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val rawText = arguments?.getString(OCRKeys.RAW_TEXT) ?: return

        val result = OCRProcessor.process(rawText)

        view.findViewById<View>(R.id.btnConfirm).setOnClickListener {
            setFragmentResult(
                OCRKeys.REQUEST_KEY,
                bundleOf(OCRKeys.RESULT_BUNDLE to result)
            )
            parentFragmentManager.popBackStack()
        }
    }
}
