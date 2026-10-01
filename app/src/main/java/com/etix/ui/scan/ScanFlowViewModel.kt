package com.etix.ui.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.etix.features.ocr.scan.ReceiptScan
import com.etix.features.ocr.scan.ReceiptScanParser
import com.etix.features.ocr.scan.ScanImageLoader
import com.etix.features.ocr.scan.ScanServices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Étapes du parcours (iOS ScannerStep) ; READY = ticket lu, à remettre au formulaire. */
sealed interface ScanStep {
    data object Intro : ScanStep
    data object Priming : ScanStep
    data class Processing(val phase: Int) : ScanStep   // 0 image, 1 texte, 2 extraction
    data object NotFound : ScanStep
    data class Failed(val reason: Reason) : ScanStep
    data class Ready(val scan: ReceiptScan) : ScanStep

    enum class Reason { UNREADABLE_IMAGE, RECOGNITION_ERROR, NO_CAMERA_APP }
}

/**
 * Lot 9 — état du parcours de scan, conservé à la rotation. Lecture hors du thread principal ; annulée si
 * l'utilisateur quitte. Aucune écriture en base : le résultat est remis au formulaire, que l'utilisateur valide.
 */
class ScanFlowViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {

    private val _step = MutableStateFlow<ScanStep>(ScanStep.Intro)
    val step: StateFlow<ScanStep> = _step
    private var job: Job? = null

    /** Fichier de la photo en cours (caméra), conservé si l'activité est recréée pendant la prise de vue. */
    var pendingPhoto: Uri?
        get() = saved[KEY_PHOTO]
        set(v) { saved[KEY_PHOTO] = v }

    fun showIntro() { job?.cancel(); _step.value = ScanStep.Intro }
    fun showPriming() { _step.value = ScanStep.Priming }
    fun fail(reason: ScanStep.Reason) { _step.value = ScanStep.Failed(reason) }

    fun process(uri: Uri, deleteAfter: Boolean) {
        job?.cancel()
        _step.value = ScanStep.Processing(0)
        job = viewModelScope.launch {
            val ctx = getApplication<Application>()
            try {
                val bitmap = withContext(Dispatchers.IO) { ScanImageLoader.load(ctx, uri) }
                _step.value = ScanStep.Processing(1)
                val text = ScanServices.reader.read(bitmap)
                _step.value = ScanStep.Processing(2)
                val scan = withContext(Dispatchers.Default) { ReceiptScanParser.parse(text) }
                _step.value = if (scan.isEmpty) ScanStep.NotFound else ScanStep.Ready(scan)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ScanImageLoader.UnreadableImage) {
                fail("image illisible", e, ScanStep.Reason.UNREADABLE_IMAGE)
            } catch (e: java.io.IOException) {          // fichier absent, accès refusé par l'application source
                fail("image inaccessible", e, ScanStep.Reason.UNREADABLE_IMAGE)
            } catch (e: SecurityException) {
                fail("image inaccessible", e, ScanStep.Reason.UNREADABLE_IMAGE)
            } catch (e: OutOfMemoryError) {
                fail("image trop lourde", e, ScanStep.Reason.UNREADABLE_IMAGE)
            } catch (e: Exception) {
                fail("reconnaissance", e, ScanStep.Reason.RECOGNITION_ERROR)
            } finally {
                // Photo temporaire (cache de l'app) : supprimée après lecture. Une image choisie n'est jamais touchée.
                if (deleteAfter) withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                    runCatching { ctx.contentResolver.delete(uri, null, null) }
                }
            }
        }
    }

    /** Échec consigné dans le journal (diagnostic, sans le contenu du ticket) puis affiché. */
    private fun fail(what: String, e: Throwable, reason: ScanStep.Reason) {
        android.util.Log.w("eTixScan", "échec du scan ($what) : ${e.javaClass.simpleName}")
        _step.value = ScanStep.Failed(reason)
    }

    /** Le résultat a été remis au formulaire : ne pas le remettre une 2e fois (rotation). */
    fun consumed() { _step.value = ScanStep.Intro }

    companion object { private const val KEY_PHOTO = "scan_pending_photo" }
}
