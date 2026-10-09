package com.etix.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

/**
 * Nom (bloc principal) suivi d'une valeur (montant, pourcentage) : côte à côte tant que chaque MOT du nom tient dans la
 * place laissée par la valeur ; sinon la valeur passe SOUS le nom (alignée au début). Le nom passe à la ligne entre
 * les mots, n'est jamais tronqué ni coupé en milieu de mot, et la police n'est jamais réduite (constaté à 320 dp,
 * police 2,0 : « Alimen / tation et pro / duits », noms tronqués « … »).
 * Enfants attendus : le bloc principal (un texte, ou un bloc vertical de textes), puis la valeur.
 * Bloc vertical : chaque texte doit loger son mot le plus long, chaque autre élément sa largeur naturelle.
 */
class TrailingValueRow @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    private val gapPx = (12 * resources.displayMetrics.density).toInt()
    private val lineGapPx = (2 * resources.displayMetrics.density).toInt()

    /** Vrai après la dernière mesure : valeur placée sous le nom. */
    var stacked = false
        private set

    private val main get() = getChildAt(0)
    private val value get() = getChildAt(1)?.takeIf { it.visibility != View.GONE }

    /** Largeur minimale du bloc principal pour qu'aucun mot ne soit coupé. */
    private fun requiredWidth(v: View): Int {
        val unbounded = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        return when {
            v.visibility == View.GONE -> 0
            v is TextView -> longestWord(v)
            v is LinearLayout && v.orientation == LinearLayout.VERTICAL ->
                (0 until v.childCount).maxOfOrNull { requiredWidth(v.getChildAt(it)) }?.plus(v.paddingLeft + v.paddingRight) ?: 0
            else -> { v.measure(unbounded, unbounded); v.measuredWidth }
        }
    }

    private fun longestWord(t: TextView): Int {
        val words = t.text.toString().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val w = words.maxOfOrNull { t.paint.measureText(it) } ?: 0f
        return kotlin.math.ceil(w).toInt() + t.compoundPaddingLeft + t.compoundPaddingRight
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val unbounded = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val m = main; val v = value
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            m.measure(unbounded, unbounded); v?.measure(unbounded, unbounded)
            stacked = false
            setMeasuredDimension(m.measuredWidth + (v?.let { gapPx + it.measuredWidth } ?: 0) + paddingLeft + paddingRight,
                max(m.measuredHeight, v?.measuredHeight ?: 0) + paddingTop + paddingBottom)
            return
        }
        val avail = max(MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight, 0)
        val atMost = MeasureSpec.makeMeasureSpec(avail, MeasureSpec.AT_MOST)
        val height: Int
        if (v == null) {
            stacked = false
            m.measure(atMost, unbounded)
            height = m.measuredHeight
        } else {
            v.measure(unbounded, unbounded)
            val left = avail - gapPx - v.measuredWidth
            stacked = v.measuredWidth > avail || requiredWidth(m) > left
            if (!stacked) {
                m.measure(MeasureSpec.makeMeasureSpec(max(left, 0), MeasureSpec.AT_MOST), unbounded)
                height = max(m.measuredHeight, v.measuredHeight)
            } else {
                m.measure(atMost, unbounded)
                v.measure(atMost, unbounded)
                height = m.measuredHeight + lineGapPx + v.measuredHeight
            }
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(height + paddingTop + paddingBottom, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val m = main; val v = value
        val x0 = paddingLeft; val y0 = paddingTop
        val rowH = b - t - paddingTop - paddingBottom
        if (v == null || stacked) {
            m.layout(x0, y0, x0 + m.measuredWidth, y0 + m.measuredHeight)
            v?.let { val y = y0 + m.measuredHeight + lineGapPx; it.layout(x0, y, x0 + it.measuredWidth, y + it.measuredHeight) }
        } else {
            val my = y0 + (rowH - m.measuredHeight) / 2
            m.layout(x0, my, x0 + m.measuredWidth, my + m.measuredHeight)
            val vx = r - l - paddingRight - v.measuredWidth
            val vy = y0 + (rowH - v.measuredHeight) / 2
            v.layout(vx, vy, vx + v.measuredWidth, vy + v.measuredHeight)
        }
    }
}
