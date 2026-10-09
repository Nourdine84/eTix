package com.etix.ui.budget

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.etix.databinding.DialogDeleteBudgetBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Confirmation de suppression d'un budget (écart volontaire avec iOS, qui supprime directement). Fenêtre Material,
 * contenu propre : titre et message défilants, « Annuler » et « Supprimer » toujours entièrement visibles (côte à côte,
 * ou empilés s'ils ne tiennent pas). Constaté à 320 dp, police 2,0 avec les boutons standard : la barre de boutons
 * défilait, « Annuler » n'était visible qu'à 47 % et le message était coupé.
 * « Annuler » (ou retour) ferme sans rien modifier ; « Supprimer » appelle [onDelete].
 */
internal object DeleteBudgetDialog {

    fun show(context: Context, message: String, onDelete: () -> Unit): AlertDialog {
        val b = DialogDeleteBudgetBinding.inflate(LayoutInflater.from(context))
        b.confirmMessage.text = message
        val dialog = MaterialAlertDialogBuilder(context).setView(b.root).create()
        b.btnConfirmCancel.setOnClickListener { dialog.dismiss() }
        b.btnConfirmDelete.setOnClickListener { dialog.dismiss(); onDelete() }
        dialog.show()
        return dialog
    }
}
