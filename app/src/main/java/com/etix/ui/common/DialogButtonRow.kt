package com.etix.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

/**
 * Boutons d'une fenêtre de confirmation, placés comme ceux de Material : alignés à droite sur une ligne
 * (« Annuler », « Supprimer ») tant que leurs libellés tiennent en entier ; sinon empilés, l'action principale au-dessus
 * (ordre Material), chacun sur toute sa largeur naturelle. Jamais réduits ni tronqués : avec la zone de texte
 * défilante de la fenêtre, les deux boutons restent entièrement visibles (constaté à 320 dp, police 2,0 : la barre de
 * boutons de Material défilait et « Annuler » n'était visible qu'à 47 %).
 * Enfants attendus : le bouton d'annulation, puis l'action principale.
 */
class DialogButtonRow @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    private val gapPx = (8 * resources.displayMetrics.density).toInt()

    var stacked = false
        private set

    private val buttons get() = (0 until childCount).map { getChildAt(it) }.filter { it.visibility != View.GONE }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val avail = max(MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight, 0)
        val unbounded = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val bs = buttons
        bs.forEach { it.measure(unbounded, unbounded) }
        val rowWidth = bs.sumOf { it.measuredWidth } + gapPx * (bs.size - 1).coerceAtLeast(0)
        stacked = rowWidth > avail
        val h = if (!stacked) bs.maxOfOrNull { it.measuredHeight } ?: 0 else {
            bs.forEach { it.measure(MeasureSpec.makeMeasureSpec(avail, MeasureSpec.AT_MOST), unbounded) }
            bs.sumOf { it.measuredHeight }
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(h + paddingTop + paddingBottom, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val right = r - l - paddingRight
        val bs = buttons
        if (!stacked) {
            var x = right
            for (v in bs.reversed()) {
                x -= v.measuredWidth
                v.layout(x, paddingTop, x + v.measuredWidth, paddingTop + v.measuredHeight)
                x -= gapPx
            }
        } else {
            var y = paddingTop
            for (v in bs.reversed()) { // action principale en haut
                v.layout(right - v.measuredWidth, y, right, y + v.measuredHeight)
                y += v.measuredHeight
            }
        }
    }
}
