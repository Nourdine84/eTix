package com.etix.ui.scan

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

/**
 * Lot 9 — parcours de scan (iOS ScannerFlowView) : intro → (autorisation caméra) → photo ou image →
 * lecture → résultat remis au formulaire « Ajouter », ou « Aucune information détectée » / erreur avec
 * « Réessayer ». Annuler, Retour ou « Saisir manuellement » ne créent aucun ticket : seul le bouton
 * « Enregistrer » du formulaire enregistre, après vérification par l'utilisateur.
 *
 * Écarts Android : capture par l'application appareil photo du système (iOS : VNDocumentCameraViewController)
 * et choix d'une image dans la galerie (absent d'iOS, demandé pour Android).
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

    private val askCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else { vm.showIntro(); showCameraDenied() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentScanFlowBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnTakePhoto.setOnClickListener { startCamera() }
        b.btnPickImage.setOnClickListener { launchPicker() }
        b.btnScanCancel.setOnClickListener { close() }
        b.btnPrimingAllow.setOnClickListener { requestCamera() }
        b.btnPrimingRefuse.setOnClickListener { vm.showIntro() }
        b.btnRetry.setOnClickListener { vm.showIntro() }
        b.btnManualEntry.setOnClickListener { close() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.step.collect(::render)
            }
        }
    }

    private fun render(step: ScanStep) {
        b.stepIntro.visibility = if (step is ScanStep.Intro) View.VISIBLE else View.GONE
        b.stepPriming.visibility = if (step is ScanStep.Priming) View.VISIBLE else View.GONE
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

    // ---------- Caméra (autorisation iOS : priming, puis demande système ; refus → explication + Paramètres) ----------

    private fun startCamera() {
        val ctx = requireContext()
        when {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
                launchCamera()
            !wasAsked(ctx) || shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> vm.showPriming()
            else -> showCameraDenied()   // refus définitif : seul le réglage système peut rétablir l'accès
        }
    }

    private fun requestCamera() {
        markAsked(requireContext())
        askCamera.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val ctx = requireContext()
        val dir = File(ctx.cacheDir, "scans").apply { mkdirs() }
        val file = File(dir, "ticket_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        vm.pendingPhoto = uri
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
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

    private fun showCameraDenied() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Accès caméra requis")
            .setMessage("Autorise l'accès à la caméra dans les paramètres de l'application pour scanner tes tickets. Tu peux aussi choisir une image.")
            .setPositiveButton("Paramètres") { _, _ ->
                runCatching {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().packageName, null)))
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun wasAsked(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)
    private fun markAsked(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ASKED, true).apply()

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
        private const val PREFS = "etix_scan"
        private const val KEY_ASKED = "camera_permission_asked"

        fun ReceiptScan.toBundle() = bundleOf(
            "store" to store.value, "storeConf" to store.confidence.name,
            "amount" to amount.value, "amountConf" to amount.confidence.name,
            "date" to date.value, "dateConf" to date.confidence.name,
            "category" to category?.category
        )
    }
}
