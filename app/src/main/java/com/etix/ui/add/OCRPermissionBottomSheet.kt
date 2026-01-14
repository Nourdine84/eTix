package com.etix.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import com.etix.features.ocr.OCRKeys
import com.etix.features.ocr.model.OCRResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OCRPermissionBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)

            addView(TextView(context).apply {
                text = "Scanner un ticket"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 0, 24)
            })

            addView(TextView(context).apply {
                text = "L'application utilise la caméra pour analyser votre ticket."
                textSize = 14f
                setPadding(0, 0, 0, 32)
            })

            addView(Button(context).apply {
                text = "Continuer"
                setOnClickListener {
                    if (hasCameraPermission()) {
                        openScanner()
                    } else {
                        requestPermissions(
                            arrayOf(Manifest.permission.CAMERA),
                            1001
                        )
                    }
                }
            })
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openScanner() {
        dismiss()

        // 🔁 TEMP : simulation OCR (stable)
        val result = OCRResult(
            merchant = "CARREFOUR",
            amount = 25.99,
            dateMillis = System.currentTimeMillis(),
            rawText = "CARREFOUR\nTOTAL 25,99€"
        )

        setFragmentResult(
            OCRKeys.REQUEST_KEY,
            Bundle().apply {
                putParcelable(OCRKeys.RESULT_BUNDLE, result)
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openScanner()
        } else {
            dismiss()
        }
    }
}