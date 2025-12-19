package com.etix.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.model.Ticket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TicketEditFragment : Fragment() {

    private var ticketId: Long = 0L
    private var currentTicket: Ticket? = null

    private lateinit var editTextStoreName: EditText
    private lateinit var editTextAmount: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextCategory: EditText
    private lateinit var editTextDate: EditText
    private lateinit var btnSaveTicket: Button

    private var selectedMillis: Long? = null
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticketId = arguments?.getLong("ticketId") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ticket_edit, container, false)

        editTextStoreName = view.findViewById(R.id.editTextStoreName)
        editTextAmount = view.findViewById(R.id.editTextAmount)
        editTextDate = view.findViewById(R.id.editTextDate)
        editTextCategory = view.findViewById(R.id.editTextCategory)
        editTextDescription = view.findViewById(R.id.editTextDescription)
        btnSaveTicket = view.findViewById(R.id.btnSaveTicket)

        editTextDate.setOnClickListener { openDatePicker() }
        btnSaveTicket.setOnClickListener { saveTicket() }

        loadTicket()
        return view
    }

    private fun loadTicket() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(requireContext()).ticketDao()
            val t = dao.getById(ticketId)
            withContext(Dispatchers.Main) {
                if (t == null) {
                    showErrorPopup("Erreur", "Ticket introuvable.")
                    findNavController().navigateUp()
                    return@withContext
                }

                currentTicket = t
                selectedMillis = t.dateMillis

                editTextStoreName.setText(t.store)
                editTextAmount.setText(t.amount.toString())
                editTextCategory.setText(t.category)
                editTextDescription.setText(t.description ?: "")
                editTextDate.setText(df.format(Date(t.dateMillis)))
            }
        }
    }

    private fun saveTicket() {
        val store = editTextStoreName.text.toString().trim()
        val amount = editTextAmount.text.toString().replace(",", ".").toDoubleOrNull()
        val category = editTextCategory.text.toString().trim()
        val description = editTextDescription.text.toString().trim()
        val millis = selectedMillis

        if (store.isEmpty() || amount == null || millis == null) {
            showErrorPopup("Erreur", "Tous les champs doivent être remplis.")
            return
        }

        val updated = currentTicket?.copy(
            store = store,
            amount = amount,
            category = category.ifBlank { "Autre" },
            description = if (description.isBlank()) null else description,
            dateMillis = millis
        ) ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getInstance(requireContext()).ticketDao().update(updated)
            withContext(Dispatchers.Main) {
                showSuccessPopup("Ticket mis à jour avec succès ✅")
                findNavController().navigateUp()
            }
        }
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        selectedMillis?.let { cal.timeInMillis = it }

        DatePickerDialog(requireContext(), { _, y, m, d ->
            val selected = Calendar.getInstance().apply {
                set(y, m, d, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedMillis = selected.timeInMillis
            editTextDate.setText(df.format(selected.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showSuccessPopup(message: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_success, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.setCancelable(true)

        val msg = dialogView.findViewById<TextView>(R.id.textMessage)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

        msg.text = message
        btnOk.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.show()
    }
}