package com.etix.ui.common

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlin.math.max

/**
 * Sélecteur segmenté (« Aujourd'hui / Ce mois / Cette année ») : boutons de même largeur côte à côte tant que chaque
 * libellé y tient EN ENTIER à la taille de police choisie ; sinon les boutons sont empilés (un par ligne, pleine
 * largeur). Même taille de texte pour tous, jamais réduite (constaté à 320 dp, police 2,0 : « Cette ann… » et trois
 * tailles différentes avec la réduction automatique).
 * Les boutons sont déclarés avec layout_width="0dp" et layout_weight="1" (disposition côte à côte).
 */
class AdaptiveToggleGroup @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialButtonToggleGroup(context, attrs) {

    /** Vrai après la dernière mesure : boutons empilés. */
    val stacked: Boolean get() = orientation == LinearLayout.VERTICAL

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED && childCount > 0) {
            val avail = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
            val unbounded = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            var widest = 0
            for (i in 0 until childCount) {
                val c = getChildAt(i)
                c.measure(unbounded, unbounded)
                widest = max(widest, c.measuredWidth)
            }
            setStacked(widest * childCount > avail)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun setStacked(stack: Boolean) {
        if (stack == stacked) return
        orientation = if (stack) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        for (i in 0 until childCount) {
            val lp = getChildAt(i).layoutParams as LinearLayout.LayoutParams
            if (stack) { lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT; lp.weight = 0f } else { lp.width = 0; lp.weight = 1f }
            getChildAt(i).layoutParams = lp
        }
    }
}
