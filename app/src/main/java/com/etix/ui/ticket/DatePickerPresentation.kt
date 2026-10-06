package com.etix.ui.ticket

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.TypedValue
import com.etix.features.ticket.DatePickerRules
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Mesures sur l'appareil pour [DatePickerRules] : libellé de mois le plus long de l'année, avec le style du bouton
 * d'en-tête du calendrier Material 3 (labelLarge : 14 sp, sans-serif-medium, interlettrage 0,1 sp). La taille en sp
 * passe par la conversion du système (non linéaire en grande police depuis Android 14), comme le bouton lui-même.
 */
internal object DatePickerPresentation {

    fun longestMonthLabelDp(context: Context, year: Int = Calendar.getInstance().get(Calendar.YEAR)): Float {
        val dm = context.resources.displayMetrics
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, dm)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.1f / 14f
        }
        val locale = Locale.getDefault()
        val format = SimpleDateFormat("LLLL yyyy", locale)
        val cal = Calendar.getInstance().apply { clear(); set(year, Calendar.JANUARY, 15) }
        var widest = 0f
        for (m in 0 until 12) {
            cal.set(Calendar.MONTH, m)
            val label = format.format(cal.time).replaceFirstChar { it.titlecase(locale) }
            widest = maxOf(widest, paint.measureText(label))
        }
        return widest / dm.density
    }

    fun prefersTextInput(context: Context): Boolean {
        val conf = context.resources.configuration
        return DatePickerRules.prefersTextInput(conf.screenWidthDp, conf.fontScale, longestMonthLabelDp(context))
    }
}
