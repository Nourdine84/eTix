package com.etix.features.ocr.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OCRPermissionBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)

            addView(TextView(context).apply {
                text = "Scanner un ticket"
                textSize = 18f
            })

            addView(TextView(context).apply {
                text = "Autorisez l'accès à la caméra pour scanner vos tickets."
                textSize = 14f
                setPadding(0, 24, 0, 24)
            })

            addView(Button(context).apply {
                text = "Autoriser et scanner"
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
        parentFragmentManager.beginTransaction()
            .replace(
                android.R.id.content,
                OCRScannerFragment()
            )
            .addToBackStack(null)
            .commit()
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
