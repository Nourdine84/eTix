package com.etix.ui.budget

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.etix.data.BudgetStore
import com.etix.databinding.FragmentBudgetEditBinding
import com.etix.features.budget.BudgetRules

/**
 * Saisie du budget mensuel d'une catégorie — iOS BudgetEditSheet. « Appliquer » inactif tant que le montant n'est pas
 * valide (> 0). « Supprimer le budget » demande une confirmation (écart volontaire avec iOS, qui supprime
 * directement) puis retire le budget de CETTE catégorie seulement ; tickets et catégories intacts.
 * Confirmation : [DeleteBudgetDialog] (message défilant, boutons toujours entiers, y compris à 320 dp et police 2,0).
 */
class BudgetEditFragment : Fragment() {

    private var _binding: FragmentBudgetEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var category: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = requireArguments().getString(ARG).orEmpty()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBudgetEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val store = BudgetStore(requireContext())
        val current = store.limit(category)
        binding.tvBudgetEditTitle.text = category
        binding.tvBudgetEditName.text = category
        // Nom affiché une seule fois (iOS : titre + « Budget mensuel — nom ») : en-tête court « Budget mensuel »
        binding.tvBudgetEditHeader.text = "Budget mensuel"
        // Nom trop long pour la barre haute (à la taille de police choisie) : affiché en entier dans une zone fixe sous la
        // barre, toujours visible clavier ouvert ; « Annuler » et « Appliquer » restent entiers dans la barre.
        binding.budgetEditBar.onTitleFitChanged = { fits ->
            _binding?.let {
                it.tvBudgetEditTitle.visibility = if (fits) View.VISIBLE else View.INVISIBLE
                it.tvBudgetEditName.visibility = if (fits) View.GONE else View.VISIBLE
            }
        }
        // Clavier ouvert / fermé, message d'erreur affiché : contenu replacé sans texte rogné en haut
        val onHeightChange = View.OnLayoutChangeListener { v, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) v.post { keepFieldClear() }
        }
        binding.budgetEditScroll.addOnLayoutChangeListener(onHeightChange)
        binding.budgetEditContent.addOnLayoutChangeListener(onHeightChange)
        if (savedInstanceState == null) current?.let { binding.inputBudget.setText(BudgetRules.formatNumber(it)) }
        binding.btnDeleteBudget.visibility = if (current != null) View.VISIBLE else View.GONE

        fun refresh() {
            val raw = binding.inputBudget.text?.toString().orEmpty()
            val valid = BudgetRules.parseLimit(raw) != null
            binding.btnBudgetApply.isEnabled = valid
            binding.btnBudgetApply.alpha = if (valid) 1f else 0.4f
            binding.budgetInputLayout.error = if (raw.isNotBlank() && !valid) "Montant invalide" else null
        }
        refresh()
        binding.inputBudget.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun afterTextChanged(s: Editable?) = refresh()
        })
        binding.btnBudgetApply.setOnClickListener {
            BudgetRules.parseLimit(binding.inputBudget.text?.toString().orEmpty())?.let { store.set(category, it) }
            close()
        }
        binding.inputBudget.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE && binding.btnBudgetApply.isEnabled) {
                binding.btnBudgetApply.performClick(); true
            } else false
        }
        binding.btnBudgetCancel.setOnClickListener { close() }
        // Suppression du budget : confirmation explicite ; « Annuler » ne modifie rien. Seul le budget de CETTE
        // catégorie est retiré ; aucun ticket ni aucune catégorie n'est touché.
        binding.btnDeleteBudget.setOnClickListener {
            val limit = store.limit(category) ?: return@setOnClickListener
            hideKeyboard() // le clavier ne reste pas ouvert derrière la confirmation
            binding.inputBudget.clearFocus()
            DeleteBudgetDialog.show(requireContext(),
                "Le budget mensuel de ${BudgetRules.formatEuro(limit)} pour «\u00A0$category\u00A0» sera supprimé. " +
                    "Les tickets ne sont pas modifiés.") {
                store.set(category, null)
                close()
            }
        }
        binding.inputBudget.requestFocus()
        binding.inputBudget.setSelection(binding.inputBudget.text?.length ?: 0)
        binding.inputBudget.post {
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(_binding?.inputBudget ?: return@post, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * Champ de saisie (avec son message d'erreur) visible sans texte coupé au-dessus : tout le haut de la zone défilante
     * s'il tient avec le champ, sinon défilement jusqu'au champ, les lignes au-dessus entièrement sorties de la vue
     * (le défilement automatique vers le champ laissait l'en-tête à moitié visible). Le reste reste accessible en
     * faisant défiler.
     */
    private fun keepFieldClear() {
        val b = _binding ?: return
        if (!b.inputBudget.hasFocus()) return
        val scroll = b.budgetEditScroll
        val content = b.budgetEditContent
        val field = b.budgetInputLayout
        val fieldTop = content.top + field.top
        val fieldBottom = content.top + field.bottom
        val above = (field.layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
        val target = if (fieldBottom <= scroll.height) 0 else fieldTop - above
        // Contenu sous le champ trop court pour amener le champ en haut (défilement bloqué au maximum, nom coupé :
        // 320 dp, police 1,5 et 2,0, run 37907769754) : hauteur minimale temporaire, retirée quand tout tient
        val minHeight = if (target > 0) target + scroll.height - content.top else 0
        if (content.minimumHeight != minHeight) {
            content.minimumHeight = minHeight
            scroll.post { _binding?.budgetEditScroll?.smoothScrollTo(0, target) }
            return
        }
        // smoothScrollTo remplace un défilement animé en cours (curseur, focus)
        scroll.smoothScrollTo(0, target.coerceAtLeast(0))
    }

    private fun close() {
        hideKeyboard()
        parentFragmentManager.popBackStack()
    }

    private fun hideKeyboard() {
        val v = _binding?.inputBudget ?: return
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(v.windowToken, 0)
    }

    override fun onDestroyView() {
        hideKeyboard()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG = "budget_category"
        fun newInstance(category: String) = BudgetEditFragment().apply {
            arguments = Bundle().apply { putString(ARG, category) }
        }
    }
}
