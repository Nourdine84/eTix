package com.etix.ui.ticket

import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.etix.R
import com.etix.databinding.ViewTicketFormBinding
import com.etix.features.ocr.scan.ScanConfidence
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
private const val OTHER_LABEL = "Autre…"

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

    private var _bound = false

    init {
        b.inputDate.setOnClickListener { pickDate() }
        b.rowCategory.setOnClickListener { pickCategory() }
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) = renderMarks()
        }
        b.inputStore.addTextChangedListener(watcher)
        b.inputAmount.addTextChangedListener(watcher)
        _bound = true
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
        clearScanMarks()
        render()
    }

    /**
     * Lot 9 — marque d'un champ après un scan. [scanned] = contenu du champ au moment du scan : dès que
     * l'utilisateur le modifie, la marque disparaît (la valeur est alors la sienne, plus celle de l'OCR) ;
     * elle réapparaît s'il revient exactement à la valeur lue.
     */
    enum class ScanMark { DETECTED, TO_CHECK, NOT_READ }
    private data class Mark(val mark: ScanMark, val scanned: String)

    private var storeMark: Mark? = null
    private var amountMark: Mark? = null
    private var dateMark: Mark? = null

    /**
     * Lot 9 — préremplissage après un scan (iOS AddTicketViewModel.handleOCRResult) : magasin, montant et date
     * remplacés s'ils ont été lus ; catégorie appliquée seulement si aucune n'est déjà choisie. Rien n'est
     * enregistré ici. Marques (écart iOS, décision du 02/10/2026) : « Détecté » (confiance haute, jamais
     * « Vérifié »), « À vérifier », et « Non lu » / « Non lue » pour un champ absent du ticket : son contenu
     * (vide, saisie précédente ou date du jour) ne doit pas sembler avoir été lu.
     */
    fun applyScan(
        store: String?, storeConfidence: ScanConfidence,
        amount: Double?, amountConfidence: ScanConfidence,
        dateMillisRead: Long?, dateConfidence: ScanConfidence,
        suggestedCategory: String?, categoryBadge: Boolean
    ) {
        store?.let { b.inputStore.setText(it) }
        amount?.let { b.inputAmount.setText(TicketFormRules.formatAmountForInput(it)) }
        dateMillisRead?.let { dateMillis = TicketFormRules.combineDay(TicketFormRules.toPickerSelection(it), System.currentTimeMillis()) }
        storeMark = Mark(markOf(store != null, storeConfidence), b.inputStore.text.toString())
        amountMark = Mark(markOf(amount != null, amountConfidence), b.inputAmount.text.toString())
        dateMark = Mark(markOf(dateMillisRead != null, dateConfidence), dateMillis.toString())
        if (category.isEmpty() && !suggestedCategory.isNullOrBlank()) {
            category = suggestedCategory
            b.tvCategorySuggested.visibility = if (categoryBadge) android.view.View.VISIBLE else android.view.View.GONE
        }
        setAmountInvalid(false)
        render()
    }

    private fun markOf(read: Boolean, c: ScanConfidence) = when {
        !read || c == ScanConfidence.NONE -> ScanMark.NOT_READ
        c == ScanConfidence.HIGH -> ScanMark.DETECTED
        else -> ScanMark.TO_CHECK
    }

    fun clearScanMarks() {
        storeMark = null; amountMark = null; dateMark = null
        b.tvCategorySuggested.visibility = android.view.View.GONE
        renderMarks()
    }

    /** Marques du scan, pour les conserver si l'écran est recréé (rotation, changement de thème…). */
    fun saveScanMarks(out: android.os.Bundle) {
        listOf("store" to storeMark, "amount" to amountMark, "date" to dateMark).forEach { (k, m) ->
            if (m != null) { out.putString("mark_$k", m.mark.name); out.putString("mark_${k}_scanned", m.scanned) }
        }
        out.putBoolean("mark_category", b.tvCategorySuggested.visibility == android.view.View.VISIBLE)
    }

    fun restoreScanMarks(saved: android.os.Bundle) {
        fun m(k: String) = saved.getString("mark_$k")?.let { name ->
            runCatching { Mark(ScanMark.valueOf(name), saved.getString("mark_${k}_scanned").orEmpty()) }.getOrNull()
        }
        storeMark = m("store"); amountMark = m("amount"); dateMark = m("date")
        if (saved.getBoolean("mark_category")) b.tvCategorySuggested.visibility = android.view.View.VISIBLE
        renderMarks()
    }

    /** Libellé affiché pour un champ (tests). */
    internal fun markFor(field: String): ScanMark? = when (field) {
        "store" -> storeMark; "amount" -> amountMark; else -> dateMark
    }?.mark

    /** Fond coloré construit directement (une teinte de fond n'a pas d'effet sur Android 5, constaté sur émulateur API 21). */
    private fun capsule(color: Int) = android.graphics.drawable.GradientDrawable().apply { cornerRadius = 999f; setColor(color) }

    private fun renderMarks() {
        if (_bound) {
            setBadge(b.badgeStore, storeMark?.takeIf { it.scanned == b.inputStore.text.toString() }, "Magasin", false)
            setBadge(b.badgeAmount, amountMark?.takeIf { it.scanned == b.inputAmount.text.toString() }, "Montant", false)
            setBadge(b.badgeDate, dateMark?.takeIf { it.scanned == dateMillis.toString() }, "Date", true)
        }
    }

    private fun setBadge(v: android.widget.TextView, m: Mark?, field: String, feminine: Boolean) {
        val ctx = v.context
        if (m == null) { v.visibility = android.view.View.GONE; return }
        val e = if (feminine) "e" else ""
        when (m.mark) {
            ScanMark.DETECTED -> {
                v.text = "Détecté$e"
                v.setTextColor(ContextCompat.getColor(ctx, R.color.v2_primary))
                v.background = capsule(ContextCompat.getColor(ctx, R.color.v2_primary_glow))
                v.contentDescription = "$field détecté$e sur le ticket, non vérifié$e"
            }
            ScanMark.TO_CHECK -> {
                v.text = "À vérifier"
                v.setTextColor(ContextCompat.getColor(ctx, R.color.v2_attention))
                v.background = capsule(ContextCompat.getColor(ctx, R.color.v2_attention_12))
                v.contentDescription = "$field détecté$e sur le ticket, à vérifier"
            }
            ScanMark.NOT_READ -> {
                v.text = "Non lu$e"
                v.setTextColor(ContextCompat.getColor(ctx, R.color.v2_attention))
                v.background = capsule(ContextCompat.getColor(ctx, R.color.v2_attention_12))
                v.contentDescription = "$field non lu$e sur le ticket, à compléter"
            }
        }
        v.visibility = android.view.View.VISIBLE
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
        renderMarks()
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

    /**
     * iOS CategoryPickerSheet : système + utilisées, puis « Autre… » (section séparée sur iOS → dernière ligne ici),
     * « Effacer » et « Fermer ». Deux boutons maximum : trois boutons s'empilent et sortent de l'écran
     * (constaté sur émulateur API 34).
     */
    private fun pickCategory() {
        val ctx = fragment.requireContext()
        val items = TicketFormRules.pickerCategories(usedCategories())
        val labels = (items + OTHER_LABEL).toTypedArray()
        val checked = items.indexOf(category)
        val builder = MaterialAlertDialogBuilder(ctx)
            .setTitle("Catégorie")
            .setSingleChoiceItems(labels, checked) { d, which ->
                d.dismiss()
                b.tvCategorySuggested.visibility = android.view.View.GONE
                if (which == items.size) pickCustomCategory() else { category = items[which]; render() }
            }
            .setNegativeButton("Fermer", null)
        if (category.isNotEmpty()) {
            builder.setPositiveButton("Effacer") { _, _ ->
                category = ""; b.tvCategorySuggested.visibility = android.view.View.GONE; render()
            }
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
