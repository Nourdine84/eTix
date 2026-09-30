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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Saisie du budget mensuel d'une catégorie — iOS BudgetEditSheet. « Appliquer » inactif tant que le montant n'est pas
 * valide (> 0). « Supprimer le budget » demande une confirmation (écart volontaire avec iOS, qui supprime
 * directement) puis retire le budget de CETTE catégorie seulement ; tickets et catégories intacts.
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
        binding.tvBudgetEditHeader.text = "Budget mensuel — $category"
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
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer le budget ?")
                .setMessage("Le budget mensuel de ${BudgetRules.formatEuro(limit)} pour « $category » sera supprimé. " +
                    "Les tickets ne sont pas modifiés.")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Supprimer") { _, _ ->
                    store.set(category, null)
                    close()
                }
                .show()
        }
        binding.inputBudget.requestFocus()
        binding.inputBudget.setSelection(binding.inputBudget.text?.length ?: 0)
        binding.inputBudget.post {
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(_binding?.inputBudget ?: return@post, InputMethodManager.SHOW_IMPLICIT)
        }
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
