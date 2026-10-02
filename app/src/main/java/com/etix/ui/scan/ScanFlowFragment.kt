package com.etix.ui.scan

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.etix.R
import com.etix.databinding.FragmentScanFlowBinding
import com.etix.features.ocr.scan.ReceiptScan
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.launch
import java.io.File

/**
 * Lot 9 — parcours de scan (iOS ScannerFlowView) : intro → photo ou image →
 * lecture → résultat remis au formulaire « Ajouter », ou « Aucune information détectée » / erreur avec
 * « Réessayer ». Annuler, Retour ou « Saisir manuellement » ne créent aucun ticket : seul le bouton
 * « Enregistrer » du formulaire enregistre, après vérification par l'utilisateur.
 *
 * Écarts Android : capture par l'application appareil photo du système (iOS : VNDocumentCameraViewController)
 * et choix d'une image dans la galerie (absent d'iOS, demandé pour Android).
 *
 * Aucune autorisation demandée (02/10/2026) : la photo est prise par l'application appareil photo du système
 * (ACTION_IMAGE_CAPTURE), qui détient elle-même l'accès à la caméra. eTix ne déclare pas la permission CAMERA
 * (retirée du manifeste) : la déclarer obligerait à la demander, et Android refuse alors ACTION_IMAGE_CAPTURE
 * tant qu'elle n'est pas accordée. iOS demande l'accès car il capture lui-même (CameraPrimingView) : écart voulu.
 */
class ScanFlowFragment : Fragment() {

    private var _b: FragmentScanFlowBinding? = null
    private val b get() = _b!!
    private val vm: ScanFlowViewModel by viewModels()

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = vm.pendingPhoto
        vm.pendingPhoto = null
        if (ok && uri != null) vm.process(uri, deleteAfter = true) else vm.showIntro()
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.process(uri, deleteAfter = false) else vm.showIntro()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentScanFlowBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnTakePhoto.setOnClickListener { launchCamera() }
        b.btnPickImage.setOnClickListener { launchPicker() }
        b.btnScanCancel.setOnClickListener { close() }
        b.btnRetry.setOnClickListener { vm.showIntro() }
        b.btnManualEntry.setOnClickListener { close() }
        compactIfNeeded(b.stepIntro, b.introBar, listOf(b.btnPickImage, b.btnScanCancel), b.introCompactActions, b.imgScanFrame)
        compactIfNeeded(b.stepFailure, b.failureBar, listOf(b.btnManualEntry), b.failureCompactActions, null)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.step.collect(::render)
            }
        }
    }

    private fun render(step: ScanStep) {
        b.stepIntro.visibility = if (step is ScanStep.Intro) View.VISIBLE else View.GONE
        b.stepProcessing.visibility = if (step is ScanStep.Processing) View.VISIBLE else View.GONE
        b.stepFailure.visibility = if (step is ScanStep.NotFound || step is ScanStep.Failed) View.VISIBLE else View.GONE
        when (step) {
            is ScanStep.Processing -> renderPhases(step.phase)
            is ScanStep.NotFound -> {
                b.tvFailureTitle.text = "Aucune information détectée"
                b.tvFailureMessage.text = "Le ticket n'a pas pu être lu. Réessaie en cadrant mieux, ou saisis les informations manuellement."
            }
            is ScanStep.Failed -> {
                b.tvFailureTitle.text = when (step.reason) {
                    ScanStep.Reason.NO_CAMERA_APP -> "Appareil photo indisponible"
                    else -> "Lecture impossible"
                }
                b.tvFailureMessage.text = when (step.reason) {
                    ScanStep.Reason.UNREADABLE_IMAGE -> "Cette image n'a pas pu être ouverte. Réessaie avec une autre photo, ou saisis les informations manuellement."
                    ScanStep.Reason.RECOGNITION_ERROR -> "Le texte du ticket n'a pas pu être reconnu. Réessaie, ou saisis les informations manuellement."
                    ScanStep.Reason.NO_CAMERA_APP -> "Aucune application d'appareil photo n'est disponible. Choisis une image ou saisis les informations manuellement."
                }
            }
            is ScanStep.Ready -> deliver(step.scan)
            else -> Unit
        }
    }

    private fun renderPhases(phase: Int) {
        val ctx = requireContext()
        listOf(b.tvStep1, b.tvStep2, b.tvStep3).forEachIndexed { i, tv ->
            val label = tv.text.toString().removePrefix("✓ ").removePrefix("› ")
            tv.text = when {
                i < phase -> "✓ $label"
                i == phase -> "› $label"
                else -> label
            }
            tv.setTextColor(ContextCompat.getColor(ctx, if (i <= phase) R.color.v2_text_primary else R.color.v2_text_secondary))
            tv.contentDescription = label + when {
                i < phase -> ", terminé"
                i == phase -> ", en cours"
                else -> ", à venir"
            }
        }
    }

    // ---------- Petit écran / grande police ----------

    /**
     * Barre basse limitée à [MAX_BAR_FRACTION] de la hauteur de l'étape : au-delà (constaté à 320 dp / police 2,0,
     * barre ≈ 60 % de l'écran), seule l'action principale reste dans la barre ; les autres boutons passent en
     * haut du contenu défilant ([target], juste sous le titre) et l'illustration décorative est masquée.
     * Décidé une fois par création de la vue (rotation et changement de police recréent la vue).
     */
    private fun compactIfNeeded(step: View, bar: ViewGroup, secondary: List<View>, target: ViewGroup, decorative: View?) {
        step.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (step.height == 0 || bar.height == 0) return     // étape masquée : décision à son 1er affichage
                step.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (bar.height <= step.height * MAX_BAR_FRACTION) return
                secondary.forEach { v ->
                    (v.parent as ViewGroup).removeView(v)
                    target.addView(v)
                }
                target.visibility = View.VISIBLE
                decorative?.visibility = View.GONE
            }
        })
    }

    private fun launchCamera() {
        val ctx = requireContext()
        val dir = File(ctx.cacheDir, "scans").apply { mkdirs() }
        val file = File(dir, "ticket_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        vm.pendingPhoto = uri
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {   // aucune application appareil photo
            vm.pendingPhoto = null
            vm.fail(ScanStep.Reason.NO_CAMERA_APP)
        }
    }

    private fun launchPicker() {
        try {
            pickImage.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            vm.fail(ScanStep.Reason.UNREADABLE_IMAGE)
        }
    }

    // ---------- Sortie ----------

    /** Remet le résultat au formulaire « Ajouter » (rien n'est enregistré) et ferme le parcours. */
    private fun deliver(scan: ReceiptScan) {
        vm.consumed()
        parentFragmentManager.setFragmentResult(RESULT_KEY, scan.toBundle())
        (activity as? MainActivityV2)?.goToPage(MainActivityV2.PAGE_ADD) ?: parentFragmentManager.popBackStack()
    }

    /** Annuler / Saisir manuellement : aucun ticket créé, retour au formulaire « Ajouter ». */
    private fun close() {
        (activity as? MainActivityV2)?.goToPage(MainActivityV2.PAGE_ADD) ?: parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    companion object {
        const val RESULT_KEY = "scan_result"
        /** Part maximale de la hauteur occupée par la barre basse avant passage en mode compact. */
        const val MAX_BAR_FRACTION = 0.4f

        fun ReceiptScan.toBundle() = bundleOf(
            "store" to store.value, "storeConf" to store.confidence.name,
            "amount" to amount.value, "amountConf" to amount.confidence.name,
            "date" to date.value, "dateConf" to date.confidence.name,
            "category" to category?.category
        )
    }
}
