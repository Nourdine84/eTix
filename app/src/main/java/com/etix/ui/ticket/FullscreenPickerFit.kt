package com.etix.ui.ticket

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout

/**
 * Sélecteur de date plein écran (saisie jj/mm/aaaa en grande police) : l'en-tête (fermeture, OK) reste fixe en haut et
 * seule la zone de saisie défile au-dessus du clavier.
 *
 * Material 1.12 ne place pas cette zone dans un conteneur défilant : quand le message d'une date invalide (5 lignes à
 * police 2,0, 320 dp) atteint le clavier, le système faisait glisser toute la fenêtre vers le haut et poussait la
 * fermeture et OK sous la barre d'état (constaté sur émulateur API 34). Ici : fenêtre redimensionnée (plus de
 * glissement), zone de saisie placée dans un défilement réduit de la hauteur du clavier, champ et message gardés
 * visibles. Si la structure de Material change (vues introuvables), le sélecteur reste tel que Material le fournit.
 */
internal object FullscreenPickerFit {

    fun install(picker: MaterialDatePicker<*>) {
        // Dès la création de la vue, avant que Material n'y place la saisie (onStart) : le champ n'est jamais déplacé
        // après avoir reçu le focus, le clavier s'ouvre normalement.
        val owners = picker.viewLifecycleOwnerLiveData
        owners.observeForever(object : Observer<LifecycleOwner?> {
            override fun onChanged(value: LifecycleOwner?) {
                if (value == null) return
                owners.removeObserver(this)
                apply(picker)
            }
        })
    }

    private fun apply(picker: MaterialDatePicker<*>) {
        picker.dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val root = picker.view ?: return
        val frame = root.findViewById<View>(com.google.android.material.R.id.mtrl_calendar_frame) ?: return
        val parent = frame.parent as? LinearLayout ?: return // déjà installé : parent = défilement
        val index = parent.indexOfChild(frame)
        val scroll = BoundedScrollView(frame.context)
        parent.removeViewAt(index)
        scroll.addView(frame, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        parent.addView(scroll, index, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.navigationBars()).bottom
            if (v.paddingBottom != bottom) {
                v.setPadding(0, 0, 0, bottom)
                v.post { scroll.keepFocusedFieldVisible() }
            }
            insets
        }
        frame.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) scroll.post { scroll.keepFocusedFieldVisible() }
        }
        ViewCompat.requestApplyInsets(scroll)
    }

    /**
     * Défilement dont le contenu est mesuré au plus à sa propre hauteur (et non sans limite) : la saisie se mesure à sa
     * taille naturelle, le calendrier (liste de mois, accessible par la bascule) reste borné comme sans défilement.
     */
    private class BoundedScrollView(context: Context) : NestedScrollView(context) {

        override fun measureChildWithMargins(
            child: View, parentWidthMeasureSpec: Int, widthUsed: Int, parentHeightMeasureSpec: Int, heightUsed: Int
        ) {
            val lp = child.layoutParams as ViewGroup.MarginLayoutParams
            val w = ViewGroup.getChildMeasureSpec(parentWidthMeasureSpec,
                paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin + widthUsed, lp.width)
            val size = View.MeasureSpec.getSize(parentHeightMeasureSpec)
            val h = if (View.MeasureSpec.getMode(parentHeightMeasureSpec) == View.MeasureSpec.UNSPECIFIED || size == 0)
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            else View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.AT_MOST)
            child.measure(w, h)
        }

        /** Champ en cours de saisie, avec son message d'erreur, ramené au-dessus du clavier. */
        fun keepFocusedFieldVisible() {
            var target: View = findFocus() ?: return
            var p: android.view.ViewParent? = target.parent
            while (p is View && p !== this) {
                if (p is TextInputLayout) { target = p; break }
                p = (p as View).parent
            }
            val r = Rect(0, 0, target.width, target.height)
            offsetDescendantRectToMyCoords(target, r)
            val visibleBottom = scrollY + height - paddingBottom
            if (r.bottom > visibleBottom) scrollBy(0, r.bottom - visibleBottom)
            else if (r.top < scrollY) scrollBy(0, r.top - scrollY)
        }
    }
}
