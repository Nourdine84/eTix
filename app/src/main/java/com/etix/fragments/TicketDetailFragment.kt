package com.etix.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.viewmodel.TicketDetailViewModel
import com.etix.viewmodel.factory.TicketDetailVMFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketDetailFragment : Fragment() {

    private val args: TicketDetailFragmentArgs by navArgs()

    private lateinit var tvStoreName: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button

    private var actionLocked = false

    private val viewModel: TicketDetailViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        TicketDetailVMFactory(TicketRepository(dao), args.ticketId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ticket_detail, container, false)

        tvStoreName = view.findViewById(R.id.tvStoreName)
        tvAmount = view.findViewById(R.id.tvAmount)
        tvDate = view.findViewById(R.id.tvDate)
        tvCategory = view.findViewById(R.id.tvCategory)
        tvDescription = view.findViewById(R.id.tvDescription)
        btnEdit = view.findViewById(R.id.btnEditTicket)
        btnDelete = view.findViewById(R.id.btnDeleteTicket)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ticket.collectLatest { ticket ->
                ticket ?: return@collectLatest

                tvStoreName.text = "Magasin : ${ticket.store}"
                tvAmount.text =
                    "Montant : ${NumberFormat.getCurrencyInstance(Locale.FRANCE).format(ticket.amount)}"
                tvDate.text = "Date : ${formatDate(ticket.dateMillis)}"
                tvCategory.text = "Catégorie : ${ticket.category}"
                tvDescription.text = "Description : ${ticket.description ?: "-"}"

                btnEdit.setOnClickListener {
                    if (actionLocked) return@setOnClickListener
                    actionLocked = true

                    val action =
                        TicketDetailFragmentDirections.actionDetailToEdit(ticket.id)
                    findNavController().navigate(action)
                }

                btnDelete.setOnClickListener {
                    if (actionLocked) return@setOnClickListener
                    actionLocked = true

                    showDeleteConfirmation {
                        viewModel.delete()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actionLocked = false
    }

    private fun showDeleteConfirmation(onConfirm: () -> Unit) {
        // On réutilise popup_error (tu l’as déjà)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_error, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        dialogView.findViewById<TextView>(R.id.textTitle).text = "Supprimer le ticket"
        dialogView.findViewById<TextView>(R.id.textMessage).text =
            "Cette action est définitive. Continuer ?"

        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
