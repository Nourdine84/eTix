package com.etix.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.databinding.FragmentTicketDetailV2Binding
import com.etix.features.category.CategoryStats
import com.etix.features.ticket.TicketDetailFormat
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import kotlinx.coroutines.launch

/**
 * Détail d'un ticket (lot 6) — référence iOS TicketDetailView : montant, carte date, Magasin / Catégorie, note,
 * Modifier, Supprimer (confirmation obligatoire). Lecture seule hors suppression confirmée.
 */
class TicketDetailFragmentV2 : Fragment() {

    private var _binding: FragmentTicketDetailV2Binding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TicketRepository
    private var ticketId: Long = 0L
    private var current: Ticket? = null

    companion object {
        fun newInstance(ticketId: Long) = TicketDetailFragmentV2().apply {
            arguments = Bundle().apply { putLong("ticketId", ticketId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticketId = requireArguments().getLong("ticketId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTicketDetailV2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = TicketRepository(AppDatabase.getInstance(requireContext()).ticketDao())

        // viewLifecycleOwner : la collecte s'arrête quand la vue est détruite (sinon NPE après édition)
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getByIdFlow(ticketId).collect { ticket ->
                ticket ?: return@collect
                current = ticket
                bind(ticket)
            }
        }

        adaptToLargeFont()
        binding.btnDetailBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnEdit.setOnClickListener { (activity as? MainActivityV2)?.openTicketEdit(ticketId) }
        binding.btnDeleteTicket.setOnClickListener {
            current?.let { TicketDeletion.confirm(this, it, repository) }
        }
    }

    /**
     * Grande police ou écran étroit (largeur dp / taille de police < 300) : cartes empilées au lieu de côte à côte.
     * Sinon « DATE D'ACHAT », « MAGASIN », « CATÉGORIE » et le jour se coupent lettre par lettre (constaté sur émulateur :
     * 360 dp police 2,0, run 36632203892 ; 320 dp police 1,3, run 36634872566).
     * Adaptation Android ; iOS garde deux colonnes.
     */
    private fun adaptToLargeFont() {
        // Largeur disponible exprimée « en caractères » : 360 dp à 1,3 → 277 ; 320 dp à 1,3 → 246 (libellés coupés constatés)
        val cfg = resources.configuration
        if (cfg.screenWidthDp / cfg.fontScale >= 300f) return
        val dp = resources.displayMetrics.density
        with(binding) {
            dateRow.orientation = LinearLayout.VERTICAL
            dateRow.gravity = android.view.Gravity.START
            dateDivider.visibility = View.GONE
            dateRight.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = (16 * dp).toInt() }
            infoRow.orientation = LinearLayout.VERTICAL
            for (i in 0 until infoRow.childCount) {
                infoRow.getChildAt(i).layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply { if (i > 0) topMargin = (16 * dp).toInt() }
            }
        }
    }

    private fun bind(t: Ticket) {
        val amount = TicketDetailFormat.amount(t.amount)
        binding.tvAmount.text = amount
        binding.tvAmount.contentDescription = "Montant $amount"
        binding.tvStore.text = t.store
        binding.tvStoreValue.text = t.store
        binding.tvDay.text = TicketDetailFormat.day(t.dateMillis)
        binding.tvMonthYear.text = TicketDetailFormat.monthYear(t.dateMillis)
        binding.tvWeekday.text = TicketDetailFormat.weekday(t.dateMillis)
        binding.tvCategory.text = CategoryStats.displayName(t.category)
        val note = t.description?.trim().orEmpty()
        binding.tvDescription.text = note
        binding.cardNote.visibility = if (note.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
