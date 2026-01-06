package com.etix.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.etix.R
import com.etix.data.AppDatabase
import com.etix.data.TicketRepository
import com.etix.model.Ticket
import com.etix.viewmodel.TicketEditViewModel
import com.etix.viewmodel.factory.TicketEditVMFactory
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class TicketEditFragment : Fragment() {

    private val args: TicketEditFragmentArgs by navArgs()

    private lateinit var editTextStoreName: EditText
    private lateinit var editTextAmount: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextCategory: EditText
    private lateinit var editTextDate: EditText
    private lateinit var btnSaveTicket: Button

    private var selectedMillis: Long? = null
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val viewModel: TicketEditViewModel by viewModels {
        val dao = AppDatabase.getInstance(requireContext()).ticketDao()
        TicketEditVMFactory(TicketRepository(dao), args.ticketId)
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

        observeTicket()
        return view
    }

    private fun observeTicket() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.ticket.collectLatest { t ->
                t ?: run {
                    showErrorPopup("Erreur", "Ticket introuvable.")
                    findNavController().navigateUp()
                    return@collectLatest
                }

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

        viewModel.updateTicket(
            store = store,
            amount = amount,
            category = category,
            description = description,
            dateMillis = millis
        ) {
            showSuccessPopup("Ticket mis à jour avec succès ✅")
            findNavController().navigateUp()
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

        dialogView.findViewById<TextView>(R.id.textMessage).text = message
        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showErrorPopup(title: String, message: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_error, null)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)

        dialogView.findViewById<TextView>(R.id.textTitle).text = title
        dialogView.findViewById<TextView>(R.id.textMessage).text = message
        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
