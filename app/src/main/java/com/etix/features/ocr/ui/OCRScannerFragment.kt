package com.etix.features.ocr.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.etix.R
import com.etix.features.ocr.OCRKeys
import com.etix.features.ocr.core.OCRTextAnalyzer
import java.util.concurrent.Executors

class OCRScannerFragment : Fragment() {

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ocr_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        startCamera(view)
    }

    private fun startCamera(view: View) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()

            val analyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, OCRTextAnalyzer { rawText ->
                        navigateToPreview(rawText)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                analyzer
            )
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun navigateToPreview(rawText: String) {
        findNavController().navigate(
            R.id.action_ocrScanner_to_ocrPreview,
            Bundle().apply {
                putString(OCRKeys.RAW_TEXT, rawText)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
