package com.etix.ui.ticket

import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.etix.R
import com.etix.databinding.ViewTicketFormBinding
import com.etix.features.ticket.TicketFormRules
import com.etix.model.Ticket
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DateFormat
import java.util.Date

/**
 * Contrôleur du formulaire ticket partagé (Ajout / Édition) — iOS TicketForm + CategoryPickerSheet.
 * Aucune écriture en base : l'écran appelant enregistre à partir de [read].
 */
class TicketFormController(
    private val fragment: Fragment,
    private val b: ViewTicketFormBinding,
    private val usedCategories: () -> List<String>
) {
    var dateMillis: Long = System.currentTimeMillis()
        private set
    var category: String = ""
        private set

    data class Values(val store: String, val amount: Double?, val dateMillis: Long, val category: String, val description: String?)

    init {
        b.inputDate.setOnClickListener { pickDate() }
        b.rowCategory.setOnClickListener { pickCategory() }
        render()
    }

    fun fill(t: Ticket) {
        b.inputStore.setText(t.store)
        b.inputAmount.setText(TicketFormRules.formatAmountForInput(t.amount))
        b.inputDescription.setText(t.description.orEmpty())
        dateMillis = t.dateMillis
        category = if (t.category == TicketFormRules.DEFAULT_CATEGORY) "" else t.category
        render()
    }

    fun restore(date: Long, cat: String) {
        dateMillis = date; category = cat; render()
    }

    fun reset() {
        b.inputStore.setText(""); b.inputAmount.setText(""); b.inputDescription.setText("")
        dateMillis = System.currentTimeMillis(); category = ""
        setAmountInvalid(false)
        render()
    }

    fun read() = Values(
        store = b.inputStore.text.toString().trim(),
        amount = TicketFormRules.parseAmount(b.inputAmount.text.toString()),
        dateMillis = dateMillis,
        category = category.trim().ifEmpty { TicketFormRules.DEFAULT_CATEGORY },
        description = b.inputDescription.text.toString().trim().ifEmpty { null }
    )

    /** iOS : montant en rouge quand invalide, redevient normal dès la correction. */
    fun setAmountInvalid(invalid: Boolean) {
        b.inputAmount.setTextColor(ContextCompat.getColor(b.root.context,
            if (invalid) R.color.v2_negative else R.color.v2_text_primary))
    }

    private fun render() {
        b.inputDate.text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(dateMillis))
        b.inputDate.contentDescription = "Date : ${b.inputDate.text}. Modifier"
        val ctx = b.root.context
        if (category.isEmpty()) {
            b.tvCategoryValue.text = "Choisir une catégorie"
            b.tvCategoryValue.setTextColor(ContextCompat.getColor(ctx, R.color.v2_text_secondary))
        } else {
            b.tvCategoryValue.text = category
            b.tvCategoryValue.setTextColor(ContextCompat.getColor(ctx, R.color.v2_text_primary))
        }
        b.rowCategory.contentDescription = "Catégorie : ${b.tvCategoryValue.text}"
    }

    private fun pickDate() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Date du ticket")
            .setSelection(TicketFormRules.toPickerSelection(dateMillis))
            .build()
        picker.addOnPositiveButtonClickListener { sel ->
            dateMillis = TicketFormRules.combineDay(sel, dateMillis)
            render()
        }
        picker.show(fragment.childFragmentManager, "ticket_date")
    }

    /** iOS CategoryPickerSheet : système + utilisées, « Autre… » (saisie libre), « Effacer », « Fermer ». */
    private fun pickCategory() {
        val ctx = fragment.requireContext()
        val items = TicketFormRules.pickerCategories(usedCategories())
        val checked = items.indexOf(category)
        val builder = MaterialAlertDialogBuilder(ctx)
            .setTitle("Catégorie")
            .setSingleChoiceItems(items.toTypedArray(), checked) { d, which ->
                category = items[which]; render(); d.dismiss()
            }
            .setNeutralButton("Autre…") { _, _ -> pickCustomCategory() }
            .setNegativeButton("Fermer", null)
        if (category.isNotEmpty()) {
            builder.setPositiveButton("Effacer") { _, _ -> category = ""; render() }
        }
        builder.show()
    }

    private fun pickCustomCategory() {
        val ctx = fragment.requireContext()
        val input = EditText(ctx).apply {
            hint = "Nom de la catégorie"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            maxLines = 1
        }
        val pad = (20 * ctx.resources.displayMetrics.density).toInt()
        val box = LinearLayout(ctx).apply { setPadding(pad, pad / 2, pad, 0); addView(input) }
        MaterialAlertDialogBuilder(ctx)
            .setTitle("Autre catégorie")
            .setView(box)
            .setPositiveButton("OK") { _, _ ->
                val v = input.text.toString().trim()
                if (v.isNotEmpty()) { category = v; render() }
            }
            .setNegativeButton("Fermer", null)
            .show()
    }
}
