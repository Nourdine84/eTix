package com.etix.features.ocr.ui

import android.os.Bundle
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.etix.R
import com.etix.features.ocr.OCRKeys
import com.etix.features.ocr.core.OCRTextAnalyzer
import com.etix.features.ocr.model.OCRResult
import java.util.concurrent.Executors

class OCRScannerFragment : Fragment(R.layout.fragment_ocr_scanner) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        startCamera()
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())

        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build()

            val analyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor, OCRTextAnalyzer { rawText ->
                    deliverResult(rawText)
                })
            }

            provider.unbindAll()
            provider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analyzer
            )

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun deliverResult(rawText: String) {

        val result = OCRResult(
            merchant = "CARREFOUR",
            amount = 25.99,
            dateMillis = System.currentTimeMillis(),
            rawText = rawText
        )

        setFragmentResult(
            OCRKeys.REQUEST_KEY,
            Bundle().apply {
                putParcelable(OCRKeys.RESULT_BUNDLE, result)
            }
        )

        parentFragmentManager.popBackStack()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
