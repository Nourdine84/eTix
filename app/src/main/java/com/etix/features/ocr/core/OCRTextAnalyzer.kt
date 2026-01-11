package com.etix.features.ocr.core

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OCRTextAnalyzer(
    private val onTextDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isNotBlank()) {
                    onTextDetected(visionText.text)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
