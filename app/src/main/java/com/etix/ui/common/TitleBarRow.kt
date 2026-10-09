package com.etix.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

/**
 * Barre haute « action de début / titre / action de fin » (iOS : barre de navigation d'une feuille). Les deux actions
 * gardent toujours leur taille et restent accessibles ; le titre, centré, n'est affiché que s'il tient ENTIER sur une
 * ligne entre elles, à la taille de police choisie (jamais réduit ni tronqué). Sinon il est masqué et
 * [onTitleFitChanged] permet d'afficher le nom complet ailleurs (zone défilante de l'écran).
 * Enfants attendus : l'action de début, le titre, l'action de fin.
 */
class TitleBarRow @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    private val gapPx = (8 * resources.displayMetrics.density).toInt()

    /** Vrai après la dernière mesure : le titre tient sur la barre. */
    var titleFits = true
        private set

    /** Appelé (après la mise en page) quand le titre passe de « tient » à « ne tient pas » ou l'inverse. */
    var onTitleFitChanged: ((Boolean) -> Unit)? = null
    private var notified: Boolean? = null

    private val start get() = getChildAt(0)
    private val title get() = getChildAt(1)
    private val end get() = getChildAt(2)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val avail = max(width - paddingLeft - paddingRight, 0)
        val unbounded = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        // Actions : largeur naturelle, chacune au plus la moitié de la barre (libellés courts « Annuler » / « Appliquer »)
        val half = MeasureSpec.makeMeasureSpec(max(avail / 2, 0), MeasureSpec.AT_MOST)
        start.measure(half, unbounded); end.measure(half, unbounded)
        title.measure(unbounded, unbounded)
        // Titre centré : la place est symétrique, limitée par l'action la plus large
        val room = avail - 2 * (max(start.measuredWidth, end.measuredWidth) + gapPx)
        titleFits = title.measuredWidth <= room
        if (!titleFits) title.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY), unbounded)
        val h = maxOf(start.measuredHeight, end.measuredHeight, if (titleFits) title.measuredHeight else 0)
        setMeasuredDimension(width, resolveSize(max(h + paddingTop + paddingBottom, suggestedMinimumHeight), heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val h = b - t
        fun place(v: View, x: Int) { val y = (h - v.measuredHeight) / 2; v.layout(x, y, x + v.measuredWidth, y + v.measuredHeight) }
        place(start, paddingLeft)
        place(end, r - l - paddingRight - end.measuredWidth)
        if (titleFits) place(title, (r - l - title.measuredWidth) / 2) else title.layout(0, 0, 0, 0)
        if (notified != titleFits) {
            notified = titleFits
            val fits = titleFits
            post { onTitleFitChanged?.invoke(fits) }
        }
    }
}
