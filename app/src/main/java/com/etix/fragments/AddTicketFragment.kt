package com.etix.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import com.etix.viewmodel.AddTicketViewModel
import com.etix.viewmodel.AddTicketViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class AddTicketFragment : Fragment() {

    private lateinit var storeInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var amountInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var descriptionInput: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonQuickAdd: Button

    private var selectedMillis: Long? = null
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val viewModel: AddTicketViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        val repo = TicketRepository(dao)
        AddTicketViewModelFactory(repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_add_ticket, container, false)

        storeInput = v.findViewById(R.id.editTextStore)
        dateInput = v.findViewById(R.id.editTextDate)
        amountInput = v.findViewById(R.id.editAmount)
        categorySpinner = v.findViewById(R.id.spinnerCategory)
        descriptionInput = v.findViewById(R.id.editDescription)
        buttonSave = v.findViewById(R.id.buttonSave)
        buttonQuickAdd = v.findViewById(R.id.buttonQuickAdd)

        val categories = arrayOf("Supermarché", "Restaurant", "Transport", "Santé", "Autre")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        categorySpinner.adapter = adapter

        dateInput.setOnClickListener { openDatePicker() }
        dateInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                openDatePicker()
                dateInput.clearFocus()
            }
        }

        buttonSave.setOnClickListener { saveTicket() }
        buttonQuickAdd.setOnClickListener { quickAdd() }

        observeViewModel()

        return v
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val set = Calendar.getInstance().apply {
                    set(y, m, d, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedMillis = set.timeInMillis
                dateInput.setText(df.format(set.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveTicket() {
        val store = storeInput.text.toString().trim()
        val amount = amountInput.text.toString().replace(",", ".").toDoubleOrNull()
        val category = categorySpinner.selectedItem?.toString().orEmpty()
        val desc = descriptionInput.text.toString().trim()
        val millis = selectedMillis

        if (store.isEmpty()) {
            showErrorPopup("Erreur", "Le nom du magasin est requis.")
            return
        }
        if (millis == null) {
            showErrorPopup("Erreur", "La date est obligatoire.")
            return
        }
        if (amount == null || amount <= 0) {
            showErrorPopup("Erreur", "Le montant doit être supérieur à 0.")
            return
        }

        val ticket = Ticket(
            store = store,
            amount = amount,
            category = category.ifBlank { "Autre" },
            description = desc.ifBlank { null },
            dateMillis = millis
        )

        viewModel.insertTicket(ticket)
    }

    private fun quickAdd() {
        val ticket = Ticket(
            store = "Ajout rapide",
            amount = 9.99,
            category = "Autre",
            description = "Créé automatiquement",
            dateMillis = System.currentTimeMillis()
        )
        viewModel.insertTicket(ticket)
    }

    private fun observeViewModel() {
        viewModel.saving.observe(viewLifecycleOwner) { saving ->
            buttonSave.isEnabled = !saving
            buttonQuickAdd.isEnabled = !saving
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showErrorPopup("Erreur", it)
            }
        }

        viewModel.saved.observe(viewLifecycleOwner) {
            showSuccessPopup("Ticket enregistré avec succès ✅")
            clearForm()
        }
    }

    private fun clearForm() {
        storeInput.text.clear()
        dateInput.text.clear()
        amountInput.text.clear()
        descriptionInput.text.clear()
        categorySpinner.setSelection(0)
        selectedMillis = null
    }

    private fun showSuccessPopup(message: String) {
        if (!isAdded) return
        val dialogView = layoutInflater.inflate(R.layout.popup_success, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)
        dialogView.findViewById<TextView>(R.id.textMessage).text = message
        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showErrorPopup(title: String, message: String) {
        if (!isAdded) return
        val dialogView = layoutInflater.inflate(R.layout.popup_error, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialogView.findViewById<TextView>(R.id.textTitle).text = title
        dialogView.findViewById<TextView>(R.id.textMessage).text = message
        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}
