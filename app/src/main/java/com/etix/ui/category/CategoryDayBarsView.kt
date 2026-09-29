package com.etix.ui.category

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.etix.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * « Évolution journalière » — iOS CategoryBarChartView : une barre par jour (22 × max 120, écart 10, rayon 6),
 * libellé jj/MM dessous. Adaptation Android : largeur réelle (placée dans un HorizontalScrollView), car iOS déborde
 * de l'écran au-delà d'environ 12 jours.
 */
class CategoryDayBarsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private val barW = 22 * d
    private val gap = 10 * d
    private val maxH = 120 * d
    private val labelGap = 6 * d
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.v2_primary) }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.v2_text_secondary)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11f, resources.displayMetrics)
        textAlign = Paint.Align.CENTER
    }
    private val fmt = SimpleDateFormat("dd/MM", Locale.FRANCE)
    private var data: List<Pair<Long, Double>> = emptyList()
    private val r = RectF()

    fun setData(values: List<Pair<Long, Double>>) {
        data = values
        contentDescription = "Évolution journalière : " + values.joinToString(", ") { (day, v) ->
            "${fmt.format(Date(day))} ${String.format(Locale.FRANCE, "%.2f €", v)}"
        }
        requestLayout(); invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = (paddingLeft + paddingRight + data.size * barW + (data.size - 1).coerceAtLeast(0) * gap).toInt()
        val h = (paddingTop + paddingBottom + maxH + labelGap - label.ascent() + label.descent()).toInt()
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), h)
    }

    override fun onDraw(canvas: Canvas) {
        val max = data.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
        var x = paddingLeft.toFloat()
        val base = paddingTop + maxH
        for ((day, v) in data) {
            val h = (v / max * maxH).toFloat().coerceAtLeast(if (v > 0) 2 * d else 0f)
            r.set(x, base - h, x + barW, base)
            canvas.drawRoundRect(r, 6 * d, 6 * d, bar)
            canvas.drawText(fmt.format(Date(day)), x + barW / 2, base + labelGap - label.ascent(), label)
            x += barW + gap
        }
    }
}
