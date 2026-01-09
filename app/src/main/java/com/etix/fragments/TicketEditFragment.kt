package com.etix.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.viewmodel.TicketEditViewModel
import com.etix.viewmodel.factory.TicketEditVMFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TicketEditFragment : Fragment() {

    private val args: TicketEditFragmentArgs by navArgs()

    private lateinit var editStore: EditText
    private lateinit var editAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var editDescription: EditText
    private lateinit var btnPickDate: Button
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var selectedDateMillis: Long = System.currentTimeMillis()

    private val viewModel: TicketEditViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        TicketEditVMFactory(TicketRepository(dao), args.ticketId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ticket_edit, container, false)

        editStore = view.findViewById(R.id.editStore)
        editAmount = view.findViewById(R.id.editAmount)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        editDescription = view.findViewById(R.id.editDescription)
        btnPickDate = view.findViewById(R.id.btnPickDate)
        btnSave = view.findViewById(R.id.btnSave)
        btnDelete = view.findViewById(R.id.btnDelete)

        observeTicket()
        setupDatePicker()
        setupActions()

        return view
    }

    private fun observeTicket() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ticket.collect { ticket ->
                    ticket ?: return@collect

                    editStore.setText(ticket.store)
                    editAmount.setText(ticket.amount.toString())
                    editDescription.setText(ticket.description ?: "")
                    selectedDateMillis = ticket.dateMillis
                    btnPickDate.text = formatDate(ticket.dateMillis)

                    // Sélection catégorie (simple)
                    val index = (0 until spinnerCategory.count)
                        .firstOrNull { spinnerCategory.getItemAtPosition(it).toString() == ticket.category }
                        ?: 0
                    spinnerCategory.setSelection(index)
                }
            }
        }
    }

    private fun setupDatePicker() {
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
            }

            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    cal.set(y, m, d, 0, 0, 0)
                    selectedDateMillis = cal.timeInMillis
                    btnPickDate.text = formatDate(selectedDateMillis)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupActions() {

        btnSave.setOnClickListener {
            val amount = editAmount.text.toString().toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(requireContext(), "Montant invalide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.updateTicket(
                store = editStore.text.toString(),
                amount = amount,
                category = spinnerCategory.selectedItem.toString(),
                description = editDescription.text.toString().ifBlank { null },
                dateMillis = selectedDateMillis
            ) {
                findNavController().navigateUp()
            }
        }

        btnDelete.setOnClickListener {
            viewModel.deleteTicket()
            findNavController().navigateUp()
        }
    }

    private fun formatDate(ms: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        return sdf.format(Date(ms))
    }
}
