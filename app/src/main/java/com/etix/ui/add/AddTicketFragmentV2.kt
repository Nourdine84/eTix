package com.etix.ui.add

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentAddTicketV2Binding
import com.etix.features.ocr.engine.OCRCategoryGuesser
import com.etix.features.ocr.scan.ScanCategoryResolver
import com.etix.features.ocr.scan.ScanConfidence
import com.etix.ui.main.MainActivityV2
import com.etix.ui.scan.ScanFlowFragment
import kotlinx.coroutines.flow.first
import com.etix.model.Ticket
import com.etix.ui.ticket.TicketFormController
import kotlinx.coroutines.launch

/**
 * Ajouter un ticket — référence iOS AddTicketView : magasin, montant, date, catégorie, description.
 * Validation iOS : magasin non vide + montant > 0 (virgule acceptée). Après enregistrement : formulaire
 * réinitialisé, on reste sur l'écran (décision produit iOS, ROADMAP « retour Home différé »).
 * Lot 9 : « Scanner un ticket » ouvre le parcours de scan ; son résultat préremplit ce formulaire (marques
 * « Détecté » / « À vérifier » / « Non lu ») sans rien enregistrer ; « Annuler le scan » vide le formulaire.
 * Enregistrement par [AddTicketSaveViewModel] : un seul ticket par validation, même en cas de double appui ou de
 * recréation de l'écran pendant l'insertion. Corrections, marques et résultat de scan en attente conservés à la
 * recréation de l'écran.
 */
class AddTicketFragmentV2 : Fragment() {

    private var _binding: FragmentAddTicketV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TicketRepository
    private lateinit var form: TicketFormController
    private var usedCategories: List<String> = emptyList()
    private val saver: AddTicketSaveViewModel by viewModels()
    /** Résultat de scan reçu mais pas encore appliqué (lecture de l'historique en cours) : conservé si l'écran est recréé. */
    private var pendingScan: Bundle? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTicketV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())
        form = TicketFormController(this, binding.form) { usedCategories }
        savedInstanceState?.let {
            form.restore(it.getLong(KEY_DATE, System.currentTimeMillis()), it.getString(KEY_CATEGORY).orEmpty())
            form.restoreScanMarks(it)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.distinctCategoriesFlow().collect { usedCategories = it }
            }
        }

        binding.form.inputAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) { form.setAmountInvalid(false) }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Lot 9 : résultat du scan → formulaire prérempli, à vérifier ; rien n'est enregistré sans « Enregistrer »
        parentFragmentManager.setFragmentResultListener(ScanFlowFragment.RESULT_KEY, viewLifecycleOwner) { _, bundle ->
            applyScan(bundle)
        }
        binding.btnScanTicket.setOnClickListener { (activity as? MainActivityV2)?.openScanFlow() }
        binding.btnDiscardScan.setOnClickListener {
            form.reset()
            binding.scanBanner.visibility = View.GONE
        }
        if (savedInstanceState?.getBoolean(KEY_SCAN_BANNER) == true) binding.scanBanner.visibility = View.VISIBLE
        savedInstanceState?.getBundle(KEY_PENDING_SCAN)?.let { applyScan(it) }

        binding.btnSaveTicket.setOnClickListener { save() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { saver.saving.collect { binding.btnSaveTicket.isEnabled = !it } }
                saver.results.collect(::onSaveResult)
            }
        }
    }

    private fun applyScan(bundle: Bundle) {
        pendingScan = bundle
        fun conf(key: String) = runCatching { ScanConfidence.valueOf(bundle.getString(key).orEmpty()) }
            .getOrDefault(ScanConfidence.NONE)
        val store = bundle.getString("store")
        val amount = if (bundle.get("amount") != null) bundle.getDouble("amount") else null
        val date = if (bundle.get("date") != null) bundle.getLong("date") else null
        val ocrCategory = bundle.getString("category")
        viewLifecycleOwner.lifecycleScope.launch {
            // iOS StoreCategoryMapper : historique du magasin (lecture seule), puis catégorie lue par l'OCR
            val existing = repository.getAllFlow().first()
            val suggestion = ScanCategoryResolver.resolve(store,
                ocrCategory?.let { OCRCategoryGuesser.Guess(it, OCRCategoryGuesser.Source.STORE_DICTIONARY) }, existing)
            if (_binding == null) return@launch
            pendingScan = null
            form.applyScan(store, conf("storeConf"), amount, conf("amountConf"), date, conf("dateConf"),
                suggestion?.category, suggestion?.showBadge ?: false)
            binding.scanBanner.visibility = View.VISIBLE
            binding.scrollViewAdd.scrollTo(0, 0)
        }
    }

    private fun save() {
        val v = form.read()
        if (v.store.isEmpty() || v.amount == null) {
            form.setAmountInvalid(v.amount == null)
            Toast.makeText(requireContext(), "Impossible d'enregistrer. Vérifie le magasin et le montant.", Toast.LENGTH_LONG).show()
            return
        }
        val ticket = Ticket(store = v.store, amount = v.amount, dateMillis = v.dateMillis,
            category = v.category, description = v.description)
        saver.save(ticket)   // ignoré si un enregistrement est déjà en cours (double appui)
    }

    private fun onSaveResult(r: AddTicketSaveViewModel.Result) {
        when (r) {
            AddTicketSaveViewModel.Result.Saved -> {
                Toast.makeText(requireContext(), "Ticket enregistré", Toast.LENGTH_SHORT).show()
                form.reset()
                binding.scanBanner.visibility = View.GONE
            }
            AddTicketSaveViewModel.Result.Failed ->
                Toast.makeText(requireContext(), "Le ticket n'a pas pu être enregistré. Réessaie.", Toast.LENGTH_LONG).show()
        }
        saver.acknowledge()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::form.isInitialized && _binding != null) {
            outState.putLong(KEY_DATE, form.dateMillis)
            outState.putString(KEY_CATEGORY, form.category)
            outState.putBoolean(KEY_SCAN_BANNER, binding.scanBanner.visibility == View.VISIBLE)
            form.saveScanMarks(outState)
        }
        pendingScan?.let { outState.putBundle(KEY_PENDING_SCAN, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_DATE = "add_date"
        private const val KEY_CATEGORY = "add_category"
        private const val KEY_SCAN_BANNER = "add_scan_banner"
        private const val KEY_PENDING_SCAN = "add_pending_scan"
    }
}
