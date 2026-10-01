package com.etix.features.ocr.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Reconnaissance de texte sur une image (iOS `TextRecognizer`). Exécutée hors du thread principal. */
fun interface ScanTextReader {
    suspend fun read(bitmap: Bitmap): String
}

/** ML Kit Text Recognition (modèle latin embarqué, sur l'appareil : aucune image envoyée sur un serveur). */
object MlKitTextReader : ScanTextReader {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun read(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { cont.resume(it.text) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
}

/** Points d'injection du scanner (tests Robolectric : lecteur simulé ; application : ML Kit). */
object ScanServices {
    @Volatile var reader: ScanTextReader = MlKitTextReader
}

/** Image choisie ou photographiée → Bitmap réduit (côté le plus long ≤ [maxSide]) et redressé (EXIF). */
object ScanImageLoader {

    class UnreadableImage(message: String) : Exception(message)

    fun load(context: Context, uri: Uri, maxSide: Int = 2048): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream renvoie null en mode « bornes seules » : seule l'absence de flux signifie « introuvable »
        val stream = resolver.openInputStream(uri) ?: throw UnreadableImage("image introuvable")
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw UnreadableImage("format d'image non reconnu")
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw UnreadableImage("image illisible")
        val rotation = try {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        } catch (_: Exception) { 0 }
        if (rotation == 0) return decoded
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height,
            Matrix().apply { postRotate(rotation.toFloat()) }, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }
}
