package com.etix.features.ocr.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.etix.R

class OCRPermissionFragment : Fragment(R.layout.fragment_ocr_permission) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            goToScanner()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1001 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            goToScanner()
        }
    }

    private fun goToScanner() {
        findNavController().navigate(
            R.id.action_ocrPermission_to_ocrScanner
        )
    }
}
