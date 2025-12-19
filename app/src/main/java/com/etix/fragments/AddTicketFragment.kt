package com.etix.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.model.Ticket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_add_ticket, container, false)

        storeInput = v.findViewById(R.id.editTextStore)
        dateInput = v.findViewById(R.id.editTextDate)
        amountInput = v.findViewById(R.id.editAmount)  // CHANGÉ : editTextAmount → editAmount
        categorySpinner = v.findViewById(R.id.spinnerCategory)
        descriptionInput = v.findViewById(R.id.editDescription)  // CHANGÉ : editTextDescription → editDescription
        buttonSave = v.findViewById(R.id.buttonSave)
        buttonQuickAdd = v.findViewById(R.id.buttonQuickAdd)

        val categories = arrayOf("Supermarché", "Restaurant", "Transport", "Santé", "Autre")
        categorySpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, categories
        )

        // Utilise l'EditText dateInput directement au lieu de btnPickDate
        dateInput.setOnClickListener { openDatePicker() }
        dateInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                openDatePicker()
                dateInput.clearFocus()
            }
        }

        buttonSave.setOnClickListener {
            val store = storeInput.text.toString().trim()
            val amount = amountInput.text.toString().replace(",", ".").toDoubleOrNull()
            val category = categorySpinner.selectedItem?.toString().orEmpty()
            val desc = descriptionInput.text.toString().trim()
            val millis = selectedMillis

            if (store.isEmpty() || millis == null || amount == null) {
                showErrorPopup("Erreur", "Veuillez remplir tous les champs obligatoires.")
                return@setOnClickListener
            }

            val ticket = Ticket(
                store = store,
                amount = amount,
                category = category.ifBlank { "Autre" },
                description = if (desc.isBlank()) null else desc,
                dateMillis = millis
            )

            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).ticketDao().insert(ticket)
                withContext(Dispatchers.Main) {
                    showSuccessPopup("Ticket enregistré avec succès ✅")
                    clearForm()
                }
            }
        }

        buttonQuickAdd.setOnClickListener {
            val now = Calendar.getInstance().timeInMillis
            val ticket = Ticket(
                store = "Ajout rapide",
                amount = 9.99,
                category = "Autre",
                description = "Créé automatiquement",
                dateMillis = now
            )

            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).ticketDao().insert(ticket)
                withContext(Dispatchers.Main) {
                    showSuccessPopup("Ajout rapide effectué ✅")
                    clearForm()
                }
            }
        }

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

    private fun clearForm() {
        storeInput.text.clear()
        dateInput.text.clear()
        amountInput.text.clear()
        descriptionInput.text.clear()
        categorySpinner.setSelection(0)
        selectedMillis = null
    }

    private fun showSuccessPopup(message: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_success, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        val textMsg = dialogView.findViewById<TextView>(R.id.textMessage)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        textMsg.text = message
        btnOk.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.show()
    }

    private fun showErrorPopup(title: String, message: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_error, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)

        val titleView = dialogView.findViewById<TextView>(R.id.textTitle)
        val textMsg = dialogView.findViewById<TextView>(R.id.textMessage)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

        titleView.text = title
        textMsg.text = message
        btnOk.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.show()
    }
}