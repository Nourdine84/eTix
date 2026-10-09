package com.etix.ui.common

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import kotlin.math.max

/**
 * Zone défilante dont les défilements AUTOMATIQUES (champ qui prend le focus, curseur, clavier qui s'ouvre, message
 * d'erreur qui apparaît) ne laissent jamais un bloc de texte coupé en haut : si la position demandée tombe au milieu
 * d'un bloc situé au-dessus de la zone à montrer, elle est avancée juste après ce bloc (entièrement sorti de la vue),
 * ou reculée à son début si la zone à montrer reste alors entière. Constaté à 320 dp, police 2,0 : l'en-tête de la
 * saisie d'un budget restait à moitié visible au-dessus du champ. Le défilement par l'utilisateur n'est pas modifié.
 * Enfant attendu : un bloc vertical (LinearLayout) dont les enfants sont les blocs de texte et les champs.
 */
class CleanTopScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ScrollView(context, attrs) {

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int {
        val delta = super.computeScrollDeltaToGetChildRectOnScreen(rect)
        val content = getChildAt(0) as? ViewGroup ?: return delta
        val h = height - paddingTop - paddingBottom
        val maxY = max(0, content.height + paddingTop + paddingBottom - height)
        val y = (scrollY + delta).coerceIn(0, maxY)
        for (i in 0 until content.childCount) {
            val c = content.getChildAt(i)
            if (c.visibility != View.VISIBLE) continue
            val top = content.top + c.top
            val bottom = content.top + c.bottom
            // Bloc au-dessus de la zone à montrer, coupé par le bord haut
            if (top < y && y < bottom && bottom <= rect.top) {
                // Bloc entièrement sorti, sinon bloc entièrement visible : position atteignable (≤ défilement maximal) où
                // la zone à montrer reste entière. Sans le contrôle de l'atteignable, « sorti » était ramené au maximum
                // et recoupait le bloc (320 dp, police 2,0, message d'erreur affiché : run 37916561712).
                val snapped = listOf(bottom, top).firstOrNull { it in 0..maxY && rect.top >= it && rect.bottom <= it + h } ?: y
                return snapped - scrollY
            }
        }
        return y - scrollY
    }
}
