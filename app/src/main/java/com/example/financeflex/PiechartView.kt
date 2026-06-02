package com.example.financeflex

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.financeflex.data.models.CategoryTotal
/**
 * This class implements a custom View used to display expense data
 * as a pie chart within the FinanceFlex application.
 * It visualises category-based spending by converting financial data
 * into proportional slices, improving user understanding of spending patterns.
 * Sources:
 * - Android Developers (2024). Custom Views.
 * - Android Developers (2024). Canvas and Drawables.
 */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    
 //Holds the data used to draw the pie chart
    private var categoryTotals: List<CategoryTotal> = emptyList()
    
//Paint object used for drawing shapes and text
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
 //Predefined colours used for chart segments
    private val chartColors = listOf(
        Color.rgb(6, 43, 70),
        Color.rgb(40, 215, 196),
        Color.rgb(126, 217, 87),
        Color.rgb(255, 193, 7),
        Color.rgb(217, 83, 79),
        Color.rgb(108, 117, 125),
        Color.rgb(102, 16, 242)
    )
/**
     * Updates chart data and refreshes the view.
     */
    fun setData(data: List<CategoryTotal>) {
        categoryTotals = data
        invalidate()
    }
  /**
     * Core rendering method called by the Android framework.
     * Responsible for drawing the pie chart and legend.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
//Handle empty dataset
        if (categoryTotals.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val total = categoryTotals.sumOf { it.totalAmount }
 //Handle invalid totals
        if (total <= 0.0) {
            drawEmptyState(canvas)
            return
        }
//Define chart dimensions
        val size = width.coerceAtMost(height) * 0.75f
        val left = (width - size) / 2f
        val top = 20f
        val right = left + size
        val bottom = top + size
        
//start at top
        var startAngle = -90f
        
 //Draw each category slice
        categoryTotals.forEachIndexed { index, item ->
            val sweepAngle = ((item.totalAmount / total) * 360).toFloat()

            paint.color = chartColors[index % chartColors.size]
            paint.style = Paint.Style.FILL

            canvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, true, paint)

            startAngle += sweepAngle
        }
 // Draw legend below chart
        drawLegend(canvas, top + size + 35f)
    }
 /**
     * Draws legend showing category names and values.
     */
    private fun drawLegend(canvas: Canvas, startY: Float) {
        paint.textSize = 32f
        paint.style = Paint.Style.FILL

        var y = startY

        categoryTotals.forEachIndexed { index, item ->
            //Draw colour indicator
            paint.color = chartColors[index % chartColors.size]
            canvas.drawRect(30f, y - 24f, 60f, y + 6f, paint)

            // Draw label text
            paint.color = Color.rgb(3, 27, 45)
            canvas.drawText("${item.categoryName}: R${"%.2f".format(item.totalAmount)}", 75f, y, paint)

            y += 42f
        }
    }
/**
     * Displays message when no data is available.
     */
    private fun drawEmptyState(canvas: Canvas) {
        paint.color = Color.rgb(107, 114, 128)
        paint.textSize = 34f
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL

        canvas.drawText("No expense data to display", width / 2f, height / 2f, paint)
   //Reset alignment
        paint.textAlign = Paint.Align.LEFT
    }
}
