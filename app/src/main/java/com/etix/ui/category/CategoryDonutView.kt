package com.etix.ui.category

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.etix.R

/**
 * Anneau de répartition — iOS CategoryDonutView : un arc par catégorie, départ en haut (−90°), sens horaire,
 * épaisseur 40, extrémités droites. Anneau de fond (v2_fill) visible si le total est nul.
 */
class CategoryDonutView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val stroke = 40f * resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.BUTT
    }
    private val track = Paint(paint).apply { color = ContextCompat.getColor(context, R.color.v2_fill) }
    private val oval = RectF()
    private var slices: List<Pair<Double, Int>> = emptyList()

    fun setSlices(values: List<Pair<Double, Int>>) {
        slices = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val size = minOf(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom).toFloat()
        val left = paddingLeft + (width - paddingLeft - paddingRight - size) / 2 + stroke / 2
        val top = paddingTop + (height - paddingTop - paddingBottom - size) / 2 + stroke / 2
        oval.set(left, top, left + size - stroke, top + size - stroke)
        canvas.drawOval(oval, track)
        val total = slices.sumOf { it.first }
        if (total <= 0) return
        var start = -90f
        for ((value, color) in slices) {
            val sweep = (value / total * 360.0).toFloat()
            if (sweep > 0f) {
                paint.color = color
                canvas.drawArc(oval, start, sweep, false, paint)
            }
            start += sweep
        }
    }

    companion object {
        val PALETTE = intArrayOf(
            R.color.v2_chart_1, R.color.v2_chart_2, R.color.v2_chart_3, R.color.v2_chart_4,
            R.color.v2_chart_5, R.color.v2_chart_6, R.color.v2_chart_7
        )
        fun colorAt(ctx: Context, index: Int) = ContextCompat.getColor(ctx, PALETTE[index % PALETTE.size])
    }
}
