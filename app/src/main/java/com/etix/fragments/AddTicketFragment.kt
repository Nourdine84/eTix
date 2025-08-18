package com.etix.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.etix.R
import com.etix.model.Ticket
import com.etix.viewmodel.AddTicketViewModel
import com.etix.viewmodel.factory.AddTicketVMFactory
import com.etix.data.TicketRepository
import java.util.*

class AddTicketFragment : Fragment() {

    private val viewModel: AddTicketViewModel by viewModels {
        AddTicketVMFactory(TicketRepository(requireContext()))
    }

    private lateinit var storeInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var descriptionInput: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonQuickAdd: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_add_ticket, container, false)

        storeInput = view.findViewById(R.id.editTextStore)
        dateInput = view.findViewById(R.id.editTextDate)
        amountInput = view.findViewById(R.id.editTextAmount)
        categorySpinner = view.findViewById(R.id.spinnerCategory)
        descriptionInput = view.findViewById(R.id.editTextDescription)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonQuickAdd = view.findViewById(R.id.buttonQuickAdd)

        val categories = arrayOf("Supermarché", "Restaurant", "Transport", "Santé", "Autre")
        categorySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        dateInput.setOnClickListener { showDatePicker() }
        dateInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        viewModel.saving.observe(viewLifecycleOwner) {
            buttonSave.isEnabled = !it
            buttonQuickAdd.isEnabled = !it
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Ticket ajouté avec succès", Toast.LENGTH_SHORT).show()
                clearForm()
            }
        }

        buttonSave.setOnClickListener {
            val ticket = buildTicketOrNull() ?: return@setOnClickListener
            viewModel.addTicket(ticket)
        }

        buttonQuickAdd.setOnClickListener {
            val ticket = Ticket(
                store = "Test",
                date = "2025-08-14",
                amount = 9.99,
                category = "Autre",
                description = "Ajout rapide"
            )
            viewModel.addTicket(ticket)
        }

        return view
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val dlg = DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val month = (m + 1).toString().padStart(2, '0')
                val day = d.toString().padStart(2, '0')
                dateInput.setText("$y-$month-$day")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dlg.show()
    }

    private fun buildTicketOrNull(): Ticket? {
        val store = storeInput.text.toString().trim()
        val date = dateInput.text.toString().trim()
        val amount = amountInput.text.toString().toDoubleOrNull()
        val category = categorySpinner.selectedItem?.toString().orEmpty()
        val description = descriptionInput.text.toString().trim()

        if (store.isEmpty() || date.isEmpty() || amount == null) {
            Toast.makeText(requireContext(), "Tous les champs obligatoires doivent être remplis", Toast.LENGTH_SHORT).show()
            return null
        }

        return Ticket(
            store = store,
            date = date,
            amount = amount,
            category = category,
            description = description
        )
    }

    private fun clearForm() {
        storeInput.text.clear()
        dateInput.text.clear()
        amountInput.text.clear()
        descriptionInput.text.clear()
        categorySpinner.setSelection(0)
    }
}
