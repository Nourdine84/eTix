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

/**
 * Image choisie ou photographiée → Bitmap redressé (orientation EXIF) et réduit à [MAX_PIXELS] pixels au plus.
 *
 * Réduction par nombre de pixels (lot 9, 02/10/2026) et non plus par côté : l'ancienne règle (côté ≤ 2048 par
 * puissances de 2 seulement) laissait une photo 4000 × 3000 en pleine résolution (48 Mo en mémoire), et un
 * ticket long (ex. 1000 × 8000) aurait été réduit à 256 px de large, texte illisible. Décodage sous-échantillonné
 * (puissance de 2, ≤ 4 × le budget), puis mise à l'échelle exacte.
 */
object ScanImageLoader {

    class UnreadableImage(message: String) : Exception(message)

    /** ≈ 2000 × 2000 : texte d'un ticket photographié lisible, 16 Mo au plus en mémoire (ARGB_8888). */
    const val MAX_PIXELS = 4_000_000L

    fun load(context: Context, uri: Uri, maxPixels: Long = MAX_PIXELS): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream renvoie null en mode « bornes seules » : seule l'absence de flux signifie « introuvable »
        val stream = resolver.openInputStream(uri) ?: throw UnreadableImage("image introuvable")
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw UnreadableImage("format d'image non reconnu")
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth.toLong(), bounds.outHeight.toLong(), maxPixels)
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw UnreadableImage("image illisible")
        val rotation = try {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        } catch (_: Exception) { 0 }
        val px = decoded.width.toLong() * decoded.height
        val scale = if (px > maxPixels) Math.sqrt(maxPixels.toDouble() / px).toFloat() else 1f
        if (rotation == 0 && scale == 1f) return decoded
        val m = Matrix().apply { postScale(scale, scale); postRotate(rotation.toFloat()) }
        val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        if (out !== decoded) decoded.recycle()
        return out
    }

    /** Plus grande puissance de 2 laissant au moins [maxPixels] pixels (la mise à l'échelle exacte suit). */
    internal fun sampleSize(w: Long, h: Long, maxPixels: Long): Int {
        var sample = 1
        while ((w / (sample * 2)) * (h / (sample * 2)) >= maxPixels) sample *= 2
        return sample
    }
}
