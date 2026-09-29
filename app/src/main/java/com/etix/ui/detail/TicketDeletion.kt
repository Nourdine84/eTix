package com.etix.ui.detail

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import com.etix.ui.main.MainActivityV2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Suppression d'UN ticket, TOUJOURS après confirmation (iOS : ConfirmDeletePopup). Partagé par le détail (lot 6,
 * comme iOS) et l'édition (existant, inchangé). Ferme le détail et l'édition ensuite.
 */
object TicketDeletion {
    fun confirm(fragment: Fragment, ticket: Ticket, repository: TicketRepository) {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle("Supprimer ce ticket ?")
            .setMessage("${ticket.store} — cette action est définitive.")
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Supprimer") { _, _ ->
                fragment.lifecycleScope.launch {
                    repository.delete(ticket)
                    (fragment.activity as? MainActivityV2)?.closeTicketFlow()
                        ?: fragment.parentFragmentManager.popBackStack()
                }
            }
            .show()
    }
}
