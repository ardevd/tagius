package net.ardevd.tagius.features.stats.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SimplePieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val slices = mutableMapOf<String, Long>()
    private var totalValue = 0L

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private val rectF = RectF()

    fun setData(data: Map<String, Long>) {
        slices.clear()
        slices.putAll(data)
        totalValue = data.values.sum()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (totalValue == 0L) return

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) * 0.9f

        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        var currentAngle = -90f

        for ((tag, value) in slices) {
            val sweepAngle = (value.toFloat() / totalValue.toFloat()) * 360f

            // Generate a color based on tag hash or random
            paint.color = Color.HSVToColor(floatArrayOf(
                Math.floorMod(tag.hashCode(), 360).toFloat(),
                0.6f,
                0.9f
            ))

            canvas.drawArc(rectF, currentAngle, sweepAngle, true, paint)
            
            // Draw text if slice is big enough
            if (sweepAngle > 15f) {
                val textAngle = Math.toRadians((currentAngle + sweepAngle / 2).toDouble())
                val tx = cx + radius * 0.6f * kotlin.math.cos(textAngle).toFloat()
                val ty = cy + radius * 0.6f * kotlin.math.sin(textAngle).toFloat()
                canvas.drawText(tag, tx, ty - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint)
            }

            currentAngle += sweepAngle
        }
    }
}
