package com.etix.ui.budget

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.etix.data.BudgetStore
import com.etix.databinding.DialogBudgetEditBinding
import com.etix.features.budget.BudgetRules
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * iOS BudgetEditSheet : saisie du budget mensuel d'une catégorie. « Appliquer » inactif tant que le montant
 * n'est pas valide (> 0). « Supprimer le budget » (si un budget existe) retire le budget de CETTE catégorie seulement ;
 * aucun ticket ni aucune catégorie n'est modifié.
 */
object BudgetEditDialog {

    fun show(fragment: Fragment, category: String, store: BudgetStore): AlertDialog {
        val ctx = fragment.requireContext()
        val b = DialogBudgetEditBinding.inflate(LayoutInflater.from(ctx))
        val current = store.limit(category)
        b.tvBudgetEditHeader.text = "Budget mensuel — $category"
        current?.let { b.inputBudget.setText(BudgetRules.formatNumber(it)) }
        b.btnDeleteBudget.visibility = if (current != null) View.VISIBLE else View.GONE

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(category)
            .setView(b.root)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Appliquer") { _, _ ->
                BudgetRules.parseLimit(b.inputBudget.text?.toString().orEmpty())?.let { store.set(category, it) }
            }
            .create()
        b.btnDeleteBudget.setOnClickListener {
            store.set(category, null)
            dialog.dismiss()
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setOnShowListener {
            val apply = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            fun refresh() {
                val raw = b.inputBudget.text?.toString().orEmpty()
                val valid = BudgetRules.parseLimit(raw) != null
                apply.isEnabled = valid
                b.budgetInputLayout.error = if (raw.isNotBlank() && !valid) "Montant invalide" else null
            }
            refresh()
            b.inputBudget.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
                override fun afterTextChanged(s: Editable?) = refresh()
            })
            b.inputBudget.setOnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_DONE && apply.isEnabled) { apply.performClick(); true } else false
            }
            b.inputBudget.requestFocus()
            b.inputBudget.setSelection(b.inputBudget.text?.length ?: 0)
        }
        dialog.show()
        return dialog
    }
}
