package com.etix.ui.ticket

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

/**
 * Libellé de champ suivi d'une indication (ex. « CATÉGORIE » + « Suggéré par l'OCR ») : sur une même ligne quand les
 * deux tiennent à leur largeur naturelle, l'indication alignée à droite ; sinon l'indication passe SOUS le libellé.
 * Le libellé n'est jamais rétréci ni coupé en milieu de mot, la police n'est jamais réduite (constaté à 320 dp, police
 * 2,0 : dans une ligne horizontale, « CATÉGORIE » s'affichait « CAT / ÉGO / RIE »).
 * Enfants attendus : le libellé, puis l'indication (masquable).
 */
class LabelBadgeRow @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    private val gapPx = (8 * resources.displayMetrics.density).toInt()
    private val lineGapPx = (2 * resources.displayMetrics.density).toInt()

    /** Vrai après la dernière mesure : indication placée sous le libellé. */
    var stacked = false
        private set

    private val label get() = getChildAt(0)
    private val badge get() = getChildAt(1)?.takeIf { it.visibility != View.GONE }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val avail = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val unbounded = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val l = label; val b = badge
        l.measure(unbounded, unbounded)
        b?.measure(unbounded, unbounded)
        val naturalL = l.measuredWidth
        val naturalB = b?.measuredWidth ?: 0
        stacked = b != null && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED &&
            naturalL + gapPx + naturalB > avail
        val atMost = MeasureSpec.makeMeasureSpec(max(avail, 0), MeasureSpec.AT_MOST)
        val height: Int
        if (b == null || !stacked) {
            if (b != null) l.measure(MeasureSpec.makeMeasureSpec(max(avail - gapPx - naturalB, 0), MeasureSpec.AT_MOST), unbounded)
            else l.measure(atMost, unbounded)
            height = max(l.measuredHeight, b?.measuredHeight ?: 0)
        } else {
            l.measure(atMost, unbounded)
            b.measure(atMost, unbounded)
            height = l.measuredHeight + lineGapPx + b.measuredHeight
        }
        val w = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED)
            naturalL + (if (b != null) gapPx + naturalB else 0) + paddingLeft + paddingRight
        else MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, resolveSize(height + paddingTop + paddingBottom, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val l = label; val b = badge
        val x0 = paddingLeft; val y0 = paddingTop
        val rowH = bottom - top - paddingTop - paddingBottom
        if (b == null) {
            val y = y0 + (rowH - l.measuredHeight) / 2
            l.layout(x0, y, x0 + l.measuredWidth, y + l.measuredHeight)
        } else if (!stacked) {
            val ly = y0 + (rowH - l.measuredHeight) / 2
            l.layout(x0, ly, x0 + l.measuredWidth, ly + l.measuredHeight)
            val bx = right - left - paddingRight - b.measuredWidth
            val by = y0 + (rowH - b.measuredHeight) / 2
            b.layout(bx, by, bx + b.measuredWidth, by + b.measuredHeight)
        } else {
            l.layout(x0, y0, x0 + l.measuredWidth, y0 + l.measuredHeight)
            val by = y0 + l.measuredHeight + lineGapPx
            b.layout(x0, by, x0 + b.measuredWidth, by + b.measuredHeight)
        }
    }
}
