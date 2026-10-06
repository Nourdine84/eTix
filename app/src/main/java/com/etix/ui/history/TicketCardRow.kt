package com.etix.ui.history

import android.content.Context
import android.os.Build
import android.text.Layout
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.etix.R

/**
 * Ligne d'une carte ticket de l'Historique : 1er enfant = colonne « magasin / catégorie · date », 2e enfant = montant.
 *
 * Montant à droite, centré verticalement (iOS ticketCard), tant que le nom du magasin tient sur deux lignes au plus,
 * sans mot coupé ni troncature. Sinon (petit écran, grande police), le montant passe sous la date, aligné à gauche :
 * le nom dispose de toute la largeur et le montant n'est jamais masqué. Dernier recours pour un nom très long :
 * « … » en fin de deuxième ligne.
 *
 * Disposition calculée à la mesure, sans requestLayout() (pas de boucle de mise en page).
 */
class TicketCardRow @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private val density = resources.displayMetrics.density
    private val gapSide = (12 * density).toInt()
    private val gapBelow = (6 * density).toInt()

    /** Disposition retenue à la dernière mesure (montant sous la date). */
    var amountBelow: Boolean = false
        private set

    private val column: View get() = getChildAt(0)
    private val amount: View get() = getChildAt(1)

    override fun onFinishInflate() {
        super.onFinishInflate()
        // Coupure uniquement entre les mots (pas de césure automatique)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            findViewById<TextView>(R.id.tvStore)?.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val inner = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val unbounded = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        amount.measure(MeasureSpec.makeMeasureSpec(inner, MeasureSpec.AT_MOST), unbounded)
        val side = (inner - amount.measuredWidth - gapSide).coerceAtLeast(0)
        column.measure(MeasureSpec.makeMeasureSpec(side, MeasureSpec.EXACTLY), unbounded)

        val name = findViewById<TextView>(R.id.tvStore)
        amountBelow = name != null && !nameFits(name)
        val content = if (amountBelow) {
            column.measure(MeasureSpec.makeMeasureSpec(inner, MeasureSpec.EXACTLY), unbounded)
            column.measuredHeight + gapBelow + amount.measuredHeight
        } else {
            maxOf(column.measuredHeight, amount.measuredHeight)
        }
        setMeasuredDimension(
            resolveSize(width, widthMeasureSpec),
            resolveSize(content + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val left = paddingLeft
        val top = paddingTop
        if (amountBelow) {
            column.layout(left, top, left + column.measuredWidth, top + column.measuredHeight)
            val y = top + column.measuredHeight + gapBelow
            amount.layout(left, y, left + amount.measuredWidth, y + amount.measuredHeight)
        } else {
            val content = (b - t) - paddingTop - paddingBottom
            val cy = top + (content - column.measuredHeight) / 2
            column.layout(left, cy, left + column.measuredWidth, cy + column.measuredHeight)
            val ax = (r - l) - paddingRight - amount.measuredWidth
            val ay = top + (content - amount.measuredHeight) / 2
            amount.layout(ax, ay, ax + amount.measuredWidth, ay + amount.measuredHeight)
        }
    }

    override fun shouldDelayChildPressedState() = false

    companion object {
        /** Nom sur deux lignes au plus, sans troncature ni mot coupé en fin de ligne. */
        fun nameFits(tv: TextView): Boolean {
            val l = tv.layout ?: return true
            val text = tv.text
            if (l.lineCount == 0) return true
            if (l.getEllipsisCount(l.lineCount - 1) > 0) return false
            for (i in 0 until l.lineCount - 1) {
                val end = l.getLineEnd(i)
                if (end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()) return false
            }
            return l.lineCount <= 2
        }
    }
}
