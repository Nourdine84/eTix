package com.etix.ui.ticket

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout

/**
 * Saisie de la date (jj/mm/aaaa) : « Date invalide » pour une date complète qui n'existe pas (31/02/2026, 29/02/2023),
 * au lieu du message de Material « Format incorrect… » réservé à une saisie mal formée.
 *
 * Material 1.12 (DateFormatTextWatcher) affiche le même message pour toute saisie qu'il ne sait pas lire, posté sur le
 * champ (`post`, sans délai). Un second observateur, ajouté après celui de Material, poste à son tour : il passe donc
 * après lui et ne remplace que ce message-là, seulement si la saisie est complète (2 chiffres, séparateur, 2 chiffres,
 * même séparateur, 4 chiffres). Les autres messages (format, hors limites) restent ceux de Material.
 */
internal object DateInputErrorText {

    const val NONEXISTENT = "Date invalide : cette date n'existe pas."

    private val COMPLETE = Regex("""\d{2}([/.\-])\d{2}\1\d{4}""")

    fun install(picker: MaterialDatePicker<*>) {
        // Vue du sélecteur créée : chaque zone de saisie (ouverture ou bascule depuis le calendrier) est un fragment enfant
        val owners = picker.viewLifecycleOwnerLiveData
        owners.observeForever(object : Observer<LifecycleOwner?> {
            override fun onChanged(value: LifecycleOwner?) {
                if (value == null) return
                owners.removeObserver(this)
                picker.childFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                        val layout = v.findViewById<TextInputLayout>(com.google.android.material.R.id.mtrl_picker_text_input_date)
                        layout?.editText?.addTextChangedListener(watcher(layout))
                    }
                }, false)
            }
        })
    }

    private fun watcher(layout: TextInputLayout) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            layout.post {
                val shown = layout.error?.toString() ?: return@post
                val format = layout.context.getString(com.google.android.material.R.string.mtrl_picker_invalid_format)
                val text = layout.editText?.text?.toString().orEmpty()
                if (shown.startsWith(format) && COMPLETE.matches(text)) layout.error = NONEXISTENT
            }
        }
    }
}
